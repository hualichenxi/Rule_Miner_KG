package com.zzq.ruleminer;

import java.util.ArrayList;
import java.util.List;

public class RuleGraph {
    final int DEFAULT_HASH_VALUE_IN = 775817;
    final int DEFAULT_HASH_VALUE_OUT = 581777;
    private int hashValue = 1;
    private Rule rule;
    private List<Node> nodeList;
    private List<Edge> edgeList;
    
    class Node {
        public int in = 0;
        public String str;
        public int out = 0;
    }
    
    class Edge {
        public int hashValue = 1;
        public String str;
        public boolean bhead = false;
        public Node in = null;
        public Node out = null;
    }
    
    public RuleGraph(Rule rule) {
        this.rule = rule;
        this.nodeList = new ArrayList<Node>();
        this.edgeList = new ArrayList<Edge>();
    }
    
    public int calcHash() {
        for (int i=0; i<rule.getTriples().size(); i++) {
            String[] triple = rule.getTriples().get(i);
            Node n1 = null;
            Node n2 = null;
            for (Node n : nodeList) {
                if (n.str.equals(triple[0])) {
                    n1 = n;
                }
                if (n.str.equals(triple[2])) {
                    n2 = n;
                }
            }
            if (n1 == null) {
                n1 = new Node();
                n1.str = triple[0];
                nodeList.add(n1);
            }
            if (n2 == null) {
                n2 = new Node();
                n2.str = triple[2];
                nodeList.add(n2);
            }
            n1.out++;
            n2.in++;
            Edge edge = new Edge();
            edge.str = triple[1];
            edge.in = n1;
            edge.out = n2;
            if (i == 0) {
                edge.hashValue *= 31;
            }
            edgeList.add(edge);
        }
        for (Edge edge : edgeList) {
            edge.hashValue += edge.in.in * DEFAULT_HASH_VALUE_IN;
            edge.hashValue += edge.in.out * DEFAULT_HASH_VALUE_OUT;
            edge.hashValue = edge.hashValue * 31 + edge.str.hashCode();
            edge.hashValue += edge.out.in * DEFAULT_HASH_VALUE_IN;
            edge.hashValue += edge.out.out * DEFAULT_HASH_VALUE_OUT;
            this.hashValue += edge.hashValue;
        }
        return this.hashValue;
    }
    
    public static void main(String[] args) {
        Rule q = new Rule(new String[]{("?a") ,("<isCitizenOf>"), ("?b")}, 1);
        q.add(new String[]{("?d") ,("<isCitizenOf>"), ("?b")});
        q.add(new String[]{("?a") ,("<influences>"), ("?d")});
        Rule q2 = new Rule(new String[]{("?a") ,("<isCitizenOf>"), ("?b")}, 1);
        q2.add(new String[]{("?e") ,("<isCitizenOf>"), ("?b")});
        q2.add(new String[]{("?a") ,("<influences>"), ("?e")});
        @SuppressWarnings("unused")
        boolean a = q.equals(q2);
        @SuppressWarnings("unused")
        int c = 1;
        c++;
//      Rule q = new Rule();
//      for (int i = 0;  i < 50; ++i) {
//          System.out.println(Arrays.toString(q.fullyUnboundTriplePattern()));
//      }
    }
}
