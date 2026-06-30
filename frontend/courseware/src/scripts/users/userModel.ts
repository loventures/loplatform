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

import { defaults, each, intersection, isObject, isString, keys, map, pick } from 'lodash';

/*
 Moving the freeze stuff here because soon we won't need it. soon.
*/
// Some fancy ES5 stuff to prevent us from modifying user objects
// after they've been insantiated.
const freeze = function (obj: any, select?: any, unfreeze = false) {
  if (!Object.defineProperty || !Object.getOwnPropertyNames) {
    return obj;
  }

  const own = Object.getOwnPropertyNames(obj);
  const props = select ? intersection(select, own) : own;

  each(props, function (prop) {
    Object.defineProperty(obj, prop, {
      writable: unfreeze,
    });
  });

  return obj;
};

const emptyUser = {
  _type: 'user',
  imageUrl: null,
  middleName: '',
  familyName: '',
  fullName: '',
  thumbnailId: '',
  emailAddress: '',
  userName: '',
  givenName: '',
  userState: '',
  externalId: '',
  user_type: '',
  title: null,
  id: 0,
};

const userKeys = keys(emptyUser);

/**
 * Pure (zero-DI) port of the AngularJS `UserModel` service (`users/User.js`).
 * Maps enrolled-user / profile records into the canonical frozen user shape.
 */
export const userModel = {
  mapRoles(roles: any) {
    return map(roles, function (r: any) {
      const name = isObject(r) ? (r as any).name : r;
      return isString(name) ? name.toLowerCase() : name;
    });
  },

  fromProfile(info: any) {
    const rawRoles = info.role ?? info.roles;
    const roles = userModel.mapRoles(rawRoles);

    const user = defaults(
      {
        roles,
        isStudent: roles.indexOf('student') !== -1,
        isInstructor: roles.indexOf('instructor') !== -1,
        inactive: rawRoles && !rawRoles.length, // horrid but we don't always load rôles
      },
      pick(info, userKeys),
      emptyUser
    );

    freeze(user);

    return user;
  },

  //TODO this looks deprecated
  fromRoster(info: any) {
    info.fullName = info.givenName + ' ' + info.familyName;
    info.roles = info.role;
    return userModel.fromProfile(info);
  },
};

export default userModel;
