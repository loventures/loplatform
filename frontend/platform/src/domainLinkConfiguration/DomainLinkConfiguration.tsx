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

import { AdminFormCheck, AdminFormField } from '../components/adminForm';
import ReactTable from '../components/reactTable/ReactTable';
import { useTranslations } from '../redux/state';
import { SrsCollection } from '../srs';
import DomainLinkHelp from './DomainLinkHelp';

interface DomainLinkConfigurationProps {
  configurationSection: string;
  url: string;
}

interface LinkConfig {
  id?: number;
  title?: string;
  url?: string;
  newWindow?: boolean;
  [key: string]: any;
}

const columns: any[] = [
  { dataField: 'id', isKey: true },
  { dataField: 'title', sortable: false, searchable: false, required: true },
  { dataField: 'url', sortable: false, searchable: false, required: true },
  {
    dataField: 'newWindow',
    sortable: false,
    searchable: false,
    required: false,
    type: 'checkbox',
    defaultValue: true,
  },
];

const DomainLinkConfiguration: React.FC<DomainLinkConfigurationProps> = ({
  configurationSection,
  url,
}) => {
  const T = useTranslations();
  const [loaded, setLoaded] = useState(false);
  const [configs, setConfigs] = useState<LinkConfig[]>([]);
  const [help, setHelp] = useState(false);

  useEffect(() => {
    axios.get<SrsCollection<LinkConfig>>(url).then(res => {
      setConfigs(res.data.objects);
      setLoaded(true);
    });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const onHelpClick = () => {
    setHelp(true);
    return Promise.resolve(false);
  };

  const getButtonInfo = () => [
    {
      name: 'help',
      iconName: 'help',
      onClick: onHelpClick,
      alwaysEnabled: true,
    },
  ];

  const renderModal = () => {
    if (!help) return null;
    return (
      <DomainLinkHelp
        close={() => setHelp(false)}
        section={configurationSection}
        T={T}
      />
    );
  };

  const renderForm = (row: LinkConfig, validationErrors: Record<string, string>) =>
    columns
      .filter(x => !x.isKey)
      .map(col => {
        const field = col.dataField;
        if (col.type === 'checkbox') {
          return (
            <AdminFormCheck
              field={field}
              value={row[field] ? row[field] : col.defaultValue}
              invalid={validationErrors[field]}
              entity={configurationSection}
              T={T}
              key={field}
            />
          );
        } else {
          return (
            <AdminFormField
              key={field}
              entity={configurationSection}
              field={field}
              value={row[field]}
              required={col.required}
              autoFocus={field === 'title'}
              invalid={validationErrors[field]}
              T={T}
              type={col.type}
              defaultValue={col.defaultValue}
            />
          );
        }
      });

  const validateForm = (form: Record<string, any>, row: LinkConfig) => {
    const editing = Object.keys(row).length;
    const newConfigs = configs.concat([]);
    const newWindowSetting = form.newWindow === 'on';
    const data: Record<string, any> = {
      title: form.title,
      url: form.url,
      newWindow: newWindowSetting,
    };
    const missing = columns.find(col => col.required && !data[col.dataField]);
    const params = missing && {
      field: T.t(`adminPage.${configurationSection}.fieldName.${missing.dataField}`),
    };
    if (missing) {
      return {
        validationErrors: {
          [missing.dataField]: T.t('adminForm.validation.fieldIsRequired', params),
        },
      };
    } else if (!editing) {
      newConfigs.push(data);
      setConfigs(newConfigs);
      return { data: { objects: newConfigs, count: newConfigs.length } };
    } else {
      newConfigs[row.id!] = data;
      setConfigs(newConfigs);
      return { data: { objects: newConfigs, count: newConfigs.length } };
    }
  };

  const createDeleteDTO = (id: number) => {
    const newConfigs = configs.concat([]);
    newConfigs.splice(id, 1);
    setConfigs(newConfigs);
    return { data: { objects: newConfigs, count: newConfigs.length } };
  };

  const parseEntity = (entity: LinkConfig, id?: number) => ({
    id,
    ...entity,
  });

  if (!loaded) return null;
  const httpMethod = 'post';
  const handleDelete = {
    createDeleteDTO,
    deleteMethod: httpMethod,
    getDeleteUrl: () => url,
  };
  return (
    <React.Fragment>
      <ReactTable
        entity={configurationSection}
        baseUrl={url}
        paginate={false}
        columns={columns}
        defaultSortField="id"
        defaultSearchField="id"
        renderForm={renderForm}
        validateForm={validateForm}
        postUrl={url}
        updateUrl={url}
        updateMethod={httpMethod}
        parseEntity={parseEntity}
        handleDelete={handleDelete as any}
        translations={T}
        getButtons={getButtonInfo}
      />
      {renderModal()}
    </React.Fragment>
  );
};

export default DomainLinkConfiguration;
