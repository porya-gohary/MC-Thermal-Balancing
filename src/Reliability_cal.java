/*******************************************************************************
 * Copyright © 2019 Porya Gohary
 * Written by Porya Gohary (Email: gohary@ce.sharif.edu)
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import static java.lang.Math.*;

public class Reliability_cal {
    //the fault rate at the maximum voltage
    double landa0;
    //Transient faults are usually assumed to follow a Poisson distribution with an average rate LANDA
    double landa;
    //d is a technology dependent constant
    double d;
    // scaled supply voltage
    double v_i;
    // maximum supply voltage
    double v_max;
    //ratio of v_i / v_max
    double rou;
    //ratio of v_min / v_max
    double rou_min;
    //All possible Freq
    int[] freq;
    //Execution Time LO
    double t_i_LO;
    //Execution Time HI
    double t_i_HI;
    //minimum Reliability For each Task
    File Rel;
    ArrayList<Double> rel_f;
    //Minimum Voltage
    double v_min;

    // Minimum Execution Time In Maximum Voltage And Frequency (LO)
    double t_min_LO;
    // Minimum Execution Time In Maximum Voltage And Frequency (HI)
    double t_min_HI;

    // All possible Voltage
    double[] v;

    //Reliability In Fault Free case
    double R_1;
    //Reliability In Faulty case
    double R_2;

    //Reliability
    double R_3;
    //Number Of Copy For Each Task
    double n;
    McDAG dag;

    String v_name;

    //probability of making an incorrect decision during the acceptance test
    double alpha = 0;

    //Verbose
    boolean VERBOSE = false;


    public Reliability_cal(double landa0, double d, double v_max, double v_min, File rel, double[] v, int[] freq, McDAG dag, boolean VERBOSE) {

        this.landa0 = landa0;
        this.d = d;

        this.v_max = v_max;
        this.v_min = v_min;

        Rel = rel;
        this.v = v;
        this.freq = freq;
        this.dag = dag;
        this.VERBOSE = VERBOSE;
        //cal();
        Read_file();
    }


    //Read Reliability From File And Set it on Every Vertices
    public void Read_file() {

        rel_f = new ArrayList<>();
        BufferedReader reader;
        try {

            reader = new BufferedReader(new FileReader(Rel));

            String line = reader.readLine();
            int i = 0;
            while (dag.getVertices().size() != i) {
                rel_f.add(Double.parseDouble(line));
//                System.out.println(line);
                String s = "D0N" + i;
                dag.getNodebyName(s).setReliability(Double.parseDouble(line));

                i++;
                line = reader.readLine();
            }
            reader.close();


        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void cal(String Task_name) throws Exception {
        Vertex task = dag.getNodebyName(Task_name);
        t_min_LO = task.getWcet(0);
        t_min_HI = task.getWcet(1);

        rou_min = v_min / v_max;


        v_i = v[v.length - 1];

        rou = v_i / v_max;
        t_i_LO = t_min_LO * freq[freq.length - 1] / freq[freq.length - 1];
        t_i_HI = t_min_HI * freq[freq.length - 1] / freq[freq.length - 1];

        landa = (landa0 * pow(10, ((d * (1 - rou)) / (1 - rou_min))));
        landa = (-1) * landa;

        // PoF LO
        double r_LO = exp((landa * t_i_LO));
        r_LO = (1 - alpha) * r_LO;
        double PoF_LO = 1 - r_LO;


        // PoF HI
        double r_HI = exp((landa * t_i_HI));
        r_LO = (1 - alpha) * r_HI;
        double PoF_HI = 1 - r_HI;

        //Calculate minimum Number of Replica
        int replica_lower_bound = (int) ceil(log((1 - task.getReliability()) / PoF_HI) / log(PoF_LO));
        //Calculate maximum Number of Replica
        int replica_upper_bound = (int) ceil(log((1 - task.getReliability()) / PoF_HI) / log(PoF_HI));

        if (VERBOSE)
            System.out.println(task.getName() + " Reliability = " + task.getReliability() + " [ " + replica_lower_bound + " , " + replica_upper_bound + " ]");

        //Set Number of Replica  + main task
        task.setReplica(replica_upper_bound + 1);

    }


}
