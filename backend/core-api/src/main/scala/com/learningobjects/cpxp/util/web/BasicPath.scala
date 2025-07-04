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

package com.learningobjects.cpxp.util.web

import java.util as ju

import com.google.common.collect.ImmutableListMultimap
import scalaz.*
import scalaz.std.string.*
import scalaz.syntax.foldable.*
import scalaz.syntax.std.boolean.*
import scaloi.MultiMap.{empty as emptyMultiMap, *}
import scaloi.syntax.OptionOps.*

import scala.jdk.CollectionConverters.*
import scala.compat.java8.OptionConverters.*

final case class BasicPath(
  path: String,
  queryParams: MultiMap[String, String], // = emptyMultiMap,
) extends Path:

  // to Java, with pity
  def this(path: String) = this(path, emptyMultiMap)

  // append
  def /(segment: String): BasicPath = copy(path = s"$path/$segment")
  def /(segment: Long): BasicPath   = copy(path = s"$path/$segment")

  // prepend
  def /:(segment: String): BasicPath = copy(path = s"$segment/$path")

  def withOptionalQueryParam(key: String, value: Option[String]): BasicPath =
    value.map(v => (_: BasicPath).withQueryParam(key, v)) ~?> this

  def withOptionalQueryParam(key: String, value: ju.Optional[String]): BasicPath =
    withOptionalQueryParam(key, value.asScala)

  def withQueryParam(key: String, value: Any): BasicPath =
    withQueryParams(key -> value)

  def withQueryParams(kvs: (String, Any)*): BasicPath =
    copy(queryParams = queryParams.add(kvs.map(kv => (kv._1, kv._2.toString))*))

  def withQueryParams(params: MultiMap[String, String]): BasicPath =
    copy(queryParams = queryParams `combine` params)

  // because we can't get away from these
  def withMatrixParam[V /*: Show*/ ](key: String, value: V): BasicPath =
    withMatrixParams[Id.Id, V](key, value)

  def withMatrixParams[F[_]: Foldable, V /*: Show*/ ](key: String, values: F[V]): BasicPath =
    if values.empty then this
    else copy(path = s"$path;$key=${values.foldMap(v => s",$v").tail}")

  override def getPath        = path
  override def getQueryParams =
    queryParams
      .foldLeft(ImmutableListMultimap.builder[String, String]) { (builder, kvs) =>
        builder.putAll(kvs._1, kvs._2.asJava)
      }
      .build()

  override def toString = path + (queryParams.isEmpty !? {
    queryParams.distributed().map({ case (k, v) => s"$k=$v" }).mkString("?", "&", "")
  })
end BasicPath

object BasicPath:
  implicit class BasicPathStringOps(pathString: String):
    def toPath: BasicPath = new BasicPath(pathString)
