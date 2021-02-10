import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Scanner;

public class MedinaReplication {
    //Deadline
    int deadline;
    //Number of CPU Core
    int n_core = 4;
    //Number of Redundancy
    double n;
    //DAG
    McDAG dag;
    CPU cpu;
    String xml_name;

    int max_freq = 1200;

    double overrun_percent;
    int n_overrun;

    Vertex sorted_tasks[];

    boolean VERBOSE = false;

    String pathSeparator = File.separator;

    //HotSpot location and information
    String hotspot_path = "MatEx-1.0" + pathSeparator + "MatEx";
    String hotspot_config = "MatEx-1.0" + pathSeparator + "configs" + pathSeparator;
    String floorplan = "MatEx-1.0" + pathSeparator + "floorplans" + pathSeparator;
    String powertrace = "MatEx-1.0" + pathSeparator + "powertrace" + pathSeparator;
    String thermaltrace = "MatEx-1.0" + pathSeparator + "thermaltrace" + pathSeparator + "thermal.ttrace";

    public MedinaReplication(int deadline, int n_core, double n, McDAG dag, String xml_name, double overrun_percent, boolean VERBOSE) {
        this.deadline = deadline;
        this.n_core = n_core;
        this.n = n;
        this.dag = dag;
        this.xml_name = xml_name;
        this.overrun_percent = overrun_percent;
        this.VERBOSE = VERBOSE;
    }

    public void start() throws Exception {
        //Sort Tasks
        sorted_tasks = sort_tasks(dag.getVertices().toArray(new Vertex[0]).clone());
        reset_schedule();
        feasibility();
        reset_schedule();
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
        if(overrun_percent==0){
            do {
                t = dag.getNodebyName(get_task(true));
                if (t == null) break;
                int startTime = 0;
                for (Edge e : t.getRcvEdges()) {
                    if (cpu.getEndTimeTask(e.getSrc().getName() + " CR"+e.getSrc().getReplica()) > startTime) {
                        startTime = cpu.getEndTimeTask(e.getSrc().getName()+ " CR"+e.getSrc().getReplica()) + 1;
                    }
                }
                boolean scheduled = false;
                for (int i = startTime; i < deadline - t.getWcet(0); i++) {
                    for (int j = 0; j < n_core; j++) {
                        if (cpu.CheckTimeSlot(j, i, i + t.getWcet(0))) {
                            cpu.SetTaskOnCore(t.getName() + " CR"+(t.getScheduled() + 1), j, i, i + t.getWcet(0) - 1);;
                            t.setScheduled(t.getScheduled() + 1);
                            scheduled = true;
                            break;
                        }
                    }
                    if(scheduled) break;
                }
                if (VERBOSE)
                    System.out.println("MED SCH: [ "+t.getName() + " "+t.getScheduled()+" ]  ");
                if (!scheduled)
                    throw new Exception("Infeasible!");


            } while (t != null);
        }else{
            do {
                t = dag.getNodebyName(get_task(false));
                if (t == null) break;
                int startTime = 0;
                for (Edge e : t.getRcvEdges()) {
                    if (cpu.getEndTimeTask(e.getSrc().getName() + " CO"+1) > startTime) {
                        startTime = cpu.getEndTimeTask(e.getSrc().getName()+ " CO"+1) + 1;
                    }
                }
                boolean scheduled = false;
                for (int i = startTime; i < deadline - t.getWcet(1); i++) {
                    for (int j = 0; j < n_core; j++) {
                        if (cpu.CheckTimeSlot(j, i, i + t.getWcet(1))) {
                            cpu.SetTaskOnCore(t.getName() + " CR"+(t.getScheduled() + 1), j, i, i + t.getWcet(0) - 1);
                            cpu.SetTaskOnCore(t.getName() + " CO"+(t.getScheduled() + 1), j, i + t.getWcet(0), i + t.getWcet(1) - 1);
                            t.setScheduled(t.getScheduled() + 1);
                            scheduled = true;
                            break;
                        }
                    }
                    if(scheduled) break;
                }
                if (!scheduled)
                    throw new Exception("Infeasible!");

            } while (t != null);
        }
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
                if (cpu1.getEndTimeTask(e.getSrc().getName() + " CO"+e.getSrc().getReplica()) > startTime) {
                    startTime = cpu1.getEndTimeTask(e.getSrc().getName()+ " CO"+e.getSrc().getReplica()) + 1;
                }
            }
            boolean scheduled = false;
            for (int i = startTime; i < deadline - t.getWcet(1); i++) {
                for (int j = 0; j < n_core; j++) {
                    if (cpu1.CheckTimeSlot(j, i, i + t.getWcet(1))) {
                        cpu1.SetTaskOnCore(t.getName() + " CR"+(t.getScheduled() + 1), j, i, i + t.getWcet(0) - 1);
                        cpu1.SetTaskOnCore(t.getName() + " CO"+(t.getScheduled() + 1), j, i + t.getWcet(0), i + t.getWcet(1) - 1);
                        t.setScheduled(t.getScheduled() + 1);
                        scheduled = true;
                        break;
                    }
                }
                if(scheduled) break;
            }
            if (!scheduled)
                throw new Exception("Infeasible!");

        } while (t != null);
        cpu1.debug("MedinaTest");
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


    //Sorting Tasks from big LPL to small LPL
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
//        floorplan += "Alpha" + cpu.getN_Cores() + ".flp";
        floorplan += "A15_" + cpu.getN_Cores() + ".flp";
//        powertrace += "Alpha" + cpu.getN_Cores() + ".ptrace";
        powertrace += "A15_" + cpu.getN_Cores() + ".ptrace";
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
