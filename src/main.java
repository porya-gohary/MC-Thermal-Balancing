import org.apache.commons.cli.*;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
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
    //Number of DAG
    static int n_DAGs = 10;

    public static void main(String[] args) throws Exception {

        // --> Command-line Args
        Options options = new Options();
        Option verbose = new Option("v", "verbose", false, "Echo all output.");
        verbose.setRequired(false);
        options.addOption(verbose);

        Option graph = new Option("g", "graph", true, "Number of graphs.");
        verbose.setRequired(false);
        options.addOption(graph);

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
        if (cmd.hasOption("g")) n_DAGs = Integer.parseInt(cmd.getOptionValue("graph"));
        if (cmd.hasOption("h")) {
            formatter.printHelp("Mixed-Criticality Thermal-Balancing", options);
            System.exit(1);
        }

        //Number of system cores
        int n_core = 16;

        //Graph Deadline
        int deadline;
        //deadline Coefficient
        double x = 4;

        //Reliability Coefficient
        double y = 7;

        //Bool For make New DAGS
        boolean create_dag = true;

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

        double n = 3;

        Path temp;

//        double percent[] = {0.0, 0.20, 0.40, 0.60, 0.80, 1.0};
        double percent[] = {0.0};
        double overrun_percent = 0.1;
        double fault_pecent = 0.0;

        McDAG All_DAG[] = new McDAG[n_DAGs + 1];
        int All_deadline[] = new int[n_DAGs + 1];

        //Scheduling Results:
        int PR_Sch;
        int ANS_Sch;
        int SAL_Sch;
        int MED_Sch;
        int MEDR_Sch;

        // Power Results
        double Pro_power[] = new double[2];
        double Ans_power[] = new double[2];
        double Sal_power[] = new double[2];
        double Med_power[] = new double[2];
        double MedR_power[] = new double[2];

        //Temperature Results [0] Avg. Diff. [1] Max. Diff. [2] Max. Temp. [3] Avg. Temp.
        double temp_before[] = new double[4];
        double temp_after[] = new double[4];
        double temp_Ans[] = new double[4];
        double temp_Sal[] = new double[4];
        double temp_Med[] = new double[4];
        double temp_MedR[] = new double[4];

        //avg Temperature Results [0] Avg. Diff. [1] Max. Diff. [2] Max. Temp. [3] Avg. Temp.
        double avg_temp_before[] = new double[4];
        double avg_temp_after[] = new double[4];
        double avg_temp_Ans[] = new double[4];
        double avg_temp_Sal[] = new double[4];
        double avg_temp_Med[] = new double[4];
        double avg_temp_MedR[] = new double[4];

        //Boolean for Run Each Method
        boolean Pro_run = true;
        boolean Ans_run = true;
        boolean Sal_run = true;
        boolean Med_run = true;
        boolean MedR_run = true;

        //QoS
        double PR_QoS = 0;
        double ANS_QoS = 0;
        double SAL_QoS = 0;
        double MED_QoS = 0;
        double MEDR_QoS = 0;

        //Benchmarks Name
//        String benchmark[] = {"Blackscholes1", "Blackscholes2", "Blackscholes3", "Bodytrack1", "Bodytrack2", "Canneal1", "Dedup1", "Ferret1", "Ferret2", "Fluidanimate1", "Fluidanimate2", "Freqmine1", "Freqmine2", "Streamcluster1", "Streamcluster2", "Swaptions1", "Swaptions2", "x264"};
//        int benchmark_time[] = {40, 30, 50, 20, 50, 60, 100, 50, 70, 65, 100, 35, 80, 45, 85, 30, 20, 100};
        double v[] = {0.973, 1.023, 1.062, 1.115, 1.3};
        //Possible Frequencies
        int freq[] = {1000, 1200, 1400, 1600, 2000};
        //Benchmarks Name
        String benchmark[] = {"Basicmath", "Bitcount", "Dijkstra", "FFT", "JPEG", "Patricia", "Qsort", "Sha", "Stringsearch", "Susan"};
        int benchmark_time[] = {156, 25, 33, 160, 28, 87, 25, 13, 8, 20};

//        double t_inf[] = {50.92, 49.42, 49.79, 50.67, 54.49, 51.89, 51.29, 54.11, 54.47, 54.45, 57.49, 50.31, 52.68, 54.92, 55.18, 52.89, 51.27, 54.16};
//        double t_inf[] = {90.7775, 78.0747, 78.2149, 100.672, 126.482, 99.5193, 94.7322, 124.045, 123.281, 131.98, 116.297, 99.8964, 102.252, 112.041, 106.907, 107.353, 106.97, 102.146};
        double t_inf[] = {72.34,72.34,72.76,72.88,73.32,72.02,72.5,72.94,73.11,73.09};

        double[][] peak_power = {{45.1820133, 27.2266533, 27.2250333, 47.8884033, 68.8520133, 47.5627833, 45.3773133, 68.3694333, 67.7015433, 74.7559233, 61.2237933, 47.6432433, 49.4941833, 56.3823333, 51.5399733, 52.5211533, 52.5430233, 49.2146433},
                {50.202237, 30.251837, 30.250037, 53.209337, 76.502237, 52.847537, 50.419237, 75.966037, 75.223937, 83.062137, 68.026437, 52.936937, 54.993537, 62.647037, 57.266637, 58.356837, 58.381137, 54.682937}};

        //Possible Voltages
//        double v[] = {0.9, 1.1, 1.2};
        //Possible Frequencies
//        int freq[] = {800, 1000, 1200};


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
                if (VERBOSE) System.out.println("Deadline = " + deadline + "\n");


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


//            WriteObjectToFile(All_DAG.clone(), "DAGs.txt");
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
            ANS_Sch = n_DAGs;
            SAL_Sch = n_DAGs;
            MED_Sch = n_DAGs;
            MEDR_Sch = n_DAGs;

            PR_QoS = 0;
            ANS_QoS = 0;
            SAL_QoS = 0;
            MED_QoS = 0;
            MEDR_QoS = 0;

            Arrays.fill(Pro_power, 0);
            Arrays.fill(Ans_power, 0);
            Arrays.fill(Sal_power, 0);
            Arrays.fill(Med_power, 0);
            Arrays.fill(MedR_power, 0);

            Arrays.fill(avg_temp_before,0);
            Arrays.fill(avg_temp_after,0);
            Arrays.fill(avg_temp_Ans,0);
            Arrays.fill(avg_temp_Sal,0);
            Arrays.fill(avg_temp_Med,0);
            Arrays.fill(avg_temp_MedR,0);

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
                    if (VERBOSE) System.out.println("------------> Proposed Method <----------");
                    outputWriter.write("------------> Proposed Method <----------" + "\n");
                    try {
                        proposedMothod proposedMothod = new proposedMothod(deadline, n_core, dag, xml_name, overrun_percent, VERBOSE);
                        proposedMothod.start();
                        onlineBalancer onlineBalancer = new onlineBalancer(proposedMothod.getBps(), proposedMothod.getCpu(), proposedMothod.getDag(), VERBOSE);
                        temp_before = onlineBalancer.balanceCalculator();
                        temp = Files.move(Paths.get("MatEx-1.0" + pathSeparator + "thermaltrace" + pathSeparator + "thermal.ttrace"),
                                Paths.get("OV" + overrun_percent + "F" + fault_pecent + pathSeparator + xml_name + pathSeparator + "PR_thermal[Before].txt"));

                        temp = Files.move(Paths.get("MatEx-1.0" + pathSeparator + "powertrace" + pathSeparator + "A15_" + n_core + ".ptrace"),
                                Paths.get("OV" + overrun_percent + "F" + fault_pecent + pathSeparator + xml_name + pathSeparator + "PR_POWER[Before].txt"));

                        //AFTER BALANCING
                        onlineBalancer.run();
                        temp_after = onlineBalancer.balanceCalculator();
                        Pro_power[0] += proposedMothod.getCpu().power_results()[0];
                        Pro_power[1] += proposedMothod.getCpu().power_results()[1];
                        PR_QoS += proposedMothod.QoS();
                        outputWriter.write("Avg. Power= " + proposedMothod.getCpu().power_results()[0] + "\n");
                        outputWriter.write("Peak Power= " + proposedMothod.getCpu().power_results()[1] + "\n");
                        outputWriter.write("═════╣  QoS = " + proposedMothod.QoS() + "\n");
                        outputWriter.write("═══════════════════ Before ════════════════════════ " + "\n");
                        //Temperature Results [0] Avg. Diff. [1] Max. Diff. [2] Max. Temp. [3] Avg. Temp.
                        outputWriter.write("Avg. Diff. = " + temp_before[0] + "\n");
                        outputWriter.write("Max. Diff. = " + temp_before[1] + "\n");
                        outputWriter.write("Max. Temp. = " + temp_before[2] + "\n");
                        outputWriter.write("Avg. Temp. = " + temp_before[3] + "\n");
                        for (int k = 0; k < 4; k++) {
                            avg_temp_before[k] += temp_before[k];
                        }

                        outputWriter.write("═══════════════════  After  ════════════════════════ " + "\n");
                        //Temperature Results [0] Avg. Diff. [1] Max. Diff. [2] Max. Temp. [3] Avg. Temp.
                        outputWriter.write("Avg. Diff. = " + temp_after[0] + "\n");
                        outputWriter.write("Max. Diff. = " + temp_after[1] + "\n");
                        outputWriter.write("Max. Temp. = " + temp_after[2] + "\n");
                        outputWriter.write("Avg. Temp. = " + temp_after[3] + "\n");

                        for (int k = 0; k < 4; k++) {
                            avg_temp_after[k] += temp_after[k];
                        }

                        outputWriter.write("End Time = " + proposedMothod.getCpu().Endtime(-1) + "\n");
                        temp = Files.move(Paths.get("MatEx-1.0" + pathSeparator + "thermaltrace" + pathSeparator + "thermal.ttrace"),
                                Paths.get("OV" + overrun_percent + "F" + fault_pecent + pathSeparator + xml_name + pathSeparator + "PR_thermal[After].txt"));

                        temp = Files.move(Paths.get("MatEx-1.0" + pathSeparator + "powertrace" + pathSeparator + "A15_" + n_core + ".ptrace"),
                                Paths.get("OV" + overrun_percent + "F" + fault_pecent + pathSeparator + xml_name + pathSeparator + "PR_POWER[After].txt"));

                    } catch (Exception e) {
                        if (VERBOSE) e.printStackTrace();
                        outputWriter.write("[ PROPOSED METHOD ] Infeasible!   " + xml_name + "\n");
                        outputWriter.write(e.getStackTrace().toString() + "\n");
                        PR_Sch--;
                    }

                }

                if (Ans_run) {
                    progressBar.setMethod("Ansari Method");
                    if (VERBOSE) System.out.println("------------> Ansari Method <----------");
                    outputWriter.write("\n------------> Ansari Method <----------" + "\n");
                    Ansari2019 ansari2019 = new Ansari2019(deadline, n_core, n, dag, xml_name, overrun_percent, VERBOSE);
                    try {
                        ansari2019.start();
                        Ans_power[0] += ansari2019.getCpu().power_results()[0];
                        Ans_power[1] += ansari2019.getCpu().power_results()[1];
                        ANS_QoS += ansari2019.QoS();

                        outputWriter.write("Avg. Power= " + ansari2019.getCpu().power_results()[0] + "\n");
                        outputWriter.write("Peak Power= " + ansari2019.getCpu().power_results()[1] + "\n");
                        outputWriter.write("═════╣  QoS = " + ansari2019.QoS() + "\n");

                        temp_Ans = ansari2019.balanceCalculator();
                        outputWriter.write("═══════════════════  Temp.  ════════════════════════ " + "\n");
                        //Temperature Results [0] Avg. Diff. [1] Max. Diff. [2] Max. Temp. [3] Avg. Temp.
                        outputWriter.write("Avg. Diff. = " + temp_Ans[0] + "\n");
                        outputWriter.write("Max. Diff. = " + temp_Ans[1] + "\n");
                        outputWriter.write("Max. Temp. = " + temp_Ans[2] + "\n");
                        outputWriter.write("Avg. Temp. = " + temp_Ans[3] + "\n");

                        for (int k = 0; k < 4; k++) {
                            avg_temp_Ans[k] += temp_Ans[k];
                        }
                        outputWriter.write("End Time = " + ansari2019.getCpu().Endtime(-1) + "\n");
                        temp = Files.move(Paths.get("MatEx-1.0" + pathSeparator + "thermaltrace" + pathSeparator + "thermal.ttrace"),
                                Paths.get("OV" + overrun_percent + "F" + fault_pecent + pathSeparator + xml_name + pathSeparator + "ANS_thermal.txt"));


                    } catch (Exception e) {
                        if (VERBOSE) e.printStackTrace();
                        outputWriter.write("[ ANSARI METHOD ] Infeasible!   " + xml_name + "\n");
                        ANS_Sch--;
                    }
                }

                if (Sal_run) {
                    progressBar.setMethod("Salehi Method");
                    if (VERBOSE) System.out.println("------------> Medina Replication Method <----------");
                    outputWriter.write("\n------------> Salehi Method <----------" + "\n");
                    Salehi salehi = new Salehi(deadline, n_core, n, dag, xml_name, fault_pecent, VERBOSE);
                    try {
                        salehi.start();
                        Sal_power[0] += salehi.getCpu().power_results()[0];
                        Sal_power[1] += salehi.getCpu().power_results()[1];
                        SAL_QoS += salehi.QoS();

                        outputWriter.write("Avg. Power= " + salehi.getCpu().power_results()[0] + "\n");
                        outputWriter.write("Peak Power= " + salehi.getCpu().power_results()[1] + "\n");
                        outputWriter.write("═════╣  QoS = " + salehi.QoS() + "\n");

                        temp_Sal = salehi.balanceCalculator();
                        outputWriter.write("═══════════════════  Temp.  ════════════════════════ " + "\n");
                        //Temperature Results [0] Avg. Diff. [1] Max. Diff. [2] Max. Temp. [3] Avg. Temp.
                        outputWriter.write("Avg. Diff. = " + temp_Sal[0] + "\n");
                        outputWriter.write("Max. Diff. = " + temp_Sal[1] + "\n");
                        outputWriter.write("Max. Temp. = " + temp_Sal[2] + "\n");
                        outputWriter.write("Avg. Temp. = " + temp_Sal[3] + "\n");

                        for (int k = 0; k < 4; k++) {
                            avg_temp_Sal[k] += temp_Sal[k];
                        }

                        outputWriter.write("End Time = " + salehi.getCpu().Endtime(-1) + "\n");

                        temp = Files.move(Paths.get("MatEx-1.0" + pathSeparator + "thermaltrace" + pathSeparator + "thermal.ttrace"),
                                Paths.get("OV" + overrun_percent + "F" + fault_pecent + pathSeparator + xml_name + pathSeparator + "SAL_thermal.txt"));


                    } catch (Exception e) {
                        if (VERBOSE) e.printStackTrace();
                        outputWriter.write("[ SALEHI METHOD ] Infeasible!   " + xml_name + "\n");
                        SAL_Sch--;
                    }
                }

                if (Med_run) {
                    progressBar.setMethod("Medina Method");
                    if (VERBOSE) System.out.println("------------> Medina Method <----------");
                    outputWriter.write("\n------------> Medina Method <----------" + "\n");
                    Medina medina = new Medina(deadline, n_core, n, dag, xml_name, overrun_percent, VERBOSE);
                    try {
                        medina.start();
                        Med_power[0] += medina.getCpu().power_results()[0];
                        Med_power[1] += medina.getCpu().power_results()[1];
                        MED_QoS += medina.QoS();

                        outputWriter.write("Avg. Power= " + medina.getCpu().power_results()[0] + "\n");
                        outputWriter.write("Peak Power= " + medina.getCpu().power_results()[1] + "\n");
                        outputWriter.write("═════╣  QoS = " + medina.QoS() + "\n");

                        temp_Med = medina.balanceCalculator();
                        outputWriter.write("═══════════════════  Temp.  ════════════════════════ " + "\n");
                        //Temperature Results [0] Avg. Diff. [1] Max. Diff. [2] Max. Temp. [3] Avg. Temp.
                        outputWriter.write("Avg. Diff. = " + temp_Med[0] + "\n");
                        outputWriter.write("Max. Diff. = " + temp_Med[1] + "\n");
                        outputWriter.write("Max. Temp. = " + temp_Med[2] + "\n");
                        outputWriter.write("Avg. Temp. = " + temp_Med[3] + "\n");

                        for (int k = 0; k < 4; k++) {
                            avg_temp_Med[k] += temp_Med[k];
                        }

                        outputWriter.write("End Time = " + medina.getCpu().Endtime(-1) + "\n");

                        temp = Files.move(Paths.get("MatEx-1.0" + pathSeparator + "thermaltrace" + pathSeparator + "thermal.ttrace"),
                                Paths.get("OV" + overrun_percent + "F" + fault_pecent + pathSeparator + xml_name + pathSeparator + "MED_thermal.txt"));


                    } catch (Exception e) {
                        if (VERBOSE) e.printStackTrace();
                        outputWriter.write("[ MEDINA METHOD ] Infeasible!   " + xml_name + "\n");
                        MED_Sch--;
                    }
                }

                if (MedR_run) {
                    progressBar.setMethod("Medina Rep. Method");
                    if (VERBOSE) System.out.println("------------> Medina Replication Method <----------");
                    outputWriter.write("\n------------> Medina Replication Method <----------" + "\n");
                    MedinaReplication medinaReplication = new MedinaReplication(deadline, n_core, n, dag, xml_name, overrun_percent, VERBOSE);
                    try {
                        medinaReplication.start();
                        MedR_power[0] += medinaReplication.getCpu().power_results()[0];
                        MedR_power[1] += medinaReplication.getCpu().power_results()[1];
                        MEDR_QoS += medinaReplication.QoS();

                        outputWriter.write("Avg. Power= " + medinaReplication.getCpu().power_results()[0] + "\n");
                        outputWriter.write("Peak Power= " + medinaReplication.getCpu().power_results()[1] + "\n");
                        outputWriter.write("═════╣  QoS = " + medinaReplication.QoS() + "\n");

                        temp_MedR = medinaReplication.balanceCalculator();
                        outputWriter.write("═══════════════════  Temp.  ════════════════════════ " + "\n");
                        //Temperature Results [0] Avg. Diff. [1] Max. Diff. [2] Max. Temp. [3] Avg. Temp.
                        outputWriter.write("Avg. Diff. = " + temp_MedR[0] + "\n");
                        outputWriter.write("Max. Diff. = " + temp_MedR[1] + "\n");
                        outputWriter.write("Max. Temp. = " + temp_MedR[2] + "\n");
                        outputWriter.write("Avg. Temp. = " + temp_MedR[3] + "\n");

                        for (int k = 0; k < 4; k++) {
                            avg_temp_MedR[k] += temp_MedR[k];
                        }
                        outputWriter.write("End Time = " + medinaReplication.getCpu().Endtime(-1) + "\n");
                        temp = Files.move(Paths.get("MatEx-1.0" + pathSeparator + "thermaltrace" + pathSeparator + "thermal.ttrace"),
                                Paths.get("OV" + overrun_percent + "F" + fault_pecent + pathSeparator + xml_name + pathSeparator + "MedR_thermal.txt"));


                    } catch (Exception e) {
                        if (VERBOSE) e.printStackTrace();
                        outputWriter.write("[ MEDINA Replication METHOD ] Infeasible!   " + xml_name + "\n");
                        MEDR_Sch--;
                    }
                }


                outputWriter.write("\n");
                outputWriter.flush();
            }
            outputWriter.write("\n");
            outputWriter.write(">>>>>>>>>>>>> SUMMARY OF ALL DAGs <<<<<<<<<<<<" + "\n");
            outputWriter.write("Proposed Method SCH: " + PR_Sch + "\n");
            outputWriter.write("Ansari Method SCH: " + ANS_Sch + "\n");
            outputWriter.write("Salehi Method SCH: " + SAL_Sch + "\n");
            outputWriter.write("Medina Method SCH: " + MED_Sch + "\n");
            outputWriter.write("Medina Replication Method SCH: " + MEDR_Sch + "\n");

            outputWriter.write("Proposed Method Avg. Power= " + (Pro_power[0] / PR_Sch) + "\n");
            outputWriter.write("Ansari Method Avg. Power= " + (Ans_power[0] / ANS_Sch) + "\n");
            outputWriter.write("Salehi Method Avg. Power= " + (Sal_power[0] / SAL_Sch) + "\n");
            outputWriter.write("Medina Method Avg. Power= " + (Med_power[0] / MED_Sch) + "\n");
            outputWriter.write("Medina Replication Method Avg. Power= " + (MedR_power[0] / MEDR_Sch) + "\n");

            outputWriter.write("Proposed Method Peak Power= " + (Pro_power[1] / PR_Sch) + "\n");
            outputWriter.write("Ansari Method Peak Power= " + (Ans_power[1] / ANS_Sch) + "\n");
            outputWriter.write("Salehi Method Peak Power= " + (Sal_power[1] / SAL_Sch) + "\n");
            outputWriter.write("Medina Method Peak Power= " + (Med_power[1] / MED_Sch) + "\n");
            outputWriter.write("Medina Replication Method Peak Power= " + (MedR_power[1] / MEDR_Sch) + "\n");

            outputWriter.write("═══════════════════  Before  ════════════════════════ " + "\n");
            outputWriter.write("Proposed Method Avg. Diff.= " + (avg_temp_before[0] / PR_Sch) + "\n");
            outputWriter.write("Proposed Method Max. Diff.= " + (avg_temp_before[1] / PR_Sch) + "\n");
            outputWriter.write("Proposed Method Max. Temp.= " + (avg_temp_before[2] / PR_Sch) + "\n");
            outputWriter.write("Proposed Method Avg. Temp.= " + (avg_temp_before[3] / PR_Sch) + "\n");


            outputWriter.write("═══════════════════  After  ════════════════════════ " + "\n");
            outputWriter.write("Proposed Method Avg. Diff.= " + (avg_temp_after[0] / PR_Sch) + "\n");
            outputWriter.write("Proposed Method Max. Diff.= " + (avg_temp_after[1] / PR_Sch) + "\n");
            outputWriter.write("Proposed Method Max. Temp.= " + (avg_temp_after[2] / PR_Sch) + "\n");
            outputWriter.write("Proposed Method Avg. Temp.= " + (avg_temp_after[3] / PR_Sch) + "\n");

            outputWriter.write("═══════════════════  Ansari  ════════════════════════ " + "\n");
            outputWriter.write("Ansari Method Avg. Diff.= " + (avg_temp_Ans[0] / ANS_Sch) + "\n");
            outputWriter.write("Ansari Method Max. Diff.= " + (avg_temp_Ans[1] / ANS_Sch) + "\n");
            outputWriter.write("Ansari Method Max. Temp.= " + (avg_temp_Ans[2] / ANS_Sch) + "\n");
            outputWriter.write("Ansari Method Avg. Temp.= " + (avg_temp_Ans[3] / ANS_Sch) + "\n");

            outputWriter.write("═══════════════════  Salehi  ════════════════════════ " + "\n");
            outputWriter.write("Salehi Method Avg. Diff.= " + (avg_temp_Sal[0] / SAL_Sch) + "\n");
            outputWriter.write("Salehi Method Max. Diff.= " + (avg_temp_Sal[1] / SAL_Sch) + "\n");
            outputWriter.write("Salehi Method Max. Temp.= " + (avg_temp_Sal[2] / SAL_Sch) + "\n");
            outputWriter.write("Salehi Method Avg. Temp.= " + (avg_temp_Sal[3] / SAL_Sch) + "\n");

            outputWriter.write("═══════════════════  Medina  ════════════════════════ " + "\n");
            outputWriter.write("Medina Method Avg. Diff.= " + (avg_temp_Med[0] / MED_Sch) + "\n");
            outputWriter.write("Medina Method Max. Diff.= " + (avg_temp_Med[1] / MED_Sch) + "\n");
            outputWriter.write("Medina Method Max. Temp.= " + (avg_temp_Med[2] / MED_Sch) + "\n");
            outputWriter.write("Medina Method Avg. Temp.= " + (avg_temp_Med[3] / MED_Sch) + "\n");

            outputWriter.write("═════════════  Medina Replication  ═════════════════ " + "\n");
            outputWriter.write("Medina Replication Method Avg. Diff.= " + (avg_temp_MedR[0] / MEDR_Sch) + "\n");
            outputWriter.write("Medina Replication Method Max. Diff.= " + (avg_temp_MedR[1] / MEDR_Sch) + "\n");
            outputWriter.write("Medina Replication Method Max. Temp.= " + (avg_temp_MedR[2] / MEDR_Sch) + "\n");
            outputWriter.write("Medina Replication Method Avg. Temp.= " + (avg_temp_MedR[3] / MEDR_Sch) + "\n");

            outputWriter.write("\n");

            outputWriter.write("Proposed Method QoS= " + (PR_QoS / PR_Sch) + "\n");
            outputWriter.write("Ansari Method QoS= " + (ANS_QoS / ANS_Sch) + "\n");
            outputWriter.write("Salehi Method QoS= " + (SAL_QoS / SAL_Sch) + "\n");
            outputWriter.write("Medina Method QoS= " + (MED_QoS / MED_Sch) + "\n");
            outputWriter.write("Medina Replication Method QoS= " + (MEDR_QoS / MEDR_Sch) + "\n");

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
