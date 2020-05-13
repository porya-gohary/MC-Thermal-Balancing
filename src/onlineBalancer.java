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
    int[] bps;
    //CPU of System
    CPU cpu;
    //DaG
    McDAG dag;




    //Calculate predict value for thermal balancing
    public double predict(int core,int start,int end,double t_cur){
        double t=0;
        String task=cpu.getRunningTask(core,end);
        double t_inf;
        int slack;
        int WC_HI;

        if(task==null) t_inf=45.0;
        else t_inf=dag.getNodebyName(task).getT_inf();
        slack=cpu.get_slack(core,start,end);
        WC_HI=dag.getNodebyName(task).getbigWCET();

        //Predict formula
        t=slack/((t_cur-t_inf)*WC_HI);
        return t;
    }


}
