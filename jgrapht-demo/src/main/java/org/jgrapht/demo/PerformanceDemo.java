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
package org.jgrapht.demo;

import org.jgrapht.*;
import org.jgrapht.graph.*;
import org.jgrapht.traverse.*;

import java.io.*;
import java.util.*;

/**
 * A simple demo to test memory and CPU consumption on a graph with 3 million elements.
 * 一个简单演示，用于在包含 300 万个元素的图表上测试内存和 CPU 消耗。
 *
 * <p>
 * NOTE: To run this demo you may need to increase the JVM max mem size. In Sun's JVM it is done
 * using the "-Xmx" switch. Specify "-Xmx300M" to set it to 300MB.
 * 注意：要运行此演示，您可能需要增加 JVM 最大内存大小。在 Sun 的 JVM 中，这是使用“-Xmx”开关完成的。指定“-Xmx300M”将其设置为 300MB。
 *
 * </p>
 *
 * <p>
 * WARNING: Don't run this demo as-is on machines with less than 512MB memory. Your machine will
 * start paging severely. You need to first modify it to have fewer graph elements. This is easily
 * done by changing the loop counters below.
 * 警告：请勿在内存小于 512MB 的计算机上按原样运行此演示。你的机器将开始严重分页。您需要首先修改它以减少图形元素。通过更改下面的循环计数器可以轻松完成此操作。
 *
 * </p>
 *
 * @author Barak Naveh
 */
public final class PerformanceDemo
{
    /**
     * The starting point for the demo.
     *
     * @param args ignored.
     */
    public static void main(String[] args)
    {
        long time = System.currentTimeMillis();

        reportPerformanceFor("starting at", time);

        Graph<Object, DefaultEdge> g = new Pseudograph<>(DefaultEdge.class);
        Object prev;
        Object curr;

        curr = prev = new Object();
        g.addVertex(prev);

        int numVertices = 10000;
        int numEdgesPerVertex = 200;
        int numElements = numVertices * (1 + numEdgesPerVertex);

        System.out.println(
            "\n" + "allocating graph with " + numElements
                + " elements (may take a few tens of seconds)...");

        for (int i = 0; i < numVertices; i++) {
            curr = new Object();
            g.addVertex(curr);

            for (int j = 0; j < numEdgesPerVertex; j++) {
                g.addEdge(prev, curr);
            }

            prev = curr;
        }

        reportPerformanceFor("graph allocation", time);

        time = System.currentTimeMillis();

        for (Iterator<Object> i = new BreadthFirstIterator<>(g); i.hasNext();) {
            i.next();
        }

        reportPerformanceFor("breadth traversal", time);

        time = System.currentTimeMillis();

        for (Iterator<Object> i = new DepthFirstIterator<>(g); i.hasNext();) {
            i.next();
        }

        reportPerformanceFor("depth traversal", time);

        System.out.println("\n" + "Paused: graph is still in memory (to check mem consumption).");
        System.out.print("press enter to free memory and finish...");

        try {
            System.in.read();
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("done.");
    }

    private static void reportPerformanceFor(String msg, long refTime)
    {
        double time = (System.currentTimeMillis() - refTime) / 1000.0;
        double mem = usedMemory() / (1024.0 * 1024.0);
        mem = Math.round(mem * 100) / 100.0;
        System.out.println(msg + " (" + time + " sec, " + mem + "MB)");
    }

    private static long usedMemory()
    {
        Runtime rt = Runtime.getRuntime();

        return rt.totalMemory() - rt.freeMemory();
    }
}
