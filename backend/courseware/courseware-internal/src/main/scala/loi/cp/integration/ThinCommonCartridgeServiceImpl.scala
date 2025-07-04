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

package loi.cp.integration

import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.service.CurrentUrlService
import com.learningobjects.cpxp.service.domain.DomainDTO
import com.learningobjects.cpxp.service.group.GroupConstants.GroupType
import loi.asset.course.model.Course
import loi.asset.lesson.model.Lesson
import loi.asset.module.model.Module
import loi.authoring.asset.Asset
import loi.cp.content.{ContentTree, CourseContent, CourseContentService}
import loi.cp.course.CourseComponent
import scalaz.syntax.std.boolean.*
import scaloi.data.ListTree.{Leaf, Node}
import scaloi.syntax.option.*

import scala.xml.{Elem, NodeSeq}

@Service
class ThinCommonCartridgeServiceImpl(
  domain: => DomainDTO,
  contentService: CourseContentService,
  currentUrlService: CurrentUrlService
) extends ThinCommonCartridgeService:

  override def getLoConfig =
    <cartridge_basiclti_link
      xmlns="http://www.imsglobal.org/xsd/imslticc_v1p0"
      xmlns:blti="http://www.imsglobal.org/xsd/imsbasiclti_v1p0"
      xmlns:lticm="http://www.imsglobal.org/xsd/imslticm_v1p0"
      xmlns:lticp="http://www.imsglobal.org/xsd/imslticp_v1p0"
      xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
      xsi:schemaLocation="http://www.imsglobal.org/xsd/imslticc_v1p0
                   http://www.imsglobal.org/xsd/lti/ltiv1p0/imslticc_v1p0.xsd
                   http://www.imsglobal.org/xsd/imsbasiclti_v1p0
                   http://www.imsglobal.org/xsd/lti/ltiv1p0/imsbasiclti_v1p0p1.xsd
                   http://www.imsglobal.org/xsd/imslticm_v1p0
                   http://www.imsglobal.org/xsd/lti/ltiv1p0/imslticm_v1p0.xsd
                   http://www.imsglobal.org/xsd/imslticp_v1p0
                   http://www.imsglobal.org/xsd/lti/ltiv1p0/imslticp_v1p0.xsd">
      <blti:title>Learning Objects Local</blti:title>
      <blti:description>Import Learning Objects courses.</blti:description>
      {getCanvasExtensions()}
    </cartridge_basiclti_link>

  override def getLtiXml(lwc: CourseComponent): Elem =
    val (url, title, description) = (
      currentUrlService.getUrl(s"/lwlti/offering/${lwc.getGroupId}"),
      lwc.getName,
      lwc.loadCourse().data.subtitle
    )

    <cartridge_basiclti_link
      xmlns="http://www.imsglobal.org/xsd/imslticc_v1p0"
      xmlns:blti="http://www.imsglobal.org/xsd/imsbasiclti_v1p0"
      xmlns:lticm="http://www.imsglobal.org/xsd/imslticm_v1p0"
      xmlns:lticp="http://www.imsglobal.org/xsd/imslticp_v1p0"
      xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
      xsi:schemaLocation="http://www.imsglobal.org/xsd/imslticc_v1p0
                   http://www.imsglobal.org/xsd/lti/ltiv1p0/imslticc_v1p0.xsd
                   http://www.imsglobal.org/xsd/imsbasiclti_v1p0
                   http://www.imsglobal.org/xsd/lti/ltiv1p0/imsbasiclti_v1p0p1.xsd
                   http://www.imsglobal.org/xsd/imslticm_v1p0
                   http://www.imsglobal.org/xsd/lti/ltiv1p0/imslticm_v1p0.xsd
                   http://www.imsglobal.org/xsd/imslticp_v1p0
                   http://www.imsglobal.org/xsd/lti/ltiv1p0/imslticp_v1p0.xsd">
      <blti:launch_url>{url}</blti:launch_url>
      <blti:title>{title}</blti:title>
      <blti:description>{description}</blti:description>
      <blti:custom>
        <lticm:property name="presentation_mode">meek</lticm:property>
      </blti:custom>
      {getCanvasExtensions(false, None)}
    </cartridge_basiclti_link>
  end getLtiXml

  override def getThinCommonCartridgeConfiguration(
    lwc: CourseComponent,
    modules: Boolean
  ): Elem =
    val crs          = lwc.course
    val contentTree  = contentService.getCourseContents(lwc).get
    val courseLaunch = (lwc.getGroupType == GroupType.CourseOffering)
      .fold(s"/lwlti/offering/${lwc.getGroupId}", s"/lwlti/section/${lwc.getExternalId | lwc.getGroupId}")

    val resources =
      contentTree.nonRootElements
        .filter(ci =>
          ci.asset match
            case Module.Asset(_) => modules
            case Lesson.Asset(_) => false
            case _               => true
        )
        .map(ci => ResourceInfo(crs, ci, currentUrlService.getUrl(s"$courseLaunch/${ci.edgePath}")))
        .map(info => buildResource(info))

    buildCC(buildOrganization(lwc, modules)(contentTree.tree.subForest), resources)
  end getThinCommonCartridgeConfiguration

  def buildOrganization(lwc: CourseComponent, modules: Boolean)(courseLevelContents: Seq[ContentTree]): Elem =
    val crs = lwc.loadCourse()
    <organizations>
      <organization identifier={lwc.getGroupId} structure="rooted-hierarchy">
        <title>{esc(lwc.getName)}</title>
        <item identifier="_root_">
          {courseLevelContents flatMap buildCourseLevelContent(crs, modules)}
        </item>
      </organization>
    </organizations>
  end buildOrganization

  def buildCourseLevelContent(crs: Asset[Course], modules: Boolean)(contents: ContentTree): Seq[Elem] = contents match
    case Leaf(activity) =>
      buildItemOrganization(ResourceInfo(crs, activity)) :: Nil

    case Node(container, containerContent) if container.asset.data.isInstanceOf[Lesson] =>
      containerContent flatMap buildCourseLevelContent(crs, modules)

    case Node(
          container,
          containerContent
        ) => // the spec suggests that `identifierref` on a folder is uncouth but needs must
      <item identifier={s"ITEM_${container.edgePath}"} identifierref={
        modules.option(xml.Text(container.edgePath.toString))
      }>
        <title>{esc(container.title)}</title>
        {containerContent flatMap buildCourseLevelContent(crs, modules)}
      </item> :: Nil

  def buildCC(organization: Elem, resources: Seq[Elem]): Elem =
    <manifest identifier="thinccvp1_test05"
              xmlns="http://www.imsglobal.org/xsd/imsccv1p3/imscp_v1p1"
              xmlns:lomr="http://ltsc.ieee.org/xsd/imsccv1p3/LOM/resource"
              xmlns:lomm="http://ltsc.ieee.org/xsd/imsccv1p3/LOM/manifest"
              xmlns:lticc="http://www.imsglobal.org/xsd/imslticc_v1p3"
              xmlns:lomc="http://ltsc.ieee.org/xsd/imsccv1p3/LOM/imscclti"
              xmlns:blti="http://www.imsglobal.org/xsd/imsbasiclti_v1p0"
              xmlns:lticm="http://www.imsglobal.org/xsd/imslticm_v1p0"
              xmlns:lticp="http://www.imsglobal.org/xsd/imslticp_v1p0"
              xmlns:wl="http://www.imsglobal.org/xsd/imsccv1p3/imswl_v1p3"
              xmlns:csm="http://www.imsglobal.org/xsd/imsccv1p3/imscsmd_v1p0"
              xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
              xsi:schemaLocation="http://www.imsglobal.org/xsd/imsccv1p3/imscp_v1p1 http://www.imsglobal.org/profile/cc/ccv1p3/ccv1p3_imscp_v1p2_v1p0.xsd
                            http://ltsc.ieee.org/xsd/imsccv1p3/LOM/resource http://www.imsglobal.org/profile/cc/ccv1p3/LOM/ccv1p3_lomresource_v1p0.xsd
                            http://ltsc.ieee.org/xsd/imsccv1p3/LOM/manifest http://www.imsglobal.org/profile/cc/ccv1p3/LOM/ccv1p3_lommanifest_v1p0.xsd
                            http://www.imsglobal.org/xsd/imsccv1p3/imscsmd_v1p0 http://www.imsglobal.org/profile/cc/ccv1p3/ccv1p3_imscsmd_v1p0.xsd
                            http://www.imsglobal.org/xsd/imslticc_v1p3 http://www.imsglobal.org/xsd/lti/ltiv1p3/imslticc_v1p3.xsd
                            http://ltsc.ieee.org/xsd/imsccv1p3/LOM/imscclti http://www.imsglobal.org/profile/cc/ccv1p3/LOM/ccv1p3_lomccltilink_v1p0.xsd
                            http://www.imsglobal.org/xsd/imslticp_v1p0 http://www.imsglobal.org/xsd/lti/ltiv1p0/imslticp_v1p0.xsd
                            http://www.imsglobal.org/xsd/imslticm_v1p0 http://www.imsglobal.org/xsd/lti/ltiv1p0/imslticm_v1p0.xsd
                            http://www.imsglobal.org/xsd/imsbasiclti_v1p0 http://www.imsglobal.org/xsd/lti/ltiv1p0/imsbasiclti_v1p0p1.xsd
                            http://www.imsglobal.org/xsd/imsccv1p3/imswl_v1p3 http://www.imsglobal.org/profile/cc/ccv1p3/ccv1p3_imswl_v1p3.xsd">
      <metadata>
        <schema>IMS Thin Common Cartridge</schema>
        <schemaversion>1.3.0</schemaversion>
      </metadata>

      {organization}

      <resources>
        {resources}
      </resources>
    </manifest>

  def buildItemOrganization(ci: ResourceInfo): Elem =
    <item identifier={s"ITEM_${ci.guid}"} identifierref={ci.guid}>
      <title>{esc(ci.name)}</title>
      <metadata>
        <lomm:lom>
          <lomm:general>
            <lomm:title>
              <lomm:string language="en-US">{esc(ci.subtitle)}</lomm:string>
            </lomm:title>
            <lomm:description>
              <lomm:string language="en-US">{esc(ci.description)}</lomm:string>
            </lomm:description>
            <lomm:language>en-us</lomm:language>
          </lomm:general>
        </lomm:lom>
      </metadata>
    </item>

  def buildResource(info: ResourceInfo): Elem =
    <resource identifier={info.guid} type="imsbasiclti_xmlv1p3">
      <lticc:cartridge_basiclti_link>
        <blti:title>{esc(info.name)}</blti:title>
        <blti:description>{esc(info.description)}</blti:description>
        <blti:launch_url>{info.ltiUrl}</blti:launch_url>
        <blti:custom>
          <lticm:property name="presentation_mode">meek</lticm:property>
        </blti:custom>
        <blti:vendor>
          <lticp:code>learningobjects.com</lticp:code>
          <lticp:name>Learning Objects</lticp:name>
        </blti:vendor>
        <lticc:metadata>
          <lomc:lom>
            <lomc:general>
              <lomc:identifier>
                <lomc:entry>{info.guid}</lomc:entry>
              </lomc:identifier>
            </lomc:general>
          </lomc:lom>
        </lticc:metadata>
        {
      getCanvasExtensions(
        includeContentItem = false,
        info.isGraded.flatMap(isGraded =>
          if isGraded then
            Some(
              <lticm:property name="outcome">
                  {info.pointsPossible.getOrElse(0d)}
                </lticm:property>
            )
          else None
        )
      )
    }
      </lticc:cartridge_basiclti_link>
    </resource>

  private def esc(s: String): String =
    // we can't escape quotes, apostrophes, or ampersands since canvas doesn't understand their encoded versions...
    // StringEscapeUtils.escapeXml10(s)
    Option(s).fold("")(_.replaceAll("<", "&lt;").replaceAll(">", "&gt"))

  def getCanvasExtensions(includeContentItem: Boolean = true, additional: Option[Elem] = None) =
    <blti:extensions platform="canvas.instructure.com">
      {additional.getOrElse(NodeSeq.Empty)}
      <lticm:property name="domain">{domain.hostName}</lticm:property>
      <lticm:property name="privacy_level">public</lticm:property>
      {
      if includeContentItem then
        <lticm:options name="migration_selection">
          <lticm:property name="privacy_level">public</lticm:property>
          <lticm:property name="message_type">ContentItemSelectionRequest</lticm:property>
          <lticm:property name="url">{currentUrlService.getUrl(s"/lti_new/curriculum/cc")}</lticm:property>
          <lticm:property name="selection_width">500</lticm:property>
          <lticm:property name="selection_height">500</lticm:property>
        </lticm:options>
      else {}
    }
    </blti:extensions>
end ThinCommonCartridgeServiceImpl

case class ResourceInfo(
  guid: String,
  name: String,
  description: String,
  ltiUrl: String,
  subtitle: String,
  isGraded: Option[Boolean],
  pointsPossible: Option[Double],
)

object ResourceInfo:

  def apply(
    course: Asset[Course],
    content: CourseContent,
    url: String = ""
  ): ResourceInfo =
    ResourceInfo(
      guid = content.edgePath.toString,
      name = content.title,
      description = "",
      ltiUrl = url,
      subtitle = course.data.subtitle,
      isGraded = content.gradingPolicy.map(_.isForCredit),
      pointsPossible = content.gradingPolicy.map(_.pointsPossible.doubleValue)
    )
end ResourceInfo
