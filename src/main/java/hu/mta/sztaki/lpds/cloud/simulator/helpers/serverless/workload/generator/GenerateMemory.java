/*
 *  ========================================================================
 *  Helper classes to support simulations of large scale distributed systems
 *  ========================================================================
 *  
 *  This file is part of DistSysJavaHelpers.
 *  
 *    DistSysJavaHelpers is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 *
 *   DistSysJavaHelpers is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 *  (C) Copyright 2022, Gabor Kecskemeti (g.kecskemeti@ljmu.ac.uk)
 *  (C) Copyright 2022, Dilshad H. Sallo (sallo@iit.uni-miskolc.hu)
 */
package hu.mta.sztaki.lpds.cloud.simulator.helpers.serverless.workload.generator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Random;

import org.apache.commons.math3.genetics.AbstractListChromosome;
import org.apache.commons.math3.genetics.Chromosome;
import org.apache.commons.math3.genetics.ElitisticListPopulation;
import org.apache.commons.math3.genetics.FixedGenerationCount;
import org.apache.commons.math3.genetics.GeneticAlgorithm;
import org.apache.commons.math3.genetics.InvalidRepresentationException;
import org.apache.commons.math3.genetics.OnePointCrossover;
import org.apache.commons.math3.genetics.Population;
import org.apache.commons.math3.genetics.StoppingCondition;
import org.apache.commons.math3.genetics.TournamentSelection;
import org.apache.commons.math3.stat.regression.SimpleRegression;

import hu.mta.sztaki.lpds.cloud.simulator.helpers.trace.random.DistributionSpecifier;

public class GenerateMemory {
	// Total number of function invoked during a day
	private static int totalCount;
	// Execution time for each percentile
	private static double  p1, p5, p25, p50, p75, p95, p99, p100;
	// All execution time for each day within percentile
	private static PriorityQueue<Double> allMemoryValues = new PriorityQueue<Double>();
	// Percentile with required execution time
	private static DistributionSpecifier ds; 

    //Parameters for the GA
    private static final int POPULATION_SIZE = 10;
    private static final int NUM_GENERATIONS = 10;
    private static final double ELITISM_RATE = 0.3;
    private static final double CROSSOVER_RATE = 0.7;
    private static final double MUTATION_RATE = 0.1;
    private static final int TOURNAMENT_ARITY = 2;
    
	private static int pg5, pg1, pg25, pg50, pg75, pg99, pg100, pg95;
	
	//list of values that represent chromosome(individual)
	private static List<Double> indList = new ArrayList<Double>();
	
	public static void GeneMemory(String totCount,  String per1, String per5, String per25, String per50, String per75,String per95 ,String per99, String per100) {
		allMemoryValues.clear();
		totalCount = Integer.parseInt(totCount.replace(".0", ""));
		p1 = Double.parseDouble(per1.replace(".0", ""));
		p5 = Double.parseDouble(per5.replace(".0", ""));
		p25 = Double.parseDouble(per25.replace(".0", ""));
		p50 = Double.parseDouble(per50.replace(".0", ""));
		p75 = Double.parseDouble(per75.replace(".0", ""));
		p95 = Double.parseDouble(per95.replace(".0", ""));
		p99 = Double.parseDouble(per99.replace(".0", ""));
		p100 = Double.parseDouble(per100.replace(".0", ""));
		
		GeneticAlgorithm ga = new GeneticAlgorithm(
			    new OnePointCrossover<Double>(),
			    CROSSOVER_RATE,
			    new SimpleMutation(),
			    MUTATION_RATE,
			    new TournamentSelection(TOURNAMENT_ARITY));
		//Create populations
		Population initial = randomPopulation();

        StoppingCondition stopCond = new FixedGenerationCount(NUM_GENERATIONS);
       
        // Run the algorithm
        Population finalPopulation = ga.evolve(initial, stopCond);	    

        // Best chromosome from the final population
        Chromosome bestFinal = finalPopulation.getFittestChromosome();
       // System.out.println(bestFinal.);
 
        //Get list of values for best individual
        String bestIndList =bestFinal.toString();
        
        //eliminate odd symbol
        bestIndList = bestIndList.replace("[", ",");
        bestIndList = bestIndList.replace("]", ",");
        bestIndList = bestIndList.replace("(", ",");
        bestIndList = bestIndList.replace(")", ",");
        
        //split String based on comma
        String [] items = bestIndList.split("\\s*,\\s*");
        List<String> container = Arrays.asList(items);


        //Add values of best individual to calculate percentiles and average
        for(int j=3; j<container.size(); j++) {
        	
        	allMemoryValues.add(Double.valueOf(container.get(j)));
        }
	}
	public static PriorityQueue<Double> getAllMemory(){
		if (allMemoryValues.isEmpty()) {
			return null;
		}
		reset();
		return allMemoryValues;	
	}
	 // Rest all fields for next function
	private static void reset() {
		 p1 = p5 = p25 = p50 = p75 = p95 = p99 = p100 = 0;
		 totalCount = 0;

	}
	 private static ElitisticListPopulation randomPopulation() {
	        List<Chromosome> popList = new ArrayList();
	    	
	    	for(int j=0; j<POPULATION_SIZE; j++) {
	    		
	    		//Always Gabor approach
	    		 indList.clear();//reset generated values
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
	    		 
				  for (int i = 1; i <= totalCount; i++) {
					  indList.add(ds.nextDouble() * p100);
				}//end for
				  
	        	Chromosome randChrom =  new Individual(indList);
	            popList.add(randChrom);
	        }//end while population
	        
	        return new ElitisticListPopulation(popList, popList.size(), ELITISM_RATE);
	    }//end random
	 
	 private static class Individual extends AbstractListChromosome<Double>{
			//inner list representation
			private static List<Double> innerRep = new ArrayList<Double>();
			
			public Individual(List representation) throws InvalidRepresentationException {
				super(representation);
			
			}

			@Override
			public double fitness() {
				 innerRep.clear();//Reset
				innerRep.addAll(this.getRepresentation());
				
				Collections.sort(innerRep);//Sort list
				
				  //Calculate position of percentiles
					pg1 = (int) (0.01 * innerRep.size());
					pg5 = (int) (0.05 * innerRep.size());
					pg25 = (int) (0.25 * innerRep.size());
				    pg50 = (int) (0.5 * innerRep.size());
					pg75 = (int) (0.75 * innerRep.size());
					pg95 = (int) (0.95 * innerRep.size());
					pg99 = (int) (0.99 * innerRep.size());
					pg100 = innerRep.size()-1;
				 /*
				  * Calculate R-Squared
				  */
				 SimpleRegression R = new SimpleRegression();
				 
				 R.addData(p1, (double) innerRep.get(pg1));
				 R.addData(p5, (double) innerRep.get(pg5));
				 R.addData(p25, (double) innerRep.get(pg25));
				 R.addData(p50, (double) innerRep.get(pg50));
				 R.addData(p75, (double) innerRep.get(pg75));
				 R.addData(p95, (double) innerRep.get(pg95));
				 R.addData(p99, (double) innerRep.get(pg99));
				 R.addData(p100, (double) innerRep.get(pg100));

				return R.getRSquare();
			}

			@Override
			protected void checkValidity(List<Double> chromosomeRepresentation) throws InvalidRepresentationException {
				// TODO Auto-generated method stub
				
			}

			@Override
			public AbstractListChromosome<Double> newFixedLengthChromosome(List<Double> chromosomeRepresentation) {
				return new Individual(chromosomeRepresentation);
			}
	    }//end class
}