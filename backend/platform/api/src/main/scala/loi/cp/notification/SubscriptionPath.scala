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

package loi.cp.notification

import argonaut.{CodecJson, DecodeJson, EncodeJsonKey}

/** A subscription path describes the logical path to a notification that users can subscribe to receive. Users can
  * subscribe to any prefix and will receive the notification. A typical subscription path would be a discussion board
  * id and a discussion thread id.
  */
final case class SubscriptionPath(path: List[String]):

  assert(path.forall(_.indexOf('/') < 0), s"subscription path elements may not contain '/': $path")

  /** Add an element to this subscription path. */
  def +(el: String): SubscriptionPath = SubscriptionPath(path :+ el)

  /** Return all the prefixes of this subscription path. */
  def ancestry: Iterator[SubscriptionPath] =
    path.inits.map(SubscriptionPath.apply)

  /** Return whether this subscription path includes a notification path. */
  def includes(other: SubscriptionPath): Boolean = other.path.startsWith(path)

  /** Convert to a string. */
  override def toString: String = path.mkString("", "/", "/")
end SubscriptionPath

object SubscriptionPath extends SubscriptionPathCodecs:
  final val empty = SubscriptionPath(Nil)

  def apply(elements: String*): SubscriptionPath =
    SubscriptionPath(elements.toList)

  def apply(path: Long): SubscriptionPath =
    SubscriptionPath(List(path.toString))

  def apply(path0: Long, path1: Long): SubscriptionPath =
    SubscriptionPath(List(path0, path1).map(_.toString))

  def parse(path: String): SubscriptionPath =
    SubscriptionPath(path.split("/").toList)
end SubscriptionPath

trait SubscriptionPathCodecs:
  implicit val subscriptionPathCodec: CodecJson[SubscriptionPath] =
    CodecJson.derived[String].xmap(SubscriptionPath.parse)(_.toString)

  implicit val subscriptionPathKeyCodec: EncodeJsonKey[SubscriptionPath] =
    EncodeJsonKey.from[SubscriptionPath](_.toString)

  implicit def subscriptionPathMapDecodeJson[T](implicit
    decode: DecodeJson[Map[String, T]]
  ): DecodeJson[Map[SubscriptionPath, T]] =
    DecodeJson(json => decode(json).map(_.map(t => SubscriptionPath.parse(t._1) -> t._2)))
end SubscriptionPathCodecs
