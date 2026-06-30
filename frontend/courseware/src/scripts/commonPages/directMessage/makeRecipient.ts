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

import { pick } from 'lodash';

/**
 * Pure replacement for the AngularJS `new UserClass(role, user)` (utilities/User.js) used to build
 * directMessage recipients. `UserClass` did `extend(this, emptyUser, pick(user, userKeys))`, normalized the
 * role to a lowercase array, and froze the result. The send path (FullMessage.ts) reads `recipient.id`, and the
 * recipient picker (messaging/directives/recipientPicker.tsx) renders selected recipients via
 * `recipient.getName()` — so we replicate that one prototype method (as a closure, no `this`). Drained off
 * lojector for K3c.
 */
const EMPTY_USER = {
  _type: 'user',
  imageUrl: null as string | null,
  middleName: '',
  familyName: '',
  fullName: '',
  externalId: '',
  thumbnailId: '',
  emailAddress: '',
  userName: '',
  givenName: '',
  userState: '',
  title: null as string | null,
  id: 0,
  handle: '',
};

const USER_KEYS = Object.keys(EMPTY_USER);

/**
 * Pure replacement for `new UserClass(role, user)`: copies the known user keys onto a frozen-ish plain
 * object, normalizes the role to a lowercase array (`roles`), and exposes the two prototype methods that
 * downstream code reads — `getName()` and `getExternalId()`. Shared by `makeRecipient` (directMessage)
 * and `UserContainer` (userContainer.ts / UserListStore).
 */
// UserClass normalized each role: objects → `.name`, strings → lowercased.
const normalizeRole = (r: any) => {
  const v = r && typeof r === 'object' ? r.name : r;
  return typeof v === 'string' ? v.toLowerCase() : v;
};

export const makeUser = (user: any, role = 'student') => {
  const fields = { ...EMPTY_USER, ...pick(user, USER_KEYS), roles: [normalizeRole(role)] };
  return {
    ...fields,
    // UserClass.prototype.getName: fullName, else `${givenName} ${familyName}`.
    getName: () => (fields.fullName ? fields.fullName : `${fields.givenName} ${fields.familyName}`),
    // UserClass.prototype.getExternalId.
    getExternalId: () => fields.externalId,
  };
};

export const makeRecipient = (user: any, role = 'student') => makeUser(user, role);
