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

import com.learningobjects.cpxp.scala.json.{Absent, OptionalField}
import scalaz.syntax.std.map.*
import scaloi.syntax.AnyOps.*

final case class EnrollmentImportItem(
  // .......... user ..........

  // Identify user by userName
  userName: OptionalField[String] = Absent(),

  // Identify user by externalId
  userExternalId: OptionalField[String] = Absent(),

  // Identify user by connector
  userIntegration: Option[IntegrationImportItem] = None,

  // .......... group ..........

  // Identify group by groupId
  groupId: OptionalField[String] = Absent(),

  // Identify group by externalId
  groupExternalId: OptionalField[String] = Absent(),

  // Identify group by connector
  groupIntegration: Option[IntegrationImportItem] = None,

  // .......... enrollment ..........
  role: String = "",
  departmentUniqueId: OptionalField[String] = Absent(),
  startTime: OptionalField[String] = Absent(),
  endTime: OptionalField[String] = Absent(),
  status: EnrollmentImportItemStatus = EnrollmentImportItemStatus.enabled,
) extends ImportItem(EnrollmentImportItem.Type)

object EnrollmentImportItem:
  final val Type = "Enroll"

  import argonaut.*

  /* Enrollment imports used to have course((External)?Id|Integration) fields
   * instead of the group-... fields currently on it. In the interest of backwards
   * compatibility, we'll try really hard to make the old format still work. */

  /** A map from the name which previously worked to the new name which should be used. */
  val deprecatedNameMap: Map[String, String] = Map(
    "courseId"                     -> "groupId",
    "courseExternalId"             -> "groupExternalId",
    "courseIntegration"            -> "groupIntegration",
    /* these next two shouldn't show up in JSON, but they will in CSV and it's
     * easier just to keep them with the rest of the renaming stuff */
    "courseIntegrationUniqueId"    -> "groupIntegrationUniqueId",
    "courseIntegrationConnectorId" -> "groupIntegrationConnectorId",
  ) withDefault identity

  /** Is a field name a new field name (the replacement of a deprecated name) and thus flags the import item as desirous
    * of the new semantics?
    */
  private val isNewName: String => Boolean =
    deprecatedNameMap.values.toSet

  /** A decoder which decodes using the standard case class decoder if any new-style field names are seen, and accepts
    * the old-style names otherwise.
    */
  private val backwardsCompatibleDecode: DecodeJson[EnrollmentImportItem] =
    /* Since we have optional fields for everything, the standard decoder
     * will always succeed even if none of the course- keys are specified.*/
    val standardDecode = DecodeJson
      .derive[EnrollmentImportItem]
      .validate(_.focus.obj.exists(_.fieldSet.exists(isNewName)), "standard group ids missing")

    val deprecatedDecode = standardDecode.flatMapCursor {
      _.withFocus(_.withObject {
        _.toMap.mapKeys(deprecatedNameMap) |> JsonObject.fromIterable
      }) |> DecodeResult.ok
    }

    standardDecode ||| deprecatedDecode
  end backwardsCompatibleDecode

  implicit val codec: CodecJson[EnrollmentImportItem] =
    CodecJson.derived(using
      E = EncodeJson.derive[EnrollmentImportItem],
      D = backwardsCompatibleDecode
    )
end EnrollmentImportItem
