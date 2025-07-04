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

import cats.syntax.option.*
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.learningobjects.de.authorization.Secured
import loi.cp.admin.right.CourseAdminRight
import loi.cp.config.JsonSchema.*
import loi.cp.config.{ConfigurationKey, ConfigurationKeyBinding}

import scala.collection.immutable.ListMap

@JsonIgnoreProperties(ignoreUnknown = true)
case class CoursePreferences(
  reviewPeriodOffset: Long = 0L,
  betaSelfStudyCourses: Boolean = false,
  allowPrintingEntireLesson: Boolean = false,
  editGatingPolicies: Boolean = true,
  isSearchEnabled: Boolean = false,
  lOFooter: Boolean = true,
  logoBrandingLO: Boolean = true,
  nearDueWarning: Option[Long] = Some(2),
  allowDirectMessaging: Boolean = true,
  groupChatFeature: Boolean = true,
  presenceChatFeature: Boolean = true,
  skippingIsOK: Boolean = true,
  progressReportPageEnabled: Boolean = false,
  instructorDashboardPageEnabled: Boolean = true,
  instructorControlsV2: Boolean = false,
  instructorRoster: Boolean = false,
  globalNavFeedback: Boolean = true,
  discussionJumpBar: Boolean = false,
  discussionTitle: Boolean = false,
  rubricGraderUseMaxCriterionValue: Boolean = false,
  CBLPROD16934InstructorResources: Option[String] = None,
  onlyAllowModuleHiding: Boolean = true,
  customLogoUrl: Option[String] = None,
  discussionReviewers: String = "",
  enableNotifications: Boolean = false,
  googleAnalyticsToken: Option[String] = None,
  allowAudioRecording: Boolean = false,
  strictDueDate: Boolean = false,
  hideUnassessableQuestions: Boolean = false,
  alwaysReleaseFeedback: Boolean = true,
  masteryEnabled: Boolean = true,
  enableInstructorLinkChecker: Boolean = false,
  enableContentSearch: Boolean = true,
  surveyCollectorUrl: Option[String] = None,
  @JsonDeserialize(contentAs = classOf[java.lang.Double])
  surveyCollectorLaterHours: Option[Double] = Some(48),
  CBLPROD18092GradebookSync: Boolean = false,
  useProjectedGrade: Boolean = false,
  ShowDiscussionBoardSummaries: Boolean = true,
  showContentSurveys: Boolean = false,
  disablePresence: Boolean = false,
  cdnWebAssets: Boolean = false,
  enableQualtrics: Boolean = false,
  eagerProgressRecalculation: Boolean = true,
  enableTutorials: Boolean = false,
  enableInlineLTI: Boolean = false,
  enableContentFeedback: Boolean = false,
  enableInstructorFeedback: Boolean = false,
  enableAnalyticsPage: Boolean = false,
  sectionDashboardId: Long = 10,
  learnerDashboardId: Long = 11,
  enableInstructorPostPurge: Boolean = false,
  enableVideoRecording: Boolean = false,
  enableQna: Boolean = false,
  topicQuizzing: Boolean = false,
  qnaCategories: ListMap[String, List[String]] = ListMap.empty,
  ltiISBN: Option[String] = None,
  ltiCourseKey: Option[String] = None,
  updateOutcomesOnPublish: UpdateOutcomesOnPublishSettings = UpdateOutcomesOnPublishSettings(),
)

@ConfigurationKeyBinding(
  value = "coursePreferences",
  read = new Secured(allowAnonymous = true),
  write = new Secured(value = Array(classOf[CourseAdminRight]))
)
object CoursePreferences extends ConfigurationKey[CoursePreferences]:
  final val key = "coursePreferences"

  override final val schema: Schema = Schema(
    title = "Course".some,
    properties = List(
      BooleanField(
        name = "showContentSurveys",
        description = Some("Show surveys to collect feedback on content to all students.")
      ),
      NumberField(name = "reviewPeriodOffset", description = Some("Default review period for courses (in hours).")),
      BooleanField(name = "betaSelfStudyCourses", description = Some("Self-study courses (Beta).")),
      BooleanField(name = "allowPrintingEntireLesson", description = Some("Allow printing entire lesson")),
      BooleanField(name = "editGatingPolicies", description = Some("Allow instructor to edit gates")),
      BooleanField(
        name = "isSearchEnabled",
        description = Some("Allow users to search for items in the current course")
      ),
      BooleanField(name = "lOFooter", description = Some("Allow users to be redirected from footer links")),
      BooleanField(name = "logoBrandingLO", description = Some("Allow users to go to the LO website from the footer")),
      NumberField(
        name = "nearDueWarning",
        description = Some("Changes the color of the due date indicator based on number of days before due date")
      ),
      BooleanField(name = "allowDirectMessaging", description = Some("Allow users to email other users in a course")),
      BooleanField(name = "groupChatFeature", description = Some("Allow users to access group chat in the course")),
      BooleanField(
        name = "presenceChatFeature",
        description = Some("Allow users to chat with other users in a course")
      ),
      BooleanField(name = "skippingIsOK", description = Some("Allow users to skip questions during a quiz")),
      BooleanField(
        name = "progressReportPageEnabled",
        description = Some("Show instructors learner progress within a course")
      ),
      BooleanField(name = "instructorDashboardPageEnabled", description = Some("Enable the instructor dashboard page")),
      BooleanField(name = "instructorControlsV2", description = Some("Enable the instructor controls page")),
      BooleanField(name = "instructorRoster", description = Some("Enable the instructor roster controls")),
      BooleanField(
        name = "globalNavFeedback",
        description = Some("Allow all users to apply feedback from global navigation dropdown")
      ),
      BooleanField(name = "discussionJumpBar", description = Some("Display the discussion jumpbar")),
      BooleanField(name = "discussionTitle", description = Some("Allow titles on discussion posts")),
      BooleanField(
        name = "rubricGraderUseMaxCriterionValue",
        description = Some("Default rubric grader to maximum value for criterion.")
      ),
      StringField(name = "CBLPROD16934InstructorResources", description = Some("Instructor Resources URL")),
      BooleanField(
        name = "onlyAllowModuleHiding",
        description = Some("Only allow modules to be hidden via instructor controls")
      ),
      StringField(name = "customLogoUrl", description = Some("Custom URL to use for rendering the navbar logo")),
      StringField(
        name = "discussionReviewers",
        description = Some("Comma separated usernames to escalate an inappropriate post to: Jan-2019")
      ),
      BooleanField(name = "enableNotifications", description = Some("When true, show the notifications bell")),
      StringField(name = "googleAnalyticsToken", description = Some("The google analytics token to use")),
      BooleanField(
        name = "allowAudioRecording",
        description = Some("Whether or not audio recording is allowed in the rich content editor")
      ),
      BooleanField(
        name = "strictDueDate",
        description = Some("When true, disallow learners editing or submitting attempts after the due date.")
      ),
      BooleanField(
        name = "hideUnassessableQuestions",
        description = Some("A question is unassessable when anything it assesses was not taught")
      ),
      BooleanField(
        name = "alwaysReleaseFeedback",
        description = Some("Learners can see instructor feedback before instructor posts grade")
      ),
      BooleanField(
        name = "masteryEnabled",
        description = Some(
          "When enabled, the course app will have the testing out feature enabled as well as the student competency master checkboxes"
        )
      ),
      BooleanField(name = "enableInstructorLinkChecker", description = Some("Enable link check report feature")),
      StringField(name = "surveyCollectorUrl", description = Some("Survey collector link")),
      NumberField(name = "surveyCollectorLaterHours", description = Some("Hours later for user to be notified again")),
      BooleanField(name = "CBLPROD18092GradebookSync", description = Some("Enable Force Sync button in gradebook")),
      BooleanField(
        name = "useProjectedGrade",
        description = Some("Use projected grade when displaying student grades")
      ),
      BooleanField(
        name = "ShowDiscussionBoardSummaries",
        description = Some("Disabling may improve performance in very large courses.")
      ),
      BooleanField(
        name = "disablePresence",
        description = Some("Disable use of presence entirely.")
      ),
      BooleanField(
        name = "cdnWebAssets",
        description = Some("Use external CDN for web assets.")
      ),
      BooleanField(
        name = "enableQualtrics",
        description = Some("Insert Qualtrics scripts on student pages.")
      ),
      BooleanField(
        name = "eagerProgressRecalculation",
        description = Some("Recalculate progress immediately upon course structure change")
      ),
      BooleanField(
        name = "enableTutorials",
        description = Some("Enable tutorials")
      ),
      BooleanField(
        name = "enableInlineLTI",
        description = Some("Enable inline LTI")
      ),
      BooleanField(
        name = "enableContentFeedback",
        description = Some("Enable content feedback")
      ),
      BooleanField(
        name = "enableInstructorFeedback",
        description = Some("Enable instructor feedback")
      ),
      BooleanField(
        name = "enableContentSearch",
        description = Some("Enable content search")
      ),
      BooleanField(
        name = "enableAnalyticsPage",
        description = Some("Enable analytics")
      ),
      NumberField(
        name = "sectionDashboardId",
        description = Some(
          "ID of the Metabase dashboard to use for section-level analytics"
        )
      ),
      NumberField(
        name = "learnerDashboardId",
        description = Some(
          "ID of the Metabase dashboard to use for learner-level analytics"
        )
      ),
      BooleanField(
        name = "enableInstructorPostPurge",
        description = Some("Enable course-wide discussion purge feature for instructors.")
      ),
      BooleanField(
        name = "enableVideoRecording",
        description = Some("Enables video recording in assignment submissions.")
      ),
      BooleanField(
        name = "enableQna",
        description = Some("Enables the Question and Answer feature.")
      ),
      BooleanField(
        name = "topicQuizzing",
        description = Some("Enables topic-based quizzing.")
      ),
      ObjectField(
        name = "qnaCategories",
        title = Some("Q&A Categorization"),
        additionalProperties = Some(ArrayField("Subcategory", "string"))
      ),
      StringField(
        name = "ltiISBN",
        title = Some("LTI ISBN"),
        description = Some("ISBN for IAC dispensation")
      ),
      StringField(
        name = "ltiCourseKey",
        title = Some("LTI Course Key"),
        description = Some("Remot Course Key")
      ),
      ObjectField(
        name = "updateOutcomesOnPublish",
        description = "Update LIS Line Items and re-scale existing grades when publishing new content".some,
        properties = List(
          BooleanField("enabled"),
          StringField("emailTo", description = "comma-separated email addresses".some),
          StringField(
            "emailCc",
            "Email CC".some,
            "comma-separated email addresses".some
          )
        )
      )
    )
  )

  override val init: CoursePreferences = new CoursePreferences

  /** Java access. */
  val instance: this.type = this
end CoursePreferences

case class UpdateOutcomesOnPublishSettings(
  enabled: Boolean = false,
  emailTo: Option[String] = None,
  emailCc: Option[String] = None
)
