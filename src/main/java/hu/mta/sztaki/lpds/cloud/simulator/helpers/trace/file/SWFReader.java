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
 *  (C) Copyright 2015, Gabor Kecskemeti (kecskemeti.gabor@sztaki.mta.hu)
 */

package hu.mta.sztaki.lpds.cloud.simulator.helpers.trace.file;

import java.lang.reflect.InvocationTargetException;

import hu.mta.sztaki.lpds.cloud.simulator.helpers.job.Job;

/**
 * An implementation of the generic trace file reader functionality to support
 * files from the standard workloads archive (http://www.cs.huji.ac.il/labs/parallel/workload/swf.html).
 * 
 * @author "Gabor Kecskemeti, Department of Computer Science, Liverpool John
 *         Moores University, (c) 2016"
 * @author "Gabor Kecskemeti, Laboratory of Parallel and Distributed Systems,
 *         MTA SZTAKI (c) 2015"
 */
public class SWFReader extends TraceFileReaderFoundation {

	public SWFReader(String fileName, int from, int to, boolean allowReadingFurther, Class<? extends Job> jobType)
			throws SecurityException, NoSuchMethodException {
		super("Standard workload format", fileName, from, to, allowReadingFurther, jobType);
	}

	/**
	 * Determines if a particular line in the SWF file is representing a job
	 * 
	 * Actually ignores empty lines and lines starting with ';'
	 */
	@Override
	protected boolean isTraceLine(String line) {
		return basicTraceLineDetector(";", line);
	}

	/**
	 * Collects the total number of processors in the trace if specified in the
	 * comments
	 */
	@Override
	protected void metaDataCollector(String line) {
		if (line.contains("MaxProcs")) {
			String[] splitLine = line.split("\\s");
			try {
				maxProcCount = parseLongNumber(splitLine[splitLine.length - 1].trim());
			} catch (NumberFormatException e) {
				// safe to ignore as there is no useful data here then
			}
		}
	}

	@Override
	protected Job createJobFromLine(String line)
			throws IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException {
		final String[] fragments = line.trim().split("\\s+");
		try {
		/**
		 * String id, long submit, long queue, long exec, int nprocs, double
		 * ppCpu, long ppMem, String user, String group, String executable, Job
		 * preceding, long delayAfter
		 */
		// 1 done, 0 fail, 5 cancel
		int jobState = Integer.parseInt(fragments[10]);
		int procs = Integer.parseInt(fragments[4]);
		long runtime = Long.parseLong(fragments[3]);
		long waitTime = Long.parseLong(fragments[2]);
		if (jobState != 1 && (procs < 1 || runtime < 0)) {
			return null;
		} else {
			final String preceedingJobId = fragments[16].trim();
			Job preceedingJob = null;
			if (!preceedingJobId.equals("-1")) {
				preceedingJob = jobLookupInCache(preceedingJobId);
			}
			return jobCreator.newInstance(
					// id:
					fragments[0],
					// submit time in secs:
					Long.parseLong(fragments[1]),
					// wait time in secs:
					Math.max(0, waitTime),
					// run time in secs:
					Math.max(0, runtime),
					// allocated processors:
					Math.max(1, procs),
					// average cpu time:
					(long) Double.parseDouble(fragments[5]),
					// average memory:
					Long.parseLong(fragments[6]),
					// userid:
					fragments[11],
					// groupid:
					fragments[12],
					// execid:
					fragments[13], preceedingJob, preceedingJob == null ? 0 : Long.parseLong(fragments[17]));
		}
		} catch(ArrayIndexOutOfBoundsException ex) {
			// Incomplete line, ignore it
			return null;
		}
	}

}
