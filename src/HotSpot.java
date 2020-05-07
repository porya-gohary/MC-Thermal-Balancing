import java.io.IOException;

public class HotSpot {
    String[] s = new String[]{};
    //./hotspot -c hotspot_a9.config -f ~/Desktop/Alpha4.flp -p ~/Desktop/Alpha4.ptrace -steady_file Alpha4.steady -model_type grid -o alpha4.ttrace

    String hotspot_path;
    String hotspot_config=" -c ";
    String floorplan=" -f ";
    String powertrace=" -p ";
    String steady_file=" -steady_file Alpha.steady";
    String model_type=" -model_type grid";
    String thermaltrace=" -o ";


    public void run(){
        Runtime runtime = Runtime.getRuntime();
        try
        {
            runtime.exec("");
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }
}
