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
package net.w3des.extjs.ui.test;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class EmptyTest {

    @Test
    public void empty() {
        assertTrue(ExtJSUITest.getDefault().started);
    }
}
