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
import { useParams } from 'react-router-dom';

import ReactTable from '../components/reactTable/ReactTable';
import { useTranslations } from '../redux/state';
import AccessCodeInfo, { AccessCode } from './AccessCodeInfo';

type AccessCodesProps = {
  setLastCrumb: (title: string) => void;
};

const AccessCodes: React.FC<AccessCodesProps> = ({ setLastCrumb }) => {
  const T = useTranslations();
  const [accessCodeInfo, setAccessCodeInfo] = useState<AccessCode | null>(null);

  const { batchId = '' } = useParams<{ batchId: string }>();

  useEffect(() => {
    axios.get(`/api/v2/accessCodes/batches/${batchId}`).then(res => {
      setLastCrumb(res.data.name);
    });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const columns = [
    { dataField: 'id', isKey: true },
    {
      dataField: 'accessCode',
      sortable: true,
      searchable: true,
      filterable: false,
      searchOperator: 'eq',
    },
    { dataField: 'redemptionCount', sortable: true, searchable: false, filterable: false },
    { dataField: 'batch_id', sortable: false, searchable: false, filterable: false, hidden: true },
  ];

  const renderModal = () => {
    if (accessCodeInfo) {
      return (
        <AccessCodeInfo
          T={T}
          accessCodeInfo={accessCodeInfo}
          close={() => setAccessCodeInfo(null)}
        />
      );
    } else {
      return null;
    }
  };

  const onViewClick = (row: AccessCode) => {
    setAccessCodeInfo(row);
    return Promise.resolve(false);
  };

  const getButtonInfo = () => {
    return [
      {
        name: 'viewAccessCode',
        iconName: 'visibility',
        onClick: onViewClick,
      },
    ];
  };

  return (
    <React.Fragment>
      <ReactTable
        entity="accessCodes"
        columns={columns}
        defaultSortField="accessCode"
        defaultSearchField="accessCode"
        createButton={false}
        translations={T}
        deleteButton={true}
        updateButton={false}
        embed="redemptions,batch"
        getButtons={getButtonInfo}
        customFilters={[
          {
            property: 'batch_id',
            operator: 'eq',
            value: batchId,
            prefilter: true,
          },
        ]}
      />
      {renderModal()}
    </React.Fragment>
  );
};

export default AccessCodes;
