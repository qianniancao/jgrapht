/*
 * (C) Copyright 2003-2023, by John V Sichi and Contributors.
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
package org.jgrapht.traverse;

import org.jgrapht.*;
import org.jheaps.*;
import org.jheaps.tree.*;

import java.util.*;
import java.util.function.*;

/**
 * A closest-first iterator for a directed or undirected graph. For this iterator to work correctly
 * the graph must not be modified during iteration. Currently there are no means to ensure that, nor
 * to fail-fast. The results of such modifications are undefined.
 *
 * <p>
 * The metric for <i>closest</i> here is the weighted path length from a start vertex, i.e.
 * Graph.getEdgeWeight(Edge) is summed to calculate path length. Negative edge weights will result
 * in an IllegalArgumentException. Optionally, path length may be bounded by a finite radius. A
 * custom heap implementation can be specified during the construction time. Pairing heap is used by
 * default.
 * </p>
 *
 * @param <V> the graph vertex type
 * @param <E> the graph edge type
 * @author John V. Sichi
 */
public class ClosestFirstIterator<V, E>
    extends CrossComponentIterator<V, E,
        AddressableHeap.Handle<Double, ClosestFirstIterator.QueueEntry<V, E>>>
{
    /**
     * Priority queue of fringe vertices.
     */
    private AddressableHeap<Double, QueueEntry<V, E>> heap;

    /**
     * Maximum distance to search.
     */
    private double radius = Double.POSITIVE_INFINITY;

    private boolean initialized = false;

    /**
     * Creates a new closest-first iterator for the specified graph. Iteration will start at the
     * specified start vertex and will be limited to the connected component that includes that
     * vertex. If the specified start vertex is <code>null</code>, iteration will start at an
     * arbitrary vertex and will not be limited, that is, will be able to traverse all the graph.
     *
     * @param g the graph to be iterated.
     * @param startVertex the vertex iteration to be started.
     */
    public ClosestFirstIterator(Graph<V, E> g, V startVertex)
    {
        this(g, startVertex, Double.POSITIVE_INFINITY);
    }

    /**
     * Creates a new closest-first iterator for the specified graph. Iteration will start at the
     * specified start vertices and will be limited to the subset of the graph reachable from those
     * vertices. Iteration order is based on minimum distance from any of the start vertices,
     * regardless of the order in which the start vertices are supplied. Because of this, the entire
     * traversal is treated as if it were over a single connected component with respect to events
     * fired.
     *
     * @param g the graph to be iterated.
     * @param startVertices the vertices iteration to be started.
     */
    public ClosestFirstIterator(Graph<V, E> g, Iterable<V> startVertices)
    {
        this(g, startVertices, Double.POSITIVE_INFINITY);
    }

    /**
     * Creates a new radius-bounded closest-first iterator for the specified graph. Iteration will
     * start at the specified start vertex and will be limited to the subset of the connected
     * component which includes that vertex and is reachable via paths of weighted length less than
     * or equal to the specified radius. The specified start vertex may not be <code>
     * null</code>.
     *
     * @param g the graph to be iterated.
     * @param startVertex the vertex iteration to be started.
     * @param radius limit on weighted path length, or Double.POSITIVE_INFINITY for unbounded
     *        search.
     */
    public ClosestFirstIterator(Graph<V, E> g, V startVertex, double radius)
    {
        this(
            g, startVertex == null ? null : Collections.singletonList(startVertex), radius,
            PairingHeap::new);
    }

    /**
     * Creates a new radius-bounded closest-first iterator for the specified graph. Iteration will
     * start at the specified start vertex and will be limited to the subset of the connected
     * component which includes that vertex and is reachable via paths of weighted length less than
     * or equal to the specified radius. The specified start vertex may not be <code>
     * null</code>. This algorithm will use the heap supplied by the {@code heapSupplier}.
     *
     * @param g the graph to be iterated.
     * @param startVertex the vertex iteration to be started.
     * @param radius limit on weighted path length, or Double.POSITIVE_INFINITY for unbounded
     *        search.
     * @param heapSupplier supplier of the preferable heap implementation
     */
    public ClosestFirstIterator(
        Graph<V, E> g, V startVertex, double radius,
        Supplier<AddressableHeap<Double, QueueEntry<V, E>>> heapSupplier)
    {
        this(
            g, startVertex == null ? null : Collections.singletonList(startVertex), radius,
            heapSupplier);
    }

    /**
     * Creates a new radius-bounded closest-first iterator for the specified graph. Iteration will
     * start at the specified start vertices and will be limited to the subset of the graph
     * reachable from those vertices via paths of weighted length less than or equal to the
     * specified radius. The specified collection of start vertices may not be <code>null</code>.
     * Iteration order is based on minimum distance from any of the start vertices, regardless of
     * the order in which the start vertices are supplied. Because of this, the entire traversal is
     * treated as if it were over a single connected component with respect to events fired.
     *
     * @param g the graph to be iterated.
     * @param startVertices the vertices iteration to be started.
     * @param radius limit on weighted path length, or Double.POSITIVE_INFINITY for unbounded
     *        search.
     */
    public ClosestFirstIterator(Graph<V, E> g, Iterable<V> startVertices, double radius)
    {
        this(g, startVertices, radius, PairingHeap::new);
    }

    /**
     * Creates a new radius-bounded closest-first iterator for the specified graph. Iteration will
     * start at the specified start vertices and will be limited to the subset of the graph
     * reachable from those vertices via paths of weighted length less than or equal to the
     * specified radius. The specified collection of start vertices may not be <code>null</code>.
     * Iteration order is based on minimum distance from any of the start vertices, regardless of
     * the order in which the start vertices are supplied. Because of this, the entire traversal is
     * treated as if it were over a single connected component with respect to events fired. This
     * algorithm will use the heap supplied by the {@code heapSupplier}.
     *
     * @param g the graph to be iterated.
     * @param startVertices the vertices iteration to be started.
     * @param radius limit on weighted path length, or Double.POSITIVE_INFINITY for unbounded
     *        search.
     * @param heapSupplier supplier of the preferable heap implementation
     */
    public ClosestFirstIterator(
        Graph<V, E> g, Iterable<V> startVertices, double radius,
        Supplier<AddressableHeap<Double, QueueEntry<V, E>>> heapSupplier)
    {
        super(g, startVertices);
        this.radius = radius;
        Objects.requireNonNull(heapSupplier, "Heap supplier cannot be null");
        this.heap = heapSupplier.get();
        checkRadiusTraversal(isCrossComponentTraversal());
        initialized = true;
        if (!crossComponentTraversal) {
            // prime the heap by processing the first start vertex
            hasNext();
            Iterator<V> iter = startVertices.iterator();
            if (iter.hasNext()) {
                // discard the first since we already primed it above
                iter.next();
                // prime the heap with the rest of the start vertices so that
                // we can process them all simultaneously
                while (iter.hasNext()) {
                    V v = iter.next();
                    encounterVertex(v, null);
                }
            }
        }
    }

    // override AbstractGraphIterator
    @Override
    public void setCrossComponentTraversal(boolean crossComponentTraversal)
    {
        if (initialized) {
            checkRadiusTraversal(crossComponentTraversal);
        }
        super.setCrossComponentTraversal(crossComponentTraversal);
    }

    /**
     * Get the weighted length of the shortest path known to the given vertex. If the vertex has
     * already been visited, then it is truly the shortest path length; otherwise, it is the best
     * known upper bound.
     *
     * @param vertex vertex being sought from start vertex
     * @return weighted length of shortest path known, or Double.POSITIVE_INFINITY if no path found
     *         yet
     */
    public double getShortestPathLength(V vertex)
    {
        AddressableHeap.Handle<Double, QueueEntry<V, E>> node = getSeenData(vertex);

        if (node == null) {
            return Double.POSITIVE_INFINITY;
        }

        return node.getKey();
    }

    /**
     * Get the spanning tree edge reaching a vertex which has been seen already in this traversal.
     * This edge is the last link in the shortest known path between the start vertex and the
     * requested vertex. If the vertex has already been visited, then it is truly the minimum
     * spanning tree edge; otherwise, it is the best candidate seen so far.
     *
     * @param vertex the spanned vertex.
     * @return the spanning tree edge, or null if the vertex either has not been seen yet or is a
     *         start vertex.
     */
    public E getSpanningTreeEdge(V vertex)
    {
        AddressableHeap.Handle<Double, QueueEntry<V, E>> node = getSeenData(vertex);

        if (node == null) {
            return null;
        }

        return node.getValue().spanningTreeEdge;
    }

    /**
     * @see CrossComponentIterator#isConnectedComponentExhausted()
     */
    @Override
    protected boolean isConnectedComponentExhausted()
    {
        if (heap.size() == 0) {
            return true;
        } else {
            if (heap.findMin().getKey() > radius) {
                heap.clear();

                return true;
            } else {
                return false;
            }
        }
    }

    /**
     * @see CrossComponentIterator#encounterVertex(Object, Object)
     */
    @Override
    protected void encounterVertex(V vertex, E edge)
    {
        double shortestPathLength;
        if (edge == null) {
            shortestPathLength = 0;
        } else {
            shortestPathLength = calculatePathLength(vertex, edge);
        }
        AddressableHeap.Handle<Double, QueueEntry<V, E>> handle =
            heap.insert(shortestPathLength, new QueueEntry<>(vertex, edge));
        putSeenData(vertex, handle);
    }

    /**
     * Override superclass. When we see a vertex again, we need to see if the new edge provides a
     * shorter path than the old edge.
     *
     * @param vertex the vertex re-encountered
     * @param edge the edge via which the vertex was re-encountered
     */
    @Override
    protected void encounterVertexAgain(V vertex, E edge)
    {
        AddressableHeap.Handle<Double, QueueEntry<V, E>> node = getSeenData(vertex);

        if (node.getValue().frozen) {
            // no improvement for this vertex possible
            return;
        }

        double candidatePathLength = calculatePathLength(vertex, edge);

        if (candidatePathLength < node.getKey()) {
            node.getValue().spanningTreeEdge = edge;
            node.decreaseKey(candidatePathLength);
        }
    }

    /**
     * @see CrossComponentIterator#provideNextVertex()
     */
    @Override
    protected V provideNextVertex()
    {
        AddressableHeap.Handle<Double, QueueEntry<V, E>> node = heap.deleteMin();
        node.getValue().frozen = true;

        return node.getValue().vertex;
    }

    private void assertNonNegativeEdge(E edge)
    {
        if (getGraph().getEdgeWeight(edge) < 0) {
            throw new IllegalArgumentException("negative edge weights not allowed");
        }
    }

    /**
     * Determine weighted path length to a vertex via an edge, using the path length for the
     * opposite vertex.
     *
     * @param vertex the vertex for which to calculate the path length.
     * @param edge the edge via which the path is being extended.
     * @return calculated path length.
     */
    private double calculatePathLength(V vertex, E edge)
    {
        assertNonNegativeEdge(edge);

        V otherVertex = Graphs.getOppositeVertex(getGraph(), edge, vertex);
        AddressableHeap.Handle<Double, QueueEntry<V, E>> otherEntry = getSeenData(otherVertex);

        return otherEntry.getKey() + getGraph().getEdgeWeight(edge);
    }

    private void checkRadiusTraversal(boolean crossComponentTraversal)
    {
        if (crossComponentTraversal && (radius != Double.POSITIVE_INFINITY)) {
            throw new IllegalArgumentException(
                "radius may not be specified for cross-component traversal");
        }
    }

    /**
     * Private data to associate with each entry in the priority queue.
     */
    static class QueueEntry<V, E>
    {
        /**
         * The vertex reached.
         */
        V vertex;

        /**
         * Best spanning tree edge to vertex seen so far.
         */
        E spanningTreeEdge;

        /**
         * True once spanningTreeEdge is guaranteed to be the true minimum.
         */
        boolean frozen;

        QueueEntry(V vertex, E spanningTreeEdge)
        {
            this.vertex = vertex;
            this.spanningTreeEdge = spanningTreeEdge;
        }
    }
}
