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

import ScoreHorseshoe from '../../../../components/circles/ScoreHorseshoe.jsx';
import dayjs from 'dayjs';
import localized from 'dayjs/plugin/localizedFormat';
import { viewParentFromContentActionCreator } from '../../../../courseContentModule/actions/contentPageActions.js';
import { withTranslation } from '../../../../i18n/translationContext.js';
import { connect } from 'react-redux';
import { lojector } from '../../../../loject.js';

dayjs.extend(localized);

const QuizAttemptScorePanel = ({ translate, attempt }) => (
  <div className="chart-results-wrap my-3">
    <div className="results-pad" />
    {attempt.score && (
      <div
        className="chart-quiz-results"
        aria-labelledby="score-horseshoe-percentage score-horseshoe-correct"
      >
        <ScoreHorseshoe
          correct={attempt.score.itemsCorrect}
          incorrect={attempt.score.itemsIncorrect}
        >
          <div
            id="score-horseshoe-percentage"
            className="h2 score half-sized-percent"
          >
            {lojector.get('gradeFilter')(attempt.score, 'percent')}
          </div>
          <div
            id="score-horseshoe-correct"
            className="h4"
          >
            {translate('SCORE_LABEL_CORRECT')}
          </div>
        </ScoreHorseshoe>
      </div>
    )}

    <div className="results-info">
      {attempt.score && (
        <p className="h5">
          {translate('ASSESSMENT_RESULTS_SCORE_FORMAT', {
            correct: attempt.score.itemsCorrect,
            total: attempt.score.itemsCorrect + attempt.score.itemsIncorrect,
          })}
        </p>
      )}

      <div className="flex-col-fluid attempt-submission-time print-only mb-3">
        {dayjs(attempt.submitTime).format('LLL')}
      </div>
    </div>
  </div>
);

export default connect(null, {
  visitParent: viewParentFromContentActionCreator,
})(withTranslation(QuizAttemptScorePanel));
