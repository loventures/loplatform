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

import React, { lazy, Suspense } from 'react';

import WaitDotGif from '../components/WaitDotGif';
import { IoSettingsOutline } from 'react-icons/io5';

const LazyApp = lazy(() => import('./config'));

const Configurations = props => (
  <Suspense
    fallback={
      <WaitDotGif
        color="secondary"
        size={64}
      />
    }
  >
    <LazyApp {...props} />
  </Suspense>
);

Configurations.pageInfo = {
  identifier: 'configurations',
  icon: IoSettingsOutline,
  link: '/Configurations',
  group: 'domain',
  right: 'loi.cp.admin.right.ConfigurationAdminRight',
};

export default Configurations;
