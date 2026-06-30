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

import axios, { AxiosResponse } from 'axios';
import moment from 'moment/moment';
import React, { useRef, useState } from 'react';
import { useDispatch } from 'react-redux';
import { useNavigate } from 'react-router-dom';
import { Button, Modal, ModalBody, ModalFooter, ModalHeader } from 'reactstrap';

import ReactTable from '../components/reactTable/ReactTable';
import { setPortalAlertStatus } from '../redux/actions/MainActions';
import { useTranslations } from '../redux/state';
import AccessCodeTypes from './AccessCodeTypes';
import {
  AccessCodeForm,
  ModalError,
  ValidationErrors,
} from './AccessCodeTypes/AccessCodeBatch';

interface BatchRow {
  id: number;
  disabled?: boolean;
  redemptionLimit: number;
  [key: string]: unknown;
}

const postUrl = '/api/v2/accessCodes/batches';

const AccessCodeBatches: React.FC = () => {
  const navigate = useNavigate();
  const T = useTranslations();
  const dispatch = useDispatch();
  const [type, setType] = useState<string | null>(null);
  // The form is collected in validateForm and read back in afterCreateOrUpdate
  // within the same submit cycle (no re-render between), so a ref preserves the
  // original synchronous `this.state.form` read.
  const formRef = useRef<AccessCodeForm>({});
  const [modalError, setModalError] = useState<ModalError | null>(null);
  const [downloadLink, setDownloadLink] = useState<string | null>(null);

  const formatCreateTime = (t: string) => {
    const fmt = T.t('adminPage.courseSections.momentFormat.createTime');
    return t ? moment(t).format(fmt) : '';
  };

  const columns = [
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
      dataFormat: formatCreateTime,
      width: '10%',
    },
    {
      dataField: 'redemptionCount',
      sortable: false,
      searchable: false,
      filterable: false,
      dataFormat: (t: [number, number], r: BatchRow) => `${t[0]} / ${t[1] * r.redemptionLimit}`,
      width: '10%',
    },
  ];

  const onModalErrorChange = (err: ModalError | null) => setModalError(err);

  const renderForm = (_row: unknown, validationErrors: ValidationErrors) => {
    const Component = AccessCodeTypes[type!].component;
    return (
      <Component
        onModalErrorChange={onModalErrorChange}
        validationErrors={validationErrors}
        T={T}
      />
    );
  };

  const validateForm = (form: AccessCodeForm) => {
    formRef.current = form;
    if (modalError) {
      const field = modalError.field;
      return { validationErrors: { [field]: modalError.message } };
    } else {
      return AccessCodeTypes[type!].validateForm(form, T, modalError);
    }
  };

  const afterCreateOrUpdate = (res: AxiosResponse): Promise<AxiosResponse> => {
    const form = formRef.current;
    return AccessCodeTypes[type!].afterCreateOrUpdate(res, form).then(r2 => {
      if (form.generating) {
        setDownloadLink(`/api/v2/accessCodes/batches/${res.data.id}/export.csv`);
      }
      return r2;
    });
  };

  const getDropDownItems = () => {
    return [
      {
        name: T.t('adminPage.accessCodes.types.iacAccessCodeBatch'),
        key: 'iacAccessCodeBatch',
        onClick: () => setType('iacAccessCodeBatch'),
      },
      {
        name: T.t('adminPage.accessCodes.types.enrollAccessCodeBatch'),
        key: 'enrollAccessCodeBatch',
        onClick: () => setType('enrollmentAccessCodeBatch'),
      },
    ];
  };

  const renderDownloadModal = () => {
    const hider = () => setDownloadLink(null);
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
            href={downloadLink ?? undefined}
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

  const renderModal = () => {
    if (downloadLink) {
      return renderDownloadModal();
    } else {
      return null;
    }
  };

  const transition = (row: BatchRow) => {
    return axios
      .put(`/api/v2/accessCodes/batches/${row.id}/disabled`, `${!row.disabled}`, {
        headers: { 'Content-Type': 'application/json;charset=UTF-8' },
      })
      .catch(e => {
        console.log(e);
        dispatch(setPortalAlertStatus(true, false, T.t('error.unexpectedError')));
      });
  };

  const onViewClick = (row: BatchRow) => {
    navigate(`/AccessCodes/${row.id}`);
    return Promise.resolve(false);
  };

  const getButtonInfo = (row: BatchRow) => {
    return [
      {
        name: !row.disabled ? 'suspend' : 'reinstate',
        iconName: !row.disabled ? 'not_interested' : 'check',
        onClick: transition,
      },
      {
        name: 'viewAccessCodes',
        iconName: 'visibility',
        onClick: onViewClick,
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

  const trClassFormat = ({ disabled }: { id: number; disabled?: boolean }) =>
    disabled ? 'row-disabled' : '';

  return (
    <React.Fragment>
      <ReactTable
        entity="accessCodes/batches"
        dropdownItems={getDropDownItems()}
        columns={columns}
        defaultSortField="createTime"
        defaultSortOrder="desc"
        defaultSearchField="name"
        createButton={false}
        renderForm={renderForm}
        validateForm={validateForm}
        translations={T}
        createDropdown={true}
        deleteButton={true}
        updateButton={false}
        postUrl={postUrl}
        afterCreateOrUpdate={afterCreateOrUpdate}
        schema={type ?? undefined}
        getButtons={getButtonInfo}
        trClassFormat={trClassFormat}
        embed="redemptionCount"
      />
      {renderModal()}
    </React.Fragment>
  );
};

export default AccessCodeBatches;
