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

import React from 'react';
import { useDispatch } from 'react-redux';

import { AdminFormField } from '../components/adminForm';
import ReactTable from '../components/reactTable/ReactTable';
import { setPortalAlertStatus } from '../redux/actions/MainActions';
import { useTranslations } from '../redux/state';
import { trim } from '../services';

const BannedIps: React.FC = () => {
  const T = useTranslations();
  const dispatch = useDispatch();

  const columns = [
    { dataField: 'id', isKey: true },
    { dataField: 'ip', sortable: false, searchable: false, required: true },
  ];

  const renderForm = (row: Record<string, any>, validationErrors: Record<string, string>) => {
    return (
      <AdminFormField
        key="ip"
        entity="bannedIps"
        field="ip"
        value={row.ip}
        invalid={validationErrors.ip}
        T={T}
      />
    );
  };

  const validateForm = (form: Record<string, any>) => {
    const data = trim(form.ip);
    const missing = data === '';
    const params = { field: T.t(`adminPage.bannedIps.fieldName.ip`) };
    if (missing) {
      return { validationErrors: { ip: T.t('adminForm.validation.fieldIsRequired', params) } };
    }
    return {
      data: JSON.stringify(data),
      headers: { headers: { 'Content-Type': 'application/json' } },
    };
  };

  const parseEntity = (ip: string) => ({ id: ip, ip: ip });

  const createDeleteDTO = (ip: string) => {
    return {
      data: JSON.stringify(ip),
      headers: { 'Content-Type': 'application/json' },
    };
  };

  const handleDelete = {
    createDeleteDTO: createDeleteDTO as (id: any) => { data: any; headers: any },
    deleteMethod: 'post',
    getDeleteUrl: () => '/api/v2/overlord/bannedIps/delete',
  };

  return (
    <ReactTable
      entity="bannedIps"
      columns={columns}
      renderForm={renderForm}
      validateForm={validateForm}
      translations={T}
      setPortalAlertStatus={(error: any, success: boolean, message: string) =>
        dispatch(setPortalAlertStatus(error, success, message))
      }
      parseEntity={parseEntity}
      paginate={false}
      updateButton={false}
      baseUrl="/api/v2/overlord/bannedIps"
      postUrl="/api/v2/overlord/bannedIps"
      handleDelete={handleDelete}
    />
  );
};

export default BannedIps;
