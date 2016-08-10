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

import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import hu.mta.sztaki.lpds.cloud.simulator.helpers.Chartable;
import hu.mta.sztaki.lpds.cloud.simulator.helpers.job.Job;
import hu.mta.sztaki.lpds.cloud.simulator.helpers.trace.TraceManagementException;

/**
 * 
 * This is a simple probabilistic trace generator where the number of processors
 * for each job are generated with a distribution function.
 * 
 * @author "Gabor Kecskemeti, Department of Computer Science, Liverpool John
 *         Moores University, (c) 2016"
 * @author "Gabor Kecskemeti, Laboratory of Parallel and Distributed Systems,
 *         MTA SZTAKI (c) 2012-5"
 *
 */
public class SimpleRandomTraceGenerator extends GenericRandomTraceGenerator implements Chartable {

	/**
	 * The generators that comply with the given distribution functions
	 */
	private final DistributionSpecifier sizeDistribution, durationDistribution, distanceDistribution;

	/**
	 * The maximum distance between the start time of two jobs
	 */
	private final int maxJobDistance;
	/**
	 * The maximum length of a job
	 */
	private final long maxJobDuration;
	/**
	 * The submission time used for the currently generated job (this is always
	 * updated once a job is generated
	 */
	private long currentSubmitTime = 0;
	/**
	 * Those jobs that still prevail until the current submission time (and thus
	 * could cause resource overutilisation)
	 */
	private final ArrayList<Job> overlapCheckers = new ArrayList<Job>();

	/**
	 * Creates the simple generator instance, and does basic checking on the
	 * parameters
	 * 
	 * @param jobType
	 *            the jobtype to be passed to TraceProducerFoundation
	 * @param distribution
	 *            the distribution function for the number of processors per job
	 *            (the constructor checks if the probabilities of all ranges sum
	 *            up to 1)
	 * @param maxJobDistance
	 *            the maximum gap between two job's startup times
	 * @param maxJobDuration
	 *            the maximum duration of a job
	 * @throws NoSuchMethodException
	 *             from TraceProducerFoundation
	 * @throws SecurityException
	 *             from TraceProducerFoundation
	 */
	public SimpleRandomTraceGenerator(final Class<? extends Job> jobType, DistributionSpecifier size,
			DistributionSpecifier duration, final long maxJobDuration, DistributionSpecifier gap,
			final int maxJobDistance) throws NoSuchMethodException, SecurityException {
		super(jobType);
		this.maxJobDistance = maxJobDistance;
		this.maxJobDuration = maxJobDuration;
		if (!size.isFinalized()) {
			size.finalizeDistribution();
		}
		if (!duration.isFinalized()) {
			duration.finalizeDistribution();
		}
		if (!gap.isFinalized()) {
			gap.finalizeDistribution();
		}
		sizeDistribution = size;
		durationDistribution = duration;
		distanceDistribution = gap;
	}

	/**
	 * gets the job instance at current submit time (a member variable) with the
	 * duration and processor count specified in the parameters
	 * 
	 * @param duration
	 *            the length of the newly created job instance (==
	 *            stoptime-starttime)
	 * @param procCount
	 *            the number of processors to be requested by the job instance
	 * @return the created job instance
	 * @throws InstantiationException
	 *             if there was some trouble with reflection
	 * @throws IllegalAccessException
	 *             if there was some trouble with reflection
	 * @throws IllegalArgumentException
	 *             if there was some trouble with reflection
	 * @throws InvocationTargetException
	 *             if there was some trouble with reflection
	 */
	private Job getJobInstance(long duration, int procCount)
			throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		return jobCreator.newInstance(null, currentSubmitTime, 0, duration, procCount, -1, -1, "", "", "", null, 0);
	}

	/**
	 * the main job generator function, ensures that no jobs overload the
	 * infrastructure (adds an upper cap of resource utilization equalling to
	 * getMaxTotalProcs())
	 * 
	 * Also follows the distribution function specified for the number of
	 * processors.
	 * 
	 * The rest of the parameters for each job are chosen with a uniform random
	 * number generator
	 * 
	 */
	@Override
	protected List<Job> generateJobs() throws TraceManagementException {
		try {
			System.err.println("Simple Random Trace Generator parameters: jobnum - " + getJobNum() + " totprocs - "
					+ getMaxTotalProcs());
			final int maxLen = getJobNum();
			final ArrayList<Job> generatedList = new ArrayList<Job>(maxLen);
			for (int i = 0; i < maxLen; i++) {
				final long duration = (long) (durationDistribution.nextDouble() * maxJobDuration);
				currentSubmitTime += (long) (distanceDistribution.nextDouble() * maxJobDistance);
				int procs = (int) (Math.ceil(sizeDistribution.nextDouble() * getMaxTotalProcs()));
				Job j = getJobInstance(duration, procs);
				long maxCores;
				do {
					maxCores = procs;
					final Iterator<Job> jit = overlapCheckers.iterator();
					while (jit.hasNext()) {
						final Job other = jit.next();
						if (j.isOverlapping(other)) {
							maxCores += other.nprocs;
							if (maxCores > getMaxTotalProcs()) {
								currentSubmitTime = other.getStoptimeSecs() + 1;
								j = getJobInstance(duration, procs);
								break;
							}
						} else {
							jit.remove();
						}
					}
				} while (maxCores > getMaxTotalProcs());
				overlapCheckers.add(j);
				generatedList.add(j);
			}
			return generatedList;
		} catch (Exception e) {
			throw new TraceManagementException("Could not generate jobs", e);
		}
	}

	/**
	 * The marker for the maximum inter-job submission gap in the trace
	 * definition file
	 */
	public static final String jobDistMarker = "maxJobDistance=";

	/**
	 * The marker for the maximum job duration
	 */
	public static final String jobDurMarker = "maxJobDuration=";
	/**
	 * The marker for a single entry in the distribution function for the number
	 * of processors
	 */
	public static final String sizeDistMarker = "sizeDist=";
	/**
	 * The marker for a single entry in the pdistribution function for the
	 * length of the tasks
	 */
	public static final String durDistMarker = "durDist=";
	/**
	 * The marker for a single entry in the distribution function for the gaps
	 * between the tasks
	 */
	public static final String gapDistMarker = "gapDist=";

	private static boolean parseandaddDistLine(String definition, String[] preTexts,
			DistributionSpecifier[] wheretoAdd) {
		for (int i = 0; i < preTexts.length; i++) {
			if (definition.startsWith(preTexts[i])) {
				String[] rangeDefinition = definition.substring(preTexts[i].length()).split(",");
				wheretoAdd[i].addRange(Double.parseDouble(rangeDefinition[0]), Double.parseDouble(rangeDefinition[1]),
						Double.parseDouble(rangeDefinition[2]));
				return true;
			}
		}
		return false;
	}

	/**
	 * An easy setup for the data required in the constructor
	 * 
	 * This function parses a file and based on its contents sets up a random
	 * trace generator.
	 * 
	 * REMARK1: Multiple occurrences of job distance and duration definitions
	 * are overwriting their values
	 * 
	 * REMARK2: There could be as many range entries as many are needed to give
	 * a sufficiently detailed distribution function
	 * 
	 * @param jobType
	 *            the kind of job the future generator is expected to emit
	 * @param fileName
	 *            the name of the file in which the definition of the trace is
	 *            found
	 * @return the trace generator
	 * @throws IOException
	 *             if there was an error during the reading of the trace
	 *             definition file
	 * @throws NoSuchMethodException
	 * @throws SecurityException
	 */
	public static SimpleRandomTraceGenerator getInstanceFromFile(final Class<? extends Job> jobType, String fileName)
			throws IOException, NoSuchMethodException, SecurityException {
		System.err.println("Random trace properties loaded from file: " + fileName);
		final RandomAccessFile raf = new RandomAccessFile(fileName, "r");
		final DistributionSpecifier d = new DistributionSpecifier(r), g = new DistributionSpecifier(r),
				s = new DistributionSpecifier(r);
		final String[] preTextList = new String[] { sizeDistMarker, durDistMarker, gapDistMarker };
		final DistributionSpecifier[] distList = new DistributionSpecifier[] { s, d, g };
		int maxJobDist = -1;
		long maxJobDur = -1;

		int lineCounter = 0;
		String line;
		try {
			// Parsing
			while ((line = raf.readLine()) != null) {
				lineCounter++;
				if (!parseandaddDistLine(line, preTextList, distList) && line.startsWith(jobDistMarker)) {
					maxJobDist = Integer.parseInt(line.substring(jobDistMarker.length()));
				} else if (line.startsWith(jobDurMarker)) {
					maxJobDur = Long.parseLong(line.substring(jobDurMarker.length()));
				}
			}
		} catch (NumberFormatException e) {
			throw new RuntimeException("Number parsing error in line " + lineCounter, e);
		} catch (RuntimeException e) {
			throw new RuntimeException("Range parsing error in line " + lineCounter, e);
		} finally {
			raf.close();
		}

		// File correctness checks
		if (maxJobDist < 0) {
			throw new RuntimeException("No " + jobDistMarker + " was specified");
		}
		if (maxJobDur < 0) {
			throw new RuntimeException("No " + jobDurMarker + " was specified");
		}
		for (int i = 0; i < preTextList.length; i++) {
			try {
				distList[i].finalizeDistribution();
			} catch (RuntimeException e) {
				throw new RuntimeException("No " + preTextList[i] + " was specified", e);
			}
		}
		return new SimpleRandomTraceGenerator(jobType, s, d, maxJobDur, g, maxJobDist);
	}

	/**
	 * Offers all a serialized chart data for all three distributions included
	 * in the generator
	 */
	@Override
	public String toCSV() {
		StringBuilder sb = new StringBuilder("Size:\n");
		sb.append(sizeDistribution.toCSV());
		sb.append("Duration (max - " + maxJobDuration + "):\n");
		sb.append(durationDistribution.toCSV());
		sb.append("Distance (max - " + maxJobDistance + "):\n");
		sb.append(distanceDistribution.toCSV());
		return sb.toString();
	}
}
