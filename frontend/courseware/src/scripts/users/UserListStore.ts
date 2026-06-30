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

import { isFunction } from 'lodash';

import Course from '../bootstrap/course.ts';
import { loConfig } from '../bootstrap/loConfig.ts';
import { ResourceStore } from '../srs/pure/resourceStore.ts';
import { Roles } from '../utilities/pure/roles.ts';
import { UserContainer } from './userContainer.ts';

/**
 * Pure-TS port of the AngularJS `UserListStore` SRS store (the messaging recipient picker's roster).
 * Prototype-chains the pure `ResourceStore` (kept the old `Object.create` / `.call` pattern the original
 * used). `Roles` and `UserContainer` are now pure singletons/classes (the latter wraps `makeUser`).
 */

export function UserListStore(this: any, courseId?: any, roles?: any) {
  if (Roles.isInstructor()) {
    this.url = loConfig.enrollment.users; // Full user; only available to instructors/admins
  } else {
    this.url = loConfig.cohort.users; // Slimmed-down, FERPA-compliant profile
  }
  this.courseId = courseId || (Course as any).id;
  this.roles = roles || ['student', 'trialLearner'];
  this.data = [];
  (ResourceStore as any).call(this, this.url, { contextId: this.courseId });
}

UserListStore.prototype = Object.create((ResourceStore as any).prototype);
UserListStore.prototype.constructor = UserListStore;

UserListStore.prototype.searchByProps = {
  GIVEN_NAME: 'givenName',
  FAMILY_NAME: 'familyName',
  USER_NAME: 'userName',
  EXTERNAL_ID: 'externalId',
  EMAIL_ADDRESS: 'emailAddress',
};

UserListStore.prototype.sortByProps = {
  GIVEN_NAME_ASC: { property: 'givenName', order: 1 },
  GIVEN_NAME_DESC: { property: 'givenName', order: -1 },
  FAMILY_NAME_ASC: { property: 'familyName', order: 1 },
  FAMILY_NAME_DESC: { property: 'familyName', order: -1 },
};

UserListStore.prototype.load = function (this: any) {
  this.filters.setPrefilters([['role.roleId', 'in', this.roles.join(',')]]);
  return (ResourceStore as any).prototype.load.apply(this);
};

UserListStore.prototype.searchByName = function (this: any, str: any) {
  return this.search(str, this.searchByProps);
};

UserListStore.prototype.deserialize = function (user: any) {
  return new UserContainer(user);
};

UserListStore.prototype.create = function () {
  throw new Error('Not allowed');
};
UserListStore.prototype.update = function () {
  throw new Error('Not allowed');
};
UserListStore.prototype.remove = function () {
  throw new Error('Not allowed');
};

UserListStore.prototype.getName = function (item: any) {
  return item.getName();
};

UserListStore.prototype.rowAction = function (row: any) {
  console.log('click on row', row);
};

UserListStore.prototype.setRowTextFunction = function (this: any, func: any) {
  if (isFunction(func)) {
    console.warn('Overriding normal row text function.');
    this.getRowText = func;
  } else {
    console.error('Failed to override the row text function with ', func);
  }
};

UserListStore.prototype.getRowText = function (row: any) {
  return row.givenName + ' ' + row.familyName;
};
