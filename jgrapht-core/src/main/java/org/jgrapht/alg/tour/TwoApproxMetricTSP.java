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
package org.jgrapht.alg.tour;

import org.jgrapht.*;
import org.jgrapht.alg.spanning.*;
import org.jgrapht.graph.*;
import org.jgrapht.traverse.*;
import org.jgrapht.util.*;

import java.util.*;

/**
 * A 2-approximation algorithm for the metric TSP problem.
 *
 * <p>
 * The travelling salesman problem (TSP) asks the following question: "Given a list of cities and
 * the distances between each pair of cities, what is the shortest possible route that visits each
 * city exactly once and returns to the origin city?". In the metric TSP, the intercity distances
 * satisfy the triangle inequality.
 *
 * <p>
 * This is an implementation of the folklore algorithm which returns a depth-first ordering of the
 * minimum spanning tree. The algorithm is a 2-approximation assuming that the instance satisfies
 * the triangle inequality. The implementation requires the input graph to be undirected and
 * complete. The running time is $O(|V|^2 \log |V|)$.
 *
 * <p>
 * See <a href="https://en.wikipedia.org/wiki/Travelling_salesman_problem">wikipedia</a> for more
 * details.
 *
 * @param <V> the graph vertex type
 * @param <E> the graph edge type
 *
 * @author Dimitrios Michail
 */
public class TwoApproxMetricTSP<V, E>
    extends HamiltonianCycleAlgorithmBase<V, E>
{

    /**
     * Computes a 2-approximate tour.
     *
     * @param graph the input graph
     * @return a tour
     * @throws IllegalArgumentException if the graph is not undirected
     * @throws IllegalArgumentException if the graph is not complete
     * @throws IllegalArgumentException if the graph contains no vertices
     */
    @Override
    public GraphPath<V, E> getTour(Graph<V, E> graph)
    {
        checkGraph(graph);
        Set<V> vertices = graph.vertexSet();
        int n = vertices.size();
        if (vertices.size() == 1) {
            return getSingletonTour(graph);
        }

        // Create MST
        Graph<V, DefaultEdge> mst = new SimpleGraph<>(null, DefaultEdge::new, false);
        vertices.forEach(mst::addVertex);
        for (E e : new KruskalMinimumSpanningTree<>(graph).getSpanningTree().getEdges()) {
            mst.addEdge(graph.getEdgeSource(e), graph.getEdgeTarget(e));
        }

        // Perform a depth-first-search traversal
        Set<V> found = CollectionUtil.newHashSetWithExpectedSize(n);
        List<V> tour = new ArrayList<>(n + 1);
        V start = vertices.iterator().next();
        Iterator<V> dfsIt = new DepthFirstIterator<>(mst, start);
        while (dfsIt.hasNext()) {
            V v = dfsIt.next();
            if (found.add(v)) {
                tour.add(v);
            }
        }
        // Explicitly build the path.
        return vertexListToTour(tour, graph);
    }
}
