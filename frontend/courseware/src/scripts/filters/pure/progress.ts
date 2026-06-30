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

import { isEmpty } from 'lodash';

interface Progress {
  weightedPercentage?: number;
  pointsAwarded?: number;
  pointsPossible?: number;
}

/**
 * Render a progress object as a whole-number percentage.
 *
 * Behaviour is preserved verbatim from the original `progress` Angular filter:
 * only `weightedPercentage` is honoured, so a bare number (wrapped into a
 * points object that has no `weightedPercentage`) resolves to 0.
 */
export const progressPercent = (progress: number | Progress): number => {
  let p: Progress;
  if (typeof progress === 'number') {
    p = { pointsAwarded: progress, pointsPossible: 1 };
  } else {
    p = progress;
  }

  if (isEmpty(p)) {
    return 0;
  }

  if (p.weightedPercentage) {
    return Math.round(p.weightedPercentage);
  } else {
    // meh, if we're not returning weightedPercentage it's a bug
    return 0;
  }
};
