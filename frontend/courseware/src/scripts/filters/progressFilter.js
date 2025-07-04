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

angular.module('lo.filters').filter('progress', function () {
  //The only use of this right now is 'color'
  //If we need to support other formats,
  //implment a grade filter like scheme
  return function (progress) {
    //If they pass in score percent this will render it.
    if (angular.isNumber(progress)) {
      progress = {
        pointsAwarded: progress,
        pointsPossible: 1,
      };
    }

    if (isEmpty(progress)) {
      return 0;
    }

    if (progress.weightedPercentage) {
      return Math.round(progress.weightedPercentage);
    } else {
      // meh, if we're not returning weightedPercentage it's a bug
      return 0;
    }
  };
});
