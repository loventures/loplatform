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

import React from 'react';

import { useAllEditedOutEdges, useEditedAssetTitle } from '../../graphEdit';
import { usePolyglot } from '../../hooks';
import { NarrativeSettings, plural, sentence } from '../story';
import { useIsEditable } from '../storyHooks';
import { AssessmentTypeEditor } from './components/AssessmentTypeEditor';
import { AttemptsEditor } from './components/AttemptsEditor';
import { CategoryEditor } from './components/CategoryEditor';
import { CreditEditor } from './components/CreditEditor';
import { DisplayConfidenceEditor } from './components/DisplayConfidenceEditor';
import { DurationEditor } from './components/DurationEditor';
import { ImmediateFeedbackEditor } from './components/ImmediateFeedbackEditor';
import { LayoutEditor } from './components/LayoutEditor';
import { PointsEditor } from './components/PointsEditor';
import { ScoringOptionEditor } from './components/ScoringOptionEditor';
import { ShowAnswersEditor } from './components/ShowAnswersEditor';
import { TimeLimitEditor } from './components/TimeLimitEditor';

// Structural html.1 also offers:
// . Keywords
// . Icon
// . Dependencies
// . Activity Resources
// . Duration

// Structural also offers:
// . Subtitle
// . Keywords
// . Icon
// . Soft Limit <- deliberately deprecating
export const AssessmentSettings: NarrativeSettings<'assessment.1'> = ({ asset }) => {
  const polyglot = usePolyglot();
  const editMode = useIsEditable(asset.name, 'EditSettings');

  const forCredit = asset.data.isForCredit;
  const allEdges = useAllEditedOutEdges(asset.name);
  const categoryEdge = allEdges.find(edge => edge.group === 'gradebookCategory');
  const categoryTitle = useEditedAssetTitle(categoryEdge?.targetName, 'Uncategorized');
  const points = asset.data.pointsPossible;
  const attempts = asset.data.unlimitedAttempts ? 0 : asset.data.maxAttempts;
  const scoringOption =
    attempts === 1 ? 'onlyAttemptScore' : (asset.data.scoringOption ?? 'mostRecentAttemptScore');
  const assessmentType = asset.data.assessmentType ?? 'formative';
  const singlePage = asset.data.singlePage;
  const displayConfidenceIndicators = asset.data.displayConfidenceIndicators;
  const immediateFeedback = asset.data.immediateFeedback;
  const hideAnswerIfIncorrect = asset.data.hideAnswerIfIncorrect;
  const duration = asset.data.duration;
  const maxMinutes = asset.data.maxMinutes;

  return editMode ? (
    <div className="mx-3 mb-3 text-center d-flex justify-content-center form-inline gap-2 parameter-center">
      <CreditEditor
        asset={asset}
        forCredit={forCredit}
      />
      <CategoryEditor
        asset={asset}
        category={categoryEdge}
      />
      <PointsEditor
        asset={asset}
        points={points}
      />
      <AssessmentTypeEditor
        asset={asset}
        assessmentType={assessmentType}
      />
      <AttemptsEditor
        asset={asset}
        attempts={attempts}
      />
      <ScoringOptionEditor
        asset={asset}
        attempts={attempts}
        scoringOption={scoringOption}
      />
      <TimeLimitEditor
        asset={asset}
        maxMinutes={maxMinutes}
      />
      <LayoutEditor
        asset={asset}
        singlePage={singlePage}
      />
      {!singlePage && (
        <DisplayConfidenceEditor
          asset={asset}
          displayConfidenceIndicators={displayConfidenceIndicators}
        />
      )}
      {!singlePage && (
        <ImmediateFeedbackEditor
          asset={asset}
          immediateFeedback={immediateFeedback}
        />
      )}
      <ShowAnswersEditor
        asset={asset}
        hideAnswerIfIncorrect={hideAnswerIfIncorrect}
      />
      <DurationEditor
        asset={asset}
        duration={duration}
      />
    </div>
  ) : (
    <div className="mx-3 mb-2 d-flex justify-content-center">
      <span className="input-padding text-muted text-center feedback-context">
        {sentence(
          polyglot.t(`STORY_forCredit_${forCredit}`),
          categoryTitle,
          plural(points, 'point'),
          assessmentType,
          !attempts ? 'unlimited attempts' : plural(attempts, 'attempt'),
          polyglot.t(`STORY_scoringOption_${scoringOption}`),
          !maxMinutes ? 'no time limit' : `${maxMinutes} minute time limit`,
          polyglot.t(`STORY_singlePage_${!!singlePage}`),
          singlePage ? '' : polyglot.t(`STORY_displayConfidence_${!!displayConfidenceIndicators}`),
          singlePage ? '' : polyglot.t(`STORY_immediateFeedback_${!!immediateFeedback}`),
          polyglot.t(`STORY_hideAnswerIfIncorrect_${!!hideAnswerIfIncorrect}`),
          !duration ? 'no duration set' : `${duration} minute duration`
        )}
      </span>
    </div>
  );
};
