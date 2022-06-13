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
package hu.mta.sztaki.lpds.cloud.simulator.helpers.trace.file;

import java.lang.reflect.InvocationTargetException;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import hu.mta.sztaki.lpds.cloud.simulator.helpers.job.Job;

public class CSVReader extends TraceFileReaderFoundation{
	
	static int idCounter = -1;
	static long epoch = 1557656918;

	public CSVReader(String fileName, int from, int to, boolean allowReadingFurther,
			Class<? extends Job> jobType) throws SecurityException, NoSuchMethodException {
		super("Serverless workload", fileName, from, to, allowReadingFurther, jobType);
		super.generateFromCSV = true;
	}
	public void init(boolean extract, boolean scale, int requiredJobs, String trigger, boolean swf, boolean gwf, boolean aws, 
			int minMinute, int maxMinute, int minExecution) {
		
		scaleWithUserBehaviour(scale, requiredJobs);
		if(scale) {
		exportData(extract);
		}
		generate(trigger, minMinute, maxMinute, minExecution);
		if(swf) {
			generateSWFFormat();
		}
		if(gwf) {
			generateGWFFormat();
		}
		if(aws) {
			generateAWSTrace();
		}
		}
	
	@Override
	protected boolean isTraceLine(String line) {
		return true;
	}

	@Override
	protected void metaDataCollector(String line) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected Job createJobFromLine(String jobstring)
			throws IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException {
		if(super.azureAws) {
		String[] elements = jobstring.trim().split("\\s+");
		String userID = elements[0];
		String executableID = elements[1];
		String triggerGroup = elements[2];
		String uniQId = String.valueOf(++idCounter);
		long subTime = Long.parseLong(elements[3]);
		long max = subTime * 60;
		long min = max - 60;
		long subTimeSec = ThreadLocalRandom.current().nextLong(min, max + 1);
		long submittime = epoch + subTimeSec;
		long runtime = Long.parseLong(elements[4]);
		long memory =  Long.parseLong(elements[5]);
		int procs = (int) (runtime / memory);
		long waitTime = 1;
		double ppCpu = 0;
		
		return jobCreator.newInstance(uniQId, submittime, waitTime, runtime, Math.max(1, procs), ppCpu, memory, 
				userID,triggerGroup, executableID, null, 0);
		}else {
			String[] elements = jobstring.trim().split(",");
			String id = elements[0];
			String executableID = elements[4];
			long submittime = (long) (Double.parseDouble(elements[13]) / 1000);
			long runtime = (long) Double.parseDouble(elements[22]);
			long memory =  (long) Double.parseDouble(elements[18]);
			char cpuValue = elements[2].charAt(1);
			final int procs = Integer.parseInt(String.valueOf(cpuValue));
			long waitTime = 0;
			double ppCpu = 0;
			//end time can be stop
			return jobCreator.newInstance(id, submittime, waitTime, runtime, Math.max(1, procs), ppCpu, memory, 
					"U","G", executableID, null, 0);
		}
	}

}
