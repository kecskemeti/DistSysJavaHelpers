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
 *  (C) Copyright 2012-2015, Gabor Kecskemeti (kecskemeti.gabor@sztaki.mta.hu)
 */

package hu.mta.sztaki.lpds.cloud.simulator.helpers.trace;

import hu.mta.sztaki.lpds.cloud.simulator.helpers.job.Job;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Random;

/**
 * A trace producer that generates a trace on the fly with random data. The
 * generated trace can be characterized with several properties. Please note
 * that even omitting one property during the class' setup will result in an
 * exception.
 * 
 * @author 
 *         "Gabor Kecskemeti, Laboratory of Parallel and Distributed Systems, MTA SZTAKI (c) 2012-5"
 */
public class RepetitiveRandomTraceGenerator implements GenericTraceProducer {
	/**
	 * To influence the random behavior of this class one is allowed to replace
	 * the generator any time with other Random implementations or with other
	 * generators with a different seed.
	 */
	public static Random r = new Random(1);

	/**
	 * These are the main characteristics of the generated trace. If even one of
	 * them end up being -1 by the time the trace is supposed to be generated,
	 * then the trace generation will not proceed further. For details of each
	 * individual property, see their getter and setter functions.
	 */
	private int jobNum = -1, parallel = -1, maxStartSpread = -1, execmin = -1,
			execmax = -1, mingap = -1, maxgap = -1, minNodeProcs = -1,
			maxNodeprocs = -1, maxTotalProcs = -1;

	/**
	 * The constructor of the Job implementation to be used during the
	 * generation process.
	 * 
	 * The class is implemented to use a constructor with the following
	 * signature: Job(long,long,long,int,String,String). For details see the
	 * implementation of the Job class.
	 */
	private final Constructor<? extends Job> jobCreator;

	/**
	 * The list of currently generated jobs. (this list gets overwritten if a
	 * new trace generation is initiated with the generateJobs function.
	 */
	private List<Job> currentlyGenerated;

	/**
	 * Shows the position from which the getJobs function can collect the jobs
	 * of the currentlyGenerated list.
	 */
	private int jobIndex = -1;

	/**
	 * The time instance when the trace will start.
	 */
	private long submitStart = 0;

	/**
	 * To be used by the setter functions. Determines if the currentlyGenerated
	 * list of jobs got obsolete. If they did then it asks for their
	 * regeneration with the new trace characteristics.
	 */
	private void regenJobs() {
		if (currentlyGenerated != null) {
			generateJobs();
		}
	}

	/**
	 * The total number of jobs to be generated in a single run. This is the
	 * maximum length of the currentlyGenerated list.
	 * 
	 * @return the current number of jobs.
	 */
	public int getJobNum() {
		return jobNum;
	}

	/**
	 * Allows a different number of jobs to be generated in a single run.
	 * 
	 * @param jobNum
	 *            the new number of jobs for the length of the
	 *            currentlyGenerated list.
	 */
	public void setJobNum(int jobNum) {
		this.jobNum = jobNum;
		regenJobs();
	}

	/**
	 * Determines how many jobs can run in parallel at any given time. (this is
	 * a maximum number, the actual trace might not contain any parallel
	 * fragments that high in parallelism)
	 * 
	 * @return the current level of maximum parallelism.
	 */
	public int getParallel() {
		return parallel;
	}

	/**
	 * Sets a new level of parallelism for jobs in the newly generated traces.
	 * 
	 * @param parallel
	 *            the maximum number of jobs allowed in parallel in any part of
	 *            the generated trace.
	 */
	public void setParallel(int parallel) {
		this.parallel = parallel;
		regenJobs();
	}

	/**
	 * The starting time of any job in a parallel section can be dispersed over
	 * a given period of time. This time period is specified here.
	 * 
	 * @return The range (specified in seconds) over which the current trace
	 *         spreads the start events for all jobs in a parallel section.
	 */
	public int getMaxStartSpread() {
		return maxStartSpread;
	}

	/**
	 * The function allows to set a new spread value for the start time of the
	 * jobs in every parallel section generated by the current trace generator.
	 * 
	 * @param maxStartSpread
	 *            over how many seconds should be dispersed the start events of
	 *            each job in a parallel section
	 */
	public void setMaxStartSpread(int maxStartSpread) {
		this.maxStartSpread = maxStartSpread;
		regenJobs();
	}

	/**
	 * Retrieves the minimum execution time of a job
	 * 
	 * @return the number of seconds a job should be minimally occupying a CPU
	 *         with 100% load
	 */
	public int getExecmin() {
		return execmin;
	}

	/**
	 * Sets the shortest execution time for any job in the trace
	 * 
	 * @param execmin
	 *            the number of seconds a job should be minimally occupying a
	 *            CPU with 100% load
	 */
	public void setExecmin(int execmin) {
		this.execmin = execmin;
		regenJobs();
	}

	/**
	 * Retrieves the maximum possible execution time for a job
	 * 
	 * @return the number of seconds a job should be maximally occupying a CPU
	 *         with 100% load
	 */
	public int getExecmax() {
		return execmax;
	}

	/**
	 * Sets the longest possible execution time for any job in the trace
	 * 
	 * @param execmin
	 *            the number of seconds a job should be maximally occupying a
	 *            CPU with 100% load
	 */
	public void setExecmax(int execmax) {
		this.execmax = execmax;
		regenJobs();
	}

	/**
	 * The minimum gap between two parallel sections. The duration of a parallel
	 * section is calculated as the sum of the maximum starting spread and the
	 * maximum job length. Thus the next parallel section after the currently
	 * ongoing one will be initiated by the trace generator after the duration
	 * of the previous section and the length of the gap between the two
	 * sections.
	 * 
	 * @return the absolute minimum duration (in seconds) that should be spent
	 *         before a new parallel section can be started in the generated
	 *         trace
	 */
	public int getMingap() {
		return mingap;
	}

	/**
	 * Sets the minimum gap. See the getMingap function for details.
	 * 
	 * @param mingap
	 *            the new minimum duration (in seconds) to be spent without
	 *            tasks between parallel sections.
	 */
	public void setMingap(int mingap) {
		this.mingap = mingap;
		regenJobs();
	}

	/**
	 * The maximum gap between parallel sections. See the getMingap function for
	 * details.
	 * 
	 * @return the maximum duration (in seconds) that could be past without
	 *         having any activities submitted to the system between two
	 *         parallel sections.
	 */
	public int getMaxgap() {
		return maxgap;
	}

	/**
	 * Sets the maximum gap between parallel sections. See the getMingap
	 * function for details.
	 * 
	 * @param maxgap
	 *            the maximum jobless gap in seconds between two parallel
	 *            sections.
	 */
	public void setMaxgap(int maxgap) {
		this.maxgap = maxgap;
		regenJobs();
	}

	/**
	 * The minimum number of processors to be used by single job.
	 * 
	 * Note: If the level of maximum parallelism is not reached by in the
	 * current parallel section, but maxtotalprocs has been already reached then
	 * this minimum number of processors are assigned for the rest of the newly
	 * generated jobs in the current parallel section.
	 * 
	 * @return currently set minimum processors limit
	 */
	public int getMinNodeProcs() {
		return minNodeProcs;
	}

	/**
	 * The minimum number of processors to be used by single job.
	 * 
	 * @param minNodeProcs
	 *            new minimum processors limit
	 */
	public void setMinNodeProcs(int minNodeProcs) {
		this.minNodeProcs = minNodeProcs;
		regenJobs();
	}

	/**
	 * The maximum number of processors to be used by a single job.
	 * 
	 * @return the currently set maximum processor count
	 */
	public int getMaxNodeprocs() {
		return maxNodeprocs;
	}

	/**
	 * The maximum number of processors to be used by a single job.
	 * 
	 * Note: The maximum can temporarily drop to the minimum processor limit.
	 * For details see getMinNodeProcs().
	 * 
	 * @param maxNodeprocs
	 *            the newly required maximum processor count
	 */
	public void setMaxNodeprocs(int maxNodeprocs) {
		this.maxNodeprocs = maxNodeprocs;
		regenJobs();
	}

	/**
	 * Determines the total number of processors available in the particular
	 * infrastructure. This is in fact used to determine the maximum number of
	 * processors used in parallel by all the jobs in a parallel section
	 * generated by this trace producer.
	 * 
	 * Note: if this value is bigger than the number of processors available in
	 * the simulated distributed infrastructure, then the generated trace will
	 * be useful for evaluating under-provisioning situations.
	 * 
	 * @return Maximum processor count in a parallel section.
	 */
	public int getMaxTotalProcs() {
		return maxTotalProcs;
	}

	/**
	 * Sets the total processors available for a parallel section. For details,
	 * see the documentation of getMaxTotalProcs().
	 * 
	 * @param maxTotalProcs
	 *            a new maximum processor count in the later generated parallel
	 *            sections.
	 */
	public void setMaxTotalProcs(int maxTotalProcs) {
		this.maxTotalProcs = maxTotalProcs;
		regenJobs();
	}

	/**
	 * Basic constructor of the class. This constructor only ensures that the
	 * jobType in question can be used with the kind of constructor required to
	 * instantiate the jobs in the randomly generated jobs in the trace of this
	 * producer.
	 * 
	 * Warning: The construction of this object will not result in a readily
	 * usable component. Please make sure that all setters of this class are
	 * used after the object's creation. These setters will allow the generator
	 * to know the characteristics of the required trace.
	 * 
	 * @param jobType
	 *            The class of the job implementation that needs to be produced
	 *            by this particular trace producer.
	 * @throws SecurityException
	 *             If the class of the jobType cannot be accessed by the
	 *             classloader of the caller.
	 * @throws NoSuchMethodException
	 *             If the class of the jobType does not hold one of the expected
	 *             constructors.
	 */
	public RepetitiveRandomTraceGenerator(Class<? extends Job> jobType)
			throws SecurityException, NoSuchMethodException {
		jobCreator = jobType
				.getConstructor(String.class, long.class, long.class,
						long.class, int.class, double.class, long.class,
						String.class, String.class, String.class, Job.class,
						long.class);
	}

	/**
	 * The main trace generator function. It's purpose is to construct the trace
	 * characterized by the values acquired through the object's setters (for
	 * details how the trace is generated please see the documentation of each
	 * getter and setter pair). The trace output is written to the
	 * currentlyGenerated list.
	 * 
	 * @throws RuntimeException
	 *             when the trace generator is not initialized properly or when
	 *             the job object cannot be created with the constructor
	 *             specified during the initialization of this trace producer.
	 */
	private void generateJobs() {
		try {
			if (jobNum == -1 || parallel == -1 || maxStartSpread == -1
					|| execmin == -1 || execmax == -1 || mingap == -1
					|| maxgap == -1 || minNodeProcs == -1 || maxNodeprocs == -1
					|| maxTotalProcs == -1) {
				throw new Exception(
						"TraceGenerator not initialized properly, some of its customization options left unset");
			}
			System.err.println("Trace Generator starts with parameters (JN: "
					+ jobNum + ", parallel: " + parallel + ", startSpr: "
					+ maxStartSpread + ", exec: " + execmin + "-" + execmax
					+ ", gap: " + mingap + "-" + maxgap + ", nodeprocs: "
					+ minNodeProcs + "-" + maxNodeprocs + ", totalProcs: "
					+ maxTotalProcs + ") at "
					+ Calendar.getInstance().getTime());
			currentlyGenerated = new ArrayList<Job>();
			final int execspace = execmax - execmin;
			final int gapspace = maxgap - mingap;
			final int nodeSpace = maxNodeprocs - minNodeProcs;
			for (int i = 0; i < jobNum / parallel; i++) {
				int usedProcs = 0;
				long currentMaxTime = submitStart;
				for (int j = 0; j < parallel; j++) {
					final long submittime = submitStart
							+ (maxStartSpread == 0 ? 0 : r
									.nextInt(maxStartSpread));
					int nprocs = minNodeProcs
							+ (nodeSpace == 0 ? 0 : r.nextInt(nodeSpace));
					nprocs = Math.min(maxTotalProcs - usedProcs, nprocs);
					nprocs = nprocs <= 0 ? 1 : nprocs;
					final long exectime = execmin
							+ (execspace == 0 ? 0 : r.nextInt(execspace));
					usedProcs += nprocs;
					currentlyGenerated.add(jobCreator.newInstance(null,
							submittime, 0, exectime, nprocs, -1, -1, "", "",
							"", null, 0));
					currentMaxTime = Math.max(currentMaxTime, submittime
							+ exectime);
				}
				submitStart = currentMaxTime + mingap
						+ (gapspace == 0 ? 0 : r.nextInt(gapspace));
			}
			jobIndex = 0;
			System.err.println("Trace Generator stops at "
					+ Calendar.getInstance().getTime());
		} catch (Exception e) {
			currentlyGenerated = null;
			jobIndex = -1;
			throw new RuntimeException(e);
		}
	}

	/**
	 * Generates a new set of jobs then return the complete set immediately to
	 * the caller. The generation is done with the generateJobs function.
	 * 
	 * @return a completely new trace with the length of jobNum.
	 */
	@Override
	public List<Job> getAllJobs() {
		generateJobs();
		jobIndex = currentlyGenerated.size();
		return currentlyGenerated;
	}

	/**
	 * Produces a trace with the length of 'num'. Trace generation only happens
	 * if the currentlyGenerated list does not contain enough entries. In such
	 * case first the previously generated sublist is served, then a new set is
	 * generated and added as its tail. The function recursively invokes itself
	 * until there are enough elements produced for a trace with the lenght of
	 * 'num'.
	 * 
	 * @param num
	 *            The number of trace elements expected in the returning list.
	 * 
	 * @return a trace list with the length of num
	 */
	@Override
	public List<Job> getJobs(int num) {
		if (jobIndex + num < currentlyGenerated.size()) {
			List<Job> listPart = currentlyGenerated.subList(jobIndex, jobIndex
					+ num);
			jobIndex += num;
			return listPart;
		} else {
			List<Job> firstPart = currentlyGenerated.subList(jobIndex,
					currentlyGenerated.size());
			num -= currentlyGenerated.size() - jobIndex;
			generateJobs();
			List<Job> secondPart = getJobs(num);
			firstPart.addAll(secondPart);
			return secondPart;
		}
	}

}
