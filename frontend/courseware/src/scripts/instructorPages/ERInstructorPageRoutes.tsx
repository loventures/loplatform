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
import { Redirect, Route, Switch } from 'react-router';

import InstructorMulticastPage from '../qna/InstructorMulticastPage';

const ERInstructorPageRoutes = () => {
  return (
    <Switch>
      <Route path="/instructor/dashboard">
        <ERInstructorDashboard />
      </Route>

      <Route
        path="/instructor/content/:contentId"
        render={({ location }) => (
          <ERContentPlayer
            search={location.search}
            state={location.state}
          />
        )}
      />

      <Route path="/instructor/print/:contentId">
        <ERContentPrinter />
      </Route>

      <Route path="/instructor/discussions">
        <ERDiscussionListPage />
      </Route>

      <Route path="/instructor/learners">
        <ERLearnerListPage />
      </Route>

      {progressReportPageEnabled && (
        <Route path="/instructor/progress-report">
          <ProgressReportPage />
        </Route>
      )}

      <Route path="/instructor/assignments/:contentId/grader">
        <ERGraderPage />
      </Route>

      <Route path="/instructor/assignments/:contentId">
        <ERActivityOverviewPage />
      </Route>

      <Route path="/instructor/assignments">
        <ERAssignmentsPage />
      </Route>

      <Route path="/instructor/gradebook/learner-assignments">
        <ERGradebookLearnerAssignmentsPage />
      </Route>

      <Route path="/instructor/gradebook">
        <ERGradebookPage />
      </Route>

      <Route path="/instructor/competencies">
        <ERInstructorCourseCompetenciesPage />
      </Route>

      {(instructorControlsV2 || instructorLinkChecker || instructorPurgeDiscussions) && (
        <Route
          key="controls"
          path="/instructor/controls"
        >
          <ERControlsPage />
        </Route>
      )}

      <Route
        key="bookmarks"
        path="/instructor/bookmarks"
      >
        <ERBookmarksPage />
      </Route>

      {contentSearch && (
        <Route
          key="search"
          path="/instructor/search"
          render={({ location }) => <ERSearchPage search={location.search} />}
        />
      )}

      {enableAnalyticsPage && (
        <Route
          key="analytics"
          path="/instructor/analytics"
        >
          <AnalyticsPage />
        </Route>
      )}

      {qnaEnabled && (
        <>
          <Route
            key="qna"
            path="/instructor/qna"
            exact
          >
            <InstructorQnaListPage />
          </Route>
          <Route
            key="qnaquestion"
            path="/instructor/qna/question/:questionId"
            render={a => <InstructorQnaQuestionPage questionId={+a.match.params.questionId} />}
          ></Route>
          <Route
            key="multicast"
            path="/instructor/qna/send"
            exact
          >
            <InstructorMulticastPage />
          </Route>
        </>
      )}

      <Route
        key="default"
        render={({ location }) => (
          <Redirect to={redirectPreserveParams('/instructor/dashboard', location)} />
        )}
      />
    </Switch>
  );
};

export default ERInstructorPageRoutes;
