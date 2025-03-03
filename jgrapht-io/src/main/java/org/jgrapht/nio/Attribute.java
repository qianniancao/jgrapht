/*
 * (C) Copyright 2017-2023, by Dimitrios Michail and Contributors.
 *
 * JGraphT : a free Java graph-theory library
 *
 * See the CONTRIBUTORS.md file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the
 * GNU Lesser General Public License v2.1 or later
 * which is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1-standalone.html.
 *
 * SPDX-License-Identifier: EPL-2.0 OR LGPL-2.1-or-later
 */
package org.jgrapht.nio;

/**
 * An attribute
 * 
 * @author Dimitrios Michail
 */
public interface Attribute
{
    /**
     * Get the value of the attribute
     * 
     * @return the value of the attribute
     */
    String getValue();

    /**
     * Get the type of the attribute
     * 
     * @return the type of the attribute
     */
    AttributeType getType();

}
