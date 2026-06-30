/*
 * LO Platform copyright (C) 2007–2026 LO Ventures LLC.
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
import React, { useEffect, useRef, useState } from 'react';
import { useDispatch } from 'react-redux';
import { Button, Modal, ModalBody, ModalFooter, ModalHeader } from 'reactstrap';

import { AdminFormCheck, AdminFormField, AdminFormSelect } from '../components/adminForm';
import ReactTable from '../components/reactTable/ReactTable';
import WaitDotGif from '../components/WaitDotGif';
import { setPortalAlertStatus } from '../redux/actions/MainActions';
import { useTranslations } from '../redux/state';
import { ContentTypeURLEncoded } from '../services';
import { SrsCollection } from '../srs';
import { IoPersonAddOutline } from 'react-icons/io5';

interface DomainRole {
  id: string;
  name: string;
}

interface AccountRequestRow {
  id: number;
  user: { fullName: string; emailAddress: string };
  attributes?: Record<string, unknown>;
}

interface AccountRequestsPageInfo {
  identifier: string;
  icon: React.ElementType;
  link: string;
  group: string;
  right: string;
  entity: string;
}

const AccountRequests: React.FC & { pageInfo: AccountRequestsPageInfo } = () => {
  const T = useTranslations();
  const dispatch = useDispatch();
  const [modalType, setModalType] = useState<string | null>(null);
  const [modalRow, setModalRow] = useState<AccountRequestRow | null>(null);
  const [modalSubmitting, setModalSubmitting] = useState(false);
  const [roles, setRoles] = useState<DomainRole[] | null>(null);
  const refreshTable = useRef<() => void>(() => null);

  useEffect(() => {
    axios
      .get<SrsCollection<DomainRole>>('/api/v2/accountRequests/roles')
      .then(rolesRes => setRoles(rolesRes.data.objects));
  }, []);

  const formatCreateTime = (t: string) => {
    const fmt = T.t('adminPage.accountRequests.momentFormat.createTime');
    return t ? moment(t).format(fmt) : '';
  };

  const columns = [
    { dataField: 'id', isKey: true },
    {
      dataField: 'createTime',
      sortable: true,
      defaultSort: 'desc',
      width: '20%',
      dataFormat: formatCreateTime,
    },
    {
      dataField: 'user.fullName',
      searchable: true,
      searchOperator: 'ts',
      dataFormat: (_a: unknown, b: AccountRequestRow) => b.user.fullName,
    },
    {
      dataField: 'user.emailAddress',
      searchable: true,
      searchOperator: 'sw',
      dataFormat: (_a: unknown, b: AccountRequestRow) => b.user.emailAddress,
    },
  ];

  const showModal = (style: string, row: AccountRequestRow) => {
    setModalType(style);
    setModalRow(row);
    setModalSubmitting(false);
  };

  const hideModal = () => {
    setModalType(null);
    setModalRow(null);
  };

  const getButtonInfo = (selectedRow: AccountRequestRow) => [
    {
      name: 'approve',
      iconName: 'check',
      onClick: () => {
        showModal('approve', selectedRow);
        return Promise.resolve(false);
      },
      className: 'btn-success',
    },
    {
      name: 'reject',
      iconName: 'close',
      onClick: () => {
        showModal('reject', selectedRow);
        return Promise.resolve(false);
      },
      className: 'btn-danger lastButton',
    },
  ];

  const renderForm = () => null;

  const validateForm = () => {};

  const onSubmit = (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    if (!modalRow || !modalType) return;
    const isApprove = modalType === 'approve';
    const form = e.target as typeof e.target & {
      email: { checked: boolean };
      domainRole: { value: string };
    };
    const email = form.email.checked;
    const role = isApprove ? form.domainRole.value : '';
    const action = isApprove ? 'accept' : 'reject';
    const data = 'email=' + email + '&role=' + role;
    setModalSubmitting(true);
    axios
      .post(`/api/v2/accountRequests/${modalRow.id}/${action}`, data, ContentTypeURLEncoded)
      .then(() => {
        setModalSubmitting(false);
        setModalType(null);
        refreshTable.current();
        dispatch(
          setPortalAlertStatus(
            false,
            true,
            T.t(`adminPage.accountRequests.${modalType}Alert`, modalRow.user)
          )
        );
      })
      .catch(e => {
        console.log(e);
        setModalSubmitting(false); // meh.. close modal
        setModalType(null);
        dispatch(setPortalAlertStatus(true, false, T.t('error.unexpectedError')));
      });
  };

  // TODO: one day refactor reactable so that it can raise custom modal types
  const renderModal = () => {
    if (!modalType || !modalRow || !roles) return null;
    const isApprove = modalType === 'approve';
    const hasAttributes =
      modalRow && modalRow.attributes && !!Object.keys(modalRow.attributes).length;
    const options = [
      { id: '', name: T.t('adminPage.accountRequests.domainRole.none') },
    ].concat(roles.sort((a, b) => a.name.toLowerCase().localeCompare(b.name.toLowerCase())));
    return (
      <Modal
        id="accountRequest-table-modal"
        isOpen={true}
        backdrop="static"
        size="lg"
        autoFocus={false}
        toggle={hideModal}
        className="crudTable-modal"
      >
        <ModalHeader tag="h2">
          {T.t(`adminPage.accountRequests.modal.${modalType}.title`, modalRow.user)}
        </ModalHeader>
        <form
          id="accountRequest-modalForm"
          className="admin-form"
          onSubmit={onSubmit}
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
              onClick={hideModal}
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
    );
  };

  if (!roles) return null;
  return (
    <React.Fragment>
      <ReactTable
        entity="accountRequests"
        createButton={false}
        updateButton={false}
        deleteButton={false}
        columns={columns}
        defaultSortField="createTime"
        defaultSortOrder="desc"
        defaultSearchField="user.fullName"
        getButtons={getButtonInfo}
        renderForm={renderForm}
        validateForm={validateForm}
        translations={T}
        setPortalAlertStatus={(error: any, success: boolean, message: string) =>
          dispatch(setPortalAlertStatus(error, success, message))
        }
        refreshRef={r => (refreshTable.current = r)}
      />
      {renderModal()}
    </React.Fragment>
  );
};

AccountRequests.pageInfo = {
  identifier: 'accountRequests',
  icon: IoPersonAddOutline,
  link: '/AccountRequests',
  group: 'users',
  right: 'loi.cp.admin.right.AdminRight',
  entity: 'accountRequests',
};

export default AccountRequests;
