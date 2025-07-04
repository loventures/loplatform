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

import ReactTable from '../components/reactTable/ReactTable';
import LoPropTypes from '../react/loPropTypes';
import * as MainActions from '../redux/actions/MainActions';
import AccessCodeInfo from './AccessCodeInfo';

class Codes extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      type: null,
      form: {},
      accessCodeInfo: null,
      modalError: null,
    };
  }

  componentDidMount() {
    const { match, setLastCrumb } = this.props;
    const { batchId } = match.params;
    axios.get(`/api/v2/accessCodes/batches/${batchId}`).then(res => {
      setLastCrumb(res.data.name);
    });
  }

  columns = [
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

  renderModal = () => {
    const { translations: T } = this.props;
    const { accessCodeInfo } = this.state;
    if (accessCodeInfo) {
      return (
        <AccessCodeInfo
          T={T}
          accessCodeInfo={accessCodeInfo}
          close={() => this.setState({ accessCodeInfo: null })}
        />
      );
    } else {
      return null;
    }
  };

  onViewClick = row => {
    this.setState({ accessCodeInfo: row });
    return Promise.resolve(false);
  };

  getButtonInfo = () => {
    return [
      {
        name: 'viewAccessCode',
        iconName: 'visibility',
        onClick: this.onViewClick,
      },
    ];
  };

  render() {
    const { type } = this.state;
    return (
      <React.Fragment>
        <ReactTable
          entity="accessCodes"
          columns={this.columns}
          defaultSortField="accessCode"
          defaultSearchField="accessCode"
          createButton={false}
          renderForm={this.renderForm}
          validateForm={this.validateForm}
          translations={this.props.translations}
          setPortalAlertStatus={this.props.setPortalAlertStatus}
          deleteButton={true}
          updateButton={false}
          postUrl={this.postUrl}
          embed="redemptions,batch"
          schema={type}
          getButtons={this.getButtonInfo}
          customFilters={[
            {
              property: 'batch_id',
              operator: 'eq',
              value: this.props.match.params.batchId,
              prefilter: true,
            },
          ]}
        />
        {this.renderModal()}
      </React.Fragment>
    );
  }
}

Codes.propTypes = {
  translations: LoPropTypes.translations,
  setPortalAlertStatus: PropTypes.func.isRequired,
};

function mapStateToProps(state) {
  return {
    translations: state.main.translations,
  };
}

function mapDispatchToProps(dispatch) {
  return bindActionCreators(MainActions, dispatch);
}

const AccessCodes = connect(mapStateToProps, mapDispatchToProps)(Codes);

export default AccessCodes;
