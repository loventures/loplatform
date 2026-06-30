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
import { AdminFormCheck, AdminFormField, AdminFormFile } from '../components/adminForm';
import ReactTable, { ReactTableProps } from '../components/reactTable/ReactTable';
import WaitDotGif from '../components/WaitDotGif';
import { setPortalAlertStatus } from '../redux/actions/MainActions';
import { useTranslations } from '../redux/state';
import { ContentTypeMultipart, trim } from '../services';
import { GiSpikedFence } from 'react-icons/gi';

const RestrictedSites: React.FC & AdminPage = () => {
  const T = useTranslations();
  const dispatch = useDispatch();
  const [modalType, setModalType] = useState<string | null>(null);
  const [modalSubmitting, setModalSubmitting] = useState(false);
  const [uploadFile, setUploadFile] = useState<File | null>(null);
  const refreshTable = useRef<(() => void) | null>(null);

  const columns = [
    { dataField: 'id', isKey: true },
    {
      dataField: 'restricted',
      width: '10%',
      dataFormat: (value: any) => T.t(`adminPage.sites.restricted.${value}`),
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

  const renderForm = (row: any, validationErrors: any) => {
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

  const validateForm = (form: any, row: any) => {
    const data = {
      siteId: row.id ? undefined : trim(form.siteId),
      name: trim(form.name),
      restricted: !!form.restricted,
    };
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

  const onSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setModalSubmitting(true);
    const formData = new FormData();
    formData.append('upload', uploadFile as Blob);
    axios
      .post(`/api/v2/restrictedSites/upload`, formData, ContentTypeMultipart)
      .then(res => {
        const count = res.data;
        hideModal();
        refreshTable.current?.();
        dispatch(
          setPortalAlertStatus(
            count === 0,
            count > 0,
            T.t(`adminPage.restrictedSites.${modalType}Alert`, { count })
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
          id="restrictedSite-table-modal"
          isOpen={true}
          backdrop="static"
          size="lg"
          autoFocus={false}
          toggle={hideModal}
          className="crudTable-modal"
        >
          <ModalHeader tag="h2">
            {T.t(`adminPage.restrictedSites.modal.${modalType}.title`)}
          </ModalHeader>
          <form
            id="restrictedSite-modalForm"
            className="admin-form"
            onSubmit={onSubmit}
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
                  onChange={(uploadFile: File) => setUploadFile(uploadFile)}
                  T={T}
                />
              </div>
            </ModalBody>
            <ModalFooter>
              <Button
                id="restrictedSite-table-close-modal-btn"
                disabled={modalSubmitting}
                onClick={hideModal}
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

  return (
    <React.Fragment>
      <ReactTable
        entity="sites"
        createButton={true}
        updateButton={true}
        deleteButton={true}
        columns={columns}
        defaultSortField="siteId"
        defaultSortOrder="asc"
        defaultSearchField="siteId"
        renderForm={renderForm}
        validateForm={validateForm}
        translations={T}
        setPortalAlertStatus={(error, success, message) =>
          dispatch(setPortalAlertStatus(error, success, message))
        }
        refreshRef={r => (refreshTable.current = r)}
        onModalError={((e: any) => {
          const dup = e?.response?.data?.duplicateSiteId;
          return dup
            ? {
                modalError: T.t('adminPage.sites.alert.formError'),
                validationErrors: {
                  siteId: T.t('adminPage.sites.duplicateSiteId'),
                },
              }
            : undefined;
        }) as ReactTableProps['onModalError']}
      />
      {renderModal()}
    </React.Fragment>
  );
};

RestrictedSites.pageInfo = {
  identifier: 'restrictedSites',
  icon: GiSpikedFence,
  link: '/RestrictedSites',
  group: 'users',
  right: 'loi.cp.admin.right.IntegrationAdminRight',
  entity: 'restrictedSites',
};

export default RestrictedSites;
