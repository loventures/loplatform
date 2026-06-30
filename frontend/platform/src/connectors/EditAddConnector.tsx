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
import Polyglot from 'node-polyglot';
import React, { useEffect, useState } from 'react';

import { AdminFormCheck, AdminFormField, AdminFormSecret } from '../components/adminForm';
import ConnectorTypes from './ConnectorTypes';
import {
  ConnectorComponentProps,
  ConnectorConfig,
  ConnectorRow,
  ConnectorTypeEntry,
} from './types';

interface EditAddConnectorProps {
  implementation?: string;
  row: ConnectorRow;
  onSchemaChange: (schema: string, configs: ConnectorConfig[], impl: string) => void;
  T: Polyglot;
  validationErrors: Record<string, string>;
}

const EditAddConnector: React.FC<EditAddConnectorProps> = ({
  implementation,
  row,
  onSchemaChange,
  T,
  validationErrors,
}) => {
  const [configs, setConfigs] = useState<ConnectorConfig[] | null>(null);

  const impl = Object.keys(row).length ? row.implementation : implementation;

  useEffect(() => {
    axios.get('/api/v2/connectors/config/' + impl).then(res => {
      onSchemaChange(res.data.schema, res.data.configs || [], impl);
      setConfigs(res.data.configs || []);
    });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const renderField = (config: ConnectorConfig): React.ReactNode => {
    if (config.type === 'Boolean') {
      return (
        <AdminFormCheck
          key={config.id}
          entity={'connector'}
          field={config.id}
          label={config.name}
          value={row && row[config.id]}
          T={T}
        />
      );
    } else if (config.type === 'Password') {
      return (
        <AdminFormSecret
          key={config.id}
          label={config.name}
          entity={'connector'}
          type="text"
          field={config.id}
          value={row ? row[config.id] : ''}
          T={T}
        />
      );
    } else {
      return (
        <AdminFormField
          key={config.id}
          label={config.name}
          entity={'connector'}
          type={config.type === 'Text' ? 'textarea' : 'text'}
          field={config.id}
          value={row ? row[config.id] : ''}
          T={T}
        />
      );
    }
  };

  const renderConnectorIdName = () => {
    if (!configs) return null;
    return ['systemId', 'name'].map(field => (
      <AdminFormField
        key={field}
        entity="connectors"
        field={field}
        value={row[field]}
        required={true}
        invalid={validationErrors[field]}
        T={T}
      />
    ));
  };

  const renderStatus = () => {
    if (!configs) return null;
    return (
      <AdminFormCheck
        entity="connectors"
        field="status"
        label="Active"
        value={!row.disabled}
        T={T}
      />
    );
  };

  const entry = ConnectorTypes[impl] as ConnectorTypeEntry | undefined;
  const ConnectorComponent =
    entry && typeof entry !== 'function' ? entry.component : undefined;

  return (
    <React.Fragment>
      {row.id && <div className="entity-id">{row.id}</div>}
      {renderConnectorIdName()}
      {configs &&
        (ConnectorComponent ? (
          <ConnectorComponent
            T={T}
            row={row}
            renderField={renderField as ConnectorComponentProps['renderField']}
            configs={configs}
          />
        ) : (
          configs.map(config => renderField(config))
        ))}
      {renderStatus()}
    </React.Fragment>
  );
};

export default EditAddConnector;
