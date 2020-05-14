import java.io.File;
import java.io.FileNotFoundException;
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

    public onlineBalancer(Integer[] bps, CPU cpu, McDAG dag, boolean VERBOSE) {
        this.bps = bps;
        this.cpu = cpu;
        this.dag = dag;
        this.VERBOSE = VERBOSE;
    }

    public void run() {
        for (int i = 0; i < bps.length - 1; i++) {
            for (int j = 0; j < cpu.getN_Cores(); j++) {
                System.out.println("Core " + j + "\tTask Name " + cpu.getRunningTask(j, bps[i + 1] - 1));
                System.out.println(">>>>> " + predict(j, bps[i], bps[i + 1] - 1, 45));
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

    public void balanceCalculator() {
        String mFolder = "HotSpot";
        String sFolder = "thermaltrace";
        String filename = "thermal.ttrace";
        String pathSeparator = File.separator;
        File thermalFile = null;
        try {
            thermalFile = new File(mFolder + pathSeparator + sFolder + pathSeparator + filename);
            Scanner Reader = new Scanner(thermalFile);
            //Reader.hasNextLine()
            double diff = 0;
            for (int j = 0; j < cpu.Endtime(-1); j++) {
                String data = Reader.nextLine();
                String Sdatavalue[] = data.split("\t");
                double value[] = new double[cpu.getN_Cores()];
                for (int i = 0; i < cpu.getN_Cores(); i++) {
                    value[i] = Double.parseDouble(Sdatavalue[i]);
                }
                diff += getMax(value) - getMin(value);
                //System.out.println(data);
            }
            Reader.close();
            System.out.println(diff);
            System.out.println(diff / cpu.Endtime(-1));
        } catch (FileNotFoundException e) {
            if (VERBOSE) {
                System.out.println("An error occurred in Reading Thermal Trace File.");
                System.out.println("Path: " + thermalFile.getAbsolutePath());
                e.printStackTrace();
            }
        }

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


}
