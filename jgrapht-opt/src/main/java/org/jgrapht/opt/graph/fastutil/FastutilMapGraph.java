/*
 * (C) Copyright 2018-2023, by Dimitrios Michail and Contributors.
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
package org.jgrapht.opt.graph.fastutil;

import org.jgrapht.*;
import org.jgrapht.graph.*;

import java.util.function.*;

/**
 * A graph implementation using fastutil's map implementations for storage.
 * 
 * <p>The following example creates a simple undirected weighted graph: <blockquote>
 * 
 * <pre>
 * Graph&lt;String,
 *     DefaultWeightedEdge&gt; g = new FastutilMapGraph&lt;&gt;(
 *         SupplierUtil.createStringSupplier(), SupplierUtil.createDefaultWeightedEdgeSupplier(),
 *         DefaultGraphType.simple().asWeighted());
 * </pre>
 * 
 * </blockquote>
 * 
 * <p>In case you have integer vertices, consider using the {@link FastutilMapIntVertexGraph}.
 *
 * @param <V> the graph vertex type
 * @param <E> the graph edge type
 * 
 * @see FastutilMapIntVertexGraph
 * 
 * @author Dimitrios Michail
 */
public class FastutilMapGraph<V, E>
    extends
    AbstractBaseGraph<V, E>
{
    private static final long serialVersionUID = -2261627370606792673L;

    /**
     * Construct a new graph.
     *
     * @param vertexSupplier the vertex supplier, can be null
     * @param edgeSupplier the edge supplier, can be null
     * @param type the graph type
     * @param fastLookups whether to index vertex pairs to allow (expected) constant time edge
     *        lookups (by vertex endpoints)
     * @throws IllegalArgumentException if the graph type is not supported by this implementation
     */
    public FastutilMapGraph(
        Supplier<V> vertexSupplier, Supplier<E> edgeSupplier, GraphType type, boolean fastLookups)
    {
        super(
            vertexSupplier, edgeSupplier, type,
            fastLookups ? new FastutilFastLookupGSS<>()
                : new FastutilGSS<>());
    }

    /**
     * Construct a new graph.
     * 
     * <p>By default we index vertex pairs to allow (expected) constant time edge lookups.
     *
     * @param vertexSupplier the vertex supplier, can be null
     * @param edgeSupplier the edge supplier, can be null
     * @param type the graph type
     * @throws IllegalArgumentException if the graph type is not supported by this implementation
     */
    public FastutilMapGraph(Supplier<V> vertexSupplier, Supplier<E> edgeSupplier, GraphType type)
    {
        this(vertexSupplier, edgeSupplier, type, true);
    }

}
