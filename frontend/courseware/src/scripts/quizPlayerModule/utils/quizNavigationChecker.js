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

import { findIndex, map } from 'lodash';

import {
  QUIZ_NAVIGATION_UNRESTRICTED,
  QUIZ_NAVIGATION_REQUIRE_ANSWER,
  QUIZ_NAVIGATION_ENFORCE_LINEAR,
  QUIZ_NAVIGATION_YOLO,
  ResultReleaseTimes,
} from '../../utilities/assessmentSettings.js';

export const checkCanGo = function (
  fromIndex,
  toIndex,
  fromQuestionTuple,
  toQuestionTuple,
  numQuestions,
  settings
) {
  //baisc dumb checks
  if (toIndex < 0 || toIndex > numQuestions - 1 || toIndex === fromIndex) {
    return false;
  }

  if (settings.assessmentNavigationPolicy) {
    switch (settings.assessmentNavigationPolicy) {
      case QUIZ_NAVIGATION_UNRESTRICTED:
        return true;
      case QUIZ_NAVIGATION_REQUIRE_ANSWER:
        return fromQuestionTuple.state.answered || toQuestionTuple.state.answered;
      case QUIZ_NAVIGATION_ENFORCE_LINEAR:
        return toIndex > fromIndex && fromQuestionTuple.state.answered;
      case QUIZ_NAVIGATION_YOLO:
        console.warn(QUIZ_NAVIGATION_YOLO, '!');
        return toIndex > fromIndex;
      default:
        return false;
    }
  } else if (settings.navigationPolicy) {
    const { backtrackingAllowed, skippingAllowed } = settings.navigationPolicy;

    if (toIndex - fromIndex === 1 && fromQuestionTuple.state.answered) {
      return true;
    } else if (toIndex < fromIndex) {
      return backtrackingAllowed;
    } else {
      return skippingAllowed;
    }
  }
};

export const getCanGoStatus = (currentIndex, questionTuples, settings) =>
  map(questionTuples, (toTuple, toIndex) => {
    return checkCanGo(
      currentIndex,
      questionTuples[currentIndex],
      toIndex,
      toTuple,
      questionTuples.length,
      settings
    );
  });

export const getIndexToGoAfter = (currentIndex, lastQuestionIndex, settings, isSkipping) => {
  if (
    !isSkipping &&
    settings.resultsPolicy.resultReleaseTime === ResultReleaseTimes.OnResponseScore
  ) {
    return currentIndex;
  } else if (currentIndex === lastQuestionIndex) {
    return -1;
  } else {
    return currentIndex + 1;
  }
};

export const getInitialQuestionIndex = questionTuples => {
  const firstUnansweredIndex = findIndex(questionTuples, tuple => {
    return !tuple.state.answered && !tuple.state.skipped;
  });

  if (firstUnansweredIndex !== -1) {
    return firstUnansweredIndex;
  } else {
    return questionTuples.length - 1;
  }
};

export const getShouldDisplaySkip = settings => {
  switch (settings.assessmentNavigationPolicy) {
    case QUIZ_NAVIGATION_REQUIRE_ANSWER:
    case QUIZ_NAVIGATION_ENFORCE_LINEAR:
      return true;
    default:
      return false;
  }
};
