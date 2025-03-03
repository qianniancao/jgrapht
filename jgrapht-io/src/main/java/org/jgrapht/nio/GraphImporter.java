/*
 * (C) Copyright 2016-2023, by Dimitrios Michail and Contributors.
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
package org.jgrapht.nio;

import org.jgrapht.*;

import java.io.*;
import java.nio.charset.*;

/**
 * Interface for graph importers
 * 
 * @param <V> the graph vertex type
 * @param <E> the graph edge type
 */
public interface GraphImporter<V, E>
{

    /**
     * Import a graph from the given {@link InputStream}.
     * <p>
     * It is the callers responsibility to ensure the {@code InputStream} is closed after this
     * method returned.
     * </p>
     * 
     * @param g the graph
     * @param in the input stream
     * @throws ImportException in case any error occurs, such as I/O or parse error
     */
    default void importGraph(Graph<V, E> g, InputStream in)
    {
        importGraph(g, new InputStreamReader(in, StandardCharsets.UTF_8));
    }

    /**
     * Import a graph using the given {@link Reader}.
     * <p>
     * It is the callers responsibility to ensure the {@code Reader} is closed after this method
     * returned.
     * </p>
     * 
     * @param g the graph
     * @param in the input reader
     * @throws ImportException in case any error occurs, such as I/O or parse error
     */
    void importGraph(Graph<V, E> g, Reader in);

    /**
     * Import a graph from the given {@link File}.
     * 
     * @param g the graph
     * @param file the file to read from
     * @throws ImportException in case any error occurs, such as I/O or parse error
     */
    default void importGraph(Graph<V, E> g, File file)
    {
        try (Reader reader = new FileReader(file, StandardCharsets.UTF_8)) {
            importGraph(g, reader);
        } catch (IOException e) {
            throw new ImportException(e);
        }
    }
}
