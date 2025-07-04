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
import moment from 'moment/moment';
import PropTypes from 'prop-types';
import React from 'react';
import { connect } from 'react-redux';
import { Button, Modal, ModalBody, ModalFooter, ModalHeader } from 'reactstrap';
import { bindActionCreators } from 'redux';

import ReactTable from '../components/reactTable/ReactTable';
import LoPropTypes from '../react/loPropTypes';
import * as MainActions from '../redux/actions/MainActions';
import AccessCodeTypes from './AccessCodeTypes';

class Batches extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      type: null,
      form: {},
      modalError: null,
      downloadLink: null,
    };
    this.postUrl = '/api/v2/accessCodes/batches';
  }

  formatCreateTime = t => {
    const { translations: T } = this.props;
    const fmt = T.t('adminPage.courseSections.momentFormat.createTime');
    return t ? moment(t).format(fmt) : '';
  };

  columns = [
    { dataField: 'id', isKey: true },
    {
      dataField: 'name',
      sortable: true,
      searchable: true,
      filterable: false,
      searchOperator: 'co',
      width: '20%',
    },
    { dataField: 'description', sortable: false, searchable: false, filterable: false },
    {
      dataField: 'createTime',
      sortable: true,
      searchable: false,
      filterable: false,
      dataFormat: this.formatCreateTime,
      width: '10%',
    },
    {
      dataField: 'redemptionCount',
      sortable: false,
      searchable: false,
      filterable: false,
      dataFormat: (t, r) => `${t[0]} / ${t[1] * r.redemptionLimit}`,
      width: '10%',
    },
  ];

  renderForm = (row, validationErrors) => {
    const Component = AccessCodeTypes[this.state.type].component;
    return (
      <Component
        onModalErrorChange={this.onModalErrorChange}
        validationErrors={validationErrors}
        T={this.props.translations}
      />
    );
  };

  onModalErrorChange = err => this.setState({ modalError: err });

  validateForm = form => {
    const { type, modalError } = this.state;
    const { translations: T } = this.props;
    this.setState({ form: form });
    if (modalError) {
      const field = modalError.field;
      return { validationErrors: { [field]: modalError.message } };
    } else {
      return AccessCodeTypes[type].validateForm(form, T, modalError);
    }
  };

  afterCreateOrUpdate = res => {
    const { type, form } = this.state;
    return AccessCodeTypes[type].afterCreateOrUpdate(res, form).then(r2 => {
      if (form.generating) {
        this.setState({ downloadLink: `/api/v2/accessCodes/batches/${res.data.id}/export.csv` });
      }
      return r2;
    });
  };

  getDropDownItems = () => {
    const { translations: T } = this.props;
    return [
      {
        name: T.t('adminPage.accessCodes.types.iacAccessCodeBatch'),
        key: 'iacAccessCodeBatch',
        onClick: () => this.setState({ type: 'iacAccessCodeBatch' }),
      },
      {
        name: T.t('adminPage.accessCodes.types.enrollAccessCodeBatch'),
        key: 'enrollAccessCodeBatch',
        onClick: () => this.setState({ type: 'enrollmentAccessCodeBatch' }),
      },
    ];
  };

  renderModal = () => {
    const { downloadLink } = this.state;
    if (downloadLink) {
      return this.renderDownloadModal();
    } else {
      return null;
    }
  };

  renderDownloadModal = () => {
    const { translations: T } = this.props;
    const { downloadLink } = this.state;
    const hider = () => this.setState({ downloadLink: null });
    return (
      <Modal
        id="accessCodes-download-modal"
        isOpen={true}
        backdrop="static"
        size="md"
        toggle={hider}
      >
        <ModalHeader
          id="accessCodes-details-modal-header"
          tag="h2"
        >
          {T.t('adminPage.accessCodes.modal.download.title')}
        </ModalHeader>
        <ModalBody>
          <p>{T.t('adminPage.accessCodes.modal.download.message')}</p>
          <Button
            id={`accessCode-download-button`}
            href={downloadLink}
            download
            color="success"
          >
            {T.t('adminPage.accessCodes.button.download')}
            <i
              className="material-icons md-18 ms-1"
              aria-hidden="true"
            >
              vertical_align_bottom
            </i>
          </Button>
        </ModalBody>
        <ModalFooter>
          <Button
            id="accessCodes-details-modal-close"
            color="secondary"
            onClick={hider}
          >
            {T.t('adminPage.accessCodes.accessCodeInfo.close')}
          </Button>
        </ModalFooter>
      </Modal>
    );
  };

  transition = row => {
    return axios
      .put(`/api/v2/accessCodes/batches/${row.id}/disabled`, `${!row.disabled}`, {
        headers: { 'Content-Type': 'application/json;charset=UTF-8' },
      })
      .catch(e => {
        console.log(e);
        const T = this.props.translations;
        this.props.setPortalAlertStatus(true, false, T.t('error.unexpectedError'));
      });
  };

  onViewClick = row => {
    this.props.history.push(`/AccessCodes/${row.id}`);
    return Promise.resolve(false);
  };

  getButtonInfo = row => {
    return [
      {
        name: !row.disabled ? 'suspend' : 'reinstate',
        iconName: !row.disabled ? 'not_interested' : 'check',
        onClick: this.transition,
      },
      {
        name: 'viewAccessCodes',
        iconName: 'visibility',
        onClick: this.onViewClick,
      },
      {
        name: 'download',
        iconName: 'download',
        href: `/api/v2/accessCodes/batches/${row.id}/redemptionReport.csv`,
        target: '_blank',
        download: true,
      },
    ];
  };

  trClassFormat = ({ disabled }) => (disabled ? 'row-disabled' : '');

  render() {
    const { type } = this.state;
    return (
      <React.Fragment>
        <ReactTable
          entity="accessCodes/batches"
          dropdownItems={this.getDropDownItems()}
          columns={this.columns}
          defaultSortField="createTime"
          defaultSortOrder="desc"
          defaultSearchField="name"
          createButton={false}
          renderForm={this.renderForm}
          validateForm={this.validateForm}
          translations={this.props.translations}
          createDropdown={true}
          setPortalAlertStatus={this.props.setPortalAlertStatus}
          deleteButton={true}
          updateButton={false}
          postUrl={this.postUrl}
          afterCreateOrUpdate={this.afterCreateOrUpdate}
          schema={type}
          getButtons={this.getButtonInfo}
          trClassFormat={this.trClassFormat}
          embed="redemptionCount"
        />
        {this.renderModal()}
      </React.Fragment>
    );
  }
}

Batches.propTypes = {
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

const AccessCodeBatches = connect(mapStateToProps, mapDispatchToProps)(Batches);

export default AccessCodeBatches;
