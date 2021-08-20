package hu.mta.sztaki.lpds.cloud.simulator.helpers.trace.serverless.generator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 
 * @author Dilshad Sallo
 *
 */
public class CSVFileReader {
	private int numLines;
	private int minR;
	private int maxR;
	private final File toBeRead;
	private BufferedReader actualReader;
	private static int counterKeep = 2;//remember last one
	// for which lines we want to read
	private ArrayList<Integer> rangeLines = new ArrayList<Integer>();
	
	//Execution Time
	private HashMap<Integer,SingleExecution> oriExecution = new HashMap<Integer,SingleExecution>();
	private HashMap<Integer, SingleExecution> generateExecution = new HashMap<Integer,SingleExecution>();
	
	//Memory
	private HashMap<Integer,SingleMemory> oriMemory = new HashMap<Integer,SingleMemory>();
	private HashMap<Integer,SingleMemory> generateMemory = new HashMap<Integer,SingleMemory>();
	
	public CSVFileReader(int numLines, int minR, int maxR, String file) {
		this.numLines = numLines;
		this.minR = minR;
		this.maxR = maxR;
		if (numLines > (maxR-minR)) {
			throw new RuntimeException("Number of lines is bigger than range");
		}
		toBeRead = new File(file);
	}
	
	public void generateLines() {
		for(int i=0; i < numLines; i++) {
			int number = ThreadLocalRandom.current().nextInt(minR, maxR);
					if (rangeLines.contains(number)) { //not dupulicate line
						i--;
						continue;
					}
					rangeLines.add(number);
		}
		//sort lines
		Collections.sort(rangeLines);
	}
	
	public void GenAvePercentiles() {
		try {
			String line = null;
			final int aveCol = 1444;
			final int mavCol = aveCol + 12;
			final int countCol = 1445;
			actualReader = new BufferedReader(new FileReader(toBeRead));
			line = actualReader.readLine();//skip header
			
			//Determine how many lines must read
			for(int i=0; i<rangeLines.size(); i++) {
				int lineNumber = rangeLines.get(i);
				    //skip and rembember
					for( ; counterKeep<lineNumber; counterKeep++) {
						line = actualReader.readLine();
					}//endSkip
					counterKeep++;
			line = actualReader.readLine();//read each line
			
			if(line != null){
				String[] rowData = line.trim().split(",");
				//Save original average and percentiles
				int max = Integer.parseInt(rowData[aveCol+3]);
				int ave = Integer.parseInt(rowData[aveCol]);
				int count = Integer.parseInt(rowData[countCol]);
				//Execution Time
				if(max != 0 && ave != 0 && count >= 50){
				oriExecution.put(lineNumber, new SingleExecution(rowData[aveCol], rowData[aveCol+4], rowData[aveCol+5], 
						rowData[aveCol+6], rowData[aveCol+7], rowData[aveCol+8], rowData[aveCol+9], rowData[aveCol+10]));
				//Generate
				generateExecution.put(lineNumber, GenExePercentiles.generate(rowData[aveCol+2], rowData[aveCol+3],  rowData[aveCol+4], rowData[aveCol+5], 
						rowData[aveCol+6], rowData[aveCol+7], rowData[aveCol+8], rowData[aveCol+9], rowData[aveCol+10]));
				}//end if to check max and average
				
				//Memory original
				oriMemory.put(lineNumber, new SingleMemory(rowData[mavCol], rowData[mavCol+1], rowData[mavCol+2], 
						rowData[mavCol+3], rowData[mavCol+4], rowData[mavCol+5], rowData[mavCol+6],rowData[mavCol+7], rowData[mavCol+8]));
				//Generate memory
				generateMemory.put(lineNumber, GenMemPercentiles.generate(rowData[mavCol+1], rowData[mavCol+2], 
						rowData[mavCol+3], rowData[mavCol+4], rowData[mavCol+5], rowData[mavCol+6],rowData[mavCol+7], rowData[mavCol+8]));
				
				
			}//end line
		}//endfor
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void generatedExecutionToCSVFile() {
		try {
			RandomAccessFile raf = new RandomAccessFile("generateExecutionGabor.csv", "rw");
			raf.writeBytes("Row"+ "," + "OriginalAve" +" ,"+"GenerateAve"+ "," + "Ori-per0"+ ","+ "Ori-per1" + ","+ "Ori-per25"+
			"," + "Ori-per50"+ "," + "Ori-per75"+ "," +"Ori-per99" +","+ "Ori-per100"+ ","+ "Gen-per0"+ ","+ "Gen-per1" + ","+ "Gen-per25"+
			"," + "Gen-per50"+ "," + "Gen-per75"+ "," +"Gen-per99" +","+ "Gen-per100"+"\n");
			//write values
			Iterator oriEex = oriExecution.entrySet().iterator();
			Iterator genEex = generateExecution.entrySet().iterator();
			
			 while (oriEex.hasNext() && genEex.hasNext()) {
				 Map.Entry oe = (Map.Entry)oriEex.next();
				 Map.Entry ee = (Map.Entry)genEex.next();
				 SingleExecution o = (SingleExecution) oe.getValue();
				 SingleExecution e = (SingleExecution) ee.getValue();
				 raf.writeBytes(oe.getKey() + "," + o.average + "," + e.average + "," + o.per0 + "," + o.per1 + "," + o.per25 + "," +
				 o.per50 + "," + o.per75 + "," + o.per99 + "," + o.per100 + "," + e.per0 + "," + e.per1 + "," + e.per25 + "," +
				 e.per50 + "," + e.per75 + "," + e.per99 + "," + e.per100 +"\n"); 
				 
			 }
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void generatedMemoryToCSVFile() {
		try {
			RandomAccessFile raf = new RandomAccessFile("generateMemoryGabor.csv", "rw");
			raf.writeBytes("Row"+ "," + "OriginalAve" +" ,"+"GenerateAve"+ "," + "Ori-per1"+ ","+ "Ori-per5" + ","+ "Ori-per25"+
			"," + "Ori-per50"+ "," + "Ori-per75"+ "," + "Ori-per95"+ "," +"Ori-per99" +","+ "Ori-per100"+ ","+ "Gen-per1"+ ","+ "Gen-per5" + ","+ "Gen-per25"+
			"," + "Gen-per50"+ "," + "Gen-per75"+ "," +"Gen-per95" +"," +"Gen-per99" +","+ "Gen-per100"+"\n");
			//write values
			Iterator oriMem = oriMemory.entrySet().iterator();
			Iterator genMem = generateMemory.entrySet().iterator();
			
			 while (oriMem.hasNext() && genMem.hasNext()) {
				 Map.Entry oe = (Map.Entry)oriMem.next();
				 Map.Entry ee = (Map.Entry)genMem.next();
				 SingleMemory o = (SingleMemory) oe.getValue();
				 SingleMemory e = (SingleMemory) ee.getValue();
				 raf.writeBytes(oe.getKey() + "," + o.average + "," + e.average + "," + o.per1 + "," + o.per5 + "," + o.per25 + "," +
				 o.per50 + "," + o.per75 + "," + o.per95  +"," + o.per99 + "," + o.per100 + "," + e.per1 + "," + e.per5 + "," + e.per25 + "," +
				 e.per50 + "," + e.per75 + "," + e.per95  +"," + e.per99 + "," + e.per100 +"\n"); 
				 
			 }
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
	}

}