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

import { currentUser as currentUserData } from './currentUserData.ts';

/** The fields/methods callers use off the AngularJS `User` (current-user) service. */
export interface CurrentUser {
  id: number;
  handle: string;
  fullName: string;
  getId(): number;
  getHandle(): string;
  getName(): string;
  getRoles(): string[];
  isStudent(): boolean;
  isInstructor(): boolean;
  isStrictlyInstructor(): boolean;
  isPreview(): boolean;
  recordActivity(): boolean;
}

/**
 * Typed accessor for the *current* user. Returns the pure-TS `currentUser` singleton
 * (utilities/currentUserData.ts), which mirrors how the AngularJS `UserProvider` builds
 * its current user at bootstrap (`window.lo_platform.user` + the primary `Roles`). This
 * replaces the former `lojector.get('User')` reach-in.
 *
 * The current user is static (the instructor preview / "view-as" mutation flow is dead;
 * React view-as is param-based over the `actualUser` redux slice), so the value is safe to
 * read once. The accessor signature is retained so existing callers are unchanged.
 */
export const currentUser = (): CurrentUser => currentUserData;

export default currentUser;
