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

import { first } from 'lodash';

import { makeUser } from '../commonPages/directMessage/makeRecipient';

/**
 * Pure-TS port of the AngularJS `.factory('UserContainer')` (users/UserContainer.js), which wrapped
 * `new UserClass(role, profileOrUser)`. The pure `makeUser` (makeRecipient.ts) reproduces UserClass's
 * field/role normalization plus the `getName()`/`getExternalId()` methods. Drained off lojector for K3c.
 */
export class UserContainer {
  user: ReturnType<typeof makeUser>;
  id: any;

  constructor(profileOrUser: any) {
    // TODO assume user has only one role for now
    // TODO Do we even need roles?
    const role = first(profileOrUser.role || profileOrUser.roles);

    this.user = makeUser(profileOrUser, role as any);

    this.id = this.user.id;
  }

  getName() {
    return this.user.getName();
  }

  getExternalId() {
    return this.user.getExternalId();
  }
}
