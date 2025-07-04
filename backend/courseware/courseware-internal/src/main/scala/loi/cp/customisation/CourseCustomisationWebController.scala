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

package loi.cp.customisation

import java.time.Instant

import argonaut.Json
import com.learningobjects.cpxp.component.annotation.*
import com.learningobjects.cpxp.component.web.{ApiRootComponent, ArgoBody, Method}
import com.learningobjects.de.authorization.securedByImplementation
import loi.authoring.asset.factory.AssetTypeId
import loi.cp.reference.EdgePath
import scaloi.data.ListTree

import scala.util.Try

@securedByImplementation
@Controller(root = true)
trait CourseCustomisationWebController extends ApiRootComponent:

  import CourseCustomisationWebController.*

  @RequestMapping(path = "lwc/{context}/customise/reset", method = Method.POST)
  def resetCustomisation(@PathVariable("context") ontext: Long): Try[Unit]

  @RequestMapping(path = "lwc/{context}/customise/{path}", method = Method.POST)
  def updateContent(
    @PathVariable("context") context: Long,
    @PathVariable("path") path: EdgePath,
    @RequestBody update: ArgoBody[ContentOverlayUpdate]
  ): Try[ArgoBody[ContentOverlay]]

  @RequestMapping(path = "lwc/{context}/customise/updates", method = Method.POST)
  def updateContents(
    @PathVariable("context") context: Long,
    @RequestBody update: ArgoBody[Map[EdgePath, ContentOverlayUpdate]]
  ): Try[ArgoBody[Map[EdgePath, ContentOverlay]]]

  @RequestMapping(path = "lwc/{context}/contents/customisable", method = Method.GET)
  def customisableContents(@PathVariable("context") context: Long): Try[ArgoBody[ListTree[CustomisableContent]]]

  @RequestMapping(path = "lwc/{context}", method = Method.GET)
  def getCourseInfo(@PathVariable("context") context: Long): Try[CourseInfo]
end CourseCustomisationWebController

object CourseCustomisationWebController:

  final case class CourseInfo(name: String, url: String)

  final case class CustomisableContent(
    id: EdgePath,
    title: String,
    instructions: Option[String],
    resourceType: Option[String],
    gateDate: Option[Instant],
    dueDate: Option[Instant],
    gateDateOffset: Option[Long],
    dueDateOffset: Option[Long],
    gradable: Boolean,
    titleCustomised: Boolean,
    instructionsCustomised: Boolean,
    dueDateCustomised: Boolean,
    gateDateCustomised: Boolean,
    pointsPossible: Option[java.math.BigDecimal],
    pointsPossibleCustomised: Boolean,
    isForCredit: Option[Boolean],
    isForCreditCustomised: Boolean,
    typeId: AssetTypeId,
    hide: List[EdgePath],
    orderCustomised: Boolean,
    metadata: Option[Json]
  )

  object CustomisableContent:

    import argonaut.{DecodeJson, EncodeJson}
    import scaloi.json.ArgoExtras.*

    implicit def encodeJson: EncodeJson[CustomisableContent] = EncodeJson.derive[CustomisableContent]

    implicit def decodeJson: DecodeJson[CustomisableContent] = DecodeJson.derive[CustomisableContent]
end CourseCustomisationWebController
