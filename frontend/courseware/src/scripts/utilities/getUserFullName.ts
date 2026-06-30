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

type NameUser = {
  fullName?: string;
  givenName?: string;
  middleName?: string;
  familyName?: string;
  userName?: string;
};

export const getUserFullName = (user: NameUser) => {
  if (!user) {
    return '';
  } else if (user.fullName) {
    return user.fullName;
  } else if (user.givenName || user.familyName) {
    return `${user.givenName || ''} ${user.middleName || ''} ${user.familyName || ''}`;
  } else {
    return user.userName;
  }
};
