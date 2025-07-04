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

import { AdminFormFile } from '../components/adminForm';
import ReactTable from '../components/reactTable/ReactTable';
import WaitDotGif from '../components/WaitDotGif';
import * as MainActions from '../redux/actions/MainActions';
import { ContentTypeMultipart } from '../services';
import { BsPersonLock } from 'react-icons/bs';

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
      dataField: 'email',
      searchable: true,
      searchOperator: 'eq',
    },
  ];

  getButtonInfo = () => [
    {
      name: 'upload',
      iconName: 'cloud_upload',
      onClick: () => {
        this.showModal('upload');
        return Promise.resolve(false);
      },
      className: 'btn-success',
      alwaysEnabled: true,
    },
  ];

  renderForm() {}

  validateForm() {}

  renderModal = () => {
    const { translations: T } = this.props;
    const { modalType, modalSubmitting, uploadFile } = this.state;
    return (
      this.state.modalType && (
        <Modal
          id="restrictedLearner-table-modal"
          isOpen={true}
          backdrop="static"
          size="lg"
          autoFocus={false}
          toggle={this.hideModal}
          className="crudTable-modal"
        >
          <ModalHeader tag="h2">
            {T.t(`adminPage.restrictedLearners.modal.${modalType}.title`)}
          </ModalHeader>
          <form
            id="restrictedLearner-modalForm"
            className="admin-form"
            onSubmit={this.onSubmit}
          >
            <ModalBody>
              <div className="mt-3">
                <AdminFormFile
                  entity="restrictedLearners"
                  field="emails"
                  accept={['.csv']}
                  noUpload
                  required
                  help={T.t(`adminPage.restrictedLearners.modal.${modalType}.emailsHelp`)}
                  onChange={uploadFile => this.setState({ uploadFile })}
                  T={T}
                />
              </div>
            </ModalBody>
            <ModalFooter>
              <Button
                id="restrictedLearner-table-close-modal-btn"
                disabled={modalSubmitting}
                onClick={this.hideModal}
              >
                {T.t('crudTable.modal.closeButton')}
              </Button>{' '}
              <Button
                id="restrictedLearner-table-submit-modal-btn"
                type="submit"
                color="primary"
                disabled={modalSubmitting || !uploadFile}
              >
                {T.t(`adminPage.restrictedLearners.modal.${modalType}.submitButton`)}
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
      .post(`/api/v2/restrictedLearners/upload`, formData, ContentTypeMultipart)
      .then(res => {
        const count = res.data;
        this.hideModal();
        this.refreshTable();
        this.props.setPortalAlertStatus(
          count === 0,
          count > 0,
          T.t(`adminPage.restrictedLearners.${modalType}Alert`, { count })
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
    return (
      <React.Fragment>
        <ReactTable
          entity="restrictedLearners"
          createButton={false}
          updateButton={false}
          deleteButton={true}
          columns={this.columns}
          defaultSortField="email"
          defaultSortOrder="asc"
          defaultSearchField="email"
          getButtons={this.getButtonInfo}
          renderForm={this.renderForm}
          validateForm={this.validateForm}
          translations={this.props.translations}
          setPortalAlertStatus={this.props.setPortalAlertStatus}
          refreshRef={r => (this.refreshTable = r)}
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

const RestrictedLearners = connect(mapStateToProps, mapDispatchToProps)(App);

RestrictedLearners.pageInfo = {
  identifier: 'restrictedLearners',
  icon: BsPersonLock,
  link: '/RestrictedLearners',
  group: 'users',
  right: 'loi.cp.admin.right.UserAdminRight',
  entity: 'restrictedLearners',
};

export default RestrictedLearners;
