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

package loi.cp.imports.importers

import argonaut.*
import argonaut.Argonaut.*
import com.learningobjects.cpxp.scala.json.{Absent, Null, OptionalField, Present}
import com.learningobjects.cpxp.scala.util.I18nMessage
import loi.cp.imports.*
import loi.cp.imports.errors.*
import scalaz.*
import scalaz.std.list.*
import scalaz.syntax.either.*
import scalaz.syntax.std.map.*
import scalaz.syntax.std.option.*
import scalaz.syntax.validation.*

import java.time.Instant
import java.util.Date
import scala.util.{Failure, Success}

trait DeserializingCsvImporter extends ImporterConverters:

  val log: org.log4s.Logger

  def ifHeadersMatchValues[Item <: ImportItem](headers: Seq[String], values: Seq[String])(
    f: (CsvRow) => ValidationError \/ Item
  ): ValidationError \/ Item =
    if headers.lengthCompare(values.length) < 0 then
      ValidationError(List(ItemViolation(I18nMessage("Too many value columns")))).left
    else if headers.lengthCompare(values.length) > 0 then
      ValidationError(List(ItemViolation(I18nMessage("Too few columns")))).left
    else
      // convert headers & values to a map, where headers are the keys
      f(CsvRow((headers zip values).toMap))

  /** Perform json deserialization with predefined violation if failure.
    */
  def deserializeJson[T: DecodeJson](value: String)(ifError: => Violation): ViolationNel[T] =
    deserializeJson[T, T](value, identity[T])(ifError)

  /** Perform json deserialization and transform the result with specified function, with predefined violation if
    * failure.
    */
  def deserializeJson[T: DecodeJson, U](value: String, transform: T => U)(ifError: => Violation): ViolationNel[U] =
    value
      .decodeOption[T]
      .toSuccessNel(ifError)
      .map(transform)

  /** Converts a string to an Instant, or returns a validation error
    *
    * @param date
    *   an ISO-8601-compatible string
    */
  def parseInstant(colName: String)(date: String): ViolationNel[Instant] =
    \/.attempt(Instant.parse(date))(t =>
      FieldViolation(colName, date, s"$colName was not formatted appropriately")
    ).toValidationNel

  case class CsvRow(map: Map[String, String]):

    val lowercaseMap = map.mapKeys(_.toLowerCase)

    private def column(colName: String): Option[String] =
      lowercaseMap.get(colName.toLowerCase)

    def mapHeaders(hm: String => String): CsvRow =
      CsvRow(map mapKeys hm)

    /** Returns optional value for column. Here's what to expect:
      *   - Value not present... None
      *   - Value present but null... Some(None)
      *   - Value present but empty... Some(None)
      *   - Value present and non-empty... Some(Some(a))
      */
    def getOptional(colName: String): Option[Option[String]] =
      column(colName).map {
        case null                           => None // null
        case a: String if a.trim.equals("") => None // empty
        case a: String                      => Some(a)
      }

    /** Returns optional value for column. Similar to [[getOptional]], but returns OptionalField value. Here's what to
      * expect:
      *   - Value not present... Absent
      *   - Value present but null... Null
      *   - Value present but empty... Null
      *   - Value present and non-empty... Present(a)
      */
    def getOptionalField(colName: String): OptionalField[String] =
      getOptional(colName) match
        case None          => Absent()
        case Some(None)    => Null() // null or empty
        case Some(Some(a)) => Present(a)

    def failIfNone(colName: String): ViolationNel[String] =
      column(colName) match
        case Some(value) => value.successNel
        case None        =>
          FieldViolation(colName, "", s"required field $colName was not included").widen.failureNel

    def getOptionalDate(colName: String): ViolationNel[OptionalField[Date]] =
      getOptionalField(colName) match
        case Absent()      => Absent[Date]().successNel
        case Null()        => Null[Date]().successNel
        case Present(date) =>
          val violation = FieldViolation(colName, date, s"$colName was not formatted appropriately")
          deserializeJson[String, OptionalField[Date]](
            value = s""""$date"""",
            transform = (str) =>
              stringToDate(str) match
                case Success(d) => Present(d)
                case Failure(e) => throw e
          ) {
            violation
          }

    def getOptionalInstant(colName: String): ViolationNel[OptionalField[Instant]] =
      getOptionalDate(colName).map(_.map(_.toInstant))
  end CsvRow
end DeserializingCsvImporter
