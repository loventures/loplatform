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
import moment from 'moment';
import React from 'react';
import { connect } from 'react-redux';
import { Button, Modal, ModalBody, ModalFooter, ModalHeader } from 'reactstrap';
import { bindActionCreators } from 'redux';

import { AdminFormCheck, AdminFormField, AdminFormSelect } from '../components/adminForm';
import ReactTable from '../components/reactTable/ReactTable';
import WaitDotGif from '../components/WaitDotGif';
import * as MainActions from '../redux/actions/MainActions';
import { ContentTypeURLEncoded } from '../services';
import { IoPersonAddOutline } from 'react-icons/io5';

class App extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      modalType: null,
      modalRow: null,
      modalSubmitting: false,
      roles: null,
    };
  }

  componentDidMount() {
    axios
      .get('/api/v2/accountRequests/roles')
      .then(rolesRes => this.setState({ roles: rolesRes.data.objects }));
  }

  formatCreateTime = t => {
    const fmt = this.props.translations.t('adminPage.accountRequests.momentFormat.createTime');
    return t ? moment(t).format(fmt) : '';
  };

  columns = [
    { dataField: 'id', isKey: true },
    {
      dataField: 'createTime',
      sortable: true,
      defaultSort: 'desc',
      width: '20%',
      dataFormat: this.formatCreateTime,
    },
    {
      dataField: 'user.fullName',
      searchable: true,
      searchOperator: 'ts',
      dataFormat: (a, b) => b.user.fullName,
    },
    {
      dataField: 'user.emailAddress',
      searchable: true,
      searchOperator: 'sw',
      dataFormat: (a, b) => b.user.emailAddress,
    },
  ];

  getButtonInfo = selectedRow => [
    {
      name: 'approve',
      iconName: 'check',
      onClick: () => {
        this.showModal('approve', selectedRow);
        return Promise.resolve(false);
      },
      className: 'btn-success',
    },
    {
      name: 'reject',
      iconName: 'close',
      onClick: () => {
        this.showModal('reject', selectedRow);
        return Promise.resolve(false);
      },
      className: 'btn-danger lastButton',
    },
  ];

  renderForm() {}

  validateForm() {}

  // TODO: one day refactor reactable so that it can raise custom modal types
  renderModal = () => {
    const { translations: T } = this.props;
    const { modalType, modalSubmitting, modalRow, roles } = this.state;
    const isApprove = modalType === 'approve';
    const hasAttributes =
      modalRow && modalRow.attributes && !!Object.keys(modalRow.attributes).length;
    const options = [{ id: '', name: T.t('adminPage.accountRequests.domainRole.none') }].concat(
      roles.sort((a, b) => a.name.toLowerCase().localeCompare(b.name.toLowerCase()))
    );
    return (
      this.state.modalType && (
        <Modal
          id="accountRequest-table-modal"
          isOpen={true}
          backdrop="static"
          size="lg"
          autoFocus={false}
          toggle={this.hideModal}
          className="crudTable-modal"
        >
          <ModalHeader tag="h2">
            {T.t(`adminPage.accountRequests.modal.${modalType}.title`, modalRow.user)}
          </ModalHeader>
          <form
            id="accountRequest-modalForm"
            className="admin-form"
            onSubmit={this.onSubmit}
          >
            <ModalBody>
              <div className="mt-3">
                <AdminFormField
                  entity="accountRequests"
                  field="user.fullName"
                  value={modalRow.user.fullName}
                  readOnly
                  T={T}
                />
                <AdminFormField
                  entity="accountRequests"
                  field="user.emailAddress"
                  value={modalRow.user.emailAddress}
                  readOnly
                  T={T}
                />
                {isApprove && (
                  <AdminFormSelect
                    entity="accountRequests"
                    field="domainRole"
                    options={options}
                    T={T}
                  />
                )}
                <AdminFormCheck
                  entity="accountRequests"
                  inputName="email"
                  field={`${modalType}Email`}
                  value={isApprove}
                  T={T}
                />
                {hasAttributes && (
                  <pre className="my-1 p-2 bg-light border border-secondary rounded small">
                    {JSON.stringify(modalRow.attributes, null, 2)}
                  </pre>
                )}
              </div>
            </ModalBody>
            <ModalFooter>
              <Button
                id="accountRequest-table-close-modal-btn"
                disabled={modalSubmitting}
                onClick={this.hideModal}
              >
                {T.t('crudTable.modal.closeButton')}
              </Button>{' '}
              <Button
                id="accountRequest-table-submit-modal-btn"
                type="submit"
                color={isApprove ? 'primary' : 'danger'}
                disabled={modalSubmitting}
              >
                {T.t(`adminPage.accountRequests.modal.${modalType}.submitButton`)}
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
    const { modalType, modalRow } = this.state;
    const isApprove = modalType === 'approve';
    const email = e.target.email.checked;
    const role = isApprove ? e.target.domainRole.value : '';
    const action = isApprove ? 'accept' : 'reject';
    const data = 'email=' + email + '&role=' + role;
    this.setState({ modalSubmitting: true });
    axios
      .post(`/api/v2/accountRequests/${modalRow.id}/${action}`, data, ContentTypeURLEncoded)
      .then(() => {
        this.setState({ modalSubmitting: false, modalType: null });
        this.refreshTable();
        this.props.setPortalAlertStatus(
          false,
          true,
          T.t(`adminPage.accountRequests.${modalType}Alert`, modalRow.user)
        );
      })
      .catch(e => {
        console.log(e);
        this.setState({ modalSubmitting: false, modalType: null }); // meh.. close modal
        this.props.setPortalAlertStatus(true, false, T.t('error.unexpectedError'));
      });
  };

  showModal = (style, row) => {
    this.setState({ modalType: style, modalRow: row, modalSubmitting: false });
  };

  hideModal = () => {
    this.setState({ modalType: null, modalRow: null });
  };

  render() {
    return (
      this.state.roles && (
        <React.Fragment>
          <ReactTable
            entity="accountRequests"
            createButton={false}
            updateButton={false}
            deleteButton={false}
            columns={this.columns}
            defaultSortField="createTime"
            defaultSortOrder="desc"
            defaultSearchField="user.fullName"
            getButtons={this.getButtonInfo}
            renderForm={this.renderForm}
            validateForm={this.validateForm}
            translations={this.props.translations}
            setPortalAlertStatus={this.props.setPortalAlertStatus}
            refreshRef={r => (this.refreshTable = r)}
          />
          {this.renderModal()}
        </React.Fragment>
      )
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

const AccountRequests = connect(mapStateToProps, mapDispatchToProps)(App);

AccountRequests.pageInfo = {
  identifier: 'accountRequests',
  icon: IoPersonAddOutline,
  link: '/AccountRequests',
  group: 'users',
  right: 'loi.cp.admin.right.AdminRight',
  entity: 'accountRequests',
};

export default AccountRequests;
