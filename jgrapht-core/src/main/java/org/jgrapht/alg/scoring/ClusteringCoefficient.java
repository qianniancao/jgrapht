/*
 * (C) Copyright 2018-2023, by Alexandru Valeanu and Contributors.
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
package org.jgrapht.alg.scoring;

import org.jgrapht.*;
import org.jgrapht.alg.interfaces.*;
import org.jgrapht.alg.util.*;

import java.util.*;

/**
 * Clustering coefficient. This implementation computes the global, the local and the average
 * clustering coefficient in an undirected or a directed network.
 *
 * <p>
 * The
 * <a href="https://en.wikipedia.org/wiki/Clustering_coefficient#Local_clustering_coefficient">local
 * clustering coefficient</a> of a vertex in a graph quantifies how close its neighbors are to being
 * a clique. For a vertex $v$ it counts how many of its direct neighbors are connected by an edge
 * over the total number of neighbor pairs. In the case of undirected graphs the total number of
 * possible neighbor pairs is only half compared to directed graphs.
 * 
 * <p>
 * The local clustering coefficient of a graph was introduced in <i>D. J. Watts and Steven Strogatz
 * (June 1998). "Collective dynamics of 'small-world' networks". Nature. 393 (6684): 440–442.
 * doi:10.1038/30918</i>. It is simply the average of the local clustering coefficients of all the
 * vertices of the graph.
 *
 * <p>
 * The global clustering coefficient of a graph is based on triplets of nodes. A triplet is three
 * graph nodes which are connected either by two edges or by three edges. A triplet which is
 * connected by two edges, is called an open triplet. A triplet which is connected with three edges
 * is called a closed triplet. The global clustering coefficient is defined as the number of closed
 * triplets over the total number of triplets (open and closed). It was introduced in <i>R. D. Luce
 * and A. D. Perry (1949). "A method of matrix analysis of group structure". Psychometrika. 14 (1):
 * 95–116. doi:10.1007/BF02289146</i>.
 *
 * <p>
 * The running time is $O(|V| + \Delta(G)^2)$ where $|V|$ is the number of vertices and $\Delta(G)$
 * is the maximum degree of a vertex. The space complexity is $O(|V|)$.
 *
 * @param <V> the graph vertex type
 * @param <E> the graph edge type
 *
 * @author Alexandru Valeanu
 */
public class ClusteringCoefficient<V, E>
    implements VertexScoringAlgorithm<V, Double>
{

    /**
     * Underlying graph
     */
    private final Graph<V, E> graph;

    /**
     * The actual scores
     */
    private Map<V, Double> scores;

    private boolean fullyComputedMap = false;

    /**
     * Global Clustering Coefficient
     */
    private boolean computed = false;
    private double globalClusteringCoefficient;

    /**
     * Average Clustering Coefficient
     */
    private boolean computedAverage = false;
    private double averageClusteringCoefficient;

    /**
     * Construct a new instance
     *
     * @param graph the input graph
     * @throws NullPointerException if {@code graph} is {@code null}
     */
    public ClusteringCoefficient(Graph<V, E> graph)
    {
        this.graph = Objects.requireNonNull(graph);
        this.scores = new HashMap<>();
    }

    /**
     * Computes the global clustering coefficient. The global clustering coefficient $C$ is defined
     * as $C = 3 \times number\_of\_triangles / number\_of\_triplets$.
     *
     * <p>
     * A triplet is three nodes that are connected by either two (open triplet) or three (closed
     * triplet) undirected ties.
     * </p>
     *
     * @return the global clustering coefficient
     */
    public double getGlobalClusteringCoefficient()
    {
        if (!computed) {
            computeGlobalClusteringCoefficient();
        }

        return globalClusteringCoefficient;
    }

    /**
     * Computes the average clustering coefficient. The average clustering coefficient $\={C}$ is
     * defined as $\={C} = \frac{\sum_{i=1}^{n} C_i}{n}$ where $n$ is the number of vertices.
     *
     * Note: the average is $0$ if the graph is empty
     *
     * @return the average clustering coefficient
     */
    public double getAverageClusteringCoefficient()
    {
        if (graph.vertexSet().isEmpty())
            return 0;

        if (!computedAverage) {
            computeFullScoreMap();
            computedAverage = true;
            averageClusteringCoefficient = 0;

            for (double value : scores.values())
                averageClusteringCoefficient += value;

            averageClusteringCoefficient /= graph.vertexSet().size();
        }

        return averageClusteringCoefficient;
    }

    private void computeGlobalClusteringCoefficient()
    {
        NeighborCache<V, E> neighborCache = new NeighborCache<>(graph);
        computed = true;
        double numberTriplets = 0;

        for (V v : graph.vertexSet()) {
            if (graph.getType().isUndirected()) {
                numberTriplets += 1.0 * graph.degreeOf(v) * (graph.degreeOf(v) - 1) / 2;
            } else {
                numberTriplets += 1.0 * neighborCache.predecessorsOf(v).size()
                    * neighborCache.successorsOf(v).size();
            }
        }

        globalClusteringCoefficient = 3 * GraphMetrics.getNumberOfTriangles(graph) / numberTriplets;
    }

    private double computeLocalClusteringCoefficient(V v)
    {
        if (scores.containsKey(v)) {
            return scores.get(v);
        }

        NeighborCache<V, E> neighborCache = new NeighborCache<>(graph);
        Set<V> neighbourhood = neighborCache.neighborsOf(v);

        final double k = neighbourhood.size();
        double numberTriplets = 0;

        for (V p : neighbourhood)
            for (V q : neighbourhood)
                if (graph.containsEdge(p, q))
                    numberTriplets++;

        if (k <= 1)
            return 0.0;
        else
            return numberTriplets / (k * (k - 1));
    }

    private void computeFullScoreMap()
    {
        if (fullyComputedMap) {
            return;
        }

        fullyComputedMap = true;

        for (V v : graph.vertexSet()) {
            if (scores.containsKey(v)) {
                continue;
            }

            scores.put(v, computeLocalClusteringCoefficient(v));
        }
    }

    /**
     * Get a map with the local clustering coefficients of all vertices
     *
     * @return a map with all local clustering coefficients
     */
    @Override
    public Map<V, Double> getScores()
    {
        computeFullScoreMap();
        return Collections.unmodifiableMap(scores);
    }

    /**
     * Get a vertex's local clustering coefficient
     *
     * @param v the vertex
     * @return the local clustering coefficient
     */
    @Override
    public Double getVertexScore(V v)
    {
        if (!graph.containsVertex(v)) {
            throw new IllegalArgumentException("Cannot return score of unknown vertex");
        }

        return computeLocalClusteringCoefficient(v);
    }
}
