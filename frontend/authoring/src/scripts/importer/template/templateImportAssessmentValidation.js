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

import { isEmpty, isInteger, isUndefined, memoize, pickBy, toNumber } from 'lodash';

import { dcmStore } from '../../dcmStore';
import { error } from './templateImportError';
import * as TEMPLATE_IMPORT from './templateImportTranslationKeys';

// NOTE: this short circuits redux to get the translations from the store without passing them down.
const polyglot = memoize(() => dcmStore.getState().configuration.translations);
const getTranslatedValue = key => polyglot().t(key).toLowerCase();

export const validAssessmentTypes = [
  'assessment.1',
  'checkpoint.1',
  'diagnostic.1',
  'poolAssessment.1',
];
const validAssessmentSettingsColumns = [
  'assessmentTemplate',
  'pointsPossible',
  'singlePage',
  'immediateFeedback',
  'displayConfidenceIndicators',
  'hideAnswerIfIncorrect',
  'assessmentType',
  'maxAttempts',
  'maxMinutes',
  'scoringOption',
  'numberOfQuestionsForAssessment',
  'isForCredit',
  'instructions',
];
const validScoringOptions = [
  'firstAttemptScore',
  'mostRecentAttemptScore',
  'highestScore',
  'averageScore',
  'fullCreditOnAnyCompletion',
];

function validateInstructions(instructions) {
  if (isUndefined(instructions)) {
    return {};
  } else {
    return {
      instructions: {
        parts: [
          {
            html: instructions,
            renderedHtml: instructions,
            partType: 'html',
          },
        ],
        renderedHtml: instructions,
        partType: 'block',
      },
    };
  }
}

function validateIsForCredit(isForCreditFromInput) {
  if (isUndefined(isForCreditFromInput)) {
    return {};
  }
  const no = getTranslatedValue('IS_FOR_CREDIT_FALSE');
  const yes = getTranslatedValue('IS_FOR_CREDIT_TRUE');
  const isForCredit = isForCreditFromInput.toLowerCase();
  if (isForCredit === no) {
    return { isForCredit: false };
  } else if (isForCredit === yes) {
    return { isForCredit: true };
  } else {
    return {
      isForCredit: error(TEMPLATE_IMPORT.IS_FOR_CREDIT_INVALID, {
        inputValue: isForCreditFromInput,
      }),
    };
  }
}

function validateNumberOfQuestionsForAssessment(numberOfQuestionsForAssessmentFromInput, typeId) {
  if (typeId !== 'poolAssessment.1') {
    if (!isUndefined(numberOfQuestionsForAssessmentFromInput)) {
      return { ignoredKey: error(TEMPLATE_IMPORT.NUMBER_OF_QUESTIONS_POOL_ONLY, { typeId }) };
    } else {
      return {};
    }
  } else {
    if (isUndefined(numberOfQuestionsForAssessmentFromInput)) {
      return {};
    }
    const useAllQuestions = getTranslatedValue('USE_ALL_QUESTIONS');
    const numberOfQuestionsLower = numberOfQuestionsForAssessmentFromInput.toLowerCase();
    const numberOfQuestions = toNumber(numberOfQuestionsForAssessmentFromInput);
    if (numberOfQuestionsLower === useAllQuestions) {
      return {
        numberOfQuestionsForAssessment: 0,
        useAllQuestions: true,
      };
    } else if (
      !isFinite(numberOfQuestions) ||
      !isInteger(numberOfQuestions) ||
      numberOfQuestions < 0
    ) {
      return {
        ignoredKey: error(TEMPLATE_IMPORT.NUMBER_OF_QUESTIONS_INVALID, {
          inputValue: numberOfQuestionsForAssessmentFromInput,
        }),
      };
    } else {
      return {
        numberOfQuestionsForAssessment: numberOfQuestions,
        useAllQuestions: false,
      };
    }
  }
}

function validateScoringOption(scoringOptionFromInput, { maxAttempts }, typeId) {
  if (isUndefined(scoringOptionFromInput)) {
    return {};
  } else if (typeId === 'diagnostic.1') {
    return { scoringOption: error(TEMPLATE_IMPORT.SCORING_OPTION_DIAGNOSTIC) };
  } else if (maxAttempts === 1) {
    return { scoringOption: error(TEMPLATE_IMPORT.SCORING_OPTION_ONE_ATTEMPT) };
  }
  switch (scoringOptionFromInput.toLowerCase()) {
    case getTranslatedValue('firstAttemptScore'):
      return { scoringOption: validScoringOptions[0] };
    case getTranslatedValue('mostRecentAttemptScore'):
      return { scoringOption: validScoringOptions[1] };
    case getTranslatedValue('highestScore'):
      return { scoringOption: validScoringOptions[2] };
    case getTranslatedValue('averageScore'):
      return { scoringOption: validScoringOptions[3] };
    case getTranslatedValue('fullCreditOnAnyCompletion'):
      return { scoringOption: validScoringOptions[4] };
    default:
      return {
        scoringOption: error(TEMPLATE_IMPORT.SCORING_OPTION_INVALID, {
          inputValue: scoringOptionFromInput,
        }),
      };
  }
}

function validateMaxAttempts(maxAttemptsFromInput, typeId) {
  if (isUndefined(maxAttemptsFromInput)) {
    return {};
  }
  const unlimited = getTranslatedValue('UNLIMITED_ATTEMPTS');
  const maxAttemptsLower = maxAttemptsFromInput.toLowerCase();
  const maxAttempts = toNumber(maxAttemptsFromInput);
  if (typeId === 'diagnostic.1') {
    return { maxAttempts: error(TEMPLATE_IMPORT.MAX_ATTEMPTS_DIAGNOSTIC) };
  } else if (maxAttemptsLower === unlimited) {
    return { maxAttempts: 0 };
  } else if (!isFinite(maxAttempts) || !isInteger(maxAttempts) || maxAttempts <= 0) {
    return {
      maxAttempts: error(TEMPLATE_IMPORT.MAX_ATTEMPTS_INVALID, {
        inputValue: maxAttemptsFromInput,
      }),
    };
  }
  return { maxAttempts };
}

function validateMaxMinutes(maxMinutesFromInput, typeId) {
  if (isUndefined(maxMinutesFromInput)) {
    return {};
  }
  const maxMinutes = toNumber(maxMinutesFromInput);
  if (typeId === 'checkpoint.1') {
    return { maxMinutes: error(TEMPLATE_IMPORT.MAX_MINUTES_CHECKPOINT) };
  } else if (!isFinite(maxMinutes) || !isInteger(maxMinutes) || maxMinutes <= 0) {
    return {
      maxMinutes: error(TEMPLATE_IMPORT.MAX_MINUTES_INVALID, {
        inputValue: maxMinutesFromInput,
      }),
    };
  }
  return { maxMinutes };
}

function validateAssessmentType(assessmentTypeFromInput, typeId) {
  if (isUndefined(assessmentTypeFromInput)) {
    return {};
  }
  const formative = getTranslatedValue('ASSESSMENT_TYPE_FORMATIVE');
  const summative = getTranslatedValue('ASSESSMENT_TYPE_SUMMATIVE');
  const assessmentType = assessmentTypeFromInput.toLowerCase();
  if (typeId === 'diagnostic.1') {
    return { assessmentType: error(TEMPLATE_IMPORT.ASSESSMENT_TYPE_DIAGNOSTIC) };
  } else if (assessmentType === formative) {
    return { assessmentType: 'formative' };
  } else if (assessmentType === summative) {
    return { assessmentType: 'summative' };
  } else {
    return {
      assessmentType: error(TEMPLATE_IMPORT.ASSESSMENT_TYPE_INVALID, {
        inputValue: assessmentTypeFromInput,
      }),
    };
  }
}

function validateHideAnswerIfIncorrect(hideAnswerIfIncorrectFromInput) {
  if (isUndefined(hideAnswerIfIncorrectFromInput)) {
    return {};
  }
  const always = getTranslatedValue('ShowCorrectOptions');
  const forCorrectResponses = getTranslatedValue('HideCorrectOptions');
  const hideAnswerIfIncorrect = hideAnswerIfIncorrectFromInput.toLowerCase();
  if (hideAnswerIfIncorrect === always) {
    return { hideAnswerIfIncorrect: false };
  } else if (hideAnswerIfIncorrect === forCorrectResponses) {
    return { hideAnswerIfIncorrect: true };
  } else {
    return {
      hideAnswerIfIncorrect: error(TEMPLATE_IMPORT.HIDE_ANSWER_INVALID, {
        inputValue: hideAnswerIfIncorrectFromInput,
      }),
    };
  }
}

function validateConfidenceIndicators(displayConfidenceIndicatorsFromInput, { singlePage }) {
  if (isUndefined(displayConfidenceIndicatorsFromInput)) {
    return {};
  }
  const yes = getTranslatedValue('Use Confidence');
  const no = getTranslatedValue('Disable Confidence');
  const displayConfidenceIndicators = displayConfidenceIndicatorsFromInput.toLowerCase();
  if (singlePage === true && displayConfidenceIndicators === yes) {
    return { displayConfidenceIndicators: error(TEMPLATE_IMPORT.CONFIDENCE_INDICATOR_SINGLE_PAGE) };
  } else if (displayConfidenceIndicators === yes) {
    return { displayConfidenceIndicators: true };
  } else if (displayConfidenceIndicators === no) {
    return { displayConfidenceIndicators: false };
  } else {
    return {
      displayConfidenceIndicators: error(TEMPLATE_IMPORT.CONFIDENCE_INDICATOR_INVALID, {
        inputValue: displayConfidenceIndicatorsFromInput,
      }),
    };
  }
}

function validateImmediateFeedback(immediateFeedbackFromInput, { singlePage }) {
  if (isUndefined(immediateFeedbackFromInput)) {
    return {};
  }
  const afterEachQuestion = getTranslatedValue('FormativeAssessment');
  const afterAssessmentCompletion = getTranslatedValue('SummativeAssessment');
  const immediateFeedback = immediateFeedbackFromInput.toLowerCase();
  if (singlePage === true && immediateFeedback === afterEachQuestion) {
    return { immediateFeedback: error(TEMPLATE_IMPORT.IMMEDIATE_FEEDBACK_SINGLE_PAGE) };
  } else if (immediateFeedback === afterAssessmentCompletion) {
    return { immediateFeedback: false };
  } else if (immediateFeedback === afterEachQuestion) {
    return { immediateFeedback: true };
  } else {
    return {
      immediateFeedback: error(TEMPLATE_IMPORT.IMMEDIATE_FEEDBACK_INVALID, {
        inputValue: immediateFeedbackFromInput,
      }),
    };
  }
}

function validateSinglePage(singlePageFromInput) {
  if (isUndefined(singlePageFromInput)) {
    return {};
  }
  const oneQuestionPerPage = getTranslatedValue('MultiPageAssessment');
  const allQuestionsOn1Page = getTranslatedValue('SinglePageAssessment');
  const singlePage = singlePageFromInput.toLowerCase();
  if (singlePage === oneQuestionPerPage) {
    return { singlePage: false };
  } else if (singlePage === allQuestionsOn1Page) {
    return { singlePage: true };
  } else {
    return {
      singlePage: error(TEMPLATE_IMPORT.SINGLE_PAGE_INVALID, { inputValue: singlePageFromInput }),
    };
  }
}

function validatePointsPossible(pointsPossibleFromInput) {
  if (isUndefined(pointsPossibleFromInput)) {
    return {};
  }
  const pointsPossible = toNumber(pointsPossibleFromInput);
  if (!isFinite(pointsPossible) || pointsPossible < 1) {
    return {
      pointsPossible: error(TEMPLATE_IMPORT.POINTS_POSSIBLE_INVALID, {
        inputValue: pointsPossibleFromInput,
      }),
    };
  }
  return { pointsPossible };
}

function validateIndividualSettings(settings, typeId) {
  const pointsPossible = validatePointsPossible(settings.pointsPossible);
  const singlePage = validateSinglePage(settings.singlePage);
  const immediateFeedback = validateImmediateFeedback(settings.immediateFeedback, singlePage);
  const displayConfidenceIndicators = validateConfidenceIndicators(
    settings.displayConfidenceIndicators,
    singlePage
  );
  const hideAnswerIfIncorrect = validateHideAnswerIfIncorrect(settings.hideAnswerIfIncorrect);
  const assessmentType = validateAssessmentType(settings.assessmentType, typeId);
  const maxAttempts = validateMaxAttempts(settings.maxAttempts, typeId);
  const maxMinutes = validateMaxMinutes(settings.maxMinutes, typeId);
  const scoringOption = validateScoringOption(settings.scoringOption, maxAttempts, typeId);
  const numberOfQuestionsForAssessment = validateNumberOfQuestionsForAssessment(
    settings.numberOfQuestionsForAssessment,
    typeId
  );
  const isForCredit = validateIsForCredit(settings.isForCredit);
  const instructions = validateInstructions(settings.instructions);

  return {
    ...pointsPossible,
    ...singlePage,
    ...immediateFeedback,
    ...displayConfidenceIndicators,
    ...hideAnswerIfIncorrect,
    ...assessmentType,
    ...maxAttempts,
    ...maxMinutes,
    ...scoringOption,
    ...numberOfQuestionsForAssessment,
    ...isForCredit,
    ...instructions,
  };
}

function validateAssessmentStructure(row, typeId) {
  const settings = pickBy(row, (value, key) => {
    return validAssessmentSettingsColumns.includes(key) && value !== '';
  });
  const settingsExist = !isEmpty(settings);
  const onAssessment = validAssessmentTypes.includes(typeId);

  if (onAssessment) {
    return validateIndividualSettings(settings, typeId);
  } else {
    if (settingsExist) {
      return error(TEMPLATE_IMPORT.SETTINGS_FOR_NON_ASSESSMENT, { typeId: row.typeId });
    } else {
      return {};
    }
  }
}

export function validateAssessmentSettings(row, typeId) {
  const maybeSettings = validateAssessmentStructure(row, typeId);

  if (maybeSettings.err) {
    return { errorCollection: [maybeSettings] };
  }

  const errorCollection = Object.values(maybeSettings).filter(v => v.err);

  return errorCollection.length ? { errorCollection } : maybeSettings;
}
