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

package loi.cp.analytics.entity

import java.util.UUID

import argonaut.CodecJson
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.annotation.JsonDeserialize

@JsonInclude(JsonInclude.Include.NON_EMPTY)
case class CourseId(
  section: ExternallyIdentifiableEntity,
  assetGuid: Option[UUID] = None,
  @JsonDeserialize(contentAs = classOf[java.lang.Long])
  projectId: Option[Long] = None,
  @JsonDeserialize(contentAs = classOf[java.lang.Long])
  offeringId: Option[Long] = None, // contains groupfinder.mastercourse_id
  @JsonDeserialize(contentAs = classOf[java.lang.Long])
  branchId: Option[Long] = None,
  @JsonDeserialize(contentAs = classOf[java.lang.Long])
  commitId: Option[Long] = None
)

object CourseId:
  implicit val codec: CodecJson[CourseId] = CodecJson.derive[CourseId]
