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

import React from 'react';

import App from './App';
import { RxPinBottom, RxPinTop } from 'react-icons/rx';

const capitalize = s => s.charAt(0).toUpperCase() + s.slice(1);

const mkPage = (section, url, icon) => {
  const page = props => (
    <App
      {...props}
      configurationSection={section}
      url={url}
    />
  );
  page.pageInfo = {
    identifier: section,
    icon: icon,
    link: '/' + capitalize(section),
    group: 'domain',
    right: 'loi.cp.admin.right.ExternalLinkAdminRight',
    entity: section,
  };
  return page;
};

export const HeaderConfigurations = mkPage(
  'headerConfigurations',
  '/api/v2/siteconfiguration/header',
  RxPinTop
);
export const FooterConfigurations = mkPage(
  'footerConfigurations',
  '/api/v2/siteconfiguration/footer',
  RxPinBottom
);
