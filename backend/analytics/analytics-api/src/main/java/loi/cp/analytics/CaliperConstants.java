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

package loi.cp.analytics;

import com.fasterxml.jackson.annotation.JsonValue;

public interface CaliperConstants {

    String CALIPER_ENTITY_ID_PROPERTY = "@id";
    String CALIPER_ENTITY_TYPE_PROPERTY = "@type";

    String EVENT_CONTEXT = "http://purl.imsglobal.org/ctx/caliper/v1/Context";

    String CALIPER_EVENT_PROPERTY_ASSESSMENT_CONTENT_TYPE = "contentType";
    String CALIPER_EVENT_PROPERTY_ASSESSMENT_ID = "assessmentId";
    String CALIPER_EVENT_PROPERTY_ASSESSMENT_NAME = "assessmentName";
    String CALIPER_EVENT_PROPERTY_ASSESSMENT_QUESTION_TYPE = "assessmentQuestionType";
    String CALIPER_EVENT_PROPERTY_ASSESSMENT_TYPE = "assessmentType";
    String CALIPER_EVENT_PROPERTY_ATTEMPT_ID = "attemptId";
    String CALIPER_EVENT_PROPERTY_AUTH_METHOD = "authenticationMethod";
    String CALIPER_EVENT_PROPERTY_BECAME_ACTIVE = "becameActive";
    String CALIPER_EVENT_PROPERTY_COMPETENCIES = "competencies";
    String CALIPER_EVENT_PROPERTY_CONFIDENCE_LEVEL = "confidenceLevel";
    String CALIPER_EVENT_PROPERTY_CONTENT_ITEM_ID = "contentItemId";
    String CALIPER_EVENT_PROPERTY_CONTENT_ITEM_TITLE = "contentItemTitle";
    String CALIPER_EVENT_PROPERTY_CONTENT_ITEM_TYPE = "contentItemType";
    String CALIPER_EVENT_PROPERTY_CORRECT_VALUES = "correctValues";
    String CALIPER_EVENT_PROPERTY_COURSE_EXTERNAL_ID = "courseExternalId";
    String CALIPER_EVENT_PROPERTY_COURSE_ID = "courseId";
    String CALIPER_EVENT_PROPERTY_EVIDENCE_ALIGNED_COMPETENCIES = "alignedCompetencies";
    String CALIPER_EVENT_PROPERTY_EVIDENCE_EVIDENCE_SOURCE_TYPE = "sourceType";
    String CALIPER_EVENT_PROPERTY_EVIDENCE_SCORE = "score";
    String CALIPER_EVENT_PROPERTY_EXTERNAL_ID = "externalId";
    String CALIPER_EVENT_PROPERTY_IMPERSONATED_ID = "impersonatedUserId";
    String CALIPER_EVENT_PROPERTY_LAST_ACTIVE = "lastActive";
    String CALIPER_EVENT_PROPERTY_MAX_SCORE = "maxScore";
    String CALIPER_EVENT_PROPERTY_RESPONSE = "response";
    String CALIPER_EVENT_PROPERTY_RESPONSES = "responses";
    String CALIPER_EVENT_PROPERTY_RESPONSE_CHAR_COUNT = "responseCharacterCount";
    String CALIPER_EVENT_PROPERTY_RUBRIC = "rubric";
    String CALIPER_EVENT_PROPERTY_SESSION_ID = "sessionId";
    String CALIPER_EVENT_PROPERTY_COMMIT_ID = "commitId";

    enum Source {
        IMS, LO
    }

    enum EventType {
        // plz keep alphabetized
        AssessmentEvent("AssessmentEvent", Source.IMS),
        AssessmentItemEvent("AssessmentItemEvent", Source.IMS),
        CompetencyEvidenceEvent("CompetencyEvidenceEvent", Source.LO),
        CourseEntryEvent("CourseEntryEvent", Source.LO),
        LtiLaunchEvent("LtiLaunchEvent", Source.IMS),
        MasteryEvent("MasteryEvent", Source.LO),
        MessageEvent("MessageEvent", Source.LO),
        NavigationEvent("NavigationEvent", Source.IMS),
        OutcomeEvent("OutcomeEvent", Source.IMS),
        PresenceEvent("PresenceEvent", Source.LO),
        QuestionGradedEvent("QuestionGradedEvent", Source.LO),
        SessionEvent("SessionEvent", Source.IMS)
        ;
        private static final String EventRoot = "http://purl.imsglobal.org/caliper/v1/";

        private String name;
        private final Source source;

        EventType(final String name, final Source source) {
            this.name = name;
            this.source = source;
        }

        @JsonValue
        public String toString() {
            return (Source.IMS.equals(source) ? EventRoot : "") + name;
        }
    }


    enum ActionType {
        // plz keep alphabetized
        Achieved("Achieved", Source.LO), // +mastery
        BecameActive("BecameActive", Source.LO), // presence
        BecameIdle("BecameIdle", Source.LO),  // presence
        Completed("Completed", Source.LO),
        Deactivated("Deactivated", Source.IMS),
        Entered("Entered", Source.LO), // for person.enter.course_section
        Generated("Generated", Source.LO), // +evidence
        Graded("Graded", Source.IMS),  // grading events
        Issued("Issued", Source.LO), // +badging events. TODO: update after the spec changes to be learner-centric
        LoggedIn("LoggedIn", Source.IMS),
        LoggedOut("LoggedOut", Source.IMS),
        NavigatedTo("NavigatedTo", Source.IMS),
        Posted("Posted", Source.LO),
        Saved("Saved", Source.LO), // +saving a question (purposely different from standardized "complete")
        Submitted("Submitted", Source.IMS), // submitting an assessment attempt
        Viewed("Viewed", Source.IMS)
        ;
        private static final String ActionRoot = "http://purl.imsglobal.org/vocab/caliper/v1/action#";

        private final String name;
        private final Source source;

        ActionType(final String name, final Source source) {
            this.name = name;
            this.source = source;
        }

        @JsonValue
        public String toString() {
            return (Source.IMS.equals(source) ? ActionRoot : "") + name;
        }
    }

    enum EntityType {
        Assertion("Assertion", Source.LO),
        Assessment("Assessment", Source.IMS),
        AssessmentItem("AssessmentItem", Source.IMS),
        AssignableDigitalResource("AssignableDigitalResource", Source.IMS),
        Attempt("Attempt", Source.IMS),
        BadgeClass("BadgeClass", Source.LO),
        Competency("Competency", Source.LO),
        CompetencySet("CompetencySet", Source.LO),
        CourseSection("CourseSection", Source.LO),
        DigitalResource("DigitalResource", Source.IMS),
        Evidence("Evidence", Source.LO),
        Message("Message", Source.LO),
        Person("Person", Source.IMS),
        Result("Result", Source.IMS),
        Session("Session", Source.IMS),
        SoftwareApplication("SoftwareApplication", Source.LO) // TODO: There's an EdApp; why don't we use it!?!
        ;
        private static final String EntityRoot = "http://purl.imsglobal.org/caliper/v1/lis/"; // Learning Information Services

        public final String name;
        private final Source source;

        EntityType(final String name, final Source source) {
            this.name = name;
            this.source = source;
        }

        @JsonValue
        public String toString() {
            return (Source.IMS.equals(source) ? EntityRoot : "") + name;
        }
    }
}
