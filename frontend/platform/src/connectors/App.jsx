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
import React from 'react';
import { connect } from 'react-redux';
import { bindActionCreators } from 'redux';

import ReactTable from '../components/reactTable/ReactTable';
import * as MainActions from '../redux/actions/MainActions';
import ConnectorTypes from './ConnectorTypes';
import EditAddConnector from './EditAddConnector';
import { humanize } from './util.js';
import { IoGitNetworkOutline } from 'react-icons/io5';

class App extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      connectorTypes: {},
      loaded: false,
      schema: null,
      dropdownOpen: false,
      implementation: null,
      configs: null,
      impl: null,
    };
  }

  toggle = () => this.setState({ dropdownOpen: !this.state.dropdownOpen });

  componentDidMount() {
    const { translations: T } = this.props;
    axios
      .get('/api/v2/components;filter=interface:eq(loi.cp.integration.SystemComponent)')
      .then(res => {
        const connectorTypes = res.data.objects.reduce((types, component) => {
          const i18nKey = `adminPage.connectors.schema.${component.schema}`;
          const name = T.has(i18nKey) ? T.t(i18nKey) : humanize(component.identifier);
          types[component.schema] = { name, id: component.identifier };
          return types;
        }, {});
        this.setState({ loaded: true, connectorTypes: connectorTypes });
      });
  }

  generateDropdownItems = () => {
    const { connectorTypes } = this.state;
    return Object.keys(connectorTypes)
      .filter(type => {
        const id = connectorTypes[type].id;
        return !ConnectorTypes[id] || !ConnectorTypes[id].unaddable;
      })
      .map(type => ({
        name: connectorTypes[type].name,
        key: type,
        onClick: () => this.setState({ implementation: connectorTypes[type].id }),
      }))
      .sort((a, b) => a.name.toLowerCase().localeCompare(b.name.toLowerCase()));
  };

  generateColumns = () => {
    const {
      lo_platform: { isProdLike },
    } = this.props;
    const connectorTypes = this.state.connectorTypes;
    const typeFilterOptions = Object.keys(connectorTypes)
      .sort()
      .map(type => {
        return (
          <option
            key={connectorTypes[type].id}
            value={connectorTypes[type].id}
          >
            {connectorTypes[type].name}
          </option>
        );
      });
    const formatType = (c, r) => {
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

  onSchemaChange = (schema, configs, impl) => {
    const configurations = Object.keys(configs).reduce((obj, config) => {
      return {
        ...obj,
        [configs[config].id]: configs[config].type,
      };
    }, {});
    this.setState({ schema: schema, configs: configurations, impl: impl });
  };

  renderForm = (row, validationErrors) => {
    return (
      <EditAddConnector
        T={this.props.translations}
        row={row}
        onSchemaChange={this.onSchemaChange}
        validationErrors={validationErrors}
        implementation={this.state.implementation}
      />
    );
  };

  validateForm = form => {
    const { configs, impl } = this.state;
    const { translations: T } = this.props;
    const parsedForm = Object.keys(form).reduce((obj, prop) => {
      const bool = configs[prop] === 'Boolean';
      const val = form[prop] === 'on';
      return {
        ...obj,
        [prop]: bool ? val : form[prop],
      };
    }, {});
    const validator = ConnectorTypes[impl]
      ? ConnectorTypes[impl].validateForm
      : ConnectorTypes.DefaultFormValidator;
    const validated = validator && validator(parsedForm);
    const validatedDTO = validated && validated.dto;
    const missing =
      (validatedDTO && validatedDTO.validationErrors) ||
      this.generateColumns().find(col => col.required && !form[col.dataField]);
    const params = missing && { field: T.t(`adminPage.connectors.fieldName.${missing.dataField}`) };
    const validatedData = (validatedDTO && validatedDTO.data) || {};
    const restOfParsedForm = (validated && validated.parsedForm) || {};
    const data = {
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

  parseConnector = connector => {
    const T = this.props.translations;
    return {
      ...connector,
      status: connector.disabled
        ? T.t('adminPage.connectors.status.inactive')
        : T.t('adminPage.connectors.status.active'),
      connectorId: connector.systemId,
      type: this.state.connectorTypes[connector._type].name,
    };
  };

  render() {
    if (!this.state.loaded) return null;
    return (
      <ReactTable
        entity="connectors"
        columns={this.generateColumns()}
        defaultSortField="name"
        defaultSearchField="name"
        parseEntity={this.parseConnector}
        renderForm={this.renderForm}
        validateForm={this.validateForm}
        translations={this.props.translations}
        schema={this.state.schema}
        setPortalAlertStatus={this.props.setPortalAlertStatus}
        createButton={false}
        createDropdown={true}
        dropdownItems={this.generateDropdownItems()}
      />
    );
  }
}

function mapStateToProps(state) {
  return {
    lo_platform: state.main.lo_platform,
    translations: state.main.translations,
  };
}

function mapDispatchToProps(dispatch) {
  return bindActionCreators(MainActions, dispatch);
}

const Connectors = connect(mapStateToProps, mapDispatchToProps)(App);

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
