package hu.mta.sztaki.lpds.cloud.simulator.helpers.trace.serverless.generator;
/*
 * Dilshad H. Sallo
 * 
 * This class generate execution time values, average and percentiles
 */

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Random;
import java.util.Map.Entry;

import hu.mta.sztaki.lpds.cloud.simulator.helpers.trace.random.DistributionSpecifier;

public class GenExePercentiles {
		private static boolean approach = true;
		private static int line;
		final private static int totalCount = 5000;
		private static DistributionSpecifier ds; 
		private static LinkedList<Double> allExecTime = new LinkedList<Double>();
		private static double min;
		private static double max;
		private static double p0;
		private static double p1;
		private static double p25;
		private static double p50;
		private static double p75;
		private static double p99;
		private static double p100;
		private static double p85;
		
		
		
		private static int pg0, pg1, pg25, pg50, pg75, pg99, pg100;
		private static double average;
		private static HashMap<String, Integer> fastCache = new HashMap<String, Integer>();
		
		public static SingleExecution generate( String minV, String maxV, String per0, String per1, String per25,
				String per50, String per75, String per99, String per100) {
			//reset
			allExecTime.clear();
			fastCache.clear();
			 min = max = p0 = p1 = p25 = p50 = p75 = p85 = p99 = p100 = 0.0; 
			line = pg0 = pg1 = pg25 = pg50 = pg75 = pg99 = pg100 = 0;
			min = Double.parseDouble(minV);
			max = Double.parseDouble(maxV);
			p0 = Double.parseDouble(per0);
			p1 = Double.parseDouble(per1);
			p25 = Double.parseDouble(per25);
			p50 = Double.parseDouble(per50);
			p75 = Double.parseDouble(per75);
			p99 = Double.parseDouble(per99);
			p100 = Double.parseDouble(per100);
			p85 = (p99 + p75) / 2;
		
			//Choose which dilshad or gabor approach
			if(approach) {
				ds = new DistributionSpecifier(new Random());
				  ds.addRange(Double.valueOf(p0)/Double.valueOf(max), Double.valueOf(p0)/Double.valueOf(max), 0.0024);
				  ds.addRange(Double.valueOf(p0)/Double.valueOf(max), Double.valueOf(p1-p0+p1)/Double.valueOf(max), 0.0113);
				  double dp25=Math.min(p25-p1,p50-p25);
				  ds.addRange(Double.valueOf(p25-dp25)/Double.valueOf(max), Double.valueOf(p25+dp25)/Double.valueOf(max), 0.4123);
				  double dp50=Math.min(p50-p25,p75-p50);	
				  ds.addRange(Double.valueOf(p50-dp50)/Double.valueOf(max), Double.valueOf(p50+dp50)/Double.valueOf(max), 0.1185);
				  double dp75=Math.min(p75-p50,p99-p75);	
				  ds.addRange(Double.valueOf(p75-dp75)/Double.valueOf(max), Double.valueOf(p75+dp75)/Double.valueOf(max), 0.4349);
				  double dp99=Math.min(p99-p75,p100-p99);	
				  ds.addRange(Double.valueOf(p99-dp99)/Double.valueOf(max), Double.valueOf(p99+dp99)/Double.valueOf(max), 0.019);
				  double dp100=Math.min(p100-p99,max-p100);	
				  ds.addRange(Double.valueOf(p100-dp100)/Double.valueOf(max), Double.valueOf(p100+dp100)/Double.valueOf(max), 0.0013);

				  ds.finalizeDistribution();
					for (int i = 0; i < totalCount; i++) {
						allExecTime.add(ds.nextDouble() * max);
				}//end for
					//sort values
					Collections.sort(allExecTime);
					
					//Calculate position of percentiles
					 pg0 = 0;
					 pg1 = (int) (0.01 * allExecTime.size());
					 pg25 = (int) (0.25 * allExecTime.size());
					 pg50 = (int) (0.5 * allExecTime.size());
					 pg75 = (int) (0.75 * allExecTime.size());
					 pg99 = (int) (0.99 * allExecTime.size());
					 pg100 = allExecTime.size()-1;
					 
					double sum = 0;
					for(int i=0; i<allExecTime.size(); i++) {
						sum+= allExecTime.get(i);
					}
				    average = (sum/allExecTime.size());
					
					
			}else {
				int perRank1 = Math.round(((totalCount * 1) / 100));
				fastCache.put("perRank1", perRank1);
				
				int perRank25 = Math.round(((totalCount * 25) / 100));
				fastCache.put("perRank25", perRank25 - perRank1);
				
				int perRank50 = Math.round(((totalCount * 50) / 100));
				fastCache.put("perRank50", perRank50 - perRank25);
				
				int perRank75 = Math.round(((totalCount * 75) / 100));
				fastCache.put("perRank75", perRank75 - perRank50);
				
				int perRank99 = Math.round(((totalCount * 99) / 100));
				fastCache.put("perRank99", perRank99 - perRank75);
				
				int perRank100 = Math.round(((totalCount * 100) / 100));
				fastCache.put("perRank100", perRank100 - perRank99);
				
				for (Entry<String, Integer> entry : fastCache.entrySet()) {
					switch(entry.getKey()) {
					case "perRank1":
						for(int i=0; i<entry.getValue(); i++) {
							allExecTime.add(Double.valueOf(getRandomNumber(p0, p1)));
						}
						break;				
					case "perRank25":
						for(int i=0; i<entry.getValue(); i++) {
							allExecTime.add(Double.valueOf(getRandomNumber(p1,  p25)));
						}
						break;
					case "perRank50":
						for(int i=0; i<entry.getValue(); i++) {
							allExecTime.add(Double.valueOf(getRandomNumber(p25, p50)));
						}
						break;
					case "perRank75":
						for(int i=0; i<entry.getValue(); i++) {
							allExecTime.add(Double.valueOf(getRandomNumber(p50,  p75)));
						
						}
						break;
					case "perRank99":
						for(int i=0; i<entry.getValue(); i++) {
							allExecTime.add(Double.valueOf(getRandomNumber(p75,  p85)));

						}
						break;
					case "perRank100":
						for(int i=0; i<entry.getValue(); i++) {
							allExecTime.add(Double.valueOf(getRandomNumber(p75,  p85)));
						}
						break;
					}//end case
				}
				Collections.sort(allExecTime);
				 pg0 = 0;
				 pg1 = (int) (0.01 * allExecTime.size());
				 pg25 = (int) (0.25 * allExecTime.size());
				 pg50 = (int) (0.5 * allExecTime.size());
				 pg75 = (int) (0.75 * allExecTime.size());
				 pg99 = (int) (0.99 * allExecTime.size());
				 pg100 = allExecTime.size()-1;
				 
				double sum = 0;
				for(int i=0; i<allExecTime.size(); i++) {
				sum+= allExecTime.get(i);
				}
			    average = (sum/allExecTime.size());

				
			}
			
			return new SingleExecution(average, allExecTime.get(pg0), allExecTime.get(pg1), allExecTime.get(pg25), 
					allExecTime.get(pg50), allExecTime.get(pg75), allExecTime.get(pg99), allExecTime.get(pg100));
		}
		public static double getRandomNumber(double min, double max) {
		    return (double) ((Math.random() * (max - min)) + min);
		}

}
