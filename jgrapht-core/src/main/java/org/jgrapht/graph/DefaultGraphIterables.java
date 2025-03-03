/*
 * (C) Copyright 2020-2023, by Dimitrios Michail and Contributors.
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
package org.jgrapht.graph;

import java.util.Objects;

import org.jgrapht.Graph;
import org.jgrapht.GraphIterables;

/**
 * The default implementation of the graph iterables which simply delegates to the set
 * implementations.
 *
 * 图可迭代的默认实现，简单地委托给集合实现。
 *
 * @author Dimitrios Michail
 *
 * @param <V> the graph vertex type
 * @param <E> the graph edge type
 */
public class DefaultGraphIterables<V, E>
    implements GraphIterables<V, E>
{
    /**
     * The underlying graph
     * 底层图
     */
    protected Graph<V, E> graph;

    /**
     * Create new graph iterables
     * 创建新的图可迭代（无参构造方法）
     */
    public DefaultGraphIterables()
    {
        this(null);
    }

    /**
     * Create new graph iterables
     * 创建新的图可迭代（有参构造方法）
     * @param graph the underlying graph
     */
    public DefaultGraphIterables(Graph<V, E> graph)
    {
        this.graph = Objects.requireNonNull(graph);
    }

    @Override
    public Graph<V, E> getGraph()
    {
        return graph;
    }

}
