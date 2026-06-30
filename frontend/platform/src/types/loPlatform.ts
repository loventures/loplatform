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

/**
 * Shapes of the `lo_platform` bootstrap object (window.lo_platform / GET
 * /api/v2/lo_platform). These mirror the Jackson-serialized backend components;
 * fields are added as conversions surface a need for them.
 */

export interface Resource {
  url: string;
}

export interface DomainDto {
  id?: number;
  name: string;
  shortName?: string;
  hostName?: string;
  logo?: Resource | null;
  logo2?: Resource | null;
  favicon?: Resource | null;
  css?: Resource | null;
  image?: Resource | null;
  primaryColor?: string;
  secondaryColor?: string;
  accentColor?: string;
  locale?: string;
  timeZone?: string;
  [key: string]: unknown;
}

export interface UserSession {
  sudoed: boolean;
  [key: string]: unknown;
}

/** A logged-in or administered user (loi.cp.user.UserComponent). */
export interface User {
  id: number;
  user_type?: string;
  userName?: string;
  givenName?: string;
  middleName?: string;
  familyName?: string;
  fullName: string;
  emailAddress?: string;
  externalId?: string;
  imageUrl?: string;
  url?: string;
  thumbnailId?: number | null;
  userState?: string;
  state?: { disabled?: boolean } | null;
  disabled?: boolean;
  subtenant?: number | null;
  rights?: string[];
  roles?: string[];
  [key: string]: unknown;
}

export interface Enrollment {
  id: number;
  role?: string;
  context_id?: number;
  [key: string]: unknown;
}

export interface LoPlatform {
  adminLink?: string;
  authoringLink?: string;
  clusterType: string;
  domain: DomainDto;
  enrollments: Enrollment[];
  isProduction: boolean;
  isProdLike: boolean;
  isOverlord: boolean;
  user: User;
  session?: UserSession;
  i18n?: Record<string, string>;
  [key: string]: unknown;
}
