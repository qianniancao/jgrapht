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
package org.jgrapht;

import org.jgrapht.util.LiveIterableWrapper;

/**
 * Presents a graph as a collection of views suitable for graphs which contain a very large number
 * of vertices or edges. Graph algorithms written these methods can work with graphs without the
 * restrictions imposed by 32-bit arithmetic.
 *
 * 将图表示为视图集合，适用于包含非常多顶点或边的图。使用这些方法编写的图算法可以使用不受32位算术限制的图。
 *
 * <p>
 * Whether the returned iterators support removal of elements is left to the graph implementation.
 * It is the responsibility of callers who rely on this behavior to only use graph implementations
 * which support it.
 * </p>
 *
 * 无论返回的迭代器是否支持删除元素都取决于图实现。依赖此行为的调用者有责任仅使用支持它的图实现。
 *
 * @param <V> the graph vertex type
 * @param <E> the graph edge type
 *
 * @author Dimitrios Michail
 */
public interface GraphIterables<V, E>
{
    /**
     * Get the underlying graph.
     *
     * 获取底层图。
     *
     * @return the underlying graph
     */
    Graph<V, E> getGraph();

    /**
     * Returns an iterable over the edges of the graph.
     *
     * 返回图的边的可迭代对象。
     *
     * <p>
     * Whether the ordering is deterministic, depends on the actual graph implementation. It is the
     * responsibility of callers who rely on this behavior to only use graph implementations which
     * support it.
     *
     * 顺序是否确定取决于实际的图实现。依赖此行为的调用者有责任仅使用支持它的图实现。
     *
     * @return an iterable over the edges of the graph.
     *
     * 图的边的可迭代对象。
     */
    default Iterable<E> edges()
    {
        return new LiveIterableWrapper<>(() -> getGraph().edgeSet());
    }

    /**
     * Return the number of edges in the graph.
     *
     * 返回图中的边数。
     *
     * @return the number of edges.
     */
    default long edgeCount()
    {
        return getGraph().edgeSet().size();
    }

    /**
     * Returns an iterable view over the vertices contained in this graph. The returned iterator is
     * a live view of the vertices. If the graph is modified while an iteration is in progress, the
     * results of the iteration are undefined.
     *
     * 返回此图中包含的顶点的可迭代视图。返回的迭代器是顶点的实时视图。如果在迭代正在进行时修改了图，则迭代的结果是未定义的。
     *
     * <p>
     * The graph implementation may maintain a particular ordering for deterministic iteration, but
     * this is not required. It is the responsibility of callers who rely on this behavior to only
     * use graph implementations which support it.
     * </p>
     *
     * @return an iterable view of the vertices contained in this graph
     */
    default Iterable<V> vertices()
    {
        return new LiveIterableWrapper<>(() -> getGraph().vertexSet());
    }

    /**
     * Return the number of vertices in the graph.
     *
     * 返回图中的顶点数。
     *
     * @return the number of vertices
     */
    default long vertexCount()
    {
        return getGraph().vertexSet().size();
    }

    /**
     * Returns an iterable view over all edges touching the specified vertex. The returned iterators
     * are live views. If the graph is modified while an iteration is in progress, the results of
     * the iteration are undefined. If no edges are touching the specified vertex, the returned
     * iterators are already exhausted.
     *
     * 返回与指定顶点相连的所有边的可迭代视图。返回的迭代器是边的实时视图。如果在迭代正在进行时修改了图，则迭代的结果是未定义的。如果没有边与指定的顶点相连，则返回的迭代器已经耗尽。
     *
     * @param vertex input vertex
     * @return an iterable view of the vertices contained in this graph
     * @throws IllegalArgumentException if vertex is not found in the graph.
     * @throws NullPointerException if vertex is <code>null</code>.
     */
    default Iterable<E> edgesOf(V vertex)
    {
        return new LiveIterableWrapper<>(() -> getGraph().edgesOf(vertex));
    }

    /**
     * Returns the degree of the specified vertex.
     *
     * 返回指定顶点的度。
     *
     * <p>
     * A degree of a vertex in an undirected graph is the number of edges touching that vertex.
     * Edges with same source and target vertices (self-loops) are counted twice.
     *
     * 无向图中顶点的度是与该顶点相连的边数。具有相同源和目标顶点（自循环）的边计数两次。
     *
     * <p>
     * In directed graphs this method returns the sum of the "in degree" and the "out degree".
     *
     * 在有向图中，此方法返回“入度”和“出度”的总和。
     *
     * @param vertex vertex whose degree is to be calculated.
     * @return the degree of the specified vertex.
     *
     * @throws IllegalArgumentException if vertex is not found in the graph.
     * @throws NullPointerException if vertex is <code>null</code>.
     */
    default long degreeOf(V vertex)
    {
        return getGraph().degreeOf(vertex);
    }

    /**
     * Returns an iterable view over all edges incoming into the specified vertex. The returned
     * iterators are live views. If the graph is modified while an iteration is in progress, the
     * results of the iteration are undefined.
     *
     * 返回进入指定顶点的所有边的可迭代视图。返回的迭代器是边的实时视图。如果在迭代正在进行时修改了图，则迭代的结果是未定义的。
     *
     * <p>
     * In the case of undirected graphs the returned iterators return all edges touching the vertex,
     * thus, some of the returned edges may have their source and target vertices in the opposite
     * order.
     *
     * 在无向图的情况下，返回的迭代器返回与顶点相连的所有边，因此，返回的某些边的源和目标顶点的顺序相反。
     *
     * @param vertex input vertex
     * @return an iterable view of all edges incoming into the specified vertex
     * @throws IllegalArgumentException if vertex is not found in the graph.
     * @throws NullPointerException if vertex is <code>null</code>.
     */
    default Iterable<E> incomingEdgesOf(V vertex)
    {
        return new LiveIterableWrapper<>(() -> getGraph().incomingEdgesOf(vertex));
    }

    /**
     * Returns the "in degree" of the specified vertex.
     *
     * 返回指定顶点的“入度”。
     *
     * <p>
     * The "in degree" of a vertex in a directed graph is the number of inward directed edges from
     * that vertex. See <a href="http://mathworld.wolfram.com/Indegree.html">
     * http://mathworld.wolfram.com/Indegree.html</a>.
     *
     * 有向图中顶点的“入度”是从该顶点开始的内向有向边的数量。请参见http://mathworld.wolfram.com/Indegree.html。
     *
     * <p>
     * In the case of undirected graphs this method returns the number of edges touching the vertex.
     * Edges with same source and target vertices (self-loops) are counted twice.
     *
     * 在无向图的情况下，此方法返回与顶点相连的边数。具有相同源和目标顶点（自循环）的边计数两次。
     *
     * @param vertex vertex whose degree is to be calculated.
     * @return the degree of the specified vertex.
     *
     * @throws IllegalArgumentException if vertex is not found in the graph.
     * @throws NullPointerException if vertex is <code>null</code>.
     */
    default long inDegreeOf(V vertex)
    {
        return getGraph().inDegreeOf(vertex);
    }

    /**
     * Returns an iterable view over all edges outgoing into the specified vertex. The returned
     * iterators are live views. If the graph is modified while an iteration is in progress, the
     * results of the iteration are undefined.
     *
     * 返回进入指定顶点的所有边的可迭代视图。返回的迭代器是边的实时视图。如果在迭代正在进行时修改了图，则迭代的结果是未定义的。
     *
     * <p>
     * In the case of undirected graphs the returned iterators return all edges touching the vertex,
     * thus, some of the returned edges may have their source and target vertices in the opposite
     * order.
     *
     * 在无向图的情况下，返回的迭代器返回与顶点相连的所有边，因此，返回的某些边的源和目标顶点的顺序相反。
     *
     * @param vertex input vertex
     * @return an iterable view of all edges outgoing from the specified vertex
     * @throws IllegalArgumentException if vertex is not found in the graph.
     * @throws NullPointerException if vertex is <code>null</code>.
     */
    default Iterable<E> outgoingEdgesOf(V vertex)
    {
        return new LiveIterableWrapper<>(() -> getGraph().outgoingEdgesOf(vertex));
    }

    /**
     * Returns the "out degree" of the specified vertex.
     *
     * 返回指定顶点的“出度”。
     *
     * <p>
     * The "out degree" of a vertex in a directed graph is the number of outward directed edges from
     * that vertex. See <a href="http://mathworld.wolfram.com/Outdegree.html">
     * http://mathworld.wolfram.com/Outdegree.html</a>.
     *
     * 有向图中顶点的“出度”是从该顶点开始的外向有向边的数量。请参见http://mathworld.wolfram.com/Outdegree.html。
     *
     * <p>
     * In the case of undirected graphs this method returns the number of edges touching the vertex.
     * Edges with same source and target vertices (self-loops) are counted twice.
     *
     * 在无向图的情况下，此方法返回与顶点相连的边数。具有相同源和目标顶点（自循环）的边计数两次。
     *
     * @param vertex vertex whose degree is to be calculated.
     * @return the degree of the specified vertex.
     *
     * @throws IllegalArgumentException if vertex is not found in the graph.
     * @throws NullPointerException if vertex is <code>null</code>.
     */
    default long outDegreeOf(V vertex)
    {
        return getGraph().outDegreeOf(vertex);
    }

    /**
     * Returns an iterable view over all edges connecting source vertex to target vertex if such
     * vertices exist in this graph. The returned iterators are live views. If the graph is modified
     * while an iteration is in progress, the results of the iteration are undefined.
     *
     * 如果此类顶点存在，则返回连接源顶点和目标顶点的所有边的可迭代视图。返回的迭代器是边的实时视图。如果在迭代正在进行时修改了图，则迭代的结果是未定义的。
     *
     * If any of the vertices does not exist or is <code>null</code>, returns <code>null</code>. If
     * both vertices exist but no edges found, returns an iterable which returns exhausted
     * iterators.
     *
     * 如果任何一个顶点不存在或为null，则返回null。如果两个顶点都存在但没有找到边，则返回一个可迭代的返回耗尽的迭代器。
     *
     * <p>
     * In undirected graphs, some of the returned edges may have their source and target vertices in
     * the opposite order. In simple graphs the returned set is either singleton set or empty set.
     *
     * 在无向图中，返回的某些边的源和目标顶点的顺序相反。在简单图中，返回的集合是单例集或空集。
     * </p>
     *
     * @param sourceVertex source vertex of the edge.
     * @param targetVertex target vertex of the edge.
     *
     * @return an iterable view of all edges connecting source to target vertex.
     *
     * @throws IllegalArgumentException if vertex is not found in the graph.
     * @throws NullPointerException if vertex is <code>null</code>.
     */
    default Iterable<E> allEdges(V sourceVertex, V targetVertex)
    {
        return new LiveIterableWrapper<>(() -> getGraph().getAllEdges(sourceVertex, targetVertex));
    }

}
