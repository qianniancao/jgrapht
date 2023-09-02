/*
 * (C) Copyright 2005-2023, by John V Sichi and Contributors.
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

import java.util.*;

/**
 * A factory for edge sets. This interface allows the creator of a graph to choose the
 * {@link java.util.Set} implementation used internally by the graph to maintain sets of edges. This
 * provides control over performance tradeoffs between memory and CPU usage.
 *
 * 边集的工厂。 此接口允许图的创建者选择图内部用于维护边集的Set实现。 这提供了在内存和CPU使用之间进行性能权衡的控制。
 *
 * @param <V> the graph vertex type
 * @param <E> the graph edge type
 *
 * @author John V. Sichi
 */
public interface EdgeSetFactory<V, E>
{
    /**
     * Create a new edge set for a particular vertex.
     *
     * @param vertex the vertex for which the edge set is being created; sophisticated factories may
     *        be able to use this information to choose an optimal set representation (e.g.
     *        ArrayUnenforcedSet for a vertex expected to have low degree, and LinkedHashSet for a
     *        vertex expected to have high degree)
     *
     * @return new set
     *
     * 为特定顶点创建新的边集。
     *
     * @param vertex 正在创建边集的顶点； 复杂的工厂可以使用此信息来选择最佳的集合表示（例如，对于预期具有低度的顶点的ArrayUnenforcedSet，对于预期具有高度的顶点的LinkedHashSet）
     *
     * @return 新的集合
     */
    Set<E> createEdgeSet(V vertex);
}
