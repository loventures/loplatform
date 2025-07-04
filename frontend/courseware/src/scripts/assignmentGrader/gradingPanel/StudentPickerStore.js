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

import LocalResourceStore from '../../srs/LocalResourceStore.js';

export default angular
  .module('lo.assignmentGrader.studentPickerStore', [LocalResourceStore.name])
  .factory('StudentPickerStore', [
    'LocalResourceStore',
    function (LocalResourceStore) {
      class StudentPickerStore extends LocalResourceStore {
        constructor(grader) {
          super();
          this.grader = grader;

          this.searchByProps = {
            GIVEN_NAME: 'givenName',
            FAMILY_NAME: 'familyName',
            USER_NAME: 'userName',
          };

          this.sortByProps = {
            GIVEN_NAME_ASC: {
              property: 'givenName',
              order: 1,
            },
            GIVEN_NAME_DESC: {
              property: 'givenName',
              order: -1,
            },
            FAMILY_NAME_ASC: {
              property: 'familyName',
              order: 1,
            },
            FAMILY_NAME_DESC: {
              property: 'familyName',
              order: -1,
            },
          };
        }

        doRemoteLoad() {
          return this.grader.loadUsers();
        }
      }

      return StudentPickerStore;
    },
  ]);
