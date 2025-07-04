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

import { map, isEmpty, find, findIndex, isObject } from 'lodash';

import {
  BASIC_SCORE,
  RUBRIC_SCORE,
  BASIC_FEEDBACK,
  RUBRIC_FEEDBACK,
} from '../../utilities/scoreTypes.js';

import { ATTEMPT_OPEN, ATTEMPT_FINALIZED } from '../../utilities/attemptStates.js';

export const buildBasicScore = (pointsPossible, pointsAwarded) => {
  return {
    scoreType: BASIC_SCORE,
    pointsPossible,
    pointsAwarded,
  };
};

export const buildRubricScore = (pointsPossible, rubric) => {
  return {
    scoreType: RUBRIC_SCORE,
    pointsPossible,
    nullableCriterionScores: rubric.getCriterionResponse(true),
  };
};

export const buildBasicFeedback = (comment, feedbackManager) => {
  return {
    feedbackType: BASIC_FEEDBACK,
    comment,
    uploads: feedbackManager.getFilesInStaging(),
    attachments: map(feedbackManager.getAttachedFiles(), 'id'),
  };
};

export const buildRubricFeedback = rubric => {
  const criterionFeedback = rubric.getCriterionFeedback();

  return map(criterionFeedback, (comment, sectionName) => {
    return {
      feedbackType: RUBRIC_FEEDBACK,
      sectionName,
      comment,
      uploads: [],
      attachments: [],
    };
  });
};

export const buildAllFeedback = (comment, feedbackManager, rubric) => {
  let feedback = [];
  if (comment || feedbackManager.hasFiles()) {
    feedback.push(buildBasicFeedback(comment, feedbackManager));
  }
  if (rubric) {
    const rubricFeedback = buildRubricFeedback(rubric);
    feedback = [...feedback, ...rubricFeedback];
  }
  return feedback;
};

export const getBasicResponseFeedback = ({ feedback = [], attachmentInfos = [] }) => {
  const basicFeedback = find(feedback, { feedbackType: BASIC_FEEDBACK });

  if (!basicFeedback) {
    return {};
  }

  return {
    comment: basicFeedback.comment,
    attachments: map(basicFeedback.attachments, attachment => {
      if (isObject(attachment) || isEmpty(attachmentInfos)) {
        return attachment;
      }
      return attachmentInfos[attachment];
    }),
  };
};

export const getRubricResponse = ({ score, feedback }, rubric) => {
  const criterionScores = (score && score.nullableCriterionScores) || {};
  if (!rubric || (score && score.scoreType === BASIC_SCORE)) {
    return null;
  }
  const responseByIndex = map(rubric.sections, section => {
    const sectionScore = criterionScores[section.name];
    const sectionFeedback = find(feedback, { sectionName: section.name }) || {};
    let levelIndex =
      sectionScore && findIndex(section.levels, { points: sectionScore.pointsAwarded });
    const manual = sectionScore && levelIndex === -1;
    return {
      levelIndex: levelIndex === -1 ? null : levelIndex,
      levelGrade: sectionScore && sectionScore.pointsAwarded,
      feedback: sectionFeedback.comment,
      manual,
    };
  });

  return responseByIndex;
};

export const buildScorableAttemptState = (attempt, userInfo) => {
  const requiresInstructorInput =
    attempt.state === ATTEMPT_OPEN
      ? true
      : findIndex(userInfo.gradeableAttempts, attemptId => attemptId === attempt.id) != -1;
  return {
    requiresInstructorInput,
    awaitsInstructorInput: isEmpty(attempt.score),
    hasInstructorInput: !isEmpty(attempt.score),
    scorePosted: attempt.state === ATTEMPT_FINALIZED,
  };
};
