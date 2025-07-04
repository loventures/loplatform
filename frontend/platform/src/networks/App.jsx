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

import { AdminFormField, AdminFormSelect } from '../components/adminForm';
import ReactTable from '../components/reactTable/ReactTable';
import LoPropTypes from '../react/loPropTypes';
import * as MainActions from '../redux/actions/MainActions';
import { trim } from '../services';
import { IoPeopleCircleOutline } from 'react-icons/io5';

// TODO: flattening ought not be a thing, instead we should be able to render the peerNetwork property

class App extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      peerNetworks: [],
    };
  }

  columns = [
    { dataField: 'id', isKey: true },
    { dataField: 'networkId', sortable: true, searchable: true, required: true },
    { dataField: 'name', sortable: true, searchable: true, required: true },
    { dataField: 'connectionModel', sortable: true, searchable: true, required: true },
    { dataField: 'peerNetworkName', sortable: false, searchable: false },
  ];

  renderForm = (row, validationErrors) => {
    const T = this.props.translations;
    return this.columns
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
          const networks = [{ id: '', name: '' }, ...this.state.peerNetworks];
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

  validateForm = form => {
    const data = {
      networkId: trim(form.networkId),
      name: trim(form.name),
      connectionModel: trim(form.connectionModel),
    };
    const extras = { peerNetwork: parseInt(form.peerNetwork, 10) || null };
    const missing = this.columns.find(col => col.required && data[col.dataField] === '');
    const T = this.props.translations;
    const params = missing && { field: T.t(`adminPage.networks.fieldName.${missing.dataField}`) };
    return missing
      ? {
          validationErrors: {
            [missing.dataField]: T.t('adminForm.validation.fieldIsRequired', params),
          },
        }
      : { data, extras };
  };

  flattenNetwork = network => {
    return { ...network, peerNetworkName: (network.peerNetwork && network.peerNetwork.name) || '' };
  };

  setPeerNetwork = (response, extras) => {
    if (extras && extras.peerNetwork) {
      const data = { network: parseInt(extras.peerNetwork, 10) };
      return axios.post('/api/v2/networks/' + response.data.id + '/peer', data);
    } else {
      return response;
    }
  };

  loadPeerNetworks = row => {
    if (!row.peerNetwork_id) {
      this.setState({ peerNetworks: [] });
      axios
        .get('/api/v2/networks;filter=peerNetwork_id:eq(null)')
        .then(response => {
          this.setState({ peerNetworks: response.data.objects });
        })
        .catch(error => {
          // hmm
          console.log(error);
        });
    }
  };

  render() {
    return (
      <ReactTable
        entity="networks"
        embed="peerNetwork"
        parseEntity={this.flattenNetwork}
        columns={this.columns}
        defaultSortField="networkId"
        defaultSearchField="networkId"
        renderForm={this.renderForm}
        beforeCreateOrUpdate={this.loadPeerNetworks}
        afterCreateOrUpdate={this.setPeerNetwork}
        validateForm={this.validateForm}
        translations={this.props.translations}
        setPortalAlertStatus={this.props.setPortalAlertStatus}
      />
    );
  }
}

App.propTypes = {
  setPortalAlertStatus: PropTypes.func.isRequired,
  translations: LoPropTypes.translations.isRequired,
};

function mapStateToProps(state) {
  return {
    translations: state.main.translations,
  };
}

function mapDispatchToProps(dispatch) {
  return bindActionCreators(MainActions, dispatch);
}

const Networks = connect(mapStateToProps, mapDispatchToProps)(App);

Networks.pageInfo = {
  identifier: 'networks',
  icon: IoPeopleCircleOutline,
  link: '/SocialNetworks',
  group: 'users',
  right: 'loi.cp.admin.right.AdminRight',
  entity: 'networks',
};

export default Networks;
