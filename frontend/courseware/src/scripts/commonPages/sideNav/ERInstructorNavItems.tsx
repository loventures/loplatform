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

import { trackViewInstructorResourcesEvent } from '../../analytics/trackEvents';
import { LeftNavListItem } from '../../commonPages/sideNav/LeftNavListItem';
import { useBookmarks } from '../../components/bookmarks/bookmarksReducer';
import {
  enableAnalyticsPage,
  instructorLinkChecker,
  instructorPurgeDiscussions,
  qnaEnabled,
} from '../../utilities/preferences';
import { useCourseSelector } from '../../loRedux';
import { resetQnaQuery } from '../../qna/qnaActions';
import QnaIcon from '../../qna/QnaIcon';
import {
  AnalyticsPageLink,
  BookmarksLink,
  DiscussionListPageLink,
  InstructorAssignmentListPageLink,
  InstructorCompetenciesPageLink,
  InstructorControlsHomeLink,
  InstructorControlsLink,
  InstructorGradebookGradesPageLink,
  InstructorGradebookLink,
  InstructorLearnerListPageLink,
  InstructorProgressReportPageLink,
  QnaPageLink,
} from '../../utils/pageLinks';
import { useTranslation } from '../../i18n/translationContext';
import { selectRouter } from '../../utilities/rootSelectors';
import React from 'react';
import {
  FiBookmark,
  FiClipboard,
  FiDownload,
  FiExternalLink,
  FiFile,
  FiSettings,
  FiTrendingUp,
  FiUsers,
} from 'react-icons/fi';
import { IoChatbubblesOutline, IoCheckboxOutline, IoShieldCheckmarkOutline } from 'react-icons/io5';
import { useDispatch } from 'react-redux';
import { ListGroupItem } from 'reactstrap';

import { selectCourseStatus } from '../../landmarks/pageHeader/pageHeaderSelectors';

const ERInstructorNavItems: React.FC = () => {
  const translate = useTranslation();
  const {
    hasAssignments,
    hasCompetencies,
    hasDiscussions,
    progressReportPageEnabled,
    instructorResources,
    instructorControlsV2,
  } = useCourseSelector(selectCourseStatus);
  const bookmarks = useBookmarks();
  const dispatch = useDispatch();
  const path = useCourseSelector(s => selectRouter(s).path);

  const downloadInstructorResources = !!instructorResources?.match(/zip$/i);

  return (
    <>
      {(instructorControlsV2 || instructorLinkChecker || instructorPurgeDiscussions) && (
        <LeftNavListItem
          id="course-nav-instructor-controls"
          to={InstructorControlsHomeLink}
          prefix={InstructorControlsLink}
        >
          <FiSettings
            aria-hidden={true}
            strokeWidth={1.5}
            stroke="#2e4954"
            className="me-2"
          />
          <span>{translate('ER_INSTRUCTOR_TOOLS')}</span>
        </LeftNavListItem>
      )}

      {instructorResources && (
        <ListGroupItem id="course-nav-instructor-resources">
          <a
            download={downloadInstructorResources}
            target="_blank"
            rel="noopener noreferrer"
            className="course-navigation-link"
            href={instructorResources}
            onClick={trackViewInstructorResourcesEvent}
          >
            <div className="d-flex justify-content-between align-items-center w-100">
              <span>
                <FiFile
                  aria-hidden={true}
                  strokeWidth={1.5}
                  stroke="#2e4954"
                  className="me-2"
                />
                {translate('INSTRUCTOR_RESOURCE')}
              </span>
              {downloadInstructorResources ? (
                <FiDownload
                  size="1rem"
                  strokeWidth={2}
                  title={translate('DOWNLOAD')}
                  aria-hidden={true}
                  style={{ marginLeft: '.25rem', marginRight: '-.75rem' }}
                />
              ) : (
                <FiExternalLink
                  size="1rem"
                  strokeWidth={2}
                  title={translate('NEW_WINDOW')}
                  aria-hidden={true}
                  style={{ marginLeft: '.25rem', marginRight: '-.75rem' }}
                />
              )}
            </div>
          </a>
        </ListGroupItem>
      )}

      <LeftNavListItem
        id="course-nav-instructor-student-list"
        to={InstructorLearnerListPageLink}
      >
        <FiUsers
          aria-hidden={true}
          strokeWidth={1.5}
          stroke="#2e4954"
          className="me-2"
        />
        <span>{translate('INSTRUCTOR_STUDENTS')}</span>
      </LeftNavListItem>

      {enableAnalyticsPage && (
        <LeftNavListItem
          id="course-nav-instructor-analytics"
          to={AnalyticsPageLink}
        >
          <FiTrendingUp
            aria-hidden={true}
            strokeWidth={1.5}
            stroke="#2e4954"
            className="me-2"
          />
          <span>{translate('INSTRUCTOR_ANALYTICS')}</span>
        </LeftNavListItem>
      )}

      {progressReportPageEnabled && (
        <LeftNavListItem
          id="course-nav-instructor-student-progress"
          to={InstructorProgressReportPageLink}
        >
          <FiTrendingUp
            aria-hidden={true}
            strokeWidth={1.5}
            stroke="#2e4954"
            className="me-2"
          />
          <span>{translate('INSTRUCTOR_STUDENTS_PROGRESS')}</span>
        </LeftNavListItem>
      )}

      {qnaEnabled && (
        <LeftNavListItem
          id="course-nav-instructor-qna"
          to={QnaPageLink}
          onClick={() => {
            // if going to QnA from somewhere else, reset matrix query
            if (!QnaPageLink.match(path)) dispatch(resetQnaQuery());
          }}
        >
          <QnaIcon
            className="me-2"
            style={{ height: '1rem', width: '1rem', color: '#2e4954' }}
            aria-hidden={true}
          />
          <span>{translate('QNA_INSTRUCTOR_LINK')}</span>
        </LeftNavListItem>
      )}

      {hasAssignments && (
        <LeftNavListItem
          id="course-nav-instructor-assignments"
          to={InstructorAssignmentListPageLink}
        >
          <FiClipboard
            aria-hidden={true}
            strokeWidth={1.5}
            stroke="#2e4954"
            className="me-2"
          />
          <span>{translate('INSTRUCTOR_ASSIGNMENTS')}</span>
        </LeftNavListItem>
      )}

      <LeftNavListItem
        id="course-nav-instructor-gradebook"
        to={InstructorGradebookGradesPageLink}
        prefix={InstructorGradebookLink}
      >
        <IoCheckboxOutline
          aria-hidden={true}
          stroke="#2e4954"
          className="me-2"
        />
        <span>{translate('INSTRUCTOR_GRADEBOOK')}</span>
      </LeftNavListItem>

      {hasDiscussions && (
        <LeftNavListItem
          id="course-nav-instructor-discussion-list"
          to={DiscussionListPageLink}
        >
          <IoChatbubblesOutline
            aria-hidden={true}
            className="me-2"
            stroke="#2e4954"
          />
          <span>{translate('COURSE_DISCUSSION_BOARDS')}</span>
        </LeftNavListItem>
      )}

      {hasCompetencies && (
        <LeftNavListItem
          id="course-nav-instructor-competency-list"
          to={InstructorCompetenciesPageLink}
        >
          <IoShieldCheckmarkOutline
            aria-hidden={true}
            className="me-2"
            stroke="#2e4954"
          />
          <span>{translate('INSTRUCTOR_COMPETENCIES')}</span>
        </LeftNavListItem>
      )}
      {!!Object.keys(bookmarks).length && (
        <LeftNavListItem to={BookmarksLink}>
          <FiBookmark
            aria-hidden={true}
            strokeWidth={1.5}
            stroke="#2e4954"
            className="me-2"
          />
          <span>{translate('ER_BOOKMARKS')}</span>
        </LeftNavListItem>
      )}
    </>
  );
};

export default ERInstructorNavItems;
