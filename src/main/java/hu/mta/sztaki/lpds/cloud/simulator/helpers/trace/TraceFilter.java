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
 *  (C) Copyright 2015, Gabor Kecskemeti (kecskemeti.gabor@sztaki.mta.hu)
 */

package hu.mta.sztaki.lpds.cloud.simulator.helpers.trace;

import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import hu.mta.sztaki.lpds.cloud.simulator.helpers.job.Job;

/**
 * A trace producer that encapsulates another trace producer. All its production
 * functions filter the encapsulated producer's output according to the
 * acceptance criteria specified in its constructor.
 * 
 * @author "Gabor Kecskemeti, Laboratory of Parallel and Distributed Systems, MTA SZTAKI (c) 2015"
 */
public class TraceFilter implements GenericTraceProducer {
	public static interface Acceptor {
		boolean accept(Job j);
	}

	private final GenericTraceProducer masterTrace;
	private final Acceptor acceptor;

	/**
	 * Allows an arbitrary trace producer's output to be filtered with the help
	 * of an acceptor.
	 * 
	 * @param master
	 *            the original trace that is supposed to be filtered
	 * @param a
	 *            the acceptor function which determines if a particular job
	 *            could remain in the filtered set
	 */
	public TraceFilter(GenericTraceProducer master, Acceptor a) {
		masterTrace = master;
		acceptor = a;
	}

	/**
	 * Filters a job set: removes all jobs that are not supposed to be in the
	 * jobset according to the acceptance criteria.
	 * 
	 * WARNING this function modifies its parameter!
	 * 
	 * @param jobs
	 *            the jobset to be filtered
	 * @return returns the filtered jobset
	 */
	private List<Job> filterJobSet(List<Job> jobs) {
		if (jobs != null) {
			Iterator<Job> i = jobs.iterator();
			while (i.hasNext()) {
				Job currJob = i.next();
				if (!acceptor.accept(currJob)) {
					i.remove();
				}
			}
		}
		return jobs;
	}

	@Override
	public List<Job> getAllJobs() throws TraceManagementException {
		return filterJobSet(masterTrace.getAllJobs());
	}

	@Override public List<Job> getAllJobs(Comparator<Job> jobComparator) throws TraceManagementException {
		return filterJobSet(masterTrace.getAllJobs(jobComparator));
	}

	@Override
	public List<Job> getJobs(int num) throws TraceManagementException {
		return filterJobSet(masterTrace.getJobs(num));
	}

	@Override public List<Job> getJobs(int num, Comparator<Job> jobComparator) throws TraceManagementException {
		return filterJobSet(masterTrace.getJobs(num, jobComparator));
	}

	@Override
	public long getMaxProcCount() {
		return masterTrace.getMaxProcCount();
	}
}
