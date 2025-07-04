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

import com.google.common.cache.{CacheBuilder, CacheLoader}
import org.apache.commons.lang3.ClassUtils

import java.lang.reflect.Proxy
import javax.sql.DataSource
import scala.jdk.CollectionConverters.*

/** Support for proxying things that should be monitored.
  */
object MonitorProxy:

  /** Wrap a data source in a proxy that monitors it.
    *
    * @param ds
    *   the data source
    * @return
    *   the proxied data source
    */
  def proxy(ds: DataSource): DataSource = apply(ds, Array())

  /** Wrap any proxyable value in a proxy that monitors it.
    *
    * @param a
    *   the value to proxy
    * @param args
    *   the arguments used to construct the value
    * @tparam A
    *   the value type with `Proxied` evidence
    * @return
    *   the proxied value
    */
  def apply[A <: AnyRef: Proxied](a: A, args: Array[AnyRef]): A =
    Proxy
      .newProxyInstance(getClass.getClassLoader, interfaces.get(a.getClass), Proxied.handler(a, args))
      .asInstanceOf[A]

  /** Cache from a class to all the interfaces implemented by the class. */
  private final val interfaces =
    CacheBuilder.newBuilder `build` new CacheLoader[Class[?], Array[Class[?]]]:
      override def load(key: Class[?]): Array[Class[?]] = ClassUtils.getAllInterfaces(key).asScala.toArray
end MonitorProxy
