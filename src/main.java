import org.apache.commons.cli.*;

import java.io.File;

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

    public static void main(String[] args) throws Exception {

        // --> Command-line Args
        boolean VERBOSE = false;


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


        //Graph Deadline
        int deadline;
        //Number of system cores
        int n_core;
        //Bool For make New DAGS
        boolean create_dag = true;
        //Number of DAG
        int n_DAGs = 50;
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

        double percent[] = {0.0, 0.20, 0.40, 0.60, 0.80, 1.0};
        double overrun_percent = 0.0;
        double fault_pecent = 0.0;

        McDAG All_DAG[] = new McDAG[n_DAGs + 1];
        int All_deadline[] = new int[n_DAGs + 1];

        //Scheduling Results:
        int PR_Sch;

        // Power Results
        double Pro_power[] = new double[2];

        //Boolean for Run Each Method
        boolean Pro_run = false;

        //Benchmarks Name
        String benchmark[] = {"Blackscholes1", "Blackscholes2", "Blackscholes3", "Bodytrack1", "Bodytrack2", "Canneal1", "Dedup1", "Ferret1", "Ferret2", "Fluidanimate1", "Fluidanimate2", "Freqmine1", "Freqmine2", "Streamcluster1", "Streamcluster2", "Swaptions1", "Swaptions2", "x264"};
        int benchmark_time[] = {40, 30, 50, 20, 50, 60, 100, 50, 70, 65, 100, 35, 80, 45, 85, 30, 20, 100};


        //HotSpot location and information
        String hotspot_path = "HotSpot" + pathSeparator + "hotspot";
        String hotspot_config = "HotSpot" + pathSeparator + "configs" + pathSeparator + "hotspot_4.config";
        String floorplan = "HotSpot" + pathSeparator + "floorplans" + pathSeparator + "Alpha4.flp";
        String powertrace = "HotSpot" + pathSeparator + "powertrace" + pathSeparator + "Alpha4.ptrace";
        String thermaltrace = "HotSpot" + pathSeparator + "thermaltrace" + pathSeparator + "thermal.ttrace";
        ;

        HotSpot hotSpot = new HotSpot(hotspot_path, VERBOSE);
        hotSpot.run(hotspot_config, floorplan, powertrace, thermaltrace);


    }
}
