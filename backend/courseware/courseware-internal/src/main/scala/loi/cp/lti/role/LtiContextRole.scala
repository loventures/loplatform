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

package loi.cp.lti.role

import enumeratum.{Enum, EnumEntry}

/** An LTI role within a context. */
sealed abstract class LtiContextRole(val suffix: String) extends EnumEntry with LtiRole:
  override val entryName: String = s"${LtiContextRole.Prefix}$suffix"

object LtiContextRole extends Enum[LtiContextRole]:
  // noinspection TypeAnnotation
  val values = findValues

  def withNameUnprefixableOption(name: String): Option[LtiContextRole] =
    withNameOption(name) orElse withNameOption(Prefix + name)

  final val Prefix = "urn:lti:role:ims/lis/"

  case object Learner                                extends LtiContextRole("Learner")
  case object Learner_Learner                        extends LtiContextRole("Learner/Learner")
  case object Learner_NonCreditLearner               extends LtiContextRole("Learner/NonCreditLearner")
  case object Learner_GuestLearner                   extends LtiContextRole("Learner/GuestLearner")
  case object Learner_ExternalLearner                extends LtiContextRole("Learner/ExternalLearner")
  case object Learner_Instructor                     extends LtiContextRole("Learner/Instructor")
  case object Instructor                             extends LtiContextRole("Instructor")
  case object Instructor_PrimaryInstructor           extends LtiContextRole("Instructor/PrimaryInstructor")
  case object Instructor_Lecturer                    extends LtiContextRole("Instructor/Lecturer")
  case object Instructor_GuestInstructor             extends LtiContextRole("Instructor/GuestInstructor")
  case object Instructor_ExternalInstructor          extends LtiContextRole("Instructor/ExternalInstructor")
  case object ContentDeveloper                       extends LtiContextRole("ContentDeveloper")
  case object ContentDeveloper_ContentDeveloper      extends LtiContextRole("ContentDeveloper/ContentDeveloper")
  case object ContentDeveloper_Librarian             extends LtiContextRole("ContentDeveloper/Librarian")
  case object ContentDeveloper_ContentExpert         extends LtiContextRole("ContentDeveloper/ContentExpert")
  case object ContentDeveloper_ExternalContentExpert extends LtiContextRole("ContentDeveloper/ExternalContentExpert")
  case object Member                                 extends LtiContextRole("Member")
  case object Member_Member                          extends LtiContextRole("Member/Member")
  case object Manager                                extends LtiContextRole("Manager")
  case object Manager_AreaManager                    extends LtiContextRole("Manager/AreaManager")
  case object Manager_CourseCoordinator              extends LtiContextRole("Manager/CourseCoordinator")
  case object Manager_Observer                       extends LtiContextRole("Manager/Observer")
  case object Manager_ExternalObserver               extends LtiContextRole("Manager/ExternalObserver")
  case object Mentor                                 extends LtiContextRole("Mentor")
  case object Mentor_Mentor                          extends LtiContextRole("Mentor/Mentor")
  case object Mentor_Reviewer                        extends LtiContextRole("Mentor/Reviewer")
  case object Mentor_Advisor                         extends LtiContextRole("Mentor/Advisor")
  case object Mentor_Auditor                         extends LtiContextRole("Mentor/Auditor")
  case object Mentor_Tutor                           extends LtiContextRole("Mentor/Tutor")
  case object Mentor_LearningFacilitator             extends LtiContextRole("Mentor/LearningFacilitator")
  case object Mentor_ExternalMentor                  extends LtiContextRole("Mentor/ExternalMentor")
  case object Mentor_ExternalReviewer                extends LtiContextRole("Mentor/ExternalReviewer")
  case object Mentor_ExternalAdvisor                 extends LtiContextRole("Mentor/ExternalAdvisor")
  case object Mentor_ExternalAuditor                 extends LtiContextRole("Mentor/ExternalAuditor")
  case object Mentor_ExternalTutor                   extends LtiContextRole("Mentor/ExternalTutor")
  case object Mentor_ExternalLearningFacilitator     extends LtiContextRole("Mentor/ExternalLearningFacilitator")
  case object Administrator                          extends LtiContextRole("Administrator")
  case object Administrator_Administrator            extends LtiContextRole("Administrator/Administrator")
  case object Administrator_Support                  extends LtiContextRole("Administrator/Support")
  case object Administrator_Developer                extends LtiContextRole("Administrator/ExternalDeveloper")
  case object Administrator_SystemAdministrator      extends LtiContextRole("Administrator/SystemAdministrator")
  case object Administrator_ExternalSystemAdministrator
      extends LtiContextRole("Administrator/ExternalSystemAdministrator")
  case object Administrator_ExternalDeveloper        extends LtiContextRole("Administrator/ExternalDeveloper")
  case object Administrator_ExternalSupport          extends LtiContextRole("Administrator/ExternalSupport")
  case object TeachingAssistant                      extends LtiContextRole("TeachingAssistant")
  case object TeachingAssistant_TeachingAssistant    extends LtiContextRole("TeachingAssistant/TeachingAssistant")
  case object TeachingAssistant_TeachingAssistantSection
      extends LtiContextRole("TeachingAssistant/TeachingAssistantSection")
  case object TeachingAssistantSectionAssociation
      extends LtiContextRole("TeachingAssistant/TeachingAssistantSectionAssociation")
  case object TeachingAssistantOffering              extends LtiContextRole("TeachingAssistant/TeachingAssistantOffering")
  case object TeachingAssistantTemplate              extends LtiContextRole("TeachingAssistant/TeachingAssistantTemplate")
  case object TeachingAssistant_TeachingAssistantGroup
      extends LtiContextRole("TeachingAssistant/TeachingAssistantGroup")
  case object TeachingAssistant_Grader               extends LtiContextRole("TeachingAssistant/Grader")
end LtiContextRole
