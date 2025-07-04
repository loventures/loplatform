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
import { IoExtensionPuzzleOutline } from 'react-icons/io5';

// codemirror is large and slow to fetch

const LazyApp = lazy(() => import('./App'));

const Components = () => (
  <Suspense
    fallback={
      <WaitDotGif
        color="secondary"
        size={64}
      />
    }
  >
    <LazyApp />
  </Suspense>
);

Components.pageInfo = {
  identifier: 'components',
  icon: IoExtensionPuzzleOutline,
  link: '/Components',
  group: 'domain',
  right: 'loi.cp.admin.right.ComponentAdminRight',
};

export default Components;
