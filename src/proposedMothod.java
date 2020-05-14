import java.util.*;

/*******************************************************************************
 * Copyright © 2020 Pourya Gohari
 * Written by Pourya Gohari (Email: gohary@ce.sharif.edu)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
public class proposedMothod {

    Vertex v[];
    Set<Vertex> HIv;
    Vertex sorted_tasks[];

    //Deadline
    int deadline;
    //Number of CPU Core
    int n_core;
    //Number of Redundancy
    double n;
    //DAG
    McDAG dag;
    CPU cpu;
    String xml_name;

    int max_freq_cores = 1200;

    double overrun_percent;
    int n_overrun;
    Set<String> ov_tasks;

    Set<Integer> balancingPoint;
    Integer[] bps;


    boolean VERBOSE = false;

    public proposedMothod(int deadline, int n_core, McDAG dag, String xml_name, double overrun_percent, boolean VERBOSE) {
        this.deadline = deadline;
        this.n_core = n_core;
        this.dag = dag;
        this.xml_name = xml_name;
        this.overrun_percent = overrun_percent;
        this.VERBOSE = VERBOSE;
        balancingPoint = new HashSet<Integer>();
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

        bps =new Integer[balancingPoint.size()];
        balancingPoint.toArray(bps);
        Arrays.sort(bps);
        if (VERBOSE) {
            for (int bp : bps) {
                System.out.println("--> " + bp);
            }
        }
    }

    //Check Feasibility of System
    public void feasibility() throws Exception {
        String Task[];
        CPU cpu1 = new CPU(deadline, n_core, dag, VERBOSE);
        do {
            Task = get_tasks(false);
            if (VERBOSE) System.out.println("-------------");
            int startTime = (cpu1.Endtime(-1) == 0) ? 0 : cpu1.Endtime(-1) + 1;
            for (int i = 0; i < Task.length; i++) {
                if (Task[i] == null) continue;
                int blockSize = dag.getNodebyName(Task[0]).getWcet(0);

                Vertex v = dag.getNodebyName(Task[i]);
                cpu1.SetTaskOnCore(v.getName() + " CR" + (v.getScheduled() + 1), i, startTime + (blockSize - v.getWcet(0)), startTime + blockSize - 1);
                cpu1.SetTaskOnCore(v.getName() + " CO" + (v.getScheduled() + 1), i, startTime + blockSize, startTime + blockSize + v.getWcet(1) - 1);
                v.setScheduled(v.getScheduled() + 1);

                if (VERBOSE)
                    System.out.println(v.getName() + " SCH: [ " + v.getScheduled() + " / " + v.getReplica() + " ]  " + v.isDone());
            }
        } while (Task[0] != null);

        cpu1.debug("TEST");
    }

    //Main Scheduling of System
    public void mainScheduling() throws Exception {
        //Make array for tasks in specific blocks
        String t[] = new String[n_core];
        String Task[];
        cpu = new CPU(deadline, n_core, dag, n, max_freq_cores, VERBOSE);
        do {
            Task = get_tasks(true);
            if (VERBOSE) System.out.println("------M------");
            int startTime = (cpu.Endtime(-1) == 0) ? 0 : cpu.Endtime(-1) + 1;
            for (int i = 0; i < Task.length; i++) {
                if (Task[i] == null) continue;
                int blockSize = dag.getNodebyName(Task[0]).getWcet(0);

                Vertex v = dag.getNodebyName(Task[i]);
                cpu.SetTaskOnCore(v.getName() + " CR" + (v.getScheduled() + 1), i, startTime + (blockSize - v.getWcet(0)), startTime + blockSize - 1);
                if (ov_tasks.contains(v.getName() + " CR" + (v.getScheduled() + 1)))
                    cpu.SetTaskOnCore(v.getName() + " CO" + (v.getScheduled() + 1), i, startTime + blockSize, startTime + blockSize + v.getWcet(1) - 1);
                v.setScheduled(v.getScheduled() + 1);

                if (VERBOSE)
                    System.out.println(v.getName() + " SCH: [ " + v.getScheduled() + " / " + v.getReplica() + " ]  " + v.isDone());
            }
            //Add end of this Block to balancing points set
            balancingPoint.add(cpu.Endtime(-1)+1);
        } while (Task[0] != null);


        cpu.debug("main");
    }


    //Get Tasks for every blocks
    public String[] get_tasks(boolean LO) {
        //Make array for tasks in specific blocks
        String t[] = new String[n_core];
        Vertex s = null;
        int k = 0;
        do {
            s = null;
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
                    //Check for number of a task in the block
                else if (countOccurrences(t, a.getName()) + a.getScheduled() + 1 > a.getReplica()) continue;
                else {
                    s = a;
                    t[k] = s.getName();
                    k++;
                    if (k == 4) break;
                }
            }
        } while (k != 4 && s != null);
        return t;
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

    public int countOccurrences(String arr[], String x) {
        int n = 0;
        for (int i = 0; i < arr.length; i++) {
            if (arr[i] == x) n++;
        }
        return n;
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

    // QoS Calculator
    public double QoS() {
        double QoS = 0;
        for (int i = 0; i < sorted_tasks.length; i++) {
            if (!sorted_tasks[i].isHighCr() && sorted_tasks[i].isRun()) QoS++;
        }
        QoS = QoS / (sorted_tasks.length - dag.getNodes_HI().size());
        return QoS;
    }

    //Reset condition of scheduled tasks
    public void reset_schedule() {
        for (Vertex a : sorted_tasks) {
            a.setDone(false);
            a.setScheduled(0);
        }
    }

    public CPU getCpu() {
        return cpu;
    }

    public Integer[] getBps() {
        return bps;
    }

    public McDAG getDag() {
        return dag;
    }
}
