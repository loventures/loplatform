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

import { intersection, each, keys, map, defaults, pick } from 'lodash';

/*
 Moving the freeze stuff here because soon we won't need it. soon.
*/
// Some fancy ES5 stuff to prevent us from modifying user objects
// after they've been insantiated.
const freeze = function (obj, select, unfreeze = false) {
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

export default angular.module('lo.users.User', []).service('UserModel', function () {
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

  var userKeys = keys(emptyUser);

  const service = {};

  service.mapRoles = roles =>
    map(roles, function (r) {
      const name = angular.isObject(r) ? r.name : r;
      return angular.isString(name) ? name.toLowerCase() : name;
    });

  service.fromProfile = function (info) {
    const rawRoles = info.role ?? info.roles;
    const roles = service.mapRoles(rawRoles);

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
  };

  //TODO this looks deprecated
  service.fromRoster = function (info) {
    info.fullName = info.givenName + ' ' + info.familyName;
    info.roles = info.role;
    return service.fromProfile(info);
  };

  return service;
});
