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

package loi.cp.imports

import argonaut.*
import argonaut.Argonaut.*
import scaloi.json.ArgoExtras.*

/** A representation of an importable data item that is validatable & persistable into a Difference Engine instance.
  * Known subclasses are: [[UserImportItem]], [[EnrollmentImportItem]], [[ConnectionImportItem]],
  * [[EnrollmentImportItem]], [[LearnerTransferImportItem]], and [[CourseImportItem]]
  */
abstract class ImportItem(final val _type: String)

object ImportItem:
  import language.implicitConversions

  /* This is only necessary because the import item types are spread across
   * multiple files. It would be a cool thing if we didn't have to list the
   * cases, but that's beyond the power of both argonaut and Jackson. */
  implicit val decoder: DecodeJson[ImportItem] = DecodeJson[ImportItem] { hc =>
    hc.as[Json] match
      case DecodeResult(Right(obj)) if obj.isObject =>
        obj.field("_type").flatMap(_.string) match
          case Some(_type) =>
            _type match
              case ConnectionImportItem.Type      => hc.as[ConnectionImportItem].widen
              case CourseImportItem.Type          => hc.as[CourseImportItem].widen
              case EnrollmentImportItem.Type      => hc.as[EnrollmentImportItem].widen
              case LearnerTransferImportItem.Type => hc.as[LearnerTransferImportItem].widen
              case UserImportItem.Type            => hc.as[UserImportItem].widen
              case _                              =>
                DecodeResult.fail(s"Unknown import type ${_type}", hc.history)
          case None        =>
            DecodeResult.fail("Cannot determine import item type as it has no `_type` field", hc.history)
      case DecodeResult(Right(_))                   =>
        DecodeResult.fail("ImportItems should be objects", hc.history)
      case DecodeResult(Left(err))                  =>
        DecodeResult.fail(err._1, err._2)
  }

  implicit val encoder: EncodeJson[ImportItem] = EncodeJson[ImportItem] {
    case conn: ConnectionImportItem      => ("_type" := ConnectionImportItem.Type) ->: conn.asJson
    case course: CourseImportItem        => ("_type" := CourseImportItem.Type) ->: course.asJson
    case enroll: EnrollmentImportItem    => ("_type" := EnrollmentImportItem.Type) ->: enroll.asJson
    case xfer: LearnerTransferImportItem => ("_type" := LearnerTransferImportItem.Type) ->: xfer.asJson
    case user: UserImportItem            => ("_type" := UserImportItem.Type) ->: user.asJson
    case unknown                         => throw new MatchError(unknown)
  }
end ImportItem
