package com.zzq.ruleminer;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;
import java.util.HashMap;
import java.util.Collections;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.List;

// singleton
public class KB {
    private Map<String, Map<String, Map<String, Int> > > subjectRelationObjectMap = new HashMap<String, Map<String, Map<String, Int> > >();
    private Map<String, Map<String, Map<String, Int> > > subjectObjectRelationMap = new HashMap<String, Map<String, Map<String, Int> > >();
    private Map<String, Map<String, Map<String, Int> > > relationSubjectObjectMap = new HashMap<String, Map<String, Map<String, Int> > >();
    private Map<String, Map<String, Map<String, Int> > > relationObjectSubjectMap = new HashMap<String, Map<String, Map<String, Int> > >();
    private Map<String, Map<String, Map<String, Int> > > objectSubjectRelationMap = new HashMap<String, Map<String, Map<String, Int> > >();
    private Map<String, Map<String, Map<String, Int> > > objectRelationSubjectMap = new HashMap<String, Map<String, Map<String, Int> > >();
    
    private Map<String, Int> subjectSize = new HashMap<String, Int>();
    private Map<String, Int> relationSize = new HashMap<String, Int>();
    private Map<String, Int> objectSize = new HashMap<String, Int>();
    private long size;
    private static KB instance;
    
    /** Can instantiate a variable in a query with a value */
	public static class Instantiator implements Closeable {
		List<String[]> query;

		int[] positions;

		String variable;

		public Instantiator(List<String[]> q, String var) {
			positions = new int[q.size() * 3];
			int numPos = 0;
			query = q;
			variable = var;
			for (int i = 0; i < query.size(); i++) {
				for (int j = 0; j < query.get(i).length; j++) {
					if (query.get(i)[j].equals(variable))
						positions[numPos++] = i * 3 + j;
				}
			}

			if (numPos < positions.length)
				positions[numPos] = -1;
		}

		public List<String[]> instantiate(String value) {
			for (int i = 0; i < positions.length; i++) {
				if (positions[i] == -1)
					break;
				query.get(positions[i] / 3)[positions[i] % 3] = value;
			}
			return (query);
		}

		@Override
		public void close() {
			for (int i = 0; i < positions.length; i++) {
				if (positions[i] == -1)
					break;
				query.get(positions[i] / 3)[positions[i] % 3] = variable;
			}
		}
	}
    
    public KB() {
        KB.instance = this;
    }
    
    public static KB getInstance() {
        return KB.instance;
    }
    
    public long getSize() {
        return size;
    }
    
    public Map<String, Int> getRelations() {
    	return this.relationSize;
    }
    
    public void loadFile(List<File> files) {
        
        for(final File f : files) {
            try {
                loadFile(f);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    protected void loadFile(File file) throws IOException {
        String encoding="utf-8";
        
        if(file.isDirectory()) {
            for(File f : file.listFiles()) {
                loadFile(f);
                return;
            }
        }
        
        if(file.isFile() && file.exists()) {
            InputStreamReader reader = new InputStreamReader(
                new FileInputStream(file),
                encoding);
            String line = null;
            BufferedReader bufferedReader = new BufferedReader(reader);
            while((line = bufferedReader.readLine()) != null) {
                //System.out.println(line);
                if(line.endsWith(".")) {
                    line = line.substring(0, line.length() - 1);
                }
                String[] split = line.split("\t");
                if (split.length == 3)
                    add(split[0].trim(), split[1].trim(), split[2].trim());
                else if (split.length == 4)
                    add(split[1].trim(), split[2].trim(), split[3].trim());
//                System.out.println(split[0]+split[1]+split[2]);
//                System.out.println(split.length);
//                System.out.println(split[0].trim());
            }
            bufferedReader.close();
            reader.close();
        }
    }
    
    protected void add(String subject, String relation, String object) {
        add(subject, relation, object, subjectRelationObjectMap);
        add(subject, object, relation, subjectObjectRelationMap);
        add(relation, subject, object, relationSubjectObjectMap);
        add(relation, object, subject, relationObjectSubjectMap);
        add(object, subject, relation, objectSubjectRelationMap);
        add(object, relation, subject, objectRelationSubjectMap);
        
        Int it = subjectSize.get(subject);
        if(it == null) {
            it = new Int(0);
            subjectSize.put(subject, it);
        }
        it.value++;
        
        it = relationSize.get(relation);
        if(it == null) {
            it = new Int(0);
            relationSize.put(relation, it);
        }
        it.value++;
        
        it = objectSize.get(object);
        if(it == null) {
            it = new Int(0);
            objectSize.put(object, it);
        }
        it.value++;
        
        size++;
    }
    
    protected void add(String str1, String str2, String str3, Map<String, Map<String, Map<String, Int> > > mm) {
        Map<String, Map<String, Int> > m2 = mm.get(str1);
        if(m2 == null) {
            m2 = new HashMap<String, Map<String, Int> >();
            mm.put(str1, m2);
        }
        
        Map<String, Int> m3 = m2.get(str2);
        if(m3 == null) {
            m3 = new HashMap<String, Int>();
            m2.put(str2, m3);
        }
        
        Int it = m3.get(str3);
        if(it == null) {
            it = new Int(0);
            m3.put(str3, it);
        }
        it.value++;
    }
    
    public void init() {
        
    }
    
    public double functionality(String relation) {
        return relationSubjectObjectMap.get(relation).size() * 1.0
                / relationSize.get(relation).value;
    }
    
    public static boolean isVariable(String var) {
    	return (var.length() > 0 && var.charAt(0) == '?');
    }
    
    public static int numVariables(String[] triple) {
    	int cnt = 0;
    	for(int i=0; i<triple.length; i++) {
    		if (isVariable(triple[i])) {
    			cnt++;
    		}
    	}
    	return cnt;
    }
    
    // map, key1
    protected Map<String, Int> get(Map<String, Map<String, Map<String, Int>>> map, String key) {
    	Map<String, Map<String, Int>> m = map.get(key);
    	Map<String, Int> out = new HashMap<String, Int>();
    	for (Entry<String, Map<String, Int>> entry : m.entrySet())
    		out.put(entry.getKey(), new Int(entry.getValue().size()));
		return out;
	}
    
    // map, key1, key2
    protected Map<String, Int> get(Map<String, Map<String, Map<String, Int>>> map, String key1, String key2) {
    	Map<String, Map<String, Int>> mm = map.get(key1);
    	if(mm == null)
    		return new HashMap<String, Int>();
    	Map<String, Int> mm2 = mm.get(key2);
    	if (mm2 == null) {
    	    return new HashMap<String, Int>();
    	}
    	return mm2;
    }
    
    // ����String : frequency��Map��posָ��String��λ�ã�subject or relation or object����
    // select triple[pos] from map where &&{projectionTriple[not var] = Instantiated atom}
    public Map<String, Int> countBindings(int pos, String[] projectionTriple) {
    	if(numVariables(projectionTriple) == 1) {
    		resultsOneVariable(projectionTriple);
//    		switch (pos) {
//    		case 0:
//    			return relationObjectSubjectMap.get(projectionTriple[1]).get(projectionTriple[2]);
//    		case 1:
//    			return subjectObjectRelationMap.get(projectionTriple[0]).get(projectionTriple[2]);
//    		case 2:
//    			return subjectRelationObjectMap.get(projectionTriple[0]).get(projectionTriple[1]);
//    		}
    	} else if (numVariables(projectionTriple) == 2) {
	    	switch (pos) {
	    	case 0: // get subjects frequency
	    		if (isVariable(projectionTriple[1])) { // ?x ?r object
	    			return get(objectSubjectRelationMap, projectionTriple[2]);
	    		} else { // ?x relation ?y
	    			return get(relationSubjectObjectMap, projectionTriple[1]);
	    		}
	
	    	case 1: // get relations frequency
	    		if (isVariable(projectionTriple[1])) { // ?x ?r object
	    			return get(objectRelationSubjectMap, projectionTriple[2]);
	    		} else { // ?subject ?r ?y
	    			return get(subjectRelationObjectMap, projectionTriple[1]);
	    		}
	
	    	case 2: // get objects frequency
	    		if (isVariable(projectionTriple[1])) { // ?subject ?r ?y
	    			return get(subjectObjectRelationMap, projectionTriple[2]);
	    		} else { // ?x relation ?y
	    			return get(relationObjectSubjectMap, projectionTriple[1]);
	    		}
	    	}
    	} else if(numVariables(projectionTriple) == 3) {
    		switch (pos) {
    		case 0:
    			return subjectSize;
    		case 1:
    			return relationSize;
    		case 2:
    			return objectSize;
    		}
    	}
    	return null;
    }
    
    public Map<String, Int> countProjectionBindings(int pos, String[] projectionTriple, List<String[]> otherTriples) {
    	Map<String, Int> result = new HashMap<String, Int>();
    	switch (numVariables(projectionTriple)) {
    	case 1:
    		try (Instantiator insty = new Instantiator(otherTriples, projectionTriple[pos])) {
    			for (String inst : resultsOneVariable(projectionTriple).keySet()) {
    				if(existsBS(insty.instantiate(inst))) {
    					result.get(inst).value++;
    				}
    			}
    		} catch (Exception e) {
    			e.printStackTrace();
    		}
    		break;
    	case 2:
			int firstVar = getFirstVarPos(projectionTriple);
			int secondVar = getSecondVarPos(projectionTriple);
			Map<String, Map<String, Int>> instantiations = resultsTwoVariables(
					projectionTriple, 
					firstVar,
					secondVar);
			try (Instantiator insty1 = new Instantiator(otherTriples,
					projectionTriple[firstVar]);
					Instantiator insty2 = new Instantiator(otherTriples,
							projectionTriple[secondVar])) {
				for (String val1 : instantiations.keySet()) {
					insty1.instantiate(val1);
					for (String val2 : instantiations.get(val1).keySet()) {
						if (existsBS(insty2.instantiate(val2)))
							result.get(firstVar == pos ? val1 : val2).value++;
					}
				}
			} catch (Exception e) {
    			e.printStackTrace();
    		}
			break;
    	case 3:
    	default:
    		break;
    	}
    	return result;
    }
    
    public int firstVariableInCommon(String[] t1, String[] t2) {
		for (int i = 0; i < t1.length; ++i) {
			if (KB.isVariable(t1[i]) && getVarPos(t2, t1[i]) != -1)
				return i;
		}
		return -1;
	}
    
    public static void MapAdd(Map<String, Int> map, String key, long delta) {
    	Int val = map.get(key);
    	if (val == null) {
    		val = new Int(0);
    		map.put(key, val);
    	}
    	val.value += delta;
    }
    
    public static void MapIncrease(Map<String, Int> map, Set<String> keySet) {
    	for (String key : keySet) {
    		MapAdd(map, key, 1);
    	}
    }
    
    public static void MapIncrease(Map<String, Int> map, Map<String, Int> mm) {
    	for (Entry<String, Int> entry : mm.entrySet()) {
    		MapAdd(map, entry.getKey(), entry.getValue().value);
    	}
    }

    // the variable may be not in the projectionTriple. it returns the number of instantiation of projection triple
    public Map<String, Int> countProjectionBindings(String variable, String[] projectionTriple, List<String[]> otherTriples) {
    	int pos = Arrays.asList(projectionTriple).indexOf(variable);
    	if(otherTriples.isEmpty()) {
    		return countBindings(pos, projectionTriple);
    	}
    	if(pos != -1) {
    		return countProjectionBindings(pos, projectionTriple, otherTriples);
    	}
    	
    	/* modified by zzq*/
    	//projectionTriple = otherTriples.get(0);
    	//otherTriples.remove(0);
    	//return countProjectionBindings(variable, otherTriples.get(0), otherTriples.subList(1, otherTriples.size()));
    	
    	List<String[]> wholeQuery = new ArrayList<String[]>();
		wholeQuery.add(projectionTriple);
		wholeQuery.addAll(otherTriples);

		String instVar = null;
		int posRestrictive = mostRestrictiveTriple(wholeQuery);
		String[] mostRestrictive = posRestrictive != -1 ? wholeQuery
				.get(posRestrictive) : projectionTriple;
		Map<String, Int> result = new HashMap<String, Int>();
		int posInCommon = (mostRestrictive != projectionTriple) ? firstVariableInCommon(
				mostRestrictive, projectionTriple) : -1;
		int nHeadVars = numVariables(projectionTriple);

		// Avoid ground facts in the projection triple
		if (mostRestrictive == projectionTriple || posInCommon == -1
				|| nHeadVars == 1) {
			switch (numVariables(projectionTriple)) {
			case 1:
				instVar = projectionTriple[getFirstVarPos(projectionTriple)];
				try (Instantiator insty = new Instantiator(otherTriples,
						instVar)) {
					for (String inst : resultsOneVariable(projectionTriple).keySet()) {
						MapIncrease(result, selectDistinct(variable, insty.instantiate(inst)));
					}
				}
				break;
			case 2:
				int firstVar = getFirstVarPos(projectionTriple);
				int secondVar = getSecondVarPos(projectionTriple);
				Map<String, Map<String, Int>> instantiations = resultsTwoVariables(
						projectionTriple, firstVar, secondVar);
				try (Instantiator insty1 = new Instantiator(otherTriples,
						projectionTriple[firstVar]);
						Instantiator insty2 = new Instantiator(otherTriples,
								projectionTriple[secondVar])) { 
					for (String val1 : instantiations.keySet()) {
						insty1.instantiate(val1);
						for (String val2 : instantiations.get(val1).keySet()) {
						    Set<String> distinct = selectDistinct(variable,
									insty2.instantiate(val2));
							MapIncrease(result, distinct);
						}
					}
				}
				break;
			case 3:
			default:
//				throw new UnsupportedOperationException(
//						"3 variables in the projection triple are not yet supported: "
//								+ toString(projectionTriple) + ", "
//								+ toString(otherTriples));
			}
		} else {
			List<String[]> otherTriples2 = new ArrayList<String[]>(
					wholeQuery);
			List<String[]> projectionTripleList = new ArrayList<String[]>(
					1);
			projectionTripleList.add(projectionTriple);
			otherTriples2.remove(projectionTriple);
			// Iterate over the most restrictive triple
			switch (numVariables(mostRestrictive)) {
			case 1:
				// Go for an improved plan, but remove the bound triple
				otherTriples2.remove(mostRestrictive);
				instVar = mostRestrictive[getFirstVarPos(mostRestrictive)];
				try (Instantiator insty1 = new Instantiator(otherTriples2,
						instVar);
						Instantiator insty2 = new Instantiator(
								projectionTripleList, instVar)) {
					for (String inst : resultsOneVariable(mostRestrictive).keySet()) {
						MapIncrease(result, countProjectionBindings(variable, 
								insty2.instantiate(inst).get(0),
								insty1.instantiate(inst)));
					}
				}
				break;
			case 2:
				int projectionPosition = getVarPos(
						projectionTriple, mostRestrictive[posInCommon]);
				// If the projection triple has two variables, bind the common
				// variable without problems
				if (nHeadVars == 2) {
					try (Instantiator insty1 = new Instantiator(otherTriples2,
							mostRestrictive[posInCommon]);
							Instantiator insty3 = new Instantiator(
									projectionTripleList,
									projectionTriple[projectionPosition])) {
						Map<String, Int> instantiations = countBindings(
								posInCommon, mostRestrictive);
						for (String b1 : instantiations.keySet()) {
							//System.out.println(variable + insty3.instantiate(b1).get(0) + insty1.instantiate(b1));
							MapIncrease(result, countProjectionBindings(variable,
									insty3.instantiate(b1).get(0),
									insty1.instantiate(b1)));
						}
					}
				} else if (nHeadVars == 1) {
					instVar = projectionTriple[getFirstVarPos(projectionTriple)];
					try (Instantiator insty = new Instantiator(otherTriples,
							instVar)) {
						for (String inst : resultsOneVariable(projectionTriple).keySet()) {
							MapIncrease(result, selectDistinct(variable,
									insty.instantiate(inst)));
						}
					}
				}
				break;
			case 3:
			default:
//				throw new UnsupportedOperationException(
//						"3 variables in the most restrictive triple are not yet supported: "
//								+ toString(mostRestrictive) + ", "
//								+ toString(wholeQuery));
			}
		}
		return result;
    }
    
 // returns the distinct instances of the variable;
    private Set<String> selectDistinct(String variable, List<String[]> query) {
        if (query.size() == 1) {
            String[] triple = query.get(0);
            switch(numVariables(triple)) {
            case 0:
                return Collections.emptySet();
            case 1:
                return resultsOneVariable(triple).keySet();
            case 2:
                int firstVarPos = getFirstVarPos(triple);
                int secondVarPos = getSecondVarPos(triple);
                if (triple[firstVarPos].equals(variable)) {
                    return resultsTwoVariables(triple, firstVarPos, secondVarPos).keySet();
                } else {
                    return resultsTwoVariables(triple, secondVarPos, firstVarPos).keySet();
                }
            case 3:
            default:
                if (triple[0].equals(variable)) {
                    return subjectSize.keySet();
                } else if (triple[1].equals(variable)) {
                    return relationSize.keySet();
                } else if (triple[2].equals(variable)) {
                    return objectSize.keySet();
                }
            }
        }
        
        Set<String> result = new LinkedHashSet<String>();
        int bestPos = mostRestrictiveTriple(query);
        if (bestPos == -1) {
            return Collections.emptySet();
        }
        String[] best = query.get(bestPos);
        if (best[0].equals(variable)
                || best[1].equals(variable)
                || best[2].equals(variable)) {
            switch (numVariables(best)) {
            case 1:
                List<String[]> rem = new ArrayList<String[]>(query);
                rem.remove(bestPos);
                try (Instantiator insty = new Instantiator(rem, variable)) {
                    for (String inst : resultsOneVariable(best).keySet()) {
                        if (existsBS(insty.instantiate(inst)))
                            result.add(inst);
                        insty.close();
                    }
                }
                break;
            case 2:
                int firstVar = getFirstVarPos(best);
                int secondVar = getSecondVarPos(best);
                Map<String, Map<String, Int>> instantiations =
                        best[firstVar].equals(variable)
                        ? resultsTwoVariables(best, firstVar, secondVar)
                                : resultsTwoVariables(best, secondVar, firstVar);
                try (Instantiator insty = new Instantiator(query, variable)) {
                    for (String val : instantiations.keySet()) {
                        if (existsBS(insty.instantiate(val)))
                            result.add(val);
                        insty.close();
                    }
                }
                break;
            case 3:
            default:
                rem = new ArrayList<String[]>(query);
                rem.remove(bestPos);
                try (Instantiator insty = new Instantiator(rem, variable)) {
                    int varPos = getVarPos(best, variable);
                    int var1, var2, var3;
                    switch (varPos) {
                    case 0 :
                        var1 = 0;
                        var2 = 1;
                        var3 = 2;
                        break;
                    case 1 :
                        var1 = 1;
                        var2 = 0;
                        var3 = 2;
                        break;
                    case 2 : default :
                        var1 = 2;
                        var2 = 0;
                        var3 = 1;
                        break;                          
                    }

                    for (String inst : resultsThreeVariables(best, var1, var2, var3).keySet()) {
                        if (existsBS(insty.instantiate(inst)))
                            result.add(inst);
                    }
                }
                break;
            }
            return result;
        }
        
        List<String[]> others = new ArrayList<String[]>(query);
        others.remove(bestPos);
        switch (numVariables(best)) {
        case 0:
            return (selectDistinct(variable, others));
        case 1:
            String var = best[getFirstVarPos(best)];
            try (Instantiator insty = new Instantiator(others, var)) {
                for (String inst : resultsOneVariable(best).keySet()) {
                    Set<String> set = selectDistinct(variable,
                        insty.instantiate(inst));
                    result.addAll(set);
                }
            }
            break;
        case 2:
            int firstVar = getFirstVarPos(best);
            int secondVar = getSecondVarPos(best);
            Map<String, Map<String, Int>> instantiations = resultsTwoVariables(best, firstVar, secondVar);
            try (Instantiator insty1 = new Instantiator(others, best[firstVar]);
                    Instantiator insty2 = new Instantiator(others,
                            best[secondVar])) {
                for (String val1 : instantiations.keySet()) {
                    insty1.instantiate(val1);
                    for (String val2 : instantiations.get(val1).keySet()) {
                        result.addAll(selectDistinct(variable,
                                insty2.instantiate(val2)));
                    }
                }
            }
            break;
        case 3:
        default:
            firstVar = getFirstVarPos(best);
            secondVar = getSecondVarPos(best);
            Map<String, Map<String, Map<String, Int>>> map = 
                    resultsThreeVariables(best, 0, 1, 2);
            try (Instantiator insty1 = new Instantiator(others, best[0]);
                    Instantiator insty2 = new Instantiator(others, best[1]);
                        Instantiator insty3 = new Instantiator(others, best[2])) {
                for (String val1 : map.keySet()) {
                    insty1.instantiate(val1);
                    instantiations = map.get(val1);
                    for (String val2 : instantiations.keySet()) {
                        insty2.instantiate(val2);
                        Map<String, Int> instantiations2 = instantiations.get(val2);
                        for (String val3 : instantiations2.keySet()) {
                            result.addAll(selectDistinct(variable, insty3.instantiate(val3)));
                        }
                    }
                }
            }
            break;

        }
        return (result);
    }
    
    public long countDistinct(String variable, List<String[]> query) {
        return (long) (selectDistinct(variable, query).size());
    }
    
    public long countDistinctPairs(String var1, String var2,
                                   List<String[]> query) {
        long result = 0;

        try (Instantiator insty1 = new Instantiator(query, var1)) {
            Set<String> bindings = selectDistinct(var1, query);
            for (String val1 : bindings) {
                result += countDistinct(var2, insty1.instantiate(val1));
            }
        }

        return (result);
    }

	public long countProjection(String[] projectionTriple, List<String[]> otherTriples) {
    	if (otherTriples.isEmpty()) {
    		return count(projectionTriple);
    	}
    	switch (numVariables(projectionTriple)) {
		case 0:
			return (count(projectionTriple));
		case 1:
			long counter = 0;
			String variable = projectionTriple[getFirstVarPos(projectionTriple)];
			try (Instantiator insty = new Instantiator(otherTriples, variable)) {
				for (String inst : resultsOneVariable(projectionTriple).keySet()) {
					if (existsBS(insty.instantiate(inst)))
						counter++;
				}
			}
			return (counter);
		case 2:
			counter = 0;
			int firstVar = getFirstVarPos(projectionTriple);
			int secondVar = getSecondVarPos(projectionTriple);
			Map<String, Map<String, Int>> instantiations = resultsTwoVariables(projectionTriple, firstVar, secondVar);
			try (Instantiator insty1 = new Instantiator(otherTriples,
					projectionTriple[firstVar])) {
				for (String val1 : instantiations.keySet()) {
					try (Instantiator insty2 = new Instantiator(
							insty1.instantiate(val1),
							projectionTriple[secondVar])) {
						for (String val2 : instantiations.get(val1).keySet()) {
							if (existsBS(insty2.instantiate(val2)))
								counter++;
						}
					}
				}
			}
			return (counter);
		}
    	return -1;
    }
    
    protected boolean contains(String[] fact) {
        //System.out.println(fact[0] + fact[1] + fact[2]);
    	return get(subjectRelationObjectMap, fact[0], fact[1]).containsKey(fact[2]);
    }
    
    protected long count(String... triples) {
    	switch(numVariables(triples)) {
    	case 0:
    		return contains(triples) ? 1 : 0;
    	case 1:
    		return countOneVariable(triples);
    	case 2:
    		return countTwoVariable(triples);
    	case 3:
    		return getSize();
    	}
    	return -1;
    }
    
    protected long countOneVariable(String[] triple) {
    	return resultsOneVariable(triple).size();
    }    
    
    protected long countTwoVariable(String[] triple) {
    	Int res = null;
    	if(!isVariable(triple[0])) {
    		res = subjectSize.get(triple[0]);
    	} else if(!isVariable(triple[1])) {
    		res = relationSize.get(triple[1]);
    	} else if(!isVariable(triple[2])) {
    		res = objectSize.get(triple[2]);
    	}
    	if (res == null)
    		return 0;
    	return res.value;
    }
    
    protected int getVarPos(String[] triple, String variable) {
    	for(int i=0; i<triple.length; i++) { 
    		if(triple[i].equals(variable))
    			return i;
    	}
    	return -1;
    }
    
    public static int getFirstVarPos(String[] triple) {
    	for(int i=0; i<triple.length; i++) { 
    		if(isVariable(triple[i]))
    			return i;
    	}
    	return -1;
    }
    
    public static int getSecondVarPos(String[] triple) {
    	for(int i=getFirstVarPos(triple) + 1; i<triple.length; i++) { 
    		if(isVariable(triple[i]))
    			return i;
    	}
    	return -1;
    }
    
    protected int mostRestrictiveTriple(List<String[]> triples) {
    	int bestPos = -1;
		long cnt = Long.MAX_VALUE;
		for (int i = 0; i < triples.size(); i++) {
			long myCount = count(triples.get(i));
			if (myCount >= cnt)
				continue;
			if (myCount == 0)
				return (-1);
			bestPos = i;
			cnt = myCount;
		}
		return (bestPos);
	}
    
    protected int mostRestrictiveTriple(List<String[]> triples, String variable) {
    	int bestPos = -1;
		long cnt = Long.MAX_VALUE;
		for (int i = 0; i < triples.size(); i++) {
			if (getVarPos(triples.get(i), variable) == -1) {
				continue;
			}
			long myCount = count(triples.get(i));
			if (myCount >= cnt)
				continue;
			if (myCount == 0)
				return (-1);
			bestPos = i;
			cnt = myCount;
		}
		return (bestPos);
	}
    
    // If there is a fact that supports the triples rule
    protected boolean existsBS(List<String[]> triples) {
    	if(triples.isEmpty())
    		return false;
    	if(triples.size() == 1) {
    		return count(triples.get(0)) > 0;
    	}
    	int bestPos = mostRestrictiveTriple(triples);
    	if (bestPos == -1) {
    		return false;
    	}

        List<String[]> rem = new ArrayList<String[]>(triples);
    	String[] bestTriple = triples.get(bestPos);
    	switch (numVariables(bestTriple)) {
    	case 0:
    		if(!contains(bestTriple)) {
    			return false;
    		}
    		rem.remove(bestPos);
    		return existsBS(rem);
    	case 1:
    		rem.remove(bestPos);
    		int firstVarIdx = getFirstVarPos(bestTriple);
    		try (Instantiator insty = new Instantiator(rem, bestTriple[firstVarIdx])) {
    			for (String inst : resultsOneVariable(bestTriple).keySet()) {
    				if(existsBS(insty.instantiate(inst))) {
    					return true;
    				}
    			}
    		}
    		return false;
		case 2:
			int firstVar = getFirstVarPos(bestTriple);
			int secondVar = getSecondVarPos(bestTriple);
			Map<String, Map<String, Int>> instantiations = resultsTwoVariables(bestTriple, firstVar, secondVar);
			rem.remove(bestPos);
			try (Instantiator insty1 = new Instantiator(rem,
					bestTriple[firstVar]);
					Instantiator insty2 = new Instantiator(rem,
							bestTriple[secondVar])) {
				for (String val1 : instantiations.keySet()) {
					insty1.instantiate(val1);
					for (String val2 : instantiations.get(val1).keySet()) {
						if (existsBS(insty2.instantiate(val2)))
							return (true);
					}
				}
			}
			return false;
		case 3:
		default:
			return getSize() > 0;
    	}
    }
    
    protected Map<String, Int> resultsOneVariable(String... projectionTriple) {
    	Map<String, Int> output = null;
    	Map<String, Map<String, Int>> outputTmp = null;
		if (isVariable(projectionTriple[0])) {
			outputTmp = relationObjectSubjectMap.get(projectionTriple[1]);
			if (outputTmp!=null)
				output = outputTmp.get(projectionTriple[2]);
		} else if (isVariable(projectionTriple[1])) {
			outputTmp = objectSubjectRelationMap.get(projectionTriple[2]);
			if (outputTmp!=null)
				output = outputTmp.get(projectionTriple[0]);
		} else if (isVariable(projectionTriple[2])) {
			outputTmp = subjectRelationObjectMap.get(projectionTriple[0]);
			if (outputTmp!=null)
				output = outputTmp.get(projectionTriple[1]);
		}
    	if (output == null) {
    		output = new HashMap<String, Int>();
    	}
    	return output;
    }
    
    protected Map<String, Map<String, Int>> resultsTwoVariables(String[] triple, int pos1, int pos2) {
    	Map<String, Map<String, Int>> output = null;
    	switch (pos1) {
    	case 0:
    		switch (pos2) {
    		case 1:
    			output = objectSubjectRelationMap.get(triple[2]);
    			break;
    		case 2:
    			output = relationSubjectObjectMap.get(triple[1]);
    			break;
    		}
			break;
    	case 1:
    		switch (pos2) {
    		case 0:
    			output = objectRelationSubjectMap.get(triple[2]);
    			break;
    		case 2:
    			output = subjectRelationObjectMap.get(triple[0]);
    			break;
    		}
			break;
    	case 2:
    		switch (pos2) {
    		case 0:
    			output = relationObjectSubjectMap.get(triple[1]);
    			break;
    		case 1:
    			output = subjectObjectRelationMap.get(triple[0]);
    			break;
    		}
			break;
    	}
    	if (output == null) {
    		output = new HashMap<String, Map<String, Int>>();
    	}
    	return output;
    }
    
    protected Map<String, Map<String, Map<String, Int>>> resultsThreeVariables(String[] triples, int pos1, int pos2, int pos3) {
    	switch (pos1) {
    	case 0:
    		switch (pos2) {
    		case 1:
    			return subjectRelationObjectMap;
    		case 2:
    			return subjectObjectRelationMap;
    		}
    	case 1:
    		switch (pos2) {
    		case 0:
    			return relationSubjectObjectMap;
    		case 2:
    			return relationObjectSubjectMap;
    		}
    	case 2:
    		switch (pos2) {
    		case 0:
    			return objectSubjectRelationMap;
    		case 1:
    			return objectRelationSubjectMap;
    		}
    	}
    	return null;
    }
}
