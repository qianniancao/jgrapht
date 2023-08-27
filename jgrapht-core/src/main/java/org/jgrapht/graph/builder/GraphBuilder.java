/*
 * (C) Copyright 2015-2023, by Andrew Chen and Contributors.
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
package org.jgrapht.graph.builder;

import org.jgrapht.*;
import org.jgrapht.graph.*;

import java.util.function.*;

/**
 * A builder class for {@link Graph}. This is a helper class which helps adding vertices and edges
 * into an already constructed graph instance.
 *
 * Graph的构建器类。这是一个帮助类，它帮助将顶点和边添加到已构造的图实例中。
 *
 * <p>
 * Each graph implementation contains a static helper method for the construction of such a builder.
 * For example class {@link DefaultDirectedGraph} contains method
 * {@link DefaultDirectedGraph#createBuilder(Supplier)}.
 *
 * 每个图实现都包含一个静态帮助方法，用于构建这样的构建器。例如，类DefaultDirectedGraph包含方法DefaultDirectedGraph.createBuilder（Supplier）。
 *
 * <p>
 * See {@link GraphTypeBuilder} for a builder of the actual graph instance.
 *
 * 有关实际图实例的构建器，请参见GraphTypeBuilder。
 *
 * @param <V> the graph vertex type 图顶点类型
 * @param <E> the graph edge type 图边类型
 * @param <G> type of the resulting graph 结果图的类型
 *
 * @author Andrew Chen
 * @see GraphTypeBuilder
 */
public class GraphBuilder<V, E, G extends Graph<V, E>>
    extends AbstractGraphBuilder<V, E, G, GraphBuilder<V, E, G>>
{
    /**
     * Creates a builder based on {@code baseGraph}. {@code baseGraph} must be mutable.
     *
     * 创建基于baseGraph的构建器。baseGraph必须是可变的。
     *
     * <p>
     * The recommended way to use this constructor is: {@code new
     * GraphBuilderBase<...>(new YourGraph<...>(...))}.
     *
     * 推荐使用此构造函数的方法是：new GraphBuilderBase <...>（new YourGraph <...>（...））。
     *
     * <p>
     * NOTE: {@code baseGraph} should not be an existing graph. If you want to add an existing graph
     * to the graph being built, you should use the {@link #addVertex(Object)} method.
     *
     * baseGraph不应该是现有的图。如果要将现有图添加到正在构建的图中，则应使用addVertex（Object）方法。
     *
     * @param baseGraph the graph object to base building on
     */
    public GraphBuilder(G baseGraph)
    {
        super(baseGraph);
    }

    @Override
    protected GraphBuilder<V, E, G> self()
    {
        return this;
    }

}
