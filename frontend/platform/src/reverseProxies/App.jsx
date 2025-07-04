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

import React from 'react';
import { connect } from 'react-redux';
import { bindActionCreators } from 'redux';

import { AdminFormCheck, AdminFormField } from '../components/adminForm';
import ReactTable from '../components/reactTable/ReactTable';
import * as MainActions from '../redux/actions/MainActions';
import { trim } from '../services';
import { TbArrowBearLeft2 } from 'react-icons/tb';

class App extends React.Component {
  constructor(props) {
    super(props);
    this.state = {};
  }

  columns = [
    { dataField: 'id', isKey: true },
    { dataField: 'status', width: '15%' },
    { dataField: 'name', sortable: true, searchable: true, required: true },
    { dataField: 'url', required: true },
    { dataField: 'remoteUrl', required: true },
  ];

  fields = ['name', 'url', 'remoteUrl', 'cookieNames', 'rewriteRules'];

  fieldHelp = {
    url: '/HelpSite',
    remoteUrl: 'http://example.org',
    cookieNames: '_ga, myCookie',
  };

  renderForm = (row, validationErrors) => {
    const T = this.props.translations;
    const entity = 'reverseProxies';
    return (
      <React.Fragment>
        {this.fields.map(field => (
          <AdminFormField
            key={field}
            help={this.fieldHelp[field]}
            entity={entity}
            type={field === 'rewriteRules' ? 'textarea' : 'text'}
            field={field}
            value={row[field]}
            required={field !== 'cookieNames'}
            autoFocus={field === 'name'}
            invalid={validationErrors[field]}
            T={T}
          />
        ))}
        <AdminFormCheck
          entity={entity}
          field="active"
          value={!row.disabled}
          T={T}
        />
      </React.Fragment>
    );
  };

  parseEntity = l => {
    const T = this.props.translations;
    const status = l.disabled
      ? T.t('adminPage.reverseProxies.status.suspended')
      : T.t('adminPage.reverseProxies.status.active');
    const cookieNames = l.cookieNames.join(', ');
    const rewriteRules = JSON.stringify(l.rewriteRules, null, 2);
    return { ...l, status, cookieNames, rewriteRules };
  };

  validateForm = form => {
    const T = this.props.translations;
    const data = this.fields.reduce((o, f) => ({ ...o, [f]: trim(form[f]) }), {
      disabled: !form.active,
    });
    const missing = this.columns.find(col => col.required && !data[col.dataField]);
    if (missing) {
      const params = { field: T.t(`adminPage.reverseProxies.fieldName.${missing.dataField}`) };
      return {
        validationErrors: {
          [missing.dataField]: T.t('adminForm.validation.fieldIsRequired', params),
        },
      };
    } else {
      data.cookieNames = data.cookieNames.split(/ *, */).filter(o => o !== '');
      try {
        data.rewriteRules = JSON.parse(data.rewriteRules || '[]');
      } catch (e) {
        const params = { field: T.t('adminPage.reverseProxies.fieldName.rewriteRules') };
        return {
          validationErrors: { rewriteRules: T.t('adminForm.validation.fieldMustBeValid', params) },
        };
      }
      return { data };
    }
  };

  render() {
    return (
      <ReactTable
        entity="reverseProxies"
        schema="reverseProxy"
        columns={this.columns}
        defaultSortField="name"
        defaultSearchField="name"
        parseEntity={this.parseEntity}
        renderForm={this.renderForm}
        validateForm={this.validateForm}
        translations={this.props.translations}
        setPortalAlertStatus={this.props.setPortalAlertStatus}
      />
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

const ReverseProxies = connect(mapStateToProps, mapDispatchToProps)(App);

ReverseProxies.pageInfo = {
  identifier: 'reverseProxies',
  icon: TbArrowBearLeft2,
  link: '/ReverseProxies',
  group: 'media',
  right: 'loi.cp.reverseproxy.ReverseProxyAdminRight',
  entity: 'reverseProxies',
};

export default ReverseProxies;
