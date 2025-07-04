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
import { Redirect, Route, Switch } from 'react-router';

const ERLearnerPageRoutes: React.FC = () => (
  <Switch>
    <Route
      path="/student/dashboard"
      exact={true}
    >
      <ERStudentDashboard />
    </Route>

    <Route
      path="/student/content/:contentId"
      render={({ location }) => (
        <ERContentPlayer
          search={location.search}
          state={location.state}
        />
      )}
    />

    <Route path="/student/print/:contentId">
      <ERContentPrinter />
    </Route>

    <Route path="/student/discussions">
      <ERDiscussionListPage />
    </Route>

    <Route path="/student/assignments">
      <ERLearnerAssignmentsListPage />
    </Route>

    <Route path="/student/competencies">
      <ERStudentCourseCompetenciesPage />
    </Route>

    <Route path="/student/bookmarks">
      <ERBookmarksPage />
    </Route>

    <Route path="/student/qna/:questionId">
      <LearnerQnaPage />
    </Route>

    <Route path="/student/qna">
      <LearnerQnaPage />
    </Route>

    {contentSearch && (
      <Route
        path="/student/search"
        render={({ location }) => <ERSearchPage search={location.search} />}
      />
    )}

    <Route
      render={({ location }) => (
        <Redirect to={redirectPreserveParams('/student/dashboard', location)} />
      )}
    />
  </Switch>
);

export default ERLearnerPageRoutes;
