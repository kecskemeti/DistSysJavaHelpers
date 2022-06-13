package hu.mta.sztaki.lpds.cloud.simulator.helpers.serverless.workload.generator;

import org.apache.commons.math3.exception.MathIllegalArgumentException;
import org.apache.commons.math3.genetics.Chromosome;
import org.apache.commons.math3.genetics.MutationPolicy;

public class SimpleMutation implements MutationPolicy{

	@Override
	public Chromosome mutate(Chromosome original) throws MathIllegalArgumentException {
		if(!(original instanceof Chromosome)) {
			System.err.println("Is not type of Chromosome");
		}
		return original;
	}

}
