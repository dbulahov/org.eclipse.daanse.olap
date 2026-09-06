/*
 * This software is subject to the terms of the Eclipse Public License v1.0
 * Agreement, available at the following URL:
 * http://www.eclipse.org/legal/epl-v10.html.
 * You must accept the terms of that agreement to use this software.
 *
 * Copyright (c) 2002-2017 Hitachi Vantara..  All rights reserved.
 * ---- All changes after Fork in 2023 ------------------------
 * 
 * Project: Eclipse daanse
 * 
 * Copyright (c) 2023 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors after Fork in 2023:
 *   SmartCity Jena - initial
 *   Stefan Bischof (bipolis.org) - initial
 */

package org.eclipse.daanse.olap.api.cache;

import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

import org.eclipse.daanse.olap.api.element.Cube;
import org.eclipse.daanse.olap.api.element.Dimension;
import org.eclipse.daanse.olap.api.element.Level;
import org.eclipse.daanse.olap.api.element.Member;

/**
 * API for controlling the contents of the cell cache and the member cache. A
 * CellRegion denotes a portion of the cell cache, and a MemberSet denotes a
 * portion of the member cache. Both caches can be flushed, and the member cache
 * can be edited.
 *
 * To create an instance of this interface, use
 * org.eclipse.daanse.olap.api.Connection#getCacheControl.
 *
 * Methods concerning cell cache: createMemberRegion(Member, boolean)
 * createMemberRegion(boolean, Member, boolean, Member, boolean)
 * createUnionRegion(CellRegion[])
 * createCrossjoinRegion(CellRegion[])
 * createMeasuresRegion(Cube)
 * flush(CellRegion)
 *
 * Methods concerning member cache: createMemberSet(Member, boolean)
 * createMemberSet(boolean, Member, boolean, Member, boolean)
 * createAddCommand(Member) createDeleteCommand(Member)
 * createDeleteCommand(MemberSet)
 * createCompoundCommand(java.util.List)
 * createCompoundCommand(MemberEditCommand[])
 * createSetPropertyCommand(Member, String, Object)
 * createSetPropertyCommand(MemberSet,java.util.Map)
 * flush(MemberSet)
 * execute(MemberEditCommand)
 *
 * @author jhyde
 * @since Sep 27, 2006
 */
public interface CacheControl {

    // cell cache control

    /**
     * Creates a cell region consisting of a single member.
     *
     * @param member      the member
     * @param descendants When true, include descendants of the member in the
     *                    region.
     * @return the new cell region
     */
    CellRegion createMemberRegion(Member member, boolean descendants);

    /**
     * Creates a cell region consisting of a range between two members.
     *
     * The members must belong to the same level of the same hierarchy. One of the
     * bounds may be null.
     *
     * For example, given
     *
     * Member member97Q3; // [Time].[1997].[Q3] Member member98Q2; //
     * [Time].[1998].[Q2]
     *
     * then
     *
     * Expression Meaning
     *
     * createMemberRegion(true, member97Q3, true, member98Q2, false) The members
     * between 97Q3 and 98Q2, inclusive: [Time].[1997].[Q3], [Time].[1997].[Q4],
     * [Time].[1998].[Q1], [Time].[1998].[Q2]
     *
     * createMemberRegion(true, member97Q3, false, member98Q2, false)
     *
     * The members between 97Q3 and 98Q2, exclusive: [Time].[1997].[Q4],
     * [Time].[1998].[Q1]
     *
     * createMemberRegion(true, member97Q3, false, member98Q2, false)
     * 
     * The members between 97Q3 and 98Q2, including their descendants, and including
     * the lower bound but not the upper bound: [Time].[1997].[Q3],
     * [Time].[1997].[Q3].[7], [Time].[1997].[Q3].[8], [Time].[1997].[Q3].[9],
     * [Time].[1997].[Q4], [Time].[1997].[Q4].[10], [Time].[1997].[Q4].[11],
     * [Time].[1997].[Q4].[12], [Time].[1998].[Q1], [Time].[1998].[Q1].[1],
     * [Time].[1998].[Q1].[2], [Time].[1998].[Q1].[3]
     *
     * @param lowerInclusive whether the the range includes the lower bound; ignored
     *                       if the lower bound is not specified
     * @param lowerMember    lower bound member. If null, takes all preceding
     *                       members
     * @param upperInclusive whether the the range includes the upper bound; ignored
     *                       if the upper bound is not specified
     * @param upperMember    upper bound member. If null, takes all preceding
     *                       members
     * @param descendants    when true, include descendants of the member in the
     *                       region
     * @return the new cell region
     */
    CellRegion createMemberRegion(boolean lowerInclusive, Member lowerMember, boolean upperInclusive,
            Member upperMember, boolean descendants);

    /**
     * Forms the cartesian product of two or more cell regions.
     * 
     * @param regions the operands
     * @return the cartesian product of the operands
     */
    CellRegion createCrossjoinRegion(CellRegion... regions);

    /**
     * Forms the union of two or more cell regions. The regions must have the same
     * dimensionality.
     *
     * @param regions the operands
     * @return the cartesian product of the operands
     * 
     */
    CellRegion createUnionRegion(CellRegion... regions);

    /**
     * Creates a region consisting of all measures in a given cube.
     * 
     * @param cube a cube
     * @return the region
     */
    CellRegion createMeasuresRegion(Cube cube);

    /**
     * Atomically flushes all the cells in the cell cache that correspond to
     * measures in a cube and to a given region.
     *
     * @param region a region
     */
    void flush(CellRegion region);

    /**
     * Prints the state of the cell cache as it pertains to a given region.
     * 
     * @param pw     the output target
     * @param region the CellRegion of interest
     */
    void printCacheState(PrintWriter pw, CellRegion region);

    // member cache control

    /**
     * Creates a member set containing either a single member, or a member and its
     * descendants.
     * 
     * @param member      a member
     * @param descendants when true, include descendants in the set
     * @return the set
     */
    MemberSet createMemberSet(Member member, boolean descendants);

    /**
     * Creates a member set consisting of a range between two members. The members
     * must belong to the same level of the same hierarchy. One of the bounds may be
     * null. (Similar to #createMemberRegion(boolean, Member, boolean, Member,
     * boolean), which see for examples.)
     *
     * @param lowerInclusive whether the the range includes the lower bound; ignored
     *                       if the lower bound is not specified
     * @param lowerMember    lower bound member. If null, takes all preceding
     *                       members
     * @param upperInclusive whether the the range includes the upper bound; ignored
     *                       if the upper bound is not specified
     * @param upperMember    upper bound member. If null, takes all preceding
     *                       members
     * @param descendants    when true, include descendants of the member in the
     *                       region
     * @return the set
     */
    MemberSet createMemberSet(boolean lowerInclusive, Member lowerMember, boolean upperInclusive, Member upperMember,
            boolean descendants);

    /**
     * Forms the union of two or more member sets.
     *
     * @param sets the operands
     * @return the union of the operands
     */
    MemberSet createUnionSet(MemberSet... sets);

    /**
     * Filters a member set, keeping all members at a given Level.
     *
     * @param level   Level
     * @param baseSet Member set
     * @return Member set with members not at the given level removed
     */
    MemberSet filter(Level level, MemberSet baseSet);

    /**
     * Atomically flushes all members in the member cache which belong to a given
     * set.
     *
     * @param set a set of members
     */
    void flush(MemberSet set);

    // edit member cache contents

    /**
     * Executes a command that edits the member cache.
     * 
     * @param cmd the command
     */
    void execute(MemberEditCommand cmd);

    /**
     * Builds a compound command which is executed atomically.
     *
     * @param cmds a list of the component commands
     * @return the compound command
     */
    MemberEditCommand createCompoundCommand(List<MemberEditCommand> cmds);

    /**
     * Builds a compound command which is executed atomically.
     * 
     * @param cmds the component commands
     * @return the compound command
     */
    MemberEditCommand createCompoundCommand(MemberEditCommand... cmds);

    // commands to change the structure of the member cache

    /**
     * Creates a command to delete a member and its descendants from the member
     * cache.
     *
     * @param member the member
     * @return the command
     */
    MemberEditCommand createDeleteCommand(Member member);

    /**
     * Creates a command to delete a set of members from the member cache.
     *
     * @param memberSet the set
     * @return the command
     */
    MemberEditCommand createDeleteCommand(MemberSet memberSet);

    /**
     * Creates a command to add a member to the cache. The added member and its
     * parent must have the same Dimension and the correct Levels, Null parent means
     * add to the top level of its Dimension.
     *
     * The ordinal position of the new member among its siblings is implied by its
     * properties.
     *
     * @param member the new member
     * @return the command
     *
     * @throws IllegalArgumentException if member null or if member belongs to a
     *                                  parent-child hierarchy
     */
    MemberEditCommand createAddCommand(Member member) throws IllegalArgumentException;

    /**
     * Creates a command to Move a member (with its descendants) to a new location,
     * that is to a new parent.
     * 
     * @param member the member moved
     * @param loc    the new parent
     * @return the command
     *
     * @throws IllegalArgumentException if member is null, or loc is null, or member
     *                                  belongs to a parent-child hierarchy, or if
     *                                  loc is incompatible with member
     */
    MemberEditCommand createMoveCommand(Member member, Member loc) throws IllegalArgumentException;

    // commands to change member properties

    /**
     * Creates a command to change one property of a member.
     *
     * @param member the member
     * @param name   the property name
     * @param value  the property value
     * @return the command
     * @throws IllegalArgumentException if the property is invalid for the member
     */
    MemberEditCommand createSetPropertyCommand(Member member, String name, Object value)
            throws IllegalArgumentException;

    /**
     * Creates a command to several properties changes over a set of members. All
     * members must belong to the same Level.
     *
     * @param set            the set of members
     * @param propertyValues Collection of property-value pairs
     * @return the command
     * @throws IllegalArgumentException for an invalid property, or if all members
     *                                  in the set do not belong to the same Level.
     */
    MemberEditCommand createSetPropertyCommand(MemberSet set, Map<String, Object> propertyValues)
            throws IllegalArgumentException;

    // other

    /**
     * Prints a debug message.
     *
     * @param message the message
     */
    void trace(String message);

    /**
     * Tells if tracing is enabled.
     */
    boolean isTraceEnabled();

    /**
     * Flushes the cache which maps schema URLs to metadata.
     *
     * This cache is referenced only when creating a new connection, so existing
     * connections will continue to use the same schema definition.
     *
     * Flushing the schema cache will flush all aggregations and segments associated
     * to it as well.
     */
    void flushSchemaCache();

    /** a region of cells in the cell cache */
    public interface CellRegion {
        /**
         * Returns the dimensionality of a region.
         * 
         * @return a list of org.eclipse.daanse.olap.api.element.Dimension objects.
         */
        List<Dimension> getDimensionality();
    }

    /**
     * A specification of a set of members in the member cache.
     *
     * Member sets can be created using methods CacheControl#createMemberSet(Member,
     * boolean), CacheControl#createMemberSet(boolean, Member, boolean, Member,
     * boolean),
     * CacheControl#createUnionSet(MemberSet[]).
     */
    public interface MemberSet {
    }

    /**
     * An operation to be applied to the member cache. The operation does not take
     * effect until CacheControl#execute is called.
     */
    public interface MemberEditCommand {
    }

}
