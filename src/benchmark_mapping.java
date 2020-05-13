import java.util.Arrays;
import java.util.Random;

public class benchmark_mapping {
    //DAG of Tasks
    McDAG dag;
    String benchmark[];
    int benchmark_time[];
    double t_inf[];

    public benchmark_mapping(McDAG dag, String[] benchmark, int[] benchmark_time, double[] t_inf) {
        this.dag = dag;
        this.benchmark = benchmark;
        this.benchmark_time = benchmark_time;
        this.t_inf = t_inf;
    }

    public void mapping() {
        Random rn = new Random();

        int t1 = 0;
        int t2 = 0;
        for (Vertex a : dag.getVertices()) {
            if (a.isHighCr()) {
                t1 = rn.nextInt(benchmark.length);
                a.setLO_name(benchmark[t1]);
                a.setWCET_LO(benchmark_time[t1]);
                //System.out.println("%%%% >> "+benchmark_time[t1]);
                t2 = rn.nextInt(benchmark.length);
                a.setHI_name(benchmark[t2]);
                a.setWCET_HI(benchmark_time[t2] + a.getWcet(0));
                a.setT_inf(Math.max(t_inf[t1], t_inf[t2]));
            } else {
                t1 = rn.nextInt(benchmark.length);
                a.setLO_name(benchmark[t1]);
                a.setWCET_LO(benchmark_time[t1]);
                a.setHI_name(null);
                a.setWCET_HI(0);
                a.setT_inf(t_inf[t1]);
            }


        }
    }

    public void cal_LPL() {
        int t = 1;
        for (int i = dag.getVertices().size() - 1; i >= 0; i--) {
            Vertex a = dag.getNodebyName("D0N" + i);
            //System.out.println("D0N"+i);
//            System.out.print(t+"/"+dag.getVertices().size()+"  Calculating LPL: "+a.getName()+" ");
            a.setLPL(LPtoLeaves(a));
//            System.out.println(a.getLPL());
            t++;
        }

    }

    public int cal_deadline(double n) {
        Vertex v[] = dag.getVertices().stream().toArray(Vertex[]::new).clone();
        Arrays.sort(v);
        Random rn = new Random();
        int m;
        do {
            // m = rn.nextInt((int) (n *n*1.6));
            m = rn.nextInt((int) (n * n * 0.4));
        } while (m == 0);
        return (v[v.length - 1].getLPL() * m);
    }

    // A Recursive Method For Finding Longest Path To Leaves for Vertex
    public int LPtoLeaves(Vertex vertex) {
        int LPL = 0;
        if (vertex.isExitNode()) {
            if (vertex.getWcet(0) > vertex.getWcet(1)) {
                //System.out.print("<->");
                return vertex.getWcet(0);
            } else {
                // System.out.print("<->");
                return vertex.getWcet(1);
            }
        }

        for (Edge e : vertex.getSndEdges()) {
            if (e.getDest().getName().equals(e.getSrc().getName())) continue;
            if (e.getDest().getLPL() > 0) {
                if (e.getDest().getLPL() > LPL) LPL = e.getDest().getLPL();
            } else if (LPtoLeaves(e.getDest()) > LPL) LPL = LPtoLeaves(e.getDest());
        }
        if (vertex.getWcet(0) > vertex.getWcet(1)) {
            LPL += vertex.getWcet(0);
        } else {
            LPL += vertex.getWcet(1);
        }

        return LPL;
    }

    public void debug() {

        System.out.println(">>>>>>   MAPPING DEBUG MODE <<<<<<<");
        for (Vertex a : dag.getVertices()) {
            System.out.println(a.getName());
            System.out.println("LO MODE Benchmark:  " + a.getLO_name());
            System.out.println("LO MODE Benchmark TIME:  " + a.getWcet(0));
            System.out.println("HI MODE Benchmark:  " + a.getHI_name());
            System.out.println("HI MODE Benchmark TIME:  " + a.getWcet(1));


        }
        System.out.println("---> Number of LO-critical Tasks =  " + (dag.getVertices().size() - dag.getNodes_HI().size()));
        System.out.println("---> Number of HI-critical Tasks =  " + dag.getNodes_HI().size());
        System.out.println("---> Total Number of Tasks =  " + dag.getVertices().size());
    }
}
