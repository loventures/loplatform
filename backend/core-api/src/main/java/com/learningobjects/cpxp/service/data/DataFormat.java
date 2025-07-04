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

package com.learningobjects.cpxp.service.data;

// TODO: composite data format?

/**
 * The format of data associated with a data type.
 */
public enum DataFormat {
    /** short indexed string */
    string,
    /** long unindexed string */
    text,
    /** number data */
    number,
    /** double precision number */
    DOUBLE, // loud
    /** time */
    time,
    /** boolean */
    bool,
    /** item reference */
    item,
    /** materialized item path */
    path,
    /** json (postgresql 9.3) */
    json,
    /** tsvector */
    tsvector,
    /** UUID */
    uuid
}
