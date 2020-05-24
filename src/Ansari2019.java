import java.util.*;

import static java.lang.Math.ceil;
import static java.lang.Math.floor;

public class Ansari2019 {

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

    //Number of active core
    int activeCore;

    int max_freq = 1200;

    double overrun_percent;
    int n_overrun;

    Set<String> ov_tasks;
    boolean VERBOSE = false;
    Set<Vertex> HIv;
    Vertex sorted_tasks[];


    public Ansari2019(int deadline, int n_core,double n, McDAG dag, String xml_name, double overrun_percent, boolean VERBOSE) {
        this.deadline = deadline;
        this.n_core = n_core;
        this.n=n;
        this.dag = dag;
        this.xml_name = xml_name;
        this.overrun_percent = overrun_percent;
        this.VERBOSE = VERBOSE;
        activeCore = n_core / 2;
    }

    public void start() throws Exception {
        //Sort Tasks from big to small by WCET LO
        sorted_tasks = sort_tasks(dag.getVertices().toArray(new Vertex[0]).clone());
        ov_tasks = SelectOverrunTasks(dag.getNodes_HI().toArray(new Vertex[0]).clone());
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
        if (VERBOSE) {
            System.out.println("::::::::::");
            System.out.println(xml_name + " Successfully Scheduled! QoS = " + QoS());
            System.out.println("::::::::::");
        }

    }

    //Check Feasibility of System
    public void feasibility() throws Exception {
        Vertex t;
        CPU cpu1 = new CPU(deadline, n_core, dag, VERBOSE);
        do {
            t = dag.getNodebyName(get_task(false));
            if(t==null) break;
            int startTime = 0;
            for (Edge e : t.getRcvEdges()) {
                if (cpu1.getEndTimeTask(e.getSrc().getName() + " CO" + (int) (n - 1)) > startTime) {
                    startTime = cpu1.getEndTimeTask(e.getSrc().getName()) + 1;
                }
            }

            for (int k = 0; k < (int) ceil(n / 2); k++) {
                int mappedCore = cpu1.worstFit();
                boolean scheduled=false;
                for (int i = startTime; i < deadline-t.getWcet(1); i++) {
                    if (cpu1.CheckTimeSlot(mappedCore, i, i + t.getWcet(1)) && (cpu1.maxCoreInterval(i, i + t.getWcet(1)) < activeCore)) {
                        cpu1.SetTaskOnCore(t.getName() + " CR" + k, mappedCore, i, i + t.getWcet(0) - 1);
                        cpu1.SetTaskOnCore(t.getName() + " CO" + k, mappedCore, i+t.getWcet(0), i + t.getWcet(1) - 1);
                        scheduled =true;
                        break;
                    }
                }
                if(!scheduled)
                    throw new Exception("Infeasible!");
            }
            startTime = cpu1.getEndTimeTask(t.getName() + " CO"+(int) (ceil(n / 2)-1)) + 1;
            int name_number=(int) ceil(n / 2);
            for (int k = 0; k < (int) floor(n / 2); k++) {
                int mappedCore = cpu1.worstFit();
                boolean scheduled=false;
                for (int i = startTime; i < deadline-t.getWcet(1); i++) {
                    if (cpu1.CheckTimeSlot(mappedCore, i, i + t.getWcet(1)) && (cpu1.maxCoreInterval(i, i + t.getWcet(1)) < activeCore)) {
                        cpu1.SetTaskOnCore(t.getName() + " CR" + (name_number+k), mappedCore, i, i + t.getWcet(0) - 1);
                        cpu1.SetTaskOnCore(t.getName() + " CO" + (name_number+k), mappedCore, i+t.getWcet(0), i + t.getWcet(1) - 1);
                        scheduled =true;
                        break;
                    }
                }
                if(!scheduled)
                    throw new Exception("Infeasible!");

            }
            t.setDone(true);
            if (VERBOSE)
                System.out.println(" SCH: [ "+t.getName() + " ]  ");


        } while (t != null);

        cpu1.debug("AnsariTest");
    }


    //Main Scheduling of System
    public void mainScheduling() throws Exception {
        Vertex t;
        cpu = new CPU(deadline, n_core, dag, VERBOSE);

        do {
            t = dag.getNodebyName(get_task(true));
            if(t==null) break;
            int startTime = 0;
            for (Edge e : t.getRcvEdges()) {
                if(ov_tasks.contains(e.getSrc().getName()+" CR" + (int) (n - 1))) {
                    if (cpu.getEndTimeTask(e.getSrc().getName() + " CO" + (int) (n - 1)) > startTime) {
                        startTime = cpu.getEndTimeTask(e.getSrc().getName()) + 1;
                    }
                }else{
                    if (cpu.getEndTimeTask(e.getSrc().getName() + " CR" + (int) (n - 1)) > startTime) {
                        startTime = cpu.getEndTimeTask(e.getSrc().getName()) + 1;
                    }
                }
            }

            if(t.isHighCr()) {
                for (int k = 0; k < (int) ceil(n / 2); k++) {
                    int mappedCore = cpu.worstFit();
                    boolean scheduled = false;
                    if(ov_tasks.contains(t.getName()+" CR" + (int) (k))) {
                        for (int i = startTime; i < deadline - t.getWcet(1); i++) {
                            if (cpu.CheckTimeSlot(mappedCore, i, i + t.getWcet(1)) && (cpu.maxCoreInterval(i, i + t.getWcet(1)) < activeCore)) {
                                cpu.SetTaskOnCore(t.getName() + " CR" + k, mappedCore, i, i + t.getWcet(0) - 1);
                                cpu.SetTaskOnCore(t.getName() + " CO" + k, mappedCore, i + t.getWcet(0), i + t.getWcet(1) - 1);
                                if (VERBOSE)
                                    System.out.println(">>> " + (t.getName() + " CR" + k) + " S: " + i + " E: " + (i + t.getWcet(0) - 1));
                                scheduled = true;
                                break;
                            }
                        }

                    }else{
                        for (int i = startTime; i < deadline - t.getWcet(0); i++) {
                            if (cpu.CheckTimeSlot(mappedCore, i, i + t.getWcet(0)) && (cpu.maxCoreInterval(i, i + t.getWcet(0)) < activeCore)) {
                                cpu.SetTaskOnCore(t.getName() + " CR" + k, mappedCore, i, i + t.getWcet(0) - 1);
                                if (VERBOSE)
                                    System.out.println(">>> " + (t.getName() + " CR" + k) + " S: " + i + " E: " + (i + t.getWcet(0) - 1));
                                scheduled = true;
                                break;
                            }
                        }
                    }
                    if (!scheduled)
                        throw new Exception("Infeasible!");
                }
                startTime = cpu.getEndTimeTask(t.getName() + " CO" + (int) (ceil(n / 2) - 1)) + 1;
                int name_number = (int) ceil(n / 2);
                for (int k = 0; k < (int) floor(n / 2); k++) {
                    int mappedCore = cpu.worstFit();
                    boolean scheduled = false;
                    if(ov_tasks.contains(t.getName()+" CR" + (int) (k))) {
                        for (int i = startTime; i < deadline - t.getWcet(1); i++) {
                            if (cpu.CheckTimeSlot(mappedCore, i, i + t.getWcet(1)) && (cpu.maxCoreInterval(i, i + t.getWcet(1)) < activeCore)) {
                                cpu.SetTaskOnCore(t.getName() + " CR" + (name_number + k), mappedCore, i, i + t.getWcet(0) - 1);
                                cpu.SetTaskOnCore(t.getName() + " CO" + (name_number + k), mappedCore, i + t.getWcet(0), i + t.getWcet(1) - 1);
                                if (VERBOSE)
                                    System.out.println(">>> " + (t.getName() + " CR" + (name_number + k)) + " S: " + i + " E: " + (i + t.getWcet(0) - 1));
                                scheduled = true;
                                break;
                            }
                        }
                    }else{
                        for (int i = startTime; i < deadline - t.getWcet(0); i++) {
                            if (cpu.CheckTimeSlot(mappedCore, i, i + t.getWcet(0)) && (cpu.maxCoreInterval(i, i + t.getWcet(0)) < activeCore)) {
                                cpu.SetTaskOnCore(t.getName() + " CR" + (name_number + k), mappedCore, i, i + t.getWcet(0) - 1);
                                if (VERBOSE)
                                    System.out.println(">>> " + (t.getName() + " CR" + (name_number + k)) + " S: " + i + " E: " + (i + t.getWcet(0) - 1));
                                scheduled = true;
                                break;
                            }
                        }
                    }
                    if (!scheduled)
                        throw new Exception("Infeasible!");

                }
                t.setDone(true);
                if (VERBOSE)
                    System.out.println(" SCH: [ " + t.getName() + " ]  ");

            }else{
                //LO Critical Tasks Scheduling.
                int mappedCore = cpu.worstFit();
                boolean scheduled = false;
                for (int i = startTime; i < deadline - t.getWcet(0); i++) {
                    if (cpu.CheckTimeSlot(mappedCore, i, i + t.getWcet(0)) && (cpu.maxCoreInterval(i, i + t.getWcet(0)) < activeCore)) {
                        cpu.SetTaskOnCore(t.getName() + " CR" + (n-1), mappedCore, i, i + t.getWcet(0) - 1);
                        if (VERBOSE)
                            System.out.println(">>> LO: " + (t.getName() + " CR" + (n-1)) + " S: " + i + " E: " + (i + t.getWcet(0) - 1));
                        scheduled = true;
                        break;
                    }
                }
                t.setDone(true);
                if (!scheduled)
                    throw new Exception("Infeasible!");

            }


        } while (t != null);
        cpu.debug("AnsariMain");
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


    //Random Select Overrun tasks
    public Set<String> SelectOverrunTasks(Vertex v[]) {
        Set<String> ov_tasks = new HashSet<String>();
        Set<String> temp = new HashSet<String>();

        for (Vertex a : v) {
            for (int i = 1; i <= a.getReplica(); i++) {
                temp.add(a.getName() + " CR" + i);
            }
        }

        n_overrun = (int) (overrun_percent * temp.size());
        String[] temp2 = temp.stream().toArray(String[]::new).clone();

        //Select Tasks
        Random ov = new Random();
        int o;
        for (int i = 0; i < n_overrun; i++) {
            do {
                o = ov.nextInt(temp2.length);
            } while (ov_tasks.contains(temp2[o]));
            if (VERBOSE) System.out.println("Overrun Task => " + temp2[o]);
            ov_tasks.add(temp2[o]);
        }
        return ov_tasks;
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

    public CPU getCpu() {
        return cpu;
    }


    public McDAG getDag() {
        return dag;
    }


}
