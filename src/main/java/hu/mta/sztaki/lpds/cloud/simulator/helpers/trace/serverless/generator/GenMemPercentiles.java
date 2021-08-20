package hu.mta.sztaki.lpds.cloud.simulator.helpers.trace.serverless.generator;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import hu.mta.sztaki.lpds.cloud.simulator.helpers.trace.random.DistributionSpecifier;

/*
 * Dilshad H. Sallo
 * 
 * This class generate memory values, average and percentiles
 */
public class GenMemPercentiles {
	//Gabor appraoch true
	private static boolean approach = true;
	final private static int totalCount = 5000;
	private static DistributionSpecifier ds; 
	private static LinkedList<Double> allExecTime = new LinkedList<Double>();
	private static HashMap<String, Integer> fastCache = new HashMap<String, Integer>();
	
	private static double p5;
	private static double p1;
	private static double p25;
	private static double p50;
	private static double p75;
	private static double p99;
	private static double p100;
	private static double p95;

	private static int pg5, pg1, pg25, pg50, pg75, pg99, pg100, pg95;
	private static double average;
	
	public static SingleMemory generate(String per1, String per5, String per25, String per50, String per75, 
			String per95, String per99, String per100) {
		//reset
		allExecTime.clear();
		fastCache.clear();
		average = p1 = p5 = p25 = p50 = p75 = p95 = p99 = p100 = 0;
		p5 = Double.parseDouble(per5);
		p1 = Double.parseDouble(per1);
		p25 = Double.parseDouble(per25);
		p50 = Double.parseDouble(per50);
		p75 = Double.parseDouble(per75);
		p95 = Double.parseDouble(per95);
		p99 = Double.parseDouble(per99);
		p100 = Double.parseDouble(per100);
		
		//Choose approach
		if(approach) {
			  ds = new DistributionSpecifier(new Random());
			  ds.addRange(Double.valueOf(p1)/Double.valueOf(p100), Double.valueOf(p1)/Double.valueOf(p100), 0.00001);
			  double dp5=Math.min(p5-p1,p25-p5);
			  ds.addRange(Double.valueOf(p5-dp5)/Double.valueOf(p100), Double.valueOf(p5+dp5)/Double.valueOf(p100), 0.01999);
			  double dp25=Math.min(p25-p5,p50-p25);
			  ds.addRange(Double.valueOf(p25-dp25)/Double.valueOf(p100), Double.valueOf(p25+dp25)/Double.valueOf(p100), 0.24);
			  double dp50=Math.min(p50-p25,p75-p50);	
			  ds.addRange(Double.valueOf(p50-dp50)/Double.valueOf(p100), Double.valueOf(p50+dp50)/Double.valueOf(p100), 0.24);
			  double dp75=Math.min(p75-p50,p95-p75);	
			  ds.addRange(Double.valueOf(p75-dp75)/Double.valueOf(p100), Double.valueOf(p75+dp75)/Double.valueOf(p100), 0.24);
			  double dp95=Math.min(p95-p75,p99-p95);
			  ds.addRange(Double.valueOf(p95-dp95)/Double.valueOf(p100), Double.valueOf(p95+dp95)/Double.valueOf(p100), 0.24);
			  double dp99=Math.min(p99-p95,p100-p99);	
			  ds.addRange(Double.valueOf(p99-dp99)/Double.valueOf(p100), Double.valueOf(p99+dp99)/Double.valueOf(p100), 0.01999);
			  double dp100=Math.min(p100-p99,0); //p100-p100	
			  ds.addRange(Double.valueOf(p100-dp100)/Double.valueOf(p100), Double.valueOf(p100+dp100)/Double.valueOf(p100), 0.00001);

			  ds.finalizeDistribution();
			for (int i = 0; i < totalCount; i++) {
				allExecTime.add(ds.nextDouble() * p100);
			}
			//sort values
			Collections.sort(allExecTime);
			//calculate percentiles pro
			pg1 = (int) (0.01 * allExecTime.size());
			pg5 = (int) (0.05 * allExecTime.size());
			pg25 = (int) (0.25 * allExecTime.size());
		    pg50 = (int) (0.5 * allExecTime.size());
			pg75 = (int) (0.75 * allExecTime.size());
			pg95 = (int) (0.95 * allExecTime.size());
			pg99 = (int) (0.99 * allExecTime.size());
			pg100 = allExecTime.size()-1;
			//calculate average
			double sum = 0;
			for(int i=0; i<allExecTime.size(); i++) {
				sum+= allExecTime.get(i);
			}
		    average = (sum/allExecTime.size());
		}else {
			
			int perRank1 = Math.round(((totalCount * 1) / 100));
			fastCache.put("perRank1", perRank1);

			int perRank5 = Math.round(((totalCount * 5) / 100));
			fastCache.put("perRank5", perRank5 - perRank1);

			int perRank25 = Math.round(((totalCount * 25) / 100));
			fastCache.put("perRank25", perRank25 - perRank5);

			int perRank50 = Math.round(((totalCount * 50) / 100));
			fastCache.put("perRank50", perRank50 - perRank25);

			int perRank75 = Math.round(((totalCount * 75) / 100));
			fastCache.put("perRank75", perRank75 - perRank50);
			
			int perRank95 = Math.round(((totalCount * 95) / 100));
			fastCache.put("perRank95", perRank95 - perRank75);

			int perRank99 = Math.round(((totalCount * 99) / 100));
			fastCache.put("perRank99", perRank99 - perRank95);

			int perRank100 = Math.round(((totalCount * 100) / 100));
			fastCache.put("perRank100", perRank100 - perRank99);
			
			
			for (Entry<String, Integer> entry : fastCache.entrySet()) {
				switch(entry.getKey()) {
				case "perRank1":
					for(int i=0; i<entry.getValue(); i++) {
						allExecTime.add(ThreadLocalRandom.current().nextDouble(1,  p1 + 1));
					}
					break;
				case "perRank5":
					for(int i=0; i<entry.getValue(); i++) {
						allExecTime.add(ThreadLocalRandom.current().nextDouble(p1,  p5 + 1));
					}
					break;
				case "perRank25":
					for(int i=0; i<entry.getValue(); i++) {
						allExecTime.add(ThreadLocalRandom.current().nextDouble(p5,  p25 + 1));
					}
					break;
				case "perRank50":
					for(int i=0; i<entry.getValue(); i++) {
						allExecTime.add(ThreadLocalRandom.current().nextDouble(p25,  p50 + 1));
					}
					break;
				case "perRank75":
					for(int i=0; i<entry.getValue(); i++) {
						allExecTime.add(ThreadLocalRandom.current().nextDouble(p50,  p75 + 1));
					}
					break;
				case "perRank95":
					for(int i=0; i<entry.getValue(); i++) {
						allExecTime.add(ThreadLocalRandom.current().nextDouble(p75,  p95 + 1));
					}
					break;
				case "perRank99":
					for(int i=0; i<entry.getValue(); i++) {
						allExecTime.add(ThreadLocalRandom.current().nextDouble(p95,  p99 + 1));
					}
					break;
				case "perRank100":
					for(int i=0; i<entry.getValue(); i++) {
						allExecTime.add(ThreadLocalRandom.current().nextDouble(p99,  p100 + 1));
					}
					break;
				}//end case
			}
			Collections.sort(allExecTime);
			pg1 = (int) (0.01 * allExecTime.size());
			pg5 = (int) (0.05 * allExecTime.size());
			pg25 = (int) (0.25 * allExecTime.size());
			pg50 = (int) (0.5 * allExecTime.size());
			pg75 = (int) (0.75 * allExecTime.size());
			pg95 = (int) (0.95 * allExecTime.size());
			pg99 = (int) (0.99 * allExecTime.size());
			pg100 = allExecTime.size()-1;
			
			double sum = 0;
			for(int i=0; i<allExecTime.size(); i++) {
				sum+= allExecTime.get(i);
			}
		    average = (sum/allExecTime.size());
		}
		
		return new SingleMemory(average, allExecTime.get(pg1), allExecTime.get(pg5), allExecTime.get(pg25),
				allExecTime.get(pg50), allExecTime.get(pg75), allExecTime.get(pg95), allExecTime.get(pg99), allExecTime.get(pg100));
	}//end generate

}