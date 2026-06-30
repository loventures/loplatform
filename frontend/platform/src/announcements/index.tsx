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

import React, { lazy, Suspense } from 'react';

import { PageInfo } from '../adminPortal/types';
import WaitDotGif from '../components/WaitDotGif';
import { IoChatboxOutline } from 'react-icons/io5';

const LazyApp = lazy(() => import('./Announcements'));

interface AnnouncementsPageInfo extends PageInfo {
  entity: string;
}

const Announcements: React.FC<Record<string, unknown>> & { pageInfo: AnnouncementsPageInfo } = (
  props
) => (
  <Suspense
    fallback={
      <WaitDotGif
        color="secondary"
        size={64}
      />
    }
  >
    <LazyApp {...(props as any)} />
  </Suspense>
);

Announcements.pageInfo = {
  identifier: 'announcements',
  icon: IoChatboxOutline,
  link: '/Announcements',
  group: 'domain',
  right: 'loi.cp.announcement.AnnouncementAdminRight',
  entity: 'announcements',
};

export default Announcements;
