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

package hu.mta.sztaki.lpds.cloud.simulator.helpers.trace.filters;

import hu.mta.sztaki.lpds.cloud.simulator.helpers.job.Job;
import hu.mta.sztaki.lpds.cloud.simulator.helpers.trace.TraceFilter;

/**
 * A job acceptor that determines if a job is running at a given time
 * 
 * @author "Gabor Kecskemeti, Laboratory of Parallel and Distributed Systems, MTA SZTAKI (c) 2015"
 *
 */
public class RunningAtaGivenTime implements TraceFilter.Acceptor {
	private final long whenshouldjobsrun;

	/**
	 * the acceptor's constructor where the time instance for job's run check
	 * should be specified
	 * 
	 * @param givenTime
	 *            the time instance against which the acceptance criteria is
	 *            evaluated
	 */
	public RunningAtaGivenTime(final long givenTime) {
		whenshouldjobsrun = givenTime;
	}

	/**
	 * This function checks if the job given as its parameter runs at the time
	 * instance given in the constructor of this class
	 * 
	 * @return <i>true</i> if a job supposed to run at a given time instance
	 */
	@Override
	public boolean accept(Job j) {
		return j.getStartTimeInstance() <= whenshouldjobsrun && j.getStoptimeSecs() >= whenshouldjobsrun;
	}
}
