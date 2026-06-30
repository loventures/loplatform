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

import { AiOutlineCode } from 'react-icons/ai';
import { BsDatabase, BsDatabaseGear } from 'react-icons/bs';
import { IoFileTrayFullOutline } from 'react-icons/io5';

import AccessCodes from '../accessCodes/AccessCodesPage';
import AccountRequests from '../accountRequests/AccountRequests';
import Announcements from '../announcements';
import AnalyticBuses from '../buses/AnalyticBuses';
import MessageBuses from '../buses/MessageBuses';
import Components from '../component';
import Config from '../config';
import Connectors from '../connectors/Connectors';
import CourseOfferings from '../courseOfferings';
import { FooterConfigurations, HeaderConfigurations } from '../domainLinkConfiguration';
import DomainRoles from '../domainRoles/DomainRoles';
import DomainSettings from '../domainSettings/DomainSettings';
import CourseSections from '../groups/courseSections/CourseSectionsPage';
import TestSections from '../groups/testSections/TestSectionsPage';
import Imports from '../imports/Imports';
import Jobs from '../jobs';
import Languages from '../languages/Languages';
import LtiTools from '../ltiTools/LtiTools';
import Networks from '../networks/Networks';
import Redirects from '../redirects/Redirects';
import RestrictedLearners from '../restrictedLearners/RestrictedLearners';
import RestrictedSites from '../restrictedSites/RestrictedSites';
import ReverseProxies from '../reverseProxies/ReverseProxies';
import Rights from '../rights/Rights';
import { OverlordRight } from '../services/Rights';
import { store } from '../store';
import Subtenants from '../subtenants/Subtenants';
import Tutorials from '../tutorials/Tutorials';
import Users from '../users/UsersPage';
import ZipSites from '../zipSites/ZipSites';
import { AdminPage } from './types';

/* Each page imported here should have a property `pageInfo` defined as:
 *
 * FooApp.pageInfo = {
 *  identifier: '...',
 *
 *  iconName: 'material_design_icon_name',
 *  ... OR ...
 *  icon: 'image url suitable for <img src=... />'
 *
 *  link : '/AdminPagePathFragment',
 *
 *  group: 'courses|domain|integration|media|reporting|users',
 *
 *  right: "loi.cp.admin.right.SomeAdminRight",
 * };
 *
 */

const SysScriptScala: AdminPage = {
  pageInfo: {
    identifier: 'sysScriptScala',
    href: '/sys/script/scala',
    icon: AiOutlineCode,
    group: 'overlord',
    right: OverlordRight,
  },
};

const SysScriptSQL: AdminPage = {
  pageInfo: {
    identifier: 'sysScriptSQL',
    href: '/sys/script/sql',
    icon: BsDatabase,
    group: 'overlord',
    right: OverlordRight,
  },
};

const SysScriptRedshift: AdminPage = {
  pageInfo: {
    identifier: 'sysScriptRedshift',
    href: '/sys/script/redshift',
    icon: BsDatabaseGear,
    group: 'overlord',
    right: OverlordRight,
  },
};

const StorageBrowser: AdminPage = {
  pageInfo: {
    identifier: 'storageBrowser',
    href: '/sys/storage',
    icon: IoFileTrayFullOutline,
    group: 'overlord',
    right: OverlordRight,
  },
};

const pages: AdminPage[] = [
  AccessCodes,
  AccountRequests,
  Announcements,
  AnalyticBuses,
  Components,
  Config,
  Connectors,
  CourseOfferings,
  CourseSections,
  DomainRoles,
  DomainSettings,
  FooterConfigurations,
  HeaderConfigurations,
  Imports,
  Jobs,
  Languages,
  LtiTools,
  MessageBuses,
  Networks,
  Redirects,
  ReverseProxies,
  RestrictedLearners,
  RestrictedSites,
  Rights,
  StorageBrowser,
  Subtenants,
  SysScriptSQL,
  SysScriptScala,
  SysScriptRedshift,
  TestSections,
  Tutorials,
  // UsersPage attaches `pageInfo` via an `as any` cast, so its inferred type
  // does not structurally carry `AdminPage`; assert it here.
  Users as unknown as AdminPage,
  ZipSites,
];

export default function getAvailableAdminPages(): AdminPage[] {
  const lop = store.getState().main.lo_platform;
  const rights: string[] = lop.user.rights ?? [];

  return pages.filter(
    ({ pageInfo: { right, enforce } }) => rights.includes(right) && (!enforce || enforce(lop))
  );
}
