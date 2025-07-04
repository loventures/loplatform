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

import { usePolyglot } from '../../hooks';
import { NarrativeSettings, plural, sentence } from '../story';
import { useIsEditable } from '../storyHooks';
import { DisplayConfidenceEditor } from './components/DisplayConfidenceEditor';
import { DurationEditor } from './components/DurationEditor';
import { ImmediateFeedbackEditor } from './components/ImmediateFeedbackEditor';
import { LayoutEditor } from './components/LayoutEditor';
import { PointsEditor } from './components/PointsEditor';
import { ShowAnswersEditor } from './components/ShowAnswersEditor';
import { TimeLimitEditor } from './components/TimeLimitEditor';

// Structural also offers:
// . Counts for Credit <- deliberately not showing
// . Gradebook Category <- deliberately now showing
// . Subtitle
// . Keywords
// . Icon
export const DiagnosticSettings: NarrativeSettings<'diagnostic.1'> = ({ asset }) => {
  const polyglot = usePolyglot();
  const editMode = useIsEditable(asset.name, 'EditSettings');

  // None of the gradebook settings make any sense for a diagnostic. Even points is
  // a bit nonsense, but the student does see the value on their scores page.
  const points = asset.data.pointsPossible;
  const singlePage = asset.data.singlePage;
  const displayConfidenceIndicators = asset.data.displayConfidenceIndicators;
  const immediateFeedback = asset.data.immediateFeedback;
  const hideAnswerIfIncorrect = asset.data.hideAnswerIfIncorrect;
  const duration = asset.data.duration;
  const maxMinutes = asset.data.maxMinutes;

  return editMode ? (
    <div className="mx-3 mb-3 text-center d-flex justify-content-center form-inline gap-2 parameter-center">
      <PointsEditor
        asset={asset}
        points={points}
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
          plural(points, 'point'),
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
