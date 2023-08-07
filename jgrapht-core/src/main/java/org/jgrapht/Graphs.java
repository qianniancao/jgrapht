/*
 * (C) Copyright 2003-2023, by Barak Naveh and Contributors.
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

import org.jgrapht.graph.*;
import org.jgrapht.util.*;

import java.util.*;
import java.util.function.*;

/**
 * A collection of utilities to assist with graph manipulation.
 *
 * 用于辅助图操作的实用程序集。
 *
 * @author Barak Naveh
 */
public abstract class Graphs
{

    /**
     * Creates a new edge and adds it to the specified graph similarly to the
     * {@link Graph#addEdge(Object, Object)} method.
     *
     * 创建一个新的边并将其添加到指定的图中，类似于Graph#addEdge(Object, Object)方法。
     *
     * @param g the graph for which the edge to be added
     *          要添加边的图
     * @param sourceVertex source vertex of the edge
     *                     边的源顶点
     * @param targetVertex target vertex of the edge
     *                     边的目标顶点
     * @param weight weight of the edge
     *               边的权重
     * @param <V> the graph vertex type
     *           图的顶点类型
     * @param <E> the graph edge type
     *           图的边类型
     *
     * @return The newly created edge if added to the graph, otherwise <code>
     * null</code>.
     *        如果添加到图中，则返回新创建的边，否则返回null。
     *
     * @throws UnsupportedOperationException if the graph has no edge supplier
     *                                      如果图中没有边供应器
     *
     * @see Graph#addEdge(Object, Object)
     */
    public static <V, E> E addEdge(Graph<V, E> g, V sourceVertex, V targetVertex, double weight)
    {
        Supplier<E> edgeSupplier = g.getEdgeSupplier();
        if (edgeSupplier == null) {
            throw new UnsupportedOperationException("Graph contains no edge supplier");
        }
        E e = edgeSupplier.get();

        if (g.addEdge(sourceVertex, targetVertex, e)) {
            g.setEdgeWeight(e, weight);
            return e;
        } else {
            return null;
        }
    }

    /**
     * Adds the specified source and target vertices to the graph, if not already included, and
     * creates a new edge and adds it to the specified graph similarly to the
     * {@link Graph#addEdge(Object, Object)} method.
     *
     * 如果尚未包含，则将指定的源和目标顶点添加到图中，并创建一个新的边并将其添加到指定的图中，类似于Graph#addEdge(Object, Object)方法。
     *
     * @param g the graph for which the specified edge to be added
     * @param sourceVertex source vertex of the edge
     * @param targetVertex target vertex of the edge
     * @param <V> the graph vertex type
     * @param <E> the graph edge type
     *
     * @return The newly created edge if added to the graph, otherwise <code>
     *     如果添加到图中，则返回新创建的边，否则返回null。
     * null</code>.
     */
    public static <V, E> E addEdgeWithVertices(Graph<V, E> g, V sourceVertex, V targetVertex)
    {
        g.addVertex(sourceVertex);
        g.addVertex(targetVertex);

        return g.addEdge(sourceVertex, targetVertex);
    }

    /**
     * Adds the specified edge to the graph, including its vertices if not already included.
     *
     * 将指定的边添加到图中，如果尚未包含，则包括其顶点。
     *
     * @param targetGraph the graph for which the specified edge to be added
     *                    要添加指定边的图
     * @param sourceGraph the graph in which the specified edge is already present
     *                    指定边已经存在的图
     * @param edge edge to add
     *             要添加的边
     * @param <V> the graph vertex type
     *           图的顶点类型
     * @param <E> the graph edge type
     *           图的边类型
     *
     * @return <code>true</code> if the target graph did not already contain the specified edge.
     *        如果目标图尚未包含指定的边，则返回true。
     */
    public static <V,
        E> boolean addEdgeWithVertices(Graph<V, E> targetGraph, Graph<V, E> sourceGraph, E edge)
    {
        V sourceVertex = sourceGraph.getEdgeSource(edge);
        V targetVertex = sourceGraph.getEdgeTarget(edge);

        targetGraph.addVertex(sourceVertex);
        targetGraph.addVertex(targetVertex);

        return targetGraph.addEdge(sourceVertex, targetVertex, edge);
    }

    /**
     * Adds the specified source and target vertices to the graph, if not already included, and
     * creates a new weighted edge and adds it to the specified graph similarly to the
     * {@link Graph#addEdge(Object, Object)} method.
     *
     * 如果尚未包含，则将指定的源和目标顶点添加到图中，并创建一个新的加权边并将其添加到指定的图中，类似于Graph#addEdge(Object, Object)方法。
     *
     * @param g the graph for which the specified edge to be added
     *          要添加指定边的图
     * @param sourceVertex source vertex of the edge
     *                     边的源顶点
     * @param targetVertex target vertex of the edge
     *                     边的目标顶点
     * @param weight weight of the edge
     *               边的权重
     * @param <V> the graph vertex type
     *           图的顶点类型
     * @param <E> the graph edge type
     *           图的边类型
     *
     * @return The newly created edge if added to the graph, otherwise <code>
     *     如果添加到图中，则返回新创建的边，否则返回null。
     * null</code>.
     */
    public static <V,
        E> E addEdgeWithVertices(Graph<V, E> g, V sourceVertex, V targetVertex, double weight)
    {
        g.addVertex(sourceVertex);
        g.addVertex(targetVertex);

        return addEdge(g, sourceVertex, targetVertex, weight);
    }

    /**
     * Adds all the vertices and all the edges of the specified source graph to the specified
     * destination graph. First all vertices of the source graph are added to the destination graph.
     * Then every edge of the source graph is added to the destination graph. This method returns
     * <code>true</code> if the destination graph has been modified as a result of this operation,
     * otherwise it returns <code>false</code>.
     *
     * 将指定源图的所有顶点和所有边添加到指定的目标图中。首先将源图的所有顶点添加到目标图中。然后将源图的每条边添加到目标图中。
     * 如果目标图因此操作而被修改，则此方法返回true，否则返回false。
     *
     * <p>
     * The behavior of this operation is undefined if any of the specified graphs is modified while
     * operation is in progress.
     *
     * 如果在操作正在进行时修改了任何指定的图，则此操作的行为是未定义的。
     * </p>
     *
     * @param destination the graph to which vertices and edges are added
     *                    要添加顶点和边的图
     * @param source the graph used as source for vertices and edges to add
     *               用作要添加的顶点和边的源的图
     * @param <V> the graph vertex type
     *           图的顶点类型
     * @param <E> the graph edge type
     *           图的边类型
     *
     * @return <code>true</code> if and only if the destination graph has been changed as a result
     *         of this operation.
     *         如果且仅当目标图因此操作而更改时，则返回true。
     */
    public static <V,
        E> boolean addGraph(Graph<? super V, ? super E> destination, Graph<V, E> source)
    {
        boolean modified = addAllVertices(destination, source.vertexSet());
        modified |= addAllEdges(destination, source, source.edgeSet());

        return modified;
    }

    /**
     * Adds all the vertices and all the edges of the specified source digraph to the specified
     * destination digraph, reversing all of the edges. If you want to do this as a linked view of
     * the source graph (rather than by copying to a destination graph), use
     * {@link EdgeReversedGraph} instead.
     *
     * 将指定源图的所有顶点和所有边添加到指定的目标图中，反转所有边。
     * 如果要将此作为源图的链接视图（而不是通过复制到目标图）执行，则使用EdgeReversedGraph。
     *
     * <p>
     * The behavior of this operation is undefined if any of the specified graphs is modified while
     * operation is in progress.
     *
     * 如果在操作正在进行时修改了任何指定的图，则此操作的行为是未定义的。
     *
     * @param destination the graph to which vertices and edges are added
     *                    要添加顶点和边的图
     * @param source the graph used as source for vertices and edges to add
     *               用作要添加的顶点和边的源的图
     * @param <V> the graph vertex type
     *           图的顶点类型
     * @param <E> the graph edge type
     *           图的边类型
     *
     * @see EdgeReversedGraph
     */
    public static <V,
        E> void addGraphReversed(Graph<? super V, ? super E> destination, Graph<V, E> source)
    {
        if (!source.getType().isDirected() || !destination.getType().isDirected()) {
            throw new IllegalArgumentException("graph must be directed");
        }

        addAllVertices(destination, source.vertexSet());

        for (E edge : source.edgeSet()) {
            destination.addEdge(source.getEdgeTarget(edge), source.getEdgeSource(edge));
        }
    }

    /**
     * Adds a subset of the edges of the specified source graph to the specified destination graph.
     * The behavior of this operation is undefined if either of the graphs is modified while the
     * operation is in progress. {@link #addEdgeWithVertices} is used for the transfer, so source
     * vertexes will be added automatically to the target graph.
     *
     * 将指定源图的边的子集添加到指定的目标图中。如果在操作正在进行时修改了任何一个图，则此操作的行为是未定义的。
     * 使用addEdgeWithVertices进行传输，因此源顶点将自动添加到目标图中。
     *
     * @param destination the graph to which edges are to be added
     * @param source the graph used as a source for edges to add
     * @param edges the edges to be added
     * @param <V> the graph vertex type
     * @param <E> the graph edge type
     *
     * @return <code>true</code> if this graph changed as a result of the call
     */
    public static <V, E> boolean addAllEdges(
        Graph<? super V, ? super E> destination, Graph<V, E> source, Collection<? extends E> edges)
    {
        boolean modified = false;

        for (E e : edges) {
            V s = source.getEdgeSource(e);
            V t = source.getEdgeTarget(e);
            destination.addVertex(s);
            destination.addVertex(t);
            modified |= destination.addEdge(s, t, e);
        }

        return modified;
    }

    /**
     * Adds all of the specified vertices to the destination graph. The behavior of this operation
     * is undefined if the specified vertex collection is modified while the operation is in
     * progress. This method will invoke the {@link Graph#addVertex(Object)} method.
     *
     * 将所有指定的顶点添加到目标图中。如果在操作正在进行时修改了指定的顶点集合，则此操作的行为是未定义的。
     * 此方法将调用Graph.addVertex（Object）方法。
     *
     * @param destination the graph to which edges are to be added
     *                    要添加边的图
     * @param vertices the vertices to be added to the graph
     *                 要添加到图中的顶点
     * @param <V> the graph vertex type
     *           图的顶点类型
     * @param <E> the graph edge type
     *           图的边类型
     *
     * @return <code>true</code> if graph changed as a result of the call
     *        如果图因调用而更改，则返回true
     *
     * @throws NullPointerException if the specified vertices contains one or more null vertices, or
     *         if the specified vertex collection is <code>
     * null</code>.
     *        如果指定的顶点包含一个或多个空顶点，或者如果指定的顶点集合为null。
     *
     * @see Graph#addVertex(Object)
     */
    public static <V, E> boolean addAllVertices(
        Graph<? super V, ? super E> destination, Collection<? extends V> vertices)
    {
        boolean modified = false;

        for (V v : vertices) {
            modified |= destination.addVertex(v);
        }

        return modified;
    }

    /**
     * Returns a list of vertices that are the neighbors of a specified vertex. If the graph is a
     * multigraph vertices may appear more than once in the returned list.
     *
     * 返回一个列表，其中包含指定顶点的邻居。如果图是多图，则顶点可能会多次出现在返回的列表中。
     *
     * <p>
     * The method uses {@link Graph#edgesOf(Object)} to traverse the graph.
     *
     * 该方法使用Graph.edgesOf（Object）遍历图。
     *
     * @param g the graph to look for neighbors in
     *          要查找邻居的图
     * @param vertex the vertex to get the neighbors of
     *               要获取邻居的顶点
     * @param <V> the graph vertex type
     *           图的顶点类型
     * @param <E> the graph edge type
     *           图的边类型
     *
     * @return a list of the vertices that are the neighbors of the specified vertex.
     *        一个列表，其中包含指定顶点的邻居。
     */
    public static <V, E> List<V> neighborListOf(Graph<V, E> g, V vertex)
    {
        List<V> neighbors = new ArrayList<>();

        for (E e : g.iterables().edgesOf(vertex)) {
            neighbors.add(getOppositeVertex(g, e, vertex));
        }

        return neighbors;
    }

    /**
     * Returns a set of vertices that are neighbors of a specified vertex.
     *
     * 返回一组顶点，这些顶点是指定顶点的邻居。
     *
     * @param g the graph to look for neighbors in
     *          要查找邻居的图
     * @param vertex the vertex to get the neighbors of
     *               要获取邻居的顶点
     * @param <V> the graph vertex type
     *
     * @param <E> the graph edge type
     * @return a set of the vertices that are neighbors of the specified vertex
     */
    public static <V, E> Set<V> neighborSetOf(Graph<V, E> g, V vertex)
    {
        Set<V> neighbors = new LinkedHashSet<>();

        for (E e : g.iterables().edgesOf(vertex)) {
            neighbors.add(Graphs.getOppositeVertex(g, e, vertex));
        }

        return neighbors;
    }

    /**
     * Returns a list of vertices that are the direct predecessors of a specified vertex. If the
     * graph is a multigraph, vertices may appear more than once in the returned list.
     *
     * 返回一个列表，其中包含指定顶点的直接前驱。如果图是多图，则顶点可能会多次出现在返回的列表中。
     *
     * <p>
     * The method uses {@link Graph#incomingEdgesOf(Object)} to traverse the graph.
     *
     * 该方法使用Graph.incomingEdgesOf（Object）遍历图。
     *
     * @param g the graph to look for predecessors in
     *          要查找前驱的图
     * @param vertex the vertex to get the predecessors of
     *               要获取前驱的顶点
     * @param <V> the graph vertex type
     *           图的顶点类型
     * @param <E> the graph edge type
     *           图的边类型
     *
     * @return a list of the vertices that are the direct predecessors of the specified vertex.
     *       一个列表，其中包含指定顶点的直接前驱。
     */
    public static <V, E> List<V> predecessorListOf(Graph<V, E> g, V vertex)
    {
        List<V> predecessors = new ArrayList<>();

        for (E e : g.iterables().incomingEdgesOf(vertex)) {
            predecessors.add(getOppositeVertex(g, e, vertex));
        }

        return predecessors;
    }

    /**
     * Returns a list of vertices that are the direct successors of a specified vertex. If the graph
     * is a multigraph vertices may appear more than once in the returned list.
     *
     * 返回一个列表，其中包含指定顶点的直接后继。如果图是多图，则顶点可能会多次出现在返回的列表中。
     *
     * <p>
     * The method uses {@link Graph#outgoingEdgesOf(Object)} to traverse the graph.
     *
     * 该方法使用Graph.outgoingEdgesOf（Object）遍历图。
     *
     * @param g the graph to look for successors in
     *          要查找后继的图
     * @param vertex the vertex to get the successors of
     *               要获取后继的顶点
     * @param <V> the graph vertex type
     *           图的顶点类型
     * @param <E> the graph edge type
     *           图的边类型
     *
     * @return a list of the vertices that are the direct successors of the specified vertex.
     *      一个列表，其中包含指定顶点的直接后继。
     */
    public static <V, E> List<V> successorListOf(Graph<V, E> g, V vertex)
    {
        List<V> successors = new ArrayList<>();

        for (E e : g.iterables().outgoingEdgesOf(vertex)) {
            successors.add(getOppositeVertex(g, e, vertex));
        }

        return successors;
    }

    /**
     * Returns an undirected view of the specified graph. If the specified graph is directed,
     * returns an undirected view of it. If the specified graph is already undirected, just returns
     * it.
     *
     * 返回指定图的无向视图。如果指定的图是有向的，则返回其无向视图。如果指定的图已经是无向的，则只返回它。
     *
     * @param g the graph for which an undirected view is to be returned
     *          要返回无向视图的图
     * @param <V> the graph vertex type
     *           图的顶点类型
     * @param <E> the graph edge type
     *           图的边类型
     *
     * @return an undirected view of the specified graph, if it is directed, or or the specified
     *         graph itself if it is already undirected.
     *         指定图的无向视图（如果它是有向的），或者如果它已经是无向的，则指定的图本身。
     *
     * @throws IllegalArgumentException if the graph is neither directed nor undirected
     *                                 如果图既不是有向的也不是无向的
     * @see AsUndirectedGraph
     */
    public static <V, E> Graph<V, E> undirectedGraph(Graph<V, E> g)
    {
        if (g.getType().isDirected()) {
            return new AsUndirectedGraph<>(g);
        } else if (g.getType().isUndirected()) {
            return g;
        } else {
            throw new IllegalArgumentException("graph must be either directed or undirected");
        }
    }

    /**
     * Tests whether an edge is incident to a vertex.
     *
     * 测试边缘是否与顶点相邻。
     *
     * @param g graph containing e and v
     *          包含e和v的图
     * @param e edge in g
     *          g中的边缘
     * @param v vertex in g
     *          g中的顶点
     * @param <V> the graph vertex type
     *           图的顶点类型
     * @param <E> the graph edge type
     *           图的边类型
     *
     * @return true iff e is incident on v
     *        当且仅当e与v相邻时为真
     */
    public static <V, E> boolean testIncidence(Graph<V, E> g, E e, V v)
    {
        return (g.getEdgeSource(e).equals(v)) || (g.getEdgeTarget(e).equals(v));
    }

    /**
     * Gets the vertex opposite another vertex across an edge.
     *
     * 获取边缘上另一个顶点的对面顶点。
     *
     * @param g graph containing e and v
     *          包含e和v的图
     * @param e edge in g
     *          g中的边缘
     * @param v vertex in g
     *          g中的顶点
     * @param <V> the graph vertex type
     *           图的顶点类型
     * @param <E> the graph edge type
     *           图的边类型
     *
     * @return vertex opposite to v across e
     *        在e对面的顶点v
     */
    public static <V, E> V getOppositeVertex(Graph<V, E> g, E e, V v)
    {
        V source = g.getEdgeSource(e);
        V target = g.getEdgeTarget(e);
        if (v.equals(source)) {
            return target;
        } else if (v.equals(target)) {
            return source;
        } else {
            throw new IllegalArgumentException("no such vertex: " + v.toString());
        }
    }

    /**
     * Removes the given vertex from the given graph. If the vertex to be removed has one or more
     * predecessors, the predecessors will be connected directly to the successors of the vertex to
     * be removed.
     *
     * 从给定的图中删除给定的顶点。如果要删除的顶点有一个或多个前驱，那么前驱节点将直接连接到要删除的顶点的后继。
     *
     * @param graph graph to be mutated
     *              要改变的图
     * @param vertex vertex to be removed from this graph, if present
     *               要从此图中删除的顶点（如果存在）
     * @param <V> the graph vertex type
     *           图的顶点类型
     * @param <E> the graph edge type
     *           图的边类型
     *
     * @return true if the graph contained the specified vertex; false otherwise.
     *        如果图包含指定的顶点，则为true；否则为false。
     */
    public static <V, E> boolean removeVertexAndPreserveConnectivity(Graph<V, E> graph, V vertex)
    {
        if (!graph.containsVertex(vertex)) {
            return false;
        }

        if (vertexHasPredecessors(graph, vertex)) {
            List<V> predecessors = Graphs.predecessorListOf(graph, vertex);
            List<V> successors = Graphs.successorListOf(graph, vertex);

            for (V predecessor : predecessors) {
                addOutgoingEdges(graph, predecessor, successors);
            }
        }

        graph.removeVertex(vertex);
        return true;
    }

    /**
     * Filters vertices from the given graph and subsequently removes them. If the vertex to be
     * removed has one or more predecessors, the predecessors will be connected directly to the
     * successors of the vertex to be removed.
     *
     * 从给定的图中过滤顶点，然后删除它们。如果要删除的顶点有一个或多个前驱，那么前驱节点将直接连接到要删除的顶点的后继。
     *
     * @param graph graph to be mutated
     *              要改变的图
     * @param predicate a non-interfering stateless predicate to apply to each vertex to determine
     *        if it should be removed from the graph
     *                  一个非干扰的无状态谓词，用于应用于每个顶点，以确定是否应该从图中删除它
     *                  （译者注：predicate是一个函数式接口，它接受一个参数，返回一个boolean值）
     * @param <V> the graph vertex type
     *           图的顶点类型
     * @param <E> the graph edge type
     *           图的边类型
     *
     * @return true if at least one vertex has been removed; false otherwise.
     *       如果至少删除了一个顶点，则为true；否则为false。
     */
    public static <V,
        E> boolean removeVerticesAndPreserveConnectivity(Graph<V, E> graph, Predicate<V> predicate)
    {
        List<V> verticesToRemove = new ArrayList<>();

        for (V node : graph.vertexSet()) {
            if (predicate.test(node)) {
                verticesToRemove.add(node);
            }
        }

        return removeVertexAndPreserveConnectivity(graph, verticesToRemove);
    }

    /**
     * Removes all the given vertices from the given
     * graph. If the vertex to be removed has one or
     * more predecessors, the predecessors will be connected directly to the successors of the
     * vertex to be removed.
     *
     * 从给定的图中删除所有给定的顶点。如果要删除的顶点有一个或多个前驱，那么前驱节点将直接连接到要删除的顶点的后继。
     *
     * @param graph to be mutated
     *              要改变的图
     * @param vertices vertices to be removed from this graph, if present
     *                 要从此图中删除的顶点（如果存在）
     * @param <V> the graph vertex type
     *           图的顶点类型
     * @param <E> the graph edge type
     *           图的边类型
     *
     * @return true if at least one vertex has been removed; false otherwise.
     *      如果至少删除了一个顶点，则为true；否则为false。
     */
    public static <V,
        E> boolean removeVertexAndPreserveConnectivity(Graph<V, E> graph, Iterable<V> vertices)
    {
        boolean atLeastOneVertexHasBeenRemoved = false;

        for (V vertex : vertices) {
            if (removeVertexAndPreserveConnectivity(graph, vertex)) {
                atLeastOneVertexHasBeenRemoved = true;
            }
        }

        return atLeastOneVertexHasBeenRemoved;
    }

    /**
     * Add edges from one source vertex to multiple target vertices. Whether duplicates are created
     * depends on the underlying {@link Graph} implementation.
     *
     * 从一个源顶点添加边到多个目标顶点。是否创建重复取决于底层的{@link Graph}实现。
     *
     * @param graph graph to be mutated
     *              要改变的图
     * @param source source vertex of the new edges
     *               新边的源顶点
     * @param targets target vertices for the new edges
     *                新边的目标顶点
     * @param <V> the graph vertex type
     *           图的顶点类型
     * @param <E> the graph edge type
     *           图的边类型
     */
    public static <V, E> void addOutgoingEdges(Graph<V, E> graph, V source, Iterable<V> targets)
    {
        if (!graph.containsVertex(source)) {
            graph.addVertex(source);
        }
        for (V target : targets) {
            if (!graph.containsVertex(target)) {
                graph.addVertex(target);
            }
            graph.addEdge(source, target);
        }
    }

    /**
     * Add edges from multiple source vertices to one target vertex. Whether duplicates are created
     * depends on the underlying {@link Graph} implementation.
     *
     * 从多个源顶点添加边到一个目标顶点。是否创建重复取决于底层的{@link Graph}实现。
     *
     * @param graph graph to be mutated
     *              要改变的图
     * @param target target vertex for the new edges
     *               新边的目标顶点
     * @param sources source vertices for the new edges
     *                新边的源顶点
     * @param <V> the graph vertex type
     *           图的顶点类型
     * @param <E> the graph edge type
     *           图的边类型
     */
    public static <V, E> void addIncomingEdges(Graph<V, E> graph, V target, Iterable<V> sources)
    {
        if (!graph.containsVertex(target)) {
            graph.addVertex(target);
        }
        for (V source : sources) {
            if (!graph.containsVertex(source)) {
                graph.addVertex(source);
            }
            graph.addEdge(source, target);
        }
    }

    /**
     * Check if a vertex has any direct successors.
     *
     * 检查顶点是否有任何直接后继。
     *
     * @param graph the graph to look for successors
     *              要查找后继的图
     * @param vertex the vertex to look for successors
     *               要查找后继的顶点
     * @param <V> the graph vertex type
     *           图的顶点类型
     * @param <E> the graph edge type
     *           图的边类型
     *
     * @return true if the vertex has any successors, false otherwise
     *     如果顶点有任何后继，则为true；否则为false。
     */
    public static <V, E> boolean vertexHasSuccessors(Graph<V, E> graph, V vertex)
    {
        return !graph.outgoingEdgesOf(vertex).isEmpty();
    }

    /**
     * Check if a vertex has any direct predecessors.
     *
     * @param graph the graph to look for predecessors
     * @param vertex the vertex to look for predecessors
     * @param <V> the graph vertex type
     * @param <E> the graph edge type
     *
     * @return true if the vertex has any predecessors, false otherwise
     */
    public static <V, E> boolean vertexHasPredecessors(Graph<V, E> graph, V vertex)
    {
        return !graph.incomingEdgesOf(vertex).isEmpty();
    }

    /**
     * Compute a new mapping from the vertices of a graph to the integer range $[0, n)$ where $n$ is
     * the number of vertices in the graph.
     *
     * @param graph the input graph
     * @param <V> the graph vertex type
     * @param <E> the graph edge type
     * @throws NullPointerException if {@code graph} is {@code null}
     *
     * @return the mapping as an object containing the {@code vertexMap} and the {@code indexList}
     *
     * @see VertexToIntegerMapping
     */
    public static <V, E> VertexToIntegerMapping<V> getVertexToIntegerMapping(Graph<V, E> graph)
    {
        return new VertexToIntegerMapping<>(Objects.requireNonNull(graph).vertexSet());
    }
}
