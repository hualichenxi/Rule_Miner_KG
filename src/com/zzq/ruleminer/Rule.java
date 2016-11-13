package com.zzq.ruleminer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;


public class Rule {
    
    private List<String[]> triples = new ArrayList<String[]>();
    
    private double stdConfidence = 0.0;
    private double pcaConfidence = 0.0;
    private double headCoverage = 0.0;
    
    private long support = 0;
    private double supportRatio = 0.0;
    
    private double stdConfidenceUpperBound = 0.0;
    private double pcaConfidenceUpperBound = 0.0;
    
//    private String[] head = new String[3];
//    private List<String[]> body = new ArrayList<String[]>();
    
    private List<Rule> ancestors;
    private Rule parent;
    
    private int varPatternCnt = 0;
    
    private KB kb = null;
    
    private String headKey = null;
    
    private int functionalVariablePosition;
    
    private double pcaBodySize = 0.0;
    private double stdBodySize = 0.0;

    public Rule() {
        this.setKb(KB.getInstance());
        this.ancestors = new ArrayList<Rule>();
    }
    
    public Rule(String[] atom, double cardinality) {
    	this();
    	this.setSupport((long)cardinality);
    	this.add(atom);
    	this.varPatternCnt = atom[2].charAt(1) - 'a' + 1;
    }
    
    public Rule(Rule rule, String[] atom, long support) {
    	this(rule, support);
    	this.add(atom);
    	this.varPatternCnt = atom[2].charAt(1) - 'a' + 1;
    }
    
    public Rule(Rule rule, long support) {
    	this();
    	this.stdConfidence = rule.stdConfidence;
    	this.pcaConfidence = rule.pcaConfidence;
    	this.headCoverage = rule.headCoverage;
    	this.setSupport(support);
    	this.supportRatio = rule.supportRatio;
    	this.stdConfidenceUpperBound = rule.stdConfidenceUpperBound;
    	this.pcaConfidenceUpperBound = rule.pcaConfidenceUpperBound;
    	for(String[] triple : rule.triples) {
    		this.add(triple);
    	}
    	this.varPatternCnt = rule.varPatternCnt;
    }
    
    public KB getKb() {
        return kb;
    }

    public void setKb(KB kb) {
        this.kb = kb;
    }
    
    public double getStdConfidence() {
        //return stdConfidence;
        return (double)this.support / this.stdBodySize;
    }

    public void setStdConfidence(double stdConfidence) {
        this.stdConfidence = stdConfidence;
    }

    public double getPcaConfidence() {
        //return pcaConfidence;
        return (double)this.support / this.pcaBodySize;
    }

    public void setPcaConfidence(double pcaConfidence) {
        this.pcaConfidence = pcaConfidence;
    }

    public double getHeadCoverage() {
        return headCoverage;
    }

    public void setHeadCoverage(double headCoverage) {
        this.headCoverage = headCoverage;
    }

    public double getSupportRatio() {
        return supportRatio;
    }

    public void setSupportRatio(double supportRatio) {
        this.supportRatio = supportRatio;
    }

    public double getStdConfidenceUpperBound() {
        return stdConfidenceUpperBound;
    }

    public void setStdConfidenceUpperBound(double stdConfidenceUpperBound) {
        this.stdConfidenceUpperBound = stdConfidenceUpperBound;
    }

    public long getSupport() {
		return support;
	}

	public void setSupport(long support) {
		this.support = support;
	}

	public double getPcaBodySize() {
        return pcaBodySize;
    }

    public void setPcaBodySize(double pcaBodySize) {
        this.pcaBodySize = pcaBodySize;
    }

    public double getStdBodySize() {
        return stdBodySize;
    }

    public void setStdBodySize(double stdBodySize) {
        this.stdBodySize = stdBodySize;
    }

    public double getPcaConfidenceUpperBound() {
        return pcaConfidenceUpperBound;
    }

    public void setPcaConfidenceUpperBound(double pcaConfidenceUpperBound) {
        this.pcaConfidenceUpperBound = pcaConfidenceUpperBound;
    }

    public String[] getHead() {
		return this.triples.get(0);
	}

	public List<String[]> getBody() {
		return this.triples.subList(1, this.triples.size());
	}

	public List<Rule> getAncestors() {
		return ancestors;
	}

	public void setAncestors(List<Rule> ancestors) {
		this.ancestors = ancestors;
	}

	public Rule getParent() {
		return parent;
	}

	public void setParent(Rule parent) {
		//this.parent = parent;
	    if (parent != null)
	        this.ancestors.add(parent);
	}

	public void add(String []str) {
        this.triples.add(str.clone());
    }
    
    public List<String[]> getTriples() {
        return this.triples;
    }
    
    public void add(String subject, String predicate, String object) {
        String []strSeq = {new String(subject), new String(predicate), new String(object)};
        this.add(strSeq);
    }
    
    public long calcSupport() {
        long count = 0;
        //for(Map<String, Map<String, Integer>> m2 : kb.)
        return count;
    }
    
    public int length() {
        return this.triples.size();
    }
    
    public boolean isPerfect() {
    	return (this.getPcaConfidence() >= 1.0 && this.pcaBodySize > 0);
    }
    
    public String toString() {
    	String out = new String();
    	String[] strs;
    	for (int i=this.triples.size() - 1; i>=0; i--) {
    	    strs = this.triples.get(i);
    	    if (i > 0)
    	        out = out + strs[0] + " " + strs[1] + " " + strs[2] + "\t";
    	    else
    	        out = out + "=>\t" + strs[0] + " " + strs[1] + " " + strs[2] + "\t";
    	}
    	return out;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) support;
        result = prime * result + (int) getRealLength();
        result = prime * result + ((getHeadKey() == null) ? 0 : getHeadKey().hashCode());
        return result;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (obj.getClass() != this.getClass())
            return false;
        Rule rule = (Rule)obj;
        if (rule.getTriples().size() != this.getTriples().size())
            return false;
//        for (String[] triple1 : this.getTriples()) {
//            boolean same = false;
//            for (String[] triple2 : rule.getTriples()) {
//                if (triple2[0].equals(triple1[0]) && triple2[1].equals(triple1[1]) && triple2[2].equals(triple1[2])) {
//                    same = true;
//                    break;
//                }
//            }
//            if (!same) {
//                return false;
//            }
//        }
        if (!this.getHead()[1].equals(rule.getHead()[1])) {
            return false;
        }
        RuleGraph rg1 = new RuleGraph(this);
        RuleGraph rg2 = new RuleGraph(rule);
        return rg1.calcHash() == rg2.calcHash();
    }
    
    public String[] getTriplePattern() {
    	String[] result = new String[3];
    	result[1] = "?p";
    	if(varPatternCnt < 26) {
    		result[0] = "?" + (char)('a' + varPatternCnt);
    	} else {
    		result[0] = "?" + (char)('a' + varPatternCnt % 26) + varPatternCnt / 26;
    	}
    	varPatternCnt++;
    	if(varPatternCnt < 26) {
    		result[2] = "?" + (char)('a' + varPatternCnt);
    	} else {
    		result[2] = "?" + (char)('a' + varPatternCnt % 26) + varPatternCnt / 26;
    	}
    	varPatternCnt++;
    	return result;
    }
       
    protected Map<String, Int> getVarHistogram() {
    	Map<String, Int> varHistogram = new HashMap<String, Int>();
    	for (String[] triple: triples) {
    		if (KB.isVariable(triple[0])) {
    			if (varHistogram.containsKey(triple[0])) {
    				varHistogram.get(triple[0]).value++;
    			} else {
    				varHistogram.put(triple[0], new Int(1));
    			}
    		}
    		if(!triple[2].equals(triple[0])) {
    			if (varHistogram.containsKey(triple[2])) {
    				varHistogram.get(triple[2]).value++;
    			} else {
    				varHistogram.put(triple[2], new Int(1));
    			}
    		}
    	}
    	return varHistogram;
    }
    
    public boolean isClosed() {
    	if (triples.isEmpty())
    		return false;
    	
    	Map<String, Int> varHistogram = getVarHistogram();
    	for(Entry<String, Int> var : varHistogram.entrySet()) {
    		if (varHistogram.get(var.getKey()).value < 2)
    			return false;
    	}
    	
    	return true;
    }
    
    public List<String> getOpenVars() {
    	List<String> openVars = new ArrayList<String>();
    	Map<String, Int> varGram = getVarHistogram();
    	for (Entry<String, Int> var : varGram.entrySet()) {
    		if (varGram.get(var.getKey()).value < 2) {
    			openVars.add(var.getKey());
    		}
    	}
    	return openVars;
    }

    public List<String> getAllVars() {
    	List<String> allVars = new ArrayList<String>();
    	for (String[] triple : this.triples) {
    		if (KB.isVariable(triple[0]) && !allVars.contains(triple[0])) {
    			allVars.add(triple[0]);
    		}
    		if (KB.isVariable(triple[2]) && !allVars.contains(triple[2])) {
    			allVars.add(triple[2]);
    		}
    	}
    	return allVars;
    }
    
    public static void main(String[] args) {
    	Rule q = new Rule();
    	for (int i = 0;  i < 50; ++i) {
    		System.out.println(Arrays.toString(q.getTriplePattern()));
    	}
    }
    
    public int cardinalityForRelation(String relation) {
        int count = 0;
        for (String[] triple : triples) {
            if (triple[1].equals(relation)) {
                ++count;
            }
        }
        return count;
    }
    
    protected boolean equals(String[] atom1, String[] atom2) {
    	return atom1[0].equals(atom2[0]) && atom1[1].equals(atom2[1]) && atom1[2].equals(atom2[2]);
    }
    
    public boolean isRedundantRecursive() {
    	List<String[]> redundantAtoms = getRedundantAtoms();
    	String[] lastPattern = this.triples.isEmpty() ? null : this.triples.get(this.triples.size() - 1);
        for (String[] redundantAtom : redundantAtoms) {
            if (equals(lastPattern, redundantAtom)) {
                return true;
            }
        }

        return false;
    }

    public List<String[]> getRedundantAtoms() {
    	String[] newAtom = this.triples.isEmpty() ? null : this.triples.get(this.triples.size() - 1);
        List<String[]> redundantAtoms = new ArrayList<String[]>();
        for (String[] pattern : triples) {
            if (pattern != newAtom) {
                if (isUnifiable(pattern, newAtom) || isUnifiable(newAtom, pattern)) {
                    redundantAtoms.add(pattern);
                }
            }
        }

        return redundantAtoms;
    }
    
    public boolean containsLevel2RedundantSubgraphs() {
        if (!isClosed() || triples.size() < 4 || triples.size() % 2 == 1) {
            return false;
        }

        Map<String, Int> relationCardinalities = new HashMap<String, Int>();
        for (String[] pattern : triples) {
            KB.MapAdd(relationCardinalities, pattern[1], 1);
        }

        for (Entry<String, Int> relation : relationCardinalities.entrySet()) {
            if (relation.getValue().value != 2) {
                return false;
            }
        }

        return true;
    }
    
    public static boolean isUnifiable(String[] pattern, String[] newAtom) {
        boolean unifiesSubject = pattern[0].equals(newAtom[0]) || KB.isVariable(pattern[0]);
        if (!unifiesSubject) {
            return false;
        }

        boolean unifiesPredicate = pattern[1].equals(newAtom[1]) || KB.isVariable(pattern[1]);
        if (!unifiesPredicate) {
            return false;
        }

        boolean unifiesObject = pattern[2].equals(newAtom[2]) || KB.isVariable(pattern[2]);
        if (!unifiesObject) {
            return false;
        }

        return true;
    }
    
    public String getHeadRelation() {
    	return this.triples.get(0)[1].toString();
    }
    
    public int getRealLength() {
        int length = 0;
        for (@SuppressWarnings("unused") String[] triple : triples) {
//            if (!triple[1].equals(KB.DIFFERENTFROMbs)) {
                ++length;
//            }
        }
        return length;
    }
    
    public String getHeadKey() {
    	if (headKey == null) {
    		computeHeadKey();
    	}
		return headKey;
	}

	public void setHeadKey(String headKey) {
		this.headKey = headKey;
	}

	public Rule instantiateConstant(int danglingPosition, String constant, long cardinality) {
        Rule newQuery = new Rule(this, cardinality);
        String[] lastNewPattern = newQuery.getTriples().get(newQuery.getTriples().size() - 1);
        lastNewPattern[danglingPosition] = constant;
        newQuery.computeHeadKey();
        return newQuery;
    }
    
    public void computeHeadKey() {
    	this.setHeadKey(this.triples.get(0)[1]);
    	if (!KB.isVariable(this.triples.get(0)[2])) {
    		setHeadKey(getHeadKey() + this.triples.get(0)[2]);
    	} else if (!KB.isVariable(this.triples.get(0)[0])) {
    		setHeadKey(getHeadKey() + this.triples.get(0)[0]);
    	}
    }
    
    public int getFunctionalVariablePosition() {
    	return this.functionalVariablePosition;
    }
    
    public String getFunctionalVariable() {
        return this.triples.get(0)[this.functionalVariablePosition];
    }
    
    public boolean contains(String[] triple, boolean head) {
        if (head) {
            String[] t = triples.get(0);
            if (t[0].equals(triple[0]) && t[1].equals(triple[1]) && t[2].equals(triple[2]))
                return true;
        } else {
            for (int i=1; i<triples.size(); i++) {
                String[] t = triples.get(i);
                if (t[0].equals(triple[0]) && t[1].equals(triple[1]) && t[2].equals(triple[2]))
                    return true;
            }
        }
        return false;
    }
}
