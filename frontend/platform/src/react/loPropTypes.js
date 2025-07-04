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

import PT from 'prop-types';

const resource = PT.shape({
  url: PT.string.isRequired,
});

const domainDto = PT.shape({
  logo: resource, // null on overlörd?
  logo2: resource,
  name: PT.string.isRequired,
});

const userDto = PT.shape({
  imageUrl: PT.string,
  fullName: PT.string.isRequired,
});

const lo_platform = PT.shape({
  adminLink: PT.string,
  authoringLink: PT.string,
  clusterType: PT.string.isRequired,
  domain: domainDto.isRequired,
  enrollments: PT.array.isRequired,
  isProduction: PT.bool.isRequired,
  isProdLike: PT.bool.isRequired,
  isOverlord: PT.bool.isRequired,
  session: PT.shape({
    sudoed: PT.bool.isRequired,
  }),
  user: PT.shape({
    rights: PT.array.isRequired,
  }),
});

const routerMatch = PT.shape({
  params: PT.object,
  path: PT.string.isRequired,
});

const translations = PT.shape({
  has: PT.func.isRequired,
  t: PT.func.isRequired,
});

const translationsOpt = PT.shape({
  t: PT.func,
});

export default {
  domainDto,
  lo_platform,
  routerMatch,
  translations,
  translationsOpt,
  userDto,
};
