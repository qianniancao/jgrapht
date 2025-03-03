/*
 * (C) Copyright 2019-2023, by Dimitrios Michail and Contributors.
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
package org.jgrapht.opt.graph.sparse.specifics;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.jgrapht.GraphType;
import org.jgrapht.alg.util.Pair;
import org.jgrapht.graph.DefaultGraphType;

/**
 * Specifics for a sparse undirected graph using an incidence matrix representation.
 * 
 * @author Dimitrios Michail
 */
public class IncidenceMatrixSparseUndirectedSpecifics
    implements
    SparseGraphSpecifics
{
    protected static final String UNMODIFIABLE = "this graph is unmodifiable";

    protected CSRBooleanMatrix incidenceMatrix;
    protected int[] source;
    protected int[] target;

    /**
     * Create a new graph from an edge stream
     * 
     * @param numVertices number of vertices
     * @param numEdges number of edges
     * @param edges a supplier of an edge stream
     */
    public IncidenceMatrixSparseUndirectedSpecifics(
        int numVertices, int numEdges, Supplier<Stream<Pair<Integer, Integer>>> edges)
    {
        final int m = numEdges;
        source = new int[m];
        target = new int[m];

        List<Pair<Integer, Integer>> nonZeros = new ArrayList<>(m);

        int[] eIndex = new int[1];
        edges.get().forEach(e -> {
            nonZeros.add(Pair.of(e.getFirst(), eIndex[0]));
            nonZeros.add(Pair.of(e.getSecond(), eIndex[0]));
            source[eIndex[0]] = e.getFirst();
            target[eIndex[0]] = e.getSecond();
            eIndex[0]++;
        });
        incidenceMatrix = new CSRBooleanMatrix(numVertices, m, nonZeros);
    }

    @Override
    public long edgesCount()
    {
        return incidenceMatrix.columns();
    }

    @Override
    public long verticesCount()
    {
        return incidenceMatrix.rows();
    }

    @Override
    public long degreeOf(Integer vertex)
    {
        assertVertexExist(vertex);
        return incidenceMatrix.nonZeros(vertex);
    }

    @Override
    public Set<Integer> edgesOf(Integer vertex)
    {
        assertVertexExist(vertex);
        return incidenceMatrix.nonZerosSet(vertex);
    }

    @Override
    public long inDegreeOf(Integer vertex)
    {
        assertVertexExist(vertex);
        return incidenceMatrix.nonZeros(vertex);
    }

    @Override
    public Set<Integer> incomingEdgesOf(Integer vertex)
    {
        assertVertexExist(vertex);
        return incidenceMatrix.nonZerosSet(vertex);
    }

    @Override
    public long outDegreeOf(Integer vertex)
    {
        assertVertexExist(vertex);
        return incidenceMatrix.nonZeros(vertex);
    }

    @Override
    public Set<Integer> outgoingEdgesOf(Integer vertex)
    {
        assertVertexExist(vertex);
        return incidenceMatrix.nonZerosSet(vertex);
    }

    @Override
    public GraphType getType()
    {
        return new DefaultGraphType.Builder()
            .undirected().weighted(false).modifiable(false).allowMultipleEdges(true)
            .allowSelfLoops(true).build();
    }

    @Override
    public Integer getEdgeSource(Integer e)
    {
        assertEdgeExist(e);
        return source[e];
    }

    @Override
    public Integer getEdgeTarget(Integer e)
    {
        assertEdgeExist(e);
        return target[e];
    }

    /**
     * {@inheritDoc}
     * 
     * This operation costs $O(d)$ where $d$ is the degree of the source vertex.
     */
    @Override
    public Integer getEdge(Integer sourceVertex, Integer targetVertex)
    {
        if (sourceVertex < 0 || sourceVertex >= incidenceMatrix.rows()) {
            return null;
        }
        if (targetVertex < 0 || targetVertex >= incidenceMatrix.rows()) {
            return null;
        }

        Iterator<Integer> it = incidenceMatrix.nonZerosPositionIterator(sourceVertex);
        while (it.hasNext()) {
            int eId = it.next();

            int v = getEdgeSource(eId);
            int u = getEdgeTarget(eId);

            if (v == sourceVertex.intValue() && u == targetVertex.intValue()
                || v == targetVertex.intValue() && u == sourceVertex.intValue())
            {
                return eId;
            }
        }
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * This operation costs $O(d)$ where $d$ is the degree of the source vertex.
     */
    @Override
    public Set<Integer> getAllEdges(Integer sourceVertex, Integer targetVertex)
    {
        if (sourceVertex < 0 || sourceVertex >= incidenceMatrix.rows()) {
            return null;
        }
        if (targetVertex < 0 || targetVertex >= incidenceMatrix.rows()) {
            return null;
        }

        Set<Integer> result = new LinkedHashSet<>();
        Iterator<Integer> it = incidenceMatrix.nonZerosPositionIterator(sourceVertex);
        while (it.hasNext()) {
            int eId = it.next();

            int v = getEdgeSource(eId);
            int u = getEdgeTarget(eId);

            if (v == sourceVertex.intValue() && u == targetVertex.intValue()
                || v == targetVertex.intValue() && u == sourceVertex.intValue())
            {
                result.add(eId);
            }
        }
        return result;
    }

}
