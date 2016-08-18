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
 */
package uk.ac.ljmu.cms.distsys.simulator.tests;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Random;

import org.junit.Assert;
import org.junit.Test;

import hu.mta.sztaki.lpds.cloud.simulator.helpers.job.Job;
import hu.mta.sztaki.lpds.cloud.simulator.helpers.trace.FileBasedTraceProducerFactory;
import hu.mta.sztaki.lpds.cloud.simulator.helpers.trace.GenericTraceProducer;
import hu.mta.sztaki.lpds.cloud.simulator.helpers.trace.TraceManagementException;
import hu.mta.sztaki.lpds.cloud.simulator.helpers.trace.random.GenericRandomTraceGenerator;

public class TraceFileLoaderTest {
	final public static String srtgFileContent = "sizeDist=0,0.1,0.8\n" + "sizeDist=0.1,1,0.2\n"
			+ "maxJobDuration=100\n" + "durDist=0.01,0.1,0.5\n" + "durDist=0.1,0.9,0.45\n" + "durDist=0.9,1,0.05\n"
			+ "maxJobDistance=100\n" + "gapDist=0,0.2,0.9\n" + "gapDist=0.2,1,0.1";

	private final int len = 20;
	private final int offset = 10;
	private final int maxProcs = 100;

	@Test
	public void repeatedSrtgLoadTestWithOffset()
			throws IOException, SecurityException, NoSuchMethodException, TraceManagementException {

		File temp = File.createTempFile("DistSysJavaHelpers-test", ".srtg");
		BufferedWriter bw = new BufferedWriter(new FileWriter(temp));
		bw.write(srtgFileContent);
		bw.close();
		GenericTraceProducer gtp = FileBasedTraceProducerFactory.getProducerFromFile(temp.getAbsolutePath(), 0, len,
				false, maxProcs, JobTest.RealJob.class);
		List<Job> jobsBefore = gtp.getAllJobs();
		GenericRandomTraceGenerator.r.setSeed(GenericRandomTraceGenerator.defaultSeed);
		gtp = FileBasedTraceProducerFactory.getProducerFromFile(temp.getAbsolutePath(), offset, len + offset, false,
				maxProcs, JobTest.RealJob.class);
		List<Job> jobsAfter = gtp.getAllJobs();
		for (int i = 0; i < len; i++) {
			Job jb = jobsBefore.get(i);
			Job ja = jobsAfter.get(i);
			Assert.assertFalse("Should generate completely different jobs at the same positions",
					jb.getExectimeSecs() == ja.getExectimeSecs() && jb.getQueuetimeSecs() == ja.getQueuetimeSecs()
							&& jb.getSubmittimeSecs() == ja.getSubmittimeSecs() && jb.nprocs == ja.nprocs);
		}
		for (int i = offset; i < len; i++) {
			Job jb = jobsBefore.get(i);
			Job ja = jobsAfter.get(i - offset);
			Assert.assertTrue("Should generate the same jobs if the offset is applied in the later queried joblist",
					jb.getExectimeSecs() == ja.getExectimeSecs() && jb.getQueuetimeSecs() == ja.getQueuetimeSecs()
							&& jb.getSubmittimeSecs() == ja.getSubmittimeSecs() && jb.nprocs == ja.nprocs);
		}
		temp.delete();
	}
}
