package com.zzq.ruleminer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class MiningAssistant {
    
    private double minHeadCoverage = 0.01;
    
    private int maxLen = 3; // if there is only head atom, then maxLen = 1;
    
    private double minStdConfidence = 0.1;
    
    private double minPcaConfidence = 0.1;
    
    private KB kb = null;

	private int recursivityLimit = 3;

	//private int minSupportThreshold = 0;

	protected HashMap<String, Double> headCardinalities = null;
	
	private boolean allowConstants = false;
	
	private boolean enforceConstants = false;
	
	public boolean countAlwaysOnSubject = false;
	
	private long totalSubjectCount = 0;
	
	private long totalObjectCount = 0;
	
	public enum ConfidenceMetric {StdConfidence, PcaConfidence}
	
	private ConfidenceMetric confidenceMetric = ConfidenceMetric.StdConfidence;
    
    public MiningAssistant(KB kb) {
        this.setKb(kb);
        this.headCardinalities = new HashMap<String, Double>();
        buildRelationsDictionary();
    }

    public KB getKb() {
        return kb;
    }

    public void setKb(KB kb) {
        this.kb = kb;
    }
    
    public double getMinHeadCoverage() {
        return minHeadCoverage;
    }

    public void setMinHeadCoverage(double minHeadCoverage) {
        this.minHeadCoverage = minHeadCoverage;
    }

    public int getMaxLen() {
        return maxLen;
    }

    public void setMaxLen(int maxLen) {
        this.maxLen = maxLen;
    }

    public double getMinStdConfidence() {
        return minStdConfidence;
    }

    public void setMinStdConfidence(double minStdConfidence) {
        this.minStdConfidence = minStdConfidence;
    }

    public double getMinPcaConfidence() {
        return minPcaConfidence;
    }

    public void setMinPcaConfidence(double minPcaConfidence) {
        this.minPcaConfidence = minPcaConfidence;
    }
    
    public ConfidenceMetric getConfidenceMetric() {
        return confidenceMetric;
    }

    public void setConfidenceMetric(ConfidenceMetric confidenceMetric) {
        this.confidenceMetric = confidenceMetric;
    }

    public Collection<Rule> getInitialAtoms(double minSupportThreshold) {
    	Collection<Rule> out = new LinkedHashSet<Rule>();
    	List<String[]> otherProjectionTriples = new ArrayList<String[]>();
    	String[] projectionTriple = {"?x", "?y", "?z"};
    	Map<String, Int> relations = this.kb.countProjectionBindings(projectionTriple[1], projectionTriple, otherProjectionTriples);
    	
    	Rule query = new Rule();
    	String[] pattern = query.getTriplePattern();
    	for (Entry<String, Int> relation : relations.entrySet()) {
    		double cardinality = relations.get(relation.getKey()).value;
    		if(cardinality >= minSupportThreshold) {
    			// TODO
    			String[] succedent = pattern.clone();
    			succedent[1] = relation.getKey();
    			Rule candidate = new Rule(succedent, cardinality);
    			out.add(candidate);
    		}
    	}
    	return out;
    }

    public Collection<Rule> getDanglingAtoms(Rule rule, double minCardinality) {
    	Collection<Rule> output = new ArrayList<Rule>();
    	if(rule.getTriples().isEmpty())
    		return null;
    	if(rule.getRealLength() >= this.maxLen)
    		return output;
    	if(rule.getRealLength() == this.maxLen - 1) {
    		if(!rule.getOpenVars().isEmpty() && !this.allowConstants && !this.enforceConstants)
    			return output;
    	}
    	
    	List<String> joinVars = null;
    	List<String> openVars = rule.getOpenVars();
    	
        if(rule.isClosed()) {              
            joinVars = rule.getAllVars();
        } else {
            joinVars = openVars;
        }
        
    	int nPatterns = rule.getTriples().size();
    	
    	String[] edge = rule.getTriplePattern();

    	for(int joinPos = 0; joinPos <= 2; joinPos += 2) {
    	    for (String joinVar : joinVars) {
        	    String[] newTriple = edge.clone();
        	    newTriple[joinPos] = joinVar;
    	       
    	        rule.getTriples().add(newTriple);

                Map<String, Int> promisingRelations = this.kb.countProjectionBindings(newTriple[1], rule.getHead(), rule.getBody());
                rule.getTriples().remove(nPatterns);
                
                //int danglingPosition = (joinPos == 0 ? 2 : 0);
                //boolean boundHead = !KB.isVariable(rule.getTriples().get(0)[danglingPosition]);
                for (Entry<String, Int> relation : promisingRelations.entrySet()) {
                    long cardinality = relation.getValue().value;
                    if(cardinality < minCardinality) {
                        continue;
                    }
                    // language bias test
                    if (rule.cardinalityForRelation(relation.getKey()) >= recursivityLimit) {
                        continue;
                    }
                    newTriple[1] = relation.getKey();
                    
                    Rule candidate = new Rule(rule, newTriple, cardinality);

                    if (!candidate.isRedundantRecursive()) {
                       candidate.setHeadCoverage(candidate.getSupport() / this.headCardinalities.get(candidate.getHeadRelation()));
                       candidate.setSupportRatio(candidate.getSupport() / this.kb.getSize());
                       candidate.setParent(rule);  
                       output.add(candidate);
                    }
                }
    	    }
    	}
    	return output;
    }
    
    public Collection<Rule> getClosingAtoms(Rule rule, double minCardinality) {
    	Collection<Rule> output = new ArrayList<Rule>();
    	int nPatterns = rule.getTriples().size();
    	
    	if (nPatterns == 0)
    		return output;
    	
    	if (nPatterns >= this.maxLen)
    		return output;
    	
    	List<String> openVars = rule.getOpenVars();
    	List<String> allVars = rule.getAllVars();
    	List<String> srcVars;
    	List<String> destVars;
    	
    	if(allVars.size() < 2)
    		return output;
    	
    	if(rule.isClosed()) {
    		srcVars = allVars;
    		destVars = allVars;
    	} else {
    		srcVars = openVars;
    		if (openVars.size() > 2 && rule.getTriples().size() >= this.getMaxLen() - 1) {
    			return output;
    		} else if (openVars.size() == 2) {
    			destVars = openVars;
    		} else {
    			destVars = allVars;
    		}
    	}
    	
    	int []varSetups = new int[] {0, 2, 2, 0};
    	String[] newTriple = rule.getTriplePattern();
		String relationVariable = newTriple[1];
    	
    	for (int i=0; i<2; i++) {
    		int joinPos = varSetups[i * 2];
    		int closePos = varSetups[i * 2 + 1];
    		String joinVar = newTriple[joinPos];
    		String closeVar = newTriple[closePos];
    		
    		for (String srcVar : srcVars) {
    			newTriple[joinPos] = srcVar;
    			for (String destVar : destVars) {
    				if (!srcVar.equals(destVar)) {
    					newTriple[closePos] = destVar;
    					rule.add(newTriple);
    					Map<String, Int> promisingRelations = this.kb.countProjectionBindings(newTriple[1], rule.getHead(), rule.getBody());
    					rule.getTriples().remove(nPatterns);
    					
    					List<String> listOfPromisingRelations = new ArrayList<String>();
    					listOfPromisingRelations.addAll(promisingRelations.keySet());
    					//= promisingRelations.decreasingKeys();
						for(String relation: listOfPromisingRelations){
							long cardinality = promisingRelations.get(relation).value;
							if (cardinality < minCardinality) {
								continue;
							}
							
							// Language bias test
							if (rule.cardinalityForRelation(relation) >= this.recursivityLimit) {
								continue;
							}
							
//							if (this.bodyExcludedRelations != null 
//									&& this.bodyExcludedRelations.contains(relation)) {
//								continue;
//							}
//							
//							if (this.bodyTargetRelations != null 
//									&& !this.bodyTargetRelations.contains(relation)) {
//								continue;
//							}
							
							//Here we still have to make a redundancy check							
							newTriple[1] = relation;
							Rule candidate = new Rule(rule, cardinality);
							candidate.add(newTriple);

							if(!candidate.isRedundantRecursive()){
								candidate.setHeadCoverage((double)cardinality / (double)this.headCardinalities.get(candidate.getHeadRelation()));
								candidate.setSupportRatio((double)cardinality / (double)this.kb.getSize());
								candidate.setParent(rule);
								output.add(candidate);
							}
						}
    				}
					newTriple[1] = relationVariable;
    			}
    			newTriple[closePos] = closeVar;
    			newTriple[joinPos] = joinVar;
    		}
    	}
    	return output;
    }
    
    public Collection<Rule> getInstantiatedAtoms(Rule rule, Collection<Rule> danglingEdges, double minCardinality) {
    	Collection<Rule> output = new ArrayList<Rule>();
    	if (!canAddInstantiatedAtoms()) {
    		return output;
    	}
    	List<String> queryFreshVariables = rule.getOpenVars();
    	if (rule.getRealLength() < this.maxLen - 1 || queryFreshVariables.size() < 2) {
    		for (Rule candidate : danglingEdges) {
    			int lastTripplePatternIndex = candidate.getTriples().size();
    			String []lastTriplePattern = candidate.getTriples().get(lastTripplePatternIndex);
    			List<String> candidateFreshVariables = candidate.getOpenVars();
    			int danglingPosition = 0;
				if (candidateFreshVariables.contains(lastTriplePattern[0])) {
					danglingPosition = 0;
				} else if (candidateFreshVariables.contains(lastTriplePattern[2])) {
					danglingPosition = 2;
				}
				// getInstantiatedAtoms(candidate, candidate, lastTriplePatternIndex, danglingPosition, minSupportThreshold, output);
				String []danglingEdge = candidate.getTriples().get(lastTripplePatternIndex);
				// TODO The next line may have some problems...
				Map<String, Int> constants = this.kb.countProjectionBindings(danglingEdge[danglingPosition], candidate.getHead(), candidate.getBody());
				for (Entry<String, Int> constant : constants.entrySet()) {
					long cardinality = constant.getValue().value;
					if (cardinality >= minCardinality) {
						String[] lastPatternCopy = candidate.getTriples().get(lastTripplePatternIndex).clone();
						lastPatternCopy[danglingPosition] = constant.getKey();
						Rule cand = candidate.instantiateConstant(danglingPosition, constant.getKey(), cardinality);
						if (cand.getRedundantAtoms().isEmpty()) {
							cand.setHeadCoverage((double)cardinality / headCardinalities.get(candidate.getHeadRelation()));
							cand.setSupportRatio((double)cardinality / (double)getTotalCount(candidate));
							cand.setParent(candidate);					
							output.add(cand);
						}
					}
				}
    		}
    	}
    	return output;
    }
    
    public long getTotalCount(Rule candidate) {
		if(countAlwaysOnSubject){
			return totalSubjectCount;
		}else{
			int projVarPosition = candidate.getFunctionalVariablePosition();
			if(projVarPosition == 0)
				return totalSubjectCount;
			else if(projVarPosition == 2)
				return totalObjectCount;
			else
				return 1;
		}
    }
    
    public long getHeadCardinality(Rule rule){
        return headCardinalities.get(rule.getHeadRelation()).longValue();
    }
    
    public double getThreshold(Rule rule) {
        return Math.ceil((this.minHeadCoverage * (double) this.getHeadCardinality(rule)));
    }
    
    public boolean canAddInstantiatedAtoms() {
    	return this.allowConstants || this.enforceConstants;
    }
    
    public void buildRelationsDictionary() {
    	Map<String, Int> relations = this.kb.getRelations();
    	for (Entry<String, Int> relation : relations.entrySet()) {
    		this.headCardinalities.put(relation.getKey(), (double)relation.getValue().value);
    	}
    }
    
    public double computeStdConfidence(Rule rule) {
        if (rule.getTriples().isEmpty()) {
            return 0;
        }

        List<String[]> antecedent = new ArrayList<String[]>();
        antecedent.addAll(rule.getBody());
        
        double denominator = 0.0;
        String[] head = rule.getHead();
        if (!antecedent.isEmpty()){
            //Confidence
            try{
                if(KB.numVariables(head) == 2){
                    String var1, var2;
                    var1 = head[KB.getFirstVarPos(head)];
                    var2 = head[KB.getSecondVarPos(head)];
                    denominator = (double) computeStdBodySize(var1, var2, rule);
                } else {
                    denominator = (double) this.kb.countDistinct(rule.getFunctionalVariable(), antecedent);
                }               
                rule.setStdBodySize((long)denominator);
            }catch(UnsupportedOperationException e){
                
            }
        }
        return rule.getStdConfidence();
    }
    
    public double computePcaConfidence(Rule rule) {
        if (rule.getTriples().isEmpty()) {
            return 0;
        }
        
        List<String[]> antecedent = new ArrayList<String[]>();
        antecedent.addAll(rule.getTriples().subList(1, rule.getTriples().size()));
        String[] succedent = rule.getTriples().get(0);
        double pcaDenominator = 0.0;
        String[] existentialTriple = succedent.clone();
        int freeVarPos = 0;
        int noOfHeadVars = KB.numVariables(succedent);
        
        if(noOfHeadVars == 1){
            freeVarPos = KB.getFirstVarPos(succedent) == 0 ? 2 : 0;
        }else{
            if(existentialTriple[0].equals(rule.getFunctionalVariable()))
                freeVarPos = 2;
            else
                freeVarPos = 0;
        }

        existentialTriple[freeVarPos] = new String("?xw");
        if (!antecedent.isEmpty()) {
            antecedent.add(existentialTriple);
            try{
                if (noOfHeadVars == 1) {
                    pcaDenominator = (double) this.kb.countDistinct(rule.getFunctionalVariable(), antecedent);
                } else {
                    pcaDenominator = (double) this.kb.countDistinctPairs(succedent[0], succedent[2], antecedent);                   
                }
                rule.setPcaBodySize(pcaDenominator);
            }catch(UnsupportedOperationException e){
                
            }
        }
        
        return rule.getPcaConfidence();
    }

    protected long computeStdBodySize(String var1, String var2, Rule query){
        long result = this.kb.countDistinctPairs(var1, var2, query.getBody());
        return result;
    }
    
    protected double computePcaBodySize(String var1, String var2, Rule query, List<String[]> antecedent, String[] existentialTriple, int nonExistentialPosition) {      
        antecedent.add(existentialTriple);
        long result = this.kb.countDistinctPairs(var1, var2, antecedent);
        return result;      
    }
    
    public Collection<Rule> parentOfRule(Rule r) {
        return r.getAncestors();
    }
    
    public boolean acceptForOutput(Rule candidate) {
        if (!candidate.isClosed())
            return false;
        
        computeStdConfidence(candidate);
        computePcaConfidence(candidate);
        
        if (candidate.getPcaConfidence() < this.minPcaConfidence
                || candidate.getStdConfidence() < this.minStdConfidence)
            return false;
        if (candidate.containsLevel2RedundantSubgraphs())
            return false;
        Collection<Rule> parents = parentOfRule(candidate);
        for(Rule rule : parents) {
            double r1, r2;
           //System.out.println("parent rules:"+rule.toString()+"\t");
            if (confidenceMetric == ConfidenceMetric.StdConfidence) {
                r1 = candidate.getStdConfidence();
                r2 = rule.getStdConfidence();
                
            } else {
                r1 = candidate.getPcaConfidence();
                r2 = rule.getPcaConfidence();
            }
            if(rule.isClosed() && r1 <= r2) {
                return false;
            }
//            System.out.println("candidate:"+r1+"\t"+"parent:"+r2);
//            System.out.println(r1>r2);
        }
        return true;
    }
    
    public Collection<Rule> refine(Rule in) {
        double threshold = this.getThreshold(in);
        Collection<Rule> out = new LinkedHashSet<Rule>();
//        Rule rule = new Rule();
//        rule.add(new String[]{"?a", "<isCitizenOf>", "?b"});
//        rule.add(new String[]{"?c", "<isCitizenOf>", "?b"});
//        Rule rule2 = new Rule();
//        rule2.add(new String[]{"?a", "<isCitizenOf>", "?b"});
//        rule2.add(new String[]{"?c", "<isCitizenOf>", "?b"});
//        rule.equals(rule2);
        Collection<Rule> danglingAtoms = getDanglingAtoms(in, threshold);
        Collection<Rule> instantiatedAtoms = getInstantiatedAtoms(in, danglingAtoms, threshold);
        Collection<Rule> closingAtoms = getClosingAtoms(in, threshold);
        out.addAll(danglingAtoms);
        out.addAll(instantiatedAtoms);
        out.addAll(closingAtoms);
        return out;
    }
}
