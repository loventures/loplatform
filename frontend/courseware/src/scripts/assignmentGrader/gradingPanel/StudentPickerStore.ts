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

import { LocalResourceStore } from '../../srs/pure/localResourceStore.ts';

/**
 * Pure TS port of the AngularJS `StudentPickerStore` factory: the grader's
 * "change student" picker list, driving the React SRS list stack. The only dep
 * was the SRS base, which is now imported directly (`../../srs/pure/localResourceStore`),
 * so this is no longer an Angular service — the React picker constructs it directly.
 *
 * The pure base is a constructor *function*, so the `extends` clause must be cast
 * to a constructor type. Runtime is unaffected: the original `.js` subclass did
 * `class StudentPickerStore extends LocalResourceStore { super() }` against that
 * same plain constructor function.
 */
export class StudentPickerStore extends (LocalResourceStore as unknown as {
  new (...args: any[]): any;
}) {
  grader: any;

  constructor(grader: any) {
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
