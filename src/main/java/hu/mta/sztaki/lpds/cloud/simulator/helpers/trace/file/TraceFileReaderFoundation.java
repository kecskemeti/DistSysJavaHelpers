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
 *  (C) Copyright 2012-2015, Gabor Kecskemeti (kecskemeti.gabor@sztaki.mta.hu)
 */

package hu.mta.sztaki.lpds.cloud.simulator.helpers.trace.file;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

import hu.mta.sztaki.lpds.cloud.simulator.helpers.job.Job;
import hu.mta.sztaki.lpds.cloud.simulator.helpers.trace.TraceProducerFoundation;

/**
 * A simple but generic line based trace file reader.
 * 
 * @author "Gabor Kecskemeti, Department of Computer Science, Liverpool John
 *         Moores University, (c) 2016"
 * @author "Gabor Kecskemeti, Laboratory of Parallel and Distributed Systems,
 *         MTA SZTAKI (c) 2012-5"
 * 
 */
public abstract class TraceFileReaderFoundation extends TraceProducerFoundation {
	/**
	 * A marker for the log files so one can see which kind of trace was read by
	 * the reader foundation.
	 */
	private final String traceKind;
	/**
	 * Job entry range on which this reader operates. From can start at 0. The
	 * "to"th job will not be included in the returned trace.
	 */
	private final int from, to;
	/**
	 * Determines if the reader can go further in the file than the line
	 * determined by "to".
	 */
	private final boolean furtherReadable;
	/**
	 * The tracefile
	 */
	private final File toBeRead;
	/**
	 * The reader for the tracefile. If it is null, then the tracefile is not
	 * yet read.
	 */
	private BufferedReader actualReader;
	/**
	 * The currently read contents of the trace. If this field is null, then
	 * there were no getJobs, or getAllJobs calls.
	 */
	private List<Job> currentlyOffered;

	/**
	 * allows rapid job lookups while the currently offered joblist is
	 * constructed
	 */
	private HashMap<String, Job> fastCache;
	/**
	 * The number of jobs read from the tracefile so far. In general this should
	 * be over 0, if it is -1, then the tracefile is either not yet read or its
	 * reading has been completed.
	 */
	private int lineIdx = -1;

	/**
	 * Initializes the generic fields of all line based trace file readers.
	 * 
	 * @param traceKind
	 *            Used to mark which kind of trace file parser has derived this
	 *            class. If the trace file is read, in the beginning of each
	 *            reading cycle the name of the trace file parser is reported on
	 *            the standard error.
	 * @param fileName
	 *            The full path to the file that should act as the source of the
	 *            jobs produced by this trace producer.
	 * @param from
	 *            The first job in the file that should be produced in the job
	 *            listing output. (please note the counter starts at 0)
	 * @param to
	 *            The last job in the file that should be still in the job
	 *            listing output.
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
	protected TraceFileReaderFoundation(String traceKind, String fileName, int from, int to,
			boolean allowReadingFurther, Class<? extends Job> jobType) throws SecurityException, NoSuchMethodException {
		super(jobType);
		this.traceKind = traceKind;
		toBeRead = new File(fileName);
		this.from = from;
		this.to = to;
		furtherReadable = allowReadingFurther;
	}

	/**
	 * The main trace file reading mechanism of the helpers. The reader ensures
	 * that the lines before "from" (defined in the constructor) are skipped.
	 * 
	 * @param count
	 *            the number of jobs to be read from the current location.
	 */
	private void readTrace(int count) {
		try {
			if (!furtherReadable && lineIdx >= to) {
				throw new Exception("Was set to stop after reaching the 'to' item");
			}
			System.err.println(traceKind + " trace file reader starts for: " + toBeRead + " at "
					+ Calendar.getInstance().getTime());
			currentlyOffered = new ArrayList<Job>();
			fastCache = new HashMap<String, Job>();
			if (actualReader == null) {
				actualReader = new BufferedReader(new FileReader(toBeRead));
			}

			String line = null;

			// Skip first lines until from
			while (lineIdx < from && (line = actualReader.readLine()) != null) {
				if (isTraceLine(line)) {
					lineIdx++;
				} else {
					metaDataCollector(line);
				}
			}

			// Actual reading of the lines
			do {
				if (isTraceLine(line)) {
					count--;
					lineIdx++;
					Job toAdd = createJobFromLine(line);
					if (toAdd == null)
						continue;
					fastCache.put(toAdd.getId(), toAdd);
				} else {
					metaDataCollector(line);
				}
			} while (count > 0 && (line = actualReader.readLine()) != null);
			if (line == null) {
				actualReader.close();
				lineIdx = -1; // marks the end of the file
			}
			currentlyOffered.addAll(fastCache.values());
			System.err.println(traceKind + " trace file reader stops for: " + toBeRead + " at "
					+ Calendar.getInstance().getTime());
		} catch (Exception e) {
			throw new RuntimeException("Error in line: " + lineIdx, e);
		}
	}

	/**
	 * Using job ids this function shecks if a job is in the currently offered
	 * list. If it is then returns with it.
	 * 
	 * @param id
	 *            The job's id which is looked for.
	 * @return the job with the specific jobid
	 */
	protected Job jobLookupInCache(final String id) {
		return fastCache.get(id);
	}

	/**
	 * Reads the complete trace from the file until the "to" field of the object
	 * allows.
	 * 
	 * @return If there were no previous reading of the tracefile by this
	 *         reader, then the set of jobs in the range between "from" and
	 *         "to". Otherwise a null list is returned.
	 */
	@Override
	public List<Job> getAllJobs() {
		if (actualReader != null) {
			// Only allow reading all jobs if we have not read any pieces
			return null;
		}
		readTrace(to - from);
		return currentlyOffered;
	}

	/**
	 * Collects all the specified number of jobs from the tracefile (starting
	 * from the current file pointer). And returns with them. It keeps the file
	 * pointer so on the next call the job collection can be done continuously.
	 * 
	 * @param num
	 *            the number of jobs to be collected in the current run.
	 * @return the set of jobs collected from the tracefile.
	 * @throws NoFurtherJobsException
	 *             if there are no further jobs available in the tracefile.
	 */
	@Override
	public List<Job> getJobs(int num) throws NoFurtherJobsException {
		if (actualReader != null && lineIdx == -1) {
			throw new NoFurtherJobsException("Run out of jobs in traceFile: " + toBeRead, null);
		}
		readTrace(num);
		return currentlyOffered;
	}

	/**
	 * Provides a simple implementation to determine if a particular line of the
	 * tracefile describes a job.
	 * 
	 * It only skips lines that either start with the commentIndicator or that
	 * are completely empty (except whitespaces).
	 * 
	 * @param commentIndicator
	 *            The string with which a line must start in order to be
	 *            considered a comment
	 * @param actualLine
	 *            the line to be analysed
	 * @return true if "actualLine" is representing a job in the tracefile.
	 */
	public static boolean basicTraceLineDetector(final String commentIndicator, final String actualLine) {
		final String linetri = actualLine.trim();
		return !(linetri.startsWith(commentIndicator) || linetri.isEmpty());
	}

	/**
	 * Allows long numbers to be parsed even if they are presented in a more
	 * human readable form (eg. with commas or dots)
	 * 
	 * @param suspectedLong
	 *            the string that represents the number
	 * @return the long in parsed form
	 * @throws NumberFormatException
	 *             if the long cannot be parsed
	 */
	public static long parseLongNumber(String suspectedLong) {
		return Long.parseLong(suspectedLong.replaceAll(",", "").replaceAll("[.]", ""));
	}

	/**
	 * Determines if "line" can be considered as something that can be used to
	 * instantiate a job object.
	 * 
	 * @param line
	 *            the line in question
	 * @return true if "line" is a useful job descriptor.
	 */
	protected abstract boolean isTraceLine(final String line);

	/**
	 * Allows readers to collect metadata from non-trace lines
	 * 
	 * @param line
	 *            the non-trace line in question
	 */
	protected abstract void metaDataCollector(final String line);

	/**
	 * Parses a single line of the trace and creates a Job object out of it.
	 * 
	 * @param line
	 *            the trace-line to be parsed
	 * @return a job object that is equivalent to the traceline specified in the
	 *         input
	 * @throws IllegalArgumentException
	 *             error using the constructor of the job object
	 * @throws InstantiationException
	 *             error using the constructor of the job object
	 * @throws IllegalAccessException
	 *             error using the constructor of the job object
	 * @throws InvocationTargetException
	 *             error using the constructor of the job object
	 */
	protected abstract Job createJobFromLine(final String line)
			throws IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException;
}
