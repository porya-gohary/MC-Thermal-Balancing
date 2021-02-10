import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;

import static java.lang.Math.ceil;

public class Salehi {
    //DAG
    McDAG dag;
    String xml_name;
    //Number of Core
    int n_core;
    int deadline;
    String benchmark[];
    int benchmark_time[];
    double n;
    Vertex v[];

    CPU cpu, block_cpu;

    //Faulty Tasks
    Set<Vertex> faults;
    Vertex faults_array[];


    Vertex sorted_tasks[];

    //Number of fault
    int n_fault = 0;
    double fault_percent;

    //Check feasible
    boolean f_feasible = true;

    //Hi Critical Vertex
    Vertex HIv[];

    boolean VERBOSE = false;

    String pathSeparator = File.separator;

    //HotSpot location and information
    String hotspot_path = "MatEx-1.0" + pathSeparator + "MatEx";
    String hotspot_config = "MatEx-1.0" + pathSeparator + "configs" + pathSeparator;
    String floorplan = "MatEx-1.0" + pathSeparator + "floorplans" + pathSeparator;
    String powertrace = "MatEx-1.0" + pathSeparator + "powertrace" + pathSeparator;
    String thermaltrace = "MatEx-1.0" + pathSeparator + "thermaltrace" + pathSeparator + "thermal.ttrace";

    public Salehi(int deadline, int n_core,double n, McDAG dag, String xml_name, double fault_percent, boolean VERBOSE) {
        this.deadline = deadline;
        this.n_core = n_core;
        this.n=n;
        this.dag = dag;
        this.xml_name = xml_name;
        this.fault_percent = fault_percent;
        this.VERBOSE = VERBOSE;
        n_fault = (int) (fault_percent*dag.getNodes_HI().size()/100);
        v = dag.getVertices().toArray(new Vertex[0]);
    }


    public void start() throws Exception {
        //Sort Tasks
        sorted_tasks = sort_tasks(dag.getVertices().toArray(new Vertex[0]).clone());
        reset_schedule();
        feasibility();
        reset_schedule();
        clean_fault();
        clean_sch();
        boolean finish = false;
        while (!finish) {
            boolean f = true;
            try {
                mainScheduling();
            } catch (Exception e) {
                if (VERBOSE) e.printStackTrace();
                boolean x = drop_task();
                if (!x) {
                    System.out.println("Scheduling Problem!");
                    System.exit(0);
                }
                f = false;
            } finally {
                if (f) finish = true;
            }
        }
    }

    //Main Scheduling of System
    public void mainScheduling() throws Exception {
        Vertex t;
        cpu = new CPU(deadline, n_core, dag, VERBOSE);
        do {
            t = dag.getNodebyName(get_task(true));
            if (t == null) break;
            int startTime = 0;
            if (t.isHighCr()) {
                for (Edge e : t.getRcvEdges()) {
                    if (cpu.getEndTimeTask(e.getSrc().getName() + " CO" + (int) (ceil(n / 2) - 1)) > startTime) {
                        startTime = cpu.getEndTimeTask(e.getSrc().getName() + " CO" + (int) (ceil(n / 2) - 1)) + 1;
                    }
                }
                for (int k = 0; k < (int) ceil(n / 2); k++) {
                    boolean scheduled = false;
                    for (int i = startTime; i < deadline - t.getWcet(1); i++) {
                        for (int j = 0; j < n_core; j++) {
                            if (cpu.CheckTimeSlot(j, i, i + t.getWcet(1))) {
                                cpu.SetTaskOnCore(t.getName() + " CR" + k, j, i, i + t.getWcet(0) - 1);
                                cpu.SetTaskOnCore(t.getName() + " CO" + k, j, i + t.getWcet(0), i + t.getWcet(1) - 1);
                                if (VERBOSE)
                                    System.out.println(">>> " + (t.getName() + " CR" + k) + " S: " + i + " E: " + (i + t.getWcet(0) - 1));
                                scheduled = true;
                                break;
                            }
                        }
                        if (scheduled) break;
                    }
                    if (!scheduled)
                        throw new Exception("Infeasible!");
                }
                t.setDone(true);
            } else {
                for (Edge e : t.getRcvEdges()) {
                    if (cpu.getEndTimeTask(e.getSrc().getName() + " CR" + 1) > startTime) {
                        startTime = cpu.getEndTimeTask(e.getSrc().getName() + " CR" + 1) + 1;
                    }
                }
                boolean scheduled = false;

                for (int i = startTime; i < deadline - t.getWcet(0); i++) {
                    for (int j = 0; j < n_core; j++) {
                        if (cpu.CheckTimeSlot(j, i, i + t.getWcet(1))) {
                            cpu.SetTaskOnCore(t.getName() + " CR" + 1, j, i, i + t.getWcet(0) - 1);
                            scheduled = true;
                            break;
                        }
                    }
                    if (scheduled) break;
                }
                if (!scheduled)
                    throw new Exception("Infeasible!");

                t.setDone(true);
            }


        } while (t != null);
    }


    //Check Feasibility of System
    public void feasibility() throws Exception {
        Vertex t;
        CPU cpu1 = new CPU(deadline, n_core, dag, VERBOSE);
        do {
            t = dag.getNodebyName(get_task(false));
            if (t == null) break;
            int startTime = 0;
            for (Edge e : t.getRcvEdges()) {
                if (cpu1.getEndTimeTask(e.getSrc().getName() + " CO" + (int) (ceil(n / 2) - 1)) > startTime) {
                    startTime = cpu1.getEndTimeTask(e.getSrc().getName() + " CO" + (int) (ceil(n / 2) - 1)) + 1;
                }
            }

            for (int k = 0; k < (int) ceil(n / 2); k++) {
                boolean scheduled = false;
                for (int i = startTime; i < deadline - t.getWcet(1); i++) {
                    for (int j = 0; j < n_core; j++) {
                        if (cpu1.CheckTimeSlot(j, i, i + t.getWcet(1))) {
                            cpu1.SetTaskOnCore(t.getName() + " CR" + k, j, i, i + t.getWcet(0) - 1);
                            cpu1.SetTaskOnCore(t.getName() + " CO" + k, j, i + t.getWcet(0), i + t.getWcet(1) - 1);
                            scheduled = true;
                            break;
                        }
                    }
                    if (scheduled) break;
                }
                if (!scheduled)
                    throw new Exception("Infeasible!");

            }
            t.setDone(true);


        } while (t != null);
        inject_fault(dag.getNodes_HI().size(),cpu1);
        f_feasible=false;

    }


    //get the Task that must be run
    public String get_task(boolean LO) {
        String x = null;
        for (Vertex a : sorted_tasks) {
            if (!a.isHighCr() && !LO) continue;
            else if (LO && !a.isRun()) continue;
            boolean run_flag = true;
            if (a.isDone()) continue;
            for (Edge e : a.getRcvEdges()) {
                if (!e.getSrc().isDone()) {
                    run_flag = false;
                    break;
                }
            }
            if (!run_flag) continue;
            else {
                x = a.getName();
                break;
            }
        }
        return x;
    }


    //Sorting Tasks from big to small
    public Vertex[] sort_tasks(Vertex v[]) {
        Arrays.sort(v);
        Collections.reverse(Arrays.asList(v));

        if (VERBOSE) {
            //Show Sorted Vortex Array
            System.out.println("---------------");
            System.out.println("Sorted Tasks:");
            for (Vertex a : v) {
                System.out.println(a.getName() + "  " + a.getWcet(0));
            }
            System.out.println("---------------");
        }
        return v;
    }

    //Drop LO Tasks That cannot schedule in offline phase
    public boolean drop_task() {
        for (int i = sorted_tasks.length - 1; i >= 0; i--) {
            if (!sorted_tasks[i].isHighCr() && sorted_tasks[i].isRun()) {
                sorted_tasks[i].setRun(false);
                if (VERBOSE) System.out.println("■■■  DROP TASK " + sorted_tasks[i].getName());
                return true;
            }
        }
        return false;
    }

    //Reset condition of scheduled tasks
    public void reset_schedule() {
        for (Vertex a : sorted_tasks) {
            a.setDone(false);
            a.setScheduled(0);
        }
    }

    // QoS Calculator
    public double QoS() {
        double QoS = 0;
        for (int i = 0; i < sorted_tasks.length; i++) {
            if (!sorted_tasks[i].isHighCr() && sorted_tasks[i].isRun()) QoS++;
        }
        QoS = QoS / (sorted_tasks.length - dag.getNodes_HI().size());
        return QoS;
    }


    public void inject_fault(int number_of_fault,CPU cpu) throws Exception {
        faults = new HashSet<Vertex>();

        Set<Vertex> nodesHI = new HashSet<Vertex>();
        for (Vertex a : dag.getVertices()) {
            if (a.getWcet(1) != 0)
                nodesHI.add(a);
        }
        HIv = nodesHI.toArray(new Vertex[0]);

        int f = 0;
        Random fault = new Random();
//        System.out.println("...................-->> "+number_of_fault);
        for (int i = 0; i < number_of_fault; i++) {
            do {
                f = fault.nextInt(HIv.length);
            } while (HIv[f].getInjected_fault() != 0);
            if (VERBOSE) System.out.println("↯↯ Fault injected To  " + HIv[f].getName());
            dag.getNodebyName(HIv[f].getName()).setInjected_fault(dag.getNodebyName(HIv[f].getName()).getInjected_fault() + 1);
            faults.add(dag.getNodebyName(HIv[f].getName()));
        }
        faults_array = faults.toArray(new Vertex[0]);
//        Arrays.sort(faults_array);
//        for(Vertex a:faults_array){
//            System.out.println(a.getName()+"  ==>>  "+(a.getWcet(0)+a.getWcet(1)));
//        }
        for (int i = 0; i < faults_array.length; i++) {
//            System.out.println("< >"+faults_array[i].getName() +" ... " +faults_array[i].isHighCr());
            int t = cpu.getEndTimeTask(faults_array[i].getName() + " CR" + (int) (ceil(n / 2) - 1));
            make_blocks(t,cpu);
        }
    }


    public void make_blocks(int time,CPU cpu) throws Exception {
        time++;

        Set<Vertex> block = new HashSet<Vertex>();
        //Set<Vertex> faults=new HashSet<Vertex>();
        for (Vertex a : HIv) {
            if (!a.check_runnable(cpu.get_Running_Tasks(time), n)) {
                //System.out.println("1  "+a.getName());
                continue;
            }
            if (faults.contains(a) && a.getScheduled() == n) {
                // System.out.println("2  "+a.getName());
                continue;
            }
            if ((cpu.getEndTimeTask(" CR" + (int) (ceil(n / 2) - 1)) < time) && ((cpu.getEndTimeTask(" CR" + (int) (ceil(n / 2) - 1)) != -1))) {
                //System.out.println(cpu.getEndTimeTask(" CR"+(int)(ceil(n/2)-1)) + "  < "+ time);
                //  System.out.println("3  "+a.getName());
                continue;
            }
            block.add(a);
        }

        int blk_size = Math.min(n_core, block.size());

        Vertex blk[] = block.toArray(new Vertex[0]);
        if (blk.length > 1) {
            Arrays.sort(blk);
            Collections.reverse(Arrays.asList(blk));
        }
//        for(Vertex a:blk){
//            System.out.println(a.getName());
//        }

//        if(!stackTraceElements[2].getClassName().equals("Safe_Start_Time")) {
//
//        }
        if (blk.length == 0) return;
        if (f_feasible) cpu.Task_Shifter(time, blk[0].getWcet(1));
        else cpu.Task_Shifter(time, blk[0].getWcet(0));

        for (int i = 0; i < blk_size; i++) {
            //UnCompleted for N>3
            cpu.SetTaskOnCore(blk[i].getName() + " F1", i, time + 1, time + blk[i].getWcet(0));
            if (f_feasible)
                cpu.SetTaskOnCore(blk[i].getName() + " O1", i, time + blk[i].getWcet(0) + 1, time + blk[i].getWcet(1));
            dag.getNodebyName(blk[i].getName()).setScheduled(dag.getNodebyName(blk[i].getName()).getScheduled() + 1);
        }


    }

    public void sort_vertex() {
        if (v.length > 1) {
            Arrays.sort(v);
            Collections.reverse(Arrays.asList(v));
        }
        //Show Sorted Vortex Array
//        for(Vertex a:v){
//            System.out.println(a.getName()+"  ==>>  "+(a.getWcet(0)+a.getWcet(1)));
//        }
    }

    public void clean_sch() {
        for (Vertex a : v) {
            a.setScheduled(0);
            a.setInjected_fault(0);
        }
    }

    public void clean_fault() {
        for (Vertex a : v) {
            a.setInjected_fault(0);

        }
    }

    public double[] balanceCalculator() {
        //Temperature Results [0] Avg. Diff. [1] Max. Diff. [2] Max. Temp. [3] Avg. Temp.
        double temp[]= new double[4];
        double Max=0;
        double Avg=0;

//        hotspot_config = "HotSpot" + pathSeparator + "configs" + pathSeparator;
        hotspot_config = "MatEx-1.0" + pathSeparator + "configs" + pathSeparator;
        floorplan = "MatEx-1.0" + pathSeparator + "floorplans" + pathSeparator;
        powertrace = "MatEx-1.0" + pathSeparator + "powertrace" + pathSeparator;
        HotSpot hotSpot = new HotSpot(hotspot_path, VERBOSE);
        HS_input_creator hs_input_creator = new HS_input_creator(cpu);
        try {
//            hs_input_creator.Save_HS("HotSpot", "powertrace", "Alpha" + cpu.getN_Cores() + ".ptrace", cpu.Endtime(-1));
            hs_input_creator.Save("MatEx-1.0", "powertrace", "A15_" + cpu.getN_Cores() + ".ptrace", cpu.Endtime(-1));
        } catch (IOException e) {
            e.printStackTrace();
        }

//        hotspot_config += "hotspot_" + cpu.getN_Cores() + ".config";
        hotspot_config += "matex_" + cpu.getN_Cores() + ".config";
        floorplan += "A15_" + cpu.getN_Cores() + ".flp";
//        floorplan += "Alpha" + cpu.getN_Cores() + ".flp";
        powertrace += "A15_" + cpu.getN_Cores() + ".ptrace";
//        powertrace += "Alpha" + cpu.getN_Cores() + ".ptrace";
        hotSpot.run(hotspot_config, floorplan, powertrace, thermaltrace);

//        String mFolder = "HotSpot";
        String mFolder = "MatEx-1.0";
        String sFolder = "thermaltrace";
        String filename = "thermal.ttrace";
        File thermalFile = null;
        double MaxDiff=0;
        try {
            thermalFile = new File(mFolder + pathSeparator + sFolder + pathSeparator + filename);
            Scanner Reader = new Scanner(thermalFile);
            //Reader.hasNextLine()
            double diff = 0;
            Reader.nextLine();
            for (int j = 0; j < cpu.Endtime(-1); j++) {
                String data = Reader.nextLine();
                String Sdatavalue[] = data.split("\t");
                double value[] = new double[cpu.getN_Cores()];
                int k=0;
                for (int i = 1; i < cpu.getN_Cores()+1; i++) {
                    value[k] = Double.parseDouble(Sdatavalue[i]);
                    k++;
                }

                if(getMax(value)>Max) Max = getMax(value);
                Avg+=getMax(value);

                diff += getMax(value) - getMin(value);
                if(getMax(value) - getMin(value)>MaxDiff) MaxDiff =getMax(value) - getMin(value);

            }
            Reader.close();
            if (VERBOSE) {
                System.out.println("Max. Different= " + MaxDiff);
                System.out.println("Avg. Different= " + (diff / cpu.Endtime(-1)));
            }
            //Temperature Results [0] Avg. Diff. [1] Max. Diff. [2] Max. Temp. [3] Avg. Temp.
            temp[0]=(diff / cpu.Endtime(-1));
            temp[1]=MaxDiff;
            temp[2]= Max;
            temp[3]=Avg/ cpu.Endtime(-1);
        } catch (FileNotFoundException e) {
            if (VERBOSE) {
                System.out.println("An error occurred in Reading Thermal Trace File.");
                System.out.println("Path: " + thermalFile.getAbsolutePath());
                e.printStackTrace();
            }
        }
        return temp;
    }

    // Method for getting the minimum value
    public double getMin(double[] inputArray) {
        double minValue = inputArray[0];
        for (int i = 1; i < inputArray.length; i++) {
            if (inputArray[i] < minValue) {
                minValue = inputArray[i];
            }
        }
        return minValue;
    }

    //Method for getting the maximum value
    public double getMax(double[] inputArray) {
        double maxValue = inputArray[0];
        for (int i = 1; i < inputArray.length; i++) {
            if (inputArray[i] > maxValue) {
                maxValue = inputArray[i];
            }
        }
        return maxValue;
    }

    public CPU getCpu() {
        return cpu;
    }


    public McDAG getDag() {
        return dag;
    }
}
