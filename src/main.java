import org.apache.commons.cli.*;

import java.io.*;
import java.util.Random;

import static java.lang.Math.pow;

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
public class main {
    //The system-dependent path-separator character
    static String pathSeparator = File.separator;
    static boolean VERBOSE = false;

    public static void main(String[] args) throws Exception {

        // --> Command-line Args
        Options options = new Options();
        Option verbose = new Option("v", "verbose", false, "Echo all output.");
        verbose.setRequired(false);
        options.addOption(verbose);

        Option help = new Option("h", "help", false, "Help.");
        verbose.setRequired(false);
        options.addOption(help);

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd = null;

        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("Mixed-Criticality Thermal-Balancing", options);

            System.exit(1);
        }

        if (cmd.hasOption("v")) VERBOSE = true;
        if (cmd.hasOption("h")) {
            formatter.printHelp("Mixed-Criticality Thermal-Balancing", options);
            System.exit(1);
        }

        //Number of system cores
        int n_core = 4;

        //Graph Deadline
        int deadline;
        //deadline Coefficient
        double x = 5;

        //Reliability Coefficient
        double y = 7;

        //Bool For make New DAGS
        boolean create_dag = false;
        //Number of DAG
        int n_DAGs = 1;
        //MC-DAG
        McDAG dag;
        //Dag XML Name
        String xml_name = "1";
        //Reliability File Name
        String rel_name = "rel";
        //Fault Rate Spec.
        double landa0 = 0.00001;
        int d = 3;
        //Number Of Overrun
        int n_overrun = 0;
        //Number Of Fault
        int n_fault = 0;

        double n=3;

//        double percent[] = {0.0, 0.20, 0.40, 0.60, 0.80, 1.0};
        double percent[] = {0.0};
        double overrun_percent = 0.1;
        double fault_pecent = 0.0;

        McDAG All_DAG[] = new McDAG[n_DAGs + 1];
        int All_deadline[] = new int[n_DAGs + 1];

        //Scheduling Results:
        int PR_Sch;

        // Power Results
        double Pro_power[] = new double[2];

        //Temperature Results [0] Avg. Diff. [1] Max. Diff. [2] Max. Temp. [3] Avg. Temp.
        double temp_before[] = new double[4];
        double temp_after[] = new double[4];

        //Boolean for Run Each Method
        boolean Pro_run = false;
        boolean Ans_run = true;

        //QoS
        double PR_QoS = 0;

        //Benchmarks Name
        String benchmark[] = {"Blackscholes1", "Blackscholes2", "Blackscholes3", "Bodytrack1", "Bodytrack2", "Canneal1", "Dedup1", "Ferret1", "Ferret2", "Fluidanimate1", "Fluidanimate2", "Freqmine1", "Freqmine2", "Streamcluster1", "Streamcluster2", "Swaptions1", "Swaptions2", "x264"};
        int benchmark_time[] = {40, 30, 50, 20, 50, 60, 100, 50, 70, 65, 100, 35, 80, 45, 85, 30, 20, 100};
        double t_inf[] = {50.92, 49.42, 49.79, 50.67, 54.49, 51.89, 51.29, 54.11, 54.47, 54.45, 57.49, 50.31, 52.68, 54.92, 55.18, 52.89, 51.27, 54.16};

        //Possible Voltages
        double v[] = {1, 1.115, 1.3};
        //Possible Frequencies
        int freq[] = {800, 1000, 1200};


        if (create_dag) {
            BufferedWriter outputWriter = null;
            outputWriter = new BufferedWriter(new FileWriter("DAGs_Summary.txt"));
            for (int i = 1; i <= n_DAGs; i++) {
                xml_name = i + "";
                if (VERBOSE) System.out.println("Mapping :::> DAG " + xml_name + "");
                File file = new File("DAGs//" + xml_name + ".xml");
                dag_Reader dr = new dag_Reader(file, VERBOSE);
                dag = dr.getDag();
                dag.setHINodes();
                benchmark_mapping benchmark_mapping = new benchmark_mapping(dag, benchmark, benchmark_time, t_inf);
                benchmark_mapping.mapping();
                benchmark_mapping.cal_LPL();
                deadline = benchmark_mapping.cal_deadline(x);


                outputWriter.write(">>>>>>>>>> ::: DAG " + xml_name + " ::: <<<<<<<<<<" + "\n");
                outputWriter.write("Number Of HI-Critical Tasks = " + dag.getNodes_HI().size() + "\n");
                outputWriter.write("Number Of LO-Critical Tasks = " + (dag.getVertices().size() - dag.getNodes_HI().size()) + "\n");
                outputWriter.write("Number Of Tasks = " + (dag.getVertices().size()) + "\n");
                outputWriter.write("Deadline = " + deadline + "\n");

//                benchmark_mapping.debug();
                All_deadline[i] = deadline;
                All_DAG[i] = dag;
                try {
                    relibility_creator(dag, "rel" + i, y);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            outputWriter.flush();
            outputWriter.close();


            WriteObjectToFile(All_DAG.clone(), "DAGs.txt");
            WriteObjectToFile(All_deadline, "Deadline.txt");

        } else {
            All_DAG = (McDAG[]) ReadObjectFromFile("DAGs.txt");
            All_deadline = (int[]) ReadObjectFromFile("Deadline.txt");
        }

        for (int i = 1; i <= n_DAGs; i++) {
            xml_name = i + "";

            dag = All_DAG[i];
            deadline = All_deadline[i];

            // Print Deadline
            if (VERBOSE) System.out.println("Deadline= " + deadline);

            File rel = new File(rel_name + xml_name + ".txt");
            Reliability_cal rc = new Reliability_cal(landa0, d, v[v.length - 1], v[0], rel, v, freq, dag, VERBOSE);
            for (Vertex a : dag.getVertices()) {
                if (a.isHighCr()) rc.cal(a.getName());

            }
        }
        ProgressBar progressBar = new ProgressBar();
        progressBar.start();
//        for (int i = 0; i <= 100; i++) {
//            progressBar.setPercent(i);
//            Thread.sleep(1000);
//        }
//        System.out.println();
//        progressBar.stop();
        for (int j = 0; j < percent.length; j++) {
            overrun_percent = percent[j];
            System.out.println("Overrun Prencent= " + overrun_percent);


            File newFolder2 = new File("OV" + overrun_percent + "F" + fault_pecent);
            newFolder2.mkdir();

            BufferedWriter outputWriter = null;
            outputWriter = new BufferedWriter(new FileWriter("OV" + overrun_percent + "F" + fault_pecent + "//" + "Summary.txt"));

            PR_Sch = n_DAGs;

            for (int i = 1; i <= n_DAGs; i++) {
                progressBar.setPercent(i * 100 / n_DAGs);
                xml_name = i + "";

                File newFolder = new File("OV" + overrun_percent + "F" + fault_pecent + "//" + xml_name);
                newFolder.mkdir();

                outputWriter.write(">>>>>>>>>> ::: DAG " + xml_name + " Start ::: <<<<<<<<<<" + "\n");

                dag = All_DAG[i];
                deadline = All_deadline[i];
                if (Pro_run) {
                    progressBar.setMethod("Proposed Method");
                    if(VERBOSE) System.out.println("------------> Proposed Method <----------");
                    outputWriter.write("------------> Proposed Method <----------" + "\n");
                    try {
                        proposedMothod proposedMothod = new proposedMothod(deadline, n_core, dag, xml_name, overrun_percent, VERBOSE);
                        proposedMothod.start();
                        onlineBalancer onlineBalancer = new onlineBalancer(proposedMothod.getBps(), proposedMothod.getCpu(), proposedMothod.getDag(), VERBOSE);
                        temp_before = onlineBalancer.balanceCalculator();
                        onlineBalancer.run();
                        temp_after = onlineBalancer.balanceCalculator();
                        Pro_power[0] += proposedMothod.getCpu().power_results()[0];
                        Pro_power[1] += proposedMothod.getCpu().power_results()[1];
                        PR_QoS += proposedMothod.QoS();
                        outputWriter.write("Avg. Power= " + proposedMothod.getCpu().power_results()[0] + "\n");
                        outputWriter.write("Peak Power= " + proposedMothod.getCpu().power_results()[1] + "\n");
                        outputWriter.write("═════╣  QoS = " + proposedMothod.QoS() + "\n");
                        outputWriter.write("═══════════════════ Before ════════════════════════ "+ "\n");
                        //Temperature Results [0] Avg. Diff. [1] Max. Diff. [2] Max. Temp. [3] Avg. Temp.
                        outputWriter.write("Avg. Diff. = " + temp_before[0] + "\n");
                        outputWriter.write("Max. Diff. = " + temp_before[1] + "\n");
                        outputWriter.write("Max. Temp. = " + temp_before[2] + "\n");
                        outputWriter.write("Avg. Temp. = " + temp_before[3] + "\n");

                        outputWriter.write("═══════════════════  After  ════════════════════════ "+ "\n");
                        //Temperature Results [0] Avg. Diff. [1] Max. Diff. [2] Max. Temp. [3] Avg. Temp.
                        outputWriter.write("Avg. Diff. = " + temp_after[0] + "\n");
                        outputWriter.write("Max. Diff. = " + temp_after[1] + "\n");
                        outputWriter.write("Max. Temp. = " + temp_after[2] + "\n");
                        outputWriter.write("Avg. Temp. = " + temp_after[3] + "\n");

                    } catch (Exception e) {
                        if (VERBOSE) e.printStackTrace();
                        outputWriter.write("[ PROPOSED METHOD ] Infeasible!   " + xml_name + "\n");
                        PR_Sch--;
                    }

                }

                if(Ans_run){
                    Ansari2019 ansari2019=new Ansari2019(deadline,n_core,n,dag,xml_name,overrun_percent,VERBOSE);
                    ansari2019.start();
                }
            }
            outputWriter.write("\n");
            outputWriter.write(">>>>>>>>>>>>> SUMMARY OF ALL DAGs <<<<<<<<<<<<" + "\n");
            outputWriter.write("Proposed Method SCH: " + PR_Sch + "\n");

            outputWriter.write("Proposed Method Avg. Power= " + (Pro_power[0] / PR_Sch) + "\n");

            outputWriter.write("Proposed Method Peak Power= " + (Pro_power[1] / PR_Sch) + "\n");

            outputWriter.write("Proposed Method QoS= " + (PR_QoS / PR_Sch) + "\n");

            outputWriter.flush();
            outputWriter.close();
        }
        progressBar.stop();


    }


    public static void relibility_creator(McDAG dag, String rel_name, double n) throws IOException {
        double rel[] = new double[dag.getVertices().size()];
        for (int i = 0; i < dag.getVertices().size(); i++) {
            Random rnd = new Random();
            double t = 0.9;
            int a = rnd.nextInt((int) n + 3);
            for (int j = 2; j < a + 1; j++) {
                t += pow(0.1, j) * 9;
            }
            rel[i] = t;
        }

        BufferedWriter outputWriter = null;
        outputWriter = new BufferedWriter(new FileWriter(rel_name + ".txt"));
        for (int j = 0; j < dag.getVertices().size(); j++) {
            outputWriter.write(rel[j] + "\n");
        }
        ;
        outputWriter.flush();
        outputWriter.close();

    }

    public static void WriteObjectToFile(Object serObj, String filename) {
        try {

            FileOutputStream fileOut = new FileOutputStream(filename);
            ObjectOutputStream objectOut = new ObjectOutputStream(fileOut);
            objectOut.writeObject(serObj);
            objectOut.flush();
            objectOut.close();
            if (VERBOSE) System.out.println("The Object was successfully written to a file");

        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(4);
        }
    }

    public static Object ReadObjectFromFile(String filename) {
        try {
            McDAG All_DAG[];

            int d[];
            FileInputStream fileIn = new FileInputStream(filename);
            ObjectInputStream objectIn = new ObjectInputStream(fileIn);
            if (filename == "Deadline.txt") {
                d = (int[]) objectIn.readObject();
                if (VERBOSE) System.out.println("The Object  was succesfully Read From a file");
                return d;
            } else {
                All_DAG = (McDAG[]) objectIn.readObject();
                objectIn.close();

                if (VERBOSE) System.out.println("The Object was successfully Read From a file");
                return All_DAG;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(4);
        }
        return null;
    }


}
