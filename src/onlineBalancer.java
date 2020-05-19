import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Scanner;

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
public class onlineBalancer {
    //Balancing point array
    Integer[] bps;
    //CPU of System
    CPU cpu;
    //DaG
    McDAG dag;
    //VERBOSE
    boolean VERBOSE = false;

    String pathSeparator = File.separator;

    //HotSpot location and information
    String hotspot_path = "HotSpot" + pathSeparator + "hotspot";
    String hotspot_config = "HotSpot" + pathSeparator + "configs" + pathSeparator;
    String floorplan = "HotSpot" + pathSeparator + "floorplans" + pathSeparator;
    String powertrace = "HotSpot" + pathSeparator + "powertrace" + pathSeparator;
    String thermaltrace = "HotSpot" + pathSeparator + "thermaltrace" + pathSeparator + "thermal.ttrace";

    public onlineBalancer(Integer[] bps, CPU cpu, McDAG dag, boolean VERBOSE) {
        this.bps = bps;
        this.cpu = cpu;
        this.dag = dag;
        this.VERBOSE = VERBOSE;
    }

    public void run() {
        HotSpot hotSpot = new HotSpot(hotspot_path, VERBOSE);
        HS_input_creator hs_input_creator = new HS_input_creator(cpu);


        for (int i = 0; i < bps.length - 1; i++) {
            try {
                hs_input_creator.Save("HotSpot", "powertrace", "Alpha" + cpu.getN_Cores() + ".ptrace", bps[i]);
            } catch (IOException e) {
                e.printStackTrace();
            }

            hotspot_config = "HotSpot" + pathSeparator + "configs" + pathSeparator;
            floorplan = "HotSpot" + pathSeparator + "floorplans" + pathSeparator;
            powertrace = "HotSpot" + pathSeparator + "powertrace" + pathSeparator;

            hotspot_config += "hotspot_" + cpu.getN_Cores() + ".config";
            floorplan += "Alpha" + cpu.getN_Cores() + ".flp";
            powertrace += "Alpha" + cpu.getN_Cores() + ".ptrace";
            hotSpot.run(hotspot_config, floorplan, powertrace, thermaltrace);

            boolean sw[]=new boolean[cpu.getN_Cores()];
            Arrays.fill(sw, true);
            double pre[]=new double[cpu.getN_Cores()];

            for (int j = 0; j < cpu.getN_Cores(); j++) {
                pre[j]=predict(j, bps[i], bps[i + 1] - 1, get_cur_temp(bps[i])[j]);
            }

            for (int j = 0; j < cpu.getN_Cores()/2; j++) {
                int MinIndex = getMinIndex(pre, sw);
                int MaxIndex = getMaxIndex(pre, sw);
                sw[MaxIndex]=false;
                sw[MinIndex]=false;
                cpu.remap(MaxIndex, MinIndex, bps[i], bps[i + 1]);
            }
        }

    }

    //Calculate predict value for thermal balancing
    public double predict(int core, int start, int end, double t_cur) {
        double t = 0;
        String task = cpu.getRunningTask(core, end);
        double t_inf;
        int slack;
        int WC_HI;


        if (task == null) t_inf = 45.0;
        else t_inf = dag.getNodebyName(task).getT_inf();
        slack = cpu.get_slack(core, start, end);
        if (task == null) WC_HI = 0;
        else WC_HI = dag.getNodebyName(task).getbigWCET();

        //Predict formula
        if (t_cur > t_inf) t = (slack + 1) / ((Math.abs(t_cur - t_inf) + 1) * (WC_HI + 1));
        else t = (-1) * (slack + 1) / ((Math.abs(t_cur - t_inf) + 1) * (WC_HI + 1));

        return t;
    }

    public double[] get_cur_temp(int time) {
        String mFolder = "HotSpot";
        String sFolder = "thermaltrace";
        String filename = "thermal.ttrace";
        File thermalFile = null;
        double value[] = new double[cpu.getN_Cores()];
        try {
            thermalFile = new File(mFolder + pathSeparator + sFolder + pathSeparator + filename);
            Scanner Reader = new Scanner(thermalFile);
            Reader.nextLine();
            for (int j = 0; j < cpu.Endtime(-1); j++) {
                String data = Reader.nextLine();
                if (j == time) {
                    String Sdatavalue[] = data.split("\t");
                    for (int i = 0; i < cpu.getN_Cores(); i++) {
                        value[i] = Double.parseDouble(Sdatavalue[i]);
                    }
                    break;
                }
            }
            Reader.close();

        } catch (FileNotFoundException e) {
            if (VERBOSE) {
                System.out.println("An error occurred in Reading Thermal Trace File.");
                System.out.println("Path: " + thermalFile.getAbsolutePath());
                e.printStackTrace();
            }
        }
        return value;
    }

    public double[] balanceCalculator() {
        //Temperature Results [0] Avg. Diff. [1] Max. Diff. [2] Max. Temp. [3] Avg. Temp.
        double temp[]= new double[4];
        double Max=0;
        double Avg=0;

        hotspot_config = "HotSpot" + pathSeparator + "configs" + pathSeparator;
        floorplan = "HotSpot" + pathSeparator + "floorplans" + pathSeparator;
        powertrace = "HotSpot" + pathSeparator + "powertrace" + pathSeparator;
        HotSpot hotSpot = new HotSpot(hotspot_path, VERBOSE);
        HS_input_creator hs_input_creator = new HS_input_creator(cpu);
        try {
            hs_input_creator.Save("HotSpot", "powertrace", "Alpha" + cpu.getN_Cores() + ".ptrace", cpu.Endtime(-1));
        } catch (IOException e) {
            e.printStackTrace();
        }

        hotspot_config += "hotspot_" + cpu.getN_Cores() + ".config";
        floorplan += "Alpha" + cpu.getN_Cores() + ".flp";
        powertrace += "Alpha" + cpu.getN_Cores() + ".ptrace";
        hotSpot.run(hotspot_config, floorplan, powertrace, thermaltrace);

        String mFolder = "HotSpot";
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
                for (int i = 0; i < cpu.getN_Cores(); i++) {
                    value[i] = Double.parseDouble(Sdatavalue[i]);
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

    //Method for getting the maximum available value index
    public int getMaxIndex(double[] inputArray, boolean[] available){
        double maxValue = 0;
        int index=0;
        for (int i = 0; i < inputArray.length; i++) {
            if (inputArray[i] > maxValue && available[i]) {
                maxValue = inputArray[i];
                index =i;
            }
        }
        return index;
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


    //Method for getting the minimum available value index
    public int getMinIndex(double[] inputArray, boolean[] available){
        double minValue = 200;
        int index=0;
        for (int i = 0; i < inputArray.length; i++) {
            if (inputArray[i] < minValue && available[i]) {
                minValue = inputArray[i];
                index =i;
            }
        }
        return index;
    }


}
