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

import com.learningobjects.cpxp.util.proxy.ProxyInvocationHandler

import java.lang.reflect.Method
import java.sql.Statement

/** Invocation handler that wraps a `Statement` and monitors all executed SQL.
  *
  * @param s
  *   the statement
  * @param sa
  *   the arguments used to create the statement
  */
private[mon] class StatementProxy(s: Statement, sa: Array[AnyRef]) extends ProxyInvocationHandler[Statement](s):

  /** Whether this looks like a prepared statement (single string used to create it). */
  private val looksPrepared = (sa ne null) && (sa.length == 1) && sa.head.isInstanceOf[String]

  /*
   * Statement has a series of execute*(sql: String) methods as well as executeBatch().
   * PreparedStatement extends Statement with a series of execute() methods.
   * CallableStatement extends PreparedStatement.
   */

  /** Invoke the method and then, if it looked like a SQL execution, record the statement and runtime in the
    * de-monitoring statistics.
    */
  override def invokeImpl(method: Method, args: Array[AnyRef]): AnyRef =
    if method.getName.startsWith("execute") then
      val start = System.nanoTime
      try method.invoke(s, args*)
      finally
        val runtime = System.nanoTime - start
        if (args ne null) && (args.length == 1) && args.head.isInstanceOf[String] then
          // executing sql directly.. in theory i should defang literals into ?
          DeMonitor.sqlExecuted(args.head.asInstanceOf[String], runtime)
        else if looksPrepared then
          // executing prepared sql
          DeMonitor.sqlExecuted(sa.head.asInstanceOf[String], runtime)
    else method.invoke(s, args*)
end StatementProxy
