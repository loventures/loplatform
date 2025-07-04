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

import angular from 'angular';

/**
 * @ngdoc filter
 * @alias choiceOrdinalFilter
 * @memberOf lo.filters
 *
 * @description
 *  Showing the ordinal label for choices based on index
 */
export const ordinalLabels = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ'.split('');

export const choiceOrdinal = (index: number) => ordinalLabels[index];

angular.module('lo.filters').filter('choiceOrdinal', function () {
  return ($index: number) => {
    return choiceOrdinal($index);
  };
});
