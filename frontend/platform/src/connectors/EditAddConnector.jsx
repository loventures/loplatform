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

import axios from 'axios';
import PropTypes from 'prop-types';
import React from 'react';

import { AdminFormCheck, AdminFormField, AdminFormSecret } from '../components/adminForm';
import ConnectorTypes from './ConnectorTypes';

class EditAddConnector extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      configs: null,
    };
  }

  componentDidMount() {
    const { row, implementation } = this.props;
    const impl = Object.keys(row).length ? row.implementation : implementation;
    axios.get('/api/v2/connectors/config/' + impl).then(res => {
      this.props.onSchemaChange(res.data.schema, res.data.configs || [], impl);
      this.setState({ configs: res.data.configs || [] });
    });
  }

  renderConnectorIdName = () => {
    if (!this.state.configs) return null;
    const { validationErrors, row, T } = this.props;
    return ['systemId', 'name'].map(field => {
      return (
        <AdminFormField
          key={field}
          entity="connectors"
          field={field}
          value={row[field]}
          required={true}
          invalid={validationErrors[field]}
          T={T}
        />
      );
    });
  };

  renderStatus = () => {
    if (!this.state.configs) return null;
    const { row, T } = this.props;
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

  renderField = config => {
    const { row, T } = this.props;
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

  render() {
    const { row, implementation, T } = this.props;
    const { configs } = this.state;
    const impl = Object.keys(row).length ? row.implementation : implementation;
    const ConnectorComponent = ConnectorTypes[impl] && ConnectorTypes[impl].component;
    return (
      <React.Fragment>
        {row.id && <div className="entity-id">{row.id}</div>}
        {this.renderConnectorIdName()}
        {configs &&
          (ConnectorComponent ? (
            <ConnectorComponent
              T={T}
              row={row}
              renderField={this.renderField}
              configs={configs}
            />
          ) : (
            configs.map(config => this.renderField(config))
          ))}
        {this.renderStatus()}
      </React.Fragment>
    );
  }
}

EditAddConnector.propTypes = {
  implementation: PropTypes.string,
  row: PropTypes.object,
  onSchemaChange: PropTypes.func,
  T: PropTypes.object,
  validationErrors: PropTypes.object,
};

export default EditAddConnector;
