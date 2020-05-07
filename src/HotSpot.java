import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class HotSpot {
    String command = "";
    //./hotspot -c hotspot_a9.config -f ~/Desktop/Alpha4.flp -p ~/Desktop/Alpha4.ptrace -steady_file Alpha4.steady -model_type grid -o alpha4.ttrace

    String hotspot_path = "";
    String hotspot_config = " -c ";
    String floorplan = " -f ";
    String powertrace = " -p ";
    String steady_file = " -steady_file Alpha.steady";
    String model_type = " -model_type grid";
    String thermaltrace = " -o ";

    boolean VERBOSE=false;

    public HotSpot(String hotspot_path, boolean VERBOSE) {
        this.hotspot_path += hotspot_path;
        this.VERBOSE=VERBOSE;
    }

    public void run(String hotspot_config, String floorplan, String powertrace, String thermaltrace) {
        this.hotspot_config += hotspot_config;
        this.floorplan += floorplan;
        this.powertrace += powertrace;
        this.thermaltrace += thermaltrace;
        command = hotspot_path + this.hotspot_config + this.floorplan + this.powertrace + steady_file + model_type + this.thermaltrace;
        Runtime runtime = Runtime.getRuntime();
//        String[] s = new String[] {hotspot_path , command};
        try {
            Process process=runtime.exec(command);

            if(VERBOSE) {
                InputStream stderr = process.getInputStream();
                InputStreamReader isr = new InputStreamReader(stderr);
                BufferedReader br = new BufferedReader(isr);
                String line = null;
                System.out.println("< HotSpot >");
                while ((line = br.readLine()) != null)
                    System.out.println(line);
                System.out.println("</ HotSpot >");
            }
            int exitVal = process.waitFor();
            System.out.println("Process exitValue: " + exitVal);

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
