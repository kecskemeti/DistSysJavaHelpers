package hu.mta.sztaki.lpds.cloud.simulator.helpers.trace.serverless.generator;

/**
 * 
 * @author Dilshad Sallo
 *	
 *	This class represent single execution invocation
 */
public class SingleExecution {
	public double average;
	public double per0;
	public double per1;
	public double per25;
	public double per50;
	public double per75;
	public double per99;
	public double per100;
	
	public SingleExecution(String average, String per0, String per1, String per25, String per50, String per75, String per99, String per100) {
	
		this.average = Double.parseDouble(average);
		this.per0 = Double.parseDouble(per0);
		this.per1 = Double.parseDouble(per1);
		this.per25 = Double.parseDouble(per25);
		this.per50 = Double.parseDouble(per50);
		this.per75 = Double.parseDouble(per75);
		this.per99 = Double.parseDouble(per99);
		this.per100 = Double.parseDouble(per100);
	}
	
	public SingleExecution(double average, double per0, double per1, double per25, double per50, double per75, double per99, double per100) {

		this.average = average;
		this.per0 = per0;
		this.per1 = per1;
		this.per25 = per25;
		this.per50 = per50;
		this.per75 = per75;
		this.per99 = per99;
		this.per100 = per100;
	}

}