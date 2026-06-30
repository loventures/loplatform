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

import { each, capitalize, isNil } from 'lodash';

type Features = Record<string, { isEnabled: boolean; type: string } | unknown> | undefined;

/**
 * Role-checking singleton, ported verbatim from the AngularJS `Roles` provider.
 *
 * State (`roles`, `indexPages`) is populated once at bootstrap by `init`, which
 * the thin Angular provider adapter (utilities/Roles.js) calls from the app's
 * config block. Being plain TS, this is now also importable directly from
 * React/TS without going through the Angular injector.
 */
export const Roles = {
  roles: {} as Record<string, boolean>,
  indexPages: {} as Record<string, string>,

  /** Constructor-time role setup; mirrors RolesProvider.init. */
  init(rolesArr: string[], indexPages: Record<string, string>, features?: Features) {
    let name = '';
    each(rolesArr, function (r: string) {
      name = Roles.roleToFeature(r);

      Roles.roles[name] = true;
      if (features) {
        if (isNil((features as any)[name])) {
          (features as any)[name] = { isEnabled: true, type: 'UserRole' };
        } else {
          console.warn('Not overriding feature for Role for: ', name, (features as any)[name]);
        }
      }
    });

    Roles.indexPages = indexPages;

    return Roles;
  },

  roleToFeature(role: string): string {
    const path = role.split('.');
    return path[path.length - 1];
  },

  roleTypeToFeature(roleType: string): string {
    if (roleType === 'Student') {
      return 'isLearner';
    } else if (roleType === 'Faculty') {
      return 'isInstructor';
    } else {
      return 'is' + capitalize(roleType);
    }
  },

  hasRole(roleName: string): boolean {
    return Roles.roles[roleName];
  },

  isStudent(): boolean {
    return Roles.hasRole('LearnCourseRight'); //Note admins can have both
  },

  isReadOnly(): boolean {
    return Roles.hasRole('ReadCourseRight') && !Roles.hasRole('InteractCourseRight');
  },

  isUnderTrialAccess(): boolean {
    //FullContentRight supercedes Trial
    return Roles.hasRole('TrialContentRight') && !Roles.hasRole('FullContentRight');
  },

  isInstructor(): boolean | undefined {
    if (Roles.hasRole('TeachCourseRight')) return true;

    //TODO: Advisors and Instructors have ContentCourseRight right...
    //fix this up and don't break everything via TECH-712
    if (Roles.hasRole('ContentCourseRight') && !Roles.hasRole('LearnCourseRight')) return true;
  },

  //Temp fix till we refactor in TECH-712
  isStrictlyInstructor(): boolean | undefined {
    if (Roles.hasRole('TeachCourseRight')) return true;
    if (Roles.hasRole('EditCourseGradeRight') && !Roles.hasRole('LearnCourseRight')) return true;
  },

  isAdvisor(): boolean | undefined {
    if (Roles.hasRole('ViewCourseGradeRight') && !Roles.hasRole('EditCourseGradeRight')) return true;
  },

  isAdmin(): boolean {
    // Always false today; admin support is unimplemented.
    // TODO: Make this supported — originally `false && Roles.hasRole('CourseRight')`.
    return false;
  },

  /** What is the primary access right for this user?  Legacy support. */
  getPrimaryRole(): string {
    if (Roles.isAdmin()) {
      return 'administrator';
    } else if (Roles.isInstructor()) {
      return 'instructor';
    } else if (Roles.isStudent()) {
      return 'student';
    } else {
      return 'unknown';
    }
  },

  /** Index page for the primary role; used when returning from previews. */
  getPrimaryRoleIndex(): string {
    return Roles.indexPages[Roles.getPrimaryRole()];
  },
};

export type RolesService = typeof Roles;
