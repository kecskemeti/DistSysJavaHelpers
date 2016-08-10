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
 *  (C) Copyright 2016, Gabor Kecskemeti (g.kecskemeti@ljmu.ac.uk)
 *  (C) Copyright 2012-2015, Gabor Kecskemeti (kecskemeti.gabor@sztaki.mta.hu)
 */

package hu.mta.sztaki.lpds.cloud.simulator.helpers.trace.random;

import java.util.Random;

import gnu.trove.list.array.TDoubleArrayList;
import hu.mta.sztaki.lpds.cloud.simulator.helpers.Chartable;

/**
 * Allows to specify a distribution function for basic data.
 * 
 * @author "Gabor Kecskemeti, Department of Computer Science, Liverpool John
 *         Moores University, (c) 2016"
 * @author "Gabor Kecskemeti, Laboratory of Parallel and Distributed Systems,
 *         MTA SZTAKI (c) 2015"
 *
 */
public class DistributionSpecifier implements Chartable {
	/**
	 * The distribution function's probability limits
	 */
	private final TDoubleArrayList probs = new TDoubleArrayList();
	/**
	 * The distribution function's output value lower bound list
	 */
	private final TDoubleArrayList lowbnds = new TDoubleArrayList();
	/**
	 * The distribution function's output value upper bound list
	 */
	private final TDoubleArrayList upbnds = new TDoubleArrayList();

	private final Random r;
	private boolean isFinalized = false;

	/**
	 * Ensures that all data is within [0,1] - i.e. it is relative
	 * 
	 * @param val
	 *            the data to be checked
	 * @return <i>true</i> if the data is within the above range
	 */
	private boolean rangecheck(final double val) {
		return val >= 0 && val <= 1;
	}

	public DistributionSpecifier(Random uniformGenerator) {
		r = uniformGenerator;
	}

	public void addRange(final double lower, final double upper, final double probability) {
		if (isFinalized) {
			throw new RuntimeException("Tried to add new distribution data to an already finalized distribution.");
		} else {
			double newProb = (probs.size() == 0 ? 0 : probs.getQuick(probs.size() - 1)) + probability;
			if (rangecheck(lower) && rangecheck(upper) && rangecheck(newProb)) {
				probs.add(newProb);
				lowbnds.add(lower);
				upbnds.add(upper);
			} else {
				throw new RuntimeException("Some of the specified range details are not within [0,1]");
			}
		}
	}

	public void finalizeDistribution() {
		double sumofDist = probs.getQuick(probs.size() - 1);
		if (sumofDist > 1 && sumofDist < 0.9999) {
			throw new RuntimeException("Distribution is not complete");
		}
		probs.set(probs.size() - 1, 1);
		isFinalized = true;
	}

	public boolean isFinalized() {
		return isFinalized;
	}

	public double nextDouble() {
		if (isFinalized) {
			final double currentProb = r.nextDouble();
			int presumedLoc;
			for (presumedLoc = 0; probs.getQuick(presumedLoc) < currentProb; presumedLoc++) {
			}
			return lowbnds.getQuick(presumedLoc)
					+ r.nextDouble() * (upbnds.getQuick(presumedLoc) - lowbnds.getQuick(presumedLoc));
		} else {
			throw new RuntimeException("Tried to generate random numbers with a not yet finalized distribution.");
		}
	}

	/**
	 * Generates an equalised XY plot CSV output to enable third party charting
	 */
	@Override
	public String toCSV() {
		if (isFinalized) {
			// Prepare with finding the smallest non-zero length range
			double resolution = Double.MAX_VALUE;
			for (int i = 0; i < probs.size(); i++) {
				double curr_range = upbnds.getQuick(i) - lowbnds.getQuick(i);
				if (curr_range > 0) {
					resolution = Math.min(resolution, curr_range);
				}
			}
			// the smallest range will be represented by two points
			resolution /= 2;
			
			// Create the output csv
			StringBuilder sb = new StringBuilder("Val, Prob\n");
			for (int i = 0; i < probs.size(); i++) {
				double currup = upbnds.getQuick(i);
				double currlow = lowbnds.getQuick(i);
				double currprob = i==0?probs.getQuick(i):probs.getQuick(i)-probs.getQuick(i-1);
				if (currup == currlow) {
					// zero len range
					sb.append(currup + "," + currprob+"\n");
				} else {
					// regular range to be plotted according to the smallest
					// range
					
					// Finding the plot start and endpoints
					double rangemin=currlow;
					double rangemax=currup;
					for(int j=0;j<probs.size();j++) {
						if(currlow==lowbnds.getQuick(j)&&currlow==upbnds.getQuick(j)) {
							rangemin+=resolution;
							break;
						}
					}
					for(int j=0;j<probs.size();j++) {
						if(currup==lowbnds.getQuick(j)&&currup==upbnds.getQuick(j)) {
							rangemax-=resolution;
							break;
						}
					}
					
					//Writing actual plot data
					double splits=(rangemax-rangemin)/resolution;
					double tospread=currprob/(Math.abs(splits)<0.000000001?1:splits);
					for(double x=rangemin;x<=rangemax;x+=resolution) {
						sb.append(x+","+tospread+"\n");
					}
				}
			}
			return sb.toString();
		} else {
			throw new RuntimeException("Should not generate a CSV from a not yet finalized distribution.");
		}
	}
}
