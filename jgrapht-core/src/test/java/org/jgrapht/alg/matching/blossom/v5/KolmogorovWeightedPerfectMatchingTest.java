/*
 * (C) Copyright 2018-2023, by Timofey Chudakov and Contributors.
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
package org.jgrapht.alg.matching.blossom.v5;

import org.jgrapht.*;
import org.jgrapht.alg.interfaces.*;
import org.jgrapht.generate.*;
import org.jgrapht.graph.*;
import org.jgrapht.util.*;
import org.junit.*;
import org.junit.runner.*;
import org.junit.runners.*;

import java.util.*;

import static org.jgrapht.alg.matching.blossom.v5.KolmogorovWeightedPerfectMatching.EPS;
import static org.jgrapht.alg.matching.blossom.v5.ObjectiveSense.MAXIMIZE;
import static org.jgrapht.alg.matching.blossom.v5.ObjectiveSense.MINIMIZE;
import static org.junit.Assert.*;

/**
 * Unit tests for the {@link KolmogorovWeightedPerfectMatching}
 *
 * @author Timofey Chudakov
 */
@RunWith(Parameterized.class)
public class KolmogorovWeightedPerfectMatchingTest
{
    /**
     * Algorithm options
     */
    private BlossomVOptions options;
    /**
     * The objective sense of the algorithm
     */
    private ObjectiveSense objectiveSense;

    public KolmogorovWeightedPerfectMatchingTest(
        BlossomVOptions options, ObjectiveSense objectiveSense)
    {
        this.options = options;
        this.objectiveSense = objectiveSense;
    }

    @Parameterized.Parameters
    public static Object[][] params()
    {
        BlossomVOptions[] options = BlossomVOptions.ALL_OPTIONS;
        Object[][] params = new Object[2 * options.length][2];
        for (int i = 0; i < options.length; i++) {
            params[2 * i][0] = options[i];
            params[2 * i][1] = MAXIMIZE;
            params[2 * i + 1][0] = options[i];
            params[2 * i + 1][1] = MINIMIZE;
        }
        return params;
    }

    /**
     * Checks complementary slackness conditions
     *
     * @param matching a matching
     * @param dualSolution solution to a dual linear program
     * @param objectiveSense objective sense of the algorithm
     * @param <V> graph vertex type
     * @param <E> graph edge type
     */
    static <V, E> void checkMatchingAndDualSolution(
        MatchingAlgorithm.Matching<V, E> matching,
        KolmogorovWeightedPerfectMatching.DualSolution<V, E> dualSolution,
        ObjectiveSense objectiveSense)
    {
        Graph<V, E> graph = dualSolution.getGraph();
        assertEquals(graph.vertexSet().size(), 2 * matching.getEdges().size());
        Set<E> matchedEdges = matching.getEdges();
        Set<V> vertices = new HashSet<>();
        Map<E, Double> slacks = new HashMap<>();
        for (E edge : matchedEdges) {
            V source = graph.getEdgeSource(edge);
            V target = graph.getEdgeTarget(edge);
            if (source != target) {
                assertFalse(vertices.contains(source));
                assertFalse(vertices.contains(target));
                vertices.add(source);
                vertices.add(target);
                slacks.put(edge, graph.getEdgeWeight(edge));
            } else {
                fail();
            }
        }
        for (E edge : graph.edgeSet()) {
            if (!matchedEdges.contains(edge)) {
                V source = graph.getEdgeSource(edge);
                V target = graph.getEdgeTarget(edge);
                if (source != target) {
                    slacks.put(edge, graph.getEdgeWeight(edge));
                }
            }
        }
        assertEquals(graph.vertexSet(), vertices);

        Map<Set<V>, Double> dualMap = dualSolution.getDualVariables();
        for (Map.Entry<Set<V>, Double> entry : dualMap.entrySet()) {
            double dualVariable = entry.getValue();
            if (entry.getKey().size() > 1) {
                if (objectiveSense == MAXIMIZE) {
                    // the dual variable of a pseudonode can't be greater than EPS
                    // for maximization problem
                    assertTrue(dualVariable - EPS <= 0);
                } else {
                    // the dual variable of a pseudonode can't be less than -EPS
                    // for minimization problem
                    assertTrue(dualVariable + EPS >= 0);
                }
            }
            for (V vertex : entry.getKey()) {
                for (E edge : graph.edgesOf(vertex)) {
                    if (!entry.getKey().contains(Graphs.getOppositeVertex(graph, edge, vertex))) { // checking
                                                                                                   // whether
                                                                                                   // the
                                                                                                   // edge
                                                                                                   // is
                                                                                                   // boundary
                        slacks.put(edge, slacks.get(edge) - dualVariable);
                    }
                }
            }
        }
        for (Map.Entry<E, Double> entry : slacks.entrySet()) {
            E edge = entry.getKey();
            double edgeSlack = entry.getValue();
            if (matchedEdges.contains(edge)) {
                // matched edge must have 0 slack
                assertTrue(Math.abs(edgeSlack) < EPS);
            } else if (objectiveSense == MAXIMIZE) {
                // in the optimal solution to the maximization problem edge slacks must be
                // non-positive
                assertTrue(edgeSlack - EPS <= 0);
            } else {
                // in the optimal solution to the minimization problem edge slacks must be
                // non-negative
                assertTrue(edgeSlack + EPS >= 0);
            }
        }
    }

    @Test
    public void testInvalidDualSolution()
    {
        int[][] edges = { { 1, 2, 7 }, { 2, 3, 4 }, { 3, 4, 3 }, { 4, 1, 4 }, };
        Graph<Integer, DefaultEdge> graph = TestUtil.createUndirected(edges);

        KolmogorovWeightedPerfectMatching<Integer, DefaultEdge> matching =
            new KolmogorovWeightedPerfectMatching<>(graph);
        matching.getMatching();
        Map<Integer, BlossomVNode> vertexMap = BlossomVDebugger.getVertexMap(matching.state);

        BlossomVNode node1 = vertexMap.get(1);
        node1.dual += 1;

        assertFalse(matching.testOptimality());
    }

    /**
     * Test on a triangulation of 8 points Points: (2, 10), (9, 11), (10, 4), (11, 15), (12, 5),
     * (12, 6), (13, 12), (14, 11)
     */
    @Test
    public void testGetMatching0()
    {
        int[][] edges = new int[][] { { 0, 1, 8 }, { 0, 2, 10 }, { 1, 2, 8 }, { 0, 3, 11 },
            { 1, 3, 5 }, { 2, 5, 3 }, { 1, 5, 6 }, { 2, 4, 3 }, { 4, 5, 1 }, { 1, 6, 5 },
            { 3, 6, 4 }, { 3, 7, 5 }, { 6, 7, 2 }, { 5, 7, 6 }, { 4, 7, 7 }, { 1, 7, 5 } };
        double maxWeight = 27;
        double minWeight = 18;
        test(edges, objectiveSense == MAXIMIZE ? maxWeight : minWeight, objectiveSense);
    }

    /**
     * Test on an empty graph
     */
    @Test
    public void testGetMatching1()
    {
        Graph<Integer, DefaultWeightedEdge> graph =
            new DefaultUndirectedWeightedGraph<>(DefaultWeightedEdge.class);
        KolmogorovWeightedPerfectMatching<Integer, DefaultWeightedEdge> perfectMatching =
            new KolmogorovWeightedPerfectMatching<>(graph, options);
        MatchingAlgorithm.Matching<Integer, DefaultWeightedEdge> matching =
            perfectMatching.getMatching();

        assertEquals(0, matching.getWeight(), EPS);
        assertTrue(perfectMatching.testOptimality());
        checkMatchingAndDualSolution(matching, perfectMatching.getDualSolution(), objectiveSense);
    }

    /**
     * Smoke test
     */
    @Test
    public void testGetMatching2()
    {
        int[][] edges = new int[][] { { 1, 2, 5 } };
        double maxWeight = 5;
        double minWeight = 5;
        test(edges, objectiveSense == MAXIMIZE ? maxWeight : minWeight, objectiveSense);
    }

    /**
     * Test on a graph with an odd cycle
     */
    @Test
    public void testGetMatching3()
    {
        int[][] edges =
            new int[][] { { 1, 2, 8 }, { 2, 3, 8 }, { 3, 4, 8 }, { 4, 1, 8 }, { 2, 4, 2 } };
        double maxWeight = 16;
        double minWeight = 16;
        test(edges, objectiveSense == MAXIMIZE ? maxWeight : minWeight, objectiveSense);
    }

    /**
     * Test on a $K_{2,2}$
     */
    @Test
    public void testGetMatching4()
    {
        int[][] edges = new int[][] { { 1, 3, 11 }, { 1, 4, 8 }, { 2, 3, 8 }, { 2, 4, 2 } };
        double maxWeight = 16;
        double minWeight = 13;
        test(edges, objectiveSense == MAXIMIZE ? maxWeight : minWeight, objectiveSense);
    }

    /**
     * Test on a $K_{3,3}$
     */
    @Test
    public void testGetMatching5()
    {
        int[][] edges = new int[][] { { 0, 3, 7 }, { 0, 4, 5 }, { 0, 5, 2 }, { 1, 3, 1 },
            { 1, 4, 3 }, { 1, 5, 4 }, { 2, 3, 7 }, { 2, 4, 10 }, { 2, 5, 7 } };
        double maxWeight = 21;
        double minWeight = 12;
        test(edges, objectiveSense == MAXIMIZE ? maxWeight : minWeight, objectiveSense);
    }

    /**
     * Test on a $K_{3,3}$
     */
    @Test
    public void testGetMatching6()
    {
        int[][] edges = new int[][] { { 0, 3, 7 }, { 0, 4, 3 }, { 0, 5, 9 }, { 1, 3, 8 },
            { 1, 4, 2 }, { 1, 5, 9 }, { 2, 3, 6 }, { 2, 4, 1 }, { 2, 5, 10 } };
        double maxWeight = 21;
        double minWeight = 17;
        test(edges, objectiveSense == MAXIMIZE ? maxWeight : minWeight, objectiveSense);
    }

    /**
     * Test on a $K_{4}$
     */
    @Test
    public void testGetMatching7()
    {
        int[][] edges = new int[][] { { 0, 1, 2 }, { 0, 2, 5 }, { 0, 3, 1 }, { 1, 2, 5 },
            { 1, 3, 2 }, { 2, 3, 1 } };
        double maxWeight = 7;
        double minWeight = 3;
        test(edges, objectiveSense == MAXIMIZE ? maxWeight : minWeight, objectiveSense);
    }

    /**
     * Test on a $K_{5}$ with a dummy edge (2,5)
     */
    @Test
    public void testGetMatching8()
    {
        int[][] edges =
            new int[][] { { 0, 1, 1 }, { 0, 2, 4 }, { 0, 3, 7 }, { 0, 4, 10 }, { 1, 2, 5 },
                { 1, 3, 7 }, { 1, 4, 10 }, { 2, 3, 10 }, { 2, 4, 2 }, { 3, 4, 3 }, { 2, 5, 8 } };
        double maxWeight = 25;
        double minWeight = 12;
        test(edges, objectiveSense == MAXIMIZE ? maxWeight : minWeight, objectiveSense);
    }

    /**
     * Test on a $K_{5}$ with a dummy edge (0,5)
     */
    @Test
    public void testGetMatching9()
    {
        int[][] edges =
            new int[][] { { 0, 1, 1 }, { 0, 2, 6 }, { 0, 3, 1 }, { 0, 4, 1 }, { 1, 2, 6 },
                { 1, 3, 6 }, { 1, 4, 5 }, { 2, 3, 7 }, { 2, 4, 8 }, { 3, 4, 8 }, { 0, 5, 8 } };
        double maxWeight = 22;
        double minWeight = 20;
        test(edges, objectiveSense == MAXIMIZE ? maxWeight : minWeight, objectiveSense);
    }

    /**
     * Test on a $K_{5}$ with a dummy edge (0,5)
     */
    @Test
    public void testGetMatching10()
    {
        int[][] edges =
            new int[][] { { 0, 1, 4 }, { 0, 2, 4 }, { 0, 3, 6 }, { 0, 4, 8 }, { 1, 2, 8 },
                { 1, 3, 10 }, { 1, 4, 8 }, { 2, 3, 4 }, { 2, 4, 9 }, { 3, 4, 4 }, { 0, 5, 9 } };
        double maxWeight = 28;
        double minWeight = 21;
        test(edges, objectiveSense == MAXIMIZE ? maxWeight : minWeight, objectiveSense);
    }

    /**
     * Test on a $K_{5}$ with a dummy edge (0,5)
     */
    @Test
    public void testGetMatching11()
    {
        int[][] edges =
            new int[][] { { 0, 1, 5 }, { 0, 2, 1 }, { 0, 3, 8 }, { 0, 4, 1 }, { 1, 2, 2 },
                { 1, 3, 8 }, { 1, 4, 1 }, { 2, 3, 8 }, { 2, 4, 5 }, { 3, 4, 10 }, { 0, 5, 8 } };
        double maxWeight = 21;
        double minWeight = 17;
        test(edges, objectiveSense == MAXIMIZE ? maxWeight : minWeight, objectiveSense);
    }

    /**
     * Test on a $K_{5}$ with a dummy edge (0,5)
     */
    @Test
    public void testGetMatching12()
    {
        int[][] edges =
            new int[][] { { 0, 1, 2 }, { 0, 2, 6 }, { 0, 3, 3 }, { 0, 4, 2 }, { 1, 2, 7 },
                { 1, 3, 7 }, { 1, 4, 7 }, { 2, 3, 4 }, { 2, 4, 4 }, { 3, 4, 2 }, { 0, 5, 2 } };
        double maxWeight = 13;
        double minWeight = 11;
        test(edges, objectiveSense == MAXIMIZE ? maxWeight : minWeight, objectiveSense);
    }

    /**
     * Test on a $K_{5}$ with a dummy edge (4,3)
     */
    @Test
    public void testGetMatching13()
    {
        int[][] edges = new int[][] { { 2, 0, 5 }, { 2, 1, 9 }, { 3, 0, 2 }, { 3, 1, 6 },
            { 3, 2, 7 }, { 4, 0, 3 }, { 4, 1, 5 }, { 4, 2, 5 }, { 4, 3, 7 }, { 5, 4, 6 } };
        double maxWeight = 17;
        double minWeight = 17;
        test(edges, objectiveSense == MAXIMIZE ? maxWeight : minWeight, objectiveSense);
    }

    /**
     * Test on a $K_{5}$ with a dummy edge (4,0)
     */
    @Test
    public void testGetMatching14()
    {
        int[][] edges =
            new int[][] { { 1, 0, 9 }, { 2, 0, 8 }, { 2, 1, 3 }, { 3, 0, 5 }, { 3, 1, 4 },
                { 3, 2, 10 }, { 4, 0, 3 }, { 4, 1, 2 }, { 4, 2, 4 }, { 4, 3, 8 }, { 5, 1, 4 } };
        double maxWeight = 20;
        double minWeight = 13;
        test(edges, objectiveSense == MAXIMIZE ? maxWeight : minWeight, objectiveSense);
    }

    /**
     * Test on a $K_{5}$ with a dummy edge (4,0)
     */
    @Test
    public void testGetMatching15()
    {
        int[][] edges =
            new int[][] { { 1, 0, 8 }, { 2, 0, 7 }, { 2, 1, 10 }, { 3, 0, 8 }, { 3, 1, 5 },
                { 3, 2, 3 }, { 4, 0, 9 }, { 4, 1, 5 }, { 4, 2, 4 }, { 4, 3, 10 }, { 5, 1, 4 } };
        double maxWeight = 21;
        double minWeight = 16;
        test(edges, objectiveSense == MAXIMIZE ? maxWeight : minWeight, objectiveSense);
    }

    /**
     * Test on a $K_{6}$ with edges with zero weight
     */
    @Test
    public void testGetMatching16()
    {
        int[][] edges = new int[][] { { 0, 1, 3 }, { 0, 2, 2 }, { 0, 3, 8 }, { 0, 4, 10 },
            { 0, 5, 8 }, { 1, 2, 1 }, { 1, 3, 4 }, { 1, 4, 8 }, { 1, 5, 0 }, { 2, 3, 8 },
            { 2, 4, 5 }, { 2, 5, 0 }, { 3, 4, 7 }, { 3, 5, 0 }, { 4, 5, 0 } };
        double maxWeight = 24;
        double minWeight = 6;
        test(edges, objectiveSense == MAXIMIZE ? maxWeight : minWeight, objectiveSense);
    }

    /**
     * Test on a triangulation of 8 points
     */
    @Test
    public void testGetMatching17()
    {
        int[][] edges = new int[][] { { 1, 0, 7 }, { 1, 2, 3 }, { 0, 2, 7 }, { 0, 3, 9 },
            { 0, 4, 9 }, { 3, 4, 2 }, { 0, 5, 9 }, { 4, 5, 8 }, { 2, 5, 6 }, { 5, 6, 5 },
            { 2, 6, 6 }, { 1, 6, 9 }, { 3, 7, 8 }, { 4, 7, 6 }, { 5, 7, 5 }, { 6, 7, 9 } };
        double maxWeight = 32;
        double minWeight = 20;
        test(edges, objectiveSense == MAXIMIZE ? maxWeight : minWeight, objectiveSense);
    }

    /**
     * Test on a triangulation of 10 points
     */
    @Test
    public void testGetMatching18()
    {
        int[][] edges = new int[][] { { 0, 1, 10 }, { 0, 2, 7 }, { 1, 2, 4 }, { 2, 3, 1 },
            { 1, 3, 3 }, { 3, 4, 1 }, { 1, 4, 2 }, { 3, 5, 1 }, { 4, 5, 2 }, { 2, 5, 2 },
            { 4, 6, 3 }, { 5, 6, 3 }, { 1, 6, 3 }, { 2, 7, 5 }, { 0, 7, 6 }, { 5, 8, 2 },
            { 6, 8, 3 }, { 2, 8, 4 }, { 7, 8, 3 }, { 7, 9, 3 }, { 0, 9, 7 }, { 8, 9, 8 } };
        double maxWeight = 27;
        double minWeight = 16;
        test(edges, objectiveSense == MAXIMIZE ? maxWeight : minWeight, objectiveSense);
    }

    /**
     * Test on a triangulation of 10 points Points: (1, 2), (1, 8), (3, 5), (3, 11), (3, 12), (5,
     * 3), (10, 6), (10, 11), (14, 8), (15, 1)
     */
    @Test
    public void testGetMatching19()
    {
        int[][] edges = new int[][] { { 0, 1, 6 }, { 0, 2, 4 }, { 1, 2, 4 }, { 2, 3, 6 },
            { 1, 3, 4 }, { 1, 4, 5 }, { 3, 4, 1 }, { 0, 5, 5 }, { 2, 5, 3 }, { 2, 6, 8 },
            { 3, 6, 9 }, { 5, 6, 6 }, { 3, 7, 7 }, { 4, 7, 8 }, { 6, 7, 5 }, { 6, 8, 5 },
            { 7, 8, 5 }, { 6, 9, 8 }, { 5, 9, 11 }, { 8, 9, 9 }, { 0, 9, 15 } };
        double maxWeight = 37;
        double minWeight = 23;
        test(edges, objectiveSense == MAXIMIZE ? maxWeight : minWeight, objectiveSense);
    }

    /**
     * Test on a triangulation of 10 points Points: (1, 7), (1, 11), (4, 5), (6, 5), (7, 8), (9, 1),
     * (11, 7), (13, 7), (13, 10), (15, 9)
     */
    @Test
    public void testGetMatching20()
    {
        int[][] edges = new int[][] { { 2, 3, 2 }, { 2, 4, 5 }, { 3, 4, 4 }, { 0, 2, 4 },
            { 0, 4, 7 }, { 0, 1, 4 }, { 1, 4, 7 }, { 2, 5, 7 }, { 3, 5, 5 }, { 0, 5, 10 },
            { 3, 6, 6 }, { 5, 6, 7 }, { 4, 6, 5 }, { 5, 7, 8 }, { 6, 7, 2 }, { 4, 8, 7 },
            { 1, 8, 13 }, { 6, 8, 4 }, { 7, 8, 3 }, { 7, 9, 3 }, { 8, 9, 3 }, { 5, 9, 10 } };
        double maxWeight = 37;
        double minWeight = 19;
        test(edges, objectiveSense == MAXIMIZE ? maxWeight : minWeight, objectiveSense);
    }

    /**
     * Test on a triangulation of 10 points Points: (4, 5), (5, 14), (6, 12), (7, 11), (9, 1), (9,
     * 8), (10, 15), (12, 6), (13, 12), (14, 6)
     */
    @Test
    public void testGetMatching21()
    {
        int[][] edges = new int[][] { { 0, 1, 10 }, { 0, 2, 8 }, { 1, 2, 3 }, { 0, 3, 7 },
            { 2, 3, 2 }, { 0, 4, 7 }, { 0, 5, 6 }, { 4, 5, 7 }, { 3, 5, 4 }, { 2, 6, 5 },
            { 3, 6, 5 }, { 1, 6, 6 }, { 5, 7, 4 }, { 4, 7, 6 }, { 5, 8, 6 }, { 7, 8, 7 },
            { 3, 8, 7 }, { 6, 8, 5 }, { 8, 9, 7 }, { 7, 9, 2 }, { 4, 9, 8 } };
        double maxWeight = 34;
        double minWeight = 21;
        test(edges, objectiveSense == MAXIMIZE ? maxWeight : minWeight, objectiveSense);
    }

    /**
     * Test on a triangulation of 10 points Points: (3, 3), (3, 10), (4, 12), (5, 13), (8, 4), (10,
     * 3), (11, 5), (11, 9), (11, 13), (14, 11)
     */
    @Test
    public void testGetMatching22()
    {
        int[][] edges = new int[][] { { 1, 0, 7 }, { 1, 4, 8 }, { 0, 4, 6 }, { 0, 5, 7 },
            { 4, 5, 3 }, { 4, 6, 4 }, { 5, 6, 3 }, { 4, 7, 6 }, { 6, 7, 4 }, { 2, 3, 2 },
            { 2, 8, 8 }, { 3, 7, 8 }, { 1, 2, 3 }, { 1, 7, 9 }, { 3, 8, 7 }, { 7, 8, 5 },
            { 7, 9, 4 }, { 8, 9, 5 }, { 6, 9, 7 } };
        double maxWeight = 38;
        double minWeight = 21;
        test(edges, objectiveSense == MAXIMIZE ? maxWeight : minWeight, objectiveSense);
    }

    /**
     * Test on a triangulation of 10 points Points: (1, 15), (4, 2), (7, 13), (8, 15), (9, 5), (10,
     * 7), (11, 11), (12, 4), (12, 9), (13, 11)
     */
    @Test
    public void testGetMatching23()
    {
        int[][] edges = new int[][] { { 1, 0, 14 }, { 1, 2, 12 }, { 0, 2, 7 }, { 0, 3, 7 },
            { 2, 3, 3 }, { 2, 4, 9 }, { 1, 4, 6 }, { 2, 5, 7 }, { 4, 5, 3 }, { 2, 6, 5 },
            { 3, 6, 5 }, { 5, 6, 5 }, { 4, 7, 4 }, { 1, 7, 9 }, { 5, 7, 4 }, { 5, 8, 3 },
            { 7, 8, 5 }, { 6, 8, 3 }, { 3, 9, 7 }, { 6, 9, 2 }, { 8, 9, 3 }, { 7, 9, 8 } };
        double maxWeight = 40;
        double minWeight = 25;
        test(edges, objectiveSense == MAXIMIZE ? maxWeight : minWeight, objectiveSense);
    }

    /**
     * Test on a triangulation of 10 points Points: (1, 6), (2, 14), (5, 5), (7, 10), (9, 8), (9,
     * 10), (12, 4), (13, 8), (13, 12), (14, 10)
     */
    @Test
    public void testGetMatching24()
    {
        int[][] edges = new int[][] { { 0, 2, 5 }, { 0, 3, 8 }, { 2, 3, 6 }, { 0, 1, 9 },
            { 1, 3, 7 }, { 2, 4, 5 }, { 3, 4, 3 }, { 3, 5, 2 }, { 4, 5, 2 }, { 1, 5, 9 },
            { 4, 6, 5 }, { 2, 6, 8 }, { 4, 7, 4 }, { 5, 7, 5 }, { 6, 7, 5 }, { 5, 8, 5 },
            { 1, 8, 12 }, { 7, 8, 4 }, { 7, 9, 3 }, { 8, 9, 3 }, { 6, 9, 7 } };
        double maxWeight = 37;
        double minWeight = 22;
        test(edges, objectiveSense == MAXIMIZE ? maxWeight : minWeight, objectiveSense);
    }

    /**
     * Test on a triangulation of 10 points Points: (1,4), (5, 10), (5, 13), (8, 7), (9, 8), (10,
     * 6), (11, 2), (11, 13), (14, 3), (15, 13)
     */
    @Test
    public void testGetMatching25()
    {
        int[][] edges = new int[][] { { 0, 1, 8 }, { 0, 2, 10 }, { 1, 2, 3 }, { 0, 3, 8 },
            { 1, 3, 5 }, { 1, 4, 5 }, { 3, 4, 2 }, { 4, 5, 3 }, { 3, 5, 3 }, { 3, 6, 6 },
            { 0, 6, 11 }, { 5, 6, 5 }, { 1, 7, 7 }, { 4, 7, 6 }, { 2, 7, 6 }, { 5, 8, 5 },
            { 6, 8, 4 }, { 5, 9, 8 }, { 8, 9, 10 }, { 4, 9, 8 }, { 7, 9, 5 } };
        double maxWeight = 36;
        double minWeight = 23;
        test(edges, objectiveSense == MAXIMIZE ? maxWeight : minWeight, objectiveSense);
    }

    /**
     * Test on a triangulation of 10 points
     */
    @Test
    public void testGetMatching26()
    {
        int[][] edges = new int[][] { { 0, 1, 5 }, { 0, 2, 5 }, { 1, 2, 3 }, { 1, 3, 4 },
            { 2, 3, 3 }, { 2, 4, 5 }, { 3, 4, 6 }, { 0, 4, 5 }, { 3, 5, 5 }, { 4, 5, 3 },
            { 3, 6, 6 }, { 5, 6, 5 }, { 1, 6, 10 }, { 5, 7, 9 }, { 4, 7, 8 }, { 0, 7, 12 },
            { 5, 8, 5 }, { 7, 8, 11 }, { 6, 8, 2 }, { 7, 9, 13 }, { 8, 9, 5 }, { 6, 9, 5 } };
        double maxWeight = 39;
        double minWeight = 26;
        test(edges, objectiveSense == MAXIMIZE ? maxWeight : minWeight, objectiveSense);
    }

    /**
     * Test on a triangulation of 10 points Points: (2, 9), (4, 13), (6, 5), (6, 12), (8, 4), (8,
     * 9), (8, 14), (10, 15), (14, 10), (15, 4)
     */
    @Test
    public void testGetMatching27()
    {
        int[][] edges = new int[][] { { 0, 1, 5 }, { 0, 3, 5 }, { 1, 3, 3 }, { 2, 4, 3 },
            { 2, 5, 5 }, { 4, 5, 5 }, { 0, 2, 6 }, { 0, 5, 6 }, { 3, 5, 4 }, { 5, 6, 5 },
            { 3, 6, 3 }, { 1, 6, 5 }, { 5, 7, 7 }, { 6, 7, 3 }, { 1, 7, 7 }, { 5, 8, 7 },
            { 7, 8, 7 }, { 4, 8, 9 }, { 8, 9, 7 }, { 4, 9, 7 } };
        double maxWeight = 30;
        double minWeight = 22;
        test(edges, objectiveSense == MAXIMIZE ? maxWeight : minWeight, objectiveSense);
    }

    /**
     * Test on a triangulation of 20 points Points: (2, 24), (4, 8), (5, 21), (5, 24), (6, 12), (10,
     * 4), (15, 3), (15, 5), (17, 5), (19, 27), (20, 16), (23, 1), (23, 8), (23, 12), (24, 14), (25,
     * 21), (27, 3), (27, 11), (30, 23), (30, 28)
     */
    @Test
    public void testGetMatching28()
    {
        int[][] edges = new int[][] { { 0, 2, 5 }, { 0, 3, 3 }, { 2, 3, 3 }, { 0, 1, 17 },
            { 0, 4, 13 }, { 1, 4, 5 }, { 2, 4, 10 }, { 4, 5, 9 }, { 1, 5, 8 }, { 5, 7, 6 },
            { 4, 7, 12 }, { 5, 6, 6 }, { 6, 7, 2 }, { 6, 8, 3 }, { 7, 8, 2 }, { 2, 9, 16 },
            { 3, 9, 15 }, { 0, 8, 18 }, { 2, 10, 16 }, { 4, 10, 15 }, { 9, 10, 12 }, { 7, 10, 13 },
            { 8, 10, 12 }, { 5, 11, 14 }, { 6, 11, 9 }, { 8, 11, 8 }, { 8, 12, 7 }, { 11, 12, 7 },
            { 8, 13, 10 }, { 10, 13, 5 }, { 12, 13, 4 }, { 10, 14, 5 }, { 13, 14, 2 },
            { 10, 15, 8 }, { 14, 15, 9 }, { 9, 15, 9 }, { 12, 16, 7 }, { 11, 16, 5 }, { 14, 17, 4 },
            { 15, 17, 11 }, { 12, 17, 5 }, { 16, 17, 8 }, { 13, 17, 5 }, { 17, 18, 13 },
            { 15, 18, 6 }, { 16, 18, 21 }, { 15, 19, 9 }, { 9, 19, 12 }, { 18, 19, 5 } };
        double maxWeight = 117;
        double minWeight = 57;
        test(edges, objectiveSense == MAXIMIZE ? maxWeight : minWeight, objectiveSense);
    }

    /**
     * Test on triangulation of 50 points
     */
    @Test
    public void testGetMatching29()
    {
        int[][] edges = new int[][] { { 0, 2, 44 }, { 0, 3, 50 }, { 2, 3, 6 }, { 2, 4, 6 },
            { 3, 4, 7 }, { 3, 5, 6 }, { 4, 5, 5 }, { 0, 1, 16 }, { 0, 6, 15 }, { 1, 6, 15 },
            { 0, 7, 21 }, { 6, 7, 22 }, { 2, 7, 30 }, { 4, 7, 30 }, { 6, 8, 5 }, { 1, 8, 16 },
            { 4, 9, 16 }, { 5, 9, 13 }, { 3, 9, 18 }, { 6, 10, 7 }, { 7, 10, 21 }, { 8, 10, 6 },
            { 7, 12, 22 }, { 10, 12, 9 }, { 4, 13, 25 }, { 7, 13, 23 }, { 9, 13, 26 },
            { 10, 15, 11 }, { 12, 15, 6 }, { 8, 15, 14 }, { 12, 16, 5 }, { 15, 16, 3 },
            { 11, 14, 3 }, { 11, 17, 10 }, { 14, 17, 9 }, { 9, 11, 8 }, { 9, 17, 15 },
            { 13, 17, 19 }, { 17, 18, 2 }, { 14, 18, 8 }, { 8, 19, 21 }, { 15, 19, 14 },
            { 1, 19, 33 }, { 17, 20, 10 }, { 13, 20, 14 }, { 18, 20, 10 }, { 11, 21, 28 },
            { 14, 21, 26 }, { 9, 21, 34 }, { 3, 21, 50 }, { 7, 22, 30 }, { 12, 22, 29 },
            { 13, 22, 16 }, { 13, 23, 14 }, { 20, 23, 9 }, { 22, 23, 10 }, { 15, 24, 15 },
            { 16, 24, 14 }, { 19, 24, 13 }, { 16, 25, 15 }, { 24, 25, 12 }, { 12, 25, 19 },
            { 22, 25, 25 }, { 20, 26, 9 }, { 23, 26, 10 }, { 18, 26, 16 }, { 23, 27, 8 },
            { 22, 27, 9 }, { 26, 27, 13 }, { 18, 28, 24 }, { 26, 28, 22 }, { 14, 28, 27 },
            { 21, 28, 23 }, { 22, 29, 18 }, { 25, 29, 24 }, { 22, 30, 18 }, { 27, 30, 14 },
            { 29, 30, 3 }, { 24, 31, 20 }, { 19, 31, 29 }, { 25, 31, 24 }, { 26, 32, 18 },
            { 28, 32, 18 }, { 27, 32, 24 }, { 30, 32, 26 }, { 28, 33, 14 }, { 21, 33, 27 },
            { 30, 34, 14 }, { 29, 34, 12 }, { 25, 34, 27 }, { 31, 34, 30 }, { 30, 36, 18 },
            { 34, 36, 14 }, { 32, 36, 26 }, { 32, 35, 9 }, { 35, 36, 23 }, { 32, 37, 20 },
            { 35, 37, 17 }, { 28, 37, 22 }, { 33, 37, 14 }, { 34, 38, 13 }, { 36, 38, 20 },
            { 31, 38, 27 }, { 21, 39, 42 }, { 33, 39, 17 }, { 33, 40, 18 }, { 37, 40, 10 },
            { 39, 40, 7 }, { 37, 41, 10 }, { 35, 41, 17 }, { 40, 41, 14 }, { 38, 42, 11 },
            { 31, 42, 28 }, { 40, 43, 18 }, { 41, 43, 8 }, { 35, 43, 22 }, { 40, 44, 14 },
            { 39, 44, 11 }, { 21, 44, 52 }, { 42, 45, 14 }, { 31, 45, 32 }, { 19, 45, 60 },
            { 42, 46, 19 }, { 45, 46, 29 }, { 38, 46, 21 }, { 36, 46, 24 }, { 36, 47, 35 },
            { 46, 47, 37 }, { 35, 47, 26 }, { 43, 47, 6 }, { 46, 48, 63 }, { 47, 48, 26 },
            { 43, 48, 24 }, { 40, 48, 18 }, { 44, 48, 8 }, { 44, 49, 8 }, { 48, 49, 1 } };
        double maxWeight = 605;
        double minWeight = 279;
        test(edges, objectiveSense == MAXIMIZE ? maxWeight : minWeight, objectiveSense);
    }

    /**
     * Test on triangulation of 50 points
     */
    @Test
    public void testGetMatching30()
    {
        int[][] edges = new int[][] { { 0, 1, 34 }, { 0, 2, 146 }, { 1, 2, 113 }, { 1, 3, 34 },
            { 2, 3, 80 }, { 1, 5, 18 }, { 3, 5, 36 }, { 0, 5, 39 }, { 0, 4, 34 }, { 0, 7, 29 },
            { 4, 7, 23 }, { 5, 7, 47 }, { 2, 8, 43 }, { 3, 8, 65 }, { 2, 6, 24 }, { 6, 8, 40 },
            { 4, 9, 33 }, { 7, 9, 25 }, { 3, 10, 46 }, { 5, 10, 37 }, { 3, 11, 71 }, { 10, 11, 63 },
            { 8, 11, 21 }, { 5, 12, 42 }, { 7, 12, 65 }, { 10, 12, 16 }, { 7, 13, 40 },
            { 9, 13, 20 }, { 8, 15, 38 }, { 6, 15, 59 }, { 11, 15, 25 }, { 11, 14, 20 },
            { 14, 15, 8 }, { 7, 16, 68 }, { 12, 16, 17 }, { 13, 16, 60 }, { 11, 17, 20 },
            { 14, 17, 13 }, { 12, 18, 39 }, { 16, 18, 39 }, { 10, 18, 39 }, { 11, 18, 53 },
            { 11, 19, 45 }, { 17, 19, 32 }, { 18, 19, 15 }, { 14, 20, 20 }, { 17, 20, 24 },
            { 15, 20, 20 }, { 18, 21, 13 }, { 19, 21, 24 }, { 16, 21, 34 }, { 17, 22, 25 },
            { 19, 22, 36 }, { 20, 22, 14 }, { 15, 23, 32 }, { 6, 23, 82 }, { 20, 23, 22 },
            { 9, 24, 65 }, { 13, 24, 46 }, { 4, 24, 97 }, { 13, 25, 47 }, { 16, 25, 63 },
            { 24, 25, 8 }, { 20, 26, 26 }, { 22, 26, 23 }, { 23, 26, 23 }, { 19, 27, 36 },
            { 21, 27, 34 }, { 22, 27, 48 }, { 23, 28, 32 }, { 26, 28, 15 }, { 23, 29, 37 },
            { 28, 29, 21 }, { 6, 29, 117 }, { 27, 30, 20 }, { 27, 31, 32 }, { 30, 31, 20 },
            { 22, 31, 49 }, { 26, 31, 42 }, { 26, 32, 30 }, { 28, 32, 21 }, { 31, 32, 21 },
            { 31, 33, 6 }, { 30, 33, 15 }, { 25, 34, 43 }, { 24, 34, 45 }, { 27, 35, 47 },
            { 30, 35, 41 }, { 21, 35, 64 }, { 25, 36, 64 }, { 34, 36, 37 }, { 16, 36, 85 },
            { 21, 36, 77 }, { 35, 36, 23 }, { 34, 37, 12 }, { 36, 37, 29 }, { 6, 38, 139 },
            { 29, 38, 23 }, { 35, 39, 13 }, { 36, 39, 11 }, { 35, 40, 7 }, { 39, 40, 13 },
            { 35, 41, 11 }, { 30, 41, 40 }, { 40, 41, 7 }, { 31, 42, 21 }, { 32, 42, 30 },
            { 33, 42, 20 }, { 30, 42, 30 }, { 41, 42, 53 }, { 28, 43, 37 }, { 32, 43, 37 },
            { 29, 43, 30 }, { 38, 43, 14 }, { 38, 44, 11 }, { 43, 44, 8 }, { 42, 45, 65 },
            { 41, 45, 16 }, { 40, 45, 14 }, { 39, 45, 16 }, { 36, 45, 23 }, { 36, 46, 27 },
            { 45, 46, 37 }, { 37, 46, 18 }, { 42, 47, 14 }, { 45, 47, 75 }, { 43, 47, 42 },
            { 44, 47, 49 }, { 32, 47, 32 }, { 45, 48, 62 }, { 47, 48, 136 }, { 46, 48, 27 },
            { 37, 48, 32 }, { 34, 48, 36 }, { 47, 49, 140 }, { 48, 49, 5 }, { 24, 49, 79 },
            { 4, 49, 175 }, { 34, 49, 38 } };
        double maxWeight = 1426;
        double minWeight = 496;
        test(edges, objectiveSense == MAXIMIZE ? maxWeight : minWeight, objectiveSense);
    }

    /**
     * Test on triangulation of 100 points
     */
    @Test
    public void testGetMatching31()
    {
        int[][] edges = new int[][] { { 0, 1, 118 }, { 0, 2, 125 }, { 1, 2, 7 }, { 0, 3, 66 },
            { 1, 3, 54 }, { 0, 4, 41 }, { 3, 4, 27 }, { 0, 5, 19 }, { 4, 5, 30 }, { 5, 6, 16 },
            { 0, 6, 19 }, { 1, 7, 13 }, { 3, 7, 44 }, { 5, 8, 29 }, { 4, 8, 6 }, { 4, 9, 11 },
            { 3, 9, 19 }, { 8, 9, 10 }, { 1, 10, 15 }, { 7, 10, 11 }, { 2, 10, 17 }, { 2, 11, 30 },
            { 2, 12, 35 }, { 11, 12, 6 }, { 9, 13, 16 }, { 8, 13, 11 }, { 2, 14, 22 },
            { 10, 14, 10 }, { 8, 15, 16 }, { 5, 15, 27 }, { 13, 15, 7 }, { 7, 16, 22 },
            { 10, 16, 25 }, { 3, 16, 34 }, { 11, 17, 8 }, { 12, 17, 10 }, { 2, 17, 31 },
            { 14, 17, 25 }, { 6, 18, 25 }, { 6, 19, 25 }, { 18, 19, 3 }, { 6, 20, 34 },
            { 0, 20, 52 }, { 18, 20, 11 }, { 19, 20, 13 }, { 14, 21, 24 }, { 17, 21, 13 },
            { 14, 22, 16 }, { 14, 23, 16 }, { 22, 23, 4 }, { 21, 23, 18 }, { 17, 24, 16 },
            { 12, 24, 19 }, { 21, 24, 12 }, { 14, 25, 18 }, { 22, 25, 4 }, { 10, 25, 25 },
            { 16, 25, 30 }, { 23, 25, 8 }, { 19, 26, 12 }, { 20, 26, 19 }, { 23, 27, 13 },
            { 25, 27, 7 }, { 5, 28, 41 }, { 15, 28, 32 }, { 19, 28, 29 }, { 26, 28, 21 },
            { 6, 28, 41 }, { 9, 29, 42 }, { 13, 29, 43 }, { 3, 29, 43 }, { 16, 29, 34 },
            { 15, 29, 46 }, { 25, 30, 10 }, { 16, 30, 31 }, { 27, 30, 4 }, { 16, 31, 32 },
            { 29, 31, 33 }, { 30, 31, 13 }, { 15, 32, 38 }, { 29, 32, 50 }, { 28, 32, 20 },
            { 27, 33, 23 }, { 30, 33, 24 }, { 21, 33, 27 }, { 24, 33, 32 }, { 23, 33, 25 },
            { 28, 34, 23 }, { 32, 34, 13 }, { 26, 34, 37 }, { 24, 35, 32 }, { 33, 35, 20 },
            { 24, 36, 33 }, { 12, 36, 51 }, { 35, 36, 8 }, { 30, 37, 26 }, { 33, 37, 14 },
            { 31, 37, 28 }, { 29, 38, 28 }, { 32, 38, 38 }, { 29, 39, 33 }, { 31, 39, 33 },
            { 38, 39, 22 }, { 33, 40, 20 }, { 35, 40, 18 }, { 37, 40, 17 }, { 38, 41, 21 },
            { 39, 41, 21 }, { 31, 42, 41 }, { 37, 42, 42 }, { 39, 42, 17 }, { 38, 43, 24 },
            { 41, 43, 17 }, { 32, 43, 42 }, { 39, 44, 17 }, { 41, 44, 12 }, { 42, 44, 13 },
            { 35, 45, 28 }, { 40, 45, 18 }, { 36, 45, 29 }, { 26, 46, 56 }, { 34, 46, 33 },
            { 20, 46, 69 }, { 36, 48, 31 }, { 45, 48, 5 }, { 46, 49, 4 }, { 46, 50, 13 },
            { 49, 50, 13 }, { 34, 50, 32 }, { 32, 51, 42 }, { 34, 51, 38 }, { 43, 51, 21 },
            { 42, 52, 11 }, { 44, 52, 13 }, { 42, 47, 9 }, { 47, 52, 9 }, { 34, 53, 35 },
            { 50, 53, 8 }, { 51, 53, 17 }, { 37, 54, 38 }, { 40, 54, 40 }, { 42, 54, 22 },
            { 47, 54, 14 }, { 50, 55, 14 }, { 53, 55, 8 }, { 51, 55, 17 }, { 51, 57, 19 },
            { 55, 57, 30 }, { 43, 57, 20 }, { 44, 58, 20 }, { 52, 58, 19 }, { 41, 58, 21 },
            { 43, 58, 28 }, { 57, 58, 23 }, { 40, 59, 39 }, { 45, 59, 31 }, { 54, 59, 19 },
            { 48, 59, 32 }, { 48, 56, 13 }, { 56, 59, 34 }, { 48, 60, 29 }, { 36, 60, 51 },
            { 56, 60, 18 }, { 12, 60, 100 }, { 52, 61, 20 }, { 58, 61, 21 }, { 47, 61, 23 },
            { 54, 61, 26 }, { 46, 62, 31 }, { 49, 62, 28 }, { 20, 62, 90 }, { 54, 63, 25 },
            { 61, 63, 8 }, { 49, 64, 28 }, { 62, 64, 14 }, { 50, 64, 32 }, { 55, 64, 31 },
            { 58, 65, 20 }, { 61, 65, 30 }, { 57, 65, 20 }, { 56, 66, 27 }, { 59, 66, 22 },
            { 60, 66, 35 }, { 61, 67, 13 }, { 63, 67, 7 }, { 59, 68, 20 }, { 66, 68, 23 },
            { 63, 68, 23 }, { 67, 68, 20 }, { 54, 68, 30 }, { 57, 69, 27 }, { 55, 69, 40 },
            { 65, 69, 17 }, { 66, 70, 9 }, { 68, 70, 22 }, { 64, 71, 17 }, { 62, 71, 20 },
            { 55, 72, 31 }, { 64, 72, 29 }, { 69, 72, 29 }, { 66, 73, 28 }, { 70, 73, 25 },
            { 60, 73, 28 }, { 70, 74, 25 }, { 73, 74, 3 }, { 68, 75, 27 }, { 67, 75, 19 },
            { 65, 76, 25 }, { 69, 76, 24 }, { 67, 76, 37 }, { 75, 76, 31 }, { 61, 76, 38 },
            { 64, 77, 26 }, { 72, 77, 25 }, { 71, 77, 16 }, { 77, 78, 17 }, { 71, 78, 15 },
            { 62, 78, 33 }, { 69, 79, 25 }, { 72, 79, 18 }, { 76, 80, 10 }, { 75, 80, 26 },
            { 68, 81, 31 }, { 70, 81, 42 }, { 75, 81, 10 }, { 73, 82, 15 }, { 74, 82, 14 },
            { 60, 82, 41 }, { 72, 83, 21 }, { 77, 83, 17 }, { 79, 83, 19 }, { 75, 85, 13 },
            { 81, 85, 9 }, { 80, 85, 21 }, { 80, 84, 4 }, { 84, 85, 18 }, { 77, 86, 23 },
            { 78, 86, 13 }, { 81, 87, 14 }, { 85, 87, 7 }, { 84, 87, 18 }, { 70, 88, 33 },
            { 81, 88, 47 }, { 74, 88, 27 }, { 82, 88, 23 }, { 79, 89, 16 }, { 83, 89, 17 },
            { 79, 90, 21 }, { 89, 90, 15 }, { 69, 90, 36 }, { 76, 90, 36 }, { 80, 91, 37 },
            { 84, 91, 37 }, { 76, 91, 37 }, { 90, 91, 4 }, { 82, 93, 30 }, { 88, 93, 9 },
            { 83, 94, 25 }, { 89, 94, 30 }, { 77, 94, 27 }, { 86, 94, 20 }, { 86, 92, 10 },
            { 92, 94, 14 }, { 81, 95, 23 }, { 87, 95, 24 }, { 88, 95, 36 }, { 93, 95, 32 },
            { 92, 96, 10 }, { 86, 96, 15 }, { 78, 96, 28 }, { 62, 96, 61 }, { 20, 96, 150 },
            { 92, 97, 12 }, { 94, 97, 22 }, { 96, 97, 3 }, { 94, 98, 34 }, { 97, 98, 54 },
            { 89, 98, 16 }, { 90, 98, 15 }, { 91, 98, 15 }, { 93, 99, 41 }, { 95, 99, 12 },
            { 97, 99, 128 }, { 98, 99, 74 }, { 91, 99, 63 }, { 87, 99, 22 }, { 84, 99, 37 } };
        double maxWeight = 1781;
        double minWeight = 693;
        test(edges, objectiveSense == MAXIMIZE ? maxWeight : minWeight, objectiveSense);
    }

    /**
     * Test on triangulation of 100 points
     */
    @Test
    public void testGetMatching32()
    {
        int[][] edges = new int[][] { { 0, 1, 48 }, { 0, 3, 23 }, { 1, 3, 27 }, { 1, 6, 14 },
            { 3, 6, 16 }, { 0, 2, 80 }, { 0, 7, 53 }, { 2, 7, 28 }, { 3, 8, 13 }, { 6, 8, 5 },
            { 0, 9, 43 }, { 7, 9, 13 }, { 6, 10, 10 }, { 8, 10, 11 }, { 1, 10, 17 }, { 1, 4, 15 },
            { 4, 10, 21 }, { 0, 11, 25 }, { 9, 11, 23 }, { 2, 12, 26 }, { 7, 12, 40 }, { 2, 5, 20 },
            { 5, 12, 21 }, { 5, 13, 27 }, { 12, 13, 29 }, { 10, 14, 16 }, { 4, 14, 25 },
            { 7, 15, 25 }, { 9, 15, 19 }, { 0, 16, 35 }, { 3, 16, 37 }, { 11, 16, 23 },
            { 7, 17, 26 }, { 15, 17, 4 }, { 11, 18, 22 }, { 16, 18, 5 }, { 12, 19, 9 },
            { 13, 19, 30 }, { 3, 20, 31 }, { 16, 20, 15 }, { 11, 21, 25 }, { 18, 21, 25 },
            { 9, 21, 26 }, { 15, 21, 14 }, { 10, 22, 32 }, { 14, 22, 33 }, { 8, 22, 31 },
            { 3, 22, 33 }, { 20, 22, 9 }, { 12, 23, 16 }, { 19, 23, 8 }, { 15, 24, 13 },
            { 17, 24, 13 }, { 21, 24, 9 }, { 21, 25, 7 }, { 18, 25, 28 }, { 24, 25, 6 },
            { 17, 26, 16 }, { 24, 26, 20 }, { 17, 27, 20 }, { 7, 27, 39 }, { 26, 27, 6 },
            { 12, 27, 33 }, { 23, 27, 21 }, { 22, 29, 17 }, { 14, 29, 28 }, { 22, 30, 17 },
            { 29, 30, 9 }, { 26, 31, 11 }, { 27, 31, 13 }, { 24, 31, 20 }, { 25, 31, 24 },
            { 19, 32, 29 }, { 23, 32, 31 }, { 13, 32, 29 }, { 13, 28, 21 }, { 28, 32, 15 },
            { 29, 33, 22 }, { 14, 33, 29 }, { 30, 33, 27 }, { 16, 34, 29 }, { 18, 34, 30 },
            { 20, 34, 23 }, { 22, 34, 22 }, { 30, 34, 14 }, { 23, 35, 24 }, { 32, 35, 31 },
            { 27, 35, 25 }, { 31, 35, 28 }, { 32, 36, 15 }, { 28, 36, 24 }, { 35, 36, 34 },
            { 25, 37, 31 }, { 31, 37, 35 }, { 18, 37, 42 }, { 34, 37, 38 }, { 14, 38, 52 },
            { 4, 38, 70 }, { 33, 38, 30 }, { 31, 39, 28 }, { 35, 39, 16 }, { 30, 40, 23 },
            { 33, 40, 35 }, { 34, 40, 21 }, { 35, 42, 25 }, { 36, 42, 32 }, { 39, 42, 19 },
            { 39, 41, 7 }, { 41, 42, 16 }, { 42, 43, 26 }, { 36, 43, 17 }, { 28, 43, 40 },
            { 34, 44, 40 }, { 37, 44, 19 }, { 40, 45, 14 }, { 40, 46, 16 }, { 45, 46, 5 },
            { 34, 46, 32 }, { 44, 46, 29 }, { 41, 47, 15 }, { 42, 47, 26 }, { 39, 47, 19 },
            { 31, 47, 38 }, { 37, 47, 43 }, { 44, 48, 22 }, { 46, 48, 13 }, { 42, 49, 18 },
            { 43, 49, 18 }, { 49, 50, 9 }, { 43, 50, 17 }, { 40, 51, 34 }, { 45, 51, 25 },
            { 33, 51, 49 }, { 38, 51, 48 }, { 37, 52, 40 }, { 44, 52, 35 }, { 47, 52, 23 },
            { 44, 54, 25 }, { 48, 54, 16 }, { 51, 53, 6 }, { 51, 55, 21 }, { 53, 55, 22 },
            { 45, 55, 23 }, { 46, 55, 23 }, { 48, 55, 20 }, { 49, 56, 14 }, { 50, 56, 13 },
            { 44, 57, 33 }, { 52, 57, 14 }, { 48, 58, 19 }, { 54, 58, 10 }, { 44, 59, 28 },
            { 54, 59, 17 }, { 57, 59, 14 }, { 48, 60, 20 }, { 55, 60, 9 }, { 58, 60, 8 },
            { 51, 62, 23 }, { 53, 62, 19 }, { 38, 62, 51 }, { 58, 63, 7 }, { 60, 63, 12 },
            { 54, 63, 13 }, { 56, 64, 15 }, { 50, 64, 21 }, { 56, 61, 8 }, { 61, 64, 10 },
            { 43, 64, 37 }, { 47, 65, 35 }, { 52, 65, 36 }, { 42, 65, 39 }, { 49, 65, 34 },
            { 56, 65, 26 }, { 55, 66, 13 }, { 53, 66, 24 }, { 60, 66, 12 }, { 56, 67, 21 },
            { 65, 67, 7 }, { 54, 68, 15 }, { 59, 68, 16 }, { 63, 68, 9 }, { 60, 69, 11 },
            { 63, 69, 10 }, { 66, 69, 12 }, { 56, 70, 19 }, { 61, 70, 16 }, { 67, 70, 7 },
            { 65, 70, 13 }, { 52, 71, 25 }, { 57, 71, 27 }, { 65, 71, 22 }, { 62, 72, 17 },
            { 38, 72, 61 }, { 66, 73, 17 }, { 69, 73, 22 }, { 62, 73, 32 }, { 72, 73, 38 },
            { 53, 73, 31 }, { 69, 74, 18 }, { 73, 74, 29 }, { 63, 74, 20 }, { 68, 74, 18 },
            { 61, 75, 27 }, { 64, 75, 24 }, { 70, 75, 25 }, { 64, 76, 29 }, { 75, 76, 13 },
            { 43, 76, 65 }, { 28, 76, 104 }, { 57, 77, 40 }, { 71, 77, 33 }, { 59, 77, 41 },
            { 68, 77, 41 }, { 68, 78, 32 }, { 74, 78, 19 }, { 77, 78, 24 }, { 74, 79, 19 },
            { 78, 79, 8 }, { 72, 81, 47 }, { 73, 81, 26 }, { 72, 80, 28 }, { 80, 81, 43 },
            { 71, 82, 37 }, { 77, 82, 33 }, { 65, 82, 43 }, { 70, 82, 44 }, { 70, 83, 40 },
            { 75, 83, 27 }, { 82, 83, 24 }, { 78, 84, 14 }, { 77, 84, 31 }, { 79, 84, 11 },
            { 77, 85, 16 }, { 82, 85, 27 }, { 84, 85, 31 }, { 82, 86, 13 }, { 83, 86, 13 },
            { 79, 87, 26 }, { 84, 87, 28 }, { 74, 87, 35 }, { 73, 87, 33 }, { 81, 87, 12 },
            { 72, 88, 39 }, { 38, 88, 96 }, { 80, 88, 12 }, { 86, 89, 19 }, { 83, 89, 9 },
            { 75, 89, 29 }, { 76, 89, 31 }, { 84, 90, 9 }, { 87, 90, 26 }, { 87, 91, 14 },
            { 90, 91, 27 }, { 81, 91, 22 }, { 86, 92, 15 }, { 89, 92, 26 }, { 82, 92, 20 },
            { 82, 93, 23 }, { 85, 93, 30 }, { 92, 93, 9 }, { 81, 94, 28 }, { 91, 94, 25 },
            { 80, 94, 37 }, { 88, 94, 36 }, { 94, 95, 29 }, { 88, 95, 17 }, { 90, 96, 15 },
            { 91, 96, 22 }, { 91, 97, 22 }, { 96, 97, 4 }, { 85, 98, 29 }, { 93, 98, 40 },
            { 96, 98, 25 }, { 97, 98, 25 }, { 90, 98, 25 }, { 84, 98, 29 }, { 94, 99, 8 },
            { 95, 99, 29 }, { 91, 99, 27 }, { 97, 99, 44 }, { 98, 99, 69 } };
        double maxWeight = 1576;
        double minWeight = 728;
        test(edges, objectiveSense == MAXIMIZE ? maxWeight : minWeight, objectiveSense);
    }

    /**
     * Test on triangulation of 200 points
     */
    @Test
    public void testGetMatching33()
    {
        int[][] edges = new int[][] { { 1, 2, 47 }, { 1, 3, 33 }, { 2, 3, 15 }, { 1, 5, 18 },
            { 3, 5, 16 }, { 1, 4, 26 }, { 1, 6, 68 }, { 4, 6, 43 }, { 1, 0, 129 }, { 0, 6, 63 },
            { 4, 7, 11 }, { 6, 7, 32 }, { 0, 9, 16 }, { 0, 10, 11 }, { 9, 10, 9 }, { 3, 11, 9 },
            { 5, 11, 11 }, { 3, 12, 10 }, { 11, 12, 13 }, { 2, 12, 11 }, { 2, 8, 8 }, { 8, 12, 13 },
            { 1, 13, 23 }, { 4, 13, 8 }, { 4, 14, 14 }, { 7, 14, 7 }, { 13, 14, 18 }, { 1, 15, 14 },
            { 5, 15, 12 }, { 0, 16, 25 }, { 6, 16, 42 }, { 10, 16, 28 }, { 13, 17, 3 },
            { 14, 17, 20 }, { 1, 17, 22 }, { 1, 18, 17 }, { 17, 18, 11 }, { 1, 20, 16 },
            { 15, 20, 8 }, { 18, 20, 11 }, { 6, 21, 26 }, { 16, 21, 19 }, { 7, 22, 18 },
            { 14, 22, 13 }, { 6, 22, 22 }, { 6, 19, 13 }, { 19, 22, 12 }, { 6, 23, 20 },
            { 19, 23, 25 }, { 21, 23, 8 }, { 15, 24, 8 }, { 20, 24, 5 }, { 15, 25, 13 },
            { 24, 25, 11 }, { 5, 25, 18 }, { 11, 25, 16 }, { 9, 26, 16 }, { 10, 26, 14 },
            { 16, 27, 13 }, { 21, 27, 13 }, { 20, 28, 14 }, { 18, 28, 9 }, { 24, 28, 17 },
            { 17, 28, 13 }, { 11, 29, 14 }, { 12, 29, 16 }, { 21, 30, 10 }, { 27, 30, 5 },
            { 11, 31, 17 }, { 25, 31, 3 }, { 29, 31, 14 }, { 9, 32, 20 }, { 26, 32, 5 },
            { 12, 33, 19 }, { 29, 33, 16 }, { 8, 33, 21 }, { 26, 34, 8 }, { 32, 34, 7 },
            { 10, 34, 20 }, { 23, 35, 13 }, { 19, 35, 26 }, { 21, 35, 17 }, { 30, 35, 19 },
            { 29, 36, 18 }, { 33, 36, 4 }, { 8, 36, 24 }, { 24, 37, 14 }, { 28, 37, 25 },
            { 25, 37, 10 }, { 31, 37, 9 }, { 34, 38, 5 }, { 34, 39, 7 }, { 38, 39, 3 },
            { 10, 39, 24 }, { 16, 39, 28 }, { 19, 40, 17 }, { 22, 40, 18 }, { 31, 41, 9 },
            { 29, 41, 18 }, { 37, 41, 4 }, { 27, 42, 19 }, { 30, 42, 22 }, { 16, 42, 22 },
            { 39, 42, 13 }, { 22, 43, 18 }, { 14, 43, 24 }, { 40, 43, 12 }, { 39, 44, 4 },
            { 42, 44, 13 }, { 38, 44, 5 }, { 17, 45, 25 }, { 28, 45, 15 }, { 19, 46, 24 },
            { 35, 46, 16 }, { 40, 46, 12 }, { 40, 47, 9 }, { 46, 47, 4 }, { 37, 48, 10 },
            { 41, 48, 7 }, { 29, 48, 20 }, { 14, 49, 28 }, { 43, 49, 7 }, { 8, 50, 33 },
            { 36, 50, 11 }, { 47, 51, 4 }, { 46, 51, 7 }, { 43, 51, 17 }, { 49, 51, 20 },
            { 40, 51, 10 }, { 28, 52, 19 }, { 37, 52, 24 }, { 45, 52, 8 }, { 14, 53, 33 },
            { 49, 53, 18 }, { 17, 53, 30 }, { 45, 53, 13 }, { 38, 54, 18 }, { 44, 54, 18 },
            { 34, 54, 18 }, { 32, 54, 17 }, { 9, 54, 36 }, { 37, 55, 22 }, { 48, 55, 23 },
            { 52, 55, 9 }, { 46, 56, 13 }, { 51, 56, 16 }, { 35, 56, 19 }, { 49, 57, 14 },
            { 53, 57, 10 }, { 36, 58, 19 }, { 50, 58, 15 }, { 29, 58, 27 }, { 48, 58, 25 },
            { 52, 59, 9 }, { 55, 59, 8 }, { 45, 59, 13 }, { 9, 60, 44 }, { 54, 60, 9 },
            { 54, 61, 9 }, { 44, 61, 21 }, { 60, 61, 3 }, { 44, 62, 25 }, { 42, 62, 20 },
            { 42, 63, 23 }, { 30, 63, 29 }, { 62, 63, 6 }, { 45, 64, 15 }, { 53, 64, 15 },
            { 59, 64, 6 }, { 58, 65, 5 }, { 50, 65, 17 }, { 58, 66, 12 }, { 48, 66, 20 },
            { 65, 66, 13 }, { 35, 67, 32 }, { 56, 67, 27 }, { 30, 67, 31 }, { 63, 67, 4 },
            { 51, 68, 15 }, { 56, 68, 15 }, { 49, 68, 26 }, { 48, 69, 23 }, { 55, 69, 12 },
            { 44, 70, 24 }, { 62, 70, 10 }, { 63, 70, 14 }, { 48, 71, 22 }, { 66, 71, 24 },
            { 69, 71, 4 }, { 44, 72, 24 }, { 61, 72, 15 }, { 70, 72, 14 }, { 53, 73, 18 },
            { 57, 73, 15 }, { 64, 73, 11 }, { 50, 74, 24 }, { 65, 74, 9 }, { 66, 74, 14 },
            { 57, 75, 14 }, { 73, 75, 6 }, { 69, 76, 9 }, { 71, 76, 11 }, { 59, 76, 16 },
            { 64, 76, 17 }, { 55, 76, 16 }, { 70, 77, 12 }, { 72, 77, 7 }, { 70, 78, 8 },
            { 77, 78, 6 }, { 56, 79, 19 }, { 67, 79, 20 }, { 68, 79, 24 }, { 49, 80, 27 },
            { 68, 80, 18 }, { 57, 81, 23 }, { 75, 81, 21 }, { 49, 81, 27 }, { 80, 81, 1 },
            { 75, 82, 6 }, { 81, 82, 20 }, { 73, 82, 10 }, { 72, 83, 12 }, { 77, 83, 13 },
            { 61, 83, 17 }, { 60, 83, 18 }, { 68, 84, 14 }, { 80, 84, 8 }, { 83, 85, 3 },
            { 60, 85, 19 }, { 70, 86, 13 }, { 78, 86, 10 }, { 63, 86, 19 }, { 67, 86, 19 },
            { 64, 87, 19 }, { 73, 87, 18 }, { 76, 87, 12 }, { 73, 88, 14 }, { 82, 88, 11 },
            { 87, 88, 11 }, { 80, 90, 11 }, { 84, 90, 8 }, { 71, 91, 19 }, { 76, 91, 20 },
            { 66, 91, 28 }, { 82, 92, 10 }, { 88, 92, 6 }, { 85, 93, 9 }, { 60, 93, 26 },
            { 85, 89, 5 }, { 89, 93, 5 }, { 89, 94, 6 }, { 93, 94, 8 }, { 85, 94, 9 },
            { 83, 94, 11 }, { 77, 94, 17 }, { 78, 94, 21 }, { 86, 94, 26 }, { 80, 95, 12 },
            { 81, 95, 13 }, { 90, 95, 7 }, { 66, 96, 26 }, { 91, 96, 10 }, { 74, 96, 26 },
            { 81, 97, 14 }, { 82, 97, 20 }, { 95, 97, 5 }, { 90, 98, 12 }, { 95, 98, 17 },
            { 84, 98, 15 }, { 68, 98, 25 }, { 79, 98, 25 }, { 76, 99, 19 }, { 87, 99, 18 },
            { 91, 99, 10 }, { 96, 101, 22 }, { 74, 101, 22 }, { 96, 100, 5 }, { 100, 101, 19 },
            { 50, 101, 43 }, { 67, 102, 29 }, { 79, 102, 23 }, { 86, 102, 20 }, { 91, 103, 9 },
            { 99, 103, 5 }, { 96, 103, 14 }, { 100, 103, 16 }, { 86, 104, 17 }, { 102, 104, 9 },
            { 82, 105, 20 }, { 92, 105, 15 }, { 97, 105, 11 }, { 99, 106, 6 }, { 103, 106, 3 },
            { 79, 107, 24 }, { 98, 107, 24 }, { 102, 107, 13 }, { 92, 108, 14 }, { 105, 108, 10 },
            { 87, 109, 22 }, { 99, 109, 29 }, { 88, 109, 17 }, { 92, 109, 14 }, { 108, 109, 5 },
            { 98, 110, 20 }, { 107, 110, 13 }, { 99, 111, 16 }, { 106, 111, 12 }, { 109, 111, 25 },
            { 50, 112, 57 }, { 101, 112, 15 }, { 94, 113, 22 }, { 94, 114, 26 }, { 113, 114, 7 },
            { 86, 114, 31 }, { 104, 114, 23 }, { 101, 115, 18 }, { 100, 115, 23 }, { 112, 115, 6 },
            { 104, 116, 17 }, { 114, 116, 26 }, { 102, 116, 17 }, { 107, 116, 17 },
            { 100, 117, 18 }, { 115, 117, 16 }, { 106, 117, 23 }, { 111, 117, 23 },
            { 103, 117, 23 }, { 107, 118, 14 }, { 110, 118, 10 }, { 116, 118, 14 },
            { 110, 119, 15 }, { 118, 119, 22 }, { 98, 119, 23 }, { 94, 120, 27 }, { 113, 120, 13 },
            { 93, 120, 28 }, { 95, 121, 27 }, { 97, 121, 27 }, { 98, 121, 26 }, { 119, 121, 10 },
            { 105, 122, 22 }, { 108, 122, 25 }, { 97, 122, 26 }, { 121, 122, 11 }, { 111, 123, 14 },
            { 117, 123, 17 }, { 113, 124, 14 }, { 114, 124, 10 }, { 120, 124, 21 },
            { 116, 125, 10 }, { 118, 125, 11 }, { 108, 126, 27 }, { 122, 126, 3 }, { 121, 126, 11 },
            { 121, 127, 7 }, { 126, 127, 12 }, { 119, 127, 12 }, { 114, 128, 20 }, { 116, 128, 17 },
            { 124, 128, 12 }, { 116, 129, 16 }, { 125, 129, 13 }, { 128, 129, 6 }, { 125, 130, 6 },
            { 129, 130, 11 }, { 118, 130, 16 }, { 117, 131, 17 }, { 123, 131, 14 }, { 124, 132, 9 },
            { 128, 132, 13 }, { 120, 132, 24 }, { 111, 133, 23 }, { 109, 133, 35 },
            { 123, 133, 15 }, { 126, 134, 10 }, { 127, 134, 14 }, { 123, 135, 14 }, { 133, 135, 4 },
            { 123, 136, 14 }, { 135, 136, 13 }, { 131, 136, 8 }, { 129, 137, 9 }, { 130, 137, 18 },
            { 128, 137, 9 }, { 132, 137, 12 }, { 136, 138, 5 }, { 131, 138, 8 }, { 115, 139, 25 },
            { 112, 139, 28 }, { 131, 139, 23 }, { 138, 139, 23 }, { 117, 139, 29 }, { 132, 140, 9 },
            { 120, 140, 26 }, { 132, 141, 9 }, { 137, 141, 7 }, { 140, 141, 9 }, { 126, 142, 26 },
            { 134, 142, 21 }, { 109, 142, 37 }, { 133, 142, 26 }, { 108, 142, 37 }, { 136, 143, 8 },
            { 135, 143, 10 }, { 136, 144, 6 }, { 138, 144, 3 }, { 143, 144, 9 }, { 112, 145, 30 },
            { 139, 145, 3 }, { 130, 146, 15 }, { 137, 146, 26 }, { 118, 146, 26 }, { 119, 147, 28 },
            { 127, 147, 27 }, { 118, 147, 28 }, { 146, 147, 9 }, { 134, 148, 14 }, { 142, 148, 29 },
            { 127, 148, 19 }, { 147, 148, 23 }, { 133, 149, 13 }, { 142, 149, 31 },
            { 135, 149, 11 }, { 143, 149, 5 }, { 140, 150, 7 }, { 120, 150, 28 }, { 140, 151, 6 },
            { 150, 151, 2 }, { 147, 152, 21 }, { 148, 152, 4 }, { 143, 153, 12 }, { 144, 153, 7 },
            { 149, 153, 14 }, { 139, 153, 25 }, { 145, 153, 25 }, { 138, 153, 8 }, { 151, 154, 5 },
            { 150, 154, 7 }, { 140, 154, 9 }, { 141, 154, 11 }, { 153, 155, 26 }, { 145, 155, 9 },
            { 147, 156, 17 }, { 152, 156, 8 }, { 145, 157, 11 }, { 112, 157, 40 }, { 155, 157, 4 },
            { 152, 158, 7 }, { 156, 158, 3 }, { 147, 159, 15 }, { 146, 159, 15 }, { 156, 159, 22 },
            { 156, 160, 20 }, { 158, 160, 22 }, { 159, 160, 9 }, { 141, 161, 32 }, { 154, 161, 32 },
            { 137, 161, 31 }, { 146, 161, 25 }, { 159, 161, 15 }, { 154, 162, 19 },
            { 161, 162, 24 }, { 159, 163, 10 }, { 161, 163, 13 }, { 160, 163, 6 }, { 155, 164, 23 },
            { 153, 164, 22 }, { 149, 165, 29 }, { 142, 165, 31 }, { 149, 166, 27 },
            { 153, 166, 33 }, { 165, 166, 4 }, { 155, 167, 22 }, { 164, 167, 7 }, { 157, 167, 22 },
            { 157, 168, 19 }, { 167, 168, 17 }, { 112, 168, 58 }, { 50, 168, 114 },
            { 150, 169, 29 }, { 120, 169, 51 }, { 154, 169, 30 }, { 162, 169, 25 }, { 93, 169, 78 },
            { 60, 169, 103 }, { 152, 170, 26 }, { 158, 170, 24 }, { 148, 170, 29 },
            { 142, 170, 37 }, { 158, 171, 23 }, { 160, 171, 30 }, { 170, 171, 5 }, { 161, 172, 15 },
            { 163, 172, 11 }, { 160, 172, 16 }, { 165, 173, 9 }, { 166, 173, 10 }, { 160, 174, 30 },
            { 171, 174, 7 }, { 171, 175, 8 }, { 170, 175, 7 }, { 174, 175, 7 }, { 170, 176, 16 },
            { 175, 176, 14 }, { 165, 176, 23 }, { 173, 176, 20 }, { 142, 176, 38 },
            { 166, 177, 20 }, { 173, 177, 23 }, { 153, 177, 33 }, { 164, 177, 22 }, { 173, 178, 7 },
            { 177, 178, 24 }, { 176, 178, 19 }, { 164, 179, 23 }, { 167, 179, 24 },
            { 177, 179, 12 }, { 162, 180, 29 }, { 169, 180, 18 }, { 177, 181, 11 }, { 179, 181, 3 },
            { 160, 182, 26 }, { 172, 182, 14 }, { 174, 182, 28 }, { 162, 183, 24 },
            { 180, 183, 23 }, { 161, 183, 33 }, { 172, 183, 33 }, { 174, 184, 19 },
            { 175, 184, 24 }, { 182, 184, 18 }, { 167, 185, 31 }, { 179, 185, 34 },
            { 168, 185, 25 }, { 179, 186, 11 }, { 181, 186, 9 }, { 177, 186, 18 }, { 178, 186, 30 },
            { 168, 187, 27 }, { 50, 187, 140 }, { 185, 187, 2 }, { 185, 188, 4 }, { 187, 188, 3 },
            { 179, 188, 34 }, { 180, 189, 12 }, { 183, 189, 22 }, { 183, 190, 13 },
            { 189, 190, 18 }, { 178, 191, 35 }, { 186, 191, 13 }, { 179, 192, 31 },
            { 188, 192, 15 }, { 182, 193, 29 }, { 184, 193, 40 }, { 183, 193, 25 },
            { 190, 193, 20 }, { 172, 193, 35 }, { 190, 194, 15 }, { 189, 194, 13 },
            { 186, 195, 18 }, { 191, 195, 11 }, { 179, 195, 26 }, { 192, 195, 20 },
            { 190, 196, 13 }, { 193, 196, 19 }, { 194, 196, 13 }, { 193, 197, 20 }, { 196, 197, 1 },
            { 194, 197, 14 }, { 191, 198, 30 }, { 195, 198, 39 }, { 178, 198, 34 },
            { 195, 199, 42 }, { 198, 199, 4 }, { 193, 199, 80 }, { 197, 199, 98 }, { 184, 199, 48 },
            { 175, 199, 46 }, { 176, 199, 40 }, { 178, 199, 35 } };
        double maxWeight = 2461;
        double minWeight = 974;
        test(edges, objectiveSense == MAXIMIZE ? maxWeight : minWeight, objectiveSense);
    }

    /**
     * Test on triangulation of 300 points
     */
    @Test
    public void testGetMatching34()
    {
        int[][] edges = new int[][] { { 0, 1, 14 }, { 0, 6, 13 }, { 1, 6, 3 }, { 3, 4, 16 },
            { 3, 7, 13 }, { 4, 7, 5 }, { 7, 8, 2 }, { 4, 8, 3 }, { 0, 10, 10 }, { 6, 10, 4 },
            { 0, 9, 4 }, { 9, 10, 6 }, { 6, 11, 2 }, { 10, 11, 2 }, { 2, 3, 2 }, { 2, 12, 3 },
            { 3, 12, 2 }, { 7, 13, 1 }, { 8, 13, 3 }, { 8, 14, 4 }, { 4, 14, 3 }, { 4, 15, 5 },
            { 14, 15, 3 }, { 10, 16, 1 }, { 11, 16, 3 }, { 1, 17, 4 }, { 6, 17, 5 }, { 2, 1, 1 },
            { 2, 17, 4 }, { 12, 17, 2 }, { 3, 18, 5 }, { 12, 18, 5 }, { 3, 19, 6 }, { 7, 19, 8 },
            { 18, 19, 1 }, { 7, 20, 3 }, { 13, 20, 2 }, { 19, 20, 6 }, { 8, 21, 3 }, { 14, 21, 2 },
            { 0, 22, 5 }, { 9, 22, 2 }, { 12, 23, 3 }, { 17, 23, 4 }, { 18, 23, 3 }, { 8, 24, 4 },
            { 13, 24, 4 }, { 21, 24, 2 }, { 15, 25, 3 }, { 15, 26, 3 }, { 25, 26, 1 }, { 9, 27, 4 },
            { 22, 27, 2 }, { 9, 28, 5 }, { 27, 28, 3 }, { 10, 28, 4 }, { 16, 28, 3 }, { 6, 29, 5 },
            { 11, 29, 5 }, { 17, 29, 3 }, { 18, 30, 3 }, { 19, 30, 2 }, { 23, 30, 4 },
            { 19, 31, 4 }, { 20, 31, 4 }, { 30, 31, 3 }, { 20, 32, 3 }, { 31, 32, 2 },
            { 20, 33, 3 }, { 32, 33, 3 }, { 13, 33, 4 }, { 24, 33, 3 }, { 14, 34, 4 },
            { 21, 34, 4 }, { 15, 34, 4 }, { 25, 34, 3 }, { 25, 35, 2 }, { 34, 35, 3 },
            { 26, 35, 1 }, { 22, 37, 3 }, { 27, 37, 4 }, { 0, 37, 7 }, { 5, 0, 6 }, { 5, 37, 8 },
            { 5, 36, 5 }, { 36, 37, 6 }, { 16, 38, 4 }, { 28, 38, 2 }, { 27, 38, 5 }, { 16, 39, 3 },
            { 38, 39, 1 }, { 16, 40, 4 }, { 11, 40, 5 }, { 39, 40, 1 }, { 11, 41, 5 },
            { 29, 41, 3 }, { 40, 41, 2 }, { 29, 42, 2 }, { 41, 42, 1 }, { 17, 43, 5 },
            { 23, 43, 2 }, { 23, 44, 3 }, { 30, 44, 3 }, { 43, 44, 1 }, { 30, 45, 2 },
            { 44, 45, 1 }, { 24, 46, 3 }, { 33, 46, 1 }, { 32, 46, 4 }, { 24, 47, 3 },
            { 46, 47, 3 }, { 21, 47, 3 }, { 34, 47, 4 }, { 34, 48, 4 }, { 35, 48, 1 },
            { 17, 49, 5 }, { 29, 49, 5 }, { 43, 49, 2 }, { 44, 49, 3 }, { 30, 50, 5 },
            { 31, 50, 3 }, { 32, 50, 3 }, { 46, 50, 5 }, { 46, 51, 2 }, { 47, 51, 3 },
            { 46, 52, 5 }, { 50, 52, 1 }, { 27, 54, 5 }, { 37, 54, 4 }, { 39, 55, 4 },
            { 40, 55, 4 }, { 38, 55, 3 }, { 27, 55, 6 }, { 41, 56, 4 }, { 42, 56, 3 },
            { 42, 57, 4 }, { 56, 57, 2 }, { 29, 57, 5 }, { 49, 57, 4 }, { 49, 58, 2 },
            { 57, 58, 3 }, { 44, 58, 4 }, { 50, 59, 5 }, { 52, 59, 5 }, { 30, 59, 4 },
            { 45, 59, 4 }, { 44, 59, 4 }, { 47, 60, 4 }, { 51, 60, 5 }, { 34, 60, 5 },
            { 48, 60, 5 }, { 60, 61, 4 }, { 48, 61, 3 }, { 37, 62, 7 }, { 54, 62, 7 },
            { 36, 62, 5 }, { 36, 53, 3 }, { 53, 62, 2 }, { 27, 63, 6 }, { 54, 63, 6 },
            { 55, 63, 2 }, { 55, 64, 4 }, { 63, 64, 4 }, { 40, 64, 5 }, { 41, 64, 5 },
            { 56, 64, 3 }, { 44, 65, 5 }, { 58, 65, 4 }, { 59, 65, 2 }, { 59, 66, 2 },
            { 52, 66, 4 }, { 65, 66, 2 }, { 51, 67, 4 }, { 60, 67, 7 }, { 46, 67, 5 },
            { 52, 67, 4 }, { 53, 68, 3 }, { 62, 68, 1 }, { 62, 69, 6 }, { 54, 69, 3 },
            { 68, 69, 5 }, { 56, 70, 2 }, { 57, 70, 3 }, { 64, 70, 3 }, { 58, 71, 3 },
            { 65, 71, 2 }, { 65, 72, 1 }, { 66, 72, 3 }, { 71, 72, 1 }, { 60, 73, 3 },
            { 61, 73, 3 }, { 54, 74, 3 }, { 69, 74, 2 }, { 64, 75, 2 }, { 63, 75, 5 },
            { 70, 75, 3 }, { 58, 76, 3 }, { 71, 76, 3 }, { 57, 76, 5 }, { 71, 77, 1 },
            { 72, 77, 2 }, { 76, 77, 2 }, { 72, 78, 2 }, { 66, 78, 3 }, { 77, 78, 2 },
            { 52, 79, 5 }, { 66, 79, 6 }, { 67, 79, 3 }, { 69, 80, 5 }, { 68, 80, 3 },
            { 69, 81, 3 }, { 74, 81, 1 }, { 74, 82, 2 }, { 81, 82, 1 }, { 74, 83, 4 },
            { 82, 83, 2 }, { 54, 83, 5 }, { 63, 83, 4 }, { 57, 84, 5 }, { 70, 84, 4 },
            { 76, 84, 3 }, { 76, 85, 1 }, { 77, 85, 3 }, { 84, 85, 2 }, { 66, 86, 4 },
            { 78, 86, 3 }, { 79, 86, 5 }, { 67, 87, 3 }, { 79, 87, 2 }, { 67, 88, 5 },
            { 60, 88, 5 }, { 87, 88, 4 }, { 60, 89, 5 }, { 88, 89, 1 }, { 73, 89, 4 },
            { 63, 91, 5 }, { 75, 91, 3 }, { 78, 92, 4 }, { 86, 92, 6 }, { 77, 92, 3 },
            { 85, 92, 2 }, { 89, 93, 4 }, { 73, 93, 3 }, { 61, 93, 6 }, { 80, 90, 1 },
            { 80, 94, 4 }, { 90, 94, 4 }, { 69, 94, 5 }, { 81, 94, 3 }, { 81, 95, 3 },
            { 82, 95, 3 }, { 94, 95, 1 }, { 82, 96, 3 }, { 83, 96, 2 }, { 95, 96, 4 },
            { 83, 97, 3 }, { 96, 97, 2 }, { 63, 97, 5 }, { 63, 98, 6 }, { 91, 98, 3 },
            { 97, 98, 1 }, { 91, 99, 2 }, { 98, 99, 3 }, { 75, 99, 3 }, { 70, 99, 5 },
            { 84, 99, 6 }, { 86, 101, 3 }, { 79, 101, 4 }, { 86, 100, 3 }, { 100, 101, 1 },
            { 79, 102, 4 }, { 87, 102, 3 }, { 101, 102, 1 }, { 87, 103, 4 }, { 88, 103, 3 },
            { 89, 103, 3 }, { 89, 104, 4 }, { 103, 104, 5 }, { 93, 104, 1 }, { 61, 104, 7 },
            { 68, 105, 6 }, { 53, 105, 7 }, { 80, 105, 4 }, { 90, 105, 3 }, { 95, 106, 5 },
            { 96, 106, 1 }, { 96, 107, 2 }, { 97, 107, 2 }, { 106, 107, 1 }, { 98, 108, 1 },
            { 99, 108, 4 }, { 97, 108, 2 }, { 107, 108, 2 }, { 85, 109, 3 }, { 84, 109, 4 },
            { 92, 109, 3 }, { 87, 110, 3 }, { 102, 110, 3 }, { 87, 111, 4 }, { 103, 111, 3 },
            { 110, 111, 1 }, { 90, 112, 3 }, { 105, 112, 3 }, { 90, 113, 4 }, { 94, 113, 3 },
            { 112, 113, 1 }, { 84, 114, 5 }, { 99, 114, 4 }, { 84, 115, 5 }, { 109, 115, 4 },
            { 114, 115, 1 }, { 95, 116, 3 }, { 106, 116, 5 }, { 94, 116, 4 }, { 113, 116, 4 },
            { 99, 117, 4 }, { 108, 117, 3 }, { 114, 117, 5 }, { 109, 118, 4 }, { 115, 118, 1 },
            { 114, 118, 2 }, { 92, 119, 5 }, { 109, 119, 5 }, { 86, 119, 6 }, { 100, 119, 5 },
            { 100, 120, 4 }, { 119, 120, 1 }, { 102, 121, 4 }, { 101, 121, 3 }, { 110, 121, 4 },
            { 100, 121, 4 }, { 120, 121, 3 }, { 103, 122, 5 }, { 104, 122, 4 }, { 113, 123, 2 },
            { 112, 123, 3 }, { 113, 124, 3 }, { 116, 124, 3 }, { 123, 124, 1 }, { 116, 125, 2 },
            { 124, 125, 3 }, { 106, 125, 5 }, { 106, 126, 4 }, { 125, 126, 2 }, { 107, 127, 3 },
            { 108, 127, 4 }, { 106, 127, 4 }, { 126, 127, 2 }, { 108, 128, 4 }, { 117, 128, 2 },
            { 127, 128, 3 }, { 117, 129, 1 }, { 128, 129, 1 }, { 109, 130, 4 }, { 119, 130, 3 },
            { 120, 131, 4 }, { 121, 131, 1 }, { 110, 131, 5 }, { 125, 132, 2 }, { 126, 132, 2 },
            { 127, 132, 4 }, { 114, 133, 4 }, { 118, 133, 4 }, { 117, 133, 3 }, { 129, 133, 3 },
            { 109, 134, 4 }, { 118, 134, 4 }, { 130, 134, 3 }, { 110, 135, 5 }, { 111, 135, 5 },
            { 131, 135, 7 }, { 103, 135, 5 }, { 122, 135, 4 }, { 135, 136, 2 }, { 122, 136, 3 },
            { 122, 137, 2 }, { 136, 137, 1 }, { 122, 138, 3 }, { 104, 138, 5 }, { 137, 138, 2 },
            { 61, 138, 12 }, { 118, 139, 4 }, { 133, 139, 3 }, { 118, 140, 4 }, { 134, 140, 3 },
            { 139, 140, 2 }, { 130, 141, 3 }, { 134, 141, 2 }, { 130, 142, 2 }, { 141, 142, 1 },
            { 120, 143, 4 }, { 131, 143, 5 }, { 119, 143, 3 }, { 130, 143, 3 }, { 142, 143, 2 },
            { 136, 144, 1 }, { 137, 144, 2 }, { 135, 144, 3 }, { 112, 145, 6 }, { 123, 145, 5 },
            { 105, 145, 6 }, { 123, 146, 4 }, { 124, 146, 3 }, { 145, 146, 4 }, { 125, 147, 4 },
            { 132, 147, 4 }, { 124, 147, 4 }, { 146, 147, 1 }, { 133, 148, 3 }, { 139, 148, 4 },
            { 129, 148, 4 }, { 128, 148, 4 }, { 139, 149, 2 }, { 140, 149, 2 }, { 140, 150, 2 },
            { 149, 150, 2 }, { 134, 150, 3 }, { 141, 150, 3 }, { 131, 151, 4 }, { 143, 151, 4 },
            { 137, 152, 3 }, { 144, 152, 3 }, { 138, 152, 3 }, { 127, 153, 5 }, { 132, 153, 6 },
            { 128, 153, 5 }, { 148, 153, 4 }, { 142, 154, 2 }, { 143, 154, 3 }, { 141, 154, 3 },
            { 150, 154, 4 }, { 131, 155, 6 }, { 135, 155, 4 }, { 151, 155, 6 }, { 144, 156, 2 },
            { 152, 156, 3 }, { 135, 156, 4 }, { 147, 157, 2 }, { 146, 157, 3 }, { 148, 158, 3 },
            { 153, 158, 3 }, { 148, 159, 3 }, { 158, 159, 3 }, { 139, 159, 4 }, { 149, 159, 3 },
            { 149, 160, 2 }, { 159, 160, 2 }, { 150, 161, 3 }, { 154, 161, 5 }, { 149, 161, 3 },
            { 160, 161, 1 }, { 154, 162, 2 }, { 161, 162, 5 }, { 143, 162, 4 }, { 143, 163, 3 },
            { 151, 163, 4 }, { 162, 163, 1 }, { 151, 164, 3 }, { 155, 164, 5 }, { 155, 165, 1 },
            { 164, 165, 4 }, { 155, 166, 3 }, { 165, 166, 2 }, { 135, 166, 4 }, { 156, 166, 3 },
            { 147, 167, 4 }, { 132, 167, 6 }, { 157, 167, 2 }, { 132, 168, 6 }, { 153, 168, 3 },
            { 167, 168, 5 }, { 158, 169, 2 }, { 159, 169, 3 }, { 151, 170, 4 }, { 163, 170, 3 },
            { 164, 170, 3 }, { 162, 170, 4 }, { 156, 171, 3 }, { 152, 171, 3 }, { 146, 172, 5 },
            { 145, 172, 5 }, { 146, 173, 5 }, { 157, 173, 3 }, { 172, 173, 2 }, { 157, 174, 2 },
            { 167, 174, 2 }, { 173, 174, 2 }, { 158, 175, 3 }, { 169, 175, 4 }, { 153, 175, 3 },
            { 168, 175, 3 }, { 159, 176, 3 }, { 160, 176, 4 }, { 169, 176, 2 }, { 166, 177, 2 },
            { 165, 177, 3 }, { 156, 177, 4 }, { 171, 177, 5 }, { 152, 178, 5 }, { 138, 178, 7 },
            { 171, 178, 4 }, { 61, 178, 17 }, { 172, 179, 1 }, { 173, 179, 3 }, { 173, 180, 2 },
            { 174, 180, 2 }, { 179, 180, 3 }, { 174, 181, 2 }, { 180, 181, 2 }, { 167, 181, 2 },
            { 168, 181, 6 }, { 168, 182, 2 }, { 175, 182, 3 }, { 181, 182, 5 }, { 169, 183, 2 },
            { 175, 183, 4 }, { 176, 183, 2 }, { 176, 184, 1 }, { 183, 184, 1 }, { 160, 184, 5 },
            { 165, 185, 4 }, { 164, 185, 4 }, { 177, 185, 5 }, { 175, 186, 3 }, { 182, 186, 4 },
            { 183, 186, 3 }, { 161, 187, 5 }, { 162, 187, 6 }, { 160, 187, 5 }, { 184, 187, 6 },
            { 162, 188, 6 }, { 170, 188, 4 }, { 187, 188, 8 }, { 164, 188, 5 }, { 185, 188, 4 },
            { 177, 189, 2 }, { 185, 189, 5 }, { 177, 190, 3 }, { 171, 190, 4 }, { 189, 190, 2 },
            { 171, 191, 4 }, { 190, 191, 3 }, { 178, 191, 3 }, { 180, 192, 5 }, { 179, 192, 3 },
            { 172, 192, 4 }, { 145, 192, 7 }, { 182, 193, 2 }, { 181, 193, 6 }, { 182, 194, 3 },
            { 186, 194, 2 }, { 193, 194, 2 }, { 187, 195, 1 }, { 188, 195, 9 }, { 185, 196, 3 },
            { 188, 196, 2 }, { 185, 197, 3 }, { 196, 197, 1 }, { 185, 198, 4 }, { 189, 198, 2 },
            { 189, 199, 3 }, { 190, 199, 1 }, { 190, 200, 3 }, { 199, 200, 2 }, { 191, 200, 2 },
            { 193, 201, 3 }, { 194, 201, 1 }, { 186, 201, 3 }, { 186, 202, 5 }, { 201, 202, 5 },
            { 183, 202, 4 }, { 184, 202, 4 }, { 187, 203, 3 }, { 195, 203, 3 }, { 184, 203, 5 },
            { 202, 203, 2 }, { 185, 204, 4 }, { 197, 204, 3 }, { 198, 204, 3 }, { 198, 205, 1 },
            { 204, 205, 2 }, { 198, 206, 3 }, { 205, 206, 2 }, { 189, 206, 3 }, { 199, 206, 2 },
            { 191, 207, 3 }, { 200, 207, 4 }, { 178, 207, 4 }, { 181, 208, 5 }, { 193, 208, 5 },
            { 180, 208, 5 }, { 193, 210, 3 }, { 201, 210, 2 }, { 193, 209, 2 }, { 209, 210, 1 },
            { 202, 211, 1 }, { 203, 211, 3 }, { 197, 212, 3 }, { 196, 212, 3 }, { 204, 212, 2 },
            { 200, 213, 3 }, { 207, 213, 2 }, { 180, 214, 6 }, { 208, 214, 6 }, { 192, 214, 4 },
            { 193, 215, 4 }, { 208, 215, 3 }, { 209, 215, 3 }, { 209, 216, 2 }, { 210, 216, 1 },
            { 202, 217, 5 }, { 201, 217, 3 }, { 211, 217, 5 }, { 210, 217, 3 }, { 216, 217, 2 },
            { 203, 218, 3 }, { 211, 218, 4 }, { 195, 218, 4 }, { 196, 219, 3 }, { 212, 219, 3 },
            { 205, 220, 4 }, { 206, 220, 3 }, { 199, 220, 3 }, { 200, 220, 4 }, { 208, 221, 4 },
            { 214, 221, 3 }, { 209, 222, 2 }, { 215, 222, 3 }, { 216, 222, 2 }, { 195, 223, 6 },
            { 218, 223, 6 }, { 188, 223, 7 }, { 188, 224, 6 }, { 223, 224, 3 }, { 196, 224, 5 },
            { 219, 224, 3 }, { 205, 225, 4 }, { 220, 225, 5 }, { 204, 225, 4 }, { 212, 225, 3 },
            { 225, 226, 4 }, { 220, 226, 1 }, { 220, 227, 3 }, { 226, 227, 2 }, { 200, 227, 4 },
            { 213, 227, 3 }, { 213, 228, 3 }, { 227, 228, 3 }, { 207, 228, 3 }, { 214, 229, 3 },
            { 192, 229, 6 }, { 221, 229, 4 }, { 221, 230, 1 }, { 229, 230, 3 }, { 215, 231, 2 },
            { 222, 231, 3 }, { 208, 231, 4 }, { 216, 232, 4 }, { 217, 232, 3 }, { 222, 232, 5 },
            { 211, 232, 5 }, { 211, 233, 3 }, { 218, 233, 4 }, { 232, 233, 3 }, { 225, 234, 1 },
            { 226, 234, 5 }, { 227, 235, 3 }, { 228, 235, 2 }, { 208, 236, 4 }, { 231, 236, 3 },
            { 221, 236, 4 }, { 230, 236, 4 }, { 231, 237, 2 }, { 236, 237, 3 }, { 222, 237, 3 },
            { 232, 238, 3 }, { 233, 238, 2 }, { 218, 239, 5 }, { 223, 239, 3 }, { 223, 240, 3 },
            { 224, 240, 3 }, { 239, 240, 2 }, { 219, 241, 4 }, { 224, 241, 5 }, { 225, 241, 3 },
            { 234, 241, 3 }, { 212, 241, 4 }, { 227, 242, 3 }, { 226, 242, 5 }, { 235, 242, 1 },
            { 228, 242, 3 }, { 230, 243, 4 }, { 229, 243, 2 }, { 230, 244, 3 }, { 236, 244, 2 },
            { 243, 244, 5 }, { 232, 245, 2 }, { 238, 245, 3 }, { 243, 246, 1 }, { 244, 246, 6 },
            { 244, 247, 1 }, { 246, 247, 5 }, { 236, 247, 3 }, { 237, 247, 5 }, { 222, 248, 5 },
            { 237, 248, 5 }, { 232, 248, 4 }, { 245, 248, 2 }, { 234, 249, 4 }, { 241, 249, 3 },
            { 234, 250, 3 }, { 249, 250, 1 }, { 226, 251, 5 }, { 242, 251, 7 }, { 234, 251, 4 },
            { 250, 251, 2 }, { 245, 252, 2 }, { 248, 252, 2 }, { 245, 253, 3 }, { 238, 253, 3 },
            { 252, 253, 2 }, { 218, 254, 6 }, { 233, 254, 5 }, { 239, 254, 5 }, { 238, 254, 5 },
            { 253, 254, 4 }, { 239, 255, 4 }, { 240, 255, 3 }, { 240, 256, 4 }, { 255, 256, 2 },
            { 224, 256, 5 }, { 241, 256, 5 }, { 228, 257, 5 }, { 242, 257, 4 }, { 246, 259, 3 },
            { 247, 259, 5 }, { 246, 258, 2 }, { 258, 259, 1 }, { 247, 260, 3 }, { 259, 260, 3 },
            { 237, 261, 4 }, { 247, 261, 5 }, { 248, 261, 5 }, { 248, 262, 3 }, { 261, 262, 3 },
            { 248, 263, 2 }, { 252, 263, 2 }, { 262, 263, 1 }, { 239, 264, 5 }, { 254, 264, 4 },
            { 255, 264, 4 }, { 249, 265, 3 }, { 250, 265, 4 }, { 241, 265, 5 }, { 256, 265, 4 },
            { 242, 266, 5 }, { 251, 266, 5 }, { 257, 266, 4 }, { 259, 267, 3 }, { 258, 267, 2 },
            { 246, 267, 4 }, { 243, 267, 5 }, { 229, 267, 7 }, { 192, 267, 11 }, { 252, 268, 3 },
            { 253, 268, 3 }, { 263, 268, 3 }, { 253, 269, 3 }, { 254, 269, 3 }, { 268, 269, 3 },
            { 254, 270, 2 }, { 269, 270, 2 }, { 254, 271, 3 }, { 264, 271, 3 }, { 270, 271, 1 },
            { 247, 272, 5 }, { 260, 272, 4 }, { 261, 272, 3 }, { 261, 273, 2 }, { 262, 273, 4 },
            { 272, 273, 2 }, { 262, 274, 3 }, { 273, 274, 4 }, { 263, 274, 2 }, { 268, 274, 3 },
            { 268, 275, 1 }, { 269, 275, 4 }, { 274, 275, 2 }, { 269, 276, 2 }, { 270, 276, 2 },
            { 275, 276, 4 }, { 255, 277, 4 }, { 264, 277, 3 }, { 255, 278, 3 }, { 256, 278, 4 },
            { 277, 278, 1 }, { 256, 279, 4 }, { 265, 279, 3 }, { 278, 279, 3 }, { 265, 280, 2 },
            { 279, 280, 2 }, { 266, 281, 3 }, { 257, 281, 4 }, { 257, 282, 3 }, { 281, 282, 2 },
            { 274, 283, 1 }, { 275, 283, 3 }, { 270, 284, 3 }, { 271, 284, 2 }, { 276, 284, 3 },
            { 264, 284, 4 }, { 277, 284, 5 }, { 279, 285, 3 }, { 280, 285, 1 }, { 280, 286, 2 },
            { 285, 286, 1 }, { 265, 286, 4 }, { 250, 286, 6 }, { 259, 287, 5 }, { 267, 287, 3 },
            { 260, 288, 5 }, { 272, 288, 5 }, { 259, 288, 5 }, { 287, 288, 4 }, { 272, 289, 3 },
            { 273, 289, 3 }, { 273, 290, 3 }, { 289, 290, 2 }, { 274, 290, 4 }, { 283, 290, 4 },
            { 250, 291, 7 }, { 251, 291, 7 }, { 286, 291, 2 }, { 287, 292, 2 }, { 288, 292, 4 },
            { 289, 293, 3 }, { 290, 293, 5 }, { 288, 293, 4 }, { 292, 293, 6 }, { 272, 293, 4 },
            { 290, 294, 5 }, { 293, 294, 8 }, { 283, 294, 3 }, { 276, 294, 6 }, { 284, 294, 8 },
            { 275, 294, 4 }, { 284, 295, 10 }, { 294, 295, 16 }, { 279, 295, 4 }, { 278, 295, 5 },
            { 285, 295, 3 }, { 277, 295, 6 }, { 285, 296, 2 }, { 295, 296, 1 }, { 286, 296, 3 },
            { 291, 296, 3 }, { 291, 297, 1 }, { 296, 297, 2 }, { 291, 298, 4 }, { 297, 298, 3 },
            { 251, 298, 7 }, { 266, 298, 7 }, { 281, 299, 4 }, { 282, 299, 5 }, { 266, 299, 6 },
            { 298, 299, 3 } };
        double maxWeight = 670;
        double minWeight = 316;
        test(edges, objectiveSense == MAXIMIZE ? maxWeight : minWeight, objectiveSense);
    }

    /**
     * Test on triangulation of 400 points
     */
    @Test
    public void testGetMatching35()
    {
        int[][] edges = new int[][] { { 3, 4, 8 }, { 3, 8, 8 }, { 4, 8, 2 }, { 4, 5, 4 },
            { 4, 9, 3 }, { 5, 9, 3 }, { 5, 6, 2 }, { 5, 10, 2 }, { 6, 10, 2 }, { 9, 10, 3 },
            { 6, 11, 2 }, { 10, 11, 2 }, { 6, 12, 8 }, { 11, 12, 6 }, { 6, 7, 24 }, { 7, 12, 18 },
            { 12, 13, 10 }, { 7, 13, 8 }, { 13, 14, 2 }, { 7, 14, 6 }, { 14, 15, 5 }, { 7, 15, 1 },
            { 0, 1, 2 }, { 0, 17, 2 }, { 1, 17, 3 }, { 0, 16, 3 }, { 16, 17, 1 }, { 2, 3, 1 },
            { 2, 18, 2 }, { 3, 18, 3 }, { 1, 2, 1 }, { 1, 18, 3 }, { 17, 18, 3 }, { 3, 19, 3 },
            { 8, 19, 6 }, { 18, 19, 3 }, { 4, 20, 3 }, { 8, 20, 3 }, { 9, 20, 2 }, { 10, 21, 2 },
            { 11, 21, 2 }, { 11, 22, 2 }, { 12, 22, 6 }, { 21, 22, 2 }, { 7, 23, 3 }, { 15, 23, 2 },
            { 16, 24, 1 }, { 17, 24, 2 }, { 18, 25, 3 }, { 19, 25, 2 }, { 19, 26, 1 },
            { 25, 26, 1 }, { 19, 27, 4 }, { 8, 27, 3 }, { 26, 27, 3 }, { 10, 28, 3 }, { 21, 28, 4 },
            { 9, 28, 3 }, { 20, 28, 3 }, { 21, 29, 1 }, { 22, 29, 3 }, { 28, 29, 3 }, { 12, 31, 3 },
            { 13, 31, 9 }, { 12, 30, 3 }, { 30, 31, 1 }, { 15, 33, 3 }, { 14, 33, 4 },
            { 15, 34, 2 }, { 33, 34, 2 }, { 23, 34, 2 }, { 17, 35, 3 }, { 18, 35, 3 },
            { 24, 35, 3 }, { 25, 36, 2 }, { 26, 36, 3 }, { 18, 36, 3 }, { 35, 36, 3 },
            { 20, 37, 3 }, { 28, 37, 4 }, { 8, 37, 4 }, { 27, 37, 4 }, { 13, 38, 6 }, { 31, 38, 4 },
            { 13, 32, 3 }, { 13, 39, 5 }, { 32, 39, 4 }, { 38, 39, 1 }, { 14, 40, 3 },
            { 33, 40, 4 }, { 13, 40, 4 }, { 32, 40, 4 }, { 35, 41, 3 }, { 36, 41, 2 },
            { 28, 42, 3 }, { 37, 42, 3 }, { 28, 43, 3 }, { 29, 43, 3 }, { 42, 43, 2 },
            { 31, 44, 3 }, { 30, 44, 3 }, { 12, 44, 4 }, { 22, 44, 6 }, { 31, 45, 3 },
            { 38, 45, 3 }, { 44, 45, 3 }, { 38, 46, 1 }, { 39, 46, 2 }, { 45, 46, 2 },
            { 39, 47, 2 }, { 46, 47, 2 }, { 32, 47, 3 }, { 32, 48, 3 }, { 47, 48, 1 },
            { 24, 49, 4 }, { 35, 49, 3 }, { 41, 49, 4 }, { 36, 50, 3 }, { 41, 50, 3 },
            { 26, 50, 4 }, { 26, 51, 3 }, { 50, 51, 1 }, { 27, 52, 4 }, { 37, 52, 5 },
            { 26, 52, 4 }, { 51, 52, 2 }, { 37, 53, 2 }, { 52, 53, 4 }, { 37, 54, 3 },
            { 42, 54, 2 }, { 53, 54, 1 }, { 42, 55, 1 }, { 43, 55, 3 }, { 54, 55, 1 },
            { 44, 56, 3 }, { 45, 56, 2 }, { 46, 57, 1 }, { 47, 57, 3 }, { 45, 57, 3 },
            { 56, 57, 3 }, { 32, 58, 4 }, { 48, 58, 3 }, { 40, 58, 3 }, { 40, 59, 3 },
            { 58, 59, 1 }, { 40, 60, 3 }, { 33, 60, 4 }, { 59, 60, 2 }, { 33, 61, 4 },
            { 34, 61, 3 }, { 23, 61, 5 }, { 41, 62, 3 }, { 49, 62, 2 }, { 50, 63, 1 },
            { 51, 63, 2 }, { 41, 63, 3 }, { 43, 64, 3 }, { 55, 64, 2 }, { 54, 64, 3 },
            { 43, 65, 2 }, { 64, 65, 1 }, { 44, 66, 3 }, { 56, 66, 5 }, { 22, 66, 6 },
            { 29, 66, 7 }, { 47, 67, 2 }, { 48, 67, 3 }, { 57, 67, 3 }, { 58, 68, 2 },
            { 59, 68, 3 }, { 48, 68, 3 }, { 67, 68, 2 }, { 59, 69, 3 }, { 60, 69, 1 },
            { 60, 70, 3 }, { 69, 70, 2 }, { 33, 70, 4 }, { 61, 70, 3 }, { 70, 71, 2 },
            { 61, 71, 1 }, { 49, 72, 2 }, { 62, 72, 2 }, { 24, 72, 6 }, { 62, 73, 1 },
            { 72, 73, 1 }, { 62, 74, 3 }, { 73, 74, 2 }, { 41, 74, 3 }, { 63, 74, 3 },
            { 51, 75, 2 }, { 52, 75, 3 }, { 63, 75, 2 }, { 57, 76, 3 }, { 56, 76, 3 },
            { 57, 77, 2 }, { 67, 77, 3 }, { 76, 77, 2 }, { 59, 78, 3 }, { 68, 78, 4 },
            { 69, 78, 2 }, { 72, 79, 1 }, { 73, 79, 2 }, { 24, 79, 7 }, { 73, 80, 1 },
            { 74, 80, 3 }, { 79, 80, 1 }, { 74, 81, 2 }, { 80, 81, 3 }, { 63, 81, 3 },
            { 75, 81, 3 }, { 52, 82, 3 }, { 53, 82, 5 }, { 75, 82, 3 }, { 64, 83, 3 },
            { 65, 83, 3 }, { 65, 84, 4 }, { 83, 84, 2 }, { 29, 84, 7 }, { 66, 84, 5 },
            { 43, 84, 5 }, { 66, 85, 3 }, { 84, 85, 5 }, { 56, 85, 5 }, { 76, 85, 5 },
            { 68, 86, 3 }, { 78, 86, 3 }, { 67, 86, 4 }, { 80, 87, 1 }, { 81, 87, 4 },
            { 79, 87, 2 }, { 75, 88, 2 }, { 81, 88, 3 }, { 82, 88, 3 }, { 53, 89, 4 },
            { 82, 89, 5 }, { 54, 89, 5 }, { 64, 89, 5 }, { 64, 90, 3 }, { 83, 90, 3 },
            { 89, 90, 3 }, { 76, 91, 3 }, { 77, 91, 3 }, { 67, 92, 5 }, { 86, 92, 1 },
            { 78, 92, 3 }, { 78, 93, 3 }, { 92, 93, 4 }, { 69, 93, 4 }, { 70, 93, 4 },
            { 70, 94, 4 }, { 71, 94, 3 }, { 93, 94, 3 }, { 71, 95, 4 }, { 94, 95, 1 },
            { 61, 95, 5 }, { 23, 95, 8 }, { 81, 96, 3 }, { 87, 96, 3 }, { 81, 97, 2 },
            { 88, 97, 3 }, { 96, 97, 1 }, { 82, 98, 3 }, { 88, 98, 2 }, { 83, 99, 3 },
            { 84, 99, 3 }, { 90, 99, 4 }, { 84, 100, 2 }, { 99, 100, 1 }, { 84, 101, 3 },
            { 85, 101, 5 }, { 100, 101, 1 }, { 85, 102, 3 }, { 101, 102, 3 }, { 85, 103, 3 },
            { 102, 103, 2 }, { 76, 103, 5 }, { 91, 103, 5 }, { 87, 105, 3 }, { 79, 105, 3 },
            { 96, 105, 4 }, { 88, 106, 2 }, { 97, 106, 3 }, { 98, 106, 2 }, { 98, 107, 2 },
            { 106, 107, 2 }, { 82, 107, 3 }, { 89, 107, 5 }, { 92, 104, 1 }, { 92, 108, 3 },
            { 104, 108, 3 }, { 77, 108, 5 }, { 91, 108, 5 }, { 67, 108, 6 }, { 92, 109, 4 },
            { 93, 109, 3 }, { 104, 109, 4 }, { 96, 110, 2 }, { 105, 110, 4 }, { 97, 110, 3 },
            { 97, 111, 2 }, { 106, 111, 3 }, { 110, 111, 1 }, { 106, 112, 1 }, { 107, 112, 3 },
            { 111, 112, 2 }, { 103, 113, 2 }, { 102, 113, 3 }, { 103, 114, 3 }, { 113, 114, 1 },
            { 103, 115, 4 }, { 91, 115, 4 }, { 114, 115, 2 }, { 91, 116, 5 }, { 108, 116, 2 },
            { 115, 116, 4 }, { 108, 117, 3 }, { 116, 117, 3 }, { 104, 117, 2 }, { 109, 117, 4 },
            { 93, 118, 4 }, { 94, 118, 4 }, { 109, 118, 3 }, { 94, 119, 4 }, { 95, 119, 3 },
            { 118, 119, 3 }, { 79, 120, 6 }, { 24, 120, 11 }, { 105, 120, 3 }, { 110, 121, 1 },
            { 111, 121, 2 }, { 105, 121, 4 }, { 120, 121, 4 }, { 111, 122, 1 }, { 112, 122, 3 },
            { 121, 122, 1 }, { 112, 123, 1 }, { 122, 123, 2 }, { 107, 123, 3 }, { 89, 124, 5 },
            { 90, 124, 5 }, { 99, 125, 3 }, { 100, 125, 4 }, { 90, 125, 5 }, { 124, 125, 5 },
            { 100, 126, 3 }, { 101, 126, 4 }, { 125, 126, 1 }, { 116, 127, 1 }, { 117, 127, 4 },
            { 115, 127, 5 }, { 109, 128, 3 }, { 117, 128, 2 }, { 109, 129, 3 }, { 118, 129, 2 },
            { 128, 129, 3 }, { 107, 130, 5 }, { 123, 130, 6 }, { 89, 130, 6 }, { 124, 130, 3 },
            { 102, 131, 5 }, { 113, 131, 4 }, { 101, 131, 5 }, { 126, 131, 4 }, { 113, 132, 2 },
            { 114, 132, 3 }, { 131, 132, 3 }, { 114, 133, 3 }, { 115, 133, 2 }, { 132, 133, 3 },
            { 117, 134, 3 }, { 127, 134, 2 }, { 117, 135, 3 }, { 128, 135, 3 }, { 134, 135, 1 },
            { 118, 136, 2 }, { 129, 136, 2 }, { 118, 137, 3 }, { 136, 137, 1 }, { 119, 137, 3 },
            { 119, 138, 3 }, { 137, 138, 1 }, { 121, 139, 5 }, { 120, 139, 2 }, { 122, 140, 3 },
            { 123, 140, 3 }, { 121, 140, 3 }, { 124, 141, 3 }, { 130, 141, 1 }, { 124, 142, 4 },
            { 125, 142, 3 }, { 115, 143, 4 }, { 127, 143, 3 }, { 133, 143, 3 }, { 127, 144, 3 },
            { 134, 144, 3 }, { 143, 144, 1 }, { 128, 145, 3 }, { 135, 145, 4 }, { 129, 145, 3 },
            { 136, 145, 4 }, { 137, 146, 2 }, { 138, 146, 3 }, { 136, 146, 1 }, { 145, 146, 3 },
            { 123, 147, 4 }, { 140, 147, 3 }, { 130, 147, 5 }, { 141, 147, 5 }, { 141, 148, 1 },
            { 147, 148, 4 }, { 141, 149, 3 }, { 148, 149, 2 }, { 124, 149, 3 }, { 142, 149, 4 },
            { 125, 150, 3 }, { 126, 150, 4 }, { 142, 150, 3 }, { 126, 151, 4 }, { 131, 151, 3 },
            { 150, 151, 3 }, { 131, 152, 4 }, { 132, 152, 2 }, { 151, 152, 4 }, { 133, 153, 3 },
            { 143, 153, 4 }, { 132, 153, 3 }, { 152, 153, 2 }, { 145, 154, 2 }, { 146, 154, 3 },
            { 139, 155, 2 }, { 139, 156, 3 }, { 155, 156, 2 }, { 121, 156, 5 }, { 140, 156, 5 },
            { 140, 157, 2 }, { 147, 157, 3 }, { 156, 157, 4 }, { 148, 158, 3 }, { 149, 158, 1 },
            { 149, 159, 3 }, { 142, 159, 3 }, { 158, 159, 2 }, { 142, 160, 3 }, { 150, 160, 2 },
            { 159, 160, 2 }, { 150, 161, 1 }, { 151, 161, 4 }, { 160, 161, 1 }, { 145, 162, 3 },
            { 154, 162, 3 }, { 135, 162, 4 }, { 146, 163, 4 }, { 138, 163, 4 }, { 119, 163, 5 },
            { 156, 164, 1 }, { 157, 164, 5 }, { 155, 164, 3 }, { 147, 165, 2 }, { 157, 165, 3 },
            { 147, 166, 3 }, { 148, 166, 4 }, { 165, 166, 1 }, { 148, 167, 3 }, { 158, 167, 2 },
            { 166, 167, 4 }, { 158, 168, 1 }, { 159, 168, 3 }, { 167, 168, 1 }, { 159, 169, 2 },
            { 160, 169, 2 }, { 160, 170, 1 }, { 161, 170, 2 }, { 169, 170, 1 }, { 152, 171, 2 },
            { 153, 171, 3 }, { 151, 171, 5 }, { 143, 172, 4 }, { 153, 172, 3 }, { 144, 172, 4 },
            { 134, 173, 4 }, { 144, 173, 4 }, { 135, 173, 5 }, { 162, 173, 4 }, { 146, 174, 4 },
            { 163, 174, 2 }, { 167, 175, 2 }, { 168, 175, 1 }, { 168, 176, 2 }, { 175, 176, 1 },
            { 159, 176, 3 }, { 169, 176, 3 }, { 161, 177, 3 }, { 151, 177, 4 }, { 170, 177, 3 },
            { 151, 178, 4 }, { 171, 178, 4 }, { 177, 178, 3 }, { 153, 179, 4 }, { 171, 179, 2 },
            { 172, 179, 4 }, { 144, 180, 5 }, { 172, 180, 4 }, { 173, 180, 2 }, { 146, 181, 5 },
            { 154, 181, 5 }, { 174, 181, 2 }, { 164, 182, 3 }, { 155, 182, 4 }, { 164, 183, 2 },
            { 182, 183, 1 }, { 164, 184, 4 }, { 157, 184, 4 }, { 183, 184, 3 }, { 157, 185, 4 },
            { 165, 185, 3 }, { 184, 185, 2 }, { 165, 186, 3 }, { 166, 186, 2 }, { 185, 186, 2 },
            { 166, 187, 3 }, { 167, 187, 3 }, { 186, 187, 2 }, { 167, 188, 3 }, { 175, 188, 3 },
            { 187, 188, 1 }, { 170, 189, 3 }, { 177, 189, 4 }, { 169, 189, 2 }, { 176, 189, 3 },
            { 177, 190, 4 }, { 178, 190, 1 }, { 172, 191, 3 }, { 179, 191, 2 }, { 172, 192, 3 },
            { 180, 192, 3 }, { 191, 192, 3 }, { 180, 193, 2 }, { 192, 193, 3 }, { 173, 193, 2 },
            { 162, 193, 5 }, { 162, 194, 4 }, { 193, 194, 4 }, { 154, 194, 5 }, { 154, 195, 5 },
            { 194, 195, 3 }, { 181, 195, 2 }, { 181, 196, 1 }, { 195, 196, 1 }, { 174, 196, 3 },
            { 182, 197, 1 }, { 183, 197, 2 }, { 155, 197, 5 }, { 183, 198, 2 }, { 184, 198, 3 },
            { 197, 198, 2 }, { 184, 199, 1 }, { 185, 199, 3 }, { 198, 199, 2 }, { 185, 200, 3 },
            { 186, 200, 1 }, { 186, 201, 2 }, { 187, 201, 2 }, { 200, 201, 1 }, { 187, 202, 1 },
            { 188, 202, 2 }, { 201, 202, 1 }, { 188, 203, 2 }, { 202, 203, 2 }, { 175, 203, 3 },
            { 176, 203, 3 }, { 177, 204, 4 }, { 189, 204, 1 }, { 176, 204, 3 }, { 177, 205, 3 },
            { 190, 205, 2 }, { 179, 206, 3 }, { 191, 206, 4 }, { 171, 206, 4 }, { 178, 206, 3 },
            { 190, 206, 3 }, { 191, 207, 3 }, { 192, 207, 2 }, { 194, 209, 2 }, { 195, 209, 3 },
            { 194, 208, 1 }, { 208, 209, 1 }, { 155, 210, 6 }, { 197, 210, 1 }, { 197, 211, 2 },
            { 198, 211, 2 }, { 210, 211, 1 }, { 185, 212, 3 }, { 199, 212, 1 }, { 198, 212, 3 },
            { 202, 213, 1 }, { 203, 213, 3 }, { 201, 213, 2 }, { 200, 213, 3 }, { 176, 214, 4 },
            { 203, 214, 4 }, { 204, 214, 2 }, { 190, 215, 3 }, { 205, 215, 3 }, { 206, 215, 2 },
            { 206, 216, 2 }, { 215, 216, 2 }, { 191, 216, 3 }, { 191, 217, 2 }, { 207, 217, 3 },
            { 216, 217, 2 }, { 194, 218, 3 }, { 193, 218, 3 }, { 208, 218, 3 }, { 208, 219, 2 },
            { 218, 219, 3 }, { 209, 219, 1 }, { 195, 219, 3 }, { 174, 220, 5 }, { 163, 220, 5 },
            { 196, 220, 3 }, { 198, 221, 3 }, { 211, 221, 3 }, { 212, 221, 2 }, { 212, 222, 2 },
            { 221, 222, 2 }, { 185, 222, 4 }, { 200, 222, 4 }, { 203, 223, 3 }, { 213, 223, 1 },
            { 200, 223, 3 }, { 222, 223, 5 }, { 204, 224, 3 }, { 214, 224, 4 }, { 177, 224, 5 },
            { 205, 224, 4 }, { 205, 225, 2 }, { 224, 225, 3 }, { 215, 226, 2 }, { 216, 226, 4 },
            { 205, 226, 3 }, { 225, 226, 1 }, { 207, 227, 3 }, { 217, 227, 4 }, { 192, 227, 3 },
            { 193, 227, 5 }, { 218, 228, 4 }, { 219, 228, 1 }, { 219, 229, 4 }, { 228, 229, 3 },
            { 196, 229, 3 }, { 220, 229, 3 }, { 195, 229, 4 }, { 155, 230, 8 }, { 210, 230, 2 },
            { 211, 230, 3 }, { 211, 231, 2 }, { 230, 231, 1 }, { 211, 232, 3 }, { 221, 232, 2 },
            { 231, 232, 1 }, { 221, 233, 2 }, { 222, 233, 2 }, { 232, 233, 2 }, { 203, 234, 4 },
            { 214, 234, 3 }, { 223, 234, 4 }, { 214, 235, 3 }, { 224, 235, 2 }, { 234, 235, 4 },
            { 193, 236, 5 }, { 218, 236, 4 }, { 227, 236, 3 }, { 218, 237, 3 }, { 236, 237, 2 },
            { 218, 238, 2 }, { 228, 238, 4 }, { 237, 238, 1 }, { 228, 239, 2 }, { 229, 239, 3 },
            { 232, 240, 1 }, { 233, 240, 3 }, { 231, 240, 2 }, { 223, 242, 3 }, { 234, 242, 2 },
            { 223, 241, 2 }, { 241, 242, 2 }, { 225, 243, 3 }, { 226, 243, 3 }, { 224, 243, 3 },
            { 235, 243, 4 }, { 227, 244, 2 }, { 236, 244, 3 }, { 217, 244, 5 }, { 236, 245, 1 },
            { 237, 245, 3 }, { 244, 245, 2 }, { 237, 246, 2 }, { 238, 246, 1 }, { 228, 246, 4 },
            { 239, 246, 5 }, { 231, 247, 2 }, { 240, 247, 2 }, { 230, 247, 3 }, { 222, 248, 4 },
            { 233, 248, 4 }, { 223, 248, 5 }, { 241, 248, 4 }, { 234, 249, 3 }, { 235, 249, 4 },
            { 242, 249, 3 }, { 235, 250, 3 }, { 249, 250, 2 }, { 235, 251, 3 }, { 243, 251, 2 },
            { 250, 251, 3 }, { 226, 252, 4 }, { 243, 252, 4 }, { 216, 252, 5 }, { 217, 253, 5 },
            { 244, 253, 6 }, { 216, 253, 4 }, { 252, 253, 2 }, { 244, 254, 1 }, { 245, 254, 3 },
            { 245, 255, 2 }, { 254, 255, 3 }, { 237, 255, 3 }, { 246, 255, 3 }, { 246, 256, 1 },
            { 255, 256, 2 }, { 240, 257, 4 }, { 247, 257, 5 }, { 233, 257, 4 }, { 248, 257, 3 },
            { 241, 258, 2 }, { 248, 258, 4 }, { 242, 258, 3 }, { 249, 259, 1 }, { 250, 259, 3 },
            { 242, 259, 3 }, { 250, 260, 1 }, { 251, 260, 4 }, { 259, 260, 2 }, { 243, 261, 2 },
            { 251, 261, 2 }, { 243, 262, 3 }, { 252, 262, 2 }, { 261, 262, 2 }, { 244, 263, 5 },
            { 253, 263, 2 }, { 244, 264, 3 }, { 254, 264, 2 }, { 263, 264, 3 }, { 254, 265, 4 },
            { 255, 265, 1 }, { 255, 266, 2 }, { 265, 266, 1 }, { 256, 266, 2 }, { 256, 267, 2 },
            { 266, 267, 2 }, { 246, 267, 3 }, { 239, 267, 5 }, { 247, 268, 2 }, { 257, 268, 5 },
            { 242, 269, 4 }, { 258, 269, 2 }, { 259, 269, 4 }, { 259, 270, 1 }, { 260, 270, 3 },
            { 269, 270, 3 }, { 261, 271, 3 }, { 262, 271, 1 }, { 253, 272, 3 }, { 263, 272, 3 },
            { 252, 272, 3 }, { 262, 272, 3 }, { 271, 272, 2 }, { 258, 273, 5 }, { 269, 273, 6 },
            { 248, 273, 4 }, { 257, 273, 3 }, { 260, 274, 3 }, { 270, 274, 1 }, { 269, 274, 4 },
            { 260, 275, 3 }, { 274, 275, 3 }, { 251, 275, 4 }, { 261, 276, 3 }, { 271, 276, 4 },
            { 251, 276, 3 }, { 275, 276, 2 }, { 271, 277, 1 }, { 276, 277, 3 }, { 271, 278, 2 },
            { 272, 278, 2 }, { 277, 278, 1 }, { 272, 279, 3 }, { 278, 279, 3 }, { 263, 279, 2 },
            { 264, 279, 4 }, { 265, 280, 2 }, { 266, 280, 3 }, { 239, 281, 6 }, { 267, 281, 3 },
            { 247, 282, 5 }, { 230, 282, 7 }, { 268, 282, 3 }, { 155, 282, 13 }, { 268, 283, 3 },
            { 282, 283, 1 }, { 275, 284, 2 }, { 276, 284, 2 }, { 274, 284, 5 }, { 265, 285, 4 },
            { 280, 285, 3 }, { 254, 285, 5 }, { 264, 285, 4 }, { 280, 286, 1 }, { 285, 286, 2 },
            { 280, 287, 2 }, { 286, 287, 1 }, { 266, 287, 3 }, { 267, 288, 4 }, { 281, 288, 4 },
            { 266, 288, 4 }, { 287, 288, 1 }, { 288, 289, 4 }, { 281, 289, 2 }, { 281, 290, 3 },
            { 239, 290, 7 }, { 289, 290, 1 }, { 229, 291, 8 }, { 220, 291, 9 }, { 239, 291, 7 },
            { 290, 291, 2 }, { 282, 292, 3 }, { 283, 292, 2 }, { 268, 292, 3 }, { 268, 293, 4 },
            { 292, 293, 2 }, { 257, 293, 5 }, { 257, 294, 5 }, { 273, 294, 3 }, { 293, 294, 1 },
            { 278, 295, 2 }, { 279, 295, 4 }, { 277, 295, 3 }, { 264, 296, 5 }, { 279, 296, 3 },
            { 285, 296, 4 }, { 293, 297, 1 }, { 294, 297, 2 }, { 292, 297, 3 }, { 294, 298, 1 },
            { 297, 298, 1 }, { 294, 299, 3 }, { 273, 299, 3 }, { 298, 299, 2 }, { 273, 300, 4 },
            { 269, 300, 5 }, { 299, 300, 2 }, { 276, 301, 5 }, { 284, 301, 5 }, { 277, 301, 3 },
            { 295, 301, 2 }, { 295, 302, 2 }, { 301, 302, 2 }, { 279, 302, 4 }, { 279, 303, 4 },
            { 296, 303, 2 }, { 302, 303, 3 }, { 296, 304, 3 }, { 285, 304, 3 }, { 285, 305, 2 },
            { 286, 305, 3 }, { 304, 305, 1 }, { 292, 306, 3 }, { 282, 306, 3 }, { 297, 306, 5 },
            { 269, 307, 6 }, { 300, 307, 3 }, { 269, 308, 5 }, { 274, 308, 5 }, { 307, 308, 1 },
            { 284, 309, 5 }, { 301, 309, 2 }, { 301, 310, 1 }, { 302, 310, 3 }, { 309, 310, 1 },
            { 302, 311, 3 }, { 303, 311, 2 }, { 303, 312, 2 }, { 311, 312, 2 }, { 296, 312, 2 },
            { 304, 312, 3 }, { 288, 313, 4 }, { 289, 313, 5 }, { 287, 313, 4 }, { 289, 314, 4 },
            { 313, 314, 2 }, { 289, 315, 3 }, { 290, 315, 4 }, { 314, 315, 1 }, { 290, 316, 4 },
            { 291, 316, 3 }, { 315, 316, 3 }, { 220, 316, 12 }, { 297, 317, 3 }, { 298, 317, 3 },
            { 299, 317, 3 }, { 300, 317, 4 }, { 274, 318, 6 }, { 284, 318, 7 }, { 308, 318, 3 },
            { 284, 319, 5 }, { 309, 319, 3 }, { 309, 320, 2 }, { 310, 320, 3 }, { 319, 320, 1 },
            { 304, 321, 3 }, { 305, 321, 3 }, { 312, 321, 2 }, { 287, 322, 5 }, { 313, 322, 4 },
            { 286, 322, 4 }, { 305, 322, 3 }, { 315, 323, 2 }, { 316, 323, 3 }, { 314, 323, 3 },
            { 297, 324, 5 }, { 306, 324, 2 }, { 317, 324, 7 }, { 300, 325, 4 }, { 307, 325, 3 },
            { 317, 325, 5 }, { 308, 326, 3 }, { 318, 326, 4 }, { 307, 326, 2 }, { 325, 326, 1 },
            { 284, 327, 7 }, { 318, 327, 2 }, { 319, 327, 6 }, { 310, 328, 3 }, { 320, 328, 4 },
            { 302, 328, 4 }, { 311, 328, 4 }, { 312, 329, 2 }, { 311, 329, 3 }, { 321, 329, 2 },
            { 321, 330, 2 }, { 329, 330, 2 }, { 305, 330, 4 }, { 322, 330, 4 }, { 323, 331, 1 },
            { 316, 331, 3 }, { 314, 331, 3 }, { 325, 332, 2 }, { 326, 332, 1 }, { 318, 333, 3 },
            { 327, 333, 3 }, { 326, 333, 3 }, { 332, 333, 2 }, { 319, 334, 3 }, { 327, 334, 5 },
            { 320, 334, 3 }, { 320, 335, 4 }, { 328, 335, 1 }, { 334, 335, 5 }, { 322, 336, 3 },
            { 330, 336, 2 }, { 322, 337, 2 }, { 336, 337, 2 }, { 313, 337, 5 }, { 316, 338, 4 },
            { 331, 338, 1 }, { 317, 339, 3 }, { 324, 339, 7 }, { 325, 339, 5 }, { 332, 340, 1 },
            { 333, 340, 3 }, { 325, 340, 3 }, { 339, 340, 5 }, { 327, 341, 3 }, { 333, 341, 4 },
            { 327, 342, 3 }, { 334, 342, 3 }, { 341, 342, 1 }, { 328, 343, 3 }, { 335, 343, 3 },
            { 311, 343, 5 }, { 329, 343, 4 }, { 329, 344, 2 }, { 343, 344, 3 }, { 330, 344, 3 },
            { 336, 344, 4 }, { 313, 345, 5 }, { 337, 345, 5 }, { 314, 345, 5 }, { 331, 345, 4 },
            { 331, 346, 3 }, { 345, 346, 2 }, { 338, 346, 2 }, { 341, 347, 1 }, { 342, 347, 2 },
            { 333, 347, 4 }, { 334, 348, 2 }, { 335, 348, 6 }, { 342, 348, 3 }, { 347, 348, 3 },
            { 343, 349, 1 }, { 344, 349, 4 }, { 335, 349, 3 }, { 344, 350, 1 }, { 349, 350, 3 },
            { 337, 351, 4 }, { 345, 351, 2 }, { 345, 352, 1 }, { 351, 352, 1 }, { 346, 352, 3 },
            { 352, 353, 2 }, { 346, 353, 1 }, { 338, 353, 3 }, { 339, 354, 4 }, { 324, 354, 5 },
            { 339, 355, 2 }, { 354, 355, 3 }, { 339, 356, 3 }, { 340, 356, 5 }, { 355, 356, 1 },
            { 347, 357, 1 }, { 348, 357, 4 }, { 333, 357, 5 }, { 349, 358, 4 }, { 350, 358, 1 },
            { 350, 359, 3 }, { 358, 359, 2 }, { 344, 359, 3 }, { 336, 359, 4 }, { 352, 360, 1 },
            { 353, 360, 3 }, { 351, 360, 2 }, { 338, 361, 4 }, { 316, 361, 7 }, { 353, 361, 3 },
            { 354, 362, 3 }, { 324, 362, 6 }, { 355, 363, 2 }, { 356, 363, 3 }, { 354, 363, 3 },
            { 356, 364, 3 }, { 363, 364, 4 }, { 340, 364, 4 }, { 340, 365, 4 }, { 364, 365, 1 },
            { 348, 368, 4 }, { 335, 368, 5 }, { 348, 367, 3 }, { 367, 368, 2 }, { 335, 369, 5 },
            { 349, 369, 3 }, { 368, 369, 3 }, { 358, 370, 1 }, { 359, 370, 3 }, { 349, 370, 4 },
            { 336, 371, 5 }, { 337, 371, 5 }, { 359, 371, 3 }, { 351, 372, 3 }, { 360, 372, 4 },
            { 337, 372, 5 }, { 371, 372, 2 }, { 316, 373, 7 }, { 361, 373, 2 }, { 354, 374, 3 },
            { 362, 374, 1 }, { 354, 375, 3 }, { 363, 375, 2 }, { 374, 375, 3 }, { 357, 366, 1 },
            { 357, 376, 3 }, { 366, 376, 3 }, { 340, 376, 5 }, { 365, 376, 5 }, { 333, 376, 6 },
            { 366, 377, 2 }, { 376, 377, 3 }, { 348, 377, 4 }, { 367, 377, 4 }, { 357, 377, 3 },
            { 368, 378, 4 }, { 369, 378, 1 }, { 369, 379, 2 }, { 378, 379, 1 }, { 349, 379, 3 },
            { 370, 379, 4 }, { 370, 380, 2 }, { 379, 380, 4 }, { 359, 380, 3 }, { 359, 381, 2 },
            { 371, 381, 3 }, { 380, 381, 1 }, { 360, 382, 2 }, { 372, 382, 4 }, { 360, 383, 3 },
            { 353, 383, 4 }, { 382, 383, 1 }, { 353, 384, 3 }, { 361, 384, 3 }, { 383, 384, 1 },
            { 361, 385, 2 }, { 384, 385, 2 }, { 373, 385, 2 }, { 363, 386, 6 }, { 364, 386, 3 },
            { 375, 386, 7 }, { 365, 386, 2 }, { 376, 387, 1 }, { 377, 387, 4 }, { 377, 388, 4 },
            { 387, 388, 6 }, { 367, 388, 2 }, { 367, 389, 3 }, { 368, 389, 3 }, { 388, 389, 1 },
            { 368, 390, 3 }, { 378, 390, 3 }, { 389, 390, 2 }, { 383, 391, 1 }, { 384, 391, 2 },
            { 382, 391, 2 }, { 362, 392, 4 }, { 324, 392, 8 }, { 374, 392, 3 }, { 375, 393, 4 },
            { 386, 393, 10 }, { 374, 393, 2 }, { 392, 393, 1 }, { 387, 394, 3 }, { 388, 394, 9 },
            { 386, 394, 3 }, { 393, 394, 11 }, { 376, 394, 3 }, { 365, 394, 4 }, { 389, 395, 3 },
            { 390, 395, 1 }, { 388, 395, 4 }, { 394, 395, 11 }, { 379, 396, 3 }, { 380, 396, 6 },
            { 378, 396, 2 }, { 390, 396, 3 }, { 395, 396, 2 }, { 380, 397, 4 }, { 381, 397, 3 },
            { 396, 397, 8 }, { 371, 397, 3 }, { 372, 397, 4 }, { 382, 397, 6 }, { 391, 397, 7 },
            { 397, 398, 10 }, { 391, 398, 5 }, { 384, 398, 4 }, { 385, 398, 3 }, { 373, 398, 3 },
            { 373, 399, 4 }, { 398, 399, 1 }, { 316, 399, 11 }, { 220, 399, 21 } };
        double maxWeight = 827;
        double minWeight = 367;
        test(edges, objectiveSense == MAXIMIZE ? maxWeight : minWeight, objectiveSense);
    }

    /**
     * Test on triangulation of 500 points
     */
    @Test
    public void testGetMatching36()
    {
        int[][] edges = new int[][] { { 4, 5, 4 }, { 4, 14, 3 }, { 5, 14, 3 }, { 5, 6, 4 },
            { 5, 15, 2 }, { 6, 15, 4 }, { 14, 15, 3 }, { 10, 11, 1 }, { 10, 16, 2 }, { 11, 16, 3 },
            { 9, 10, 1 }, { 9, 16, 1 }, { 8, 9, 5 }, { 8, 16, 6 }, { 11, 12, 5 }, { 11, 17, 3 },
            { 12, 17, 4 }, { 1, 0, 7 }, { 1, 18, 3 }, { 0, 18, 7 }, { 2, 1, 1 }, { 2, 18, 3 },
            { 3, 4, 7 }, { 3, 19, 5 }, { 4, 19, 4 }, { 6, 20, 3 }, { 15, 20, 3 }, { 7, 8, 2 },
            { 7, 21, 3 }, { 8, 21, 3 }, { 11, 22, 3 }, { 16, 22, 4 }, { 17, 22, 2 }, { 17, 23, 1 },
            { 22, 23, 1 }, { 12, 13, 4 }, { 12, 24, 3 }, { 13, 24, 4 }, { 24, 25, 2 },
            { 13, 25, 3 }, { 13, 26, 2 }, { 25, 26, 1 }, { 2, 3, 1 }, { 2, 27, 4 }, { 3, 27, 4 },
            { 18, 27, 2 }, { 3, 28, 4 }, { 19, 28, 3 }, { 19, 29, 1 }, { 28, 29, 2 }, { 19, 30, 2 },
            { 29, 30, 1 }, { 19, 31, 3 }, { 4, 31, 4 }, { 30, 31, 1 }, { 4, 32, 3 }, { 14, 32, 3 },
            { 31, 32, 1 }, { 14, 33, 3 }, { 32, 33, 1 }, { 14, 34, 3 }, { 15, 34, 3 },
            { 33, 34, 2 }, { 15, 35, 3 }, { 34, 35, 1 }, { 15, 36, 2 }, { 20, 36, 3 },
            { 35, 36, 1 }, { 7, 37, 4 }, { 21, 37, 3 }, { 6, 7, 2 }, { 6, 37, 4 }, { 20, 37, 3 },
            { 8, 38, 4 }, { 16, 38, 4 }, { 21, 38, 4 }, { 16, 39, 2 }, { 22, 39, 4 }, { 38, 39, 3 },
            { 12, 40, 4 }, { 24, 40, 4 }, { 17, 40, 3 }, { 23, 40, 2 }, { 18, 41, 3 },
            { 27, 41, 3 }, { 0, 41, 7 }, { 3, 42, 5 }, { 27, 42, 2 }, { 3, 43, 5 }, { 28, 43, 2 },
            { 42, 43, 2 }, { 28, 44, 2 }, { 29, 44, 2 }, { 43, 44, 2 }, { 35, 45, 1 },
            { 36, 45, 2 }, { 34, 45, 2 }, { 33, 45, 4 }, { 21, 46, 3 }, { 37, 46, 2 },
            { 22, 47, 2 }, { 39, 47, 4 }, { 23, 47, 3 }, { 40, 47, 3 }, { 25, 48, 2 },
            { 26, 48, 3 }, { 24, 48, 3 }, { 41, 49, 4 }, { 0, 49, 6 }, { 41, 50, 2 }, { 49, 50, 2 },
            { 41, 51, 1 }, { 50, 51, 1 }, { 41, 52, 2 }, { 51, 52, 1 }, { 27, 52, 3 },
            { 42, 52, 3 }, { 43, 53, 2 }, { 44, 53, 2 }, { 44, 54, 1 }, { 53, 54, 1 },
            { 31, 55, 3 }, { 32, 55, 3 }, { 30, 55, 2 }, { 44, 55, 3 }, { 54, 55, 2 },
            { 29, 55, 3 }, { 36, 56, 2 }, { 45, 56, 2 }, { 20, 56, 4 }, { 37, 56, 5 },
            { 37, 57, 3 }, { 46, 57, 1 }, { 56, 57, 5 }, { 46, 58, 2 }, { 57, 58, 1 },
            { 21, 58, 3 }, { 38, 58, 4 }, { 39, 59, 3 }, { 47, 59, 3 }, { 47, 60, 2 },
            { 59, 60, 1 }, { 24, 61, 3 }, { 40, 61, 4 }, { 48, 61, 3 }, { 51, 62, 2 },
            { 52, 62, 1 }, { 42, 62, 3 }, { 54, 63, 1 }, { 55, 63, 3 }, { 53, 63, 2 },
            { 55, 64, 2 }, { 63, 64, 3 }, { 32, 64, 4 }, { 33, 65, 4 }, { 45, 65, 5 },
            { 32, 65, 3 }, { 64, 65, 1 }, { 39, 66, 3 }, { 38, 66, 5 }, { 59, 66, 2 },
            { 40, 67, 4 }, { 47, 67, 4 }, { 61, 67, 3 }, { 67, 68, 1 }, { 61, 68, 2 },
            { 61, 69, 1 }, { 68, 69, 1 }, { 48, 69, 3 }, { 50, 70, 2 }, { 49, 70, 3 },
            { 51, 70, 3 }, { 62, 70, 3 }, { 62, 71, 1 }, { 70, 71, 2 }, { 43, 72, 4 },
            { 42, 72, 3 }, { 53, 72, 4 }, { 62, 72, 3 }, { 71, 72, 2 }, { 53, 73, 2 },
            { 63, 73, 2 }, { 72, 73, 3 }, { 45, 74, 4 }, { 56, 74, 2 }, { 56, 75, 5 },
            { 57, 75, 3 }, { 57, 76, 3 }, { 58, 76, 2 }, { 75, 76, 2 }, { 38, 77, 5 },
            { 66, 77, 3 }, { 58, 77, 5 }, { 66, 78, 1 }, { 77, 78, 2 }, { 66, 79, 2 },
            { 78, 79, 1 }, { 59, 79, 2 }, { 60, 79, 3 }, { 60, 80, 3 }, { 79, 80, 2 },
            { 47, 80, 3 }, { 67, 80, 4 }, { 70, 81, 2 }, { 49, 81, 4 }, { 70, 82, 2 },
            { 71, 82, 2 }, { 81, 82, 2 }, { 71, 83, 2 }, { 72, 83, 2 }, { 82, 83, 2 },
            { 72, 84, 1 }, { 73, 84, 4 }, { 83, 84, 1 }, { 63, 85, 2 }, { 64, 85, 4 },
            { 73, 85, 2 }, { 64, 86, 2 }, { 65, 86, 3 }, { 85, 86, 3 }, { 65, 87, 2 },
            { 86, 87, 1 }, { 45, 88, 4 }, { 65, 88, 5 }, { 74, 88, 2 }, { 74, 89, 1 },
            { 88, 89, 1 }, { 74, 90, 3 }, { 89, 90, 2 }, { 56, 90, 4 }, { 75, 90, 3 },
            { 75, 91, 3 }, { 76, 91, 1 }, { 76, 92, 3 }, { 91, 92, 2 }, { 58, 92, 4 },
            { 77, 92, 3 }, { 77, 93, 2 }, { 92, 93, 1 }, { 77, 94, 2 }, { 78, 94, 2 },
            { 93, 94, 2 }, { 78, 95, 1 }, { 79, 95, 2 }, { 94, 95, 1 }, { 67, 96, 2 },
            { 68, 96, 3 }, { 80, 96, 4 }, { 68, 97, 2 }, { 69, 97, 3 }, { 96, 97, 1 },
            { 69, 98, 3 }, { 48, 98, 4 }, { 97, 98, 3 }, { 26, 98, 7 }, { 83, 99, 2 },
            { 84, 99, 1 }, { 73, 100, 3 }, { 85, 100, 1 }, { 85, 101, 2 }, { 86, 101, 3 },
            { 100, 101, 1 }, { 86, 102, 2 }, { 101, 102, 1 }, { 86, 103, 2 }, { 87, 103, 1 },
            { 102, 103, 2 }, { 87, 104, 3 }, { 103, 104, 2 }, { 65, 104, 4 }, { 88, 104, 3 },
            { 88, 105, 2 }, { 104, 105, 1 }, { 89, 106, 1 }, { 90, 106, 3 }, { 88, 106, 2 },
            { 105, 106, 2 }, { 75, 107, 2 }, { 90, 107, 3 }, { 75, 108, 3 }, { 91, 108, 2 },
            { 107, 108, 1 }, { 91, 109, 2 }, { 92, 109, 2 }, { 108, 109, 2 }, { 79, 110, 2 },
            { 80, 110, 3 }, { 95, 110, 2 }, { 97, 111, 2 }, { 98, 111, 3 }, { 111, 112, 2 },
            { 98, 112, 1 }, { 49, 113, 6 }, { 81, 113, 3 }, { 0, 113, 11 }, { 81, 114, 3 },
            { 82, 114, 3 }, { 113, 114, 3 }, { 73, 115, 4 }, { 100, 115, 4 }, { 84, 115, 3 },
            { 99, 115, 2 }, { 102, 116, 2 }, { 103, 116, 2 }, { 103, 117, 2 }, { 104, 117, 2 },
            { 116, 117, 2 }, { 104, 118, 1 }, { 105, 118, 2 }, { 117, 118, 1 }, { 105, 119, 1 },
            { 106, 119, 3 }, { 118, 119, 1 }, { 93, 120, 2 }, { 94, 120, 3 }, { 92, 120, 3 },
            { 109, 120, 3 }, { 94, 121, 2 }, { 120, 121, 2 }, { 95, 121, 3 }, { 95, 122, 2 },
            { 110, 122, 2 }, { 121, 122, 1 }, { 110, 123, 1 }, { 122, 123, 1 }, { 97, 124, 3 },
            { 96, 124, 2 }, { 111, 124, 3 }, { 80, 124, 5 }, { 113, 126, 3 }, { 114, 126, 2 },
            { 113, 125, 1 }, { 125, 126, 2 }, { 99, 127, 3 }, { 115, 127, 3 }, { 83, 127, 3 },
            { 82, 127, 4 }, { 114, 127, 4 }, { 101, 128, 2 }, { 100, 128, 3 }, { 102, 128, 3 },
            { 102, 129, 2 }, { 116, 129, 2 }, { 128, 129, 1 }, { 116, 130, 1 }, { 117, 130, 3 },
            { 129, 130, 1 }, { 106, 131, 3 }, { 119, 131, 4 }, { 90, 131, 4 }, { 107, 131, 4 },
            { 122, 132, 3 }, { 123, 132, 2 }, { 110, 132, 3 }, { 80, 132, 5 }, { 80, 133, 5 },
            { 132, 133, 3 }, { 124, 133, 2 }, { 111, 134, 2 }, { 124, 134, 3 }, { 111, 135, 3 },
            { 112, 135, 3 }, { 134, 135, 1 }, { 113, 136, 3 }, { 0, 136, 12 }, { 125, 136, 2 },
            { 125, 137, 3 }, { 126, 137, 1 }, { 136, 137, 3 }, { 114, 138, 2 }, { 127, 138, 4 },
            { 126, 138, 2 }, { 137, 138, 1 }, { 127, 139, 1 }, { 138, 139, 3 }, { 115, 139, 3 },
            { 115, 140, 3 }, { 139, 140, 3 }, { 100, 140, 4 }, { 128, 140, 4 }, { 117, 141, 3 },
            { 130, 141, 1 }, { 129, 141, 2 }, { 128, 141, 3 }, { 109, 142, 4 }, { 108, 142, 3 },
            { 120, 142, 5 }, { 107, 142, 4 }, { 131, 142, 5 }, { 132, 143, 2 }, { 133, 143, 3 },
            { 124, 144, 2 }, { 133, 144, 2 }, { 134, 144, 3 }, { 134, 145, 2 }, { 135, 145, 1 },
            { 135, 146, 2 }, { 112, 146, 3 }, { 145, 146, 1 }, { 112, 147, 4 }, { 146, 147, 1 },
            { 98, 147, 5 }, { 26, 147, 10 }, { 136, 148, 1 }, { 137, 148, 4 }, { 138, 149, 1 },
            { 139, 149, 4 }, { 137, 149, 2 }, { 139, 150, 3 }, { 140, 150, 2 }, { 117, 151, 4 },
            { 141, 151, 2 }, { 118, 152, 4 }, { 119, 152, 4 }, { 117, 152, 3 }, { 151, 152, 1 },
            { 133, 154, 2 }, { 143, 154, 3 }, { 144, 154, 2 }, { 144, 155, 1 }, { 154, 155, 1 },
            { 144, 156, 2 }, { 155, 156, 1 }, { 134, 156, 3 }, { 145, 156, 3 }, { 146, 157, 1 },
            { 147, 157, 2 }, { 145, 157, 2 }, { 147, 158, 1 }, { 157, 158, 1 }, { 139, 159, 3 },
            { 149, 159, 2 }, { 139, 160, 3 }, { 150, 160, 2 }, { 159, 160, 3 }, { 150, 161, 2 },
            { 160, 161, 2 }, { 140, 161, 2 }, { 141, 162, 4 }, { 151, 162, 5 }, { 128, 162, 4 },
            { 140, 162, 3 }, { 161, 162, 2 }, { 119, 163, 4 }, { 131, 163, 5 }, { 152, 163, 3 },
            { 131, 164, 4 }, { 163, 164, 4 }, { 142, 164, 4 }, { 142, 153, 1 }, { 153, 164, 4 },
            { 153, 165, 1 }, { 164, 165, 3 }, { 153, 166, 2 }, { 165, 166, 1 }, { 142, 166, 3 },
            { 120, 166, 5 }, { 120, 167, 5 }, { 121, 167, 5 }, { 166, 167, 4 }, { 122, 168, 4 },
            { 132, 168, 4 }, { 121, 168, 5 }, { 167, 168, 2 }, { 143, 169, 3 }, { 154, 169, 4 },
            { 132, 169, 3 }, { 168, 169, 2 }, { 155, 170, 1 }, { 156, 170, 2 }, { 154, 170, 2 },
            { 156, 171, 2 }, { 170, 171, 2 }, { 145, 171, 3 }, { 157, 171, 3 }, { 149, 173, 2 },
            { 159, 173, 2 }, { 159, 174, 3 }, { 160, 174, 2 }, { 173, 174, 3 }, { 161, 175, 1 },
            { 162, 175, 3 }, { 160, 175, 3 }, { 162, 176, 1 }, { 175, 176, 2 }, { 152, 177, 3 },
            { 151, 177, 2 }, { 163, 177, 4 }, { 162, 177, 5 }, { 176, 177, 4 }, { 163, 178, 1 },
            { 177, 178, 3 }, { 163, 179, 2 }, { 164, 179, 4 }, { 178, 179, 1 }, { 164, 180, 2 },
            { 165, 180, 3 }, { 167, 181, 1 }, { 168, 181, 3 }, { 166, 181, 5 }, { 154, 182, 3 },
            { 169, 182, 3 }, { 170, 182, 3 }, { 170, 183, 1 }, { 182, 183, 2 }, { 171, 183, 3 },
            { 171, 184, 1 }, { 183, 184, 2 }, { 157, 185, 2 }, { 158, 185, 3 }, { 171, 185, 3 },
            { 184, 185, 2 }, { 172, 186, 2 }, { 172, 187, 3 }, { 186, 187, 1 }, { 149, 187, 4 },
            { 173, 187, 3 }, { 148, 172, 2 }, { 148, 187, 4 }, { 137, 187, 5 }, { 160, 188, 2 },
            { 174, 188, 2 }, { 175, 188, 3 }, { 177, 189, 2 }, { 178, 189, 3 }, { 178, 190, 2 },
            { 179, 190, 1 }, { 189, 190, 3 }, { 165, 191, 3 }, { 180, 191, 1 }, { 164, 191, 3 },
            { 166, 192, 2 }, { 181, 192, 5 }, { 165, 192, 3 }, { 191, 192, 3 }, { 182, 193, 2 },
            { 183, 193, 2 }, { 186, 194, 2 }, { 187, 194, 1 }, { 187, 195, 2 }, { 173, 195, 3 },
            { 194, 195, 1 }, { 174, 196, 3 }, { 188, 196, 3 }, { 173, 196, 3 }, { 195, 196, 3 },
            { 177, 197, 3 }, { 176, 197, 3 }, { 177, 198, 3 }, { 197, 198, 1 }, { 177, 199, 2 },
            { 189, 199, 2 }, { 198, 199, 1 }, { 164, 200, 4 }, { 191, 200, 4 }, { 179, 200, 3 },
            { 190, 200, 2 }, { 181, 201, 3 }, { 192, 201, 4 }, { 169, 202, 4 }, { 182, 202, 4 },
            { 168, 202, 4 }, { 181, 202, 4 }, { 201, 202, 4 }, { 182, 203, 3 }, { 193, 203, 1 },
            { 183, 203, 3 }, { 184, 203, 4 }, { 158, 204, 4 }, { 185, 204, 3 }, { 194, 205, 1 },
            { 195, 205, 2 }, { 186, 205, 3 }, { 175, 206, 3 }, { 188, 206, 3 }, { 176, 206, 4 },
            { 198, 207, 1 }, { 199, 207, 2 }, { 197, 207, 2 }, { 191, 208, 3 }, { 200, 208, 2 },
            { 191, 209, 3 }, { 192, 209, 3 }, { 192, 210, 2 }, { 201, 210, 4 }, { 209, 210, 1 },
            { 201, 211, 5 }, { 202, 211, 1 }, { 202, 212, 4 }, { 211, 212, 3 }, { 182, 212, 3 },
            { 203, 212, 2 }, { 184, 213, 4 }, { 203, 213, 5 }, { 185, 213, 4 }, { 204, 213, 3 },
            { 213, 214, 2 }, { 204, 214, 1 }, { 186, 215, 3 }, { 205, 215, 2 }, { 172, 215, 5 },
            { 195, 216, 3 }, { 205, 216, 3 }, { 196, 216, 3 }, { 197, 217, 3 }, { 207, 217, 4 },
            { 176, 217, 4 }, { 206, 217, 3 }, { 199, 218, 2 }, { 207, 218, 2 }, { 200, 219, 3 },
            { 208, 219, 4 }, { 190, 219, 4 }, { 189, 219, 4 }, { 199, 219, 4 }, { 218, 219, 3 },
            { 191, 220, 4 }, { 208, 220, 2 }, { 209, 221, 2 }, { 210, 221, 3 }, { 191, 221, 4 },
            { 220, 221, 2 }, { 201, 222, 3 }, { 210, 222, 3 }, { 211, 223, 3 }, { 212, 223, 2 },
            { 212, 224, 1 }, { 223, 224, 1 }, { 212, 225, 2 }, { 203, 225, 2 }, { 224, 225, 1 },
            { 213, 226, 2 }, { 214, 226, 2 }, { 205, 227, 3 }, { 215, 227, 3 }, { 216, 227, 2 },
            { 216, 228, 1 }, { 227, 228, 1 }, { 216, 229, 2 }, { 196, 229, 4 }, { 228, 229, 1 },
            { 196, 230, 4 }, { 229, 230, 3 }, { 188, 230, 4 }, { 206, 230, 3 }, { 206, 231, 3 },
            { 217, 231, 2 }, { 230, 231, 3 }, { 207, 232, 3 }, { 217, 232, 3 }, { 207, 233, 2 },
            { 218, 233, 2 }, { 232, 233, 1 }, { 208, 234, 3 }, { 219, 234, 2 }, { 208, 235, 3 },
            { 220, 235, 3 }, { 234, 235, 1 }, { 220, 236, 1 }, { 221, 236, 3 }, { 235, 236, 2 },
            { 221, 237, 1 }, { 236, 237, 2 }, { 210, 237, 3 }, { 211, 238, 3 }, { 223, 238, 2 },
            { 225, 239, 2 }, { 225, 240, 3 }, { 239, 240, 1 }, { 203, 240, 4 }, { 213, 240, 3 },
            { 213, 241, 3 }, { 240, 241, 1 }, { 226, 241, 3 }, { 214, 242, 2 }, { 226, 242, 2 },
            { 172, 243, 6 }, { 215, 243, 3 }, { 215, 244, 3 }, { 227, 244, 2 }, { 243, 244, 2 },
            { 230, 245, 2 }, { 231, 245, 3 }, { 231, 246, 1 }, { 245, 246, 2 }, { 231, 247, 2 },
            { 246, 247, 1 }, { 217, 247, 2 }, { 232, 247, 3 }, { 218, 248, 3 }, { 219, 248, 3 },
            { 233, 248, 4 }, { 234, 249, 2 }, { 235, 249, 3 }, { 219, 249, 2 }, { 248, 249, 1 },
            { 236, 250, 2 }, { 237, 250, 2 }, { 235, 250, 4 }, { 237, 251, 4 }, { 250, 251, 4 },
            { 210, 251, 4 }, { 222, 251, 3 }, { 211, 252, 4 }, { 238, 252, 3 }, { 201, 252, 5 },
            { 222, 252, 5 }, { 223, 253, 3 }, { 224, 253, 2 }, { 238, 253, 3 }, { 225, 253, 3 },
            { 239, 254, 2 }, { 240, 254, 3 }, { 225, 254, 2 }, { 253, 254, 1 }, { 240, 255, 2 },
            { 241, 255, 1 }, { 254, 255, 3 }, { 241, 256, 3 }, { 255, 256, 2 }, { 226, 256, 2 },
            { 242, 256, 2 }, { 243, 257, 3 }, { 244, 257, 1 }, { 228, 258, 2 }, { 229, 258, 3 },
            { 244, 258, 3 }, { 257, 258, 2 }, { 227, 258, 3 }, { 232, 259, 3 }, { 233, 259, 3 },
            { 247, 259, 2 }, { 246, 259, 3 }, { 222, 260, 3 }, { 251, 260, 2 }, { 222, 261, 4 },
            { 252, 261, 3 }, { 260, 261, 2 }, { 255, 262, 1 }, { 256, 262, 3 }, { 254, 262, 4 },
            { 230, 263, 4 }, { 245, 263, 3 }, { 229, 263, 4 }, { 258, 263, 4 }, { 245, 264, 3 },
            { 263, 264, 1 }, { 245, 265, 2 }, { 246, 265, 3 }, { 264, 265, 1 }, { 248, 266, 2 },
            { 249, 266, 3 }, { 233, 266, 5 }, { 259, 266, 6 }, { 249, 267, 2 }, { 266, 267, 1 },
            { 235, 268, 4 }, { 250, 268, 5 }, { 249, 268, 3 }, { 267, 268, 1 }, { 251, 269, 2 },
            { 250, 269, 5 }, { 260, 269, 2 }, { 252, 270, 3 }, { 261, 270, 4 }, { 238, 270, 4 },
            { 238, 271, 3 }, { 253, 271, 3 }, { 270, 271, 1 }, { 253, 272, 3 }, { 254, 272, 2 },
            { 271, 272, 3 }, { 254, 273, 3 }, { 272, 273, 2 }, { 262, 273, 2 }, { 262, 274, 3 },
            { 256, 274, 2 }, { 242, 274, 4 }, { 264, 275, 3 }, { 265, 275, 2 }, { 246, 275, 4 },
            { 259, 275, 4 }, { 267, 276, 2 }, { 268, 276, 1 }, { 266, 276, 3 }, { 268, 277, 2 },
            { 250, 277, 5 }, { 276, 277, 1 }, { 270, 278, 1 }, { 271, 278, 2 }, { 272, 279, 1 },
            { 273, 279, 3 }, { 271, 279, 4 }, { 262, 280, 2 }, { 273, 280, 2 }, { 274, 280, 3 },
            { 266, 281, 3 }, { 276, 281, 4 }, { 259, 281, 5 }, { 250, 282, 5 }, { 277, 282, 2 },
            { 250, 283, 4 }, { 282, 283, 2 }, { 250, 284, 5 }, { 269, 284, 4 }, { 283, 284, 1 },
            { 270, 285, 3 }, { 261, 285, 4 }, { 278, 285, 3 }, { 278, 286, 3 }, { 285, 286, 4 },
            { 271, 286, 3 }, { 279, 286, 3 }, { 279, 287, 1 }, { 286, 287, 2 }, { 279, 288, 3 },
            { 287, 288, 2 }, { 273, 288, 2 }, { 280, 288, 2 }, { 280, 289, 2 }, { 288, 289, 2 },
            { 274, 289, 3 }, { 274, 290, 2 }, { 289, 290, 1 }, { 257, 291, 5 }, { 243, 291, 5 },
            { 257, 292, 5 }, { 258, 292, 5 }, { 291, 292, 1 }, { 264, 293, 4 }, { 263, 293, 4 },
            { 275, 293, 3 }, { 275, 294, 2 }, { 293, 294, 1 }, { 259, 295, 5 }, { 281, 295, 6 },
            { 275, 295, 3 }, { 294, 295, 2 }, { 281, 296, 1 }, { 295, 296, 5 }, { 281, 297, 2 },
            { 276, 297, 3 }, { 296, 297, 1 }, { 276, 298, 2 }, { 297, 298, 2 }, { 277, 298, 3 },
            { 282, 298, 3 }, { 261, 299, 5 }, { 285, 299, 3 }, { 260, 299, 5 }, { 269, 299, 4 },
            { 285, 300, 2 }, { 299, 300, 1 }, { 287, 301, 2 }, { 288, 301, 2 }, { 291, 302, 1 },
            { 292, 302, 2 }, { 292, 303, 1 }, { 302, 303, 1 }, { 292, 304, 2 }, { 303, 304, 1 },
            { 258, 304, 6 }, { 263, 304, 7 }, { 293, 305, 1 }, { 294, 305, 2 }, { 294, 306, 2 },
            { 295, 306, 2 }, { 305, 306, 2 }, { 282, 307, 3 }, { 283, 307, 3 }, { 298, 307, 4 },
            { 283, 308, 2 }, { 284, 308, 3 }, { 307, 308, 1 }, { 269, 309, 5 }, { 284, 309, 5 },
            { 299, 309, 2 }, { 299, 310, 1 }, { 300, 310, 2 }, { 309, 310, 1 }, { 288, 311, 2 },
            { 289, 311, 3 }, { 301, 311, 2 }, { 289, 312, 3 }, { 311, 312, 4 }, { 290, 312, 3 },
            { 274, 312, 5 }, { 242, 312, 7 }, { 293, 313, 3 }, { 305, 313, 3 }, { 263, 313, 5 },
            { 304, 313, 6 }, { 305, 314, 2 }, { 306, 314, 2 }, { 313, 314, 3 }, { 296, 315, 2 },
            { 297, 315, 3 }, { 284, 316, 4 }, { 308, 316, 3 }, { 309, 316, 4 }, { 309, 317, 1 },
            { 310, 317, 2 }, { 316, 317, 3 }, { 313, 318, 4 }, { 314, 318, 1 }, { 295, 319, 4 },
            { 306, 319, 2 }, { 314, 319, 2 }, { 318, 319, 1 }, { 295, 320, 5 }, { 319, 320, 4 },
            { 296, 320, 4 }, { 315, 320, 3 }, { 297, 321, 4 }, { 298, 321, 4 }, { 315, 321, 3 },
            { 298, 322, 4 }, { 307, 322, 3 }, { 321, 322, 2 }, { 316, 323, 4 }, { 317, 323, 1 },
            { 317, 324, 2 }, { 310, 324, 2 }, { 323, 324, 1 }, { 310, 325, 3 }, { 300, 325, 3 },
            { 324, 325, 1 }, { 300, 326, 4 }, { 285, 326, 4 }, { 325, 326, 1 }, { 285, 327, 5 },
            { 286, 327, 5 }, { 326, 327, 3 }, { 286, 328, 5 }, { 327, 328, 2 }, { 287, 328, 5 },
            { 301, 328, 4 }, { 301, 329, 3 }, { 311, 329, 3 }, { 328, 329, 2 }, { 311, 330, 2 },
            { 329, 330, 1 }, { 312, 330, 5 }, { 303, 331, 4 }, { 302, 331, 3 }, { 303, 332, 3 },
            { 304, 332, 4 }, { 331, 332, 1 }, { 304, 333, 3 }, { 332, 333, 1 }, { 313, 334, 3 },
            { 318, 334, 3 }, { 315, 335, 3 }, { 320, 335, 2 }, { 315, 336, 3 }, { 321, 336, 2 },
            { 335, 336, 2 }, { 329, 337, 1 }, { 330, 337, 2 }, { 328, 337, 3 }, { 330, 338, 5 },
            { 312, 338, 3 }, { 331, 339, 1 }, { 332, 339, 2 }, { 313, 341, 5 }, { 334, 341, 5 },
            { 333, 340, 2 }, { 333, 341, 3 }, { 340, 341, 1 }, { 304, 341, 5 }, { 318, 342, 3 },
            { 334, 342, 1 }, { 318, 343, 3 }, { 319, 343, 2 }, { 342, 343, 3 }, { 319, 344, 3 },
            { 343, 344, 1 }, { 319, 345, 3 }, { 320, 345, 3 }, { 344, 345, 1 }, { 320, 346, 2 },
            { 335, 346, 2 }, { 345, 346, 2 }, { 308, 347, 4 }, { 316, 347, 4 }, { 307, 347, 5 },
            { 322, 347, 4 }, { 316, 348, 4 }, { 323, 348, 5 }, { 347, 348, 1 }, { 327, 349, 2 },
            { 326, 349, 4 }, { 328, 349, 3 }, { 330, 350, 3 }, { 337, 350, 3 }, { 330, 351, 3 },
            { 350, 351, 1 }, { 338, 351, 3 }, { 333, 352, 2 }, { 340, 352, 2 }, { 332, 352, 3 },
            { 339, 352, 3 }, { 340, 353, 1 }, { 341, 353, 2 }, { 352, 353, 1 }, { 341, 354, 2 },
            { 353, 354, 2 }, { 334, 354, 4 }, { 342, 354, 4 }, { 345, 355, 1 }, { 346, 355, 3 },
            { 344, 355, 2 }, { 343, 355, 3 }, { 346, 356, 1 }, { 355, 356, 2 }, { 335, 356, 3 },
            { 321, 357, 4 }, { 322, 357, 4 }, { 336, 357, 3 }, { 322, 358, 4 }, { 347, 358, 3 },
            { 357, 358, 2 }, { 347, 359, 2 }, { 348, 359, 1 }, { 358, 359, 3 }, { 323, 360, 4 },
            { 324, 360, 4 }, { 348, 360, 4 }, { 359, 360, 3 }, { 326, 361, 4 }, { 349, 361, 5 },
            { 325, 361, 3 }, { 324, 361, 4 }, { 360, 361, 3 }, { 349, 362, 1 }, { 361, 362, 4 },
            { 349, 363, 3 }, { 362, 363, 2 }, { 328, 363, 3 }, { 337, 363, 3 }, { 337, 364, 3 },
            { 350, 364, 2 }, { 363, 364, 3 }, { 350, 365, 1 }, { 364, 365, 1 }, { 351, 365, 2 },
            { 351, 366, 2 }, { 365, 366, 2 }, { 338, 366, 3 }, { 339, 367, 3 }, { 352, 367, 2 },
            { 352, 368, 2 }, { 353, 368, 1 }, { 367, 368, 2 }, { 353, 369, 2 }, { 354, 369, 2 },
            { 368, 369, 1 }, { 354, 370, 1 }, { 369, 370, 1 }, { 354, 371, 2 }, { 342, 371, 3 },
            { 370, 371, 1 }, { 342, 372, 3 }, { 343, 372, 3 }, { 371, 372, 3 }, { 355, 373, 1 },
            { 356, 373, 3 }, { 343, 373, 3 }, { 356, 374, 1 }, { 373, 374, 2 }, { 356, 375, 3 },
            { 374, 375, 2 }, { 336, 375, 4 }, { 357, 375, 4 }, { 335, 375, 4 }, { 365, 376, 2 },
            { 366, 376, 2 }, { 370, 377, 1 }, { 371, 377, 2 }, { 369, 377, 2 }, { 368, 377, 3 },
            { 343, 378, 4 }, { 372, 378, 2 }, { 343, 379, 3 }, { 373, 379, 3 }, { 378, 379, 1 },
            { 373, 380, 1 }, { 374, 380, 3 }, { 379, 380, 2 }, { 374, 381, 3 }, { 375, 381, 1 },
            { 375, 382, 3 }, { 357, 382, 3 }, { 381, 382, 2 }, { 360, 383, 3 }, { 361, 383, 3 },
            { 361, 384, 3 }, { 362, 384, 3 }, { 362, 385, 3 }, { 363, 385, 2 }, { 364, 385, 4 },
            { 364, 386, 2 }, { 385, 386, 3 }, { 365, 386, 3 }, { 376, 386, 3 }, { 368, 387, 3 },
            { 377, 387, 4 }, { 367, 387, 3 }, { 371, 388, 2 }, { 372, 388, 4 }, { 377, 388, 2 },
            { 378, 389, 2 }, { 379, 389, 3 }, { 372, 389, 2 }, { 388, 389, 3 }, { 379, 390, 2 },
            { 380, 390, 2 }, { 389, 390, 3 }, { 374, 391, 2 }, { 380, 391, 3 }, { 381, 391, 3 },
            { 381, 392, 2 }, { 382, 392, 2 }, { 391, 392, 3 }, { 357, 393, 4 }, { 358, 393, 4 },
            { 382, 393, 3 }, { 360, 394, 4 }, { 359, 394, 4 }, { 383, 394, 3 }, { 383, 395, 1 },
            { 394, 395, 2 }, { 383, 396, 3 }, { 395, 396, 2 }, { 361, 396, 3 }, { 384, 396, 3 },
            { 384, 397, 2 }, { 396, 397, 1 }, { 362, 398, 3 }, { 384, 398, 3 }, { 385, 398, 3 },
            { 386, 399, 4 }, { 376, 399, 3 }, { 366, 399, 3 }, { 339, 400, 5 }, { 367, 400, 4 },
            { 387, 400, 3 }, { 388, 401, 2 }, { 389, 401, 3 }, { 389, 402, 2 }, { 401, 402, 1 },
            { 389, 403, 4 }, { 390, 403, 1 }, { 390, 404, 3 }, { 403, 404, 2 }, { 380, 404, 3 },
            { 391, 404, 2 }, { 391, 405, 4 }, { 392, 405, 1 }, { 392, 406, 3 }, { 405, 406, 2 },
            { 382, 406, 3 }, { 393, 406, 2 }, { 358, 407, 5 }, { 359, 407, 5 }, { 393, 407, 3 },
            { 359, 408, 5 }, { 394, 408, 4 }, { 407, 408, 1 }, { 394, 409, 2 }, { 395, 409, 2 },
            { 395, 410, 1 }, { 396, 410, 3 }, { 409, 410, 1 }, { 396, 411, 2 }, { 397, 411, 1 },
            { 410, 411, 3 }, { 397, 412, 2 }, { 411, 412, 1 }, { 384, 412, 2 }, { 398, 412, 3 },
            { 398, 413, 1 }, { 412, 413, 2 }, { 385, 413, 3 }, { 366, 414, 5 }, { 338, 414, 6 },
            { 399, 414, 2 }, { 388, 415, 3 }, { 401, 415, 4 }, { 377, 415, 4 }, { 387, 415, 3 },
            { 389, 416, 2 }, { 402, 416, 2 }, { 403, 416, 4 }, { 401, 416, 3 }, { 403, 417, 1 },
            { 404, 417, 3 }, { 416, 417, 3 }, { 404, 418, 2 }, { 417, 418, 3 }, { 391, 418, 2 },
            { 405, 418, 4 }, { 393, 419, 2 }, { 406, 419, 2 }, { 407, 419, 3 }, { 394, 420, 3 },
            { 408, 420, 2 }, { 407, 420, 3 }, { 394, 421, 2 }, { 409, 421, 2 }, { 420, 421, 2 },
            { 411, 422, 2 }, { 412, 422, 1 }, { 412, 423, 2 }, { 413, 423, 2 }, { 422, 423, 1 },
            { 387, 425, 3 }, { 415, 425, 3 }, { 400, 425, 3 }, { 400, 424, 2 }, { 424, 425, 2 },
            { 401, 426, 3 }, { 415, 426, 3 }, { 416, 426, 4 }, { 417, 427, 1 }, { 418, 427, 4 },
            { 416, 427, 4 }, { 409, 428, 2 }, { 410, 428, 3 }, { 421, 428, 2 }, { 411, 429, 2 },
            { 422, 429, 2 }, { 410, 429, 4 }, { 428, 429, 4 }, { 422, 430, 2 }, { 423, 430, 1 },
            { 429, 430, 2 }, { 413, 430, 3 }, { 385, 431, 5 }, { 413, 431, 5 }, { 386, 431, 5 },
            { 386, 432, 4 }, { 431, 432, 1 }, { 399, 433, 4 }, { 414, 433, 4 }, { 386, 433, 5 },
            { 432, 433, 1 }, { 433, 434, 3 }, { 414, 434, 2 }, { 424, 435, 2 }, { 425, 435, 2 },
            { 425, 436, 1 }, { 435, 436, 1 }, { 415, 437, 3 }, { 426, 437, 4 }, { 425, 437, 2 },
            { 436, 437, 1 }, { 418, 438, 3 }, { 427, 438, 3 }, { 418, 439, 4 }, { 405, 439, 3 },
            { 438, 439, 4 }, { 406, 440, 4 }, { 419, 440, 3 }, { 405, 440, 4 }, { 439, 440, 1 },
            { 419, 441, 3 }, { 440, 441, 3 }, { 407, 441, 4 }, { 407, 442, 3 }, { 420, 442, 3 },
            { 441, 442, 1 }, { 429, 443, 2 }, { 430, 443, 2 }, { 430, 444, 2 }, { 443, 444, 2 },
            { 413, 444, 3 }, { 431, 445, 2 }, { 432, 445, 3 }, { 413, 445, 5 }, { 444, 445, 3 },
            { 433, 446, 2 }, { 434, 446, 3 }, { 436, 447, 1 }, { 437, 447, 2 }, { 435, 447, 2 },
            { 439, 448, 1 }, { 440, 448, 2 }, { 440, 449, 1 }, { 448, 449, 1 }, { 440, 450, 3 },
            { 441, 450, 2 }, { 449, 450, 2 }, { 441, 451, 2 }, { 442, 451, 1 }, { 450, 451, 2 },
            { 421, 452, 4 }, { 420, 452, 3 }, { 442, 452, 3 }, { 451, 452, 2 }, { 421, 453, 3 },
            { 428, 453, 3 }, { 452, 453, 2 }, { 428, 454, 3 }, { 429, 454, 4 }, { 453, 454, 2 },
            { 429, 455, 3 }, { 443, 455, 3 }, { 454, 455, 2 }, { 444, 456, 2 }, { 445, 456, 3 },
            { 445, 457, 1 }, { 456, 457, 2 }, { 432, 457, 3 }, { 432, 458, 3 }, { 457, 458, 3 },
            { 433, 458, 2 }, { 446, 458, 2 }, { 424, 459, 3 }, { 435, 459, 3 }, { 435, 460, 2 },
            { 447, 460, 2 }, { 459, 460, 1 }, { 427, 461, 3 }, { 438, 461, 3 }, { 416, 461, 5 },
            { 438, 462, 3 }, { 461, 462, 1 }, { 439, 463, 3 }, { 438, 463, 4 }, { 448, 463, 2 },
            { 452, 464, 1 }, { 453, 464, 3 }, { 451, 464, 3 }, { 454, 465, 3 }, { 455, 465, 1 },
            { 455, 466, 2 }, { 443, 466, 3 }, { 465, 466, 1 }, { 444, 467, 3 }, { 456, 467, 4 },
            { 443, 467, 2 }, { 466, 467, 1 }, { 456, 468, 3 }, { 457, 468, 1 }, { 457, 469, 2 },
            { 468, 469, 1 }, { 458, 469, 3 }, { 459, 470, 3 }, { 460, 470, 2 }, { 447, 470, 2 },
            { 437, 470, 4 }, { 461, 471, 2 }, { 462, 471, 3 }, { 416, 471, 6 }, { 426, 471, 7 },
            { 438, 472, 4 }, { 462, 472, 4 }, { 463, 472, 2 }, { 463, 473, 3 }, { 472, 473, 3 },
            { 449, 473, 2 }, { 450, 473, 3 }, { 448, 473, 3 }, { 453, 474, 2 }, { 454, 474, 3 },
            { 464, 474, 3 }, { 466, 475, 2 }, { 467, 475, 1 }, { 465, 475, 3 }, { 456, 476, 2 },
            { 468, 476, 3 }, { 467, 476, 4 }, { 475, 476, 3 }, { 468, 477, 1 }, { 476, 477, 2 },
            { 469, 477, 2 }, { 469, 478, 1 }, { 477, 478, 1 }, { 458, 478, 3 }, { 446, 479, 4 },
            { 458, 479, 3 }, { 446, 480, 4 }, { 434, 480, 4 }, { 479, 480, 1 }, { 459, 481, 3 },
            { 470, 481, 1 }, { 462, 482, 3 }, { 471, 482, 4 }, { 472, 482, 3 }, { 450, 483, 4 },
            { 451, 483, 5 }, { 473, 483, 2 }, { 464, 484, 3 }, { 474, 484, 1 }, { 474, 485, 3 },
            { 484, 485, 2 }, { 454, 485, 3 }, { 465, 485, 3 }, { 475, 486, 2 }, { 476, 486, 3 },
            { 476, 487, 2 }, { 486, 487, 1 }, { 477, 488, 1 }, { 478, 488, 2 }, { 476, 488, 3 },
            { 487, 488, 3 }, { 458, 489, 3 }, { 478, 489, 3 }, { 479, 489, 3 }, { 479, 490, 2 },
            { 489, 490, 3 }, { 480, 490, 1 }, { 481, 491, 2 }, { 459, 491, 4 }, { 471, 492, 6 },
            { 482, 492, 9 }, { 481, 492, 5 }, { 491, 492, 5 }, { 470, 492, 5 }, { 426, 492, 6 },
            { 437, 492, 6 }, { 482, 493, 3 }, { 492, 493, 10 }, { 472, 493, 2 }, { 473, 493, 4 },
            { 473, 494, 3 }, { 483, 494, 1 }, { 493, 494, 4 }, { 464, 495, 4 }, { 484, 495, 5 },
            { 483, 495, 4 }, { 494, 495, 3 }, { 451, 495, 4 }, { 484, 496, 1 }, { 485, 496, 3 },
            { 495, 496, 4 }, { 485, 497, 4 }, { 496, 497, 5 }, { 465, 497, 4 }, { 475, 497, 3 },
            { 487, 498, 3 }, { 488, 498, 6 }, { 486, 498, 2 }, { 475, 498, 2 }, { 497, 498, 1 },
            { 489, 499, 2 }, { 490, 499, 5 }, { 488, 499, 3 }, { 498, 499, 7 }, { 478, 499, 3 } };
        double maxWeight = 866;
        double minWeight = 425;
        test(edges, objectiveSense == MAXIMIZE ? maxWeight : minWeight, objectiveSense);
    }

    /**
     * Test on an unweighted $K_{50}$
     */
    @Test
    public void testGetMatching37()
    {
        DefaultUndirectedWeightedGraph<Integer, DefaultWeightedEdge> graph =
            new DefaultUndirectedWeightedGraph<>(DefaultWeightedEdge.class);
        graph.setVertexSupplier(SupplierUtil.createIntegerSupplier());
        CompleteGraphGenerator<Integer, DefaultWeightedEdge> generator =
            new CompleteGraphGenerator<>(20);
        generator.generateGraph(graph);

        KolmogorovWeightedPerfectMatching<Integer, DefaultWeightedEdge> perfectMatching =
            new KolmogorovWeightedPerfectMatching<>(graph, options, objectiveSense);
        MatchingAlgorithm.Matching<Integer, DefaultWeightedEdge> matching =
            perfectMatching.getMatching();

        assertEquals(10, matching.getWeight(), EPS);
        assertTrue(perfectMatching.testOptimality());
        checkMatchingAndDualSolution(matching, perfectMatching.getDualSolution(), objectiveSense);
    }

    /**
     * Test on a weighted $K_{50}$
     */
    @Test
    public void testGetMatching38()
    {
        int[][] edges = new int[][] { { 1, 0, 597 }, { 2, 0, 614 }, { 2, 1, 57 }, { 3, 0, 554 },
            { 3, 1, 883 }, { 3, 2, 883 }, { 4, 0, 752 }, { 4, 1, 191 }, { 4, 2, 972 },
            { 4, 3, 392 }, { 5, 0, 542 }, { 5, 1, 507 }, { 5, 2, 931 }, { 5, 3, 223 }, { 5, 4, 38 },
            { 6, 0, 125 }, { 6, 1, 261 }, { 6, 2, 511 }, { 6, 3, 892 }, { 6, 4, 250 },
            { 6, 5, 791 }, { 7, 0, 118 }, { 7, 1, 184 }, { 7, 2, 33 }, { 7, 3, 665 }, { 7, 4, 446 },
            { 7, 5, 908 }, { 7, 6, 355 }, { 8, 0, 812 }, { 8, 1, 794 }, { 8, 2, 239 },
            { 8, 3, 222 }, { 8, 4, 447 }, { 8, 5, 764 }, { 8, 6, 779 }, { 8, 7, 693 },
            { 9, 0, 653 }, { 9, 1, 444 }, { 9, 2, 344 }, { 9, 3, 565 }, { 9, 4, 995 },
            { 9, 5, 999 }, { 9, 6, 251 }, { 9, 7, 173 }, { 9, 8, 501 }, { 10, 0, 945 },
            { 10, 1, 754 }, { 10, 2, 147 }, { 10, 3, 912 }, { 10, 4, 464 }, { 10, 5, 766 },
            { 10, 6, 311 }, { 10, 7, 242 }, { 10, 8, 887 }, { 10, 9, 771 }, { 11, 0, 63 },
            { 11, 1, 566 }, { 11, 2, 219 }, { 11, 3, 931 }, { 11, 4, 519 }, { 11, 5, 707 },
            { 11, 6, 630 }, { 11, 7, 702 }, { 11, 8, 258 }, { 11, 9, 164 }, { 11, 10, 13 },
            { 12, 0, 238 }, { 12, 1, 769 }, { 12, 2, 498 }, { 12, 3, 928 }, { 12, 4, 335 },
            { 12, 5, 255 }, { 12, 6, 254 }, { 12, 7, 742 }, { 12, 8, 796 }, { 12, 9, 191 },
            { 12, 10, 915 }, { 12, 11, 179 }, { 13, 0, 292 }, { 13, 1, 586 }, { 13, 2, 743 },
            { 13, 3, 543 }, { 13, 4, 180 }, { 13, 5, 9 }, { 13, 6, 394 }, { 13, 7, 843 },
            { 13, 8, 198 }, { 13, 9, 843 }, { 13, 10, 768 }, { 13, 11, 818 }, { 13, 12, 734 },
            { 14, 0, 115 }, { 14, 1, 281 }, { 14, 2, 570 }, { 14, 3, 517 }, { 14, 4, 346 },
            { 14, 5, 855 }, { 14, 6, 615 }, { 14, 7, 943 }, { 14, 8, 684 }, { 14, 9, 399 },
            { 14, 10, 906 }, { 14, 11, 87 }, { 14, 12, 753 }, { 14, 13, 392 }, { 15, 0, 851 },
            { 15, 1, 171 }, { 15, 2, 547 }, { 15, 3, 321 }, { 15, 4, 400 }, { 15, 5, 98 },
            { 15, 6, 423 }, { 15, 7, 879 }, { 15, 8, 872 }, { 15, 9, 33 }, { 15, 10, 714 },
            { 15, 11, 540 }, { 15, 12, 360 }, { 15, 13, 245 }, { 15, 14, 161 }, { 16, 0, 163 },
            { 16, 1, 729 }, { 16, 2, 442 }, { 16, 3, 509 }, { 16, 4, 352 }, { 16, 5, 301 },
            { 16, 6, 130 }, { 16, 7, 177 }, { 16, 8, 573 }, { 16, 9, 916 }, { 16, 10, 922 },
            { 16, 11, 889 }, { 16, 12, 102 }, { 16, 13, 391 }, { 16, 14, 718 }, { 16, 15, 577 },
            { 17, 0, 70 }, { 17, 1, 336 }, { 17, 2, 765 }, { 17, 3, 407 }, { 17, 4, 19 },
            { 17, 5, 892 }, { 17, 6, 807 }, { 17, 7, 246 }, { 17, 8, 28 }, { 17, 9, 294 },
            { 17, 10, 499 }, { 17, 11, 952 }, { 17, 12, 946 }, { 17, 13, 562 }, { 17, 14, 60 },
            { 17, 15, 231 }, { 17, 16, 996 }, { 18, 0, 106 }, { 18, 1, 107 }, { 18, 2, 408 },
            { 18, 3, 341 }, { 18, 4, 64 }, { 18, 5, 657 }, { 18, 6, 748 }, { 18, 7, 657 },
            { 18, 8, 177 }, { 18, 9, 59 }, { 18, 10, 700 }, { 18, 11, 861 }, { 18, 12, 106 },
            { 18, 13, 752 }, { 18, 14, 417 }, { 18, 15, 531 }, { 18, 16, 890 }, { 18, 17, 302 },
            { 19, 0, 262 }, { 19, 1, 277 }, { 19, 2, 684 }, { 19, 3, 440 }, { 19, 4, 170 },
            { 19, 5, 41 }, { 19, 6, 135 }, { 19, 7, 508 }, { 19, 8, 805 }, { 19, 9, 378 },
            { 19, 10, 419 }, { 19, 11, 280 }, { 19, 12, 655 }, { 19, 13, 367 }, { 19, 14, 723 },
            { 19, 15, 661 }, { 19, 16, 871 }, { 19, 17, 549 }, { 19, 18, 289 }, { 20, 0, 324 },
            { 20, 1, 559 }, { 20, 2, 264 }, { 20, 3, 426 }, { 20, 4, 837 }, { 20, 5, 138 },
            { 20, 6, 838 }, { 20, 7, 744 }, { 20, 8, 215 }, { 20, 9, 982 }, { 20, 10, 332 },
            { 20, 11, 270 }, { 20, 12, 313 }, { 20, 13, 596 }, { 20, 14, 883 }, { 20, 15, 859 },
            { 20, 16, 303 }, { 20, 17, 192 }, { 20, 18, 330 }, { 20, 19, 670 }, { 21, 0, 801 },
            { 21, 1, 527 }, { 21, 2, 367 }, { 21, 3, 322 }, { 21, 4, 429 }, { 21, 5, 717 },
            { 21, 6, 343 }, { 21, 7, 550 }, { 21, 8, 52 }, { 21, 9, 505 }, { 21, 10, 642 },
            { 21, 11, 220 }, { 21, 12, 1 }, { 21, 13, 877 }, { 21, 14, 533 }, { 21, 15, 363 },
            { 21, 16, 374 }, { 21, 17, 285 }, { 21, 18, 295 }, { 21, 19, 956 }, { 21, 20, 393 },
            { 22, 0, 235 }, { 22, 1, 405 }, { 22, 2, 302 }, { 22, 3, 804 }, { 22, 4, 421 },
            { 22, 5, 857 }, { 22, 6, 774 }, { 22, 7, 306 }, { 22, 8, 52 }, { 22, 9, 596 },
            { 22, 10, 558 }, { 22, 11, 749 }, { 22, 12, 817 }, { 22, 13, 244 }, { 22, 14, 656 },
            { 22, 15, 773 }, { 22, 16, 150 }, { 22, 17, 729 }, { 22, 18, 584 }, { 22, 19, 499 },
            { 22, 20, 839 }, { 22, 21, 658 }, { 23, 0, 700 }, { 23, 1, 395 }, { 23, 2, 891 },
            { 23, 3, 813 }, { 23, 4, 360 }, { 23, 5, 208 }, { 23, 6, 67 }, { 23, 7, 739 },
            { 23, 8, 386 }, { 23, 9, 1 }, { 23, 10, 157 }, { 23, 11, 689 }, { 23, 12, 159 },
            { 23, 13, 385 }, { 23, 14, 901 }, { 23, 15, 531 }, { 23, 16, 56 }, { 23, 17, 684 },
            { 23, 18, 305 }, { 23, 19, 947 }, { 23, 20, 876 }, { 23, 21, 686 }, { 23, 22, 544 },
            { 24, 0, 631 }, { 24, 1, 923 }, { 24, 2, 110 }, { 24, 3, 222 }, { 24, 4, 313 },
            { 24, 5, 253 }, { 24, 6, 415 }, { 24, 7, 14 }, { 24, 8, 671 }, { 24, 9, 21 },
            { 24, 10, 471 }, { 24, 11, 741 }, { 24, 12, 707 }, { 24, 13, 805 }, { 24, 14, 496 },
            { 24, 15, 598 }, { 24, 16, 445 }, { 24, 17, 623 }, { 24, 18, 275 }, { 24, 19, 256 },
            { 24, 20, 340 }, { 24, 21, 829 }, { 24, 22, 493 }, { 24, 23, 904 }, { 25, 0, 637 },
            { 25, 1, 157 }, { 25, 2, 730 }, { 25, 3, 484 }, { 25, 4, 34 }, { 25, 5, 813 },
            { 25, 6, 190 }, { 25, 7, 246 }, { 25, 8, 91 }, { 25, 9, 271 }, { 25, 10, 595 },
            { 25, 11, 897 }, { 25, 12, 897 }, { 25, 13, 713 }, { 25, 14, 684 }, { 25, 15, 187 },
            { 25, 16, 279 }, { 25, 17, 699 }, { 25, 18, 183 }, { 25, 19, 974 }, { 25, 20, 236 },
            { 25, 21, 430 }, { 25, 22, 89 }, { 25, 23, 68 }, { 25, 24, 59 }, { 26, 0, 766 },
            { 26, 1, 60 }, { 26, 2, 247 }, { 26, 3, 804 }, { 26, 4, 387 }, { 26, 5, 356 },
            { 26, 6, 771 }, { 26, 7, 507 }, { 26, 8, 578 }, { 26, 9, 287 }, { 26, 10, 353 },
            { 26, 11, 989 }, { 26, 12, 963 }, { 26, 13, 698 }, { 26, 14, 968 }, { 26, 15, 75 },
            { 26, 16, 799 }, { 26, 17, 560 }, { 26, 18, 778 }, { 26, 19, 872 }, { 26, 20, 543 },
            { 26, 21, 237 }, { 26, 22, 584 }, { 26, 23, 748 }, { 26, 24, 596 }, { 26, 25, 305 },
            { 27, 0, 995 }, { 27, 1, 115 }, { 27, 2, 854 }, { 27, 3, 185 }, { 27, 4, 617 },
            { 27, 5, 96 }, { 27, 6, 987 }, { 27, 7, 511 }, { 27, 8, 951 }, { 27, 9, 626 },
            { 27, 10, 711 }, { 27, 11, 705 }, { 27, 12, 473 }, { 27, 13, 409 }, { 27, 14, 106 },
            { 27, 15, 247 }, { 27, 16, 659 }, { 27, 17, 914 }, { 27, 18, 995 }, { 27, 19, 718 },
            { 27, 20, 931 }, { 27, 21, 557 }, { 27, 22, 343 }, { 27, 23, 137 }, { 27, 24, 422 },
            { 27, 25, 24 }, { 27, 26, 517 }, { 28, 0, 768 }, { 28, 1, 459 }, { 28, 2, 924 },
            { 28, 3, 793 }, { 28, 4, 697 }, { 28, 5, 288 }, { 28, 6, 557 }, { 28, 7, 355 },
            { 28, 8, 210 }, { 28, 9, 911 }, { 28, 10, 583 }, { 28, 11, 875 }, { 28, 12, 280 },
            { 28, 13, 540 }, { 28, 14, 380 }, { 28, 15, 916 }, { 28, 16, 113 }, { 28, 17, 286 },
            { 28, 18, 51 }, { 28, 19, 82 }, { 28, 20, 574 }, { 28, 21, 519 }, { 28, 22, 682 },
            { 28, 23, 209 }, { 28, 24, 13 }, { 28, 25, 642 }, { 28, 26, 780 }, { 28, 27, 971 },
            { 29, 0, 291 }, { 29, 1, 672 }, { 29, 2, 669 }, { 29, 3, 414 }, { 29, 4, 679 },
            { 29, 5, 407 }, { 29, 6, 450 }, { 29, 7, 824 }, { 29, 8, 721 }, { 29, 9, 408 },
            { 29, 10, 781 }, { 29, 11, 791 }, { 29, 12, 705 }, { 29, 13, 779 }, { 29, 14, 14 },
            { 29, 15, 66 }, { 29, 16, 780 }, { 29, 17, 353 }, { 29, 18, 862 }, { 29, 19, 69 },
            { 29, 20, 424 }, { 29, 21, 558 }, { 29, 22, 925 }, { 29, 23, 157 }, { 29, 24, 105 },
            { 29, 25, 1 }, { 29, 26, 731 }, { 29, 27, 409 }, { 29, 28, 341 }, { 30, 0, 217 },
            { 30, 1, 756 }, { 30, 2, 72 }, { 30, 3, 233 }, { 30, 4, 718 }, { 30, 5, 492 },
            { 30, 6, 373 }, { 30, 7, 159 }, { 30, 8, 512 }, { 30, 9, 321 }, { 30, 10, 116 },
            { 30, 11, 927 }, { 30, 12, 32 }, { 30, 13, 48 }, { 30, 14, 520 }, { 30, 15, 130 },
            { 30, 16, 370 }, { 30, 17, 672 }, { 30, 18, 898 }, { 30, 19, 221 }, { 30, 20, 475 },
            { 30, 21, 619 }, { 30, 22, 168 }, { 30, 23, 642 }, { 30, 24, 637 }, { 30, 25, 651 },
            { 30, 26, 952 }, { 30, 27, 155 }, { 30, 28, 212 }, { 30, 29, 681 }, { 31, 0, 135 },
            { 31, 1, 912 }, { 31, 2, 420 }, { 31, 3, 696 }, { 31, 4, 930 }, { 31, 5, 784 },
            { 31, 6, 633 }, { 31, 7, 104 }, { 31, 8, 592 }, { 31, 9, 451 }, { 31, 10, 747 },
            { 31, 11, 423 }, { 31, 12, 618 }, { 31, 13, 886 }, { 31, 14, 660 }, { 31, 15, 810 },
            { 31, 16, 704 }, { 31, 17, 32 }, { 31, 18, 423 }, { 31, 19, 977 }, { 31, 20, 586 },
            { 31, 21, 441 }, { 31, 22, 878 }, { 31, 23, 1000 }, { 31, 24, 15 }, { 31, 25, 379 },
            { 31, 26, 136 }, { 31, 27, 145 }, { 31, 28, 445 }, { 31, 29, 736 }, { 31, 30, 309 },
            { 32, 0, 583 }, { 32, 1, 798 }, { 32, 2, 651 }, { 32, 3, 799 }, { 32, 4, 910 },
            { 32, 5, 524 }, { 32, 6, 819 }, { 32, 7, 944 }, { 32, 8, 446 }, { 32, 9, 497 },
            { 32, 10, 769 }, { 32, 11, 82 }, { 32, 12, 263 }, { 32, 13, 53 }, { 32, 14, 930 },
            { 32, 15, 671 }, { 32, 16, 622 }, { 32, 17, 20 }, { 32, 18, 960 }, { 32, 19, 829 },
            { 32, 20, 445 }, { 32, 21, 827 }, { 32, 22, 444 }, { 32, 23, 861 }, { 32, 24, 120 },
            { 32, 25, 903 }, { 32, 26, 462 }, { 32, 27, 225 }, { 32, 28, 804 }, { 32, 29, 267 },
            { 32, 30, 821 }, { 32, 31, 120 }, { 33, 0, 194 }, { 33, 1, 476 }, { 33, 2, 289 },
            { 33, 3, 951 }, { 33, 4, 857 }, { 33, 5, 298 }, { 33, 6, 365 }, { 33, 7, 501 },
            { 33, 8, 722 }, { 33, 9, 213 }, { 33, 10, 515 }, { 33, 11, 379 }, { 33, 12, 637 },
            { 33, 13, 409 }, { 33, 14, 992 }, { 33, 15, 390 }, { 33, 16, 936 }, { 33, 17, 112 },
            { 33, 18, 382 }, { 33, 19, 602 }, { 33, 20, 888 }, { 33, 21, 995 }, { 33, 22, 376 },
            { 33, 23, 581 }, { 33, 24, 520 }, { 33, 25, 677 }, { 33, 26, 936 }, { 33, 27, 750 },
            { 33, 28, 270 }, { 33, 29, 715 }, { 33, 30, 845 }, { 33, 31, 40 }, { 33, 32, 741 },
            { 34, 0, 308 }, { 34, 1, 848 }, { 34, 2, 63 }, { 34, 3, 613 }, { 34, 4, 745 },
            { 34, 5, 29 }, { 34, 6, 845 }, { 34, 7, 879 }, { 34, 8, 80 }, { 34, 9, 518 },
            { 34, 10, 985 }, { 34, 11, 140 }, { 34, 12, 947 }, { 34, 13, 467 }, { 34, 14, 149 },
            { 34, 15, 894 }, { 34, 16, 680 }, { 34, 17, 998 }, { 34, 18, 258 }, { 34, 19, 6 },
            { 34, 20, 285 }, { 34, 21, 691 }, { 34, 22, 58 }, { 34, 23, 515 }, { 34, 24, 227 },
            { 34, 25, 389 }, { 34, 26, 426 }, { 34, 27, 36 }, { 34, 28, 387 }, { 34, 29, 276 },
            { 34, 30, 250 }, { 34, 31, 661 }, { 34, 32, 257 }, { 34, 33, 602 }, { 35, 0, 642 },
            { 35, 1, 734 }, { 35, 2, 884 }, { 35, 3, 764 }, { 35, 4, 587 }, { 35, 5, 458 },
            { 35, 6, 478 }, { 35, 7, 502 }, { 35, 8, 240 }, { 35, 9, 722 }, { 35, 10, 847 },
            { 35, 11, 407 }, { 35, 12, 175 }, { 35, 13, 345 }, { 35, 14, 376 }, { 35, 15, 356 },
            { 35, 16, 295 }, { 35, 17, 627 }, { 35, 18, 214 }, { 35, 19, 272 }, { 35, 20, 623 },
            { 35, 21, 110 }, { 35, 22, 614 }, { 35, 23, 969 }, { 35, 24, 209 }, { 35, 25, 107 },
            { 35, 26, 183 }, { 35, 27, 998 }, { 35, 28, 385 }, { 35, 29, 310 }, { 35, 30, 832 },
            { 35, 31, 492 }, { 35, 32, 102 }, { 35, 33, 344 }, { 35, 34, 983 }, { 36, 0, 40 },
            { 36, 1, 276 }, { 36, 2, 446 }, { 36, 3, 283 }, { 36, 4, 103 }, { 36, 5, 564 },
            { 36, 6, 210 }, { 36, 7, 431 }, { 36, 8, 737 }, { 36, 9, 905 }, { 36, 10, 343 },
            { 36, 11, 684 }, { 36, 12, 267 }, { 36, 13, 519 }, { 36, 14, 747 }, { 36, 15, 729 },
            { 36, 16, 452 }, { 36, 17, 665 }, { 36, 18, 845 }, { 36, 19, 450 }, { 36, 20, 264 },
            { 36, 21, 455 }, { 36, 22, 845 }, { 36, 23, 858 }, { 36, 24, 944 }, { 36, 25, 995 },
            { 36, 26, 867 }, { 36, 27, 998 }, { 36, 28, 610 }, { 36, 29, 726 }, { 36, 30, 863 },
            { 36, 31, 583 }, { 36, 32, 348 }, { 36, 33, 468 }, { 36, 34, 152 }, { 36, 35, 923 },
            { 37, 0, 828 }, { 37, 1, 41 }, { 37, 2, 48 }, { 37, 3, 513 }, { 37, 4, 627 },
            { 37, 5, 638 }, { 37, 6, 340 }, { 37, 7, 904 }, { 37, 8, 881 }, { 37, 9, 804 },
            { 37, 10, 50 }, { 37, 11, 299 }, { 37, 12, 642 }, { 37, 13, 588 }, { 37, 14, 499 },
            { 37, 15, 778 }, { 37, 16, 389 }, { 37, 17, 784 }, { 37, 18, 759 }, { 37, 19, 368 },
            { 37, 20, 245 }, { 37, 21, 210 }, { 37, 22, 864 }, { 37, 23, 861 }, { 37, 24, 944 },
            { 37, 25, 887 }, { 37, 26, 255 }, { 37, 27, 871 }, { 37, 28, 636 }, { 37, 29, 391 },
            { 37, 30, 83 }, { 37, 31, 250 }, { 37, 32, 970 }, { 37, 33, 179 }, { 37, 34, 656 },
            { 37, 35, 777 }, { 37, 36, 307 }, { 38, 0, 915 }, { 38, 1, 259 }, { 38, 2, 119 },
            { 38, 3, 680 }, { 38, 4, 172 }, { 38, 5, 452 }, { 38, 6, 983 }, { 38, 7, 614 },
            { 38, 8, 883 }, { 38, 9, 252 }, { 38, 10, 460 }, { 38, 11, 477 }, { 38, 12, 905 },
            { 38, 13, 894 }, { 38, 14, 406 }, { 38, 15, 337 }, { 38, 16, 514 }, { 38, 17, 90 },
            { 38, 18, 258 }, { 38, 19, 385 }, { 38, 20, 883 }, { 38, 21, 380 }, { 38, 22, 548 },
            { 38, 23, 557 }, { 38, 24, 872 }, { 38, 25, 731 }, { 38, 26, 476 }, { 38, 27, 941 },
            { 38, 28, 391 }, { 38, 29, 238 }, { 38, 30, 746 }, { 38, 31, 845 }, { 38, 32, 772 },
            { 38, 33, 689 }, { 38, 34, 975 }, { 38, 35, 828 }, { 38, 36, 82 }, { 38, 37, 253 },
            { 39, 0, 889 }, { 39, 1, 284 }, { 39, 2, 419 }, { 39, 3, 69 }, { 39, 4, 459 },
            { 39, 5, 132 }, { 39, 6, 28 }, { 39, 7, 452 }, { 39, 8, 79 }, { 39, 9, 528 },
            { 39, 10, 120 }, { 39, 11, 581 }, { 39, 12, 649 }, { 39, 13, 346 }, { 39, 14, 512 },
            { 39, 15, 458 }, { 39, 16, 689 }, { 39, 17, 282 }, { 39, 18, 99 }, { 39, 19, 238 },
            { 39, 20, 924 }, { 39, 21, 377 }, { 39, 22, 664 }, { 39, 23, 326 }, { 39, 24, 104 },
            { 39, 25, 787 }, { 39, 26, 589 }, { 39, 27, 481 }, { 39, 28, 378 }, { 39, 29, 755 },
            { 39, 30, 18 }, { 39, 31, 425 }, { 39, 32, 318 }, { 39, 33, 496 }, { 39, 34, 431 },
            { 39, 35, 410 }, { 39, 36, 818 }, { 39, 37, 173 }, { 39, 38, 543 }, { 40, 0, 719 },
            { 40, 1, 24 }, { 40, 2, 631 }, { 40, 3, 216 }, { 40, 4, 636 }, { 40, 5, 318 },
            { 40, 6, 343 }, { 40, 7, 888 }, { 40, 8, 229 }, { 40, 9, 619 }, { 40, 10, 79 },
            { 40, 11, 998 }, { 40, 12, 142 }, { 40, 13, 496 }, { 40, 14, 314 }, { 40, 15, 233 },
            { 40, 16, 364 }, { 40, 17, 584 }, { 40, 18, 891 }, { 40, 19, 353 }, { 40, 20, 528 },
            { 40, 21, 773 }, { 40, 22, 247 }, { 40, 23, 312 }, { 40, 24, 804 }, { 40, 25, 345 },
            { 40, 26, 775 }, { 40, 27, 381 }, { 40, 28, 518 }, { 40, 29, 7 }, { 40, 30, 462 },
            { 40, 31, 149 }, { 40, 32, 873 }, { 40, 33, 124 }, { 40, 34, 833 }, { 40, 35, 878 },
            { 40, 36, 515 }, { 40, 37, 532 }, { 40, 38, 244 }, { 40, 39, 73 }, { 41, 0, 690 },
            { 41, 1, 261 }, { 41, 2, 457 }, { 41, 3, 379 }, { 41, 4, 15 }, { 41, 5, 538 },
            { 41, 6, 427 }, { 41, 7, 689 }, { 41, 8, 380 }, { 41, 9, 93 }, { 41, 10, 57 },
            { 41, 11, 573 }, { 41, 12, 705 }, { 41, 13, 877 }, { 41, 14, 549 }, { 41, 15, 721 },
            { 41, 16, 418 }, { 41, 17, 575 }, { 41, 18, 796 }, { 41, 19, 975 }, { 41, 20, 573 },
            { 41, 21, 948 }, { 41, 22, 305 }, { 41, 23, 322 }, { 41, 24, 864 }, { 41, 25, 903 },
            { 41, 26, 210 }, { 41, 27, 261 }, { 41, 28, 919 }, { 41, 29, 9 }, { 41, 30, 372 },
            { 41, 31, 118 }, { 41, 32, 115 }, { 41, 33, 611 }, { 41, 34, 883 }, { 41, 35, 231 },
            { 41, 36, 646 }, { 41, 37, 247 }, { 41, 38, 753 }, { 41, 39, 905 }, { 41, 40, 698 },
            { 42, 0, 708 }, { 42, 1, 111 }, { 42, 2, 548 }, { 42, 3, 825 }, { 42, 4, 740 },
            { 42, 5, 41 }, { 42, 6, 352 }, { 42, 7, 479 }, { 42, 8, 555 }, { 42, 9, 460 },
            { 42, 10, 1 }, { 42, 11, 884 }, { 42, 12, 310 }, { 42, 13, 539 }, { 42, 14, 411 },
            { 42, 15, 175 }, { 42, 16, 526 }, { 42, 17, 719 }, { 42, 18, 234 }, { 42, 19, 444 },
            { 42, 20, 336 }, { 42, 21, 629 }, { 42, 22, 859 }, { 42, 23, 717 }, { 42, 24, 634 },
            { 42, 25, 553 }, { 42, 26, 557 }, { 42, 27, 934 }, { 42, 28, 955 }, { 42, 29, 656 },
            { 42, 30, 398 }, { 42, 31, 442 }, { 42, 32, 16 }, { 42, 33, 875 }, { 42, 34, 939 },
            { 42, 35, 812 }, { 42, 36, 631 }, { 42, 37, 551 }, { 42, 38, 824 }, { 42, 39, 936 },
            { 42, 40, 602 }, { 42, 41, 273 }, { 43, 0, 920 }, { 43, 1, 183 }, { 43, 2, 1 },
            { 43, 3, 656 }, { 43, 4, 2 }, { 43, 5, 353 }, { 43, 6, 876 }, { 43, 7, 419 },
            { 43, 8, 272 }, { 43, 9, 117 }, { 43, 10, 404 }, { 43, 11, 929 }, { 43, 12, 669 },
            { 43, 13, 180 }, { 43, 14, 277 }, { 43, 15, 87 }, { 43, 16, 522 }, { 43, 17, 748 },
            { 43, 18, 819 }, { 43, 19, 387 }, { 43, 20, 285 }, { 43, 21, 376 }, { 43, 22, 861 },
            { 43, 23, 888 }, { 43, 24, 158 }, { 43, 25, 53 }, { 43, 26, 482 }, { 43, 27, 27 },
            { 43, 28, 543 }, { 43, 29, 319 }, { 43, 30, 414 }, { 43, 31, 465 }, { 43, 32, 616 },
            { 43, 33, 553 }, { 43, 34, 345 }, { 43, 35, 491 }, { 43, 36, 277 }, { 43, 37, 196 },
            { 43, 38, 893 }, { 43, 39, 290 }, { 43, 40, 572 }, { 43, 41, 967 }, { 43, 42, 747 },
            { 44, 0, 141 }, { 44, 1, 638 }, { 44, 2, 532 }, { 44, 3, 968 }, { 44, 4, 90 },
            { 44, 5, 738 }, { 44, 6, 196 }, { 44, 7, 633 }, { 44, 8, 600 }, { 44, 9, 304 },
            { 44, 10, 128 }, { 44, 11, 792 }, { 44, 12, 586 }, { 44, 13, 1 }, { 44, 14, 344 },
            { 44, 15, 857 }, { 44, 16, 896 }, { 44, 17, 848 }, { 44, 18, 985 }, { 44, 19, 902 },
            { 44, 20, 632 }, { 44, 21, 21 }, { 44, 22, 447 }, { 44, 23, 338 }, { 44, 24, 722 },
            { 44, 25, 278 }, { 44, 26, 355 }, { 44, 27, 582 }, { 44, 28, 872 }, { 44, 29, 722 },
            { 44, 30, 21 }, { 44, 31, 231 }, { 44, 32, 156 }, { 44, 33, 980 }, { 44, 34, 250 },
            { 44, 35, 472 }, { 44, 36, 172 }, { 44, 37, 448 }, { 44, 38, 856 }, { 44, 39, 177 },
            { 44, 40, 41 }, { 44, 41, 29 }, { 44, 42, 623 }, { 44, 43, 20 }, { 45, 0, 806 },
            { 45, 1, 991 }, { 45, 2, 234 }, { 45, 3, 191 }, { 45, 4, 195 }, { 45, 5, 336 },
            { 45, 6, 595 }, { 45, 7, 699 }, { 45, 8, 464 }, { 45, 9, 490 }, { 45, 10, 292 },
            { 45, 11, 176 }, { 45, 12, 511 }, { 45, 13, 227 }, { 45, 14, 585 }, { 45, 15, 275 },
            { 45, 16, 610 }, { 45, 17, 814 }, { 45, 18, 932 }, { 45, 19, 661 }, { 45, 20, 644 },
            { 45, 21, 82 }, { 45, 22, 992 }, { 45, 23, 985 }, { 45, 24, 140 }, { 45, 25, 377 },
            { 45, 26, 319 }, { 45, 27, 137 }, { 45, 28, 147 }, { 45, 29, 447 }, { 45, 30, 23 },
            { 45, 31, 382 }, { 45, 32, 607 }, { 45, 33, 429 }, { 45, 34, 903 }, { 45, 35, 962 },
            { 45, 36, 624 }, { 45, 37, 593 }, { 45, 38, 207 }, { 45, 39, 363 }, { 45, 40, 87 },
            { 45, 41, 644 }, { 45, 42, 61 }, { 45, 43, 159 }, { 45, 44, 967 }, { 46, 0, 991 },
            { 46, 1, 772 }, { 46, 2, 735 }, { 46, 3, 147 }, { 46, 4, 355 }, { 46, 5, 327 },
            { 46, 6, 89 }, { 46, 7, 103 }, { 46, 8, 484 }, { 46, 9, 924 }, { 46, 10, 886 },
            { 46, 11, 155 }, { 46, 12, 657 }, { 46, 13, 435 }, { 46, 14, 508 }, { 46, 15, 986 },
            { 46, 16, 658 }, { 46, 17, 941 }, { 46, 18, 641 }, { 46, 19, 777 }, { 46, 20, 390 },
            { 46, 21, 442 }, { 46, 22, 845 }, { 46, 23, 834 }, { 46, 24, 216 }, { 46, 25, 986 },
            { 46, 26, 370 }, { 46, 27, 160 }, { 46, 28, 774 }, { 46, 29, 429 }, { 46, 30, 275 },
            { 46, 31, 453 }, { 46, 32, 246 }, { 46, 33, 311 }, { 46, 34, 702 }, { 46, 35, 496 },
            { 46, 36, 397 }, { 46, 37, 307 }, { 46, 38, 679 }, { 46, 39, 842 }, { 46, 40, 934 },
            { 46, 41, 368 }, { 46, 42, 323 }, { 46, 43, 607 }, { 46, 44, 123 }, { 46, 45, 881 },
            { 47, 0, 990 }, { 47, 1, 337 }, { 47, 2, 650 }, { 47, 3, 973 }, { 47, 4, 665 },
            { 47, 5, 248 }, { 47, 6, 895 }, { 47, 7, 911 }, { 47, 8, 379 }, { 47, 9, 775 },
            { 47, 10, 242 }, { 47, 11, 801 }, { 47, 12, 323 }, { 47, 13, 960 }, { 47, 14, 605 },
            { 47, 15, 81 }, { 47, 16, 455 }, { 47, 17, 119 }, { 47, 18, 21 }, { 47, 19, 438 },
            { 47, 20, 164 }, { 47, 21, 507 }, { 47, 22, 450 }, { 47, 23, 555 }, { 47, 24, 635 },
            { 47, 25, 257 }, { 47, 26, 700 }, { 47, 27, 641 }, { 47, 28, 676 }, { 47, 29, 195 },
            { 47, 30, 692 }, { 47, 31, 140 }, { 47, 32, 386 }, { 47, 33, 662 }, { 47, 34, 556 },
            { 47, 35, 220 }, { 47, 36, 595 }, { 47, 37, 193 }, { 47, 38, 620 }, { 47, 39, 963 },
            { 47, 40, 330 }, { 47, 41, 81 }, { 47, 42, 154 }, { 47, 43, 966 }, { 47, 44, 674 },
            { 47, 45, 337 }, { 47, 46, 621 }, { 48, 0, 829 }, { 48, 1, 457 }, { 48, 2, 407 },
            { 48, 3, 350 }, { 48, 4, 732 }, { 48, 5, 406 }, { 48, 6, 106 }, { 48, 7, 548 },
            { 48, 8, 484 }, { 48, 9, 273 }, { 48, 10, 383 }, { 48, 11, 91 }, { 48, 12, 572 },
            { 48, 13, 870 }, { 48, 14, 235 }, { 48, 15, 863 }, { 48, 16, 328 }, { 48, 17, 6 },
            { 48, 18, 835 }, { 48, 19, 267 }, { 48, 20, 4 }, { 48, 21, 418 }, { 48, 22, 31 },
            { 48, 23, 664 }, { 48, 24, 482 }, { 48, 25, 380 }, { 48, 26, 525 }, { 48, 27, 252 },
            { 48, 28, 831 }, { 48, 29, 356 }, { 48, 30, 121 }, { 48, 31, 505 }, { 48, 32, 4 },
            { 48, 33, 171 }, { 48, 34, 602 }, { 48, 35, 23 }, { 48, 36, 935 }, { 48, 37, 856 },
            { 48, 38, 824 }, { 48, 39, 184 }, { 48, 40, 514 }, { 48, 41, 621 }, { 48, 42, 698 },
            { 48, 43, 821 }, { 48, 44, 307 }, { 48, 45, 789 }, { 48, 46, 132 }, { 48, 47, 434 },
            { 49, 0, 147 }, { 49, 1, 52 }, { 49, 2, 493 }, { 49, 3, 178 }, { 49, 4, 635 },
            { 49, 5, 299 }, { 49, 6, 526 }, { 49, 7, 680 }, { 49, 8, 383 }, { 49, 9, 878 },
            { 49, 10, 806 }, { 49, 11, 60 }, { 49, 12, 614 }, { 49, 13, 220 }, { 49, 14, 467 },
            { 49, 15, 447 }, { 49, 16, 130 }, { 49, 17, 266 }, { 49, 18, 412 }, { 49, 19, 251 },
            { 49, 20, 106 }, { 49, 21, 357 }, { 49, 22, 858 }, { 49, 23, 729 }, { 49, 24, 975 },
            { 49, 25, 106 }, { 49, 26, 356 }, { 49, 27, 112 }, { 49, 28, 588 }, { 49, 29, 243 },
            { 49, 30, 361 }, { 49, 31, 151 }, { 49, 32, 190 }, { 49, 33, 849 }, { 49, 34, 607 },
            { 49, 35, 719 }, { 49, 36, 435 }, { 49, 37, 961 }, { 49, 38, 739 }, { 49, 39, 408 },
            { 49, 40, 951 }, { 49, 41, 28 }, { 49, 42, 346 }, { 49, 43, 335 }, { 49, 44, 681 },
            { 49, 45, 38 }, { 49, 46, 172 }, { 49, 47, 144 }, { 49, 48, 164 } };
        double maxWeight = 24192;
        double minWeight = 933;
        test(edges, objectiveSense == MAXIMIZE ? maxWeight : minWeight, objectiveSense);
    }

    /**
     * Test on a small pseudograph
     */
    @Test
    public void testGetMatching39()
    {
        int[][] edges = new int[][] { { 1, 1, 1 }, { 1, 2, 5 }, { 1, 2, 10 }, };
        double maxWeight = 10;
        double minWeight = 5;
        test(
            new WeightedPseudograph<>(DefaultEdge.class), edges,
            objectiveSense == MAXIMIZE ? maxWeight : minWeight, objectiveSense);
    }

    /**
     * Test on a pseudograph
     */
    @Test
    public void testGetMatching40()
    {
        int[][] edges = new int[][] { { 1, 1, 1 }, { 2, 2, 1 }, { 3, 3, 1 }, { 4, 4, 1 },
            { 1, 2, 5 }, { 1, 2, 10 }, { 1, 3, 2 }, { 1, 3, 5 }, { 1, 4, 4 }, { 1, 4, 6 },
            { 2, 3, 3 }, { 2, 3, 4 }, { 2, 4, 6 }, { 2, 4, 8 }, { 3, 4, 1 }, { 3, 4, 3 } };
        double maxWeight = 13;
        double minWeight = 6;
        test(
            new WeightedPseudograph<>(DefaultEdge.class), edges,
            objectiveSense == MAXIMIZE ? maxWeight : minWeight, objectiveSense);
    }

    /**
     * Test on a graph with odd number of vertices
     */
    @Test(expected = IllegalArgumentException.class)
    public void testGetMatching41()
    {
        Graph<Integer, DefaultWeightedEdge> graph =
            new DefaultUndirectedWeightedGraph<>(DefaultWeightedEdge.class);
        graph.addVertex(1);

        KolmogorovWeightedPerfectMatching<Integer, DefaultWeightedEdge> matching =
            new KolmogorovWeightedPerfectMatching<>(graph, options);
        matching.getMatching();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetMatching42()
    {
        Graph<Integer, DefaultWeightedEdge> graph =
            new DefaultUndirectedWeightedGraph<>(DefaultWeightedEdge.class);
        graph.addVertex(1);
        graph.addVertex(2);

        KolmogorovWeightedPerfectMatching<Integer, DefaultWeightedEdge> perfectMatching =
            new KolmogorovWeightedPerfectMatching<>(graph, options);
        perfectMatching.getMatching();
    }

    /**
     * Test on a $K_{3}$ with a zero-degree vertex
     */
    @Test(expected = IllegalArgumentException.class)
    public void testGetMatching43()
    {
        int[][] edges = { { 1, 2, 1 }, { 1, 3, 2 }, { 2, 3, 3 }, };
        Graph<Integer, DefaultEdge> graph = TestUtil.createUndirected(edges);
        graph.addVertex(4);

        KolmogorovWeightedPerfectMatching<Integer, DefaultEdge> perfectMatching =
            new KolmogorovWeightedPerfectMatching<>(graph, options);
        perfectMatching.getMatching();
    }

    /**
     * Test on triangulation of 9 points with a zero-degree vertex
     */
    @Test(expected = IllegalArgumentException.class)
    public void testGetMatching44()
    {
        int[][] edges = new int[][] { { 1, 2, 3 }, { 1, 3, 5 }, { 2, 3, 7 }, { 1, 4, 6 },
            { 3, 4, 1 }, { 1, 0, 11 }, { 0, 4, 7 }, { 3, 5, 3 }, { 4, 5, 2 }, { 4, 6, 3 },
            { 5, 6, 1 }, { 4, 7, 3 }, { 0, 7, 7 }, { 6, 7, 1 }, { 6, 8, 9 }, { 5, 8, 8 },
            { 7, 8, 10 }, { 3, 8, 7 }, { 2, 8, 4 } };
        Graph<Integer, DefaultEdge> graph = TestUtil.createUndirected(edges);
        graph.addVertex(9);

        KolmogorovWeightedPerfectMatching<Integer, DefaultEdge> perfectMatching =
            new KolmogorovWeightedPerfectMatching<>(graph, options);
        perfectMatching.getMatching();
    }

    /**
     * Test on triangulation of 9 points with a zero-degree vertex
     */
    @Test(expected = IllegalArgumentException.class)
    public void testGetMatching45()
    {
        int[][] edges = new int[][] { { 0, 1, 8 }, { 0, 2, 9 }, { 1, 2, 1 }, { 0, 3, 4 },
            { 1, 3, 5 }, { 0, 4, 4 }, { 3, 4, 2 }, { 0, 5, 5 }, { 4, 5, 2 }, { 4, 6, 7 },
            { 5, 6, 6 }, { 1, 7, 9 }, { 2, 7, 9 }, { 3, 7, 8 }, { 4, 7, 7 }, { 6, 7, 2 },
            { 6, 8, 4 }, { 7, 8, 2 }, { 2, 8, 10 } };
        Graph<Integer, DefaultEdge> graph = TestUtil.createUndirected(edges);
        graph.addVertex(9);

        KolmogorovWeightedPerfectMatching<Integer, DefaultEdge> perfectMatching =
            new KolmogorovWeightedPerfectMatching<>(graph, options);
        perfectMatching.getMatching();
    }

    /**
     * Test on triangulation of 99 points with a zero-degree vertex
     */
    @Test(expected = IllegalArgumentException.class)
    public void testGetMatching46()
    {
        int[][] edges = new int[][] { { 1, 2, 22 }, { 1, 3, 8 }, { 2, 3, 16 }, { 0, 1, 46 },
            { 0, 5, 24 }, { 1, 5, 24 }, { 0, 4, 20 }, { 4, 5, 4 }, { 0, 6, 5 }, { 0, 7, 7 },
            { 6, 7, 3 }, { 0, 8, 19 }, { 6, 8, 16 }, { 0, 9, 17 }, { 4, 9, 8 }, { 0, 10, 10 },
            { 7, 10, 6 }, { 9, 10, 11 }, { 4, 11, 10 }, { 5, 11, 7 }, { 9, 11, 13 }, { 5, 12, 16 },
            { 1, 12, 13 }, { 11, 12, 11 }, { 1, 13, 10 }, { 3, 13, 9 }, { 12, 13, 12 },
            { 11, 14, 8 }, { 12, 14, 5 }, { 9, 15, 6 }, { 10, 15, 7 }, { 7, 16, 14 },
            { 10, 16, 17 }, { 6, 16, 14 }, { 8, 16, 8 }, { 11, 17, 5 }, { 14, 17, 6 },
            { 10, 18, 6 }, { 15, 18, 4 }, { 16, 18, 20 }, { 11, 19, 5 }, { 9, 19, 14 },
            { 17, 19, 3 }, { 15, 20, 4 }, { 18, 20, 3 }, { 3, 21, 16 }, { 13, 21, 12 },
            { 2, 21, 19 }, { 16, 22, 5 }, { 8, 22, 11 }, { 15, 23, 6 }, { 20, 23, 4 },
            { 13, 24, 9 }, { 21, 24, 10 }, { 12, 24, 15 }, { 16, 25, 20 }, { 18, 25, 5 },
            { 22, 25, 22 }, { 20, 25, 5 }, { 23, 25, 4 }, { 9, 26, 13 }, { 19, 26, 11 },
            { 15, 26, 11 }, { 23, 26, 8 }, { 23, 27, 4 }, { 25, 27, 5 }, { 26, 27, 7 },
            { 17, 28, 17 }, { 19, 28, 19 }, { 14, 28, 15 }, { 12, 28, 14 }, { 24, 28, 7 },
            { 8, 29, 21 }, { 22, 29, 15 }, { 25, 30, 7 }, { 27, 30, 5 }, { 26, 30, 9 },
            { 24, 31, 10 }, { 28, 31, 6 }, { 25, 33, 28 }, { 22, 33, 12 }, { 30, 33, 30 },
            { 29, 33, 8 }, { 29, 32, 4 }, { 32, 33, 4 }, { 33, 34, 4 }, { 32, 34, 3 },
            { 29, 34, 6 }, { 33, 35, 4 }, { 34, 35, 2 }, { 26, 36, 10 }, { 30, 36, 9 },
            { 19, 36, 19 }, { 24, 37, 13 }, { 31, 37, 10 }, { 21, 37, 15 }, { 19, 38, 23 },
            { 36, 38, 24 }, { 28, 38, 12 }, { 31, 38, 8 }, { 31, 39, 9 }, { 37, 39, 11 },
            { 38, 39, 4 }, { 37, 40, 5 }, { 39, 40, 9 }, { 33, 41, 11 }, { 35, 41, 9 },
            { 38, 42, 20 }, { 36, 42, 11 }, { 39, 43, 9 }, { 40, 43, 9 }, { 40, 45, 9 },
            { 43, 45, 7 }, { 37, 45, 13 }, { 34, 46, 15 }, { 29, 46, 18 }, { 35, 46, 14 },
            { 41, 46, 11 }, { 41, 44, 8 }, { 41, 47, 12 }, { 44, 47, 6 }, { 33, 47, 20 },
            { 30, 47, 25 }, { 42, 48, 6 }, { 42, 49, 6 }, { 48, 49, 3 }, { 38, 49, 20 },
            { 41, 50, 11 }, { 44, 50, 13 }, { 46, 50, 3 }, { 48, 51, 4 }, { 49, 51, 1 },
            { 44, 53, 4 }, { 47, 53, 6 }, { 50, 53, 13 }, { 47, 54, 8 }, { 53, 54, 12 },
            { 30, 54, 24 }, { 42, 55, 21 }, { 36, 55, 23 }, { 48, 55, 19 }, { 30, 55, 24 },
            { 54, 55, 4 }, { 45, 56, 9 }, { 43, 56, 7 }, { 45, 52, 4 }, { 52, 56, 9 },
            { 21, 57, 35 }, { 2, 57, 48 }, { 45, 57, 19 }, { 52, 57, 18 }, { 37, 57, 26 },
            { 49, 58, 11 }, { 51, 58, 11 }, { 38, 58, 20 }, { 39, 58, 19 }, { 43, 58, 18 },
            { 56, 58, 15 }, { 52, 59, 8 }, { 56, 59, 4 }, { 53, 60, 14 }, { 50, 60, 8 },
            { 46, 60, 11 }, { 51, 61, 11 }, { 58, 61, 6 }, { 52, 62, 10 }, { 59, 62, 9 },
            { 59, 63, 6 }, { 62, 63, 7 }, { 52, 64, 14 }, { 62, 64, 7 }, { 52, 65, 14 },
            { 64, 65, 1 }, { 57, 65, 13 }, { 59, 66, 9 }, { 63, 66, 6 }, { 56, 66, 11 },
            { 58, 66, 14 }, { 48, 67, 19 }, { 55, 67, 21 }, { 51, 67, 19 }, { 61, 67, 15 },
            { 58, 68, 14 }, { 61, 68, 12 }, { 66, 68, 11 }, { 64, 69, 9 }, { 65, 69, 9 },
            { 62, 69, 11 }, { 63, 69, 14 }, { 53, 70, 22 }, { 60, 70, 24 }, { 55, 70, 21 },
            { 67, 70, 23 }, { 54, 70, 21 }, { 67, 71, 4 }, { 70, 71, 22 }, { 67, 72, 10 },
            { 71, 72, 9 }, { 61, 72, 14 }, { 68, 72, 12 }, { 63, 73, 14 }, { 69, 73, 5 },
            { 71, 74, 5 }, { 72, 74, 5 }, { 66, 75, 16 }, { 68, 75, 20 }, { 63, 75, 15 },
            { 73, 75, 3 }, { 69, 76, 6 }, { 73, 76, 7 }, { 65, 76, 13 }, { 57, 76, 24 },
            { 71, 77, 6 }, { 74, 77, 6 }, { 70, 78, 13 }, { 60, 78, 24 }, { 73, 79, 8 },
            { 75, 79, 6 }, { 76, 79, 9 }, { 70, 80, 10 }, { 78, 80, 11 }, { 70, 81, 19 },
            { 80, 81, 16 }, { 71, 81, 12 }, { 77, 81, 8 }, { 79, 82, 7 }, { 76, 82, 9 },
            { 80, 83, 5 }, { 81, 83, 13 }, { 80, 84, 8 }, { 83, 84, 4 }, { 81, 84, 14 },
            { 76, 85, 14 }, { 82, 85, 8 }, { 72, 86, 19 }, { 74, 86, 20 }, { 68, 86, 21 },
            { 77, 86, 23 }, { 81, 86, 25 }, { 82, 87, 7 }, { 85, 87, 4 }, { 80, 88, 12 },
            { 78, 88, 11 }, { 75, 89, 18 }, { 79, 89, 15 }, { 68, 89, 24 }, { 86, 89, 12 },
            { 80, 90, 11 }, { 84, 90, 8 }, { 88, 90, 4 }, { 76, 91, 19 }, { 57, 91, 38 },
            { 85, 91, 6 }, { 88, 92, 2 }, { 90, 92, 4 }, { 78, 92, 13 }, { 60, 92, 36 },
            { 79, 93, 14 }, { 89, 93, 15 }, { 82, 93, 11 }, { 87, 93, 6 }, { 87, 94, 8 },
            { 93, 94, 10 }, { 85, 94, 6 }, { 91, 94, 3 }, { 89, 95, 13 }, { 93, 95, 10 },
            { 94, 95, 18 }, { 90, 96, 12 }, { 92, 96, 13 }, { 84, 96, 14 }, { 84, 97, 23 },
            { 81, 97, 23 }, { 96, 97, 18 }, { 81, 98, 26 }, { 86, 98, 19 }, { 97, 98, 9 },
            { 89, 98, 27 }, { 95, 98, 31 } };
        Graph<Integer, DefaultEdge> graph = TestUtil.createUndirected(edges);
        graph.addVertex(99);

        KolmogorovWeightedPerfectMatching<Integer, DefaultEdge> perfectMatching =
            new KolmogorovWeightedPerfectMatching<>(graph, options);
        perfectMatching.getMatching();

    }

    /**
     * Test on a 3-regular graph with no perfect matching
     */
    @Test(expected = IllegalArgumentException.class)
    public void testGetMatching47()
    {
        int[][] edges = new int[][] { { 0, 2, 18 }, { 1, 2, 27 }, { 0, 3, 68 }, { 1, 3, 15 },
            { 2, 3, 19 }, { 0, 12, 93 }, { 1, 12, 13 }, { 12, 15, 85 }, { 4, 6, 50 }, { 5, 6, 6 },
            { 4, 7, 79 }, { 5, 7, 95 }, { 6, 7, 95 }, { 4, 13, 40 }, { 5, 13, 6 }, { 13, 15, 87 },
            { 8, 10, 51 }, { 9, 10, 44 }, { 8, 11, 96 }, { 9, 11, 95 }, { 10, 11, 9 },
            { 8, 14, 86 }, { 9, 14, 56 }, { 14, 15, 36 } };
        Graph<Integer, DefaultEdge> graph = TestUtil.createUndirected(edges);
        KolmogorovWeightedPerfectMatching<Integer, DefaultEdge> perfectMatching =
            new KolmogorovWeightedPerfectMatching<>(graph, options);
        perfectMatching.getMatching();
    }

    /**
     * Test on a 7-regular graph without perfect matching: |V| = 64, |max. cardinality matching| =
     * 29
     */
    @Test(expected = IllegalArgumentException.class)
    public void testGetMatching48()
    {
        int[][] edges = new int[][] { { 0, 2, 72 }, { 1, 2, 85 }, { 0, 3, 5 }, { 1, 3, 59 },
            { 0, 4, 96 }, { 1, 4, 38 }, { 2, 4, 94 }, { 3, 4, 17 }, { 0, 5, 54 }, { 1, 5, 49 },
            { 2, 5, 81 }, { 3, 5, 56 }, { 0, 6, 69 }, { 1, 6, 82 }, { 2, 6, 18 }, { 3, 6, 53 },
            { 4, 6, 25 }, { 5, 6, 9 }, { 0, 7, 24 }, { 1, 7, 70 }, { 2, 7, 72 }, { 3, 7, 19 },
            { 4, 7, 16 }, { 5, 7, 61 }, { 6, 7, 92 }, { 0, 56, 0 }, { 1, 56, 77 }, { 2, 56, 11 },
            { 3, 56, 20 }, { 4, 56, 23 }, { 5, 56, 95 }, { 56, 63, 56 }, { 8, 10, 24 },
            { 9, 10, 21 }, { 8, 11, 87 }, { 9, 11, 76 }, { 8, 12, 24 }, { 9, 12, 43 },
            { 10, 12, 81 }, { 11, 12, 79 }, { 8, 13, 5 }, { 9, 13, 15 }, { 10, 13, 29 },
            { 11, 13, 83 }, { 8, 14, 53 }, { 9, 14, 66 }, { 10, 14, 8 }, { 11, 14, 52 },
            { 12, 14, 73 }, { 13, 14, 61 }, { 8, 15, 83 }, { 9, 15, 78 }, { 10, 15, 9 },
            { 11, 15, 73 }, { 12, 15, 27 }, { 13, 15, 69 }, { 14, 15, 24 }, { 8, 57, 11 },
            { 9, 57, 42 }, { 10, 57, 57 }, { 11, 57, 62 }, { 12, 57, 47 }, { 13, 57, 83 },
            { 57, 63, 93 }, { 16, 18, 36 }, { 17, 18, 59 }, { 16, 19, 71 }, { 17, 19, 48 },
            { 16, 20, 88 }, { 17, 20, 66 }, { 18, 20, 43 }, { 19, 20, 84 }, { 16, 21, 63 },
            { 17, 21, 87 }, { 18, 21, 53 }, { 19, 21, 79 }, { 16, 22, 65 }, { 17, 22, 39 },
            { 18, 22, 23 }, { 19, 22, 12 }, { 20, 22, 11 }, { 21, 22, 43 }, { 16, 23, 64 },
            { 17, 23, 94 }, { 18, 23, 25 }, { 19, 23, 89 }, { 20, 23, 24 }, { 21, 23, 50 },
            { 22, 23, 54 }, { 16, 58, 66 }, { 17, 58, 11 }, { 18, 58, 81 }, { 19, 58, 0 },
            { 20, 58, 8 }, { 21, 58, 12 }, { 58, 63, 74 }, { 24, 26, 32 }, { 25, 26, 47 },
            { 24, 27, 86 }, { 25, 27, 64 }, { 24, 28, 71 }, { 25, 28, 49 }, { 26, 28, 87 },
            { 27, 28, 94 }, { 24, 29, 84 }, { 25, 29, 71 }, { 26, 29, 52 }, { 27, 29, 92 },
            { 24, 30, 98 }, { 25, 30, 86 }, { 26, 30, 70 }, { 27, 30, 47 }, { 28, 30, 56 },
            { 29, 30, 30 }, { 24, 31, 2 }, { 25, 31, 18 }, { 26, 31, 9 }, { 27, 31, 26 },
            { 28, 31, 81 }, { 29, 31, 98 }, { 30, 31, 9 }, { 24, 59, 95 }, { 25, 59, 82 },
            { 26, 59, 94 }, { 27, 59, 88 }, { 28, 59, 74 }, { 29, 59, 91 }, { 59, 63, 77 },
            { 32, 34, 27 }, { 33, 34, 21 }, { 32, 35, 49 }, { 33, 35, 6 }, { 32, 36, 80 },
            { 33, 36, 89 }, { 34, 36, 78 }, { 35, 36, 0 }, { 32, 37, 92 }, { 33, 37, 60 },
            { 34, 37, 2 }, { 35, 37, 51 }, { 32, 38, 36 }, { 33, 38, 74 }, { 34, 38, 47 },
            { 35, 38, 14 }, { 36, 38, 54 }, { 37, 38, 6 }, { 32, 39, 29 }, { 33, 39, 26 },
            { 34, 39, 95 }, { 35, 39, 17 }, { 36, 39, 26 }, { 37, 39, 20 }, { 38, 39, 67 },
            { 32, 60, 66 }, { 33, 60, 38 }, { 34, 60, 10 }, { 35, 60, 82 }, { 36, 60, 92 },
            { 37, 60, 98 }, { 60, 63, 7 }, { 40, 42, 75 }, { 41, 42, 90 }, { 40, 43, 77 },
            { 41, 43, 3 }, { 40, 44, 97 }, { 41, 44, 6 }, { 42, 44, 32 }, { 43, 44, 6 },
            { 40, 45, 46 }, { 41, 45, 36 }, { 42, 45, 67 }, { 43, 45, 88 }, { 40, 46, 59 },
            { 41, 46, 88 }, { 42, 46, 29 }, { 43, 46, 79 }, { 44, 46, 65 }, { 45, 46, 22 },
            { 40, 47, 62 }, { 41, 47, 96 }, { 42, 47, 4 }, { 43, 47, 71 }, { 44, 47, 22 },
            { 45, 47, 97 }, { 46, 47, 28 }, { 40, 61, 71 }, { 41, 61, 35 }, { 42, 61, 33 },
            { 43, 61, 63 }, { 44, 61, 22 }, { 45, 61, 25 }, { 61, 63, 5 }, { 48, 50, 97 },
            { 49, 50, 73 }, { 48, 51, 41 }, { 49, 51, 92 }, { 48, 52, 36 }, { 49, 52, 47 },
            { 50, 52, 72 }, { 51, 52, 38 }, { 48, 53, 2 }, { 49, 53, 91 }, { 50, 53, 83 },
            { 51, 53, 19 }, { 48, 54, 0 }, { 49, 54, 75 }, { 50, 54, 44 }, { 51, 54, 4 },
            { 52, 54, 73 }, { 53, 54, 17 }, { 48, 55, 54 }, { 49, 55, 70 }, { 50, 55, 84 },
            { 51, 55, 38 }, { 52, 55, 70 }, { 53, 55, 80 }, { 54, 55, 73 }, { 48, 62, 73 },
            { 49, 62, 24 }, { 50, 62, 86 }, { 51, 62, 65 }, { 52, 62, 10 }, { 53, 62, 95 },
            { 62, 63, 45 } };
        Graph<Integer, DefaultEdge> graph = TestUtil.createUndirected(edges);

        KolmogorovWeightedPerfectMatching<Integer, DefaultEdge> perfectMatching =
            new KolmogorovWeightedPerfectMatching<>(graph, options);
        perfectMatching.getMatching();
    }

    /**
     * Test on a 9-regular graph without perfect matching: |V| = 100, |max. cardinality matching| =
     * 46
     */
    @Test(expected = IllegalArgumentException.class)
    public void testGetMatching49()
    {
        int[][] edges = new int[][] { { 0, 2, 87 }, { 1, 2, 56 }, { 0, 3, 21 }, { 1, 3, 81 },
            { 0, 4, 26 }, { 1, 4, 94 }, { 2, 4, 36 }, { 3, 4, 28 }, { 0, 5, 32 }, { 1, 5, 94 },
            { 2, 5, 12 }, { 3, 5, 86 }, { 0, 6, 52 }, { 1, 6, 78 }, { 2, 6, 93 }, { 3, 6, 89 },
            { 4, 6, 11 }, { 5, 6, 4 }, { 0, 7, 64 }, { 1, 7, 93 }, { 2, 7, 16 }, { 3, 7, 70 },
            { 4, 7, 14 }, { 5, 7, 21 }, { 0, 8, 64 }, { 1, 8, 65 }, { 2, 8, 3 }, { 3, 8, 52 },
            { 4, 8, 65 }, { 5, 8, 22 }, { 6, 8, 20 }, { 7, 8, 50 }, { 0, 9, 93 }, { 1, 9, 89 },
            { 2, 9, 58 }, { 3, 9, 12 }, { 4, 9, 96 }, { 5, 9, 87 }, { 6, 9, 57 }, { 7, 9, 49 },
            { 8, 9, 78 }, { 0, 90, 0 }, { 1, 90, 95 }, { 2, 90, 79 }, { 3, 90, 15 }, { 4, 90, 15 },
            { 5, 90, 78 }, { 6, 90, 81 }, { 7, 90, 64 }, { 90, 99, 72 }, { 10, 12, 50 },
            { 11, 12, 8 }, { 10, 13, 30 }, { 11, 13, 7 }, { 10, 14, 22 }, { 11, 14, 30 },
            { 12, 14, 3 }, { 13, 14, 2 }, { 10, 15, 56 }, { 11, 15, 52 }, { 12, 15, 6 },
            { 13, 15, 66 }, { 10, 16, 53 }, { 11, 16, 64 }, { 12, 16, 72 }, { 13, 16, 61 },
            { 14, 16, 90 }, { 15, 16, 57 }, { 10, 17, 79 }, { 11, 17, 41 }, { 12, 17, 33 },
            { 13, 17, 53 }, { 14, 17, 13 }, { 15, 17, 10 }, { 10, 18, 70 }, { 11, 18, 0 },
            { 12, 18, 30 }, { 13, 18, 67 }, { 14, 18, 13 }, { 15, 18, 16 }, { 16, 18, 10 },
            { 17, 18, 92 }, { 10, 19, 97 }, { 11, 19, 52 }, { 12, 19, 71 }, { 13, 19, 51 },
            { 14, 19, 92 }, { 15, 19, 28 }, { 16, 19, 96 }, { 17, 19, 21 }, { 18, 19, 82 },
            { 10, 91, 45 }, { 11, 91, 46 }, { 12, 91, 19 }, { 13, 91, 35 }, { 14, 91, 28 },
            { 15, 91, 95 }, { 16, 91, 20 }, { 17, 91, 92 }, { 91, 99, 10 }, { 20, 22, 55 },
            { 21, 22, 25 }, { 20, 23, 46 }, { 21, 23, 76 }, { 20, 24, 14 }, { 21, 24, 91 },
            { 22, 24, 31 }, { 23, 24, 49 }, { 20, 25, 30 }, { 21, 25, 77 }, { 22, 25, 22 },
            { 23, 25, 0 }, { 20, 26, 46 }, { 21, 26, 21 }, { 22, 26, 80 }, { 23, 26, 18 },
            { 24, 26, 68 }, { 25, 26, 40 }, { 20, 27, 32 }, { 21, 27, 43 }, { 22, 27, 74 },
            { 23, 27, 32 }, { 24, 27, 31 }, { 25, 27, 65 }, { 20, 28, 91 }, { 21, 28, 38 },
            { 22, 28, 77 }, { 23, 28, 80 }, { 24, 28, 69 }, { 25, 28, 88 }, { 26, 28, 41 },
            { 27, 28, 40 }, { 20, 29, 7 }, { 21, 29, 85 }, { 22, 29, 33 }, { 23, 29, 8 },
            { 24, 29, 47 }, { 25, 29, 90 }, { 26, 29, 78 }, { 27, 29, 49 }, { 28, 29, 34 },
            { 20, 92, 93 }, { 21, 92, 88 }, { 22, 92, 90 }, { 23, 92, 54 }, { 24, 92, 33 },
            { 25, 92, 4 }, { 26, 92, 75 }, { 27, 92, 13 }, { 92, 99, 30 }, { 30, 32, 30 },
            { 31, 32, 87 }, { 30, 33, 87 }, { 31, 33, 21 }, { 30, 34, 8 }, { 31, 34, 80 },
            { 32, 34, 72 }, { 33, 34, 94 }, { 30, 35, 17 }, { 31, 35, 50 }, { 32, 35, 12 },
            { 33, 35, 86 }, { 30, 36, 26 }, { 31, 36, 72 }, { 32, 36, 37 }, { 33, 36, 81 },
            { 34, 36, 39 }, { 35, 36, 38 }, { 30, 37, 85 }, { 31, 37, 38 }, { 32, 37, 60 },
            { 33, 37, 37 }, { 34, 37, 24 }, { 35, 37, 79 }, { 30, 38, 96 }, { 31, 38, 87 },
            { 32, 38, 29 }, { 33, 38, 90 }, { 34, 38, 97 }, { 35, 38, 46 }, { 36, 38, 59 },
            { 37, 38, 44 }, { 30, 39, 18 }, { 31, 39, 55 }, { 32, 39, 87 }, { 33, 39, 93 },
            { 34, 39, 86 }, { 35, 39, 69 }, { 36, 39, 96 }, { 37, 39, 15 }, { 38, 39, 34 },
            { 30, 93, 53 }, { 31, 93, 42 }, { 32, 93, 59 }, { 33, 93, 90 }, { 34, 93, 15 },
            { 35, 93, 79 }, { 36, 93, 86 }, { 37, 93, 18 }, { 93, 99, 56 }, { 40, 42, 37 },
            { 41, 42, 41 }, { 40, 43, 91 }, { 41, 43, 4 }, { 40, 44, 81 }, { 41, 44, 55 },
            { 42, 44, 82 }, { 43, 44, 53 }, { 40, 45, 83 }, { 41, 45, 12 }, { 42, 45, 19 },
            { 43, 45, 79 }, { 40, 46, 62 }, { 41, 46, 26 }, { 42, 46, 46 }, { 43, 46, 3 },
            { 44, 46, 63 }, { 45, 46, 28 }, { 40, 47, 50 }, { 41, 47, 63 }, { 42, 47, 23 },
            { 43, 47, 16 }, { 44, 47, 5 }, { 45, 47, 52 }, { 40, 48, 91 }, { 41, 48, 33 },
            { 42, 48, 3 }, { 43, 48, 55 }, { 44, 48, 86 }, { 45, 48, 99 }, { 46, 48, 67 },
            { 47, 48, 77 }, { 40, 49, 64 }, { 41, 49, 1 }, { 42, 49, 59 }, { 43, 49, 96 },
            { 44, 49, 4 }, { 45, 49, 3 }, { 46, 49, 22 }, { 47, 49, 77 }, { 48, 49, 36 },
            { 40, 94, 31 }, { 41, 94, 12 }, { 42, 94, 6 }, { 43, 94, 91 }, { 44, 94, 30 },
            { 45, 94, 58 }, { 46, 94, 69 }, { 47, 94, 66 }, { 94, 99, 63 }, { 50, 52, 8 },
            { 51, 52, 5 }, { 50, 53, 63 }, { 51, 53, 89 }, { 50, 54, 58 }, { 51, 54, 75 },
            { 52, 54, 91 }, { 53, 54, 9 }, { 50, 55, 7 }, { 51, 55, 3 }, { 52, 55, 65 },
            { 53, 55, 4 }, { 50, 56, 71 }, { 51, 56, 90 }, { 52, 56, 69 }, { 53, 56, 89 },
            { 54, 56, 60 }, { 55, 56, 15 }, { 50, 57, 29 }, { 51, 57, 26 }, { 52, 57, 0 },
            { 53, 57, 76 }, { 54, 57, 83 }, { 55, 57, 94 }, { 50, 58, 59 }, { 51, 58, 86 },
            { 52, 58, 61 }, { 53, 58, 95 }, { 54, 58, 58 }, { 55, 58, 50 }, { 56, 58, 52 },
            { 57, 58, 35 }, { 50, 59, 70 }, { 51, 59, 56 }, { 52, 59, 48 }, { 53, 59, 0 },
            { 54, 59, 51 }, { 55, 59, 35 }, { 56, 59, 95 }, { 57, 59, 16 }, { 58, 59, 35 },
            { 50, 95, 86 }, { 51, 95, 56 }, { 52, 95, 29 }, { 53, 95, 10 }, { 54, 95, 78 },
            { 55, 95, 23 }, { 56, 95, 3 }, { 57, 95, 45 }, { 95, 99, 12 }, { 60, 62, 6 },
            { 61, 62, 82 }, { 60, 63, 94 }, { 61, 63, 29 }, { 60, 64, 0 }, { 61, 64, 40 },
            { 62, 64, 99 }, { 63, 64, 44 }, { 60, 65, 84 }, { 61, 65, 76 }, { 62, 65, 6 },
            { 63, 65, 15 }, { 60, 66, 25 }, { 61, 66, 36 }, { 62, 66, 88 }, { 63, 66, 60 },
            { 64, 66, 60 }, { 65, 66, 3 }, { 60, 67, 44 }, { 61, 67, 14 }, { 62, 67, 37 },
            { 63, 67, 12 }, { 64, 67, 51 }, { 65, 67, 7 }, { 60, 68, 1 }, { 61, 68, 13 },
            { 62, 68, 80 }, { 63, 68, 42 }, { 64, 68, 28 }, { 65, 68, 85 }, { 66, 68, 14 },
            { 67, 68, 50 }, { 60, 69, 62 }, { 61, 69, 14 }, { 62, 69, 2 }, { 63, 69, 10 },
            { 64, 69, 74 }, { 65, 69, 16 }, { 66, 69, 37 }, { 67, 69, 51 }, { 68, 69, 45 },
            { 60, 96, 83 }, { 61, 96, 58 }, { 62, 96, 16 }, { 63, 96, 28 }, { 64, 96, 75 },
            { 65, 96, 60 }, { 66, 96, 76 }, { 67, 96, 54 }, { 96, 99, 85 }, { 70, 72, 38 },
            { 71, 72, 52 }, { 70, 73, 73 }, { 71, 73, 5 }, { 70, 74, 79 }, { 71, 74, 97 },
            { 72, 74, 94 }, { 73, 74, 47 }, { 70, 75, 96 }, { 71, 75, 14 }, { 72, 75, 87 },
            { 73, 75, 24 }, { 70, 76, 85 }, { 71, 76, 36 }, { 72, 76, 20 }, { 73, 76, 15 },
            { 74, 76, 78 }, { 75, 76, 97 }, { 70, 77, 9 }, { 71, 77, 87 }, { 72, 77, 21 },
            { 73, 77, 18 }, { 74, 77, 76 }, { 75, 77, 30 }, { 70, 78, 0 }, { 71, 78, 96 },
            { 72, 78, 4 }, { 73, 78, 7 }, { 74, 78, 17 }, { 75, 78, 65 }, { 76, 78, 63 },
            { 77, 78, 24 }, { 70, 79, 52 }, { 71, 79, 25 }, { 72, 79, 30 }, { 73, 79, 20 },
            { 74, 79, 48 }, { 75, 79, 14 }, { 76, 79, 29 }, { 77, 79, 35 }, { 78, 79, 87 },
            { 70, 97, 10 }, { 71, 97, 15 }, { 72, 97, 96 }, { 73, 97, 27 }, { 74, 97, 69 },
            { 75, 97, 22 }, { 76, 97, 54 }, { 77, 97, 28 }, { 97, 99, 38 }, { 80, 82, 70 },
            { 81, 82, 61 }, { 80, 83, 37 }, { 81, 83, 42 }, { 80, 84, 53 }, { 81, 84, 75 },
            { 82, 84, 78 }, { 83, 84, 91 }, { 80, 85, 14 }, { 81, 85, 70 }, { 82, 85, 70 },
            { 83, 85, 42 }, { 80, 86, 40 }, { 81, 86, 25 }, { 82, 86, 94 }, { 83, 86, 77 },
            { 84, 86, 5 }, { 85, 86, 51 }, { 80, 87, 78 }, { 81, 87, 49 }, { 82, 87, 43 },
            { 83, 87, 72 }, { 84, 87, 91 }, { 85, 87, 14 }, { 80, 88, 90 }, { 81, 88, 80 },
            { 82, 88, 2 }, { 83, 88, 4 }, { 84, 88, 6 }, { 85, 88, 37 }, { 86, 88, 99 },
            { 87, 88, 30 }, { 80, 89, 54 }, { 81, 89, 44 }, { 82, 89, 5 }, { 83, 89, 65 },
            { 84, 89, 46 }, { 85, 89, 33 }, { 86, 89, 39 }, { 87, 89, 13 }, { 88, 89, 93 },
            { 80, 98, 13 }, { 81, 98, 93 }, { 82, 98, 28 }, { 83, 98, 64 }, { 84, 98, 42 },
            { 85, 98, 86 }, { 86, 98, 22 }, { 87, 98, 17 }, { 98, 99, 46 } };
        Graph<Integer, DefaultEdge> graph = TestUtil.createUndirected(edges);

        KolmogorovWeightedPerfectMatching<Integer, DefaultEdge> perfectMatching =
            new KolmogorovWeightedPerfectMatching<>(graph, options);
        perfectMatching.getMatching();
    }

    /**
     * Test on a 11-regular graph without perfect matching: |V| = 144, |max. cardinality matching| =
     * 67
     */
    @Test(expected = IllegalArgumentException.class)
    public void testGetMatching50()
    {
        int[][] edges = new int[][] { { 0, 2, 44 }, { 1, 2, 27 }, { 0, 3, 2 }, { 1, 3, 73 },
            { 0, 4, 11 }, { 1, 4, 4 }, { 2, 4, 98 }, { 3, 4, 28 }, { 0, 5, 0 }, { 1, 5, 53 },
            { 2, 5, 19 }, { 3, 5, 41 }, { 0, 6, 27 }, { 1, 6, 8 }, { 2, 6, 21 }, { 3, 6, 91 },
            { 4, 6, 88 }, { 5, 6, 37 }, { 0, 7, 6 }, { 1, 7, 84 }, { 2, 7, 92 }, { 3, 7, 71 },
            { 4, 7, 90 }, { 5, 7, 13 }, { 0, 8, 79 }, { 1, 8, 96 }, { 2, 8, 53 }, { 3, 8, 49 },
            { 4, 8, 48 }, { 5, 8, 32 }, { 6, 8, 46 }, { 7, 8, 63 }, { 0, 9, 97 }, { 1, 9, 43 },
            { 2, 9, 55 }, { 3, 9, 99 }, { 4, 9, 21 }, { 5, 9, 20 }, { 6, 9, 24 }, { 7, 9, 4 },
            { 0, 10, 1 }, { 1, 10, 61 }, { 2, 10, 20 }, { 3, 10, 20 }, { 4, 10, 90 }, { 5, 10, 0 },
            { 6, 10, 27 }, { 7, 10, 95 }, { 8, 10, 85 }, { 9, 10, 26 }, { 0, 11, 12 },
            { 1, 11, 18 }, { 2, 11, 85 }, { 3, 11, 0 }, { 4, 11, 50 }, { 5, 11, 72 }, { 6, 11, 37 },
            { 7, 11, 48 }, { 8, 11, 94 }, { 9, 11, 33 }, { 10, 11, 86 }, { 0, 132, 41 },
            { 1, 132, 55 }, { 2, 132, 9 }, { 3, 132, 45 }, { 4, 132, 25 }, { 5, 132, 98 },
            { 6, 132, 52 }, { 7, 132, 24 }, { 8, 132, 63 }, { 9, 132, 47 }, { 132, 143, 14 },
            { 12, 14, 12 }, { 13, 14, 71 }, { 12, 15, 19 }, { 13, 15, 31 }, { 12, 16, 16 },
            { 13, 16, 35 }, { 14, 16, 51 }, { 15, 16, 61 }, { 12, 17, 68 }, { 13, 17, 88 },
            { 14, 17, 27 }, { 15, 17, 52 }, { 12, 18, 21 }, { 13, 18, 42 }, { 14, 18, 80 },
            { 15, 18, 20 }, { 16, 18, 67 }, { 17, 18, 99 }, { 12, 19, 87 }, { 13, 19, 9 },
            { 14, 19, 46 }, { 15, 19, 44 }, { 16, 19, 53 }, { 17, 19, 16 }, { 12, 20, 40 },
            { 13, 20, 96 }, { 14, 20, 27 }, { 15, 20, 16 }, { 16, 20, 80 }, { 17, 20, 83 },
            { 18, 20, 81 }, { 19, 20, 80 }, { 12, 21, 53 }, { 13, 21, 23 }, { 14, 21, 73 },
            { 15, 21, 51 }, { 16, 21, 24 }, { 17, 21, 71 }, { 18, 21, 55 }, { 19, 21, 81 },
            { 12, 22, 48 }, { 13, 22, 45 }, { 14, 22, 1 }, { 15, 22, 71 }, { 16, 22, 97 },
            { 17, 22, 74 }, { 18, 22, 45 }, { 19, 22, 67 }, { 20, 22, 6 }, { 21, 22, 18 },
            { 12, 23, 65 }, { 13, 23, 4 }, { 14, 23, 7 }, { 15, 23, 66 }, { 16, 23, 0 },
            { 17, 23, 88 }, { 18, 23, 56 }, { 19, 23, 74 }, { 20, 23, 48 }, { 21, 23, 74 },
            { 22, 23, 44 }, { 12, 133, 61 }, { 13, 133, 84 }, { 14, 133, 7 }, { 15, 133, 70 },
            { 16, 133, 11 }, { 17, 133, 13 }, { 18, 133, 16 }, { 19, 133, 14 }, { 20, 133, 38 },
            { 21, 133, 22 }, { 133, 143, 35 }, { 24, 26, 40 }, { 25, 26, 45 }, { 24, 27, 80 },
            { 25, 27, 83 }, { 24, 28, 51 }, { 25, 28, 65 }, { 26, 28, 46 }, { 27, 28, 46 },
            { 24, 29, 3 }, { 25, 29, 70 }, { 26, 29, 75 }, { 27, 29, 82 }, { 24, 30, 47 },
            { 25, 30, 55 }, { 26, 30, 21 }, { 27, 30, 50 }, { 28, 30, 31 }, { 29, 30, 96 },
            { 24, 31, 47 }, { 25, 31, 58 }, { 26, 31, 67 }, { 27, 31, 29 }, { 28, 31, 46 },
            { 29, 31, 93 }, { 24, 32, 65 }, { 25, 32, 88 }, { 26, 32, 54 }, { 27, 32, 81 },
            { 28, 32, 78 }, { 29, 32, 84 }, { 30, 32, 97 }, { 31, 32, 45 }, { 24, 33, 71 },
            { 25, 33, 96 }, { 26, 33, 37 }, { 27, 33, 50 }, { 28, 33, 60 }, { 29, 33, 89 },
            { 30, 33, 55 }, { 31, 33, 76 }, { 24, 34, 35 }, { 25, 34, 45 }, { 26, 34, 26 },
            { 27, 34, 27 }, { 28, 34, 60 }, { 29, 34, 37 }, { 30, 34, 38 }, { 31, 34, 79 },
            { 32, 34, 88 }, { 33, 34, 30 }, { 24, 35, 90 }, { 25, 35, 67 }, { 26, 35, 18 },
            { 27, 35, 5 }, { 28, 35, 22 }, { 29, 35, 75 }, { 30, 35, 58 }, { 31, 35, 37 },
            { 32, 35, 82 }, { 33, 35, 32 }, { 34, 35, 47 }, { 24, 134, 85 }, { 25, 134, 67 },
            { 26, 134, 31 }, { 27, 134, 72 }, { 28, 134, 40 }, { 29, 134, 38 }, { 30, 134, 43 },
            { 31, 134, 68 }, { 32, 134, 63 }, { 33, 134, 80 }, { 134, 143, 82 }, { 36, 38, 87 },
            { 37, 38, 64 }, { 36, 39, 47 }, { 37, 39, 3 }, { 36, 40, 19 }, { 37, 40, 58 },
            { 38, 40, 31 }, { 39, 40, 92 }, { 36, 41, 87 }, { 37, 41, 57 }, { 38, 41, 7 },
            { 39, 41, 39 }, { 36, 42, 66 }, { 37, 42, 55 }, { 38, 42, 60 }, { 39, 42, 67 },
            { 40, 42, 78 }, { 41, 42, 43 }, { 36, 43, 5 }, { 37, 43, 5 }, { 38, 43, 16 },
            { 39, 43, 52 }, { 40, 43, 99 }, { 41, 43, 8 }, { 36, 44, 4 }, { 37, 44, 33 },
            { 38, 44, 41 }, { 39, 44, 20 }, { 40, 44, 42 }, { 41, 44, 67 }, { 42, 44, 6 },
            { 43, 44, 89 }, { 36, 45, 85 }, { 37, 45, 61 }, { 38, 45, 22 }, { 39, 45, 99 },
            { 40, 45, 93 }, { 41, 45, 56 }, { 42, 45, 48 }, { 43, 45, 78 }, { 36, 46, 84 },
            { 37, 46, 57 }, { 38, 46, 93 }, { 39, 46, 87 }, { 40, 46, 1 }, { 41, 46, 75 },
            { 42, 46, 57 }, { 43, 46, 69 }, { 44, 46, 68 }, { 45, 46, 2 }, { 36, 47, 7 },
            { 37, 47, 56 }, { 38, 47, 6 }, { 39, 47, 25 }, { 40, 47, 23 }, { 41, 47, 4 },
            { 42, 47, 59 }, { 43, 47, 99 }, { 44, 47, 4 }, { 45, 47, 36 }, { 46, 47, 60 },
            { 36, 135, 20 }, { 37, 135, 89 }, { 38, 135, 60 }, { 39, 135, 30 }, { 40, 135, 36 },
            { 41, 135, 67 }, { 42, 135, 97 }, { 43, 135, 23 }, { 44, 135, 34 }, { 45, 135, 43 },
            { 135, 143, 84 }, { 48, 50, 9 }, { 49, 50, 39 }, { 48, 51, 39 }, { 49, 51, 66 },
            { 48, 52, 96 }, { 49, 52, 85 }, { 50, 52, 60 }, { 51, 52, 36 }, { 48, 53, 22 },
            { 49, 53, 33 }, { 50, 53, 97 }, { 51, 53, 93 }, { 48, 54, 47 }, { 49, 54, 85 },
            { 50, 54, 30 }, { 51, 54, 35 }, { 52, 54, 19 }, { 53, 54, 22 }, { 48, 55, 77 },
            { 49, 55, 52 }, { 50, 55, 35 }, { 51, 55, 85 }, { 52, 55, 27 }, { 53, 55, 43 },
            { 48, 56, 40 }, { 49, 56, 32 }, { 50, 56, 99 }, { 51, 56, 24 }, { 52, 56, 79 },
            { 53, 56, 56 }, { 54, 56, 90 }, { 55, 56, 90 }, { 48, 57, 63 }, { 49, 57, 75 },
            { 50, 57, 88 }, { 51, 57, 59 }, { 52, 57, 59 }, { 53, 57, 7 }, { 54, 57, 30 },
            { 55, 57, 14 }, { 48, 58, 71 }, { 49, 58, 96 }, { 50, 58, 5 }, { 51, 58, 61 },
            { 52, 58, 98 }, { 53, 58, 59 }, { 54, 58, 27 }, { 55, 58, 33 }, { 56, 58, 42 },
            { 57, 58, 78 }, { 48, 59, 17 }, { 49, 59, 53 }, { 50, 59, 5 }, { 51, 59, 49 },
            { 52, 59, 28 }, { 53, 59, 32 }, { 54, 59, 15 }, { 55, 59, 43 }, { 56, 59, 68 },
            { 57, 59, 4 }, { 58, 59, 91 }, { 48, 136, 29 }, { 49, 136, 21 }, { 50, 136, 14 },
            { 51, 136, 63 }, { 52, 136, 68 }, { 53, 136, 59 }, { 54, 136, 25 }, { 55, 136, 13 },
            { 56, 136, 76 }, { 57, 136, 88 }, { 136, 143, 65 }, { 60, 62, 12 }, { 61, 62, 57 },
            { 60, 63, 93 }, { 61, 63, 92 }, { 60, 64, 45 }, { 61, 64, 22 }, { 62, 64, 7 },
            { 63, 64, 62 }, { 60, 65, 84 }, { 61, 65, 95 }, { 62, 65, 89 }, { 63, 65, 15 },
            { 60, 66, 65 }, { 61, 66, 83 }, { 62, 66, 74 }, { 63, 66, 6 }, { 64, 66, 81 },
            { 65, 66, 88 }, { 60, 67, 4 }, { 61, 67, 63 }, { 62, 67, 97 }, { 63, 67, 89 },
            { 64, 67, 53 }, { 65, 67, 65 }, { 60, 68, 55 }, { 61, 68, 62 }, { 62, 68, 70 },
            { 63, 68, 13 }, { 64, 68, 12 }, { 65, 68, 4 }, { 66, 68, 37 }, { 67, 68, 46 },
            { 60, 69, 14 }, { 61, 69, 38 }, { 62, 69, 20 }, { 63, 69, 40 }, { 64, 69, 40 },
            { 65, 69, 9 }, { 66, 69, 66 }, { 67, 69, 71 }, { 60, 70, 43 }, { 61, 70, 29 },
            { 62, 70, 33 }, { 63, 70, 80 }, { 64, 70, 61 }, { 65, 70, 28 }, { 66, 70, 36 },
            { 67, 70, 9 }, { 68, 70, 43 }, { 69, 70, 0 }, { 60, 71, 31 }, { 61, 71, 81 },
            { 62, 71, 74 }, { 63, 71, 81 }, { 64, 71, 86 }, { 65, 71, 22 }, { 66, 71, 38 },
            { 67, 71, 8 }, { 68, 71, 62 }, { 69, 71, 78 }, { 70, 71, 90 }, { 60, 137, 19 },
            { 61, 137, 64 }, { 62, 137, 94 }, { 63, 137, 8 }, { 64, 137, 53 }, { 65, 137, 43 },
            { 66, 137, 92 }, { 67, 137, 62 }, { 68, 137, 64 }, { 69, 137, 57 }, { 137, 143, 79 },
            { 72, 74, 41 }, { 73, 74, 35 }, { 72, 75, 17 }, { 73, 75, 36 }, { 72, 76, 21 },
            { 73, 76, 57 }, { 74, 76, 18 }, { 75, 76, 97 }, { 72, 77, 25 }, { 73, 77, 2 },
            { 74, 77, 20 }, { 75, 77, 2 }, { 72, 78, 73 }, { 73, 78, 98 }, { 74, 78, 55 },
            { 75, 78, 15 }, { 76, 78, 39 }, { 77, 78, 82 }, { 72, 79, 44 }, { 73, 79, 79 },
            { 74, 79, 3 }, { 75, 79, 44 }, { 76, 79, 19 }, { 77, 79, 60 }, { 72, 80, 10 },
            { 73, 80, 62 }, { 74, 80, 17 }, { 75, 80, 25 }, { 76, 80, 73 }, { 77, 80, 12 },
            { 78, 80, 4 }, { 79, 80, 67 }, { 72, 81, 54 }, { 73, 81, 32 }, { 74, 81, 2 },
            { 75, 81, 92 }, { 76, 81, 5 }, { 77, 81, 25 }, { 78, 81, 93 }, { 79, 81, 57 },
            { 72, 82, 46 }, { 73, 82, 14 }, { 74, 82, 87 }, { 75, 82, 36 }, { 76, 82, 62 },
            { 77, 82, 88 }, { 78, 82, 46 }, { 79, 82, 95 }, { 80, 82, 40 }, { 81, 82, 11 },
            { 72, 83, 1 }, { 73, 83, 59 }, { 74, 83, 18 }, { 75, 83, 6 }, { 76, 83, 19 },
            { 77, 83, 88 }, { 78, 83, 88 }, { 79, 83, 22 }, { 80, 83, 74 }, { 81, 83, 7 },
            { 82, 83, 77 }, { 72, 138, 99 }, { 73, 138, 53 }, { 74, 138, 51 }, { 75, 138, 13 },
            { 76, 138, 65 }, { 77, 138, 78 }, { 78, 138, 68 }, { 79, 138, 85 }, { 80, 138, 25 },
            { 81, 138, 98 }, { 138, 143, 58 }, { 84, 86, 92 }, { 85, 86, 55 }, { 84, 87, 55 },
            { 85, 87, 54 }, { 84, 88, 25 }, { 85, 88, 94 }, { 86, 88, 15 }, { 87, 88, 59 },
            { 84, 89, 36 }, { 85, 89, 59 }, { 86, 89, 51 }, { 87, 89, 53 }, { 84, 90, 37 },
            { 85, 90, 70 }, { 86, 90, 21 }, { 87, 90, 68 }, { 88, 90, 11 }, { 89, 90, 76 },
            { 84, 91, 44 }, { 85, 91, 72 }, { 86, 91, 88 }, { 87, 91, 83 }, { 88, 91, 15 },
            { 89, 91, 15 }, { 84, 92, 8 }, { 85, 92, 9 }, { 86, 92, 68 }, { 87, 92, 89 },
            { 88, 92, 55 }, { 89, 92, 37 }, { 90, 92, 62 }, { 91, 92, 50 }, { 84, 93, 66 },
            { 85, 93, 63 }, { 86, 93, 74 }, { 87, 93, 10 }, { 88, 93, 13 }, { 89, 93, 4 },
            { 90, 93, 65 }, { 91, 93, 90 }, { 84, 94, 93 }, { 85, 94, 52 }, { 86, 94, 24 },
            { 87, 94, 84 }, { 88, 94, 58 }, { 89, 94, 49 }, { 90, 94, 7 }, { 91, 94, 18 },
            { 92, 94, 75 }, { 93, 94, 60 }, { 84, 95, 1 }, { 85, 95, 98 }, { 86, 95, 12 },
            { 87, 95, 91 }, { 88, 95, 66 }, { 89, 95, 66 }, { 90, 95, 75 }, { 91, 95, 12 },
            { 92, 95, 57 }, { 93, 95, 60 }, { 94, 95, 95 }, { 84, 139, 81 }, { 85, 139, 27 },
            { 86, 139, 62 }, { 87, 139, 97 }, { 88, 139, 73 }, { 89, 139, 76 }, { 90, 139, 26 },
            { 91, 139, 22 }, { 92, 139, 30 }, { 93, 139, 50 }, { 139, 143, 81 }, { 96, 98, 59 },
            { 97, 98, 38 }, { 96, 99, 42 }, { 97, 99, 48 }, { 96, 100, 18 }, { 97, 100, 30 },
            { 98, 100, 33 }, { 99, 100, 32 }, { 96, 101, 9 }, { 97, 101, 26 }, { 98, 101, 8 },
            { 99, 101, 15 }, { 96, 102, 56 }, { 97, 102, 97 }, { 98, 102, 42 }, { 99, 102, 30 },
            { 100, 102, 83 }, { 101, 102, 76 }, { 96, 103, 37 }, { 97, 103, 45 }, { 98, 103, 79 },
            { 99, 103, 23 }, { 100, 103, 95 }, { 101, 103, 4 }, { 96, 104, 84 }, { 97, 104, 69 },
            { 98, 104, 77 }, { 99, 104, 22 }, { 100, 104, 2 }, { 101, 104, 17 }, { 102, 104, 6 },
            { 103, 104, 39 }, { 96, 105, 37 }, { 97, 105, 13 }, { 98, 105, 64 }, { 99, 105, 4 },
            { 100, 105, 61 }, { 101, 105, 29 }, { 102, 105, 1 }, { 103, 105, 58 }, { 96, 106, 18 },
            { 97, 106, 76 }, { 98, 106, 94 }, { 99, 106, 46 }, { 100, 106, 33 }, { 101, 106, 47 },
            { 102, 106, 68 }, { 103, 106, 44 }, { 104, 106, 56 }, { 105, 106, 59 }, { 96, 107, 72 },
            { 97, 107, 98 }, { 98, 107, 8 }, { 99, 107, 45 }, { 100, 107, 29 }, { 101, 107, 8 },
            { 102, 107, 21 }, { 103, 107, 78 }, { 104, 107, 97 }, { 105, 107, 62 },
            { 106, 107, 42 }, { 96, 140, 65 }, { 97, 140, 81 }, { 98, 140, 39 }, { 99, 140, 97 },
            { 100, 140, 84 }, { 101, 140, 73 }, { 102, 140, 67 }, { 103, 140, 23 },
            { 104, 140, 36 }, { 105, 140, 56 }, { 140, 143, 44 }, { 108, 110, 67 },
            { 109, 110, 26 }, { 108, 111, 6 }, { 109, 111, 30 }, { 108, 112, 95 }, { 109, 112, 41 },
            { 110, 112, 56 }, { 111, 112, 27 }, { 108, 113, 14 }, { 109, 113, 73 },
            { 110, 113, 32 }, { 111, 113, 95 }, { 108, 114, 40 }, { 109, 114, 54 },
            { 110, 114, 88 }, { 111, 114, 85 }, { 112, 114, 0 }, { 113, 114, 56 }, { 108, 115, 63 },
            { 109, 115, 18 }, { 110, 115, 87 }, { 111, 115, 74 }, { 112, 115, 12 },
            { 113, 115, 69 }, { 108, 116, 41 }, { 109, 116, 42 }, { 110, 116, 58 },
            { 111, 116, 44 }, { 112, 116, 1 }, { 113, 116, 54 }, { 114, 116, 14 }, { 115, 116, 14 },
            { 108, 117, 81 }, { 109, 117, 75 }, { 110, 117, 64 }, { 111, 117, 5 }, { 112, 117, 60 },
            { 113, 117, 72 }, { 114, 117, 40 }, { 115, 117, 84 }, { 108, 118, 67 },
            { 109, 118, 11 }, { 110, 118, 49 }, { 111, 118, 12 }, { 112, 118, 5 }, { 113, 118, 2 },
            { 114, 118, 78 }, { 115, 118, 17 }, { 116, 118, 67 }, { 117, 118, 56 },
            { 108, 119, 73 }, { 109, 119, 50 }, { 110, 119, 95 }, { 111, 119, 66 },
            { 112, 119, 82 }, { 113, 119, 52 }, { 114, 119, 53 }, { 115, 119, 90 },
            { 116, 119, 11 }, { 117, 119, 13 }, { 118, 119, 61 }, { 108, 141, 78 },
            { 109, 141, 73 }, { 110, 141, 29 }, { 111, 141, 18 }, { 112, 141, 31 }, { 113, 141, 2 },
            { 114, 141, 0 }, { 115, 141, 46 }, { 116, 141, 42 }, { 117, 141, 3 }, { 141, 143, 87 },
            { 120, 122, 74 }, { 121, 122, 90 }, { 120, 123, 23 }, { 121, 123, 0 }, { 120, 124, 43 },
            { 121, 124, 49 }, { 122, 124, 49 }, { 123, 124, 33 }, { 120, 125, 52 },
            { 121, 125, 55 }, { 122, 125, 53 }, { 123, 125, 19 }, { 120, 126, 56 },
            { 121, 126, 50 }, { 122, 126, 18 }, { 123, 126, 56 }, { 124, 126, 28 }, { 125, 126, 8 },
            { 120, 127, 61 }, { 121, 127, 79 }, { 122, 127, 27 }, { 123, 127, 45 },
            { 124, 127, 92 }, { 125, 127, 81 }, { 120, 128, 64 }, { 121, 128, 53 },
            { 122, 128, 59 }, { 123, 128, 70 }, { 124, 128, 91 }, { 125, 128, 21 },
            { 126, 128, 49 }, { 127, 128, 76 }, { 120, 129, 40 }, { 121, 129, 25 }, { 122, 129, 8 },
            { 123, 129, 46 }, { 124, 129, 30 }, { 125, 129, 30 }, { 126, 129, 82 },
            { 127, 129, 67 }, { 120, 130, 73 }, { 121, 130, 31 }, { 122, 130, 92 },
            { 123, 130, 64 }, { 124, 130, 60 }, { 125, 130, 65 }, { 126, 130, 31 },
            { 127, 130, 40 }, { 128, 130, 55 }, { 129, 130, 1 }, { 120, 131, 71 }, { 121, 131, 85 },
            { 122, 131, 90 }, { 123, 131, 93 }, { 124, 131, 21 }, { 125, 131, 84 },
            { 126, 131, 41 }, { 127, 131, 23 }, { 128, 131, 16 }, { 129, 131, 20 },
            { 130, 131, 82 }, { 120, 142, 36 }, { 121, 142, 49 }, { 122, 142, 87 }, { 123, 142, 6 },
            { 124, 142, 55 }, { 125, 142, 89 }, { 126, 142, 98 }, { 127, 142, 79 },
            { 128, 142, 77 }, { 129, 142, 25 }, { 142, 143, 29 } };
        Graph<Integer, DefaultEdge> graph = TestUtil.createUndirected(edges);

        KolmogorovWeightedPerfectMatching<Integer, DefaultEdge> perfectMatching =
            new KolmogorovWeightedPerfectMatching<>(graph, options);
        perfectMatching.getMatching();
    }

    /**
     * A method to run a test case.
     *
     * @param edges array of edges with their weights
     * @param result the expected weight of a resulting matching
     * @param objectiveSense objective sense of the algorithm
     */
    private void test(int[][] edges, double result, ObjectiveSense objectiveSense)
    {
        test(
            new DefaultUndirectedWeightedGraph<>(DefaultEdge.class), edges, result, objectiveSense);
    }

    /**
     * A method to run a test case.
     *
     * @param graph the graph to add edges to
     * @param edges array of edges with their weights
     * @param result the expected weight of a resulting matching
     * @param objectiveSense objective sense of the algorithm
     */
    private void test(
        Graph<Integer, DefaultEdge> graph, int[][] edges, double result,
        ObjectiveSense objectiveSense)
    {
        TestUtil.constructGraph(graph, edges);

        KolmogorovWeightedPerfectMatching<Integer, DefaultEdge> perfectMatching =
            new KolmogorovWeightedPerfectMatching<>(graph, options, objectiveSense);
        MatchingAlgorithm.Matching<Integer, DefaultEdge> matching = perfectMatching.getMatching();
        assertEquals(result, matching.getWeight(), EPS);
        assertTrue(perfectMatching.testOptimality());
        checkMatchingAndDualSolution(matching, perfectMatching.getDualSolution(), objectiveSense);
    }
}
