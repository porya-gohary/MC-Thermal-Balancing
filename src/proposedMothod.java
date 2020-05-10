import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

public class proposedMothod {

    Vertex v[];
    Set<Vertex> HIv;

    //Deadline
    int deadline;
    //Number of CPU Core
    int n_core;
    //Number of Redundancy
    double n;
    //DAG
    McDAG dag;
    CPU cpu;
    String xml_name;

    boolean VERBOSE = false;

    public proposedMothod(int deadline, int n_core, McDAG dag, String xml_name,boolean VERBOSE) {
        this.deadline = deadline;
        this.n_core = n_core;
        this.dag = dag;
        this.xml_name = xml_name;
        this.VERBOSE=VERBOSE;
        sort_tasks(dag.getVertices().toArray(new Vertex[0]).clone());
    }

    public void get_tasks() {
        for (Vertex a : dag.getVertices()) {
            boolean run_flag = true;
            for (Edge e : a.getRcvEdges()) {
                if (!e.getSrc().isDone()) {
                    run_flag = false;
                    break;
                }
            }
            if (!run_flag) continue;
            else {

            }
        }
    }

    //Sorting Tasks from big to small
    public void sort_tasks(Vertex v[]) {
        Arrays.sort(v);
        Collections.reverse(Arrays.asList(v));

        if(VERBOSE) {
            //Show Sorted Vortex Array
            System.out.println("---------------");
            System.out.println("Sorted Tasks:");
            for (Vertex a : v) {
                System.out.println(a.getName() + "  " + a.getWcet(0));
            }
            System.out.println("---------------");
        }

    }
}
