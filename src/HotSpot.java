import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.Duration;
import java.time.Instant;

public class HotSpot {

    //./hotspot -c hotspot_a9.config -f ~/Desktop/Alpha4.flp -p ~/Desktop/Alpha4.ptrace -steady_file Alpha4.steady -model_type grid -o alpha4.ttrace

    String hotspot_path = "";
    String hotspot_config = " -c ";
    String floorplan = " -f ";
    String powertrace = " -p ";
    String steady_file = " -steady_file Alpha.steady";
    String model_type = " -model_type grid";
    String thermaltrace = " -o ";

    char[] progress = new char[]{'|', '/', '-', '\\'};


    boolean VERBOSE = false;

    public HotSpot(String hotspot_path, boolean VERBOSE) {
        this.hotspot_path += hotspot_path;
        this.VERBOSE = VERBOSE;
    }



    public void run(String hotspot_config, String floorplan, String powertrace, String thermaltrace) {
        this.hotspot_config = " -c ";
        this.floorplan = " -f ";
        this.powertrace = " -p ";
        this.steady_file = " -steady_file Alpha.steady";
        this.model_type = " -model_type grid";
        this.thermaltrace = " -o ";
        this.hotspot_config += hotspot_config;
        this.floorplan += floorplan;
        this.powertrace += powertrace;
        this.thermaltrace += thermaltrace;
        Process process =null;
        String command = hotspot_path + this.hotspot_config + this.floorplan + this.powertrace + steady_file + model_type + this.thermaltrace;
        if (VERBOSE)System.out.println(command);
        Runtime runtime = null;
        try {
            Instant start = Instant.now();
            runtime = Runtime.getRuntime();
            process = runtime.exec(command);
            InputStream stderr = process.getInputStream();
            InputStreamReader isr = new InputStreamReader(stderr);
            BufferedReader br = new BufferedReader(isr);
            String line = null;
            int i = 0;
            while ((line = br.readLine()) != null) {
                if (VERBOSE) System.out.println(line);
                else {
                    System.out.print("Processing HotSpot : " +  progressbar(i) + "\r");
                    i++;
                }
            }
            if (!VERBOSE)System.out.println();


            int exitVal = process.waitFor();
            Instant finish = Instant.now();
            long timeElapsed = Duration.between(start, finish).toMillis();
            System.out.println("[HotSpot Completed! -- Time Elapsed: " + timeElapsed +" ms]");
            if (VERBOSE) System.out.println("Process exitValue: " + exitVal);
            process.destroy();

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public String progressbar(int i){
        String progress="[";
        i= i%20;
        for (int j = 0; j < 20; j++) {
            if(j==i) progress += ">";
            else if (j<i) progress += "=";
            else progress += "-";
        }
        progress +="]";
        return progress;
    }
}
