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

import hu.mta.sztaki.lpds.cloud.simulator.helpers.trace.file.CSVReader;

public class MainServerlessTraceGenerator {
	/**
	 * Range of lines to be read in a trace
	 */
	final int from = 1;
	final int to = 4;
	/**
	 * Extract information about users such as number of invoked tasks
	 */
	final boolean extractAnalyseTrace = false;
	/**
	 * Enable scaling workload with user behavior
	 * Note: number of lines will not consider in this case, instead required jobs will be considered
	 */
	final boolean scaleUserBehaviour = false;
	/**
	 * Approximate jobs based on user behavior
	 */
	final int requiredJobs = 2000;
	/**
	 * Range of minutes to consider in a day
	 */
	final int minMinute = 0;
	final int maxMinute = 1440;
	/**
	 * Trigger type in trace, null will consider everything
	 */
	final String triggerType = null;
	/**
	 * Minimum execution (sec) time to consider (0 default)
	 */
	final int minExeTime = 0;
	/**
	 * Convert to Standard Workload Format
	 */
	final boolean swfFormat = false;
	/**
	 * Convert to Grid Workload Format
	 */
	final boolean gwfFormat = false;
	/**
	 * Convert to AWS Lambda Format
	 */
	final boolean awsLambdaFormat = false;

	public MainServerlessTraceGenerator(String traceFileLoc) throws SecurityException, NoSuchMethodException {		
		if (traceFileLoc.endsWith(".csv")) {
			CSVReader csv = new CSVReader(traceFileLoc,  from, to, true, FaaSJob.class);
			csv.init(extractAnalyseTrace, scaleUserBehaviour, requiredJobs, triggerType, swfFormat, gwfFormat, awsLambdaFormat, 
					minMinute, maxMinute, minExeTime);
		}else {
			System.out.println("Selected file is not in CSV");
		}
	}
	public static void main(String[] args) throws SecurityException, NoSuchMethodException {
		if (args.length < 1) {
			System.err.println("No file exist, please specify a location of CSV Azure dataset file");
		}else {
			long before = System.currentTimeMillis();
			new MainServerlessTraceGenerator(args[0]);
			long totalExeution  = System.currentTimeMillis() - before;
			System.out.println("Generation took: " + totalExeution + "ms");
		}
	}
}
