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
 *  (C) Copyright 2012-2015, Gabor Kecskemeti (kecskemeti.gabor@sztaki.mta.hu)
 */

package hu.mta.sztaki.lpds.cloud.simulator.helpers.trace.random;

/**
 * Allows to specify a data range and the probability that the data is within
 * that range.
 * 
 * @author "Gabor Kecskemeti, Laboratory of Parallel and Distributed Systems, MTA SZTAKI (c) 2015"
 *
 */
public class RangeSpecifier {
	/**
	 * The relative starting point of the range
	 */
	public final double start;
	/**
	 * The relative endpoint of the range
	 */
	public final double end;
	/**
	 * The likeliness that the data is within the above range
	 */
	public final double probability;

	/**
	 * Ensures that all data is within [0,1] - i.e. it is relative
	 * 
	 * @param val
	 *            the data to be checked
	 * @return <i>true</i> if the data is within the above range
	 */
	private boolean rangecheck(final double val) {
		return val >= 0 && val <= 1;
	}

	/**
	 * Constructs the object and initializes its final variables.
	 * 
	 * @param s
	 *            start of the range
	 * @param e
	 *            end of the range
	 * @param p
	 *            probability of being in the range
	 * 
	 * @throws RuntimeException
	 *             if one or more parameters are out of range according to
	 *             the rangecheck function
	 */
	public RangeSpecifier(final double s, final double e, final double p) {
		if (rangecheck(s) && rangecheck(e) && rangecheck(p)) {
			start = s;
			end = e;
			probability = p;
		} else {
			throw new RuntimeException("Some of the specified range details are not within [0,1]");
		}
	}
}
