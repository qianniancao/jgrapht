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

import java.util.Collection;
import java.util.Set;
import java.util.function.Supplier;

import org.jgrapht.graph.DefaultGraphIterables;

/**
 * The root interface in the graph hierarchy. A mathematical graph-theory graph object
 * <code>G(V,E)</code> contains a set <code>V</code> of vertices and a set <code>
 * E</code> of edges. Each edge e=(v1,v2) in E connects vertex v1 to vertex v2. for more information
 * about graphs and their related definitions see <a href="http://mathworld.wolfram.com/Graph.html">
 * http://mathworld.wolfram.com/Graph.html</a>.
 *
 * 图层次结构中的根接口。图论数学概念中的图对象，G(V,E)包含一个顶点集合和一个边集合。每条边e=(v1,v2)在E中连接顶点v1和顶点v2。
 * 有关图和相关定义的更多信息，请参见http://mathworld.wolfram.com/Graph.html。
 *
 * <p>
 * This library generally follows the terminology found at:
 * <a href="http://mathworld.wolfram.com/topics/GraphTheory.html">
 * http://mathworld.wolfram.com/topics/GraphTheory.html</a>. Implementation of this interface can
 * provide simple-graphs, multigraphs, pseudographs etc. The package <code>org.jgrapht.graph</code>
 * provides a gallery of abstract and concrete graph implementations.
 * </p>
 *
 * 该库中的术语通常遵循以下位置的定义：http://mathworld.wolfram.com/topics/GraphTheory.html。该接口的实现可以提供简单图、多图、伪图等。
 *
 * <p>
 * This library works best when vertices represent arbitrary objects and edges represent the
 * relationships between them. Vertex and edge instances may be shared by more than one graph.
 * </p>
 *
 * 当顶点表示任意对象并且边表示它们之间的关系时，该库效果最佳。顶点和边实例可以由多个图共享。
 *
 * <p>
 * Through generics, a graph can be typed to specific classes for vertices <code>V</code> and edges
 * <code>E&lt;T&gt;</code>. Such a graph can contain vertices of type <code>V</code> and all
 * sub-types and Edges of type <code>
 * E</code> and all sub-types.
 * </p>
 *
 * 通过泛型，图可以被类型化为顶点V和边E的特定类。这样的图可以包含类型V的顶点和所有子类型以及类型E的边和所有子类型。
 *
 * <p>
 * For guidelines on vertex and edge classes, see
 * <a href="https://github.com/jgrapht/jgrapht/wiki/EqualsAndHashCode">this wiki page</a>.
 *
 * 更多关于顶点和边类的指导，请参见https://github.com/jgrapht/jgrapht/wiki/EqualsAndHashCode
 *
 * @param <V> the graph vertex type 图的顶点类型
 * @param <E> the graph edge type 图的边类型
 *
 * @author Barak Naveh
 */
public interface Graph<V, E>
{
    /**
     * Returns a set of all edges connecting source vertex to target vertex if such vertices exist
     * in this graph. If any of the vertices does not exist or is <code>null</code>, returns
     * <code>null</code>. If both vertices exist but no edges found, returns an empty set.
     *
     * 返回连接源顶点到目标顶点的所有边的集合，如果这样的顶点存在于这个图中。
     * 如果任何一个顶点不存在或为null，则返回null。如果两个顶点都存在但没有找到边，则返回一个空集。
     *
     * <p>
     * In undirected graphs, some of the returned edges may have their source and target vertices in
     * the opposite order. In simple graphs the returned set is either singleton set or empty set.
     * </p>
     *
     * 在无向图中，一些返回的边可能具有相反顺序的源和目标顶点。在简单图中，返回的集合是单例集或空集。
     *
     *
     * @param sourceVertex source vertex of the edge. 源顶点
     * @param targetVertex target vertex of the edge. 目标顶点
     *
     * @return a set of all edges connecting source vertex to target vertex.
     *
     * 返回连接源顶点到目标顶点的所有边的集合
     */
    Set<E> getAllEdges(V sourceVertex, V targetVertex);

    /**
     * Returns an edge connecting source vertex to target vertex if such vertices and such edge
     * exist in this graph. Otherwise returns <code>
     * null</code>. If any of the specified vertices is <code>null</code> returns <code>null</code>
     *
     * 如果这样的顶点和这样的边存在于这个图中，则返回连接源顶点到目标顶点的边。否则返回null。如果指定的任何顶点为null，则返回null
     *
     * <p>
     * In undirected graphs, the returned edge may have its source and target vertices in the
     * opposite order.
     * </p>
     *
     * 在无向图中，返回的边可能具有相反顺序的源和目标顶点。
     *
     * @param sourceVertex source vertex of the edge. 源顶点
     * @param targetVertex target vertex of the edge. 目标顶点
     *
     * @return an edge connecting source vertex to target vertex.
     *
     * 返回连接源顶点到目标顶点的边。
     *
     */
    E getEdge(V sourceVertex, V targetVertex);

    /**
     * Return the vertex supplier that the graph uses whenever it needs to create new vertices.
     *
     * 返回图在需要创建新顶点时使用的顶点供应器（supplier）。
     *
     * <p>
     * A graph uses the vertex supplier to create new vertex objects whenever a user calls method
     * {@link Graph#addVertex()}. Users can also create the vertex in user code and then use method
     * {@link Graph#addVertex(Object)} to add the vertex.
     *
     * 图使用顶点供应器在用户调用方法Graph#addVertex()时创建新的顶点对象。用户也可以在用户代码中创建顶点，然后使用方法Graph#addVertex(Object)添加顶点。
     *
     * <p>
     * In contrast with the {@link Supplier} interface, the vertex supplier has the additional
     * requirement that a new and distinct result is returned every time it is invoked. More
     * specifically for a new vertex to be added in a graph <code>v</code> must <i>not</i> be equal
     * to any other vertex in the graph. More formally, the graph must not contain any vertex
     * <code>v2</code> such that <code>v2.equals(v)</code>.
     *
     * 与Supplier接口相反，顶点供应器还有一个额外的要求，即每次调用它时都返回一个新的和不同的结果。更具体地说，为了在图中添加一个新的顶点v，v不能等于图中的任何其他顶点。
     * 更正式地说，图不能包含任何顶点v2，使得v2.equals(v)。
     *
     * <p>
     * Care must also be taken when interchanging calls to methods {@link Graph#addVertex(Object)}
     * and {@link Graph#addVertex()}. In such a case the user must make sure never to add vertices
     * in the graph using method {@link Graph#addVertex(Object)}, which are going to be returned in
     * the future by the supplied vertex supplier. Such a sequence will result into an
     * {@link IllegalArgumentException} when calling method {@link Graph#addVertex()}.
     *
     * 当交换调用方法Graph#addVertex(Object)和Graph#addVertex()时，也必须注意。在这种情况下，用户必须确保永远不会使用方法Graph#addVertex(Object)在图中添加顶点，
     * 这些顶点将来将由提供的顶点供应器返回。这样的序列将在调用方法Graph#addVertex()时导致IllegalArgumentException。
     * @return the vertex supplier or <code>null</code> if the graph has no such supplier
     */
    Supplier<V> getVertexSupplier();

    /**
     * Return the edge supplier that the graph uses whenever it needs to create new edges.
     *
     * 返回图在需要创建新边时使用的边供应器（supplier）。
     *
     * <p>
     * A graph uses the edge supplier to create new edge objects whenever a user calls method
     * {@link Graph#addEdge(Object, Object)}. Users can also create the edge in user code and then
     * use method {@link Graph#addEdge(Object, Object, Object)} to add the edge.
     *
     * 图使用边供应器在用户调用方法Graph#addEdge(Object, Object)时创建新的边对象。用户也可以在用户代码中创建边，然后使用方法Graph#addEdge(Object, Object, Object)添加边。
     *
     * <p>
     * In contrast with the {@link Supplier} interface, the edge supplier has the additional
     * requirement that a new and distinct result is returned every time it is invoked. More
     * specifically for a new edge to be added in a graph <code>e</code> must <i>not</i> be equal to
     * any other edge in the graph (even if the graph allows edge-multiplicity). More formally, the
     * graph must not contain any edge <code>e2</code> such that <code>e2.equals(e)</code>.
     *
     * 与Supplier接口相反，边供应器还有一个额外的要求，即每次调用它时都返回一个新的和不同的结果。更具体地说，为了在图中添加一个新的边e，e不能等于图中的任何其他边（即使这个图允许边多重性）。
     * 更正式地说，图不能包含任何边e2，使得e2.equals(e)。
     *
     * @return the edge supplier <code>null</code> if the graph has no such supplier
     * 返回边供应器，如果图没有这样的供应器，则返回null
     *
     */
    Supplier<E> getEdgeSupplier();

    /**
     * Creates a new edge in this graph, going from the source vertex to the target vertex, and
     * returns the created edge. Some graphs do not allow edge-multiplicity. In such cases, if the
     * graph already contains an edge from the specified source to the specified target, then this
     * method does not change the graph and returns <code>null</code>.
     *
     * 在这个图中创建一个新的边，从源顶点到目标顶点，并返回创建的边。一些图不允许边多重性。在这种情况下，如果图已经包含从指定源到指定目标的边，则此方法不会更改图并返回null。
     *
     * <p>
     * The source and target vertices must already be contained in this graph. If they are not found
     * in graph {@link IllegalArgumentException} is thrown.
     *
     * 源顶点和目标顶点必须已经包含在这个图中。如果它们在图中找不到，则抛出IllegalArgumentException。
     *
     * <p>
     * This method creates the new edge <code>e</code> using this graph's edge supplier (see
     * {@link #getEdgeSupplier()}). For the new edge to be added <code>e</code> must <i>not</i> be
     * equal to any other edge the graph (even if the graph allows edge-multiplicity). More
     * formally, the graph must not contain any edge <code>e2</code> such that
     * <code>e2.equals(e)</code>. If such <code>
     * e2</code> is found then the newly created edge <code>e</code> is abandoned, the method leaves
     * this graph unchanged and returns <code>null</code>.
     *
     * 这个方法使用这个图的边供应器（参见getEdgeSupplier()）创建新的边e。为了添加新的边e，e不能等于图中的任何其他边（即使这个图允许边多重性）。更正式地说，图不能包含任何边e2，使得e2.equals(e)。
     * 如果找到这样的e2，则新创建的边e被放弃，该方法保持该图不变并返回null。
     *
     * <p>
     * If the underlying graph implementation's {@link #getEdgeSupplier()} returns
     * <code>null</code>, then this method cannot create edges and throws an
     * {@link UnsupportedOperationException}.
     *
     * 如果底层图实现的getEdgeSupplier()返回null，则此方法无法创建边并抛出UnsupportedOperationException。
     *
     * @param sourceVertex source vertex of the edge. 源顶点
     * @param targetVertex target vertex of the edge. 目标顶点
     *
     * @return The newly created edge if added to the graph, otherwise <code>
     * null</code>.
     *
     * 新创建的边，如果添加到图中，否则为null。
     *
     * @throws IllegalArgumentException if source or target vertices are not found in the graph.
     * 如果源顶点或目标顶点在图中找不到。
     *
     * @throws NullPointerException if any of the specified vertices is <code>null</code>.
     * 如果任何指定的顶点是null。
     *
     * @throws UnsupportedOperationException if the graph was not initialized with an edge supplier
     * 如果图没有使用边供应器初始化
     *
     * @see #getEdgeSupplier()
     */
    E addEdge(V sourceVertex, V targetVertex);

    /**
     * Adds the specified edge to this graph, going from the source vertex to the target vertex.
     * More formally, adds the specified edge, <code>
     * e</code>, to this graph if this graph contains no edge <code>e2</code> such that
     * <code>e2.equals(e)</code>. If this graph already contains such an edge, the call leaves this
     * graph unchanged and returns <code>false</code>. Some graphs do not allow edge-multiplicity.
     * In such cases, if the graph already contains an edge from the specified source to the
     * specified target, then this method does not change the graph and returns <code>
     * false</code>. If the edge was added to the graph, returns <code>
     * true</code>.
     *
     * 将指定的边添加到这个图中，从源顶点到目标顶点。更正式地说，如果这个图不包含任何边e2，使得e2.equals(e)，则将指定的边e添加到这个图中。如果这个图已经包含这样的边，调用将保持这个图不变并返回false。
     * 一些图不允许边多重性。在这种情况下，如果图已经包含从指定源到指定目标的边，则此方法不会更改图并返回false。如果边被添加到图中，则返回true。
     *
     * <p>
     * The source and target vertices must already be contained in this graph. If they are not found
     * in graph IllegalArgumentException is thrown.
     * </p>
     *
     * 源顶点和目标顶点必须已经包含在这个图中。如果它们在图中找不到，则抛出IllegalArgumentException。
     *
     * @param sourceVertex source vertex of the edge. 源顶点
     * @param targetVertex target vertex of the edge. 目标顶点
     * @param e edge to be added to this graph. 要添加到这个图中的边。
     *
     * @return <code>true</code> if this graph did not already contain the specified edge.
     * 如果这个图没有包含指定的边，则返回true。
     *
     * @throws IllegalArgumentException if source or target vertices are not found in the graph.
     * 如果源顶点或目标顶点在图中找不到。
     *
     * @throws ClassCastException if the specified edge is not assignment compatible with the class
     *         of edges produced by the edge factory of this graph.
     * 如果指定的边与该图的边工厂生成的边的类不兼容。
     *
     * @throws NullPointerException if any of the specified vertices is <code>
     * null</code>.
     * 如果任何指定的顶点是null。
     *
     * @see #addEdge(Object, Object)
     * @see #getEdgeSupplier()
     */
    boolean addEdge(V sourceVertex, V targetVertex, E e);

    /**
     * Creates a new vertex in this graph and returns it.
     * 创建一个新的顶点在这个图中并返回它。
     *
     * <p>
     * This method creates the new vertex <code>v</code> using this graph's vertex supplier (see
     * {@link #getVertexSupplier()}). For the new vertex to be added <code>v</code> must <i>not</i>
     * be equal to any other vertex in the graph. More formally, the graph must not contain any
     * vertex <code>v2</code> such that <code>v2.equals(v)</code>. If such <code>
     * v2</code> is found then the newly created vertex <code>v</code> is abandoned, the method
     * leaves this graph unchanged and throws an {@link IllegalArgumentException}.
     * <p>
     * 此方法使用此图的顶点供应商（请参阅getVertexSupplier()）创建新的顶点v。为了添加新的顶点v，v不能等于图中的任何其他顶点。
     * 更正式地说，图中不能包含任何顶点v2，使得v2.equals(v)。如果找到这样的v2，则新创建的顶点v被放弃，该方法保持此图不变并抛出IllegalArgumentException。
     *
     * If the underlying graph implementation's {@link #getVertexSupplier()} returns
     * <code>null</code>, then this method cannot create vertices and throws an
     * {@link UnsupportedOperationException}.
     * 如果底层图实现的getVertexSupplier()返回null，则此方法无法创建顶点并抛出UnsupportedOperationException。
     *
     * <p>
     * Care must also be taken when interchanging calls to methods {@link Graph#addVertex(Object)}
     * and {@link Graph#addVertex()}. In such a case the user must make sure never to add vertices
     * in the graph using method {@link Graph#addVertex(Object)}, which are going to be returned in
     * the future by the supplied vertex supplier. Such a sequence will result into an
     * {@link IllegalArgumentException} when calling method {@link Graph#addVertex()}.
     *
     * 在交换调用方法Graph.addVertex(Object)和Graph.addVertex()时，也必须小心。在这种情况下，用户必须确保永远不会使用方法Graph.addVertex(Object)在图中添加顶点，
     * 这些顶点将来将由提供的顶点供应商返回。
     * 这样的序列将在调用方法Graph.addVertex()时导致IllegalArgumentException。
     *
     * @return The newly created vertex if added to the graph.
     * 如果添加到图中，则返回新创建的顶点。
     *
     * @throws IllegalArgumentException if the graph supplier returns a vertex which is already in
     *         the graph
     * 如果图供应商返回一个已经在图中的顶点
     *
     * @throws UnsupportedOperationException if the graph was not initialized with a vertex supplier
     * 如果图没有使用顶点供应商初始化
     *
     * @see #getVertexSupplier()
     */
    V addVertex();

    /**
     * Adds the specified vertex to this graph if not already present. More formally, adds the
     * specified vertex, <code>v</code>, to this graph if this graph contains no vertex
     * <code>u</code> such that <code>
     * u.equals(v)</code>. If this graph already contains such vertex, the call leaves this graph
     * unchanged and returns <code>false</code>. In combination with the restriction on
     * constructors, this ensures that graphs never contain duplicate vertices.
     *
     * 如果指定的顶点尚未存在，则将其添加到此图中。更正式地说，如果此图不包含顶点u.equals(v)的顶点u，则将指定的顶点v添加到此图中。
     * 如果此图已经包含这样的顶点，则调用保持此图不变并返回false。与构造函数的限制结合使用，这可以确保图形永远不会包含重复的顶点。
     *
     * @param v vertex to be added to this graph. 将要添加的顶点
     *
     * @return <code>true</code> if this graph did not already contain the specified vertex.
     * 如果此图尚未包含指定的顶点，则返回true。
     *
     * @throws NullPointerException if the specified vertex is <code>
     * null</code>.
     * 如果指定的顶点是null。
     *
     */
    boolean addVertex(V v);

    /**
     * Returns <code>true</code> if and only if this graph contains an edge going from the source
     * vertex to the target vertex. In undirected graphs the same result is obtained when source and
     * target are inverted. If any of the specified vertices does not exist in the graph, or if is
     * <code>
     * null</code>, returns <code>false</code>.
     *
     * 当且仅当此图包含从源顶点到目标顶点的边时，返回true。在无向图中，当源和目标被反转时，得到相同的结果。如果任何指定的顶点在图中不存在，或者为null，则返回false。
     *
     * @param sourceVertex source vertex of the edge. 边的源顶点
     * @param targetVertex target vertex of the edge. 边的目标顶点
     *
     * @return <code>true</code> if this graph contains the specified edge.
     * 如果此图包含指定的边，则返回true。
     */
    boolean containsEdge(V sourceVertex, V targetVertex);

    /**
     * Returns <code>true</code> if this graph contains the specified edge. More formally, returns
     * <code>true</code> if and only if this graph contains an edge <code>e2</code> such that
     * <code>e.equals(e2)</code>. If the specified edge is <code>null</code> returns
     * <code>false</code>.
     *
     * 如果此图包含指定的边，则返回true。更正式地说，当且仅当此图包含边e.equals(e2)的边e2时，返回true。如果指定的边是null，则返回false。
     *
     * @param e edge whose presence in this graph is to be tested.
     * 边的存在性要测试的边
     *
     * @return <code>true</code> if this graph contains the specified edge.
     * 如果此图包含指定的边，则返回true。
     */
    boolean containsEdge(E e);

    /**
     * Returns <code>true</code> if this graph contains the specified vertex. More formally, returns
     * <code>true</code> if and only if this graph contains a vertex <code>u</code> such that
     * <code>u.equals(v)</code>. If the specified vertex is <code>null</code> returns
     * <code>false</code>.
     *
     * 如果此图包含指定的顶点，则返回true。更正式地说，当且仅当此图包含顶点u.equals(v)的顶点u时，返回true。如果指定的顶点是null，则返回false。
     *
     * @param v vertex whose presence in this graph is to be tested.
     * 要测试其在此图中存在的顶点
     *
     * @return <code>true</code> if this graph contains the specified vertex.
     * 如果此图包含指定的顶点，则返回true。
     *
     */
    boolean containsVertex(V v);

    /**
     * Returns a set of the edges contained in this graph. The set is backed by the graph, so
     * changes to the graph are reflected in the set. If the graph is modified while an iteration
     * over the set is in progress, the results of the iteration are undefined.
     *
     * 返回此图中包含的边的集合。该集合由图支持，因此图的更改反映在集合中。如果在对集合进行迭代时修改了图，则迭代的结果是未定义的。
     *
     * <p>
     * The graph implementation may maintain a particular set ordering (e.g. via
     * {@link java.util.LinkedHashSet}) for deterministic iteration, but this is not required. It is
     * the responsibility of callers who rely on this behavior to only use graph implementations
     * which support it.
     * </p>
     *
     * 图实现可以维护特定的集合排序（例如通过java.util.LinkedHashSet）进行确定性迭代，但这不是必需的。依赖此行为的调用者有责任仅使用支持它的图实现。
     *
     * @return a set of the edges contained in this graph.
     * 此图中包含的边的集合。
     */
    Set<E> edgeSet();

    /**
     * Returns the degree of the specified vertex.
     *
     * 返回指定顶点的度。
     *
     * <p>
     * A degree of a vertex in an undirected graph is the number of edges touching that vertex.
     * Edges with same source and target vertices (self-loops) are counted twice.
     * <p>
     *
     * 在无向图中，顶点的度是接触该顶点的边数。具有相同源和目标顶点（自循环）的边计数两次。
     *
     * In directed graphs this method returns the sum of the "in degree" and the "out degree".
     *
     * 在有向图中，此方法返回“入度”和“出度”的总和。
     *
     * @param vertex vertex whose degree is to be calculated. 要计算其度的顶点
     * @return the degree of the specified vertex. 指定顶点的度
     *
     * @throws IllegalArgumentException if vertex is not found in the graph.
     * 如果在图中找不到顶点。
     *
     * @throws NullPointerException if vertex is <code>null</code>.
     * 如果顶点是null。
     *
     * @throws ArithmeticException if the result overflows an int
     * 如果结果溢出int
     */
    int degreeOf(V vertex);

    /**
     * Returns a set of all edges touching the specified vertex. If no edges are touching the
     * specified vertex returns an empty set.
     *
     * 返回接触指定顶点的所有边的集合。如果没有边接触指定的顶点，则返回一个空集。
     *
     * @param vertex the vertex for which a set of touching edges is to be returned.
     * 要返回接触边的顶点
     *
     * @return a set of all edges touching the specified vertex.
     * 所有接触指定顶点的边的集合。
     *
     * @throws IllegalArgumentException if vertex is not found in the graph.
     * 如果在图中找不到顶点。
     *
     * @throws NullPointerException if vertex is <code>null</code>.
     * 如果顶点是null。
     */
    Set<E> edgesOf(V vertex);

    /**
     * Returns the "in degree" of the specified vertex.
     *
     * 返回指定顶点的“入度”。
     *
     * <p>
     * The "in degree" of a vertex in a directed graph is the number of inward directed edges from
     * that vertex. See <a href="http://mathworld.wolfram.com/Indegree.html">
     * http://mathworld.wolfram.com/Indegree.html</a>.
     * <p>
     *
     * 在有向图中，顶点的“入度”是从该顶点开始的内向有向边的数量。请参阅http://mathworld.wolfram.com/Indegree.html。
     *
     * In the case of undirected graphs this method returns the number of edges touching the vertex.
     * Edges with same source and target vertices (self-loops) are counted twice.
     *
     * 在无向图的情况下，此方法返回接触顶点的边数。具有相同源和目标顶点（自循环）的边计数两次。
     *
     * @param vertex vertex whose degree is to be calculated.
     * 要计算其度的顶点
     *
     * @return the degree of the specified vertex.
     * 指定顶点的度
     *
     * @throws IllegalArgumentException if vertex is not found in the graph.
     * 如果在图中找不到顶点。
     *
     * @throws NullPointerException if vertex is <code>null</code>.
     * 如果顶点是null。
     *
     * @throws ArithmeticException if the result overflows an int
     * 如果结果溢出int
     */
    int inDegreeOf(V vertex);

    /**
     * Returns a set of all edges incoming into the specified vertex.
     *
     * 返回所有进入指定顶点的边的集合。
     *
     * <p>
     * In the case of undirected graphs this method returns all edges touching the vertex, thus,
     * some of the returned edges may have their source and target vertices in the opposite order.
     *
     * 在无向图的情况下，此方法返回接触顶点的所有边，因此，返回的某些边可能具有相反顺序的源和目标顶点。
     *
     * @param vertex the vertex for which the list of incoming edges to be returned.
     * 要返回其入边列表的顶点。
     *
     * @return a set of all edges incoming into the specified vertex.
     * 所有进入指定顶点的边的集合。
     *
     * @throws IllegalArgumentException if vertex is not found in the graph.
     * 如果在图中找不到顶点。
     *
     * @throws NullPointerException if vertex is <code>null</code>.
     * 如果顶点是null。
     */
    Set<E> incomingEdgesOf(V vertex);

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
     * 在有向图中，顶点的“出度”是从该顶点开始的外向有向边的数量。请参阅http://mathworld.wolfram.com/Outdegree.html。
     *
     * <p>
     * In the case of undirected graphs this method returns the number of edges touching the vertex.
     * Edges with same source and target vertices (self-loops) are counted twice.
     *
     * 在无向图的情况下，此方法返回接触顶点的边数。具有相同源和目标顶点（自循环）的边计数两次。
     *
     * @param vertex vertex whose degree is to be calculated.
     * 要计算其度的顶点
     *
     * @return the degree of the specified vertex.
     * 指定顶点的度
     *
     * @throws IllegalArgumentException if vertex is not found in the graph.
     * 如果在图中找不到顶点。
     *
     * @throws NullPointerException if vertex is <code>null</code>.
     * 如果顶点是null。
     *
     * @throws ArithmeticException if the result overflows an int
     * 如果结果溢出int
     */
    int outDegreeOf(V vertex);

    /**
     * Returns a set of all edges outgoing from the specified vertex.
     *
     * 返回从指定顶点出发的所有边的集合。
     *
     * <p>
     * In the case of undirected graphs this method returns all edges touching the vertex, thus,
     * some of the returned edges may have their source and target vertices in the opposite order.
     *
     * 在无向图的情况下，此方法返回接触顶点的所有边，因此，返回的某些边可能具有相反顺序的源和目标顶点。
     *
     * @param vertex the vertex for which the list of outgoing edges to be returned.
     * 要返回其出边列表的顶点。
     *
     * @return a set of all edges outgoing from the specified vertex.
     * 从指定顶点出发的所有边的集合。
     *
     * @throws IllegalArgumentException if vertex is not found in the graph.
     * 如果在图中找不到顶点。
     *
     * @throws NullPointerException if vertex is <code>null</code>.
     * 如果顶点是null。
     */
    Set<E> outgoingEdgesOf(V vertex);

    /**
     * Removes all the edges in this graph that are also contained in the specified edge collection.
     * After this call returns, this graph will contain no edges in common with the specified edges.
     * This method will invoke the {@link #removeEdge(Object)} method.
     *
     * 删除此图中也包含在指定边集合中的所有边。此调用返回后，此图将不包含与指定边相同的边。此方法将调用removeEdge（Object）方法。
     *
     * @param edges edges to be removed from this graph.
     * 要从此图中删除的边。
     *
     * @return <code>true</code> if this graph changed as a result of the call
     * 如果此图由于调用而更改，则为true
     *
     * @throws NullPointerException if the specified edge collection is <code>
     * null</code>.
     * 如果指定的边集合为null。
     *
     * @see #removeEdge(Object)
     * @see #containsEdge(Object)
     */
    boolean removeAllEdges(Collection<? extends E> edges);

    /**
     * Removes all the edges going from the specified source vertex to the specified target vertex,
     * and returns a set of all removed edges. Returns <code>null</code> if any of the specified
     * vertices does not exist in the graph. If both vertices exist but no edge is found, returns an
     * empty set. This method will either invoke the {@link #removeEdge(Object)} method, or the
     * {@link #removeEdge(Object, Object)} method.
     *
     * 删除从指定源顶点到指定目标顶点的所有边，并返回所有已删除边的集合。如果任何指定的顶点在图中不存在，则返回null。如果两个顶点都存在但找不到边，则返回一个空集。此方法将调用removeEdge（Object）方法或removeEdge（Object，Object）方法。
     *
     * @param sourceVertex source vertex of the edge.
     * 边的源顶点。
     *
     * @param targetVertex target vertex of the edge.
     * 边的目标顶点。
     *
     * @return the removed edges, or <code>null</code> if either vertex is not part of graph
     * 已删除的边，如果任一顶点不是图的一部分，则为null
     */
    Set<E> removeAllEdges(V sourceVertex, V targetVertex);

    /**
     * Removes all the vertices in this graph that are also contained in the specified vertex
     * collection. After this call returns, this graph will contain no vertices in common with the
     * specified vertices. This method will invoke the {@link #removeVertex(Object)} method.
     *
     * 删除此图中也包含在指定顶点集合中的所有顶点。此调用返回后，此图将不包含与指定顶点相同的顶点。此方法将调用removeVertex（Object）方法。
     *
     * @param vertices vertices to be removed from this graph.
     * 要从此图中删除的顶点。
     *
     * @return <code>true</code> if this graph changed as a result of the call
     * 如果此图由于调用而更改，则为true
     *
     * @throws NullPointerException if the specified vertex collection is <code>
     * null</code>.
     * 如果指定的顶点集合为null。
     *
     * @see #removeVertex(Object)
     * @see #containsVertex(Object)
     */
    boolean removeAllVertices(Collection<? extends V> vertices);

    /**
     * Removes an edge going from source vertex to target vertex, if such vertices and such edge
     * exist in this graph. Returns the edge if removed or <code>null</code> otherwise.
     *
     * 删除从源顶点到目标顶点的边，如果这样的顶点和这样的边存在于此图中。如果删除，则返回边，否则返回null。
     *
     * @param sourceVertex source vertex of the edge. 边的源顶点。
     * @param targetVertex target vertex of the edge. 边的目标顶点。
     *
     * @return The removed edge, or <code>null</code> if no edge removed.
     * 已删除的边，如果没有边删除，则为null。
     */
    E removeEdge(V sourceVertex, V targetVertex);

    /**
     * Removes the specified edge from the graph. Removes the specified edge from this graph if it
     * is present. More formally, removes an edge <code>
     * e2</code> such that <code>e2.equals(e)</code>, if the graph contains such edge. Returns
     * <code>true</code> if the graph contained the specified edge. (The graph will not contain the
     * specified edge once the call returns).
     *
     * 从图中删除指定的边。如果存在，则从此图中删除指定的边。更正式地说，如果图包含这样的边，则删除边e2.equals（e）。如果图包含指定的边，则返回true。 （一旦调用返回，图将不包含指定的边）。
     *
     * <p>
     * If the specified edge is <code>null</code> returns <code>
     * false</code>.
     * </p>
     *
     * 如果指定的边为null，则返回false。
     *
     * @param e edge to be removed from this graph, if present.
     * 要从此图中删除的边，如果存在。
     *
     * @return <code>true</code> if and only if the graph contained the specified edge.
     * 如果且仅当图包含指定的边时，则为true。
     */
    boolean removeEdge(E e);

    /**
     * Removes the specified vertex from this graph including all its touching edges if present.
     * More formally, if the graph contains a vertex <code>
     * u</code> such that <code>u.equals(v)</code>, the call removes all edges that touch
     * <code>u</code> and then removes <code>u</code> itself. If no such <code>u</code> is found,
     * the call leaves the graph unchanged. Returns <code>true</code> if the graph contained the
     * specified vertex. (The graph will not contain the specified vertex once the call returns).
     *
     * 从此图中删除指定的顶点，包括所有其触摸边（如果存在）。更正式地说，如果图包含一个顶点u.equals（v），则调用删除所有触摸u的边，然后删除u本身。
     * 如果没有找到这样的u，则调用保持图形不变。如果图包含指定的顶点，则返回true。 （一旦调用返回，图将不包含指定的顶点）。
     *
     * <p>
     * If the specified vertex is <code>null</code> returns <code>
     * false</code>.
     * </p>
     *
     * 如果指定的顶点为null，则返回false。
     *
     * @param v vertex to be removed from this graph, if present.
     * 要从此图中删除的顶点（如果存在）。
     *
     * @return <code>true</code> if the graph contained the specified vertex; <code>false</code>
     *         otherwise.
     * 如果图包含指定的顶点，则为true; 否则为false。
     */
    boolean removeVertex(V v);

    /**
     * Returns a set of the vertices contained in this graph. The set is backed by the graph, so
     * changes to the graph are reflected in the set. If the graph is modified while an iteration
     * over the set is in progress, the results of the iteration are undefined.
     *
     * 返回此图中包含的顶点集。该集合由图支持，因此图的更改反映在集合中。如果在对集合进行迭代时修改了图形，则迭代的结果是未定义的。
     *
     * <p>
     * The graph implementation may maintain a particular set ordering (e.g. via
     * {@link java.util.LinkedHashSet}) for deterministic iteration, but this is not required. It is
     * the responsibility of callers who rely on this behavior to only use graph implementations
     * which support it.
     * </p>
     *
     * 图实现可以维护特定的集合排序（例如通过java.util.LinkedHashSet）进行确定性迭代，但这不是必需的。依赖此行为的调用者有责任仅使用支持它的图实现。
     *
     * @return a set view of the vertices contained in this graph.
     * 此图中包含的顶点的集合视图。
     */
    Set<V> vertexSet();

    /**
     * Returns the source vertex of an edge. For an undirected graph, source and target are
     * distinguishable designations (but without any mathematical meaning).
     *
     * 返回边的源顶点。对于无向图，源和目标是可区分的指定（但没有任何数学意义）。
     *
     * @param e edge of interest
     * 感兴趣的边
     *
     * @return source vertex
     * 源顶点
     */
    V getEdgeSource(E e);

    /**
     * Returns the target vertex of an edge. For an undirected graph, source and target are
     * distinguishable designations (but without any mathematical meaning).
     *
     * 返回边的目标顶点。对于无向图，源和目标是可区分的指定（但没有任何数学意义）。
     *
     * @param e edge of interest
     * 感兴趣的边
     *
     * @return target vertex
     * 目标顶点
     */
    V getEdgeTarget(E e);

    /**
     * Get the graph type. The graph type can be used to query for additional metadata such as
     * whether the graph supports directed or undirected edges, self-loops, multiple (parallel)
     * edges, weights, etc.
     *
     * 获取图类型。图类型可用于查询其他元数据，例如图是否支持有向或无向边，自循环，多个（并行）边，权重等。
     *
     * @return the graph type
     * 图类型
     */
    GraphType getType();

    /**
     * The default weight for an edge.
     *
     * 边的默认权重。
     */
    double DEFAULT_EDGE_WEIGHT = 1.0;

    /**
     * Returns the weight assigned to a given edge. Unweighted graphs return 1.0 (as defined by
     * {@link #DEFAULT_EDGE_WEIGHT}), allowing weighted-graph algorithms to apply to them when
     * meaningful.
     *
     * 返回分配给给定边的权重。未加权的图返回1.0（由DEFAULT_EDGE_WEIGHT定义），允许加权图算法在有意义时应用于它们。
     *
     * @param e edge of interest
     * 感兴趣的边
     *
     * @return edge weight
     * 边权重
     */
    double getEdgeWeight(E e);

    /**
     * Assigns a weight to an edge.
     *
     * 为边分配权重。
     *
     * @param e edge on which to set weight
     * 要设置权重的边
     *
     * @param weight new weight for edge
     * 边的新权重
     *
     * @throws UnsupportedOperationException if the graph does not support weights
     * 如果图不支持权重
     */
    void setEdgeWeight(E e, double weight);

    /**
     * Assigns a weight to an edge between <code>sourceVertex</code> and <code>targetVertex</code>.
     * If no edge exists between <code>sourceVertex</code> and <code>targetVertex</code> or either
     * of these vertices is <code>null</code>, a <code>NullPointerException</code> is thrown.
     * <p>
     * When there exist multiple edges between <code>sourceVertex</code> and
     * <code>targetVertex</code>, consider using {@link #setEdgeWeight(Object, double)} instead.
     *
     * 为sourceVertex和targetVertex之间的边分配权重。如果sourceVertex和targetVertex之间不存在边缘，或者这些顶点之一为null，则抛出NullPointerException。
     *
     * @param sourceVertex source vertex of the edge 边的源顶点
     * @param targetVertex target vertex of the edge 边的目标顶点
     * @param weight new weight for edge 边的新权重
     *
     * @throws UnsupportedOperationException if the graph does not support weights
     * 如果图不支持权重
     */
    default void setEdgeWeight(V sourceVertex, V targetVertex, double weight)
    {
        this.setEdgeWeight(this.getEdge(sourceVertex, targetVertex), weight);
    }

    /**
     * Access the graph using the {@link GraphIterables} interface. This allows accessing graphs
     * without the restrictions imposed by 32-bit arithmetic. Moreover, graph implementations are
     * free to implement this interface without explicitly materializing intermediate results.
     *
     * 使用GraphIterables接口访问图。这允许访问图，而不受32位算术的限制。此外，图形实现可以自由实现此接口，而无需显式实现中间结果。
     *
     * @return the graph iterables
     * 图迭代器
     *
     */
    default GraphIterables<V, E> iterables()
    {
        return new DefaultGraphIterables<V, E>(this);
    }

}
