/*******************************************************************************
 * Copyright (c) 2020 Pourya Gohari
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

import java.io.*;
import java.util.Arrays;

import static java.lang.Math.ceil;

@SuppressWarnings("ALL")
public class CPU {
    //Core of CPU   [#Core] [#Time]
    private String[][] core;
    //Power Trace of Cores
    private double[][] power;
    //Deadline of System
    private int deadline;
    //Number of Core in CPU
    private int n_Cores;
    //MC-DAG
    McDAG mcDAG;

    //VERBOSE
    boolean VERBOSE = false;

    //Idle Power
    double idle_power = 3;


    //Max. Freq.
    int max_freq = 1200;

    //Location of Power Trace
    String pathSeparator = File.separator;
    String location = "Benchmark" + pathSeparator;
    //Number of Redundancy
    double n = 3;

    int max_freq_cores = 1;

    public CPU(int deadline, int n_Cores, McDAG mcDAG) {
        this.deadline = deadline;
        this.n_Cores = n_Cores;
        this.mcDAG = mcDAG;

        core = new String[n_Cores][deadline];
        power = new double[n_Cores][deadline];
        for (int i = 0; i < n_Cores; i++) {
            Arrays.fill(power[i], idle_power);
        }
    }

    public CPU(int deadline, int n_Cores, McDAG mcDAG, boolean VERBOSE) {
        this.deadline = deadline;
        this.n_Cores = n_Cores;
        this.mcDAG = mcDAG;
        this.VERBOSE = VERBOSE;
        core = new String[n_Cores][deadline];
        power = new double[n_Cores][deadline];
        for (int i = 0; i < n_Cores; i++) {
            Arrays.fill(power[i], idle_power);
        }
    }

    public CPU(int deadline, int n_Cores, McDAG mcDAG, double n, int max_freq_cores, boolean VERBOSE) {
        this.deadline = deadline;
        this.n_Cores = n_Cores;
        this.mcDAG = mcDAG;
        this.n = n;
        this.VERBOSE = VERBOSE;
        this.max_freq_cores = max_freq_cores;
        core = new String[n_Cores][deadline];
        power = new double[n_Cores][deadline];
        for (int i = 0; i < n_Cores; i++) {
            Arrays.fill(power[i], idle_power);
        }
    }

    //GET Running Task in specific Time
    public String getRunningTask(int Core, int Time) {
        if (core[Core][Time] != null) {
            if (core[Core][Time].contains(" CR")) {
                return core[Core][Time].split(" CR")[0];
            }else if(core[Core][Time].contains(" R")){
                return core[Core][Time].split(" R")[0];
            }else if(core[Core][Time].contains(" CO")){
                return core[Core][Time].split(" CO")[0];
            }else {
                return core[Core][Time].split(" OV")[0];
            }
        } else return null;
//        return (core[Core][Time] == null) ? null : core[Core][Time].split(" R")[0];
//        System.out.println(core[Core][Time].split(" R")[0]);
//        return core[Core][Time];
    }

    //GET Running Task in specific Time
    public String getRunningTaskWithReplica(int Core, int Time) {
        // System.out.println("GET Running Task in specific Time "+Core + "   "+ Time);
        return core[Core][Time];
    }

    //If Time slot was free return true;
    public boolean CheckTimeSlot(int Core, int Start, int End) {
//        System.out.println("Check Time: "+Core+"  "+Start+"    "+End);
        if (Core > (n_Cores - 1)) return false;
        if (Start > End) return false;
        if (Start < 0 || End >= deadline || Start >= deadline) return false;
        for (int i = Start; i <= End; i++) {
//           System.out.println("Check Time: "+Core+"  "+i);
            if (core[Core][i] != null) return false;
        }
        return true;
    }

    //Set Task to Core
    public void SetTaskOnCore(String Task, int Core, int Start, int End) throws Exception {
        // System.out.println(Task+"  "+ Start+"  "+End);
        try {
            for (int i = Start; i <= End; i++) {
                core[Core][i] = Task;
            }
        } catch (Exception e) {
            if (VERBOSE) System.err.println(Task + "  ⚠ ⚠ Infeasible!");
            if (VERBOSE) e.printStackTrace();
            throw new Exception("Infeasible!");
            //System.exit(1);
        }

        // For Not Mapping Power for Safe Start Time Class
        StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
        if (!stackTraceElements[2].getClassName().equals("Safe_Start_Time") && !stackTraceElements[2].getMethodName().equals("feasibility")) {
            try {
                this.setPower(Task, Start, Core);
            } catch (IOException e) {
                if (VERBOSE) e.printStackTrace();
            }
        }
    }

    // A function For Mapping Power Of Benchmarks
    public void setPower(String Task, int Start, int Core) throws IOException {
        String t = Task.split(" ")[0];

        int m = Integer.parseInt(Task.split(" ")[1].replaceAll("\\D", ""));
        Vertex v = mcDAG.getNodebyName(t);
        if (Task.contains("CR")) {
            String LO = v.getLO_name();
            Double r[] = new Double[v.getWcet(0)];
            BufferedReader reader;
            File file = new File(location + max_freq + "//" + LO + ".txt");
            reader = new BufferedReader(new FileReader(file));
            int i = 0;
            String line = reader.readLine();
            while (line != null) {
                r[i] = Double.parseDouble(line);
                line = reader.readLine();
                i++;
            }
            int l = 0;
            for (int k = Start; k < Start + r.length; k++) {
                power[Core][k] = r[l];
                l++;
            }
        } else if (Task.contains("CO")) {
            String HI = v.getHI_name();
            //System.out.println(v.getWcet(1)-v.getWcet(0));
            Double r[] = new Double[v.getWcet(1) - v.getWcet(0)];
            BufferedReader reader;
            File file = new File(location + max_freq + "//" + HI + ".txt");
            reader = new BufferedReader(new FileReader(file));
            int i = 0;
            String line = reader.readLine();
            while (line != null) {
                r[i] = Double.parseDouble(line);
                line = reader.readLine();
                i++;
            }
            int l = 0;
            for (int k = Start; k < Start + r.length; k++) {
                power[Core][k] = r[l];
                l++;
            }

        } else if (Task.contains("F") || Task.contains("O")) {
            //Faulty Task Power
            if (Task.contains("F")) {
                String LO = v.getLO_name();
                Double r[] = new Double[v.getWcet(0)];

                BufferedReader reader;
                File file = new File(location + max_freq + "//" + LO + ".txt");
                reader = new BufferedReader(new FileReader(file));
                int i = 0;
                String line = reader.readLine();
                while (line != null) {
                    r[i] = Double.parseDouble(line);
                    line = reader.readLine();
                    i++;
                }
                int l = 0;
                for (int k = Start; k < Start + r.length; k++) {
                    power[Core][k] = r[l];
                    l++;
                }
//                System.out.println("P START  :: "+Start+"   "+(Start+r.length));
//                System.out.println("<POWER> "+v.getName()+"  "+v.getLO_name()+"   "+v.getWcet(0));
//                for (int j = 0; j < r.length ; j++) {
//                    System.out.print(r[j]+",");
//                }
//                System.out.println();
                //Overrun Power
            } else if (Task.contains("O")) {
                // For Not Mapping Power for Safe Start Time Class
                String HI = v.getHI_name();
                StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
                //System.out.println(stackTraceElements[2].getClassName());
                if (stackTraceElements[2].getClassName().equals("secondApproach")) {
                    if (m <= (ceil(n / 2) - 1)) {
                        Double r[] = new Double[v.getRunningTimeHI(max_freq, v.getMin_freq()) - v.getRunningTimeLO(max_freq, v.getMin_freq())];
                        BufferedReader reader;
                        File file = new File(location + v.getMin_freq() + "//" + HI + ".txt");
                        reader = new BufferedReader(new FileReader(file));
                        int i = 0;
                        String line = reader.readLine();
                        while (line != null) {
                            r[i] = Double.parseDouble(line);
                            line = reader.readLine();
                            i++;
                        }
                        int l = 0;
                        for (int k = Start; k < Start + r.length; k++) {
                            power[Core][k] = r[l];
                            l++;
                        }
                    } else {
                        Double r[] = new Double[v.getRunningTimeHI(max_freq, max_freq) - v.getRunningTimeLO(max_freq, max_freq)];
                        BufferedReader reader;
                        File file = new File(location + max_freq + "//" + HI + ".txt");
                        reader = new BufferedReader(new FileReader(file));
                        int i = 0;
                        String line = reader.readLine();
                        while (line != null) {
                            r[i] = Double.parseDouble(line);
                            line = reader.readLine();
                            i++;
                        }
                        int l = 0;
                        for (int k = Start; k < Start + r.length; k++) {
                            power[Core][k] = r[l];
                            l++;
                        }
                    }

                } else {
                    //System.out.println(v.getName()+" <> "+HI+"    ### "+(v.getWcet(1)-v.getWcet(0)));
                    Double r[] = new Double[v.getWcet(1) - v.getWcet(0)];
                    BufferedReader reader;
                    File file = new File(location + max_freq + "//" + HI + ".txt");
                    reader = new BufferedReader(new FileReader(file));
                    int i = 0;
                    String line = reader.readLine();
                    while (line != null) {
                        r[i] = Double.parseDouble(line);
                        line = reader.readLine();
                        i++;
                    }
                    int l = 0;
                    for (int k = Start; k < Start + r.length; k++) {
                        power[Core][k] = r[l];
                        l++;
                    }
                }
//                System.out.println("P START  :: "+Start+"   "+(Start+r.length));
//                System.out.println("<POWER> "+v.getName()+"  "+v.getHI_name()+"   "+(v.getWcet(1)-v.getWcet(0)));
//                for (int j = 0; j < r.length ; j++) {
//                    System.out.print(r[j]+",");
//                }
                // System.out.println();


            }

        } else {
            if (m <= (ceil(n / 2) - 1)) {
                String LO = v.getLO_name();
                Double r[] = new Double[(v.getWcet(0) * max_freq / v.getMin_freq())];
                BufferedReader reader;
                File file = new File(location + v.getMin_freq() + "//" + LO + ".txt");
                reader = new BufferedReader(new FileReader(file));
                int i = 0;
                String line = reader.readLine();
//            System.out.println("P START  :: "+Start+"   "+(Start+r.length));
//            System.out.println("<POWER> "+v.getName()+"  "+v.getLO_name()+"   "+v.getWcet(0)+"    "+v.getMin_freq());
                while (line != null) {
                    r[i] = Double.parseDouble(line);
                    line = reader.readLine();
                    i++;
                }
                int l = 0;
                for (int k = Start; k < Start + r.length; k++) {
                    power[Core][k] = r[l];
                    l++;
                }
            } else {
                String LO = v.getLO_name();
                Double r[] = new Double[v.getWcet(0)];
                BufferedReader reader;
                File file = new File(location + max_freq + "//" + LO + ".txt");
                reader = new BufferedReader(new FileReader(file));
                int i = 0;
                String line = reader.readLine();
                while (line != null) {
                    r[i] = Double.parseDouble(line);
                    line = reader.readLine();
                    i++;
                }
                int l = 0;
                for (int k = Start; k < Start + r.length; k++) {
                    power[Core][k] = r[l];
                    l++;
                }
            }

        }
    }

    //Return Max. Core Can Use in specific Time
    public int max_core(int Time) {
        String t;
        int max = n_Cores;
        for (int i = 0; i < n_Cores; i++) {
            if (getRunningTask(i, Time) != null) {
                int m = Integer.parseInt(core[i][Time].split(" ")[1].replaceAll("\\D", ""));
                if (m <= (ceil(n / 2) - 1)) {
                    t = getRunningTask(i, Time);
//                System.out.println(mcDAG.getNodebyName(t).getTSP_Active());
                    if (mcDAG.getNodebyName(t).getTSP_Active() < max) max = mcDAG.getNodebyName(t).getTSP_Active();
                } else {
                    if (max_freq_cores < max) max = max_freq_cores;
                }
            }
        }
        return max;
    }


    public void setN_Cores(int n_Cores) {
        this.n_Cores = n_Cores;
    }

    public void setDeadline(int deadline) {
        this.deadline = deadline;
    }

    public int getN_Cores() {
        return n_Cores;
    }

    public int getDeadline() {
        return deadline;
    }

    public void instatiate_power(double min_power) {
        for (int i = 0; i < deadline; i++) {
            for (int j = 0; j < n_Cores; j++) {
                power[j][i] = min_power;
            }

        }
    }

    //Return Running Tasks
    public String[] get_Running_Tasks(int Time) {
        String[] a = new String[n_Cores];
        for (int i = 0; i < n_Cores; i++) {
            if (core[i][Time] != null)
                a[i] = core[i][Time].split(" ")[0];
        }
        return a;
    }

    //Return Number Of Running Tasks
    public int numberOfRunningTasks(int Time) {
        int r = 0;
        for (int i = 0; i < n_Cores; i++) {
            if (core[i][Time] != null) r++;
        }
        return r;
    }

    //Return Number Of Running Tasks
    public int numberOfRunningTasksInterval(int Start, int End) {
        int r = 0;
        for (int i = Start; i <= End; i++) {
            if (this.numberOfRunningTasks(i) > r) r = this.numberOfRunningTasks(i);
        }
        return r;
    }

    //Return Max Core Can Use in specific Interval  [Start Time , End Time]
    public int maxCoreInterval(int Start, int End) {
        int max = n_Cores;
        for (int i = Start; i <= End; i++) {
            if (this.max_core(i) < max) max = this.max_core(i);
        }

        return max;
    }

    public int getSafeTime(String task) {
        int SST = deadline;
//        System.out.println("++++>>"+task);
        for (int i = 0; i < n_Cores; i++) {
            for (int j = 0; j < deadline; j++) {
                // System.out.println(i+"   "+j+"  "+core[i][j]);
                if (core[i][j] != null) {
                    if (core[i][j].startsWith(task) && j < SST) {
                        SST = j;
                    }
                }
            }
            //if(Arrays.asList(core[i]).indexOf(task) < SST && Arrays.asList(core[i]).indexOf(task)!= -1) SST= Arrays.asList(core[i]).indexOf(task);
        }
        return SST;
    }

    //Write Scheduling In File for Debugging
    public void debug(String Filename) throws IOException {
        BufferedWriter outputWriter = null;
        outputWriter = new BufferedWriter(new FileWriter(Filename + ".csv"));
        for (int i = 0; i < getN_Cores(); i++) {
            for (int j = 0; j < getDeadline(); j++) {
                outputWriter.write(core[i][j] + ",");
            }
            outputWriter.write("\n");
        }
        outputWriter.flush();
        outputWriter.close();
    }

    public void Task_Shifter(int shiftTime, int amount) throws Exception {
        // System.out.println("TASK SHIFTER  "+ shiftTime+"  > > "+amount);
        for (int i = 0; i < n_Cores; i++) {
            for (int j = Endtime(i); j > (shiftTime); j--) {
                try {
                    this.SetTask(i, j + amount, this.getRunningTaskWithReplica(i, j));
                    power[i][j + amount] = power[i][j];
                } catch (Exception ex) {
                    if (VERBOSE) System.err.println(this.getRunningTaskWithReplica(i, j) + "  ⚠ ⚠ Infeasible!");
                    throw new Exception("Infeasible!");
                    // System.exit(1);
                }
            }
            for (int j = shiftTime + 1; j < shiftTime + amount + 1; j++) {
                this.SetTask(i, j, null);
                power[i][j] = 0.5;
            }
        }
    }

    //a function for determine end time in each core
    // if core == -1 then it get biggest end time of cores
    public int Endtime(int core) {
        if (core == -1) {
            int temp = 0;
            for (int i = 0; i < n_Cores; i++) {
                for (int j = deadline - 1; j >= 0; j--) {
                    if (this.getRunningTask(i, j) != null) {
                        if (j > temp) temp = j;
                    }
                }
            }
            return temp;

        } else {
            for (int i = deadline - 1; i >= 0; i--) {
                if (this.getRunningTask(core, i) != null) {
                    return i;
                }
            }
        }
        return 0;
    }


    public void SetTask(int core_number, int time, String task) throws Exception {
        try {
            core[core_number][time] = task;

        } catch (Exception e) {
            //System.err.println(task+"  ⚠ ⚠ Infeasible!");
            if (VERBOSE) e.printStackTrace();
            //System.out.println("Core  "+core_number+"  Time "+time);
            throw new Exception("Infeasible!");
            //System.exit(1);
        }
    }

    //Return Start Time of a Specific Replica of Tasks
    public int getStartTime(String Task) {
        int s = deadline;
        for (int i = 0; i < n_Cores; i++) {
            if (Arrays.asList(core[i]).indexOf(Task) != -1)
                if (Arrays.asList(core[i]).indexOf(Task) < s)
                    s = Arrays.asList(core[i]).indexOf(Task);
        }
        return s;

    }


    //Return End Time of a Specific Replica of Tasks
    public int getEndTimeTask(String Task) {
        int e = -1;
//        System.out.println(Task);
        for (int i = 0; i < n_Cores; i++) {
            if (Arrays.asList(core[i]).lastIndexOf(Task) != -1) {
                if (Arrays.asList(core[i]).lastIndexOf(Task) > e) {
                    e = Arrays.asList(core[i]).lastIndexOf(Task);
                }
            }
        }
        //   System.out.println("   >>> "+e);
        return e;
    }

    public void Save_Power(String mFolder, String Folder, String Filename) throws IOException {
        BufferedWriter outputWriter = null;
        File newFolder2 = new File(mFolder);
        newFolder2.mkdir();
        File newFolder = new File(mFolder + "//" + Folder);
        newFolder.mkdir();
        for (int i = 0; i < getN_Cores(); i++) {
            outputWriter = new BufferedWriter(new FileWriter(mFolder + "//" + Folder + "//" + Filename + "_Core_" + i + ".txt"));
            for (int j = 0; j < getDeadline(); j++) {
                outputWriter.write(power[i][j] + "\n");
            }
            ;
            outputWriter.flush();
            outputWriter.close();

        }
    }

    public double[] power_results() {
        double p[] = new double[2];
        p[0] = Avg_power();
        p[1] = Peak_power();
        if (VERBOSE) System.out.println("Avg. Power= " + Avg_power());
        if (VERBOSE) System.out.println("Peak Power= " + Peak_power());
        return p;
    }

    //Calculate Average Power Consumption of CPU
    public double Avg_power() {
        double p = 0;
        for (int i = 0; i < deadline; i++) {
            for (int j = 0; j < n_Cores; j++) {
                p += power[j][i];
            }
        }
        return (p / deadline);
    }

    //Calculate Peak Power of CPU
    public double Peak_power() {
        double max = 0;
        double p = 0;
        for (int i = 0; i < deadline; i++) {
            p = 0;
            for (int j = 0; j < n_Cores; j++) {
                p += power[j][i];
            }
            if (p > max) max = p;
        }
        return max;
    }

    //delete a specefic task from scheduling
    public void remove_task(String task) {
        for (int i = 0; i < deadline; i++) {
            for (int j = 0; j < n_Cores; j++) {
                if (core[j][i] != null) {
                    if (core[j][i].contains(task)) {
                        core[j][i] = null;
                        power[j][i] = idle_power;
                    }
                }
            }
        }
    }

    //Get slack time betwwen two time
    public int get_slack(int core,int start,int end){
        int slack=0;
        for (int i = start; i < end; i++) {
            if(this.core[core][i]==null) slack++;
        }
        return slack;
    }

    //Get power consumption of a specific core in a time
    public double get_power(int core,int time){
        return power[core][time];
    }


}
