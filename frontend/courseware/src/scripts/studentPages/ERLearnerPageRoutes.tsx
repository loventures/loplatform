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

import ERLearnerAssignmentsListPage from '../commonPages/assignmentsPage/ERLearnerAssignmentsListPage';
import ERContentPlayer from '../commonPages/contentPlayer/ERContentPlayer';
import ERContentPrinter from '../commonPages/contentPlayer/ERContentPrinter';
import ERDiscussionListPage from '../commonPages/discussionList/ERDiscussionListPage';
import ERBookmarksPage from '../components/bookmarks/ERBookmarksPage';
import ERSearchPage from '../components/search/ERSearchPage';
import LearnerQnaPage from '../qna/LearnerQnaPage';
import ERStudentCourseCompetenciesPage from '../studentPages/courseCompetenciesPage/ERStudentCourseCompetenciesPage';
import ERStudentDashboard from '../studentPages/ERStudentDashboard';
import { redirectPreserveParams } from '../utils/linkUtils';
import { contentSearch } from '../utilities/preferences';
import React from 'react';
import { Navigate, Route, Routes, useLocation } from 'react-router-dom';

// Paths are relative to the /student/* mount in ERAppRoutes. The content-player and search
// routes need location.search/state, which v6 exposes via useLocation rather than a render prop.
const ContentPlayerRoute: React.FC = () => {
  const location = useLocation();
  return (
    <ERContentPlayer
      search={location.search}
      state={location.state}
    />
  );
};

const SearchRoute: React.FC = () => {
  const location = useLocation();
  return <ERSearchPage search={location.search} />;
};

const DashboardRedirect: React.FC = () => {
  const location = useLocation();
  return (
    <Navigate
      replace
      to={redirectPreserveParams('/student/dashboard', location)}
    />
  );
};

const ERLearnerPageRoutes: React.FC = () => (
  <Routes>
    <Route
      path="dashboard"
      element={<ERStudentDashboard />}
    />

    <Route
      path="content/:contentId/*"
      element={<ContentPlayerRoute />}
    />

    <Route
      path="print/:contentId/*"
      element={<ERContentPrinter />}
    />

    <Route
      path="discussions/*"
      element={<ERDiscussionListPage />}
    />

    <Route
      path="assignments/*"
      element={<ERLearnerAssignmentsListPage />}
    />

    <Route
      path="competencies/*"
      element={<ERStudentCourseCompetenciesPage />}
    />

    <Route
      path="bookmarks/*"
      element={<ERBookmarksPage />}
    />

    <Route
      path="qna/:questionId/*"
      element={<LearnerQnaPage />}
    />

    <Route
      path="qna/*"
      element={<LearnerQnaPage />}
    />

    {contentSearch && (
      <Route
        path="search/*"
        element={<SearchRoute />}
      />
    )}

    <Route
      path="*"
      element={<DashboardRedirect />}
    />
  </Routes>
);

export default ERLearnerPageRoutes;
