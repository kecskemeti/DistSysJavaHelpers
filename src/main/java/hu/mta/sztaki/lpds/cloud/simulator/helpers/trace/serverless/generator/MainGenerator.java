package hu.mta.sztaki.lpds.cloud.simulator.helpers.trace.serverless.generator;

/**
 * 
 * @author Dilshad Sallo
 * 
 * Main class to generate serverless workload
 *
 */
public class MainGenerator {

	final public static int numLines = 36000;
	final public static int minRange = 1;
	final public static int maxRange = 36001;
	
	public MainGenerator(String traceFileLoc) {
		
		CSVFileReader fileReader = new CSVFileReader(numLines, minRange, maxRange, traceFileLoc);
		long before = System.currentTimeMillis();
		fileReader.generateLines();
		fileReader.GenAvePercentiles();
		long totalExeution  = System.currentTimeMillis() - before;
		System.out.println("Generation took: " + totalExeution + "ms");
		fileReader.generatedExecutionToCSVFile();
		fileReader.generatedMemoryToCSVFile();
		
	}

	public static void main(String[] args) {
		if (args.length < 1) {
			System.out.println("No file exist");
		}else {
			new MainGenerator(args[0]);
		}

	}

}
