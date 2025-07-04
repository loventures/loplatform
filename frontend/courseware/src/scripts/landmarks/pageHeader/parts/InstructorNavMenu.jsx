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

import LoLink from '../../../components/links/LoLink';
import { openLearnerPreviewPickerActionCreator } from '../../../landmarks/learnerPreviewHeader/actions';
import {
  CoursePageLink,
  DiscussionListPageLink,
  InstructorAssignmentListPageLink,
  InstructorCompetenciesPageLink,
  InstructorControlsCustomizePageLink,
  InstructorDashboardPageLink,
  InstructorGradebookGradesPageLink,
  InstructorLearnerListPageLink,
  InstructorProgressReportPageLink,
} from '../../../utils/pageLinks';
import { trackViewInstructorResourcesEvent } from '../../../analytics/trackEvents';
import { withTranslation } from '../../../i18n/translationContext';
import { connect } from 'react-redux';
import {
  UncontrolledDropdown as Dropdown,
  DropdownItem,
  DropdownMenu,
  DropdownToggle,
} from 'reactstrap';

import { selectCourseStatus } from '../pageHeaderSelectors';

const InstructorPageHeaderMenu = ({
  translate,
  hasAssignments,
  hasCompetencies,
  hasDiscussions,
  progressReportPageEnabled,
  instructorDashboardPageEnabled,
  instructorResources,
  instructorControlsV2,
  enableInstructorLinkChecker,
  pickPreviewUser,
}) => (
  <Dropdown className="page-navbar-dropdown pages-dropdown">
    <DropdownToggle
      className="navbar-toggler border-0 icon icon-menu"
      aria-controls="page-nav-menu"
      color="primary"
    >
      <span className="sr-only">{translate('PAGE_HEADER_MENU_TOGGLE')}</span>
    </DropdownToggle>

    <DropdownMenu
      id="page-nav-menu"
      flip={false}
    >
      {instructorDashboardPageEnabled && (
        <DropdownItem
          tag={LoLink}
          id="course-nav-instructor-dashboard"
          to={InstructorDashboardPageLink.toLink()}
        >
          <span
            className="icon icon-gauge"
            role="presentation"
          ></span>
          <span>{translate('INSTRUCTOR_DASHBOARD')}</span>
        </DropdownItem>
      )}

      <DropdownItem
        tag={LoLink}
        id="course-nav-instructor-student-list"
        to={InstructorLearnerListPageLink.toLink()}
      >
        <span
          className="icon icon-user"
          role="presentation"
        ></span>
        <span>{translate('INSTRUCTOR_STUDENTS')}</span>
      </DropdownItem>

      {progressReportPageEnabled && (
        <DropdownItem
          tag={LoLink}
          id="course-nav-instructor-student-progress"
          to={InstructorProgressReportPageLink.toLink()}
        >
          <span
            className="icon icon-stats-bar-graph"
            role="presentation"
          ></span>
          <span>{translate('INSTRUCTOR_STUDENTS_PROGRESS')}</span>
        </DropdownItem>
      )}

      {hasCompetencies && (
        <DropdownItem
          tag={LoLink}
          id="course-nav-instructor-competency-list"
          to={InstructorCompetenciesPageLink.toLink()}
        >
          <span
            className="icon icon-star-full"
            role="presentation"
          ></span>
          <span>{translate('INSTRUCTOR_COMPETENCIES')}</span>
        </DropdownItem>
      )}

      <DropdownItem
        tag={LoLink}
        id="course-nav-instructor-content"
        to={CoursePageLink.toLink()}
      >
        <span
          className="icon icon-list"
          role="presentation"
        ></span>
        <span>{translate('INSTRUCTOR_CONTENT')}</span>
      </DropdownItem>

      {hasAssignments && (
        <DropdownItem
          tag={LoLink}
          id="course-nav-instructor-assignments"
          to={InstructorAssignmentListPageLink.toLink()}
        >
          <span
            className="icon icon-file-check"
            role="presentation"
          ></span>
          <span>{translate('INSTRUCTOR_ASSIGNMENTS')}</span>
        </DropdownItem>
      )}

      <DropdownItem
        tag={LoLink}
        id="course-nav-instructor-gradebook"
        to={InstructorGradebookGradesPageLink.toLink()}
      >
        <span
          className="icon icon-trophy"
          role="presentation"
        ></span>
        <span>{translate('INSTRUCTOR_GRADEBOOK')}</span>
      </DropdownItem>

      {hasDiscussions && (
        <DropdownItem
          tag={LoLink}
          id="course-nav-instructor-discussion-list"
          to={DiscussionListPageLink.toLink()}
        >
          <span
            className="icon icon-bubbles"
            role="presentation"
          ></span>
          <span>{translate('COURSE_DISCUSSION_BOARDS')}</span>
        </DropdownItem>
      )}

      {(instructorControlsV2 || enableInstructorLinkChecker) && (
        <DropdownItem
          tag={LoLink}
          id="course-nav-instructor-controls"
          to={InstructorControlsCustomizePageLink.toLink()}
        >
          <span
            className="icon icon-cog"
            role="presentation"
          ></span>
          <span>{translate('INSTRUCTOR_CONTROLS')}</span>
        </DropdownItem>
      )}

      <DropdownItem
        tag="button"
        id="course-nav-instructor-preview"
        onClick={pickPreviewUser}
      >
        <span
          className="icon icon-users"
          role="presentation"
        ></span>
        <span>{translate('INSTRUCTOR_PREVIEW')}</span>
      </DropdownItem>

      {instructorResources && (
        <DropdownItem
          tag="a"
          id="course-nav-instructor-resources"
          target="_blank"
          href={instructorResources}
          onClick={trackViewInstructorResourcesEvent}
        >
          <span
            className="icon icon-download"
            role="presentation"
          ></span>
          <span>{translate('INSTRUCTOR_RESOURCE')}</span>
        </DropdownItem>
      )}
    </DropdownMenu>
  </Dropdown>
);

const c1 = connect(selectCourseStatus);
const c2 = connect(null, { pickPreviewUser: openLearnerPreviewPickerActionCreator });

export default c1(c2(withTranslation(InstructorPageHeaderMenu)));
