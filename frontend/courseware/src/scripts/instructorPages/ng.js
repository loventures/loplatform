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

import activityOverview from './activityOverview/ng';
import competencyList from './competencyList/ng';
import dashboard from './dashboard/ng';
import gatingEditorPage from './gatingEditorPage/ng';
import gradebook from './gradebook/ng';
import grader from './grader/ng';
import progressReportPage from './progressReportPage/ng';

export default angular.module('ple.instructorPages', [
  activityOverview.name,
  competencyList.name,
  dashboard.name,
  gatingEditorPage.name,
  gradebook.name,
  grader.name,
  progressReportPage.name,
]);
