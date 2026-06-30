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

import React, { useEffect, useState } from 'react';

import AdminPageWidget from '../components/adminPageWidget';
import { clearSavedTableState } from '../components/reactTable/ReactTable';
import { useLoPlatform, useTranslations } from '../redux/state';
import getAvailableAdminPages from './pages';
import { PageInfo } from './types';

/**
 * A page descriptor as it appears in the legacy admin-pages object (keyed by
 * group), enriched with the localized name/description. The shape is loose
 * because it merges backend-supplied legacy pages with React `pageInfo`s.
 */
interface AdminPageDescriptor extends Partial<PageInfo> {
  identifier?: string;
  name?: string;
  description?: string;
  iconName?: string;
  enabled?: boolean;
  [key: string]: unknown;
}

type AdminPagesObj = Record<string, Array<AdminPageDescriptor | React.ReactElement<any>>>;

interface PortalProps {
  adminPages: Record<string, AdminPageDescriptor[]>;
}

const Portal: React.FC<PortalProps> = ({ adminPages }) => {
  const T = useTranslations();
  const lo_platform = useLoPlatform();
  const [renderedPages, setRenderedPages] = useState<React.ReactNode[]>([]);
  const [loaded, setLoaded] = useState(false);

  // TODO: undo this evil hack when we have a reactfous overlorde
  if (lo_platform.domain.type === 'overlord') {
    document.location.href = '/';
  }

  const addReactPagesToAdminPages = (
    rightfulReactAdminPages: PageInfo[],
    adminPagesObj: AdminPagesObj
  ) => {
    for (const page of rightfulReactAdminPages) {
      const newPage: AdminPageDescriptor = {
        ...page,
        name: T.t(`adminPage.${page.identifier}.name`),
        description: T.t(`adminPage.${page.identifier}.description`),
      };
      if (adminPagesObj[page.group]) {
        adminPagesObj[page.group].push(newPage);
      } else {
        adminPagesObj[page.group] = [newPage];
      }
    }
  };

  const sortAndAddHeader = (adminPagesObj: AdminPagesObj) => {
    Object.keys(adminPagesObj).forEach(key => {
      (adminPagesObj[key] as AdminPageDescriptor[]).sort((page1, page2) => {
        const str1 = (page1.name ?? '').toUpperCase();
        const str2 = (page2.name ?? '').toUpperCase();
        if (str1 < str2) return -1;
        else if (str1 > str2) return 1;
        else return 0;
      });
      adminPagesObj[key].unshift(
        <h2
          key={`${key}.title`}
          className="group-header"
        >
          {T.t(`adminSection.${key}.name`)}
        </h2>
      );
    });
  };

  const mapToAdminPageWidget = (page: AdminPageDescriptor | React.ReactElement<any>): React.ReactNode => {
    if ((page as AdminPageDescriptor).identifier) {
      const descriptor = page as AdminPageDescriptor;
      const identifier = descriptor.identifier!.split('.').slice(-1)[0];
      return (
        <AdminPageWidget
          key={identifier}
          identifier={identifier}
          icon={descriptor.icon}
          iconName={descriptor.iconName}
          href={descriptor.href}
          link={descriptor.link || `/${identifier}`}
          title={descriptor.name!}
          description={descriptor.description}
          entity={descriptor.entity}
        />
      );
    } else {
      // a non-identifier entry is a pre-rendered element (e.g. the group header)
      return page as React.ReactElement<any>;
    }
  };

  const flattenAdminPagesObj = (adminPagesObj: AdminPagesObj): React.ReactNode[] => {
    return Object.keys(adminPagesObj)
      .sort()
      .reduce<React.ReactNode[]>((array, key) => {
        const pages = adminPagesObj[key].map(mapToAdminPageWidget);
        return array.concat(
          <div
            key={key}
            className="admin-group"
          >
            {pages}
          </div>
        );
      }, []);
  };

  // If a react page "replaces" a legacy page then it only shows up if the legacy page component is disabled.
  // If a react page defines an `enabled` property then it must be true, but this is a minor hack.
  const filterEnabled = (adminPagesObj: AdminPagesObj, page: PageInfo): boolean => {
    const group = adminPagesObj[page.group] as AdminPageDescriptor[] | undefined;
    const enabled =
      !Object.prototype.hasOwnProperty.call(page, 'enabled') ||
      (page as AdminPageDescriptor).enabled;
    return (
      !!enabled &&
      (!group || !group.find(legacy => legacy.identifier === (page as PageInfo).replaces))
    );
  };

  useEffect(() => {
    const adminPagesObj: AdminPagesObj = JSON.parse(JSON.stringify(adminPages));
    const rightfulReactAdminPages = getAvailableAdminPages()
      .map(page => page.pageInfo)
      .filter(page => filterEnabled(adminPagesObj, page));
    addReactPagesToAdminPages(rightfulReactAdminPages, adminPagesObj);
    sortAndAddHeader(adminPagesObj);
    const flattened = flattenAdminPagesObj(adminPagesObj);
    // If we deep link to the admin portal with a query string then clear any saved table state
    // so if we launch in from authoring we don't accidentally remember previous filtering stuff
    if (document.location.search) clearSavedTableState(); // wow, really?
    setRenderedPages(flattened);
    setLoaded(true);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  if (renderedPages.length === 0) {
    return (
      <div className="container-fluid">
        <h1 className="text-align-center">{T.t('adminPortal.noAdminPagesAvailable')}</h1>
      </div>
    );
  }
  return (
    <div
      id="adminPortal"
      className="container-fluid"
    >
      {loaded && (
        <div className="row">
          <div className="col admin-columns">{renderedPages}</div>
        </div>
      )}
    </div>
  );
};

export default Portal;
