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

public class GenerateExecutionTime {
	
	// Total number of function invoked during a day
	private static int totalCount;
	// Execution time for each percentile
	private static double p0, p1, p25, p50, p75, p99, p100;
	private static double max;
	// All execution time for each day within percentile
	private static PriorityQueue<Double> allExecTime = new PriorityQueue<Double>();
	//possition of percentiles
	private static int pg0, pg1, pg25, pg50, pg75, pg99, pg100;
	//object
	private static DistributionSpecifier ds; 
    //Parameters for the GA
    private static final int POPULATION_SIZE = 10;
    private static final int NUM_GENERATIONS = 10;
    private static final double ELITISM_RATE = 0.3;
    private static final double CROSSOVER_RATE = 0.7;
    private static final double MUTATION_RATE = 0.1;
    private static final int TOURNAMENT_ARITY = 2;
    	    
    //Probabilities that desired to start with
	static double pro0 = 0.0024;
	static double pro1 = 0.0113;
	static double pro25 = 0.4123;
	static double pro50 = 0.1185;
	static double pro75 = 0.4349;
	static double pro99 =  0.019;
	static double pro100 = 0.0013;
	
	//list of values that represent chromosome(individual)
	private static List<Double> indList = new ArrayList<Double>();
	
	public static void GeneExecTime(String totCount, String per0 ,String per1, String per25, String per50, String per75, String per99, String per100, String maxValue) {
		allExecTime.clear();
		totalCount = Integer.parseInt(totCount);
		p0 = Double.parseDouble(per0);
		p1 = Double.parseDouble(per1);
		p25 = Double.parseDouble(per25);
		p50 = Double.parseDouble(per50);
		p75 = Double.parseDouble(per75);
		p99 = Double.parseDouble(per99);
		p100 = Double.parseDouble(per100);
		max = Double.parseDouble(maxValue);
	
		/*
		 * initialize a new genetic algorithm
		 */
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
        	
        	 allExecTime.add(Double.valueOf(container.get(j)));
        }
	}
	// Get all generated execution values
	public static PriorityQueue<Double> getAllExecTime(){
		if(allExecTime.isEmpty())
		reset();
		return allExecTime;
	}
	 // Reset all fields for next function
	private static void reset() {
		 p0 = p1 = p25 = p50 = p75 = p99 = p100  = max = 0;
		 pg0 = pg1 = pg25 = pg50 = pg75 = pg99 = pg100 = 0;
		 totalCount = 0;
		 
	}
	/*
	 * Create initial population
	 */
    private static ElitisticListPopulation randomPopulation() {
        List<Chromosome> popList = new ArrayList();
    	
    	for(int j=0; j<POPULATION_SIZE; j++) {
        	
			  indList.clear();//reset generated values
			  ds = new DistributionSpecifier(new Random());
			  ds.addRange(Double.valueOf(p0)/Double.valueOf(max), Double.valueOf(p0)/Double.valueOf(max), pro0);
			  ds.addRange(Double.valueOf(p0)/Double.valueOf(max), Double.valueOf(p1-p0+p1)/Double.valueOf(max), pro1);
			  double dp25=Math.min(p25-p1,p50-p25);
			  ds.addRange(Double.valueOf(p25-dp25)/Double.valueOf(max), Double.valueOf(p25+dp25)/Double.valueOf(max), pro25);
			  double dp50=Math.min(p50-p25,p75-p50);	
			  ds.addRange(Double.valueOf(p50-dp50)/Double.valueOf(max), Double.valueOf(p50+dp50)/Double.valueOf(max), pro50);
			  double dp75=Math.min(p75-p50,p99-p75);	
			  ds.addRange(Double.valueOf(p75-dp75)/Double.valueOf(max), Double.valueOf(p75+dp75)/Double.valueOf(max),pro75);
			  double dp99=Math.min(p99-p75,p100-p99);	
			  ds.addRange(Double.valueOf(p99-dp99)/Double.valueOf(max), Double.valueOf(p99+dp99)/Double.valueOf(max),pro99);
			  double dp100=Math.min(p100-p99,max-p100);	
			  ds.addRange(Double.valueOf(p100-dp100)/Double.valueOf(max), Double.valueOf(p100+dp100)/Double.valueOf(max),pro100);
			  
			  ds.finalizeDistribution();
			  for (int i = 1; i <= totalCount; i++) {
				  indList.add(ds.nextDouble() * max);
			}//end for
			  
        	Chromosome randChrom =  new Individual(indList);
            popList.add(randChrom);
        }//end while population
        
        return new ElitisticListPopulation(popList, popList.size(), ELITISM_RATE);
    }
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
		     pg0 = 0;
			 pg1 = (int) (0.01 * innerRep.size());
			 pg25 = (int) (0.25 * innerRep.size());
			 pg50 = (int) (0.5 * innerRep.size());
			 pg75 = (int) (0.75 * innerRep.size());
			 pg99 = (int) (0.99 * innerRep.size());
			 pg100 = innerRep.size()-1;
			 /*
			  * Calculate R-Squared
			  */
			 SimpleRegression R = new SimpleRegression();
			 
			 R.addData(p0, (double) innerRep.get(pg0));
			 R.addData(p1, (double) innerRep.get(pg1));
			 R.addData(p25, (double) innerRep.get(pg25));
			 R.addData(p50, (double) innerRep.get(pg50));
			 R.addData(p75, (double) innerRep.get(pg75));
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
