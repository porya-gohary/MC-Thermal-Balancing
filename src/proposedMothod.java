import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

public class proposedMothod {

    Vertex v[];
    Set<Vertex> HIv;
    Vertex sorted_tasks[];

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
        //Sort Tasks from big to small by WCET LO
        sorted_tasks=sort_tasks(dag.getVertices().toArray(new Vertex[0]).clone());
        feasibility();
    }

    public void feasibility(){
        String Task[];
        do{
            Task=get_tasks();
            if (VERBOSE) System.out.println("-------------");
            for (int i = 0; i < Task.length; i++) {
                if(Task[i]==null) continue;
                Vertex v=dag.getNodebyName(Task[i]);
                v.setScheduled(v.getScheduled()+1);
                if (VERBOSE) System.out.println(v.getName()+" SCH: [ " +v.getScheduled()+ " / "+v.getReplica()+" ]  "+v.isDone());
            }
        }while(Task[0]!=null);
    }
    //Get Tasks for every blocks
    public String [] get_tasks() {
        //Make array for tasks in specific blocks
        String t[]=new String[n_core];
        Vertex s=null;
        int k=0;
        do {
            s=null;
            for (Vertex a : sorted_tasks) {
                boolean run_flag = true;
                if(a.isDone()) continue;
                for (Edge e : a.getRcvEdges()) {
                    if (!e.getSrc().isDone()) {
                        run_flag = false;
                        break;
                    }
                }
                if (!run_flag) continue;
                //Check for number of a task in the block
                else if(countOccurrences(t,a.getName())+a.getScheduled()+1 > a.getReplica()) continue;
                else {
                    s=a;
                    t[k]=s.getName();
                    k++;
                    if(k==4) break;
                }
            }
        }while(k!=4 && s!=null);
        return t;
    }

    //Sorting Tasks from big to small
    public Vertex[] sort_tasks(Vertex v[]) {
        Arrays.sort(v);
        Collections.reverse(Arrays.asList(v));

        if (VERBOSE) {
            //Show Sorted Vortex Array
            System.out.println("---------------");
            System.out.println("Sorted Tasks:");
            for (Vertex a : v) {
                System.out.println(a.getName() + "  " + a.getWcet(0));
            }
            System.out.println("---------------");
        }
        return v;
    }

    public int countOccurrences(String arr[], String x){
        int n=0;
        for (int i = 0; i < arr.length; i++) {
            if(arr[i]==x) n++;
        }
        return n;
    }
}
