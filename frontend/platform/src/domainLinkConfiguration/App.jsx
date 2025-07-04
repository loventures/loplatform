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
import { connect } from 'react-redux';
import { bindActionCreators } from 'redux';

import { AdminFormCheck, AdminFormField } from '../components/adminForm';
import ReactTable from '../components/reactTable/ReactTable';
import * as MainActions from '../redux/actions/MainActions';
import DomainLinkHelp from './DomainLinkHelp';

class App extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      loaded: false,
      configs: {},
      help: false,
    };
  }

  componentDidMount() {
    const { url } = this.props;
    axios.get(url).then(res => {
      this.setState({ loaded: true, configs: res.data.objects });
    });
  }

  columns = [
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

  onHelpClick = () => {
    this.setState({ help: true });
    return Promise.resolve(false);
  };

  getButtonInfo = () => {
    return [
      {
        name: 'help',
        iconName: 'help',
        onClick: this.onHelpClick,
        alwaysEnabled: true,
      },
    ];
  };

  renderModal = () => {
    const { configurationSection, translations: T } = this.props;
    if (!this.state.help) return null;
    return (
      <DomainLinkHelp
        close={() => this.setState({ help: false })}
        section={configurationSection}
        T={T}
      />
    );
  };

  renderForm = (row, validationErrors) => {
    const T = this.props.translations;
    const { configurationSection } = this.props;
    return this.columns
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
  };

  validateForm = (form, row) => {
    const editing = Object.keys(row).length;
    const configs = this.state.configs.concat([]);
    const newWindowSetting = form.newWindow === 'on';
    const data = { title: form.title, url: form.url, newWindow: newWindowSetting };
    const missing = this.columns.find(col => col.required && !data[col.dataField]);
    const T = this.props.translations;
    const { configurationSection } = this.props;
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
      configs.push(data);
      this.setState({ configs });
      return { data: { objects: configs, count: configs.length } };
    } else {
      configs[row.id] = data;
      this.setState({ configs });
      return { data: { objects: configs, count: configs.length } };
    }
  };

  createDeleteDTO = id => {
    const configs = this.state.configs.concat([]);
    configs.splice(id, 1);
    this.setState({ configs });
    return { data: { objects: configs, count: configs.length } };
  };

  parseEntity = (entity, id) => {
    return {
      id,
      ...entity,
    };
  };

  render() {
    if (!this.state.loaded) return null;
    const { url, configurationSection } = this.props;
    const httpMethod = 'post';
    const handleDelete = {
      createDeleteDTO: this.createDeleteDTO,
      deleteMethod: httpMethod,
      getDeleteUrl: () => url,
    };
    return (
      <React.Fragment>
        <ReactTable
          entity={configurationSection}
          baseUrl={url}
          paginate={false}
          columns={this.columns}
          defaultSortField="id"
          defaultSearchField="id"
          renderForm={this.renderForm}
          validateForm={this.validateForm}
          setPortalAlertStatus={this.props.setPortalAlertStatus}
          postUrl={url}
          updateUrl={url}
          updateMethod={httpMethod}
          parseEntity={this.parseEntity}
          handleDelete={handleDelete}
          translations={this.props.translations}
          getButtons={this.getButtonInfo}
        />
        {this.renderModal()}
      </React.Fragment>
    );
  }
}

function mapStateToProps(state) {
  return {
    translations: state.main.translations,
  };
}

function mapDispatchToProps(dispatch) {
  return bindActionCreators(MainActions, dispatch);
}

const DomainLinkConfiguration = connect(mapStateToProps, mapDispatchToProps)(App);

DomainLinkConfiguration.propTypes = {
  configurationSection: PropTypes.oneOf(['headerConfiguration', 'footerConfiguration']).isRequired,
  url: PropTypes.string.isRequired,
};

export default DomainLinkConfiguration;
