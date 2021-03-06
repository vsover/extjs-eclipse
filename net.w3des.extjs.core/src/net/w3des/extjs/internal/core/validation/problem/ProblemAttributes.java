/*******************************************************************************
 * Copyright (c) 2013 w3des.net and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *  
 * Contributors:
 *      w3des.net - initial API and implementation
 ******************************************************************************/
package net.w3des.extjs.internal.core.validation.problem;

public class ProblemAttributes {
	
	/**
	 * Name of core version
	 * @see ProblemIdentifier#ENV_MISSING_CORE
	 */
	public static final String KEY_CORE_VERSION = "extjs-version";
	
	/**
	 * Name of the library
	 * @see ProblemIdentifier#LIB_INCOMPATIBLE_VERSION
	 * @see ProblemIdentifier#LIB_MISSING
	 */
	public static final String KEY_LIBRARYNAME = "extjs-library";
	
	/**
	 * Name of the library
	 * @see ProblemIdentifier#PRJ_INCOMPATIBLE_VERSION
	 */
	public static final String KEY_PROJECTNAME = "extjs-project";

}
