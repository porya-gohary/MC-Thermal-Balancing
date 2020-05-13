import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
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
public class HS_input_creator {

    //CPU of System
    CPU cpu;

    public HS_input_creator(CPU cpu) {
        this.cpu = cpu;
    }

    public void Save(String mFolder, String Folder, String Filename) throws IOException {
        BufferedWriter outputWriter = null;
        outputWriter = new BufferedWriter(new FileWriter(mFolder + "//" + Folder + "//" + Filename));
        //Add HotSpot Header
        for (int i = 0; i < cpu.getN_Cores(); i++) {
            String s = (i != cpu.getN_Cores() - 1) ? "core_" + i + "\t" : "core_" + i + "\n";
            outputWriter.write(s);
        }

        //Add Power of each core
        for (int i = 0; i < cpu.getDeadline(); i++) {
            for (int j = 0; j < cpu.getN_Cores(); j++) {
                String s = (j != cpu.getN_Cores() - 1) ? cpu.get_power(j,i) + "\t" : cpu.get_power(j,i) + "\n";
                outputWriter.write(s);
            }
        }
        outputWriter.flush();
        outputWriter.close();
    }
}
