/*******************************************************************************
 * Copyright © 2019 Porya Gohary
 * Written by Porya Gohary (Email: gohary@ce.sharif.edu)
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

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class dag_Reader {
    File xml;
    int cores;
    int levels;

    private String inputFile;
    private String outSchedFile;
    private String outPrismFile;
    private String outGenFile;
    private String outDotFile;
    private boolean bOutPrism;

    // Writing scheduling tables
    private String[][][] sched;
    private int hPeriod;
    private int nbCores;
    private int nbLevels;

    private Set<McDAG> dags;
    private McDAG dag;

    public dag_Reader(File input) throws ParserConfigurationException, IOException, SAXException {

        xml = input;
        dags = new HashSet<McDAG>();
        readXML();
        dag = dags.iterator().next();
        System.out.println("Number Of Tasks in Graph =  "+dag.getVertices().size());

//        for (Vertex a : dag.getVertices()) {
//            System.out.print(a.getName());
//            System.out.print(" >>>> ");
//            for (Edge e : a.getSndEdges()) {
//                System.out.print(e.getDest().getName() + "   ");
//            }
//            System.out.println();
//        }
        System.out.println("..................");

//        for (Vertex a : dag.getVertices()) {
//            System.out.print(a.getName());
//            System.out.print(" -----> ");
//            System.out.println(a.getLPL());
//
//
//        }
    }





    public void readXML() throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();

        // 	Root element
        Document doc = dBuilder.parse(xml);
        doc.getDocumentElement().normalize();

        // Extract number of cores
        NodeList cList = doc.getElementsByTagName("cores");
        Element c = (Element) cList.item(0);
        setNbCores(Integer.parseInt(c.getAttribute("number")));


        // Extract number of levels
        NodeList lList = doc.getElementsByTagName("levels");
        Element l = (Element) lList.item(0);
        setNbLevels(Integer.parseInt(l.getAttribute("number")));

        // Extract DAGs that constitute the system
        NodeList eList = doc.getElementsByTagName("mcdag");
        int count = 0;

        for (int d = 0; d < eList.getLength(); d++) {
            Element eDag = (Element) eList.item(d);
            McDAG dag	= new McDAG();
            dag.setId(count);
            dag.setDeadline(Integer.parseInt(eDag.getAttribute("deadline")));
            dag.setLevels(getNbLevels());
            // Instantiate the DAG
            int nb_actors = 0;

            // List of actors(Name of Node) in the DAG
            NodeList nList = eDag.getElementsByTagName("actor");
            for (int i = 0; i < nList.getLength(); i++) {
                Node n = nList.item(i);
                if (n.getNodeType() == Node.ELEMENT_NODE) {
                    Element e = (Element) n;
                    Vertex a;
                    int[] wcets = new int[getNbLevels()];

                    // Initialize all the WCET of a node
                    NodeList wList = e.getElementsByTagName("wcet");
                    for (int j = 0; j < getNbLevels(); j++) {
                        Node w = wList.item(j);
                        if (w.getNodeType() == Node.ELEMENT_NODE) {
                            Element we = (Element) w;

                            wcets[Integer.parseInt(we.getAttribute("number"))] = Integer.parseInt(we.getTextContent());
                        }
                    }

                    if (!isbOutPrism()) {
                        a = new VertexScheduling(nb_actors++, e.getAttribute("name"),getNbLevels());
                        a.setWcets(wcets);
                    } else {
                        a = new VertexAvailability(nb_actors++, e.getAttribute("name"), wcets);
                        ((VertexScheduling) a).setfProb(Double.parseDouble(e.getElementsByTagName("fprob").item(0).getTextContent()));
                    }

                    ((VertexScheduling) a).setGraphID(count);
                    ((VertexScheduling) a).setGraphDead(dag.getDeadline());
                    dag.getVertices().add(a);
                }
            }

            // List of fault tolerance mechanisms
            NodeList ftList = eDag.getElementsByTagName("ftm");
            for (int i = 0; i < ftList.getLength(); i++) {
                Node n = ftList.item(i);
                if (n.getNodeType() == Node.ELEMENT_NODE) {
                    Element e = (Element) n;

                    if (e.getAttribute("type").contains("voter")) {
                        // Initialize all the WCET of a node
                        NodeList wList = e.getElementsByTagName("wcet");
                        int[] wcets = new int[getNbLevels()];

                        for (int j = 0; j < getNbLevels(); j++) {
                            Node w = wList.item(j);
                            if (w.getNodeType() == Node.ELEMENT_NODE) {
                                Element we = (Element) w;

                                wcets[Integer.parseInt(we.getAttribute("number"))] = Integer.parseInt(we.getTextContent());
                            }
                        }
                        VertexAvailability a = new VertexAvailability(nb_actors++, e.getAttribute("name"), wcets);
                        a.setfMechanism(true);
                        a.setfMechType(VertexAvailability.VOTER);
                        a.setVotTask(e.getElementsByTagName("vtask").item(0).getTextContent());
                        ((VertexAvailability) dag.getNodebyName(e.getElementsByTagName("vtask").item(0).getTextContent())).setVoted(true);
                        a.setNbReplicas(Integer.parseInt(e.getElementsByTagName("replicas").item(0).getTextContent()));
                        dag.getVertices().add(a);
                    } else if (e.getAttribute("type").contains("mkfirm")) {
                        VertexAvailability a = (VertexAvailability) dag.getNodebyName(e.getAttribute("name"));
                        a.setfMechanism(true);
                        a.setfMechType(VertexAvailability.MKFIRM);
                        a.setM(Integer.parseInt(e.getElementsByTagName("m").item(0).getTextContent()));
                        a.setK(Integer.parseInt(e.getElementsByTagName("k").item(0).getTextContent()));
                        a.setVoted(true);
                    } else {
                        System.err.println("[WARNING] Uknown fault tolerant mechanism.");
                    }
                }
            }

            // List of connections
            NodeList ports = eDag.getElementsByTagName("ports");
            NodeList pList = ports.item(0).getChildNodes();
            for (int i = 0; i < pList.getLength(); i++) {
                Node n = pList.item(i);
                if (n.getNodeType() == Node.ELEMENT_NODE) {
                    Element e = (Element) n;
                    // Creating the edge adds it to the corresponding nodes
                    @SuppressWarnings("unused")
                    Edge ed = new Edge(dag.getNodebyName(e.getAttribute("srcActor")),
                            dag.getNodebyName(e.getAttribute("dstActor")));
                }
            }
            // dag.sanityChecks();
            dags.add(dag);
            count++;
        }

    }



    /* Getters and setters */

    public File getInputFile() {
        return xml;
    }

    public void setInputFile(File inputFile) {
        xml = inputFile;
    }

//    public Automata getAuto() {
//        return auto;
//    }

//    public void setAuto(Automata auto) {
//        this.auto = auto;
//    }

    public String getOutSchedFile() {
        return outSchedFile;
    }

    public void setOutSchedFile(String outSchedFile) {
        this.outSchedFile = outSchedFile;
    }

    public String getOutGenFile() {
        return outGenFile;
    }

    public void setOutGenFile(String outGenFile) {
        this.outGenFile = outGenFile;
    }

//    public MCSystemGenerator getUg() {
//        return ug;
//    }

//    public void setUg(MCSystemGenerator ug) {
//        this.ug = ug;
//    }

    public int getNbCores() {
        return nbCores;
    }

    public void setNbCores(int nbCores) {
        this.nbCores = nbCores;
    }

    public Set<McDAG> getDags() {
        return dags;
    }

    public void setDags(Set<McDAG> dags) {
        this.dags = dags;
    }

    public String getOutDotFile() {
        return outDotFile;
    }

    public void setOutDotFile(String outDotFile) {
        this.outDotFile = outDotFile;
    }

    public String getOutPrismFile() {
        return outPrismFile;
    }

    public void setOutPrismFile(String outPrismFile) {
        this.outPrismFile = outPrismFile;
    }

    public int gethPeriod() {
        return hPeriod;
    }

    public void sethPeriod(int hPeriod) {
        this.hPeriod = hPeriod;
    }

    public int getNbLevels() {
        return nbLevels;
    }

    public void setNbLevels(int nbLevels) {
        this.nbLevels = nbLevels;
    }

    public boolean isbOutPrism() {
        return bOutPrism;
    }

    public void setbOutPrism(boolean bOutPrism) {
        this.bOutPrism = bOutPrism;
    }

    public String[][][] getSched() {
        return sched;
    }

    public void setSched(String[][][] sched) {
        this.sched = sched;
    }

    public void setDag(McDAG dag) {
        this.dag = dag;
    }

    public McDAG getDag() {
        return dag;
    }
}
