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
import React, { useState } from 'react';

import { AdminFormField, AdminFormSelect } from '../components/adminForm';
import ReactTable from '../components/reactTable/ReactTable';
import { AdminPage } from '../adminPortal/types';
import { useTranslations } from '../redux/state';
import { trim } from '../services';
import { IoPeopleCircleOutline } from 'react-icons/io5';

// TODO: flattening ought not be a thing, instead we should be able to render the peerNetwork property

const Networks: React.FC & AdminPage = () => {
  const T = useTranslations();
  const [peerNetworks, setPeerNetworks] = useState<any[]>([]);

  const columns = [
    { dataField: 'id', isKey: true },
    { dataField: 'networkId', sortable: true, searchable: true, required: true },
    { dataField: 'name', sortable: true, searchable: true, required: true },
    { dataField: 'connectionModel', sortable: true, searchable: true, required: true },
    { dataField: 'peerNetworkName', sortable: false, searchable: false },
  ];

  const renderForm = (row: any, validationErrors: any) => {
    return columns
      .filter(x => !x.isKey)
      .map(col => {
        const field = col.dataField;
        if (field === 'connectionModel') {
          const models = [
            { id: '', name: '' },
            { id: 'System', name: T.t('adminPage.networks.connectionModel.system') },
            { id: 'User', name: T.t('adminPage.networks.connectionModel.user') },
            { id: 'Request', name: T.t('adminPage.networks.connectionModel.request') },
          ];
          return (
            <AdminFormSelect
              key={field}
              entity="networks"
              field={field}
              invalid={validationErrors[field]}
              value={row[field]}
              options={models}
              required={col.required}
              T={T}
            />
          );
        } else if (field === 'peerNetworkName') {
          const peerable = !row.peerNetwork_id; // currently peering is one-shot
          const networks = [{ id: '', name: '' }, ...peerNetworks];
          return (
            peerable && (
              <AdminFormSelect
                key={field}
                entity="networks"
                field={field}
                invalid={validationErrors[field]}
                value={row.peerNetwork_id}
                options={networks}
                required={col.required}
                T={T}
              />
            )
          );
        } else {
          return (
            <AdminFormField
              key={field}
              entity="networks"
              field={field}
              invalid={validationErrors[field]}
              value={row[field]}
              required={col.required}
              autoFocus={field === 'networkId'}
              T={T}
            />
          );
        }
      });
  };

  const validateForm = (form: any) => {
    const data: Record<string, any> = {
      networkId: trim(form.networkId),
      name: trim(form.name),
      connectionModel: trim(form.connectionModel),
    };
    const extras = { peerNetwork: parseInt(form.peerNetwork, 10) || null };
    const missing = columns.find(col => col.required && data[col.dataField] === '');
    const params = missing && { field: T.t(`adminPage.networks.fieldName.${missing.dataField}`) };
    return missing
      ? {
          validationErrors: {
            [missing.dataField]: T.t('adminForm.validation.fieldIsRequired', params),
          },
        }
      : { data, extras };
  };

  const flattenNetwork = (network: any) => {
    return { ...network, peerNetworkName: (network.peerNetwork && network.peerNetwork.name) || '' };
  };

  const setPeerNetwork = (response: any, extras: any) => {
    if (extras && extras.peerNetwork) {
      const data = { network: parseInt(extras.peerNetwork, 10) };
      return axios.post('/api/v2/networks/' + response.data.id + '/peer', data);
    } else {
      return response;
    }
  };

  const loadPeerNetworks = (row: any) => {
    if (!row.peerNetwork_id) {
      setPeerNetworks([]);
      axios
        .get('/api/v2/networks;filter=peerNetwork_id:eq(null)')
        .then(response => {
          setPeerNetworks(response.data.objects);
        })
        .catch(error => {
          // hmm
          console.log(error);
        });
    }
  };

  return (
    <ReactTable
      entity="networks"
      embed="peerNetwork"
      parseEntity={flattenNetwork}
      columns={columns}
      defaultSortField="networkId"
      defaultSearchField="networkId"
      renderForm={renderForm}
      beforeCreateOrUpdate={loadPeerNetworks}
      afterCreateOrUpdate={setPeerNetwork}
      validateForm={validateForm}
      translations={T}
    />
  );
};

Networks.pageInfo = {
  identifier: 'networks',
  icon: IoPeopleCircleOutline,
  link: '/SocialNetworks',
  group: 'users',
  right: 'loi.cp.admin.right.AdminRight',
  entity: 'networks',
};

export default Networks;
