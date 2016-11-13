package com.zzq.ruleminer;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

import com.zzq.ruleminer.MiningAssistant.ConfidenceMetric;

public class RuleMiner {
    
    private KB kb;
    private MiningAssistant miningAssistant;
    public static String filename;
    public static String pathname;
    
    public RuleMiner() {
        
    }
    
    public boolean init(String[] args) {        
        CommandLineParser parser = new PosixParser();
        Options options = new Options();

        @SuppressWarnings("static-access")
        Option headCoverageOpt = OptionBuilder.withArgName("min-head-coverage")
                .hasArg()
                .withDescription("Minimum head coverage. Default: 0.01")
                .create("minhc");

        @SuppressWarnings("static-access")
        Option maxDepthOpt = OptionBuilder.withArgName("max-depth")
                .hasArg()
                .withDescription("Maximum number of atoms in the antecedent and succedent of rules. "
                		+ "Default: 3")
                .create("maxad");

        @SuppressWarnings("static-access")
        Option stdConfThresholdOpt = OptionBuilder.withArgName("min-std-confidence")
                .hasArg()
                .withDescription("Minimum standard confidence threshold. "
                		+ "This value is not used for pruning, only for filtering of the results. Default: 0.0")
                .create("minc");

        @SuppressWarnings("static-access")
        Option pcaConfThresholdOpt = OptionBuilder.withArgName("min-pca-confidence")
                .hasArg()
                .withDescription("Minimum PCA confidence threshold. "
                        + "This value is not used for pruning, only for filtering of the results. "
                        + "Default: 0.0")
                .create("minpca");

        options.addOption(stdConfThresholdOpt);
        options.addOption(pcaConfThresholdOpt);
        options.addOption(headCoverageOpt);
        options.addOption(maxDepthOpt);
        
        CommandLine cli = null;

        try {
            cli = parser.parse(options, args);
        } catch (ParseException e) {
            System.out.println("Unexpected exception: " + e.getMessage());
            return false;
        }
        kb = new KB();
        String[] leftOverArgs = cli.getArgs();
        List<File> files = new ArrayList<File>();
        for (String str : leftOverArgs) {
        	File file = new File(str);
        	files.add(file);
        	String tmp1[]=str.split("/|_");
        	filename=tmp1[tmp1.length-1];
        	pathname=str+"_rule";
        	//String tmp2[]=tmp1[tmp1.length-1].split();
        	//System.out.println(tmp1.length);
        	//System.out.println(tmp1[tmp1.length-1]);
        }
        if (files.isEmpty()) {
            System.out.println("Please add file(s)!");
            return false;
        }
        kb.loadFile(files);
        kb.init();
        
        miningAssistant = new MiningAssistant(kb);
        miningAssistant.setConfidenceMetric(ConfidenceMetric.StdConfidence);
        
        if (cli.hasOption("minhc")) {
            String minHeadCoverage = cli.getOptionValue("minhc");
            miningAssistant.setMinHeadCoverage(Double.parseDouble(minHeadCoverage));
        }

        if (cli.hasOption("minc")) {
            String minConfidenceStr = cli.getOptionValue("minc");
            miningAssistant.setMinStdConfidence(Double.parseDouble(minConfidenceStr));
        }

        if (cli.hasOption("minpca")) {
            String minicStr = cli.getOptionValue("minpca");
            miningAssistant.setMinPcaConfidence(Double.parseDouble(minicStr));
        }

        if (cli.hasOption("maxad")) {
            String maxDepthStr = cli.getOptionValue("maxad");
            miningAssistant.setMaxLen(Integer.parseInt(maxDepthStr));
        }
        
        return true;
    }
    
    public Collection<Rule> mining() {
        Collection<Rule> out = new LinkedHashSet<Rule>();
        Collection<Rule> q = miningAssistant.getInitialAtoms(100);
        
        ArrayList<String>  out_s_one = new ArrayList<String> (); 
        ArrayList<String>  out_s_two = new ArrayList<String> ();
        ArrayList<String>  out_s_three = new ArrayList<String> ();
        
        
        System.out.println("Using " + miningAssistant.getConfidenceMetric());
        System.out.println("Minimum StdConfidence Threshold: " + miningAssistant.getMinStdConfidence());
        System.out.println("Minimum PcaConfidence Threshold: " + miningAssistant.getMinPcaConfidence());
        System.out.println("Minimum HeadCoverage Threshold: " + miningAssistant.getMinHeadCoverage());
        System.out.println("Max Depth: " + miningAssistant.getMaxLen());
        System.out.println("Rule\tStdConfidence\tPcaConfidence\tSupport");
        
        while(!q.isEmpty()) {
            Iterator<Rule> iterator = q.iterator();
            Rule r = iterator.next();
            iterator.remove();
            
            //System.out.println(r.toString());

            if(this.miningAssistant.acceptForOutput(r)) {
                out.add(r);
                System.out.println(r.toString() + r.getStdConfidence() + "\t" + r.getPcaConfidence() + "\t" + r.getSupport());
                
                String []tmp=r.toString().split("\t");
                String v=r.toString() + ":"+
                        r.getPcaConfidence() + "\t" + r.getStdConfidence() + 
                        "\t" + r.getHeadCoverage()+"\t"+r.getSupport()+"\n";
                //System.out.println(tmp.length);
                if(tmp.length==3)
                	out_s_one.add(v);
                else if(tmp.length==4)
                	out_s_two.add(v);
                else if(tmp.length==5)
                	out_s_three.add(v);
                else
                	System.out.println("not allowed length > 3");

            }

            if(!r.isPerfect() && r.length() < miningAssistant.getMaxLen()) {
                Collection<Rule> R = miningAssistant.refine(r);
                q.addAll(R);
            }
        }
        writeRule(out_s_one,1);
        writeRule(out_s_two,2);
        writeRule(out_s_three,3);
        System.out.println("rule (length=1) "+out_s_one.size());
        System.out.println("rule (length=2) "+out_s_two.size());
        System.out.println("rule (length=3) "+out_s_three.size());
        System.out.println("total rule :"+out_s_one.size()+out_s_two.size()+out_s_three.size());
        return out;
    }    
    
    public void writeRule(ArrayList<String> rule, int len) {
    	int rule_len=rule.size();
    	//System.out.println("total rule length"+rule_len);
        FileWriter writer;
        try {
            writer = new FileWriter(pathname+"_"+len);
            for(int i=0;i<rule_len;i++)
            {
            writer.write(rule.get(i));
            }
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("written done!");
    	
    	
    }
    
    
    
    public static void main(String args[]) {
        RuleMiner ruleMiner = new RuleMiner();
        if (ruleMiner.init(args)) {
            System.out.println("Mining......Start");
            long t1 = System.currentTimeMillis();
	        Collection<Rule> rules = ruleMiner.mining();
	        //ruleMiner.outputRules(rules);
	        long t2 = System.currentTimeMillis();
            System.out.println("Mining......OK");
            System.out.println("Mined " + rules.size() + " rules");
            System.out.println("Total Time: " + (t2 - t1) / 1000.0 + " s");
        }
    }
}
