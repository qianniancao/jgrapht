/*
 * (C) Copyright 2006-2023, by John V Sichi and Contributors.
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
 * Helper for efficiently representing small sets whose elements are known to be unique by
 * construction, implying we don't need to enforce the uniqueness property in the data structure
 * itself. Use with caution.
 *
 * 用于有效表示元素是构造为唯一的集合的助手，这意味着我们不需要在数据结构本身中强制执行唯一性属性。 谨慎使用。
 *
 * <p>
 * Note that for equals/hashCode, the class implements the Set behavior (unordered), not the list
 * behavior (ordered); the fact that it subclasses ArrayList should be considered an implementation
 * detail.
 *
 * 请注意，对于equals / hashCode，该类实现了Set行为（无序），而不是list行为（有序）； 应该将其子类化ArrayList的事实视为实现细节。
 *
 * @param <E> the element type
 *
 * @author John V. Sichi
 */
public class ArrayUnenforcedSet<E>
    extends ArrayList<E>
    implements Set<E>
{
    private static final long serialVersionUID = -7413250161201811238L;

    /**
     * Constructs a new empty set
     *
     * 构造一个新的空集
     */
    public ArrayUnenforcedSet()
    {
        super();
    }

    /**
     * Constructs a set containing the elements of the specified collection.
     *
     * 构造一个包含指定集合的元素的集合。
     *
     * @param c the collection whose elements are to be placed into this set
     * @throws NullPointerException if the specified collection is null
     */
    public ArrayUnenforcedSet(Collection<? extends E> c)
    {
        super(c);
    }

    /**
     * Constructs an empty set with the specified initial capacity.
     *
     * 构造具有指定初始容量的空集。
     *
     * @param n the initial capacity of the set
     * @throws IllegalArgumentException if the specified initial capacity is negative
     */
    public ArrayUnenforcedSet(int n)
    {
        super(n);
    }

    @Override
    public boolean equals(Object o)
    {
        return new SetForEquality().equals(o);
    }

    @Override
    public int hashCode()
    {
        return new SetForEquality().hashCode();
    }

    /**
     * Multiple inheritance helper.
     */
    private class SetForEquality
        extends AbstractSet<E>
    {
        @Override
        public Iterator<E> iterator()
        {
            return ArrayUnenforcedSet.this.iterator();
        }

        @Override
        public int size()
        {
            return ArrayUnenforcedSet.this.size();
        }
    }
}
