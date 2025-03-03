/*
 * (C) Copyright 2017-2023, by Alexandru Valeanu and Contributors.
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
import org.jgrapht.util.*;

import java.util.*;

/**
 * A dynamic programming algorithm for the TSP problem.
 *
 * <p>
 * The travelling salesman problem (TSP) asks the following question: "Given a list of cities and
 * the distances between each pair of cities, what is the shortest possible route that visits each
 * city exactly once and returns to the origin city?".
 *
 * <p>
 * This is an implementation of the Held-Karp algorithm which returns a optimal, minimum-cost
 * Hamiltonian tour. The implementation requires the input graph to contain at least one vertex. The
 * running time is $O(2^{|V|} \times |V|^2)$ and it takes $O(2^{|V|} \times |V|)$ extra memory.
 *
 * <p>
 * See <a href="https://en.wikipedia.org/wiki/Travelling_salesman_problem">wikipedia</a> for more
 * details about TSP.
 *
 * <p>
 * See <a href="https://en.wikipedia.org/wiki/Held%E2%80%93Karp_algorithm">wikipedia</a> for more
 * details about the dynamic programming algorithm.
 *
 * @param <V> the graph vertex type
 * @param <E> the graph edge type
 *
 * @author Alexandru Valeanu
 */
public class HeldKarpTSP<V, E>
    extends HamiltonianCycleAlgorithmBase<V, E>
{

    private double memo(int previousNode, int state, double[][] c, double[][] w)
    {
        // have we seen this state before?
        if (c[previousNode][state] != Double.MIN_VALUE) {
            return c[previousNode][state];
        }

        // no cycle has been found yet
        double totalCost = Double.MAX_VALUE;

        // check if all nodes have been visited (i.e. state + 1 == 2^n)
        if (state == (1 << w.length) - 1) {
            // check if there is a return edge we can use
            if (w[previousNode][0] != Double.MAX_VALUE) {
                totalCost = w[previousNode][0];
            }
        } else {
            // try to find the 'best' next (i.e. unvisited and adjacent to previousNode) node in the
            // tour
            for (int i = 0; i < w.length; i++) {
                if (((state >> i) & 1) == 0 && w[previousNode][i] != Double.MAX_VALUE) {
                    totalCost =
                        Math.min(totalCost, w[previousNode][i] + memo(i, state ^ (1 << i), c, w));
                }
            }
        }

        return c[previousNode][state] = totalCost;
    }

    /**
     * Computes a minimum-cost Hamiltonian tour.
     *
     * @param graph the input graph
     * @return a minimum-cost tour if one exists, null otherwise
     * @throws IllegalArgumentException if the graph contains no vertices
     * @throws IllegalArgumentException if the graph contains more than 31 vertices
     */
    @Override
    public GraphPath<V, E> getTour(Graph<V, E> graph)
    {
        requireNotEmpty(graph);
        final int n = graph.vertexSet().size(); // number of nodes
        if (n == 1) {
            return getSingletonTour(graph);
        }

        if (n > 31) {
            throw new IllegalArgumentException(
                "The internal representation of the dynamic programming state "
                    + "space cannot represent graphs containing more than 31 vertices. "
                    + "The runtime complexity of this implementation, O(2^|V| x |V|^2),  makes it unsuitable "
                    + "for graphs with more than 31 vertices.");
        }

        // Normalize the graph by mapping each vertex to an integer.
        VertexToIntegerMapping<V> vertexToIntegerMapping = Graphs.getVertexToIntegerMapping(graph);

        // W[u, v] = the cost of the minimum weight between u and v
        double[][] w = computeMinimumWeights(vertexToIntegerMapping.getVertexMap(), graph);

        // C[prevNode, state] = the minimum cost of a tour that ends in prevNode and contains all
        // nodes in the bitmask state
        double[][] c = new double[n][1 << n];
        fill(c, Double.MIN_VALUE);

        // start the tour from node 0 (because the tour is a cycle the start vertex does not matter)
        double tourWeight = memo(0, 1, c, w);

        // check if there is no tour
        if (tourWeight == Double.MAX_VALUE) {
            return null;
        }

        List<V> vertexList = reconstructTour(vertexToIntegerMapping.getIndexList(), w, c);
        return vertexListToTour(vertexList, graph);
    }

    private double[][] computeMinimumWeights(Map<V, Integer> vertexMap, Graph<V, E> graph)
    {
        final int n = vertexMap.size();

        double[][] w = new double[n][n];
        fill(w, Double.MAX_VALUE);

        for (E e : graph.edgeSet()) {
            V source = graph.getEdgeSource(e);
            V target = graph.getEdgeTarget(e);

            int u = vertexMap.get(source);
            int v = vertexMap.get(target);

            // use Math.min in case we deal with a multigraph
            w[u][v] = Math.min(w[u][v], graph.getEdgeWeight(e));

            // If the graph is undirected we need to also consider the reverse edge
            if (graph.getType().isUndirected()) {
                w[v][u] = Math.min(w[v][u], graph.getEdgeWeight(e));
            }
        }
        return w;
    }

    private static void fill(double[][] array, double value)
    {
        for (double[] element : array) {
            Arrays.fill(element, value);
        }
    }

    private List<V> reconstructTour(List<V> indexList, double[][] w, double[][] c)
    {
        final int n = indexList.size();
        List<V> vertexList = new ArrayList<>(n);

        int lastNode = 0;
        int lastState = 1;

        vertexList.add(indexList.get(lastNode));

        for (int step = 1; step < n; step++) {
            int nextNode = -1;
            for (int node = 1; node < n; node++) {
                if ((lastState & (1 << node)) == 0 && w[lastNode][node] != Double.MAX_VALUE
                    && c[node][lastState ^ (1 << node)] != Double.MIN_VALUE
                    && Double.compare(
                        c[node][lastState ^ (1 << node)] + w[lastNode][node],
                        c[lastNode][lastState]) == 0)
                {
                    nextNode = node;
                    break;
                }
            }

            assert nextNode != -1;
            vertexList.add(indexList.get(nextNode));
            lastState ^= 1 << nextNode;
            lastNode = nextNode;
        }
        return vertexList;
    }
}
