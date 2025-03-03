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
package org.jgrapht.util;

import java.util.*;

/**
 * Helper class for building a one-to-one mapping for a collection of vertices to the integer range
 * $[0, n)$ where $n$ is the number of vertices in the collection.
 *
 * 用于构建一个一对一映射的帮助类，用于将顶点的集合映射到整数范围$[0, n)$，其中$n$是集合中顶点的数量。
 *
 * <p>
 * This class computes the mapping only once, on instantiation. It does not support live updates.
 * 此类仅在实例化时计算映射。它不支持实时更新。
 * </p>
 *
 * @author Alexandru Valeanu
 *
 * @param <V> the graph vertex type
 *           图顶点类型
 */
public class VertexToIntegerMapping<V>
{
    private final Map<V, Integer> vertexMap;
    private final List<V> indexList;

    /**
     * Create a new mapping from a list of vertices. The input list will be used as the
     * {@code indexList} so it must not be modified.
     *
     * @param vertices the input list of vertices
     * @throws NullPointerException if {@code vertices} is {@code null}
     * @throws IllegalArgumentException if the vertices are not distinct
     */
    public VertexToIntegerMapping(List<V> vertices)
    {
        Objects.requireNonNull(vertices, "the input collection of vertices cannot be null");

        vertexMap = CollectionUtil.newHashMapWithExpectedSize(vertices.size());
        indexList = vertices;

        for (V v : vertices) {
            if (vertexMap.put(v, vertexMap.size()) != null) {
                throw new IllegalArgumentException("vertices are not distinct");
            }
        }
    }

    /**
     * Create a new mapping from a collection of vertices.
     *
     * @param vertices the input collection of vertices
     * @throws NullPointerException if {@code vertices} is {@code null}
     * @throws IllegalArgumentException if the vertices are not distinct
     */
    public VertexToIntegerMapping(Collection<V> vertices)
    {
        this(
            new ArrayList<>(
                Objects
                    .requireNonNull(vertices, "the input collection of vertices cannot be null")));
    }

    /**
     * Get the {@code vertexMap}, a mapping from vertices to integers (i.e. the inverse of
     * {@code indexList}).
     *
     * @return a mapping from vertices to integers
     */
    public Map<V, Integer> getVertexMap()
    {
        return vertexMap;
    }

    /**
     * Get the {@code indexList}, a mapping from integers to vertices (i.e. the inverse of
     * {@code vertexMap}).
     *
     * @return a mapping from integers to vertices
     */
    public List<V> getIndexList()
    {
        return indexList;
    }
}
