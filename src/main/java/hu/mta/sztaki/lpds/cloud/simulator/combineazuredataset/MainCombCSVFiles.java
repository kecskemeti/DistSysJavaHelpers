/**
 * Combine approach 2 by Dilshad H. Sallo (sallo@iit.uni-miskolc.hu)
 */
package hu.mta.sztaki.lpds.cloud.simulator.combineazuredataset;

public class MainCombCSVFiles {
CombineFiles cf;
	
	public MainCombCSVFiles(String invocationFile, String exeTimeFile, String memoryFile) {
		if (invocationFile.endsWith(".csv") && exeTimeFile.endsWith(".csv") && memoryFile.endsWith(".csv")) {
			cf = new CombineFiles(invocationFile, exeTimeFile, memoryFile);
			long before = System.currentTimeMillis();
			cf.combine();
			cf.generatedirectly();
			System.err.println("Files are combined successfully");
			long after = System.currentTimeMillis();
			System.err.println("Combined time is " + (after - before) / 1000 + " s");
		}else {
			System.out.println("The files are not  in csv formats");
		}
	}
	public static void main(String[] args) {
			//path
			String fileInvocation = "E:\\Bitbucket\\experiments\\invocation.csv";
			String fileExecution = "E:\\Bitbucket\\experiments\\execution.csv";
			String filememory = "E:\\Bitbucket\\experiments\\memory.csv";
			
			new MainCombCSVFiles(fileInvocation, fileExecution, filememory);
	}
}
