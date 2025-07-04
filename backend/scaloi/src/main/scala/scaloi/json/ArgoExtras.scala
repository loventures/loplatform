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

package scaloi
package json

import argonaut.*
import scalaz.*
import scalaz.std.lazylist.*
import scalaz.std.list.*
import scalaz.std.string.*
import scalaz.syntax.std.either.*
import scalaz.syntax.std.list.*
import scalaz.syntax.std.map.*
import scalaz.syntax.std.option.*
import scalaz.syntax.traverse.*
import scaloi.data.ListTree
import scaloi.syntax.StringOps.*

import java.sql.Timestamp
import java.time.format.DateTimeParseException
import java.time.{Instant, LocalDate, LocalDateTime, ZoneOffset}
import java.util.{Date, UUID}

object ArgoExtras:
  import Argonaut.*

  implicit final def treeCodec[V: EncodeJson: DecodeJson]: CodecJson[Tree[V]] =
    CodecJson(
      t => Json.jObjectFields("value" := t.rootLabel, "children" := t.subForest),
      hc =>
        for
          v        <- hc.downField("value").as[V]
          children <- hc.downField("children").as[EphemeralStream[Tree[V]]]
        yield Tree.Node(v, children)
    )

  implicit final def strictTreeCodec[V: EncodeJson: DecodeJson]: CodecJson[StrictTree[V]] =
    CodecJson(
      t => Json.jObjectFields("value" := t.rootLabel, "children" := t.subForest),
      hc =>
        for
          v        <- hc.downField("value").as[V]
          children <- hc.downField("children").as[Vector[StrictTree[V]]]
        yield StrictTree.Node(v, children)
    )

  implicit final def listTreeCodec[V: EncodeJson: DecodeJson]: CodecJson[ListTree[V]] =
    CodecJson(
      t => Json.jObjectFields("value" := t.rootLabel, "children" := t.subForest),
      hc =>
        for
          v        <- hc.downField("value").as[V]
          children <- hc.downField("children").as[List[ListTree[V]]]
        yield ListTree.Node(v, children)
    )

  implicit def estreamCodec[A: EncodeJson: DecodeJson]: CodecJson[EphemeralStream[A]] =
    CodecJson(
      t => jArray(t.map(EncodeJson.of[A].encode).toList),
      h =>
        for a <- h.as[List[A]]
        yield a.toEphemeralStream
    )

  implicit def nelCodec[A: EncodeJson: DecodeJson]: CodecJson[NonEmptyList[A]] =
    CodecJson(
      t => jArray(t.map(EncodeJson.of[A].encode).toList),
      h =>
        for
          a   <- h.as[List[A]]
          nel <- extractNel(h.history, a)
        yield nel
    )

  private def extractNel[A](h: CursorHistory, l: List[A]): DecodeResult[NonEmptyList[A]] =
    l.toNel.cata(
      DecodeResult.ok,
      DecodeResult
        .fail[NonEmptyList[A]](s"Failed to deserialize json array into a NonEmptyList, since there were no values", h)
    )

  implicit val longKeyEncoder: EncodeJsonKey[Long] = EncodeJsonKey.from(_.toString)

  def longMapCodec[V: {EncodeJson, DecodeJson}]: CodecJson[Map[Long, V]] =
    CodecJson.derived(using longMapEncode, longMapDecode)

  implicit def longMapEncode[V: EncodeJson]: EncodeJson[Map[Long, V]] =
    EncodeJson(_.mapKeys(_.toString).asJson)

  implicit def longMapDecode[V: DecodeJson]: DecodeJson[Map[Long, V]] =
    mapDecode(_.toLong_?)

  def mapCodec[K, V: {EncodeJson, DecodeJson}](
    encodeKey: K => String,
    decodeKey: String => Option[K],
  ): CodecJson[Map[K, V]] =
    def encode(m: Map[K, V]) = m.mapKeys(encodeKey).asJson
    CodecJson.derived(using EncodeJson(encode), mapDecode[K, V](decodeKey))

  def mapDecode[K, V: DecodeJson](decodeKey: String => Option[K]): DecodeJson[Map[K, V]] =
    DecodeJson { (hc: HCursor) =>
      hc.as[Map[String, V]].flatMap { stringMap =>
        DecodeResult.fromDisjunction(hc.history) {
          stringMap
            .to(LazyList)
            .traverseU({ case (keyStr, v) =>
              decodeKey(keyStr).toSuccessNel(keyStr).map(_ -> v)
            })
            .toDisjunction
            .bimap(
              invalidKeyStrs => s"invalid keys: (${invalidKeyStrs.intercalate(", ")})",
              keyValueTuples => keyValueTuples.toMap
            )
        }
      }
    }

  implicit final val instantCodec: CodecJson[Instant] = CodecJson(
    instant => Json.jString(instant.toString), // TODO: Explicitly format to whatever postgres prefers.
    c =>
      c.as[String]
        .flatMap(str =>
          \/.attempt(Instant.parse(str))(identity)
            .orElse(\/.attempt(LocalDateTime.parse(str).toInstant(ZoneOffset.UTC))(identity))
            .fold(
              {
                case e: DateTimeParseException => DecodeResult.fail(e.toString, c.history)
                case e                         => throw e
              },
              DecodeResult.ok
            )
        )
  )

  implicit final val codecJsonForDate: CodecJson[Date]                   = instantCodec.xmap(Date.from)(_.toInstant)
  implicit final val codecJsonForTimestamp: CodecJson[Timestamp]         = instantCodec.xmap(Timestamp.from)(_.toInstant)
  implicit final val codecJsonForLocalDateTime: CodecJson[LocalDateTime] =
    codecJsonForTimestamp.xmap(_.toLocalDateTime)(Timestamp.valueOf)

  implicit final val codecJsonForLocalDate: CodecJson[LocalDate] = CodecJson(
    ld => Json.jString(ld.toString),
    c =>
      c.as[String]
        .flatMap(str =>
          \/.attempt(LocalDate.parse(str))(identity)
            .fold(
              {
                case e: DateTimeParseException => DecodeResult.fail(e.toString, c.history)
                case e                         => throw e
              },
              DecodeResult.ok
            )
        )
  )

  implicit final val encodeJsonKeyForUuid: EncodeJsonKey[UUID] = EncodeJsonKey.from(_.toString)
  implicit final val encodeJsonKeyForLong: EncodeJsonKey[Long] = EncodeJsonKey.from(_.toString)

  implicit class DecodeResultOps[A](private val dr: DecodeResult[A]) extends AnyVal:
    def mapHint(hinter: String => String): DecodeResult[A] =
      dr.fold((msg, ch) => DecodeResult.fail(hinter(msg), ch), DecodeResult.ok)
    def withHint(hint: String): DecodeResult[A]            = mapHint(_ => hint)
    def widen[AA >: A]: DecodeResult[AA]                   = dr.asInstanceOf[DecodeResult[AA]]

  implicit class DecodeResultCompanionOps(private val dr: DecodeResult.type) extends AnyVal:
    def fromDisjunction[A](h: CursorHistory)(disj: String \/ A): DecodeResult[A] =
      disj.fold(dr.fail(_, h), dr.ok)

  implicit class ParseOps(private val self: Parse.type) extends AnyVal:
    def parse_\/(value: String): String \/ Json =
      self.parse(value).toDisjunction

    def decode_\/[X: DecodeJson](value: String): String \/ X =
      self.decodeEither[X](value).toDisjunction

  def unapply1[P <: Product: scala.deriving.Mirror.ProductOf](p: P) = Option(Tuple.fromProductTyped(p)(0))

  def unapply[P <: Product: scala.deriving.Mirror.ProductOf](p: P) = Option(Tuple.fromProductTyped(p))
end ArgoExtras
