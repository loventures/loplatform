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

/**
 * @ngdoc object
 * @alias Reverse
 * @memberOf lo.utilities
 * @description
 *  reverse some common values like 1 <=> -1, 'asc' <=> 'desc'
 **/
export default angular
  .module('lo.utilities.Reverse', [])
  .constant('ReverseMap', {
    1: -1,
    '-1': 1,
    asc: 'desc',
    ASC: 'DESC',
    desc: 'asc',
    DESC: 'ASC',
  })
  .service('Reverse', [
    'ReverseMap',
    function (ReverseMap) {
      return function (key) {
        return ReverseMap[key];
      };
    },
  ]);
