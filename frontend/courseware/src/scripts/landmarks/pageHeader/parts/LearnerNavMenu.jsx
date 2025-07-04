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

import { connect } from 'react-redux';
import { withTranslation } from '../../../i18n/translationContext';

import { selectCourseStatus } from '../pageHeaderSelectors';

import LoLink from '../../../components/links/LoLink';

import {
  UncontrolledDropdown as Dropdown,
  DropdownToggle,
  DropdownMenu,
  DropdownItem,
} from 'reactstrap';

import {
  CoursePageLink,
  LearnerCompetenciesPageLink,
  DiscussionListPageLink,
  LearnerAssignmentListPageLink,
} from '../../../utils/pageLinks';

const LearnerPageHeaderMenu = ({ translate, hasAssignments, hasCompetencies, hasDiscussions }) => (
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
      <DropdownItem
        tag={LoLink}
        id="course-nav-student-home"
        to={CoursePageLink.toLink()}
      >
        <span
          className="icon icon-location"
          role="presentation"
        ></span>
        <span>{translate('COURSE_HOME')}</span>
      </DropdownItem>

      {hasCompetencies && (
        <DropdownItem
          tag={LoLink}
          id="course-nav-student-learner-competencies"
          to={LearnerCompetenciesPageLink.toLink()}
        >
          <span
            className="icon icon-star-full"
            role="presentation"
          ></span>
          <span>{translate('COURSE_LEARNER_COMPETENCIES')}</span>
        </DropdownItem>
      )}

      {hasAssignments && (
        <DropdownItem
          tag={LoLink}
          id="course-nav-student-assignment-list"
          to={LearnerAssignmentListPageLink.toLink()}
        >
          <span
            className="icon icon-check-squared"
            role="presentation"
          ></span>
          <span>{translate('COURSE_ASSIGNMENT_LIST')}</span>
        </DropdownItem>
      )}

      {hasDiscussions && (
        <DropdownItem
          tag={LoLink}
          id="course-nav-student-discussion-list"
          to={DiscussionListPageLink.toLink()}
        >
          <span
            className="icon icon-bubbles"
            role="presentation"
          ></span>
          <span>{translate('COURSE_DISCUSSION_BOARDS')}</span>
        </DropdownItem>
      )}
    </DropdownMenu>
  </Dropdown>
);

export default connect(selectCourseStatus)(withTranslation(LearnerPageHeaderMenu));
