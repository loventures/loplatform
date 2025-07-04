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

package loi.cp.lti

import scalaz.syntax.semigroup.*
import scalaz.{Monoid, NonEmptyList}

// TODO: Use the new (old, by the time you read this) LtiRole enumerata
object LtiRoles:

  implicit val wut: Monoid[Map[String, NonEmptyList[String]]] = scalaz.std.map.mapMonoid[String, NonEmptyList[String]]

  /** Contains a mapping of global LO roles to their IMS counterparts
    */
  val GlobalMappings: Map[String, NonEmptyList[String]]    = System.Mappings |+| Institution.Mappings
  val GlobalMappings1p3: Map[String, NonEmptyList[String]] = Institution1p3.Mappings

  /** Contains a mapping of enrollment LO roles to their contextual IMS counterparts
    */
  val ContextMappings: RoleMappings    = Context.Mappings
  val ContextMappings1p3: RoleMappings = Context1p3.Mappings

  type RoleMappings = Map[String, NonEmptyList[String]]

  /** A collection of system-level roles. These should map to roles that a user has in the DE system. These roles all
    * pertain to a role a user can have in a
    */
  object System:

    val Prefix = "urn:lti:sysrole:ims/lis/"

    val SysAdmin      = Prefix + "SysAdmin"
    val SysSupport    = Prefix + "SysSupport"
    val Creator       = Prefix + "Creator"
    val AccountAdmin  = Prefix + "AccountAdmin"
    val User          = Prefix + "User"
    val Administrator = Prefix + "Administrator"
    val None          = Prefix + "None"

    val Mappings: Map[String, NonEmptyList[String]] = Map(
      "administrator" -> NonEmptyList(Administrator)
    )
  end System

  object System1p3:
    // https://www.imsglobal.org/spec/lti/v1p3#lti-vocabulary-for-system-roles

    val Prefix = "http://purl.imsglobal.org/vocab/lti/system/person#"

    /** Per LTI docs
      *
      * This is a marker role to be used in conjunction with a "real" role. It indicates this user is created by the
      * platform for testing different user scenarios. The most common use case is when an instructor wants to view the
      * course as a student would see it, student-preview mode. etc etc
      */
    val TestUser = Prefix + "TestUser"

    // no mappings here because it does not map to a specific role, but rather a use case
  end System1p3

  /** A collection of institution-level roles. These are roles that a user has in DE that are specific to an educational
    * institution.
    */
  object Institution:

    val Prefix = "urn:lti:instrole:ims/lis/"

    val Student            = Prefix + "Student"
    val Faculty            = Prefix + "Faculty"
    val Member             = Prefix + "Member"
    val Learner            = Prefix + "Learner"
    val Instructor         = Prefix + "Instructor"
    val Mentor             = Prefix + "Mentor"
    val Staff              = Prefix + "Staff"
    val Alumni             = Prefix + "Alumni"
    val ProspectiveStudent = Prefix + "ProspectiveStudent"
    val Guest              = Prefix + "Guest"
    val Other              = Prefix + "Other"
    val Administrator      = Prefix + "Administrator"
    val Observer           = Prefix + "Observer"
    val None               = Prefix + "None"

    val Mappings: Map[String, NonEmptyList[String]] = Map(
      "administrator" -> NonEmptyList(Administrator),
      "teacher"       -> NonEmptyList(Instructor),
      "professor"     -> NonEmptyList(Instructor),
      "faculty"       -> NonEmptyList(Faculty),
      "staff"         -> NonEmptyList(Staff),
      "student"       -> NonEmptyList(Student),
      "instructor"    -> NonEmptyList(Instructor),
      "guest"         -> NonEmptyList(Guest),
      "member"        -> NonEmptyList(Member)
    )
  end Institution

  object Institution1p3:
    // https://www.imsglobal.org/spec/lti/v1p3#lis-vocabulary-for-institution-roles

    val Prefix = "http://purl.imsglobal.org/vocab/lis/v2/institution/person#"

    // Core institution roles
    val Administrator = Prefix + "Administrator"
    val Faculty       = Prefix + "Faculty"
    val Guest         = Prefix + "Guest"
    val None          = Prefix + "None"
    val Other         = Prefix + "Other"
    val Staff         = Prefix + "Staff"
    val Student       = Prefix + "Student"

    // Non-core institution roles
    val Alumni             = Prefix + "Alumni"
    val Instructor         = Prefix + "Instructor"
    val Learner            = Prefix + "Learner"
    val Member             = Prefix + "Member"
    val Mentor             = Prefix + "Mentor"
    val Observer           = Prefix + "Observer"
    val ProspectiveStudent = Prefix + "ProspectiveStudent"

    val Mappings: Map[String, NonEmptyList[String]] = Map(
      "administrator" -> NonEmptyList(Administrator),
      "teacher"       -> NonEmptyList(Instructor),
      "professor"     -> NonEmptyList(Instructor),
      "faculty"       -> NonEmptyList(Faculty),
      "staff"         -> NonEmptyList(Staff),
      "student"       -> NonEmptyList(Student),
      "instructor"    -> NonEmptyList(Instructor),
      "guest"         -> NonEmptyList(Guest),
      "member"        -> NonEmptyList(Member)
    )
  end Institution1p3

  /** A collection of context-level roles. These are roles that a user has in DE that are specific to a specific
    * institution context (e.g. a course, group, etc).
    */
  object Context:

    val Prefix = "urn:lti:role:ims/lis/"

    val Learner                  = Prefix + "Learner"
    val Learner_Learner          = Prefix + "Learner/Learner"
    val Learner_NonCreditLearner = Prefix + "Learner/NonCreditLearner"
    val Learner_GuestLearner     = Prefix + "Learner/GuestLearner"
    val Learner_ExternalLearner  = Prefix + "Learner/ExternalLearner"
    val Learner_Instructor       = Prefix + "Learner/Instructor"

    val Instructor                    = Prefix + "Instructor"
    val Instructor_PrimaryInstructor  = Prefix + "Instructor/PrimaryInstructor"
    val Instructor_Lecturer           = Prefix + "Instructor/Lecturer"
    val Instructor_GuestInstructor    = Prefix + "Instructor/GuestInstructor"
    val Instructor_ExternalInstructor = Prefix + "Instructor/ExternalInstructor"

    val ContentDeveloper                       = Prefix + "ContentDeveloper"
    val ContentDeveloper_ContentDeveloper      = Prefix + "ContentDeveloper/ContentDeveloper"
    val ContentDeveloper_Librarian             = Prefix + "ContentDeveloper/Librarian"
    val ContentDeveloper_ContentExpert         = Prefix + "ContentDeveloper/ContentExpert"
    val ContentDeveloper_ExternalContentExpert = Prefix + "ContentDeveloper/ExternalContentExpert"

    val Member        = Prefix + "Member"
    val Member_Member = Prefix + "Member/Member"

    val Manager                   = Prefix + "Manager"
    val Manager_AreaManager       = Prefix + "Manager/AreaManager"
    val Manager_CourseCoordinator = Prefix + "Manager/CourseCoordinator"
    val Manager_Observer          = Prefix + "Manager/Observer"
    val Manager_ExternalObserver  = Prefix + "Manager/ExternalObserver"

    val Mentor                             = Prefix + "Mentor"
    val Mentor_Mentor                      = Prefix + "Mentor/Mentor"
    val Mentor_Reviewer                    = Prefix + "Mentor/Reviewer"
    val Mentor_Advisor                     = Prefix + "Mentor/Advisor"
    val Mentor_Auditor                     = Prefix + "Mentor/Auditor"
    val Mentor_Tutor                       = Prefix + "Mentor/Tutor"
    val Mentor_LearningFacilitator         = Prefix + "Mentor/LearningFacilitator"
    val Mentor_ExternalMentor              = Prefix + "Mentor/ExternalMentor"
    val Mentor_ExternalReviewer            = Prefix + "Mentor/ExternalReviewer"
    val Mentor_ExternalAdvisor             = Prefix + "Mentor/ExternalAdvisor"
    val Mentor_ExternalAuditor             = Prefix + "Mentor/ExternalAuditor"
    val Mentor_ExternalTutor               = Prefix + "Mentor/ExternalTutor"
    val Mentor_ExternalLearningFacilitator = Prefix + "Mentor/ExternalLearningFacilitator"

    val Administrator                             = Prefix + "Administrator"
    val Administrator_Administrator               = Prefix + "Administrator/Administrator"
    val Administrator_Support                     = Prefix + "Administrator/Support"
    val Administrator_Developer                   = Prefix + "Administrator/ExternalDeveloper"
    val Administrator_SystemAdministrator         = Prefix + "Administrator/SystemAdministrator"
    val Administrator_ExternalSystemAdministrator = Prefix + "Administrator/ExternalSystemAdministrator"
    val Administrator_ExternalDeveloper           = Prefix + "Administrator/ExternalDeveloper"
    val Administrator_ExternalSupport             = Prefix + "Administrator/ExternalSupport"

    val TeachingAssistant                                     = Prefix + "TeachingAssistant"
    val TeachingAssistant_TeachingAssistant                   = Prefix + "TeachingAssistant/TeachingAssistant"
    val TeachingAssistant_TeachingAssistantSection            = Prefix + "TeachingAssistant/TeachingAssistantSection"
    val TeachingAssistant_TeachingAssistantSectionAssociation =
      Prefix + "TeachingAssistant/TeachingAssistantSectionAssociation"
    val TeachingAssistant_TeachingAssistantOffering           = Prefix + "TeachingAssistant/TeachingAssistantOffering"
    val TeachingAssistant_TeachingAssistantTemplate           = Prefix + "TeachingAssistant/TeachingAssistantTemplate"
    val TeachingAssistant_TeachingAssistantGroup              = Prefix + "TeachingAssistant/TeachingAssistantGroup"
    val TeachingAssistant_Grader                              = Prefix + "TeachingAssistant/Grader"

    /** Contains a mapping of LO context roles to their IMS counterparts.
      */
    val Mappings: RoleMappings = Map(
      "administrator" -> withShortForm(Administrator),
      "teacher"       -> withShortForm(Instructor),
      "professor"     -> withShortForm(Instructor),
      "student"       -> withShortForm(Learner),
      "instructor"    -> withShortForm(Instructor)
    )

    // For contextual roles, IMS allows a "short" form of roles,
    // unfortunately, there could be LTI providers that rely only on the short form, so let's include both.
    private def withShortForm(s: String): NonEmptyList[String] =
      NonEmptyList(s, s.replaceAll(Prefix, ""))
  end Context

  val Transient = "urn:lti:role:ims/lti/Transient"

  object Context1p3:
    // https://www.imsglobal.org/spec/lti/v1p3#lis-vocabulary-for-context-roles

    val Prefix = "http://purl.imsglobal.org/vocab/lis/v2/membership#"

    // Core context roles
    val Administrator    = Prefix + "Administrator"
    val ContentDeveloper = Prefix + "ContentDeveloper"
    val Instructor       = Prefix + "Instructor"
    val Learner          = Prefix + "Learner"
    val Mentor           = Prefix + "Mentor"

    // Non-core context roles
    val Manager = Prefix + "Manager"
    val Member  = Prefix + "Member"
    val Officer = Prefix + "Officer"

    // There exist also sub-roles here, but we do not use them currently, so I omitted here
    // see: https://www.imsglobal.org/spec/lti/v1p3#context-sub-roles

    val Mappings: RoleMappings = Map(
      "administrator" -> withShortForm(Administrator),
      "teacher"       -> withShortForm(Instructor),
      "professor"     -> withShortForm(Instructor),
      "student"       -> withShortForm(Learner),
      "instructor"    -> withShortForm(Instructor)
    )

    private def withShortForm(s: String): NonEmptyList[String] =
      NonEmptyList(s, s.replaceAll(Prefix, ""))
  end Context1p3
end LtiRoles
