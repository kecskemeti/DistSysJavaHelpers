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
 *  (C) Copyright 2016, Gabor Kecskemeti (g.kecskemeti@ljmu.ac.uk)
 *  (C) Copyright 2012-2015, Gabor Kecskemeti (kecskemeti.gabor@sztaki.mta.hu)
 */

package hu.mta.sztaki.lpds.cloud.simulator.helpers.trace.file;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.TreeMap;
import java.util.concurrent.ThreadLocalRandom;

import hu.mta.sztaki.lpds.cloud.simulator.helpers.job.Job;
import hu.mta.sztaki.lpds.cloud.simulator.helpers.job.JobListAnalyser;
import hu.mta.sztaki.lpds.cloud.simulator.helpers.serverless.workload.generator.GenerateExecutionTime;
import hu.mta.sztaki.lpds.cloud.simulator.helpers.serverless.workload.generator.GenerateMemory;
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
	 * Store number of each user with his services and invocations
	 */
	private Map<String, HashMap<Integer, Integer>> userSerInvo;
	/**
	 * Store number of required user with his services and invocations
	 */
	private Map<String, HashMap<Integer, Integer>> userSerInvoRequired;
	/**
	 * Information of header in CSV Azure file
	 */
	private String[] headerComponents;
	/**
	 * Total number of jobs (invocations)
	 */
	private float totInvocDay = 0;
	/**
	 * Store percentage of invocations (users)
	 */
	private HashMap<String, Float> invoPercentage;
	/**
	  * Approximate tasks to generate  
	  */
	private int totalUserGenerator = 0;
	/**
	 * Source and destination path
	 */
	Path path, dest = null;
	/**
	 * For approach of scaling workload with user behavior
	 */
	private boolean scaleWithUserBehaviour = false;
	/**
	 * Users map to store original and map users
	 */
	private TreeMap<String, String> users = new TreeMap<String, String>();
	/**
	 * Executable map to store original and map executable
	 */
	private TreeMap<String, String> executable = new TreeMap<String, String>();
	/**
	 * Counter for user id
	 */
	private  int userid = 0;
	/**
	 * Counter for executable id
	 */
	private  int exeid = 0;
	/**
	 * Range of minutes in a day
	 */
	private  int minMinute,maxMinute = 1440;
	/**
	 * Generate from CSV file
	 */
	protected boolean generateFromCSV = false;
	/**
	 * Trigger type
	 */
	private String trigger = null;
	/**
	 * Min execution time (s)
	 */
	private int minExecution = 0;
	/**
	 * Read Azure trace (true)
	 * Read AWS trace (false)
	 */
	protected boolean azureAws = true;
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
		path = Paths.get(fileName);
	}

	/**
	 * The main trace file reading mechanism of the helpers. The reader ensures
	 * that the lines before "from" (defined in the constructor) are skipped.
	 * 
	 * @param count
	 *            the number of jobs to be read from the current location.
	 */
	private void readTrace(int count) {		
			if(generateFromCSV) {
				if(scaleWithUserBehaviour) {
					try {
						System.err.println(traceKind + " trace file reader starts for: " + toBeRead + " at "
								+ Calendar.getInstance().getTime());
					invoPercentage.clear();
					currentlyOffered = new ArrayList<Job>();
					fastCache = new HashMap<String, Job>();
					String line = null;
					final int aveCol = 1444;
					final int mavCol = aveCol + 12;
					final int countCol = 1445;				
					final int startColum = 4; 
					final int readTotalCol = 1440; 
					final int readTotalPerce = startColum + readTotalCol + 1; 
					final int readTotoMemory = readTotalPerce + 10; 
					final int userIndex = 0;
					actualReader = new BufferedReader(new FileReader(toBeRead));
					line = actualReader.readLine();
					headerComponents = line.trim().split(",");

					while((line = actualReader.readLine()) != null) {
						String[] rowData = line.trim().split(",");
						String traceUser = rowData[userIndex];
						HashMap<Integer, Integer> tempNumSerInvo;
						
						int max = Integer.parseInt(rowData[startColum + readTotalCol+3]);
						int ave = Integer.parseInt(rowData[startColum + readTotalCol]);
						int noInvocations = Integer.parseInt(rowData[startColum + readTotalCol+1]);
						
						if(userSerInvoRequired.containsKey(traceUser)) {
							tempNumSerInvo = new HashMap<Integer, Integer>();
							tempNumSerInvo = userSerInvoRequired.get(traceUser);
						
							Iterator siValues = tempNumSerInvo.entrySet().iterator();
							Map.Entry sivalues = (Map.Entry)siValues.next();
							int valueOfInvocation = (int) sivalues.getValue();
							int valueOfServices = (int) sivalues.getKey();
							
							valueOfServices = valueOfServices > valueOfInvocation ? 5 : valueOfServices;
							if(valueOfInvocation == 0 || traceUser == null) {
								continue;
								}else {
									int invoDueServices = (int) Math.round(valueOfInvocation / valueOfServices);
									if(max != 0 && ave != 0 && invoDueServices > 2) {
									GenerateExecutionTime.GeneExecTime(String.valueOf(invoDueServices), rowData[readTotalPerce + 3] , rowData[readTotalPerce+4],
										rowData[readTotalPerce+5], rowData[readTotalPerce+6], rowData[readTotalPerce+7], rowData[readTotalPerce+8], 
										rowData[readTotalPerce +9], rowData[readTotalPerce+2]);
									PriorityQueue<Double> totExeTime = GenerateExecutionTime.getAllExecTime();
								
									GenerateMemory.GeneMemory(String.valueOf(invoDueServices), rowData[readTotoMemory+2], rowData[readTotoMemory+3], 
										rowData[readTotoMemory+4], rowData[readTotoMemory+5], rowData[readTotoMemory+6],
										rowData[readTotoMemory+7], rowData[readTotoMemory+8], rowData[readTotoMemory+9]);
									PriorityQueue<Double> totalMemory= GenerateMemory.getAllMemory(); 
									
									long minute = ThreadLocalRandom.current().nextInt(minMinute, maxMinute+1);
									String event = rowData[userIndex+3];
									long exeTime = 0;
									long memory = 0;
									
									for(int i = 0; i <invoDueServices; i++) {					
										if(trigger != null)
											if(!event.equals(trigger))
												continue;
										if(minExecution!= 0)
											if(exeTime < (minExecution *1000))
												continue;
										
										if(!totExeTime.isEmpty())
											 exeTime = Math.round(totExeTime.poll());
										if(!totalMemory.isEmpty())
											 memory = Math.round(totalMemory.poll());
										
										String jobLine = getUser(rowData[userIndex]) + "\t" + getExecutable(rowData[userIndex+1] + rowData[userIndex+2]) + "\t" + event +"\t" + minute + "\t" + exeTime + "\t" + memory;
										Job toAdd = createJobFromLine(jobLine);
										if (toAdd == null)
											continue;
										fastCache.put(toAdd.getId(), toAdd);
										}
									totExeTime.clear();
									totalMemory.clear();
									currentlyOffered.addAll(fastCache.values()); 
									fastCache.clear();
								
									if(valueOfServices == 1) {
										while(userSerInvo.containsKey(traceUser)){
											userSerInvo.remove(traceUser);
											}
										}
									}}//end-else
							}//end-if-user
						}
					System.out.println("No of generated Jobs is : " + currentlyOffered.size());
				} catch (Exception e) {
					throw new RuntimeException("Error in line: " + lineIdx, e);
				}
				}else {
					try {
					System.err.println(traceKind + " trace file reader starts for: " + toBeRead + " at "
							+ Calendar.getInstance().getTime());
					currentlyOffered = new ArrayList<Job>();
					fastCache = new HashMap<String, Job>();
					
					String line = null;
				
					final int startColum = 4; 
					final int readTotalCol = 1440; 
					final int readTotalPerce = startColum + readTotalCol + 1;
					final int readTotoMemory = readTotalPerce + 10; 
					actualReader = new BufferedReader(new FileReader(toBeRead));
					line = actualReader.readLine();
					headerComponents = line.trim().split(",");
					//determine which trace will read
					azureAws = ( headerComponents.length > 30) ? true : false;
					
					for(int i=0; i <from; i++)
						line = actualReader.readLine();
					
					if(azureAws) {
					//read form azure data set
					while(count> 0 && (line = actualReader.readLine()) != null) {
						String[] rowData = line.trim().split(",");
						
						int max = Integer.parseInt(rowData[startColum + readTotalCol+3]);
						int ave = Integer.parseInt(rowData[startColum + readTotalCol]);
						int noInvocations = Integer.parseInt(rowData[startColum + readTotalCol +1]);
						if(max != 0 && ave != 0 && noInvocations >= 10) {
							
							GenerateExecutionTime.GeneExecTime(rowData[readTotalPerce], rowData[readTotalPerce + 3] , rowData[readTotalPerce+4],
							rowData[readTotalPerce+5], rowData[readTotalPerce+6], rowData[readTotalPerce+7], rowData[readTotalPerce+8], 
							rowData[readTotalPerce +9], rowData[readTotalPerce+2]);
							PriorityQueue<Double> totExeTime = GenerateExecutionTime.getAllExecTime();
					
							GenerateMemory.GeneMemory(rowData[readTotoMemory], rowData[readTotoMemory+2], rowData[readTotoMemory+3], 
							rowData[readTotoMemory+4], rowData[readTotoMemory+5], rowData[readTotoMemory+6],
							rowData[readTotoMemory+7], rowData[readTotoMemory+8], rowData[readTotoMemory+9]);
							PriorityQueue<Double> totalMemory= GenerateMemory.getAllMemory();
							long exeTime = 0;
							for(int j = (startColum + minMinute) ; j <(startColum + maxMinute); j++ ) {
								int currentRow = Integer.parseInt(rowData[j]);
								String event = rowData[3];
								if(currentRow == 0)
									continue;
								for(int i = 0; i <currentRow; i++) {
									if(!totExeTime.isEmpty())
										 exeTime = Math.round(totExeTime.poll());
									
									if(trigger != null)
										if(!event.equals(trigger))
											continue;
									if(minExecution!= 0)
										if(exeTime < (minExecution *1000))
											continue;
									String jobLine = getUser(rowData[0]) + "\t" + getExecutable(rowData[1] + rowData[2]) + "\t" + event +"\t" + headerComponents[j] + "\t" + exeTime + "\t" + Math.round(totalMemory.poll());
									Job toAdd = createJobFromLine(jobLine);
									if (toAdd == null)
										continue;
									fastCache.put(toAdd.getId(), toAdd);
									}
								}
							totExeTime.clear();
							totalMemory.clear();
							currentlyOffered.addAll(fastCache.values());
							fastCache.clear();
							}//end-if
						count--;
						}//end-while-azure
					}else { //read from AWS
						String header = actualReader.readLine();
						while(count> 0 && (line = actualReader.readLine()) != null) {
							Job toAdd = createJobFromLine(line);
							if (toAdd == null)
								continue;
							fastCache.put(toAdd.getId(), toAdd);
							count--;
						}//end-while-aws
						currentlyOffered.addAll(fastCache.values());
						fastCache.clear();
					}//end-read-aws
					System.out.println("No of generated Jobs is : " + currentlyOffered.size());
				} catch (Exception e) {
					throw new RuntimeException("Error in line: " + lineIdx, e);
				}
					
				}//end-CSV		
			}else {
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
			}}
	protected void scaleWithUserBehaviour(boolean s, int numberJobs) {
		if(s) {
			totalUserGenerator = numberJobs;
			scaleWithUserBehaviour = s;
			calUserServiceInvocations();
			calculatePercentage();
			calculateRequiredInvocations();
		}
	}
	protected void generate(String trigger, int minMinute, int maxMinute, int minExecution) {
		this.minMinute = minMinute;
		this.maxMinute = maxMinute;
		this.trigger = trigger;
		this.minExecution = minExecution;
		generateFromCSV = true;
		readTrace(to-from);
	}
	/**
	 * Calculate services and number of invocations for each user
	 */
	private void calUserServiceInvocations() {
		try {
		userSerInvo =  new HashMap<String, HashMap<Integer, Integer>>();
		String line = null;
		
		final int startColum = 4; //Column to start
		final int readTotalCol = 1440; //Total columns
		final int totalCount = startColum + readTotalCol + 1; //Columns contains total number of jobs and percentiles
		
		actualReader = new BufferedReader(new FileReader(toBeRead));
		line = actualReader.readLine();//Header
		headerComponents = line.trim().split(",");
		
		while((line = actualReader.readLine()) != null) {
	
			String[] rowData = line.trim().split(","); 
			String user = rowData[0];//Get a user(owner hash-id)
			int invocations = Integer.parseInt(rowData[totalCount]);//Get total invocations for a user
			totInvocDay = totInvocDay + invocations;
			
			HashMap<Integer, Integer> dupUserInocationsOld; //Old data (user and invocations)
			HashMap<Integer, Integer> dupUserInocationsNew;//New data
			
			if(!userSerInvo.containsKey(user)) {
				dupUserInocationsOld = new  HashMap<Integer, Integer>();
				dupUserInocationsOld.put(1, invocations);
				userSerInvo.put(user, dupUserInocationsOld);
			}else {
			    dupUserInocationsNew = new  HashMap<Integer, Integer>();
				dupUserInocationsNew = userSerInvo.get(user); //Get service and old invocations for this user
				userSerInvo.remove(user);//Delete to be updated
					
				for (Map.Entry<Integer, Integer> me : dupUserInocationsNew.entrySet())
				{
					int key = me.getKey(); //No of services
					int currentValue  = me.getValue();//No of new invocations
					
					int newValue = currentValue + invocations;
					key = key + 1;
					dupUserInocationsNew.clear();
					dupUserInocationsNew.put(key, newValue);
				}
			userSerInvo.put(user, dupUserInocationsNew);//user with total invocations
			}
		}//end-while
		} catch (IOException e) {
			e.printStackTrace();
		}
	}//end-calUserServiceInvocations
	/**
	 * Calculate percentage of invocations for users
	 */
	private void calculatePercentage() {
		invoPercentage = new HashMap<String, Float>();
		HashMap<Integer, Integer> serviceInvocationDetails;
		Iterator allUserInfo = userSerInvo.entrySet().iterator();//Get all info
		
		while (allUserInfo.hasNext()) {
			 
			 Map.Entry alluserinfo = (Map.Entry) allUserInfo.next();
			 serviceInvocationDetails = new HashMap<Integer, Integer>();				 
			 serviceInvocationDetails  = (HashMap<Integer, Integer>) alluserinfo.getValue();
			 
			 //Iterator for values of invocations
			 Iterator dupInvo = serviceInvocationDetails.entrySet().iterator();
			 
			 Map.Entry invocationValue = (Map.Entry)dupInvo.next();	
			 
			 int value = (int) invocationValue.getValue();//get value of invocations
			 float perValue = (float) ((value / totInvocDay) * 100);
			 invoPercentage.put((String) alluserinfo.getKey(), perValue);		 
		 }
	}
	/**
	 * Calculate Required invocations based on a selected number of jobs by user
	 */
	private void calculateRequiredInvocations() {
		Iterator allUsersServicesInvocations = userSerInvo.entrySet().iterator(); 
		userSerInvoRequired = new HashMap<String, HashMap<Integer, Integer>>();
		HashMap<Integer, Integer> servicesInvocations; 
		while (allUsersServicesInvocations.hasNext()) {
			 Map.Entry alluserserviceinvocations = (Map.Entry) allUsersServicesInvocations.next();	
			 
			 servicesInvocations = new HashMap<Integer, Integer>();				 
			 servicesInvocations  = (HashMap<Integer, Integer>) alluserserviceinvocations.getValue();//Get services number and invocations
			 
			 Iterator serviceinvocation = servicesInvocations.entrySet().iterator();
			 String user = (String) alluserserviceinvocations.getKey();
			 float userPercentage = invoPercentage.get(user); // get percentage for this user

			 while (serviceinvocation.hasNext()) {
				 Map.Entry serviceInvocationNumber = (Map.Entry)serviceinvocation.next();	
				 int serviceNumber = (int) serviceInvocationNumber.getKey();
				 int newInvocationNumber = (int) (((totalUserGenerator * userPercentage) / 100));
				 HashMap<Integer, Integer> newServiceInvocation = new HashMap<Integer, Integer>();	
				 newServiceInvocation.put(serviceNumber, newInvocationNumber);
				 userSerInvoRequired.put((String) alluserserviceinvocations.getKey(), newServiceInvocation);
			 }
		 }
	}
	/**
	 * Provide user id
	 * 
	 * @param original hash-id
	 * @return user id
	 */
	private String getUser(String u) {
		if(!users.containsKey(u)) {
			users.put(u, "U" + userid);
			userid++;
		}
		return users.get(u);
	}
	/**
	 * Provide executable id
	 * 
	 * @param original executable
	 * @return executable id
	 */
	private String getExecutable(String e) {
		if(!executable.containsKey(e)) {
			executable.put(e, "X" + exeid);
			exeid++;
		}
		return executable.get(e);
	}
	/*
	 * Extract information related to users
	 */
	@SuppressWarnings("resource")
	protected void exportData(boolean extract) {
		if(extract) {
			try {
			System.err.println("Total users' tasks file is extracted to "+ path.getParent()+ "\\UsersInformation.csv");
			RandomAccessFile raf;
				raf = new RandomAccessFile(path.getParent() +"/"+"UsersInformation.csv", "rw");
				raf.writeBytes("User ID"+ "," + "Number of Services" +  "," + " Number of Invocations" +  "\n");
				Iterator allUserDetails = userSerInvo.entrySet().iterator();
				HashMap<Integer, Integer> serInvoDetails;
				 while (allUserDetails.hasNext()) {	
					 Map.Entry alluserdetails = (Map.Entry) allUserDetails.next();				 
					 serInvoDetails = new HashMap<Integer, Integer>();				 
					 serInvoDetails  = (HashMap<Integer, Integer>) alluserdetails.getValue();

					 Iterator dupInvo = serInvoDetails.entrySet().iterator();
					while (dupInvo.hasNext()) {
						 Map.Entry t = (Map.Entry)dupInvo.next();	
						 raf.writeBytes(getUser((String) alluserdetails.getKey()) + "," +  t.getKey() + "," + t.getValue() +"\n");
					 }//end-while					 
				 }//end-while
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}//end-try

			try {
				System.err.println("Users' invocations and services required file is extracted to "+  path.getParent()+"\\UsersServicesInvocationsRequired.csv");
				RandomAccessFile raf = new RandomAccessFile(path.getParent() +"/"+"UsersServicesInvocationsRequired.csv", "rw");
				raf.writeBytes("User ID"+ "," + "Number of required Services" +  "," + " Number of required Invocations" +  "\n");
				Iterator allUserDetails = userSerInvoRequired.entrySet().iterator();
				HashMap<Integer, Integer> serInvoDetails;
				 while (allUserDetails.hasNext()) {
						
					 Map.Entry alluserdetails = (Map.Entry) allUserDetails.next();				 
					 serInvoDetails = new HashMap<Integer, Integer>();				 
					 serInvoDetails  = (HashMap<Integer, Integer>) alluserdetails.getValue();

					 Iterator dupInvo = serInvoDetails.entrySet().iterator();
					while (dupInvo.hasNext()) {
						 Map.Entry t = (Map.Entry)dupInvo.next();	
						 raf.writeBytes(getUser((String) alluserdetails.getKey()) + "," +  t.getKey() + "," + t.getValue() +"\n");
					 }
				 }
			} catch (IOException e) {

				e.printStackTrace();
			}
		}//end-if
	}//end-exportData
	/**
	 * Convert generated tasks to SWF format
	 */
	protected void generateSWFFormat() {
		try {
			Collections.sort(currentlyOffered, JobListAnalyser.submitTimeComparator);
			System.err.println("SWF format trace is extracted to "+ path.getParent()+"\\Trace.swf");
			RandomAccessFile raf = new RandomAccessFile(path.getParent() +"/"+"Trace.swf", "rw");
			//Header info
			raf.writeBytes(";\n");
			raf.writeBytes(";\t Serverless workload generator by Dilshad H. Sallo and Gabor Kecskemeti \n");
			raf.writeBytes(";\t Generated Date: " + java.time.LocalDate.now() + " \n");
			raf.writeBytes(";\t Standard Workload Format \n");
			raf.writeBytes(";\n");
			raf.writeBytes(";\t1:\t Job Number\n;\t2:\t Submit Time\n;\t3:\t Wait Time\n;\t4:\t Run Time\n;\t5:\t Number of Allocated Processors\n;\t6:\t CPU Used\n"
					+ ";\t7:\t Used Memory\n;\t8:\t Processors Required\n;\t9:\t Requested Time\n;\t10:\t Requested Memory\n;\t11:\t Status\n;\t12:\t User ID\n;\t13:\t Group ID\n"
					+ ";\t14:\t Executable Number\n;\t15:\t Queue Number\n;\t16:\t Partition Number\n;\t17:\t Preceding Job Number\n;\t18:\t Think Time\n;\n");
			for (int i = 0; i < currentlyOffered.size(); i++) {
				String user =currentlyOffered.get(i).user;
				String exe = currentlyOffered.get(i).executable;
				final double runTime = currentlyOffered.get(i).getExectimeSecs()/1000D;
				//final double rumRemind = currentlyOffered.get(i).getExectimeSecs()/1000;
				user = user.replace("U", "");
				exe = exe.replace("X","");
				//1 job id
				raf.writeBytes("\t"+ currentlyOffered.get(i).getId() 
				// 2 submit time		
				+ " \t " + currentlyOffered.get(i).getSubmittimeSecs() + 
				// 3 wait time
				" \t " +  currentlyOffered.get(i).getQueuetimeSecs() 
				// 4 run time
				+ " \t " + Double.toString(runTime) + 
				// 5 Number of Allocated Processors (job)
				" \t " +  currentlyOffered.get(i).nprocs + 
				// 6 Average CPU Time Used (0)
				" \t " + "0" + 
				// 7 Used Memory
				" \t " + currentlyOffered.get(i).usedMemory 
				// 8 Processor required 
				+ " \t " + "-1" + 
				// 9 user estimate (Requested Time) use the same as  
				" \t " + "-1" +
				// 10 Requested Memory (Memory required)
				" \t " + "-1" + 
				//11 Status 
				" \t " + "1" + 
				//12 User ID
				" \t " + Integer.parseInt(user)+ 
				//13 group id
				" \t " + currentlyOffered.get(i).group.length() + 
				//14 Executable (Application) Number 
				" \t " + Integer.parseInt(exe) + 
				//15 Queue Number (1 or 0)
				" \t " + "1" + 
				// 16 Partition Number 
				" \t " + "-1"+ 
				// 17 Preceding Job Number
				" \t " +  "-1" +
				// 18 Think Time from Preceding Job
				"\t " + " -1"  + "\n");
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	/**
	 * Convert generated tasks to GWF format
	 */
	protected void generateGWFFormat() {
		try {
			Collections.sort(currentlyOffered, JobListAnalyser.submitTimeComparator);
			System.err.println("SWF format trace is extracted to "+ path.getParent()+"\\Trace.gwf");
			RandomAccessFile raf = new RandomAccessFile(path.getParent() +"/"+"Trace.gwf", "rw");
			//Header info
			raf.writeBytes("#\n");
			raf.writeBytes("#\t Serverless workload generator by Dilshad H. Sallo and Gabor Kecskemeti \n");
			raf.writeBytes("#\t Generated Date: " + java.time.LocalDate.now() + " \n");
			raf.writeBytes("#\t Grid Workload Format \n");
			raf.writeBytes("#\n");
			raf.writeBytes("# 1 JobID \n# 2 SubmitTime\t in seconds\n# 3 WaitTime\t in seconds\n# 4 RunTime\t runtime measured in wallclock seconds\n"
					+ "# 5 NProcs\tnumber of allocated processors\n# 6 AverageCPUTimeUsed\t average of CPU time over all allocated processors\n"
					+ "# 7 Used Memory\t average per processor in kilobytes\n# 8 ReqNProcs\t requested number of processors\n# 9 ReqTime\t requested time measured in wallclock seconds\n"
					+ "# 10 ReqMemory\t requested memory (average per processor)\n# 11 Status\t job completed = 1, job failed = 0,\n# "
					+ "12 UserID\t string identifier for user\n# 13 GroupID\t string identifier for group user belongs to\n# "
					+ "14 ExecutableID\t name of executable\n# 15 QueueID\t string identifier for queue\n# 16 PartitionID\t string identifier for partition\n# "
					+ "17 OrigSiteID\t string identifier for submission site\n# 18 LastRunSiteID\t string identifier for execution site\n# "
					+ "19 JobStructure\t single job = UNITARY, composite job = BoT\n# 20 JobStructureParams\t if JobStructure = BoT, contains batch identifier\n# "
					+ "21 UsedNetwork\t used network resources in kilobytes/second\n# 22 UsedLocalDiskSpace\t in megabytes\n# "
					+ "23 UsedResources\t list of comma-separated generic resources (ResourceDescription:Consumption)\n# "
					+ "24 ReqPlatform\t CPUArchitecture,OS,OSVersion\n# 25 ReqNetwork\t in kilobytes/second\n# 26 ReqLocalDiskSpace\t in megabytes\n"
					+ "# 27 ReqResources\t list of comma-separated generic resources (ResourceDescription:Consumption)\n# "
					+ "28 VOID\t identifier for Virtual Organization\n# 29 ProjectID\t identifier for project\n#\n#(fields contain -1 if not available)\n");
			raf.writeBytes("#\n");
			raf.writeBytes("#\n");
			///check these fields
			for (int i = 0; i < currentlyOffered.size(); i++) {
				final double runTime = currentlyOffered.get(i).getExectimeSecs()/1000D;
				raf.writeBytes(currentlyOffered.get(i).getId() 
						+ " \t " + currentlyOffered.get(i).getSubmittimeSecs() 
						+ " \t " + currentlyOffered.get(i).getQueuetimeSecs() 
						+ " \t " + Double.toString(runTime)
						+ " \t " + currentlyOffered.get(i).nprocs 
						+ " \t " + currentlyOffered.get(i).perProcCPUTime 
						+ " \t " + currentlyOffered.get(i).usedMemory 
						+ " \t " +currentlyOffered.get(i).nprocs 
						+  " \t " + "-1" 
						+ " \t " + "-1" 
						+ " \t " + "1" 
						+ " \t " + currentlyOffered.get(i).user 
						+ " \t " + currentlyOffered.get(i).group
						+ " \t " + currentlyOffered.get(i).executable
						+ " \t " + "-1"
						+ " \t " + "-1"
						+ " \t " + "-1"
						+ " \t " + "-1"
						+ " \t " + "-1"
						+ " \t " + "UNITARY"
						+ " \t " + "-1"
						+ " \t " + "-1" 
						+" \t " + "-1"
						+ " \t " + "-1" 
						+ " \t " + "-1" 
						+ " \t " + "-1" 
						+ " \t " + "-1" 
						+ " \t " + "-1" 
						+ " \t " + "-1" + "\n");
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}//endGWFile
	/**
	 * Convert generated task to AWS Lambda format
	 */
	protected void generateAWSTrace() {
		try {			
			System.err.println("AWS trace is extracted to "+ path.getParent()+"\\AWSTrace.csv");
			RandomAccessFile raf = new RandomAccessFile(path.getParent() +"/"+ "AWSTrace.csv", "rw");
			raf.writeBytes("" + "," + "is_cold" + "," + "cpu_info" + "," + "inst_id" + "," + "inst_priv_ip" + "," + "new_id" + "," +
					"exist_id" + "," +"uptime" + "," + "vm_id" + "," + "vm_priv_ip" + "," + "vm_pub_ip" + "," + "start_time" + "," +
					"end_time" + "," + "elapsed_time" + "," + "aws_duration" + "," + "aws_billed_duration" + "," +  "aws_max_mem" + "," +
					"io_speed" + "," +  "client_start_time" + "," + "client_end_time" + "," +   "client_elapsed_time" + "," + "\n");

			Collections.sort(currentlyOffered, JobListAnalyser.submitTimeComparator);
			for (int i = 0; i <currentlyOffered.size(); i++) {
				String vmid = currentlyOffered.get(i).executable;
				String newid = currentlyOffered.get(i).executable + Math.random();
				String cpu = String.format("\"%s,Intel(R) Xeon(R) Processor @ 2.50GHz\"", currentlyOffered.get(i).nprocs);
				String uptime = String.format("\"%s, %s\"", currentlyOffered.get(i).getMidExecInstanceSecs(), currentlyOffered.get(i).getMidExecInstanceSecs());
				long start_time = currentlyOffered.get(i).getSubmittimeSecs() * 1000l;
				long elapsed_time = currentlyOffered.get(i).getExectimeSecs();
				long end_time = start_time + elapsed_time;
				raf.writeBytes(i + "," +
				//is_cold
				currentlyOffered.get(i).is_cold + "," +		
				//cpu_info, comma existed you have to put it
				cpu + "," +				
				//inst_id
				currentlyOffered.get(i).executable + "," +
				//inst_priv_ip
				"192.168. 0.1" + "," +	
				//new_id
				newid + "," +				
				//exist_id
				currentlyOffered.get(i).executable + "," +
				//up-time 
				uptime + "," +			
				//vm_id
				vmid + "," +
				//vm_priv_ip
				"192.168. 0.1" + "," + 
				//vm_pub_ip
				"192.168. 0.1" + "," +				
				//start_time (ms)
				start_time  + "," +
				//end_time(ms)
				end_time+ "," +
				//elapsed_time (ms)
				elapsed_time + "," +
				//aws_duration 
				((currentlyOffered.get(i).getExectimeSecs() * 1000l) + currentlyOffered.get(i).getQueuetimeSecs()) + "," +				
				//aws billed duration
				"200" + "," +
				//aws_max_mem
				currentlyOffered.get(i).usedMemory + "," +
				//io_speed
				"0" + "," +				
				//client_start_time (s)
				(start_time) / 1000d + "," +		
				//client_end_time(s)
				(start_time + elapsed_time) / 1000d + "," +
				//client_elapsed_time (s)
				elapsed_time / 1000d + "," + "\n");
				
			}//end-for
			
			raf.close();
		} catch (IOException e) {
			
			e.printStackTrace();
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
