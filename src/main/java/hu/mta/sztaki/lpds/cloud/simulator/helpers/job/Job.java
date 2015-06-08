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

package hu.mta.sztaki.lpds.cloud.simulator.helpers.job;

/**
 * Class representing a job to be executed in a simulation. It allows the
 * recording the job-life-cycle related events into a single entity. This job
 * class comes rather handy when handling execution traces of real life
 * infrastructures in simulated systems.
 * 
 * @author 
 *         "Gabor Kecskemeti, Laboratory of Parallel and Distributed Systems, MTA SZTAKI (c) 2012"
 */
public abstract class Job {
	/**
	 * The internal identifier of the job
	 */
	private int id;
	/**
	 * The time the job was submitted to its original infrastructure.
	 */
	private long submittimeSecs;
	/**
	 * The time the job has got terminated on its original infrastructure.
	 */
	private long stoptimeSecs;
	/**
	 * The time it took the job to terminate on its original infrastructure.
	 */
	private long exectimeSecs;
	/**
	 * The time instance halfway through the job's execution. Useful for some
	 * job list analysis and ordering techniques.
	 */
	private long midExecInstanceSecs;
	/**
	 * The time it took for the job to get past its queuing phase on the
	 * original infrastructure.
	 */
	private long queuetimeSecs;
	/**
	 * The time instance the job has got started on its original infrastructure.
	 */
	private long starttimeSecs;
	/**
	 * The number of processors used by this job. (E.g., used by MPI jobs)
	 */
	public final int nprocs;
	/**
	 * The user executing this particular job.
	 */
	public final String user;
	/**
	 * The executable ran on the target infrastructure by the job. By checking
	 * this field one can determine what kind of virtual machine image is needed
	 * (i.e., which image contains the executable). Also it is possible to alter
	 * the behavior of the simulated execution of the job based on the usual
	 * behavior of such executables.
	 */
	public final String executable;

	/**
	 * The time it took to queue the job on the simulated infrastructure.
	 * (allows comparison to the real life infrastructure's value specified by
	 * queuetimeSecs.)
	 */
	private long realqueueTime = -1;
	/**
	 * The time instance when the job terminated on the simulated
	 * infrastructure.
	 */
	private long realstopTime = -1;
	/**
	 * Shows if this job has already been executed by the simulator.
	 */
	private boolean ran = false;

	/**
	 * Allows the creation of a job object using the GWA trace line format.
	 * 
	 * Supports GWA traces with millisecond time base (useful to load traces
	 * produced by the ASKALON workflow environment of University of Innsbruck).
	 * 
	 * @param jobstring
	 *            the job specification using the GWA trace line format.
	 */
	public Job(String jobstring) {
		boolean askalon = jobstring.endsWith("ASKALON");
		final int maxLen = 14;
		char[] str = jobstring.toCharArray();
		StringBuffer[] elements = new StringBuffer[maxLen];
		int j = 0;
		for (j = 0; j < maxLen; j++) {
			elements[j] = new StringBuffer(12);
		}
		boolean wrt = false;
		j = 0;
		for (int i = 0; i < str.length; i++) {
			if (str[i] == ' ' || str[i] == '\t') {
				if (!wrt) {
					continue;
				} else {
					j++;
					if (j == maxLen) {
						break;
					}
					wrt = false;
				}
			} else {
				wrt = true;
				elements[j].append(str[i]);
			}
		}
		// Simple but significantly slower (3x)
		// String[] elements = jobstring.split("\\s+");
		id = Integer.parseInt(elements[0].toString());
		submittimeSecs = Long.parseLong(askalon ? elements[1].substring(0,
				elements[1].length() - 3) : elements[1].toString());
		queuetimeSecs = Math.max(0, Long.parseLong(elements[2].toString()));
		exectimeSecs = Math.max(0, Long.parseLong(elements[3].toString()));
		nprocs = Math.max(1, Integer.parseInt(elements[4].toString()));
		user = parseTextualField(elements[11].toString());
		executable = parseTextualField(elements[13].toString());
		stoptimeSecs = queuetimeSecs + exectimeSecs + submittimeSecs;
		starttimeSecs = submittimeSecs + queuetimeSecs;
		midExecInstanceSecs = starttimeSecs + exectimeSecs / 2;
	}

	/**
	 * Checks if the particular GWA line entry contains useful data.
	 * 
	 * @param unparsed
	 *            the text to be checked for usefulness.
	 * @return the text altered after usefulness checking. If the text is not
	 *         useful then the string "N/A" is returned.
	 */
	private String parseTextualField(final String unparsed) {
		return unparsed.equals("-1") ? "N/A" : unparsed;
		// unparsed.matches("^-?[0-9](?:\\.[0-9])?$")?"N/A":unparsed;
	}

	/**
	 * The generic constructor to be used by most of the trace generators and in
	 * most of the use cases.
	 * 
	 * Please note the id of the job is automatically generated with this
	 * constructor.
	 * 
	 * @param submit
	 *            the time instance (in secs) the job is submitted
	 * @param queue
	 *            the duration (in secs) it took for the job to get to
	 *            execution.
	 * @param exec
	 *            the execution duration of the job on its original resource.
	 * @param nprocs
	 *            number of processors used by the particular job during its
	 *            execution
	 * @param user
	 *            the user who submitted the job
	 * @param executable
	 *            the kind of executable ran by the job
	 */
	public Job(long submit, long queue, long exec, int nprocs, String user,
			String executable) {
		this.id = this.hashCode();
		submittimeSecs = submit;
		queuetimeSecs = queue;
		exectimeSecs = exec;
		stoptimeSecs = queuetimeSecs + exectimeSecs + submittimeSecs;
		starttimeSecs = submittimeSecs + queuetimeSecs;
		midExecInstanceSecs = starttimeSecs + exectimeSecs / 2;
		this.nprocs = nprocs;
		this.user = user;
		this.executable = executable;
	}

	/**
	 * Determines the identifier of this particular job.
	 * 
	 * @return the id of the job.
	 */
	public int getId() {
		return id;
	}

	/**
	 * Provides a simple textual output to represent the particular job in the
	 * textual output of a simulation (e.g. to be used for later analysis).
	 */
	@Override
	public String toString() {
		return "Job: " + id + " Start Time: " + submittimeSecs + " Stop Time: "
				+ stoptimeSecs + " Procs: " + nprocs;
	}

	/**
	 * Determines the time it took the job to terminate on its original
	 * infrastructure.
	 * 
	 * @return the execution duration
	 */
	public long getExectimeSecs() {
		return exectimeSecs;
	}

	/**
	 * Determines the time it took for the job to get past its queuing phase on
	 * the original infrastructure.
	 * 
	 * @return queuing duration
	 */
	public long getQueuetimeSecs() {
		return queuetimeSecs;
	}

	/**
	 * Determines the time the job has got terminated on its original
	 * infrastructure.
	 * 
	 * @return stop time instance
	 */
	public long getStoptimeSecs() {
		return stoptimeSecs;
	}

	/**
	 * Determines time the job was submitted to its original infrastructure.
	 * 
	 * @return submit time instance
	 */
	public long getSubmittimeSecs() {
		return submittimeSecs;
	}

	/**
	 * Determines the time it took to queue the job on the simulated
	 * infrastructure.
	 * 
	 * @return if the queuing has not completed yet for this particular job then
	 *         it returns -1.
	 */
	public long getRealqueueTime() {
		return realqueueTime;
	}

	/**
	 * Determines the time instance when the job terminated on the simulated
	 * infrastructure.
	 * 
	 * @return the real stop time. (or -1 if the job has not stopped yet)
	 */
	public long getRealstopTime() {
		return realstopTime;
	}

	/**
	 * Determines the time instance the job has got started on its original
	 * infrastructure.
	 * 
	 * @return the start time instance
	 */
	public long getStartTimeInstance() {
		return starttimeSecs;
	}

	/**
	 * Determines the time instance halfway through the job's execution.
	 * 
	 * @return the time instance in the middle of execution on the original
	 *         infrastructure.
	 */
	public long getMidExecInstanceSecs() {
		return midExecInstanceSecs;
	}

	/**
	 * Simulator specific implementation that can set the real start time
	 * (allows the query of the current simulated time instance independent from
	 * this job class)
	 * 
	 * Once the real start time is determined, the implementation is expected to
	 * use the setter for real queue time.
	 */
	public abstract void started();

	/**
	 * Simulation specific implementation to determine the simulation specific
	 * time instance the real stop time of the job has happened. Similarly to
	 * the start time, this abstract function allows the query of the current
	 * simulated time instance independently from the simulator being used. Once
	 * the stop time is determined the implementation is expected to use the
	 * setter for real stop time.
	 */
	public abstract void completed();

	/**
	 * Should be called from custom job implementations after the completed
	 * function is called.
	 * 
	 * @param ran
	 */
	protected void setRan(boolean ran) {
		this.ran = ran;
	}

	/**
	 * To be set by implementors of the class. See the discussion next to the
	 * started function.
	 * 
	 * @param realqueueTime
	 */
	protected void setRealqueueTime(long realqueueTime) {
		this.realqueueTime = realqueueTime;
	}

	/**
	 * To be set by implementors of the class. See the discussion next the
	 * completed function.
	 * 
	 * @param realstopTime
	 */
	protected void setRealstopTime(long realstopTime) {
		this.realstopTime = realstopTime;
	}

	/**
	 * Allows the readjustment of the timings of the job in case the job has not
	 * been ran yet in the simulation.
	 * 
	 * @param offsetSecs
	 *            the offset with which each of the timing must be adjusted
	 * @throws IllegalStateException
	 *             if the job has already ran.
	 */
	public void adjust(long offsetSecs) throws IllegalStateException {
		if (!ran) {
			submittimeSecs += offsetSecs;
			starttimeSecs += offsetSecs;
			stoptimeSecs += offsetSecs;
		} else {
			throw new IllegalStateException(
					"Cannot adjust job if it has been ran already!");
		}
	}

}