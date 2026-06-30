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
import React, { useRef, useState } from 'react';
import { useDispatch } from 'react-redux';
import { Button, Modal, ModalBody, ModalFooter, ModalHeader } from 'reactstrap';

import { AdminPage } from '../adminPortal/types';
import { AdminFormFile } from '../components/adminForm';
import ReactTable from '../components/reactTable/ReactTable';
import WaitDotGif from '../components/WaitDotGif';
import { setPortalAlertStatus } from '../redux/actions/MainActions';
import { useTranslations } from '../redux/state';
import { ContentTypeMultipart } from '../services';
import { BsPersonLock } from 'react-icons/bs';

const columns = [
  { dataField: 'id', isKey: true },
  {
    dataField: 'email',
    searchable: true,
    searchOperator: 'eq',
  },
];

const RestrictedLearners: React.FC & AdminPage = () => {
  const T = useTranslations();
  const dispatch = useDispatch();
  const [modalType, setModalType] = useState<string | null>(null);
  const [modalSubmitting, setModalSubmitting] = useState(false);
  const [uploadFile, setUploadFile] = useState<File | null>(null);
  const refreshTable = useRef<(() => void) | null>(null);

  const getButtonInfo = () => [
    {
      name: 'upload',
      iconName: 'cloud_upload',
      onClick: () => {
        showModal('upload');
        return Promise.resolve(false);
      },
      className: 'btn-success',
      alwaysEnabled: true,
    },
  ];

  const renderForm = () => null;

  const validateForm = () => {};

  const onSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setModalSubmitting(true);
    const formData = new FormData();
    formData.append('upload', uploadFile as Blob);
    axios
      .post(`/api/v2/restrictedLearners/upload`, formData, ContentTypeMultipart)
      .then(res => {
        const count = res.data;
        hideModal();
        refreshTable.current?.();
        dispatch(
          setPortalAlertStatus(
            count === 0,
            count > 0,
            T.t(`adminPage.restrictedLearners.${modalType}Alert`, { count })
          )
        );
      })
      .catch(e => {
        console.log(e);
        setModalSubmitting(false);
        setModalType(null);
        setUploadFile(null); // meh.. close modal
        dispatch(setPortalAlertStatus(true, false, T.t('error.unexpectedError')));
      });
  };

  const showModal = (style: string | null) => {
    setModalType(style);
    setModalSubmitting(false);
    setUploadFile(null);
  };

  const hideModal = () => {
    showModal(null);
  };

  const renderModal = () => {
    return (
      modalType && (
        <Modal
          id="restrictedLearner-table-modal"
          isOpen={true}
          backdrop="static"
          size="lg"
          autoFocus={false}
          toggle={hideModal}
          className="crudTable-modal"
        >
          <ModalHeader tag="h2">
            {T.t(`adminPage.restrictedLearners.modal.${modalType}.title`)}
          </ModalHeader>
          <form
            id="restrictedLearner-modalForm"
            className="admin-form"
            onSubmit={onSubmit}
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
                  onChange={(uploadFile: File) => setUploadFile(uploadFile)}
                  T={T}
                />
              </div>
            </ModalBody>
            <ModalFooter>
              <Button
                id="restrictedLearner-table-close-modal-btn"
                disabled={modalSubmitting}
                onClick={hideModal}
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

  return (
    <React.Fragment>
      <ReactTable
        entity="restrictedLearners"
        createButton={false}
        updateButton={false}
        deleteButton={true}
        columns={columns}
        defaultSortField="email"
        defaultSortOrder="asc"
        defaultSearchField="email"
        getButtons={getButtonInfo}
        renderForm={renderForm}
        validateForm={validateForm}
        translations={T}
        setPortalAlertStatus={(error, success, message) =>
          dispatch(setPortalAlertStatus(error, success, message))
        }
        refreshRef={r => (refreshTable.current = r)}
      />
      {renderModal()}
    </React.Fragment>
  );
};

RestrictedLearners.pageInfo = {
  identifier: 'restrictedLearners',
  icon: BsPersonLock,
  link: '/RestrictedLearners',
  group: 'users',
  right: 'loi.cp.admin.right.UserAdminRight',
  entity: 'restrictedLearners',
};

export default RestrictedLearners;
