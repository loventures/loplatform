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

package loi.cp.job

import java.util.Date
import javax.validation.groups.Default

import com.fasterxml.jackson.annotation.{JsonProperty, JsonView}
import com.learningobjects.cpxp.component.ComponentInterface
import com.learningobjects.cpxp.component.annotation.*
import com.learningobjects.cpxp.component.web.Method
import com.learningobjects.cpxp.controller.upload.UploadInfo
import com.learningobjects.de.web.{Queryable, QueryableId}
import loi.cp.attachment.AttachmentViewComponent

/** Captures an individual run of a job. */
@Schema("run")
trait Run extends ComponentInterface with QueryableId:
  import Run.*

  /** When the run started. */
  @JsonProperty(StartTimeProperty)
  @JsonView(Array(classOf[Default]))
  @Queryable
  def getStartTime: Date

  /** When the run ended. */
  @JsonProperty(EndTimeProperty)
  @JsonView(Array(classOf[Default]))
  @Queryable
  def getEndTime: Option[Date]

  /** Whether the run succeeded or failed. */
  @JsonProperty(SuccessProperty)
  @JsonView(Array(classOf[Default]))
  @Queryable
  def getSuccess: Option[Boolean]

  /** The success or failure reason. */
  @JsonView(Array(classOf[Default]))
  def getReason: Option[String]

  /** Attachments associated with this job run. */
  @RequestMapping(path = "attachments", method = Method.Any)
  def getAttachments: AttachmentViewComponent

  /** Attach a file to this run. */
  def attach(upload: UploadInfo): Unit

  /** Records a successful job run. This commits the current transaction. */
  def succeeded(reason: String): Unit

  /** Records a failed job run. This rolls back the current transaction. */
  def failed(reason: String): Unit
end Run

/** Run component companion.
  */
object Run:

  /** The start time property. */
  final val StartTimeProperty = "startTime"

  /** The end time property. */
  final val EndTimeProperty = "endTime"

  /** The success property. */
  final val SuccessProperty = "success"
end Run
