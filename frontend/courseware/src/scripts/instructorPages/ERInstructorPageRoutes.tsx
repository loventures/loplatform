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

import ERGradebookLearnerAssignmentsPage from '../commonPages/assignmentsPage/ERGradebookLearnerAssignmentsPage';
import ERContentPlayer from '../commonPages/contentPlayer/ERContentPlayer';
import ERContentPrinter from '../commonPages/contentPlayer/ERContentPrinter';
import ERDiscussionListPage from '../commonPages/discussionList/ERDiscussionListPage';
import ERBookmarksPage from '../components/bookmarks/ERBookmarksPage';
import ERSearchPage from '../components/search/ERSearchPage';
import ERActivityOverviewPage from '../instructorPages/activityOverview/ERActivityOverviewPage';
import AnalyticsPage from '../instructorPages/analytics/AnalyticsPage';
import ERAssignmentsPage from '../instructorPages/assignments/ERAssignmentsPage';
import ERInstructorCourseCompetenciesPage from '../instructorPages/competencyList/ERInstructorCourseCompetenciesPage';
import ERControlsPage from '../instructorPages/controls/ERControlsPage';
import ERInstructorDashboard from '../instructorPages/ERInstructorDashboard';
import ERGradebookPage from '../instructorPages/gradebook/ERGradebookPage';
import ERGraderPage from '../instructorPages/grader/ERGraderPage';
import ERLearnerListPage from '../instructorPages/learnerList/ERLearnerListPage';
import ProgressReportPage from '../instructorPages/progressReportPage/ProgressReportPage';
import InstructorQnaListPage from '../qna/InstructorQnaListPage';
import InstructorQnaQuestionPage from '../qna/InstructorQnaQuestionPage';
import { redirectPreserveParams } from '../utils/linkUtils';
import {
  contentSearch,
  enableAnalyticsPage,
  instructorControlsV2,
  instructorLinkChecker,
  instructorPurgeDiscussions,
  progressReportPageEnabled,
  qnaEnabled,
} from '../utilities/preferences';
import React from 'react';
import { Navigate, Route, Routes, useLocation, useParams } from 'react-router-dom';

import InstructorMulticastPage from '../qna/InstructorMulticastPage';

// Paths are relative to the /instructor/* mount in ERAppRoutes. v6 matches exactly by default, so
// non-leaf routes carry /* to keep v5's prefix-matching (e.g. gradebook/controls/content nest).
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

const QnaQuestionRoute: React.FC = () => {
  const { questionId } = useParams<{ questionId: string }>();
  return <InstructorQnaQuestionPage questionId={+questionId!} />;
};

const DashboardRedirect: React.FC = () => {
  const location = useLocation();
  return (
    <Navigate
      replace
      to={redirectPreserveParams('/instructor/dashboard', location)}
    />
  );
};

const ERInstructorPageRoutes = () => {
  return (
    <Routes>
      <Route
        path="dashboard/*"
        element={<ERInstructorDashboard />}
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
        path="learners/*"
        element={<ERLearnerListPage />}
      />

      {progressReportPageEnabled && (
        <Route
          path="progress-report/*"
          element={<ProgressReportPage />}
        />
      )}

      <Route
        path="assignments/:contentId/grader/*"
        element={<ERGraderPage />}
      />

      <Route
        path="assignments/:contentId/*"
        element={<ERActivityOverviewPage />}
      />

      <Route
        path="assignments/*"
        element={<ERAssignmentsPage />}
      />

      <Route
        path="gradebook/learner-assignments/*"
        element={<ERGradebookLearnerAssignmentsPage />}
      />

      <Route
        path="gradebook/*"
        element={<ERGradebookPage />}
      />

      <Route
        path="competencies/*"
        element={<ERInstructorCourseCompetenciesPage />}
      />

      {(instructorControlsV2 || instructorLinkChecker || instructorPurgeDiscussions) && (
        <Route
          path="controls/*"
          element={<ERControlsPage />}
        />
      )}

      <Route
        path="bookmarks/*"
        element={<ERBookmarksPage />}
      />

      {contentSearch && (
        <Route
          path="search/*"
          element={<SearchRoute />}
        />
      )}

      {enableAnalyticsPage && (
        <Route
          path="analytics/*"
          element={<AnalyticsPage />}
        />
      )}

      {qnaEnabled && (
        <>
          <Route
            path="qna"
            element={<InstructorQnaListPage />}
          />
          <Route
            path="qna/question/:questionId/*"
            element={<QnaQuestionRoute />}
          />
          <Route
            path="qna/send"
            element={<InstructorMulticastPage />}
          />
        </>
      )}

      <Route
        path="*"
        element={<DashboardRedirect />}
      />
    </Routes>
  );
};

export default ERInstructorPageRoutes;
