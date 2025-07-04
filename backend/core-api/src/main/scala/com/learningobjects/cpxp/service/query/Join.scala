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

package com.learningobjects.cpxp.service.query

/** A join to a `QueryBuilder`.
  *
  * Applying a `Join` to a base `QueryBuilder` has the effect of causing the entity queried by `query` to be joined to
  * the base query being built, joining `query`'s `rightDataType` against the datum specified by `leftDataType` on the
  * base query.
  *
  * Left and right outer joins can contain additional conditions to be applied in the join's `ON` clause rather than in
  * the outermost `WHERE` clause. This is necessary where you want the joined rows to be filtered *before* the join is
  * calculated, so filtered rows on one side do not filter their joined rows.
  */
sealed trait Join:
  def leftDataType: String
  def query: QueryBuilder
  def rightDataType: String
  def conditions: DNF
  private[query] def joinWord: String

object Join:
  case class Inner(
    leftDataType: String,
    query: QueryBuilder,
    rightDataType: String = "#id"
  ) extends Join:
    override val conditions = DNF.empty
    override val joinWord   = " JOIN "

    // for Java
    def this(leftDataType: String, query: QueryBuilder) =
      this(leftDataType, query, "#id")
  end Inner

  case class Left(
    leftDataType: String,
    query: QueryBuilder,
    rightDataType: String = "#id",
    conditions: DNF = DNF.empty
  ) extends Join:
    override val joinWord = " LEFT JOIN "

    // for Java

    def this(leftDataType: String, query: QueryBuilder) =
      this(leftDataType, query, "#id", DNF.empty)
    def this(leftDataType: String, query: QueryBuilder, condition: Condition) =
      this(leftDataType, query, "#id", DNF(condition))

    def this(leftDataType: String, query: QueryBuilder, conditions: DNF) =
      this(leftDataType, query, "#id", conditions)
  end Left

  case class Right(
    leftDataType: String,
    query: QueryBuilder,
    rightDataType: String = "#id",
    conditions: DNF = DNF.empty
  ) extends Join:
    override val joinWord = " RIGHT JOIN "

    // for Java
    def this(leftDataType: String, query: QueryBuilder) =
      this(leftDataType, query, "#id", DNF.empty)
    def this(leftDataType: String, query: QueryBuilder, condition: Condition) =
      this(leftDataType, query, "#id", DNF(condition))
    def this(leftDataType: String, query: QueryBuilder, conditions: DNF) =
      this(leftDataType, query, "#id", conditions)
  end Right
end Join
