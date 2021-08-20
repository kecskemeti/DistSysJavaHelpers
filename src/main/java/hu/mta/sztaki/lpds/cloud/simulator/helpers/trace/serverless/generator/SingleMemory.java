package hu.mta.sztaki.lpds.cloud.simulator.helpers.trace.serverless.generator;
/**
 * 
 * @author Dilshad Sallo
 *	
 *	This class represent single memory invocation
 */
public class SingleMemory {

	public double average;
	public double per5;
	public double per1;
	public double per25;
	public double per50;
	public double per75;
	public double per95;
	public double per99;
	public double per100;
	
	public SingleMemory(String average, String per1, String per5, String per25, String per50, String per75,
			String per95, String per99, String per100) {
		this.average = Double.parseDouble(average);
		this.per1 = Double.parseDouble(per1.replace(".0", ""));
		this.per5 = Double.parseDouble(per5.replace(".0", ""));
		this.per25 = Double.parseDouble(per25.replace(".0", ""));
		this.per50 = Double.parseDouble(per50.replace(".0", ""));
		this.per75 = Double.parseDouble(per75.replace(".0", ""));
		this.per95 = Double.parseDouble(per95.replace(".0", ""));
		this.per99 = Double.parseDouble(per99.replace(".0", ""));
		this.per100 = Double.parseDouble(per100.replace(".0", ""));
	}
	
	public SingleMemory(double average, double per1, double per5, double per25, double per50, double per75, double per95, double per99, double per100) {
		this.average = average;
		this.per5 = per5;
		this.per1 = per1;
		this.per25 = per25;
		this.per50 = per50;
		this.per75 = per75;
		this.per95 = per95;
		this.per99 = per99;
		this.per100 = per100;
	}


}
