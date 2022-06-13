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

import hu.mta.sztaki.lpds.cloud.simulator.helpers.job.Job;

public class FaaSJob extends Job{

	public FaaSJob(String id, long submit, long queue, long exec, int nprocs, double ppCpu, long ppMem, String user,
			String group, String executable, Job preceding, long delayAfter) {
		super(id, submit, queue, exec, nprocs, ppCpu, ppMem, user, group, executable, preceding, delayAfter);
	}

	@Override
	public void started() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void completed() {
		// TODO Auto-generated method stub
		
	}

}
