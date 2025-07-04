/*
 * LO Platform copyright (C) 2007–2025 LO Ventures LLC.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.learningobjects.cpxp.service.relationship;

/**
 * Relationship structures.
 */
public enum RelationshipStructure {
    All, // registered and no
    Registered, // registered only
    Direct, // Sybil and Eve

    LinkedFrom, // From the origin; e.g. My friends / Math101 students / Wiki owners
    LinkedTo, // To the origin; e.g. My advisors
        /* A link exists from the origin to an item and from that item to the caller. */
    LinkedFromTo, // From the origin, to the user; e.g. Wiki owner(group) members,  Wiki owner(user) friends
    LinkedFromFrom, // From the origin, from the user; e.g. Wiki owner(user)'s advisors
    LinkedToFrom, // To the origin, from the user;
    LinkedToTo; // To the origin, to the user; e.g. My classmates
}
