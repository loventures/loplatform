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

import { PageInfo } from '../adminPortal/types';
import ReactTable from '../components/reactTable/ReactTable';
import { useLoPlatform, useTranslations } from '../redux/state';
import { SrsCollection } from '../srs';
import ConnectorTypes from './ConnectorTypes';
import EditAddConnector from './EditAddConnector';
import { ConnectorConfig, ConnectorRow, ConnectorTypeEntry, ParsedForm } from './types';
import { humanize } from './util';
import { IoGitNetworkOutline } from 'react-icons/io5';

interface ComponentDescriptor {
  identifier: string;
  schema: string;
}

interface ConnectorTypeInfo {
  name: string;
  id: string;
}

type ConnectorTypesMap = Record<string, ConnectorTypeInfo>;

interface ConnectorsPageInfo extends PageInfo {
  replaces: string;
  entity: string;
}

const Connectors: React.FC & { pageInfo: ConnectorsPageInfo } = () => {
  const T = useTranslations();
  const { isProdLike } = useLoPlatform();

  const [connectorTypes, setConnectorTypes] = useState<ConnectorTypesMap>({});
  const [loaded, setLoaded] = useState(false);
  const [schema, setSchema] = useState<string | null>(null);
  const [implementation, setImplementation] = useState<string | null>(null);
  const [configs, setConfigs] = useState<Record<string, string> | null>(null);
  const [impl, setImpl] = useState<string | null>(null);

  useEffect(() => {
    axios
      .get<SrsCollection<ComponentDescriptor>>(
        '/api/v2/components;filter=interface:eq(loi.cp.integration.SystemComponent)'
      )
      .then(res => {
        const types = res.data.objects.reduce<ConnectorTypesMap>((types, component) => {
          const i18nKey = `adminPage.connectors.schema.${component.schema}`;
          const name = T.has(i18nKey) ? T.t(i18nKey) : humanize(component.identifier);
          types[component.schema] = { name, id: component.identifier };
          return types;
        }, {});
        setLoaded(true);
        setConnectorTypes(types);
      });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const generateDropdownItems = () =>
    Object.keys(connectorTypes)
      .filter(type => {
        const id = connectorTypes[type].id;
        const entry = ConnectorTypes[id] as ConnectorTypeEntry | undefined;
        return !entry || typeof entry === 'function' || !entry.unaddable;
      })
      .map(type => ({
        name: connectorTypes[type].name,
        key: type,
        onClick: () => setImplementation(connectorTypes[type].id),
      }))
      .sort((a, b) => a.name.toLowerCase().localeCompare(b.name.toLowerCase()));

  const generateColumns = (): any[] => {
    const typeFilterOptions = Object.keys(connectorTypes)
      .sort()
      .map(type => (
        <option
          key={connectorTypes[type].id}
          value={connectorTypes[type].id}
        >
          {connectorTypes[type].name}
        </option>
      ));
    const formatType = (c: React.ReactNode, r: ConnectorRow) => {
      const { _type } = r;
      if (_type === 'basicLtiConnector' && !isProdLike) {
        return (
          <span>
            {c} <span className="hover-fade-in">(Basic)</span>
          </span>
        );
      } else {
        return c;
      }
    };
    return [
      { dataField: 'id', isKey: true },
      { dataField: 'status', sortable: false, searchable: false, required: false },
      { dataField: 'systemId', sortable: true, searchable: true, required: true },
      { dataField: 'name', sortable: true, searchable: true, required: true },
      {
        dataField: 'type',
        sortable: false,
        searchable: false,
        required: false,
        filterable: true,
        filterOptions: typeFilterOptions,
        dataFormat: formatType,
        filterProperty: 'implementation',
        baseFilter: 'Any Type',
      },
    ];
  };

  const onSchemaChange = (schema: string, configs: ConnectorConfig[], impl: string) => {
    const configurations = configs.reduce<Record<string, string>>((obj, config) => {
      return {
        ...obj,
        [config.id]: config.type as string,
      };
    }, {});
    setSchema(schema);
    setConfigs(configurations);
    setImpl(impl);
  };

  const renderForm = (row: ConnectorRow, validationErrors: Record<string, string>) => (
    <EditAddConnector
      T={T}
      row={row}
      onSchemaChange={onSchemaChange}
      validationErrors={validationErrors}
      implementation={implementation ?? undefined}
    />
  );

  const validateForm = (form: Record<string, any>) => {
    const parsedForm = Object.keys(form).reduce<ParsedForm>((obj, prop) => {
      const bool = configs?.[prop] === 'Boolean';
      const val = form[prop] === 'on';
      return {
        ...obj,
        [prop]: bool ? val : form[prop],
      };
    }, {});
    const entry = impl ? (ConnectorTypes[impl] as ConnectorTypeEntry | undefined) : undefined;
    const defaultValidator = ConnectorTypes.DefaultFormValidator;
    const validator =
      entry && typeof entry !== 'function'
        ? entry.validateForm
        : typeof defaultValidator === 'function'
          ? defaultValidator
          : undefined;
    const validated = validator && validator(parsedForm);
    const validatedDTO = validated && validated.dto;
    const missing =
      (validatedDTO && validatedDTO.validationErrors) ||
      generateColumns().find(col => col.required && !form[col.dataField]);
    const params = missing && {
      field: T.t(`adminPage.connectors.fieldName.${missing.dataField}`),
    };
    const validatedData = (validatedDTO && validatedDTO.data) || {};
    const restOfParsedForm = (validated && validated.parsedForm) || {};
    const data: Record<string, any> = {
      ...restOfParsedForm,
      ...validatedData,
      disabled: !parsedForm.status,
    };
    delete data.status;
    delete data.type;
    return missing
      ? {
          validationErrors: {
            [missing.dataField]: T.t('adminForm.validation.fieldIsRequired', params),
          },
        }
      : { data };
  };

  const parseConnector = (connector: ConnectorRow) => {
    return {
      ...connector,
      status: connector.disabled
        ? T.t('adminPage.connectors.status.inactive')
        : T.t('adminPage.connectors.status.active'),
      connectorId: connector.systemId,
      type: connectorTypes[connector._type].name,
    };
  };

  if (!loaded) return null;
  return (
    <ReactTable
      entity="connectors"
      columns={generateColumns()}
      defaultSortField="name"
      defaultSearchField="name"
      parseEntity={parseConnector}
      renderForm={renderForm}
      validateForm={validateForm}
      translations={T}
      schema={schema ?? undefined}
      createButton={false}
      createDropdown={true}
      dropdownItems={generateDropdownItems()}
    />
  );
};

Connectors.pageInfo = {
  identifier: 'connectors',
  icon: IoGitNetworkOutline,
  link: '/Connectors',
  group: 'integrations',
  right: 'loi.cp.admin.right.AdminRight',
  replaces: 'loi.cp.integration.IntegrationAdminPage',
  entity: 'connectors',
};

export default Connectors;
