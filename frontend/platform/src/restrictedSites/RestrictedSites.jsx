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
import { Button, Modal, ModalBody, ModalFooter, ModalHeader } from 'reactstrap';
import { bindActionCreators } from 'redux';

import { AdminFormCheck, AdminFormField, AdminFormFile } from '../components/adminForm';
import ReactTable from '../components/reactTable/ReactTable';
import WaitDotGif from '../components/WaitDotGif';
import * as MainActions from '../redux/actions/MainActions';
import { ContentTypeMultipart, trim } from '../services';
import { GiSpikedFence } from 'react-icons/gi';

class App extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      modalType: null,
      modalSubmitting: false,
      uploadFile: null,
    };
  }

  columns = [
    { dataField: 'id', isKey: true },
    {
      dataField: 'restricted',
      width: '10%',
      dataFormat: value => this.props.translations.t(`adminPage.sites.restricted.${value}`),
      filterable: true,
      baseFilter: 'Any Restriction',
      filterProperty: 'restricted',
      filterOptions: [
        <option
          key="true"
          value="true"
        >
          Restricted
        </option>,
        <option
          key="false"
          value="false"
        >
          Unrestricted
        </option>,
      ],
    },
    {
      dataField: 'siteId',
      searchable: true,
      filterable: true,
      searchOperator: 'eq',
      width: '30%',
    },
    {
      dataField: 'name',
      searchable: true,
      filterable: true,
      searchOperator: 'sw',
      width: '60%',
    },
  ];

  renderForm = (row, validationErrors) => {
    const { translations: T } = this.props;
    return (
      <React.Fragment>
        {row.id && <div className="entity-id">{row.id}</div>}
        <AdminFormField
          entity="sites"
          field="siteId"
          value={row.siteId}
          invalid={validationErrors.siteId}
          type="text"
          readOnly={!!row.id}
          required
          autoFocus
          T={T}
        />
        <AdminFormField
          entity="sites"
          field="name"
          value={row.name}
          invalid={validationErrors.name}
          type="text"
          required
          T={T}
        />
        <AdminFormCheck
          entity="sites"
          field="restricted"
          value={row.restricted}
          T={T}
        />
      </React.Fragment>
    );
  };

  validateForm = (form, row) => {
    const data = {
      siteId: row.id ? undefined : trim(form.siteId),
      name: trim(form.name),
      restricted: !!form.restricted,
    };
    const T = this.props.translations;
    if (!row.id && !data.siteId) {
      const params = { field: T.t(`adminPage.sites.fieldName.siteId`) };
      return {
        validationErrors: { siteId: T.t('adminForm.validation.fieldIsRequired', params) },
      };
    } else if (!data.name) {
      const params = { field: T.t(`adminPage.sites.fieldName.name`) };
      return {
        validationErrors: { name: T.t('adminForm.validation.fieldIsRequired', params) },
      };
    } else {
      return { data };
    }
  };

  renderModal = () => {
    const { translations: T } = this.props;
    const { modalType, modalSubmitting, uploadFile } = this.state;
    return (
      this.state.modalType && (
        <Modal
          id="restrictedSite-table-modal"
          isOpen={true}
          backdrop="static"
          size="lg"
          autoFocus={false}
          toggle={this.hideModal}
          className="crudTable-modal"
        >
          <ModalHeader tag="h2">
            {T.t(`adminPage.restrictedSites.modal.${modalType}.title`)}
          </ModalHeader>
          <form
            id="restrictedSite-modalForm"
            className="admin-form"
            onSubmit={this.onSubmit}
          >
            <ModalBody>
              <div className="mt-3">
                <AdminFormFile
                  entity="restrictedSites"
                  field="emails"
                  accept={['.csv']}
                  noUpload
                  required
                  help={T.t(`adminPage.restrictedSites.modal.${modalType}.emailsHelp`)}
                  onChange={uploadFile => this.setState({ uploadFile })}
                  T={T}
                />
              </div>
            </ModalBody>
            <ModalFooter>
              <Button
                id="restrictedSite-table-close-modal-btn"
                disabled={modalSubmitting}
                onClick={this.hideModal}
              >
                {T.t('crudTable.modal.closeButton')}
              </Button>{' '}
              <Button
                id="restrictedSite-table-submit-modal-btn"
                type="submit"
                color="primary"
                disabled={modalSubmitting || !uploadFile}
              >
                {T.t(`adminPage.restrictedSites.modal.${modalType}.submitButton`)}
                {modalSubmitting && (
                  <WaitDotGif
                    className="ms-2 waiting"
                    color="light"
                    size={16}
                  />
                )}
              </Button>
            </ModalFooter>
          </form>
        </Modal>
      )
    );
  };

  onSubmit = e => {
    e.preventDefault();
    const T = this.props.translations;
    const { modalType, uploadFile } = this.state;
    this.setState({ modalSubmitting: true });
    const formData = new FormData();
    formData.append('upload', uploadFile);
    axios
      .post(`/api/v2/restrictedSites/upload`, formData, ContentTypeMultipart)
      .then(res => {
        const count = res.data;
        this.hideModal();
        this.refreshTable();
        this.props.setPortalAlertStatus(
          count === 0,
          count > 0,
          T.t(`adminPage.restrictedSites.${modalType}Alert`, { count })
        );
      })
      .catch(e => {
        console.log(e);
        this.setState({ modalSubmitting: false, modalType: null, uploadFile: null }); // meh.. close modal
        this.props.setPortalAlertStatus(true, false, T.t('error.unexpectedError'));
      });
  };

  showModal = style => {
    this.setState({ modalType: style, modalSubmitting: false, uploadFile: null });
  };

  hideModal = () => {
    this.showModal(null);
  };

  render() {
    const { translations: T } = this.props;
    return (
      <React.Fragment>
        <ReactTable
          entity="sites"
          createButton={true}
          updateButton={true}
          deleteButton={true}
          columns={this.columns}
          defaultSortField="siteId"
          defaultSortOrder="asc"
          defaultSearchField="siteId"
          renderForm={this.renderForm}
          validateForm={this.validateForm}
          translations={T}
          setPortalAlertStatus={this.props.setPortalAlertStatus}
          refreshRef={r => (this.refreshTable = r)}
          onModalError={e => {
            const dup = e?.response?.data?.duplicateSiteId;
            return dup
              ? {
                  modalError: T.t('adminPage.sites.alert.formError'),
                  validationErrors: {
                    siteId: T.t('adminPage.sites.duplicateSiteId'),
                  },
                }
              : undefined;
          }}
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

const RestrictedSites = connect(mapStateToProps, mapDispatchToProps)(App);

RestrictedSites.pageInfo = {
  identifier: 'restrictedSites',
  icon: GiSpikedFence,
  link: '/RestrictedSites',
  group: 'users',
  right: 'loi.cp.admin.right.IntegrationAdminRight',
  entity: 'restrictedSites',
};

export default RestrictedSites;
