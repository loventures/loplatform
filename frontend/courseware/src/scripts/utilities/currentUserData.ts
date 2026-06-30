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

import { extend, map, pick } from 'lodash';

import { Roles } from './pure/roles.ts';
import type { CurrentUser } from './currentUser.ts';

/**
 * Pure-TS port of the AngularJS `User` (current-user) service's *current user*.
 *
 * The Angular `UserProvider` builds its current user at config time via
 * `UserProvider.initUser(RolesProvider.getPrimaryRole(), window.lo_platform.user)`
 * (see bootstrap/globalFeatures.js). This module mirrors that build exactly: it
 * reads `window.lo_platform.user` for the user fields and the pure `Roles`
 * singleton for the primary role.
 *
 * The current user is STATIC. The instructor preview / "view-as" mutation flow
 * (`UserClass.switch()` / `switchBack()`) is dead — React view-as is entirely
 * param-based (`viewingAsId`) over the `actualUser` redux slice — so the current
 * user never diverges from the bootstrap (actual) user. `isPreview()` is therefore
 * always `false`, and `recordActivity()` (`isActual() && isStudent()`) reduces to
 * `isStudent()`.
 *
 * The Angular `User` provider stays in place: the Angular `Request` instance does
 * `$injector.get('User')` for its X-UserId interceptor, and `UserContainer` /
 * `SendMessage` do `new UserClass(...)`. This module only drains the React/TS
 * reach-ins to the current-user *value*.
 *
 * Built lazily on first access so importing this module does not depend on
 * bootstrap (`globalFeatures` config block) having already populated `Roles` and
 * `window.lo_platform.user`.
 */

/** Fields the Angular provider copies onto a current user (its `emptyUser` keys). */
const emptyUser = {
  _type: 'user',
  imageUrl: null,
  middleName: '',
  familyName: '',
  fullName: '',
  externalId: '',
  thumbnailId: '',
  emailAddress: '',
  userName: '',
  givenName: '',
  userState: '',
  title: null,
  id: 0,
  handle: '',
};

const userKeys = Object.keys(emptyUser);

interface CurrentUserData extends CurrentUser {
  roles: string[];
  givenName: string;
  familyName: string;
}

const buildCurrentUser = (): CurrentUserData => {
  // Mirrors UserProvider.initUser(RolesProvider.getPrimaryRole(), window.lo_platform.user).
  const user = (typeof window !== 'undefined' && window.lo_platform && window.lo_platform.user) || ({} as any);
  const primaryRole = Roles.getPrimaryRole();

  const roles = map([primaryRole], r => (typeof r === 'string' ? r.toLowerCase() : r));

  const data = extend({}, emptyUser, pick(user, userKeys), { roles });

  const proto = {
    getId(this: CurrentUserData): number {
      return this.id;
    },
    getHandle(this: CurrentUserData): string {
      return this.handle;
    },
    getName(this: CurrentUserData): string {
      return this.fullName ? this.fullName : this.givenName + ' ' + this.familyName;
    },
    getRoles(this: CurrentUserData): string[] {
      return this.roles;
    },
    isStudent(this: CurrentUserData): boolean {
      return this.roles.indexOf('student') !== -1;
    },
    isInstructor(this: CurrentUserData): boolean {
      return this.roles.indexOf('instructor') !== -1;
    },
    // The view-as mutation flow is dead, so the current user is always the actual user.
    isPreview(): boolean {
      return false;
    },
    isStrictlyInstructor(this: CurrentUserData): boolean {
      return !this.isPreview() && !!Roles.isStrictlyInstructor();
    },
    // Original: isActual() && isStudent(). isActual() is always true (no switch).
    recordActivity(this: CurrentUserData): boolean {
      return this.isStudent();
    },
  };

  return Object.assign(data, proto) as unknown as CurrentUserData;
};

let instance: CurrentUserData | undefined;

const get = (): CurrentUserData => {
  if (!instance) {
    instance = buildCurrentUser();
  }
  return instance;
};

/**
 * Pure-TS current-user singleton. Property/method access is forwarded to the
 * lazily-built value, so callers can `import { currentUser }` at module scope and
 * read it after bootstrap.
 */
export const currentUser: CurrentUserData = new Proxy({} as CurrentUserData, {
  get(_t, prop) {
    const target = get() as any;
    const value = target[prop];
    return typeof value === 'function' ? value.bind(target) : value;
  },
  has(_t, prop) {
    return prop in get();
  },
  ownKeys() {
    return Reflect.ownKeys(get());
  },
  getOwnPropertyDescriptor(_t, prop) {
    return Object.getOwnPropertyDescriptor(get(), prop);
  },
}) as CurrentUserData;

export default currentUser;
