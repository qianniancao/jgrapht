/*
 * (C) Copyright 2009-2023, by Ilya Razenshteyn and Contributors.
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
package org.jgrapht.util;

import java.io.*;

/**
 * Binary operator for edge weights. There are some prewritten operators.
 */
public interface WeightCombiner
{
    /**
     * Sum of weights.
     */
    WeightCombiner SUM = (WeightCombiner & Serializable) (a, b) -> a + b;

    /**
     * Multiplication of weights.
     */
    WeightCombiner MULT = (WeightCombiner & Serializable) (a, b) -> a * b;

    /**
     * Minimum weight.
     */
    WeightCombiner MIN = (WeightCombiner & Serializable) Math::min;

    /**
     * Maximum weight.
     */
    WeightCombiner MAX = (WeightCombiner & Serializable) Math::max;

    /**
     * First weight.
     */
    WeightCombiner FIRST = (WeightCombiner & Serializable) (a, b) -> a;

    /**
     * Second weight.
     */
    WeightCombiner SECOND = (WeightCombiner & Serializable) (a, b) -> b;

    /**
     * Combines two weights.
     *
     * @param a first weight
     * @param b second weight
     *
     * @return result of the operator
     */
    double combine(double a, double b);
}
