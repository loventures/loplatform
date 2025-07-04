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

import { isEmpty } from 'lodash';

import { LEGACY_RESPONSE_SAVED } from '../../utilities/attemptStates.js';

export const getQuestionStates = (question, response, score, isLegacy) => {
  const skipped = response.selection && response.selection.skip;
  const answered = isLegacy
    ? !skipped && response.state === LEGACY_RESPONSE_SAVED
    : !skipped && !isEmpty(response.selection);
  const scored = !isEmpty(score);
  const correct = scored && score.pointsPossible === score.pointsAwarded;
  const incorrect = scored && !correct;
  return {
    answered,
    skipped,
    scored,
    correct,
    incorrect,
  };
};
