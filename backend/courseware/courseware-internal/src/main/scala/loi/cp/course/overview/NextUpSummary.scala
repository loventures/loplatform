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

package loi.cp.course
package overview

import argonaut.CodecJson
import loi.asset.resource.model.Resource1
import loi.cp.content.*

/** A POJO representing the minimum data you would need to present for the next up content item in a course listing
  * page.
  */
final case class NextUpSummary(
  id: String,             // edge path for lwc, pk for hwc
  name: String,           // TODO: this should be `title` like it is everywhere else
  duration: Option[Long],
  contentType: Option[String],
  /** TODO: All these fields below are needed just to solely determine what icon to display w/ the content Fix this up
    * via CBLPROD-10121
    */
  typeId: Option[String], // Option[AssetTypeId]
  resourceType: Option[String],
  assignmentType: Option[String],
)

object NextUpSummary:
  import PartialFunction.*

  implicit val codec: CodecJson[NextUpSummary] = CodecJson.derive[NextUpSummary]

  def fromCourseContent(content: CourseContent): NextUpSummary =
    val resourceType = condOpt(content.asset.data) { case r1: Resource1 =>
      r1.resourceType.toString
    }

    NextUpSummary(
      id = content.edgePath.toString,
      name = content.title,
      typeId = Some(content.asset.info.typeId.toString),
      duration = content.duration,
      resourceType = resourceType,
      // there's no real easy way to calculate these so I'm going to hope we can
      // get along without them on the frontend (it's real difficult to tell...)
      assignmentType = None,
      contentType = None,
    )
  end fromCourseContent
end NextUpSummary
