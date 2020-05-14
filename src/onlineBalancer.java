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

    public onlineBalancer(Integer[] bps, CPU cpu, McDAG dag) {
        this.bps = bps;
        this.cpu = cpu;
        this.dag = dag;
    }

    public void run(){
        for (int i = 0; i < bps.length-1; i++) {
            for (int j = 0; j < cpu.getN_Cores(); j++) {
                System.out.println("Core "+j+"\tTask Name "+cpu.getRunningTask(j,bps[i+1]-1));
                System.out.println(">>>>> "+predict(j,bps[i],bps[i+1]-1,45));
            }
        }
    }

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
        if(task==null) WC_HI=0;
        else WC_HI=dag.getNodebyName(task).getbigWCET();

        //Predict formula
        if(t_cur>t_inf)t=(slack+1)/((Math.abs(t_cur-t_inf)+1)*(WC_HI+1));
        else t=(-1)*(slack+1)/((Math.abs(t_cur-t_inf)+1)*(WC_HI+1));

        return t;
    }


}
