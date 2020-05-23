import java.util.*;

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


    public Ansari2019(int deadline, int n_core, McDAG dag, String xml_name, double overrun_percent, boolean VERBOSE) {
        this.deadline = deadline;
        this.n_core = n_core;
        this.dag = dag;
        this.xml_name = xml_name;
        this.overrun_percent = overrun_percent;
        this.VERBOSE = VERBOSE;
        activeCore = n_core / 2;
    }

    public void start() throws Exception {

    }

    //Check Feasibility of System
    public void feasibility() throws Exception {
        Vertex t;
        CPU cpu1 = new CPU(deadline, n_core, dag, VERBOSE);
        do {
            t = dag.getNodebyName(get_task(false));
            int startTime = 0;
            for (Edge e : t.getRcvEdges()) {
                if (cpu1.getEndTimeTask(e.getSrc().getName() + " CO" + (int) (n - 1)) > startTime) {
                    startTime = cpu1.getEndTimeTask(e.getSrc().getName()) + 1;
                }
            }

            for (int k = 0; k < (int) floor(n / 2); k++) {
                int mappedCore = cpu1.worstFit();
                for (int i = startTime; i < deadline; i++) {
                    if (cpu.CheckTimeSlot(mappedCore, i, i + t.getWcet(1)) && (cpu.maxCoreInterval(i, i + t.getWcet(1)) < activeCore)) {
                        cpu.SetTaskOnCore(t.getName() + " CR" + k, mappedCore, i, i + t.getWcet(0) - 1);
                        cpu.SetTaskOnCore(t.getName() + " CO" + k, mappedCore, i+t.getWcet(0), i + t.getWcet(1) - 1);
                    }
                }
            }
            //TODO Add Scheduling Faulty phase

        } while (t != null);
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


}
