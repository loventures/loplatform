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

import { createDataListUpdateMergeAction } from '../utilities/apiDataActions';

import PresenceService from '../presence/PresenceService';
import Course from '../bootstrap/course';
import contentsResource from '../resources/ContentsResource';

export default angular.module('course.bootstrap.progressListener', [PresenceService.name]).run([
  'PresenceService',
  '$ngRedux',
  function progressListener(PresenceService, $ngRedux) {
    PresenceService.on('ProgressUpdate', ({ courseId, overallProgress, progressReport }) => {
      if (Course.id != courseId) {
        return;
      }
      const key = contentsResource.getKey(courseId, progressReport.userId);
      contentsResource.transform(key, progressReport);
      $ngRedux.dispatch(
        createDataListUpdateMergeAction('overallProgressByUser', {
          [progressReport.userId]: overallProgress,
        })
      );
      $ngRedux.dispatch(
        createDataListUpdateMergeAction('progressByContentByUser', {
          [progressReport.userId]: progressReport.progress,
        })
      );
    });
  },
]);
