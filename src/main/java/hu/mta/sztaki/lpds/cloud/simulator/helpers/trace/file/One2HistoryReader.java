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

package hu.mta.sztaki.lpds.cloud.simulator.helpers.trace.file;

import hu.mta.sztaki.lpds.cloud.simulator.helpers.job.Job;

import java.lang.reflect.InvocationTargetException;

/**
 * An implementation of the generic trace file reader functionality to support
 * files from OpenNebula 2.x VM instantiation archives.
 * 
 * @author 
 *         "Gabor Kecskemeti, Laboratory of Parallel and Distributed Systems, MTA SZTAKI (c) 2012-2015"
 */

public class One2HistoryReader extends TraceFileReaderFoundation {
	/**
	 * Constructs a "one2" file reader that later on can act as a trace producer
	 * for user side schedulers.
	 * 
	 * @param fileName
	 *            The full path to the one2 vm trace file that should act as the
	 *            source of the jobs produced by this trace producer.
	 * @param from
	 *            The first job in the one2 vm trace file that should be
	 *            produced in the job listing output.
	 * @param to
	 *            The last job in the one2 vm trace file that should be still in
	 *            the job listing output.
	 * @param allowReadingFurther
	 *            If true the previously listed "to" parameter is ignored if the
	 *            "getJobs" function is called on this trace producer.
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
	public One2HistoryReader(String fileName, int from, int to,
			boolean allowReadingFurther, Class<? extends Job> jobType)
			throws SecurityException, NoSuchMethodException {
		super("OpenNebula 2.x", fileName, from, to, allowReadingFurther,
				jobType);
	}

	/**
	 * Determines if a particular line in the Opennebula 2.x trace file is
	 * representing a job
	 * 
	 * Actually ignores empty lines and lines starting with '#'
	 */
	@Override
	public boolean isTraceLine(String line) {
		return basicTraceLineDetector("#", line);
	}
	
	@Override
	protected void metaDataCollector(String line) {
		// do nothing
	}

	/**
	 * Parses a single line of the tracefile and instantiates a job object out
	 * of it.
	 * 
	 * Uses the Job classes typed constructor - Job(long, long, long, int,
	 * String, String)!
	 * 
	 * There is no support for user, executable or the number of processors
	 * currenrly.
	 */
	@Override
	public Job createJobFromLine(String line) throws IllegalArgumentException,
			InstantiationException, IllegalAccessException,
			InvocationTargetException {
		String[] fragments = line.split("\\s+");
		long submittime = Long.parseLong(fragments[7]);
		long queueendtime = Long.parseLong(fragments[11]);
		long execendtime = Long.parseLong(fragments[8]);
		if (queueendtime == 0)
			return null;
		// TODO: check if nprocs, user, and exec can be filled out properly!
		return jobCreator.newInstance(null, submittime, queueendtime
				- submittime,
				execendtime == 0 ? (Long.MAX_VALUE - queueendtime)
						: (execendtime - queueendtime), 1, -1, -1, "USER",
				"GROUP", "EXEC", null, 0);
	}
}
