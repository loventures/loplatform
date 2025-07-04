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

package de

import java.sql.{Connection, Statement}
import javax.sql.DataSource

/** DE monitoring package object.
  */
package object mon:

  /** Evidence that a proxied statement should monitor executed SQL. */
  implicit val ImplicitStatementProxy: Proxied[Statement] = (s: Statement, args: Array[AnyRef]) =>
    new StatementProxy(s, args)

  /** Evidence that a proxied connection should wrap statements. */
  implicit val ConnectionProxy: Proxied[Connection] = WrappingProxy[Connection, Statement]

  /** Evidence that a proxied data source should wrap connections. */
  implicit val DataSourceProxy: Proxied[DataSource] = WrappingProxy[DataSource, Connection]

  /** A tuple of a statistic type and the entity affected. */
  type EntityStatistic = (StatisticType, String)
end mon
