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

package loi.cp.right

import enumeratum.{Enum, EnumEntry}
import loi.authoring.security.right.*
import loi.cp.accesscode.{AccessCodeAdminRight, AccessCodeSupportRight}
import loi.cp.accountRequest.ApproveAccountRequestRight
import loi.cp.admin.right.*
import loi.cp.admin.right.user.SudoAdminRight
import loi.cp.announcement.*
import loi.cp.course.right.*
import loi.cp.lti.right.ConfigureLtiCourseRight
import loi.cp.ltitool.ManageLtiToolsAdminRight
import loi.cp.overlord.{OverlordRight, SupportRight, UnderlordRight}
import loi.cp.zip.ZipSiteAdminRight

import scala.reflect.ClassTag

/** Standard rights that should be available to all facets of the application. This is not intended to be exhaustive as
  * further modules can define and use their own rights.
  */
sealed abstract class StandardRight[T <: Right](implicit tt: ClassTag[T]) extends EnumEntry:

  /** the right itself */
  val right: Class[T] = tt.runtimeClass.asInstanceOf[Class[T]]

case object StandardRight extends Enum[StandardRight[? <: Right]]:
  val values = findValues

  object ConfigureLtiCourse extends StandardRight[ConfigureLtiCourseRight]

  // //////////////////////////////////////////////
  object AllCourse extends StandardRight[CourseRight]

  object LearnCourse         extends StandardRight[LearnCourseRight]
  object InteractCourse      extends StandardRight[InteractCourseRight]
  object CourseParticipation extends StandardRight[CourseParticipationRight]
  object FullContent         extends StandardRight[FullContentRight]
  object TrialContent        extends StandardRight[TrialContentRight]
  object ReadCourse          extends StandardRight[ReadCourseRight]
  object TeachCourse         extends StandardRight[TeachCourseRight]
  object CreateNotification  extends StandardRight[CreateNotificationRight]
  object CourseRoster        extends StandardRight[CourseRosterRight]
  object AuthorCourse        extends StandardRight[AuthorCourseRight]
  object ContentCourse       extends StandardRight[ContentCourseRight]
  object AllGrading          extends StandardRight[GradeCourseRight]
  object EditGradebook       extends StandardRight[EditGradebookRight]
  object EditGrade           extends StandardRight[EditCourseGradeRight]
  object ViewGrade           extends StandardRight[ViewCourseGradeRight]

  // //////////////////////////////////////////////
  object AllOverlord extends StandardRight[OverlordRight]

  object Support   extends StandardRight[SupportRight]
  object Underlord extends StandardRight[UnderlordRight]

  // //////////////////////////////////////////////
  object HostingAdmin               extends StandardRight[HostingAdminRight]
  object AnalyticsConsumer          extends StandardRight[AnalyticsConsumerRight]
  object ConfigurationAdmin         extends StandardRight[ConfigurationAdminRight]
  object Admin                      extends StandardRight[AdminRight]
  object SubtenantAdmin             extends StandardRight[SubtenantAdminRight]
  object AccessCodeAdmin            extends StandardRight[AccessCodeAdminRight]
  object AccessCodeSupport          extends StandardRight[AccessCodeSupportRight]
  object ContentPublisher           extends StandardRight[ContentPublisherRight]
  object ContentAuthor              extends StandardRight[ContentAuthorRight]
  object CourseAdmin                extends StandardRight[CourseAdminRight]
  object ManageLtiToolsAdmin        extends StandardRight[ManageLtiToolsAdminRight]
  object ViewLtiTool                extends StandardRight[ViewLtiToolRight]
  object ManageCoursesAdmin         extends StandardRight[ManageCoursesAdminRight]
  object ManageCoursesRead          extends StandardRight[ManageCoursesReadRight]
  object ManageLibrariesAdmin       extends StandardRight[ManageLibrariesAdminRight]
  object ManageLibrariesRead        extends StandardRight[ManageLibrariesReadRight]
  object AssetAdmin                 extends StandardRight[AssetAdminRight]
  object ReportingAdmin             extends StandardRight[ReportingAdminRight]
  object UserAdmin                  extends StandardRight[UserAdminRight]
  object SudoAdmin                  extends StandardRight[SudoAdminRight]
  object ApproveAccountRequest      extends StandardRight[ApproveAccountRequestRight]
  object RoleAdmin                  extends StandardRight[RoleAdminRight]
  object ExternalLinkAdmin          extends StandardRight[ExternalLinkAdminRight]
  object IntegrationAdmin           extends StandardRight[IntegrationAdminRight]
  object LtiAdmin                   extends StandardRight[LtiAdminRight]
  object AllAuthoringActions        extends StandardRight[AllAuthoringActionsRight]
  object ViewAllProjects            extends StandardRight[ViewAllProjectsRight]
  object PublishOffering            extends StandardRight[PublishOfferingRight]
  object AccessAuthoringAdminApp    extends StandardRight[AccessAuthoringAdminAppRight]
  object ChangeOwnerAnyProject      extends StandardRight[ChangeOwnerAnyProjectRight]
  object EditSettingsAnyProject     extends StandardRight[EditSettingsAnyProjectRight]
  object CreateProject              extends StandardRight[CreateProjectRight]
  object EditContributorsAnyProject extends StandardRight[EditContributorsAnyProjectRight]
  object AddVersionAnyProject       extends StandardRight[AddVersionAnyProjectRight]
  object EditContentAnyProject      extends StandardRight[EditContentAnyProjectRight]
  object AccessAuthoringApp         extends StandardRight[AccessAuthoringAppRight]
  object DeleteAnyProject           extends StandardRight[DeleteAnyProjectRight]
  object ProjectAdmin               extends StandardRight[ProjectAdminRight]
  object MediaAdmin                 extends StandardRight[MediaAdminRight]
  object ZipSiteAdmin               extends StandardRight[ZipSiteAdminRight]
  object ComponentAdmin             extends StandardRight[ComponentAdminRight]
  object CopyAnyProjectVersion      extends StandardRight[CopyAnyProjectVersionRight]
  object AnnouncementAdmin          extends StandardRight[AnnouncementAdminRight]

  import scala.language.implicitConversions
  implicit def rightClass[A <: Right](t: StandardRight[A]): Class[A] = t.right
end StandardRight
