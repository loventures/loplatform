/*
 * LO Platform copyright (C) 2007–2026 LO Ventures LLC.
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

import LoNavLink from '../../components/links/LoNavLink';
import { ContentFeedbackPagelet } from '../../instructorPages/controls/ContentFeedbackPagelet';
import { ContentSearchPagelet } from '../../instructorPages/controls/ContentSearchPagelet';
import CourseKeyPagelet from '../../instructorPages/controls/CourseKeyPagelet';
import DiscussionPurgePagelet from '../../instructorPages/controls/DiscussionPurgePagelet';
import LinkCheckerPagelet from '../../instructorPages/controls/LinkCheckerPagelet';
import PoliciesPage from '../../instructorPages/controls/PoliciesPage';
import { CourseCustomizer } from '../../instructorPages/customization/CourseCustomizer';
import ERContentContainer from '../../landmarks/ERContentContainer';
import {
  InstructorControlsContentFeedbackPageLink,
  InstructorControlsContentSearchPageLink,
  InstructorControlsCourseKeyLink,
  InstructorControlsCustomizePageLink,
  InstructorControlsDiscussionPurgePageLink,
  InstructorControlsHomeLink,
  InstructorControlsLinkCheckerPageLink,
  InstructorControlsPoliciesPageLink,
} from '../../utils/pageLinks';
import { useTranslation } from '../../i18n/translationContext';
import {
  contentSearch,
  enableInstructorFeedback,
  instructorControlsV2,
  instructorLinkChecker,
  instructorPurgeDiscussions,
  ltiCourseKey,
} from '../../utilities/preferences';
import React from 'react';
import { BsChatRightQuote, BsTrash } from 'react-icons/bs';
import { SlLink, SlMagnifier } from 'react-icons/sl';
import { Navigate, Route, Routes, useLocation } from 'react-router-dom';
import ERNonContentTitle from '../../commonPages/contentPlayer/ERNonContentTitle';

// Paths are relative to the /instructor/controls/* mount in ERInstructorPageRoutes.
const ContentSearchRoute: React.FC = () => {
  const location = useLocation();
  return <ContentSearchPagelet search={location.search} />;
};

const ERControlsPage: React.FC = () => {
  const translate = useTranslation();
  return (
    <ERContentContainer title={translate('ER_INSTRUCTOR_TOOLS')}>
      <div className="container p-0">
        <div className="card er-content-wrapper mb-2 m-md-3 m-lg-4">
          <div className="card-body pb-5 px-3 px-md-4 px-lg-5">
            <ERNonContentTitle label={translate('ER_INSTRUCTOR_TOOLS')} />

            <ul className="nav nav-tabs d-flex">
              <li className="nav-item">
                <LoNavLink
                  className="nav-link"
                  activeClassName="active"
                  to={InstructorControlsHomeLink.toLink()}
                  target={null}
                  disabled={false}
                >
                  {translate('CONTROLS_TAB_HOME')}
                </LoNavLink>
              </li>
              {instructorControlsV2 ? (
                <li className="nav-item">
                  <LoNavLink
                    className="nav-link"
                    activeClassName="active"
                    to={InstructorControlsCustomizePageLink.toLink()}
                    target={null}
                    disabled={false}
                  >
                    {translate('CONTROLS_TAB_CUSTOMIZE')}
                  </LoNavLink>
                </li>
              ) : null}
              {instructorControlsV2 ? (
                <li className="nav-item">
                  <LoNavLink
                    className="nav-link"
                    activeClassName="active"
                    to={InstructorControlsPoliciesPageLink.toLink()}
                    target={null}
                    disabled={false}
                  >
                    {translate('CONTROLS_TAB_POLICIES')}
                  </LoNavLink>
                </li>
              ) : null}
              {instructorLinkChecker ? (
                <li className="nav-item">
                  <LoNavLink
                    className="nav-link"
                    activeClassName="active"
                    to={InstructorControlsLinkCheckerPageLink.toLink()}
                    target={null}
                    disabled={false}
                  >
                    {translate('CONTROLS_TAB_LINK_CHECKER')}
                  </LoNavLink>
                </li>
              ) : null}
              {contentSearch ? (
                <li className="nav-item">
                  <LoNavLink
                    className="nav-link"
                    activeClassName="active"
                    to={InstructorControlsContentSearchPageLink.toLink()}
                    target={null}
                    disabled={false}
                  >
                    {translate('CONTROLS_TAB_CONTENT_SEARCH')}
                  </LoNavLink>
                </li>
              ) : null}
              {enableInstructorFeedback ? (
                <li className="nav-item">
                  <LoNavLink
                    className="nav-link"
                    activeClassName="active"
                    to={InstructorControlsContentFeedbackPageLink.toLink()}
                    target={null}
                    disabled={false}
                  >
                    {translate('CONTROLS_TAB_CONTENT_FEEDBACK')}
                  </LoNavLink>
                </li>
              ) : null}
              {instructorPurgeDiscussions ? (
                <li className="nav-item">
                  <LoNavLink
                    className="nav-link"
                    activeClassName="active"
                    to={InstructorControlsDiscussionPurgePageLink.toLink()}
                    target={null}
                    disabled={false}
                  >
                    {translate('CONTROLS_TAB_DISCUSSION_PURGE')}
                  </LoNavLink>
                </li>
              ) : null}
              {ltiCourseKey ? (
                <li className="nav-item">
                  <LoNavLink
                    className="nav-link"
                    activeClassName="active"
                    to={InstructorControlsCourseKeyLink.toLink()}
                    target={null}
                    disabled={false}
                  >
                    {translate('CONTROLS_TAB_COURSE_KEY')}
                  </LoNavLink>
                </li>
              ) : null}
            </ul>

            <Routes>
              {instructorControlsV2 ? (
                <Route
                  path="customize/*"
                  element={<CourseCustomizer />}
                />
              ) : null}
              {instructorControlsV2 ? (
                <Route
                  path="policies/*"
                  element={<PoliciesPage />}
                />
              ) : null}

              {instructorLinkChecker ? (
                <Route
                  path="link-checker/*"
                  element={<LinkCheckerPagelet />}
                />
              ) : null}

              {contentSearch ? (
                <Route
                  path="content-search/*"
                  element={<ContentSearchRoute />}
                />
              ) : null}

              {enableInstructorFeedback ? (
                <Route
                  path="content-feedback/*"
                  element={<ContentFeedbackPagelet />}
                />
              ) : null}

              {instructorPurgeDiscussions ? (
                <Route
                  path="purge-posts/*"
                  element={<DiscussionPurgePagelet />}
                />
              ) : null}

              {ltiCourseKey ? (
                <Route
                  path="course-key/*"
                  element={<CourseKeyPagelet />}
                />
              ) : null}

              <Route
                path="home/*"
                element={
                  <div className="instructor-tools-page mt-3">
                    Instructor Tools are designed to support you as the instructor. Here you will
                    find tools that help you as you manage your students and provide learner
                    assistance. Additionally, you will find tools to assist you with reviewing and
                    updating the course content.
                  </div>
                }
              />

              <Route
                path=""
                element={
                  <Navigate
                    to="/instructor/controls/home"
                    replace
                  />
                }
              />
            </Routes>
          </div>
        </div>
      </div>
    </ERContentContainer>
  );
};

export default ERControlsPage;
