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

import Announcements from '../announcements';
import BannedIps from '../bannedIps/App';
import Components from '../component';
import Configurations from '../config';
import Connectors from '../connectors/App';
import Domains from '../domains/App';
import Gdpr from '../gdpr/App';
import Jobs from '../jobs';
import Logs from '../logs/App';
import MaintenanceWindows from '../maintenanceWindows/App';
import ProvisionDomain from '../provision';
import SysScript from '../script/App';
import { OverlordRight, SupportRight, UnderlordRight } from '../services/Rights';
import Users from '../users/App';

export default [
  { identifier: 'Provision', icon: 'whatshot', right: UnderlordRight, Page: ProvisionDomain },
  { identifier: 'Domains', icon: 'location_city', right: SupportRight, Page: Domains },
  { identifier: 'Users', icon: 'people', right: OverlordRight, Page: Users },
  { identifier: 'Components', icon: 'extension', right: OverlordRight, Page: Components },
  { identifier: 'Connectors', icon: 'device_hub', right: OverlordRight, Page: Connectors },
  { identifier: 'Jobs', icon: 'event_note', right: OverlordRight, Page: Jobs },
  { identifier: 'Configurations', icon: 'settings', right: OverlordRight, Page: Configurations },
  { identifier: 'Announcements', icon: 'announcement', right: OverlordRight, Page: Announcements },
  { identifier: 'BannedIps', icon: 'not_interested', right: OverlordRight, Page: BannedIps },
  {
    identifier: 'MaintenanceWindows',
    icon: 'desktop_windows',
    right: OverlordRight,
    Page: MaintenanceWindows,
    audio: () => import('../audio/winxp.mp3'),
  }, // the product of a diseased mind
  {
    identifier: 'Trash',
    icon: 'delete_sweep',
    right: OverlordRight,
    embed: '/sys/admin/loi.cp.overlord.TrashAdminPage',
  },
  { identifier: 'Logs', icon: 'toc', right: OverlordRight, Page: Logs },
  { identifier: 'StorageBrowser', href: '/sys/storage', icon: 'sd_storage', right: OverlordRight },
  {
    identifier: 'Script/Scala',
    Page: SysScript,
    icon: 'settings_applications',
    label: 'Sys Script',
    route: '/Script/:language',
    child: { identifier: 'Script/SQL', Page: SysScript, name: 'SQL' },
    right: OverlordRight,
  },
  { identifier: 'LogWatch', href: '/sys/logwatch', icon: 'visibility', right: OverlordRight },
  {
    identifier: 'StartupTasks',
    icon: 'done_all',
    right: OverlordRight,
    embed: '/sys/admin/loi.cp.startup.StartupTaskAdminPage',
  },
  {
    identifier: 'Gdpr',
    icon: 'crisis_alert',
    right: OverlordRight,
    Page: Gdpr,
  },
];
