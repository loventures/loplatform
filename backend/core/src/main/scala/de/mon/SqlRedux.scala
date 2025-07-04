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

package de.mon

import scala.util.matching.Regex

/** Helper for manipulating SQL strings.
  */
object SqlRedux:

  /** Simplify a SQL string. If it looks like a Hibernate entity fetch, rewrite it as select * from...
    * @param sql
    *   the original SQL
    * @return
    *   the simplified SQL
    */
  def simplify(sql: String): String = sql match
    case FetchRe(tail) => s"select * $tail"
    case s             => s

  /** Pattern match for hibernate entity fetches. */
  private final val FetchRe =
    """select [_A-Za-z0-9]+\.(?:owner_)?id as [_A-Za-z0-9]+(?:, [_A-Za-z0-9]+\.[_A-Za-z0-9]+ as [_A-Za-z0-9]+)* (from [_A-Za-z0-9]+ [_A-Za-z0-9]+ where [_A-Za-z0-9]+\.(?:owner_)?id=\?)""".r

  /** Attempt to parse a SQL statement into a statement type and the table on which it is operating.
    *
    * @param sql
    *   the sql (should be lower-cased)
    * @return
    *   the matching table statement
    */
  def tableStatement(sql: String): EntityStatistic =
    sqlMatchers.view.flatMap(accept(sql.trim)).headOption getOrElse {
      if !sql.toLowerCase().startsWith("explain") then logger warn s"Unable to parse SQL: $sql"
      StatisticType.UnknownSQL -> "?"
    }

  /** Test a SQL string against a regex to see if it matches a statement type.
    * @param sql
    *   the SQL string
    * @param tuple
    *   a regex and the corresponding statement type
    * @return
    *   the matching table statement, if any
    */
  private def accept(sql: String)(tuple: (Regex, StatisticType)): Option[EntityStatistic] =
    tuple._1.findFirstMatchIn(sql).map(tuple._2 -> _.group(1))

  /** The known SQL statement regexes. This should be ordered for first match and expected frequency.
    */
  private final val sqlMatchers = Seq(
    "(?s)^select\\s+.*\\s+from\\s+([a-z0-9_]+).*\\s+for\\s+update".r                            -> StatisticType.Lock,
    "(?s)^select\\s+.*\\s+from\\s+(?:\"public\".)?\"?([a-z0-9_]+)\"?".r                         -> StatisticType.Select,
    "^update\\s+([a-z0-9_]+)\\s+".r                                                             -> StatisticType.Update,
    "^insert\\s+into\\s+([a-z0-9_]+)\\s+".r                                                     -> StatisticType.Insert,
    "^delete\\s+from\\s+([a-z0-9_]+)\\s+".r                                                     -> StatisticType.Delete,
    "^select\\s+nextval\\s*\\('([a-z0-9_]+)'\\)".r                                              -> StatisticType.Select,
    "(?s)^with\\s+.*select()".r                                                                 -> StatisticType.Cte,
    "^create\\s+table\\s+([a-z0-9_]+)\\s+".r                                                    -> StatisticType.Ddl,
    "^drop\\s+table\\s+(?:if exists\\s+)?([a-z0-9_]+)\\s+".r                                    -> StatisticType.Ddl,
    "^alter\\s+table\\s+(?:public\\.)?([a-z0-9_]+)\\s+".r                                       -> StatisticType.Ddl,
    "^create\\s+index\\s+(?:concurrently\\s+)?(?:if not exists\\s+)?.*\\s+on\\s+([a-z0-9_]+)".r -> StatisticType.Ddl,
    "^drop\\s+index\\s+(?:concurrently\\s+)?(?:if exists\\s+)?([a-z0-9]+)".r                    -> StatisticType.Ddl,
    "^create\\s+language\\s+([a-z0-9_]+)".r                                                     -> StatisticType.Ddl,
    "^set\\s+([a-z0-9_]+)".r                                                                    -> StatisticType.Ddl
  )

  // TODO: CTE... with alias as (...) select columns from name(, name)*
  // joins? multi-table selects?

  /** The logger. */
  private final val logger = org.log4s.getLogger
end SqlRedux
