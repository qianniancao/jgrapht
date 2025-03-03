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
package org.jgrapht.graph.guava;

import org.jgrapht.Graph;
import org.junit.*;

import java.io.*;
import java.util.*;
import java.util.function.*;

import com.google.common.graph.*;

import static org.junit.Assert.*;

/**
 * Check Incoming/Outgoing edges in directed and undirected graphs.
 *
 * @author Dimitrios Michail
 */
@SuppressWarnings("unchecked")
public class MutableValueGraphAdapterTest
{

    /**
     * Test value propagation
     */
    @Test
    public void testWeights()
    {
        MutableValueGraph<String, MyValue> graph =
            ValueGraphBuilder.directed().allowsSelfLoops(true).build();

        graph.addNode("v1");
        graph.addNode("v2");
        graph.addNode("v3");
        graph.addNode("v4");
        graph.addNode("v5");
        graph.putEdgeValue("v1", "v2", new MyValue(2.0));
        graph.putEdgeValue("v2", "v3", new MyValue(3.0));
        graph.putEdgeValue("v2", "v4", new MyValue(4.0));
        graph.putEdgeValue("v4", "v4", new MyValue(5.0));
        graph.putEdgeValue("v5", "v2", new MyValue(6.0));

        Graph<String, EndpointPair<String>> g = new MutableValueGraphAdapter<>(
            graph, new MyValue(1.0d), (ToDoubleFunction<MyValue> & Serializable) MyValue::getValue);

        assertFalse(g.getType().isAllowingMultipleEdges());
        assertTrue(g.getType().isAllowingSelfLoops());
        assertTrue(g.getType().isDirected());
        assertFalse(g.getType().isUndirected());
        assertTrue(g.getType().isWeighted());
        assertTrue(g.getType().isAllowingCycles());

        assertEquals(2.0, g.getEdgeWeight(EndpointPair.ordered("v1", "v2")), 1e-9);
        assertEquals(3.0, g.getEdgeWeight(EndpointPair.ordered("v2", "v3")), 1e-9);
        assertEquals(4.0, g.getEdgeWeight(EndpointPair.ordered("v2", "v4")), 1e-9);
        assertEquals(5.0, g.getEdgeWeight(EndpointPair.ordered("v4", "v4")), 1e-9);
        assertEquals(6.0, g.getEdgeWeight(EndpointPair.ordered("v5", "v2")), 1e-9);

        // add edge and make sure that weight is default
        g.addEdge("v1", "v5");
        assertEquals(1.0d, g.getEdgeWeight(EndpointPair.ordered("v1", "v5")), 1e-9);

        // check that the adapter is only one way
        EndpointPair<String> endPoints = EndpointPair.ordered("v1", "v2");
        assertThrows(UnsupportedOperationException.class, () -> g.setEdgeWeight(endPoints, 1.0));
    }

    /**
     * Test two ways values in special case where value type is double.
     */
    @Test
    public void testDoubleWeights()
    {
        MutableValueGraph<String, Double> graph =
            ValueGraphBuilder.directed().allowsSelfLoops(true).build();

        graph.addNode("v1");
        graph.addNode("v2");
        graph.addNode("v3");
        graph.addNode("v4");
        graph.addNode("v5");
        graph.putEdgeValue("v1", "v2", 2.0);
        graph.putEdgeValue("v2", "v3", 3.0);
        graph.putEdgeValue("v2", "v4", 4.0);
        graph.putEdgeValue("v4", "v4", 5.0);
        graph.putEdgeValue("v5", "v2", 6.0);

        Graph<String, EndpointPair<String>> g = new MutableDoubleValueGraphAdapter<>(graph);

        assertFalse(g.getType().isAllowingMultipleEdges());
        assertTrue(g.getType().isAllowingSelfLoops());
        assertTrue(g.getType().isDirected());
        assertFalse(g.getType().isUndirected());
        assertTrue(g.getType().isWeighted());
        assertTrue(g.getType().isAllowingCycles());

        assertEquals(2.0, g.getEdgeWeight(EndpointPair.ordered("v1", "v2")), 1e-9);
        assertEquals(3.0, g.getEdgeWeight(EndpointPair.ordered("v2", "v3")), 1e-9);
        assertEquals(4.0, g.getEdgeWeight(EndpointPair.ordered("v2", "v4")), 1e-9);
        assertEquals(5.0, g.getEdgeWeight(EndpointPair.ordered("v4", "v4")), 1e-9);
        assertEquals(6.0, g.getEdgeWeight(EndpointPair.ordered("v5", "v2")), 1e-9);

        // add edge and make sure that weight is default
        g.addEdge("v1", "v5");
        assertEquals(1.0d, g.getEdgeWeight(EndpointPair.ordered("v1", "v5")), 1e-9);

        g.setEdgeWeight(EndpointPair.ordered("v1", "v2"), 99.0);
        assertEquals(99.0d, g.getEdgeWeight(EndpointPair.ordered("v1", "v2")), 1e-9);
    }

    /**
     * Example on javadoc
     */
    @Test
    public void testExample()
    {
        MutableValueGraph<String, MyValue> valueGraph =
            ValueGraphBuilder.directed().allowsSelfLoops(true).build();

        valueGraph.addNode("v1");
        valueGraph.addNode("v2");
        valueGraph.putEdgeValue("v1", "v2", new MyValue(5.0));

        Graph<String,
            EndpointPair<String>> graph = new MutableValueGraphAdapter<>(
                valueGraph, new MyValue(1.0),
                (ToDoubleFunction<MyValue> & Serializable) MyValue::getValue);

        assertEquals(5.0, graph.getEdgeWeight(EndpointPair.ordered("v1", "v2")), 1e-9);

        valueGraph.putEdgeValue("v1", "v2", new MyValue(9.0));

        assertEquals(9.0, graph.getEdgeWeight(EndpointPair.ordered("v1", "v2")), 1e-9);
    }

    /**
     * Example on javadoc
     */
    @Test
    public void testExampleDoubleWeights()
    {
        MutableValueGraph<String, Double> graph =
            ValueGraphBuilder.directed().allowsSelfLoops(true).build();

        graph.addNode("v1");
        graph.addNode("v2");
        graph.putEdgeValue("v1", "v2", 3.0);

        Graph<String, EndpointPair<String>> g = new MutableDoubleValueGraphAdapter<>(graph);

        assertEquals(3.0, g.getEdgeWeight(EndpointPair.ordered("v1", "v2")), 1e-9);

        g.setEdgeWeight(EndpointPair.ordered("v1", "v2"), 7.0);

        assertEquals(7.0, g.getEdgeWeight(EndpointPair.ordered("v1", "v2")), 1e-9);
    }

    /**
     * Test the most general version of the directed graph.
     */
    @Test
    public void testDirectedGraph()
    {
        Graph<String,
            EndpointPair<String>> g = new MutableValueGraphAdapter<>(
                ValueGraphBuilder.directed().allowsSelfLoops(true).build(), new MyValue(1.0),
                (ToDoubleFunction<MyValue> & Serializable) MyValue::getValue);

        assertFalse(g.getType().isAllowingMultipleEdges());
        assertTrue(g.getType().isAllowingSelfLoops());
        assertTrue(g.getType().isDirected());
        assertFalse(g.getType().isUndirected());
        assertTrue(g.getType().isWeighted());
        assertTrue(g.getType().isAllowingCycles());

        g.addVertex("v1");
        g.addVertex("v2");
        g.addVertex("v3");
        g.addVertex("v4");
        g.addVertex("v5");
        EndpointPair<String> e12 = g.addEdge("v1", "v2");
        EndpointPair<String> e23 = g.addEdge("v2", "v3");
        EndpointPair<String> e24 = g.addEdge("v2", "v4");
        EndpointPair<String> e44 = g.addEdge("v4", "v4");
        EndpointPair<String> e55 = g.addEdge("v5", "v5");
        EndpointPair<String> e52 = g.addEdge("v5", "v2");

        assertEquals(1, g.degreeOf("v1"));
        assertEquals(4, g.degreeOf("v2"));
        assertEquals(1, g.degreeOf("v3"));
        assertEquals(3, g.degreeOf("v4"));
        assertEquals(3, g.degreeOf("v5"));

        assertEquals(Set.of(e12), g.edgesOf("v1"));
        assertEquals(Set.of(e12, e23, e24, e52), g.edgesOf("v2"));
        assertEquals(Set.of(e23), g.edgesOf("v3"));
        assertEquals(Set.of(e24, e44), g.edgesOf("v4"));
        assertEquals(Set.of(e52, e55), g.edgesOf("v5"));

        assertEquals(0, g.inDegreeOf("v1"));
        assertEquals(2, g.inDegreeOf("v2"));
        assertEquals(1, g.inDegreeOf("v3"));
        assertEquals(2, g.inDegreeOf("v4"));
        assertEquals(1, g.inDegreeOf("v5"));

        assertEquals(Set.of(), g.incomingEdgesOf("v1"));
        assertEquals(Set.of(e12, e52), g.incomingEdgesOf("v2"));
        assertEquals(Set.of(e23), g.incomingEdgesOf("v3"));
        assertEquals(Set.of(e24, e44), g.incomingEdgesOf("v4"));
        assertEquals(Set.of(e55), g.incomingEdgesOf("v5"));

        assertEquals(1, g.outDegreeOf("v1"));
        assertEquals(2, g.outDegreeOf("v2"));
        assertEquals(0, g.outDegreeOf("v3"));
        assertEquals(1, g.outDegreeOf("v4"));
        assertEquals(2, g.outDegreeOf("v5"));

        assertEquals(Set.of(e12), g.outgoingEdgesOf("v1"));
        assertEquals(Set.of(e23, e24), g.outgoingEdgesOf("v2"));
        assertEquals(Set.of(), g.outgoingEdgesOf("v3"));
        assertEquals(Set.of(e44), g.outgoingEdgesOf("v4"));
        assertEquals(Set.of(e52, e55), g.outgoingEdgesOf("v5"));
    }

    /**
     * Test the most general version of the undirected graph.
     */
    @Test
    public void testUndirectedGraph()
    {
        Graph<String,
            EndpointPair<String>> g = new MutableValueGraphAdapter<>(
                ValueGraphBuilder.undirected().allowsSelfLoops(true).build(), new MyValue(1.0),
                (ToDoubleFunction<MyValue> & Serializable) MyValue::getValue);

        assertFalse(g.getType().isAllowingMultipleEdges());
        assertTrue(g.getType().isAllowingSelfLoops());
        assertFalse(g.getType().isDirected());
        assertTrue(g.getType().isUndirected());
        assertTrue(g.getType().isWeighted());
        assertTrue(g.getType().isAllowingCycles());

        g.addVertex("v1");
        g.addVertex("v2");
        g.addVertex("v3");
        g.addVertex("v4");
        g.addVertex("v5");
        EndpointPair<String> e12 = g.addEdge("v1", "v2");
        EndpointPair<String> e23 = g.addEdge("v2", "v3");
        EndpointPair<String> e24 = g.addEdge("v2", "v4");
        EndpointPair<String> e44 = g.addEdge("v4", "v4");
        EndpointPair<String> e55 = g.addEdge("v5", "v5");
        EndpointPair<String> e52 = g.addEdge("v5", "v2");

        assertEquals(1, g.degreeOf("v1"));
        assertEquals(4, g.degreeOf("v2"));
        assertEquals(1, g.degreeOf("v3"));
        assertEquals(3, g.degreeOf("v4"));
        assertEquals(3, g.degreeOf("v5"));

        assertEquals(Set.of(e12), g.edgesOf("v1"));
        assertEquals(Set.of(e12, e23, e24, e52), g.edgesOf("v2"));
        assertEquals(Set.of(e23), g.edgesOf("v3"));
        assertEquals(Set.of(e24, e44), g.edgesOf("v4"));
        assertEquals(Set.of(e52, e55), g.edgesOf("v5"));

        assertEquals(1, g.inDegreeOf("v1"));
        assertEquals(4, g.inDegreeOf("v2"));
        assertEquals(1, g.inDegreeOf("v3"));
        assertEquals(3, g.inDegreeOf("v4"));
        assertEquals(3, g.inDegreeOf("v5"));

        assertEquals(Set.of(e12), g.incomingEdgesOf("v1"));
        assertEquals(Set.of(e12, e23, e24, e52), g.incomingEdgesOf("v2"));
        assertEquals(Set.of(e23), g.incomingEdgesOf("v3"));
        assertEquals(Set.of(e24, e44), g.incomingEdgesOf("v4"));
        assertEquals(Set.of(e52, e55), g.incomingEdgesOf("v5"));

        assertEquals(1, g.outDegreeOf("v1"));
        assertEquals(4, g.outDegreeOf("v2"));
        assertEquals(1, g.outDegreeOf("v3"));
        assertEquals(3, g.outDegreeOf("v4"));
        assertEquals(3, g.outDegreeOf("v5"));

        assertEquals(Set.of(e12), g.outgoingEdgesOf("v1"));
        assertEquals(Set.of(e12, e23, e24, e52), g.outgoingEdgesOf("v2"));
        assertEquals(Set.of(e23), g.outgoingEdgesOf("v3"));
        assertEquals(Set.of(e24, e44), g.outgoingEdgesOf("v4"));
        assertEquals(Set.of(e52, e55), g.outgoingEdgesOf("v5"));
    }

    /**
     * Tests serialization
     */
    @Test
    public void testSerialization()
        throws Exception
    {
        Graph<String,
            EndpointPair<String>> g = new MutableValueGraphAdapter<>(
                ValueGraphBuilder.directed().allowsSelfLoops(true).build(), new MyValue(1.0),
                (ToDoubleFunction<MyValue> & Serializable) MyValue::getValue);

        assertFalse(g.getType().isAllowingMultipleEdges());
        assertTrue(g.getType().isAllowingSelfLoops());
        assertTrue(g.getType().isDirected());
        assertFalse(g.getType().isUndirected());
        assertTrue(g.getType().isWeighted());
        assertTrue(g.getType().isAllowingCycles());

        g.addVertex("v1");
        g.addVertex("v2");
        g.addVertex("v3");
        g.addVertex("v4");
        g.addVertex("v5");
        g.addEdge("v1", "v2");
        g.addEdge("v2", "v3");
        g.addEdge("v2", "v4");
        g.addEdge("v4", "v4");
        g.addEdge("v5", "v5");
        g.addEdge("v5", "v2");

        Graph<String, EndpointPair<String>> g2 = SerializationTestUtils.serializeAndDeserialize(g);

        assertFalse(g2.getType().isAllowingMultipleEdges());
        assertTrue(g2.getType().isAllowingSelfLoops());
        assertTrue(g2.getType().isDirected());
        assertFalse(g2.getType().isUndirected());
        assertTrue(g2.getType().isWeighted());
        assertTrue(g2.getType().isAllowingCycles());
        assertTrue(g2.containsVertex("v1"));
        assertTrue(g2.containsVertex("v2"));
        assertTrue(g2.containsVertex("v3"));
        assertTrue(g2.containsVertex("v4"));
        assertTrue(g2.containsVertex("v5"));
        assertEquals(5, g2.vertexSet().size());
        assertEquals(6, g2.edgeSet().size());
        assertTrue(g2.containsEdge("v1", "v2"));
        assertTrue(g2.containsEdge("v2", "v3"));
        assertTrue(g2.containsEdge("v2", "v4"));
        assertTrue(g2.containsEdge("v4", "v4"));
        assertTrue(g2.containsEdge("v5", "v5"));
        assertTrue(g2.containsEdge("v5", "v2"));

        assertEquals(g.toString(), g2.toString());
    }

    /**
     * Tests serialization
     */
    @Test
    public void testSerialization1()
        throws Exception
    {
        Graph<String,
            EndpointPair<String>> g = new MutableValueGraphAdapter<>(
                ValueGraphBuilder.undirected().allowsSelfLoops(true).build(), new MyValue(1.0),
                (ToDoubleFunction<MyValue> & Serializable) MyValue::getValue);

        assertFalse(g.getType().isAllowingMultipleEdges());
        assertTrue(g.getType().isAllowingSelfLoops());
        assertFalse(g.getType().isDirected());
        assertTrue(g.getType().isUndirected());
        assertTrue(g.getType().isWeighted());
        assertTrue(g.getType().isAllowingCycles());

        g.addVertex("v1");
        g.addVertex("v2");
        g.addVertex("v3");
        g.addEdge("v1", "v2");
        g.addEdge("v2", "v3");
        g.addEdge("v3", "v3");

        Graph<String, EndpointPair<String>> g2 = SerializationTestUtils.serializeAndDeserialize(g);

        assertFalse(g2.getType().isAllowingMultipleEdges());
        assertTrue(g2.getType().isAllowingSelfLoops());
        assertFalse(g2.getType().isDirected());
        assertTrue(g2.getType().isUndirected());
        assertTrue(g2.getType().isWeighted());
        assertTrue(g2.getType().isAllowingCycles());
        assertTrue(g2.containsVertex("v1"));
        assertTrue(g2.containsVertex("v2"));
        assertTrue(g2.containsVertex("v3"));
        assertEquals(3, g2.vertexSet().size());
        assertEquals(3, g2.edgeSet().size());
        assertTrue(g2.containsEdge("v1", "v2"));
        assertTrue(g2.containsEdge("v2", "v3"));
        assertTrue(g2.containsEdge("v3", "v3"));
    }

    private static class MyValue
        implements Serializable
    {
        private static final long serialVersionUID = 1L;

        private double value;

        public MyValue(double value)
        {
            this.value = value;
        }

        public double getValue()
        {
            return value;
        }
    }

    @Test
    public void testEdgeCoherenceSameNetworkWithComparator()
    {
        MutableValueGraph<Integer, MyValue> g =
            ValueGraphBuilder.undirected().allowsSelfLoops(true).build();
        g.addNode(0);
        g.addNode(1);
        g.putEdgeValue(0, 1, new MyValue(1.0));

        final Graph<Integer,
            EndpointPair<Integer>> a = new MutableValueGraphAdapter<>(
                g, new MyValue(1.0d), (ToDoubleFunction<MyValue> & Serializable) MyValue::getValue,
                null, null, ElementOrderMethod.comparator(Comparator.<Integer> naturalOrder()));

        EndpointPair<Integer> e1 = a.getEdge(0, 1);
        EndpointPair<Integer> e2 = a.getEdge(1, 0);

        assertEquals(e1, e2);
        assertEquals(a.getEdgeSource(e1), a.getEdgeSource(e2));
        assertEquals(a.getEdgeTarget(e1), a.getEdgeTarget(e2));
    }

}
