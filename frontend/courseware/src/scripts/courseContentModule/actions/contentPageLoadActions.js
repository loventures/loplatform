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

import { filter, isEmpty, keyBy, mapValues } from 'lodash';

import {
  createDataListUpdateMergeAction,
  createDataListUpdateReplaceAction,
} from '../../utilities/apiDataActions.js';
import { loadingActionCreatorMaker } from '../../utilities/loadingStateUtils.js';

import { overallGradeMerge } from '../../loRedux/overallGradeByUser.js';
import { batchActions } from 'redux-batched-actions';
import { loaded } from '../../types/loadable.js';
import { updateContentsAction } from '../../loRedux/contentsReducer.js';
import { scormCheckStateAction } from '../../scorm/actions.js';
import { activityLoadedActionCreator } from '../../api/attemptOverviewApi.js';
import contentsResource from '../../resources/ContentsResource.js';
import Course from '../../bootstrap/course.js';
import { contentsToContentResponse } from '../../utilities/contentResponse.js';

const contentsLoadedActionCreator = contents =>
  createDataListUpdateMergeAction('contentItems', contents);

const contentRelationsLoadedActionCreator = contentRelations =>
  createDataListUpdateMergeAction('contentRelations', contentRelations);

const overallProgressLoadedActionCreator = (overallProgress, userId) =>
  createDataListUpdateMergeAction('overallProgressByUser', {
    [userId]: overallProgress,
  });

const overallGradeLoadedActionCreator = (overallGrade, userId) =>
  overallGradeMerge({
    [userId]: overallGrade,
  });

const gatingInformationLoadedActionCreator = (gatingInformationByContent, userId) =>
  createDataListUpdateMergeAction('gatingInformationByContentByUser', {
    [userId]: gatingInformationByContent,
  });

const progressLoadedActionCreator = (progressByContent, userId) =>
  createDataListUpdateMergeAction('progressByContentByUser', {
    [userId]: progressByContent,
  });

const dueDateExemptLoadedActionCreator = (dueDateExemptByContent, userId) =>
  createDataListUpdateMergeAction('dueDateExemptByContentByUser', {
    [userId]: dueDateExemptByContent,
  });

const gradesLoadedActionCreator = (gradesByContent, userId) =>
  createDataListUpdateMergeAction('gradeByContentByUser', {
    [userId]: gradesByContent,
  });

const competenciesLoadedActionCreator = competenciesByContent =>
  createDataListUpdateMergeAction('competenciesByContent', competenciesByContent);

export const refreshContentActionCreator =
  (userId = window.lo_platform.user.id) =>
  dispatch => {
    contentsResource
      .refetch(Course.id, userId)
      .then(srs => contentsToContentResponse(srs.objects, userId))
      .then(({ contents = {}, contentRelations = {}, contentUserData = {} }) => {
        const { gatingInformation } = contentUserData;
        dispatch(
          batchActions([
            createDataListUpdateReplaceAction('contentItems', contents),
            createDataListUpdateReplaceAction('contentRelations', contentRelations),
            createDataListUpdateReplaceAction('gatingInformationByContentByUser', {
              [userId]: gatingInformation,
            }),
          ])
        );
      });
  };

// crazy transforms over contentResponse to build the proper shape for each slice.
export const loadedActionsCreator = ({
  studentId,

  contentRelations = {},
  contents = {},
  contentUserData = {},

  overallProgress,
  overallGrade,

  rawData,
}) => {
  const actions = [];

  if (!isEmpty(rawData)) {
    // adds to api.contents
    actions.push(updateContentsAction({ [studentId]: loaded(rawData) }));
  }

  if (!isEmpty(contents)) {
    // adds to api.contentItems
    actions.push(contentsLoadedActionCreator(contents));
    const contentWithCompetencies = keyBy(
      filter(contents, content => !isEmpty(content.competencies)),
      'id'
    );

    if (!isEmpty(contentWithCompetencies)) {
      const competenciesByContent = mapValues(
        contentWithCompetencies,
        content => content.competencies
      );
      // adds to api.competenciesByContent
      actions.push(competenciesLoadedActionCreator(competenciesByContent));
    }
  }

  if (!isEmpty(contentRelations)) {
    // adds to api.contentRelations
    actions.push(contentRelationsLoadedActionCreator(contentRelations));
  }

  if (!isEmpty(overallProgress)) {
    // adds to api.overallProgressByUser
    actions.push(overallProgressLoadedActionCreator(overallProgress, studentId));
  }

  if (!isEmpty(overallGrade)) {
    // adds to api.overallGradeByUser
    actions.push(overallGradeLoadedActionCreator(overallGrade, studentId));
  }

  const { gatingInformation, progress, grades, activities, dueDateExempt } = contentUserData;

  if (!isEmpty(gatingInformation)) {
    // adds to api.gatingInformationByContentByUser
    actions.push(gatingInformationLoadedActionCreator(gatingInformation, studentId));
  }
  if (!isEmpty(progress)) {
    // adds to api.progressByContentByUser
    actions.push(progressLoadedActionCreator(progress, studentId));
  }
  if (!isEmpty(dueDateExempt)) {
    // adds to api.dueDateExemptByContentByUser
    // TODO: Remove this. The map here comes from a field on each content item
    //      And the content is always requested in the context of a user (even previews).
    //      So any place that asks for this this slice should just ask content.dueDateExempt.
    actions.push(dueDateExemptLoadedActionCreator(dueDateExempt, studentId));
  }
  if (!isEmpty(grades)) {
    // adds to api.gradeByContentByUser
    actions.push(gradesLoadedActionCreator(grades, studentId));
  }
  if (!isEmpty(activities)) {
    // adds to api.activityByContentByUser
    actions.push(activityLoadedActionCreator(activities, studentId));
  }
  actions.push(scormCheckStateAction());

  return actions;
};

export const loadContentPlayerActionCreator = loadingActionCreatorMaker(
  { sliceName: 'contentPlayerLoadingState' },
  viewingAsId =>
    contentsResource.fetch(contentsResource.getKey(Course.id, viewingAsId), {
      redux: true,
      userId: viewingAsId,
    }),
  [],
  viewingAsId => ({ id: viewingAsId })
);
