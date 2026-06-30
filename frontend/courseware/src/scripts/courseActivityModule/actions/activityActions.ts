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

import { IncrementType, Progress } from '../../api/contentsApi.ts';
import Course from '../../bootstrap/course.ts';
import contentsResource from '../../resources/ContentsResource.ts';
import { ContentWithNebulousDetails } from '../../courseContentModule/selectors/contentEntry';
import { createDataListUpdateMergeAction } from '../../utilities/apiDataActions';
import { COURSE_ROOT } from '../../utilities/courseRootType';
import { Dispatch } from 'redux';
import { progressService } from '../../services/progressService.ts';

type ProgressReason = IncrementType | 'UNVISIT';

export type ProgressUpdates = {
  userId: number;
  progress: Record<string, Progress>;
};

export const reportProgressActionCreator =
  (content: ContentWithNebulousDetails, isVisited: boolean, reason?: ProgressReason) =>
  (dispatch: Dispatch) => {
    if (content.progress && !!content.progress.isFullyCompleted === !!isVisited) {
      // not always booleans
      return;
    }
    progressService.setProgress(content.id, isVisited, reason)
      .then(progressUpdates => {
        dispatch(
          createDataListUpdateMergeAction('progressByContentByUser', {
            [progressUpdates.userId]: progressUpdates.progress,
          })
        );
        const overall = progressUpdates.progress && progressUpdates.progress[COURSE_ROOT];
        if (overall) {
          dispatch(
            createDataListUpdateMergeAction('overallProgressByUser', {
              [progressUpdates.userId]: overall,
            })
          );
        }
        return progressUpdates;
      })
      .then(progressUpdates => {
        const key = contentsResource.getKey(Course.id, progressUpdates.userId);
        contentsResource.transform(key, progressUpdates);
        return progressUpdates;
      });
  };
