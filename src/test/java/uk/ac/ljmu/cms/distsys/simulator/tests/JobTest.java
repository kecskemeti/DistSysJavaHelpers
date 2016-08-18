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
package uk.ac.ljmu.cms.distsys.simulator.tests;

import org.junit.Assert;
import org.junit.Test;

import hu.mta.sztaki.lpds.cloud.simulator.helpers.job.Job;

public class JobTest {
	public static class RealJob extends Job {
		public RealJob(String id, long submit, long queue, long exec, int nprocs, double ppCpu, long ppMem, String user,
				String group, String executable, Job preceding, long delayAfter) {
			super(id, submit, queue, exec, nprocs, ppCpu, ppMem, user, group, executable, preceding, delayAfter);
		}

		@Override
		public void completed() {
			setRan(true);
			setRealstopTime(System.currentTimeMillis() / 1000 - getSubmittimeSecs());
		}

		@Override
		public void started() {
			setRealqueueTime(System.currentTimeMillis() / 1000 - getSubmittimeSecs());
		}
	}

	public static Job genRealJob(long submit, long duration, int procs) {
		return new RealJob(null, submit, 0, duration, procs, -1, -1, "", "", "", null, 0);
	}

	@Test(timeout = 100)
	public void overlapTester() {
		Job a = genRealJob(0, 10, 1);
		Job b = genRealJob(11,10,1);
		Assert.assertFalse("Should not be reported to be overlapping", a.isOverlapping(b));
		Assert.assertFalse("Should not be reported to be overlapping", b.isOverlapping(a));
		b = genRealJob(2,3,1);
		Assert.assertTrue("Should be overlapping", a.isOverlapping(b));
		Assert.assertTrue("Should be overlapping", b.isOverlapping(a));
		b = genRealJob(5,10,1);
		Assert.assertTrue("Should be overlapping", a.isOverlapping(b));
		Assert.assertTrue("Should be overlapping", b.isOverlapping(a));
		b = genRealJob(10,10,1);
		Assert.assertFalse("Should not be reported to be overlapping", a.isOverlapping(b));
		Assert.assertFalse("Should not be reported to be overlapping", b.isOverlapping(a));
	}
}
