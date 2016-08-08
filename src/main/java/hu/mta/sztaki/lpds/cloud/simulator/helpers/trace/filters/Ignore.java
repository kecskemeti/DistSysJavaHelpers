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
package hu.mta.sztaki.lpds.cloud.simulator.helpers.trace.filters;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.TreeSet;

import hu.mta.sztaki.lpds.cloud.simulator.helpers.job.Job;
import hu.mta.sztaki.lpds.cloud.simulator.helpers.trace.TraceFilter;

/**
 * A job acceptor that excludes jobs (based on jobids) previously listed in its
 * constructor
 * 
 * @author "Gabor Kecskemeti, Department of Computer Science, Liverpool John
 *         Moores University, (c) 2016"
 *
 */
public class Ignore implements TraceFilter.Acceptor {
	final TreeSet<String> ignored = new TreeSet<String>();

	/**
	 * Reads a file with the list of job ids separated by new lines
	 * 
	 * @param process
	 *            the file which stores the jobids
	 * @throws IOException
	 *             if there was some file handling issue
	 */
	public Ignore(File process) throws IOException {
		RandomAccessFile raf = new RandomAccessFile(process, "r");
		String line;
		while ((line = raf.readLine()) != null) {
			ignored.add(line);
		}
		raf.close();
		System.err.println("Ignore filter activated with file: " + process.getName() + " which contains "
				+ ignored.size() + " jobIDs to be ignored");
	}

	/**
	 * Reads an array of strings as the job ids to be excluded
	 * 
	 * @param toIgnore
	 *            the array of to be excluded job ids
	 */
	public Ignore(String[] toIgnore) {
		addToIgnored(toIgnore);
	}

	/**
	 * After construction further jobs could be added to the exclusion list one
	 * by one
	 * 
	 * @param toIgnore
	 *            the array of to be excluded job ids
	 */
	public void addToIgnored(String[] toIgnore) {
		for (String s : toIgnore) {
			ignored.add(s);
		}
	}

	/**
	 * After construction one can remove jobs from the ignore list
	 * 
	 * @param notToIgnore
	 *            the array of job ids that need not to be ignored anymore
	 */
	public void removeIgnored(String[] notToIgnore) {
		for (String s : notToIgnore) {
			ignored.remove(s);
		}
	}

	/**
	 * This function checks if the job specified here is actually listed with
	 * its id in the ignored set maintained by the class
	 * 
	 * @return <i>true</i> if a job is not on the ignore list
	 */
	@Override
	public boolean accept(Job j) {
		return !ignored.contains(j.getId());
	}
}
