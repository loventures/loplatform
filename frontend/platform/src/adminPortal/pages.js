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

import AccessCodes from '../accessCodes/App';
import AccountRequests from '../accountRequests/App';
import Announcements from '../announcements';
import AnalyticBuses from '../buses/AnalyticBuses';
import MessageBuses from '../buses/MessageBuses';
import Components from '../component';
import Config from '../config';
import Connectors from '../connectors/App';
import CourseOfferings from '../courseOfferings';
import { FooterConfigurations, HeaderConfigurations } from '../domainLinkConfiguration';
import DomainRoles from '../domainRoles/App';
import DomainSettings from '../domainSettings/App';
import CourseSections from '../groups/courseSections/App';
import TestSections from '../groups/testSections/App';
import Imports from '../imports/App';
import Jobs from '../jobs';
import Languages from '../languages/App';
import LtiTools from '../ltiTools/App';
import Networks from '../networks/App';
import Redirects from '../redirects/App';
import RestrictedLearners from '../restrictedLearners/RestrictedLearners';
import ReverseProxies from '../reverseProxies/App';
import Rights from '../rights/App';
import { OverlordRight } from '../services/Rights';
import { store } from '../store';
import Subtenants from '../subtenants/App';
import Tutorials from '../tutorials/Tutorials';
import Users from '../users/App';
import ZipSites from '../zipSites/App';
import RestrictedSites from '../restrictedSites/RestrictedSites';
import { LuDatabase } from 'react-icons/lu';
import { BsDatabase, BsDatabaseGear } from 'react-icons/bs';
import { AiOutlineCode } from 'react-icons/ai';
import { IoFileTrayFullOutline } from 'react-icons/io5';

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

const SysScriptScala = {
  pageInfo: {
    identifier: 'sysScriptScala',
    href: '/sys/script/scala',
    icon: AiOutlineCode,
    group: 'overlord',
    right: OverlordRight,
  },
};

const SysScriptSQL = {
  pageInfo: {
    identifier: 'sysScriptSQL',
    href: '/sys/script/sql',
    icon: BsDatabase,
    group: 'overlord',
    right: OverlordRight,
  },
};

const SysScriptRedshift = {
  pageInfo: {
    identifier: 'sysScriptRedshift',
    href: '/sys/script/redshift',
    icon: BsDatabaseGear,
    group: 'overlord',
    right: OverlordRight,
  },
};

const StorageBrowser = {
  pageInfo: {
    identifier: 'storageBrowser',
    href: '/sys/storage',
    icon: IoFileTrayFullOutline,
    group: 'overlord',
    right: OverlordRight,
  },
};

const pages = [
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
  Users,
  ZipSites,
];

export default function getAvailableAdminPages() {
  const lop = store.getState().main.lo_platform;
  const rights = lop.user.rights;

  return pages.filter(
    ({ pageInfo: { right, enforce } }) => rights.includes(right) && (!enforce || enforce(lop))
  );
}
