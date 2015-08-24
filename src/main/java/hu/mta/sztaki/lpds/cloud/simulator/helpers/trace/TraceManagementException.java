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

package hu.mta.sztaki.lpds.cloud.simulator.helpers.trace;

/**
 * To be thrown if an unforeseen error occured during the trace generation
 * process
 * 
 * @author "Gabor Kecskemeti, Laboratory of Parallel and Distributed Systems, MTA SZTAKI (c) 2015"
 *
 */
public class TraceManagementException extends Exception {
	private static final long serialVersionUID = -5384129045782253106L;

	public TraceManagementException(String message, Exception e) {
		super(message, e);
	}
}