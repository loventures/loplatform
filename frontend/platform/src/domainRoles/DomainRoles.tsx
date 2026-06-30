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

import axios from 'axios';
import React, { useEffect, useState } from 'react';
import { useDispatch } from 'react-redux';

import { PageInfo } from '../adminPortal/types';
import ReactTable from '../components/reactTable/ReactTable';
import { setPortalAlertStatus } from '../redux/actions/MainActions';
import { useTranslations } from '../redux/state';
import { SrsCollection } from '../srs';
import EditAddDomainRole from './EditAddDomainRoles';
import RightsInfo from './RightsInfo';
import { IoPeopleOutline } from 'react-icons/io5';

interface RightInfo {
  name: string;
  description: string;
}

interface DomainRight {
  identifier: string;
  name: string;
  description: string;
}

type RightsMap = Record<string, RightInfo>;

interface RoleDto {
  id: number;
  roleType: { name: string; roleId: string };
  rightIds: string[];
}

/** A role parsed for table display. */
export interface Role extends RoleDto {
  name: string;
  roleId: string;
  rights: string;
}

interface Column {
  dataField: string;
  isKey?: boolean;
  sortable?: boolean;
  searchable?: boolean;
  required?: boolean;
  width?: string;
  [key: string]: unknown;
}

interface DomainRolesPageInfo extends PageInfo {
  entity: string;
}

const DomainRoles: React.FC & { pageInfo: DomainRolesPageInfo } = () => {
  const T = useTranslations();
  const dispatch = useDispatch();
  const [loaded, setLoaded] = useState(false);
  const [rightsMap, setRightsMap] = useState<RightsMap>({});
  const [rightsInfo, setRightsInfo] = useState<Role | null>(null);

  useEffect(() => {
    axios.get<SrsCollection<DomainRight>>('/api/v2/domain/rights').then(res => {
      const map = res.data.objects.reduce<RightsMap>((obj, right) => {
        return {
          ...obj,
          [right.identifier]: {
            name: right.name,
            description: right.description,
          },
        };
      }, {});
      setLoaded(true);
      setRightsMap(map);
    });
     
  }, []);

  const onRightsInfoClick = (row: Role) => {
    setRightsInfo(row);
    return Promise.resolve(false);
  };

  const getButtonInfo = () => {
    return [
      {
        name: 'viewRights',
        iconName: 'info',
        onClick: onRightsInfoClick,
      },
    ];
  };

  const renderModal = () => {
    if (!rightsInfo) return null;
    return (
      <RightsInfo
        row={rightsInfo}
        T={T}
        close={() => setRightsInfo(null)}
      />
    );
  };

  const columns: Column[] = [
    { dataField: 'id', isKey: true },
    { dataField: 'roleId', sortable: false, searchable: false, required: true, width: '20%' },
    { dataField: 'name', sortable: false, searchable: false, required: true, width: '20%' },
    { dataField: 'rights', sortable: false, searchable: false, required: false, width: '60%' },
  ];

  const parseRole = (role: RoleDto): Role => {
    return {
      ...role,
      name: role.roleType.name,
      roleId: role.roleType.roleId,
      rights: role.rightIds.length ? role.rightIds.map(id => rightsMap[id].name).join(', ') : '',
    };
  };

  const renderForm = (row: Record<string, any>, validationErrors: Record<string, string>) => {
    const isEditing = Object.keys(row).length > 0;
    return (
      <EditAddDomainRole
        T={T}
        columns={columns}
        row={row}
        validationErrors={validationErrors}
        editing={isEditing}
      />
    );
  };

  const validateForm = (form: Record<string, any>) => {
    if (form.addingSupported) {
      if (form.supportedRole) {
        const data = {
          addingSupported: true,
          supportedRole: form.supportedRole,
          name: '',
          roleId: '',
        };
        return { data };
      } else {
        const param = { field: T.t(`adminPage.roles.fieldName.supportedRoleId`) };
        return {
          validationErrors: { supportedRoleId: T.t('adminForm.validation.fieldIsRequired', param) },
        };
      }
    } else {
      const data: Record<string, any> = {
        addingSupported: false,
        roleId: form.roleId,
        name: form.name,
      };
      const missing = columns.find(col => col.required && !data[col.dataField]);
      const params = missing && { field: T.t(`adminPage.roles.fieldName.${missing.dataField}`) };
      return missing
        ? {
            validationErrors: {
              [missing.dataField]: T.t('adminForm.validation.fieldIsRequired', params),
            },
          }
        : { data };
    }
  };

  if (!loaded) return null;
  return (
    <React.Fragment>
      <ReactTable
        entity="roles"
        paginate={false}
        columns={columns}
        defaultSortField="roleId"
        defaultSearchField="roleId"
        renderForm={renderForm}
        translations={T}
        filter={false}
        setPortalAlertStatus={(error: any, success: boolean, message: string) =>
          dispatch(setPortalAlertStatus(error, success, message))
        }
        validateForm={validateForm}
        parseEntity={parseRole}
        getButtons={getButtonInfo}
      />
      {renderModal()}
    </React.Fragment>
  );
};

DomainRoles.pageInfo = {
  identifier: 'roles',
  icon: IoPeopleOutline,
  link: '/DomainRoles',
  group: 'users',
  right: 'loi.cp.admin.right.RoleAdminRight',
  entity: 'roles',
};

export default DomainRoles;
