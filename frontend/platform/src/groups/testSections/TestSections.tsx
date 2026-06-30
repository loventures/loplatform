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
import React, { useRef, useState } from 'react';
import { useDispatch } from 'react-redux';
import { useNavigate } from 'react-router-dom';
import {
  Button,
  Col,
  FormGroup,
  Input,
  Label,
  Modal,
  ModalBody,
  ModalFooter,
  ModalHeader,
} from 'reactstrap';

import ReactTable, {
  clearSavedTableState,
  ReactTableHandle,
} from '../../components/reactTable/ReactTable';
import { withProjectFilter } from '../../components/withProjectFilter';
import { putConfig } from '../../config/configApi';
import { setPortalAlertStatus } from '../../redux/actions/MainActions';
import { useLoPlatform, useTranslations } from '../../redux/state';
import Fjœrich from '../Fjoerich';
import getAuthorBtn from '../services/authorBtn';
import EditAddForm from './EditAddForm';

interface InitModal {
  projectId: number;
}

interface ColumnConfig {
  dataField: string;
  [key: string]: unknown;
}

interface TestSectionRow {
  id: number;
  url?: string;
  disabled?: boolean;
  updatable?: boolean;
  externalId?: string;
  project_id?: number;
  version_id?: number;
  project_homeNodeName?: string;
  [key: string]: unknown;
}

interface TestSectionsProps {
  initModal?: InitModal | null;
  projectCol: ColumnConfig;
  readOnly: boolean;
}

const TestSections: React.FC<TestSectionsProps> = props => {
  const { projectCol, readOnly } = props;
  const navigate = useNavigate();
  const T = useTranslations();
  const lo_platform = useLoPlatform();
  const dispatch = useDispatch();

  const [initModal, setInitModal] = useState<InitModal | null | undefined>(props.initModal);
  const [launchInfoRow, setLaunchInfoRow] = useState<TestSectionRow | null>(null);
  const [accessCodeLoaded, setAccessCodeLoaded] = useState(false);
  const [accessCode, setAccessCode] = useState<string | null>(null);
  const reactTable = useRef<ReactTableHandle | null>(null);
  const refreshTable = useRef<() => void>(() => undefined);

  const setPortalAlert = (error: boolean, success: boolean, message: string) =>
    dispatch(setPortalAlertStatus(error, success, message));

  const refresh = () => refreshTable.current();

  const formatCreateTime = (t?: string) => {
    const fmt = T.t('adminPage.testSections.momentFormat.createTime');
    return t ? moment(t).format(fmt) : '';
  };

  const formatRevision = (r?: string) => {
    return r ? T.t('adminPage.testSections.cell.projectRevision', { revision: r }) : '';
  };

  const generateColumns = (): ColumnConfig[] => {
    return [
      { dataField: 'id', isKey: true },
      { dataField: 'projectCode' },
      {
        dataField: 'project_name',
        sortable: false,
        searchable: false,
        required: true,
        width: '30%',
      },
      {
        dataField: 'name',
        sortable: true,
        searchable: true,
        required: true,
        searchOperator: 'ts',
        width: '30%',
      },
      { dataField: 'projectRevision', dataFormat: formatRevision },
      {
        dataField: 'createTime',
        sortable: true,
        defaultSort: 'desc',
        width: '10%',
        dataFormat: formatCreateTime,
      },
      projectCol,
      { dataField: 'projectProductType' },
    ];
  };

  // fake setPortalAlertStatus that renders in the modal error bar instead
  const setModalAlert = (_1: boolean, _2: boolean, msg: string) => reactTable.current?.onError(msg);

  const renderForm = (row: TestSectionRow, validationErrors: Record<string, string>) => {
    return (
      <EditAddForm
        translations={T}
        row={row}
        projectId={!row.id && initModal ? initModal.projectId : null}
        validationErrors={validationErrors}
        setPortalAlertStatus={setModalAlert}
        columns={generateColumns()}
        lo_platform={lo_platform}
      />
    );
  };

  const validateForm = (form: Record<string, any>, row: TestSectionRow, el: HTMLFormElement) =>
    EditAddForm.validateForm(form, row, el, T);

  const updateStatus = ({ id, disabled }: TestSectionRow) =>
    axios.put(`/api/v2/testSections/${id}/status`, { disabled: !disabled });

  const updateContent = (row: TestSectionRow) =>
    axios.post(`/api/v2/lwc/${row.id}/update`).then(() => {
      setPortalAlert(false, true, T.t('adminPage.testSections.syncedAlert', row));
      refresh();
    });

  const subPage =
    (page: string, entity?: string) =>
    ({ id }: TestSectionRow) => {
      if (entity) clearSavedTableState(entity);
      navigate(`/TestSections/${id}/${page}`);
      return Promise.resolve(false);
    };

  const launchInfo = (row: TestSectionRow) => {
    setLaunchInfoRow(row);
    setAccessCodeLoaded(false);
    axios
      .get(`/api/v2/lwc/${row.id}/accessCode`)
      .then(({ data: ac }) => {
        setAccessCodeLoaded(true);
        setAccessCode(ac);
      })
      .catch(resp => {
        console.log(resp.statusCode, resp);
      });
    return Promise.resolve(false);
  };

  const getButtonInfo = (selectedRows: TestSectionRow[]) => {
    const selectedRow = selectedRows.length === 1 && selectedRows[0];
    const rights = lo_platform.user.rights || [];
    const configAdmin = rights.includes('loi.cp.admin.right.ConfigurationAdminRight');
    const configurationsBtn = configAdmin
      ? [
          {
            name: 'configurations',
            iconName: 'settings',
            onClick: subPage('Configurations'),
          },
        ]
      : [];

    const changeStateButton = readOnly
      ? []
      : [
          {
            name: !selectedRow || !selectedRow.disabled ? 'suspend' : 'reinstate',
            iconName: !selectedRow || !selectedRow.disabled ? 'not_interested' : 'check',
            onClick: updateStatus,
          },
        ];

    const updateContentButton = readOnly
      ? []
      : [
          {
            name: 'sync',
            iconName: 'update',
            onClick: updateContent,
            disabled: !selectedRow || !selectedRow.updatable,
          },
        ];

    return [
      ...configurationsBtn,
      ...changeStateButton,
      ...updateContentButton,
      {
        name: 'enrollments',
        iconName: 'supervisor_account',
        onClick: subPage('Enrollments', 'enrollments'),
      },
      {
        name: 'launchInfo',
        iconName: 'settings_input_component',
        onClick: launchInfo,
      },
      ...getAuthorBtn(selectedRow, rights),
      {
        name: 'open',
        iconName: 'open_in_browser',
        href: selectedRow ? selectedRow.url : '/',
        target: '_top',
      },
    ];
  };

  const trClassFormat = ({ disabled }: any) => (disabled ? 'row-disabled' : '');

  const openSection = ({ url }: any) => {
    window.top!.location.href = url;
  };

  const headerExtra = (_row: TestSectionRow, modalType: string) => {
    return (
      modalType === 'create' && (
        <Fjœrich
          fjœr={true}
          setFjœr={() => null}
          className="rightly"
        />
      )
    );
  };

  const footerExtra = (_row: TestSectionRow, modalType: string) => {
    return (
      modalType === 'create' && (
        <div
          style={{ flex: 1, paddingLeft: '2rem' }}
          className="form-check"
        >
          <Input
            id="testSections-roster"
            type="checkbox"
            name="roster"
          />
          <Label
            check
            id="testSections-roster-label"
            for="testSections-roster"
          >
            {T.t('adminPage.testSections.fieldName.roster')}
          </Label>
        </div>
      )
    );
  };

  const onDismissModal = () => setInitModal(null);

  const afterCreateOrUpdate = (
    res: { data: { id: number } },
    extras: { roster?: boolean; configuration?: any }
  ) => {
    const configurePromise = extras.configuration
      ? putConfig('coursePreferences', res.data.id, extras.configuration)
      : Promise.resolve();
    return configurePromise.then(() => {
      if (extras.roster) {
        navigate({ search: '' }, { replace: true }); // so back doesn't reopen
        navigate(`/TestSections/${res.data.id}/Enrollments`);
        return false;
      } else {
        onDismissModal();
        return res;
      }
    });
  };

  const baseUrl = () => {
    const { hostName } = lo_platform.domain;
    return `https://${hostName}/`;
  };

  const generateAccessCode = ({ id }: TestSectionRow) =>
    axios.post(`/api/v2/lwc/${id}/accessCode`).then(({ data: ac }) => setAccessCode(ac));

  const renderLaunchInfo = () => {
    const row = launchInfoRow;
    if (!row) return null;

    const launchUrl = `${baseUrl()}lwlti/testSection/${row.externalId}`;
    const close = () => setLaunchInfoRow(null);
    const selectAll = (e: React.MouseEvent<HTMLInputElement>) =>
      (e.target as HTMLInputElement).select();

    return (
      <Modal
        id="launchInfo-modal"
        isOpen={true}
        size="lg"
        toggle={close}
        className="crudTable-modal ltiLaunchModal"
      >
        <ModalHeader tag="h2">{T.t('adminPage.testSections.launchInfo.title')}</ModalHeader>
        <ModalBody>
          <FormGroup row>
            <Label
              lg={3}
              for="launchInfo-launchUrl"
            >
              {T.t('adminPage.testSections.launchInfo.label.launchUrl')}
            </Label>
            <Col lg={9}>
              <Input
                id="launchInfo-launchUrl"
                readOnly
                value={launchUrl}
                onClick={selectAll}
              />
            </Col>
          </FormGroup>
          <FormGroup row>
            <Label
              lg={3}
              for="launchInfo-accessCode"
            >
              {T.t('adminPage.testSections.launchInfo.label.accessCode')}
            </Label>
            <Col lg={9}>
              {!accessCodeLoaded ? (
                <Input
                  key="ac-loading"
                  disabled
                />
              ) : !accessCode ? (
                <Button
                  id="launchInfo-accessCode-generate"
                  block
                  onClick={() => generateAccessCode(row)}
                >
                  {T.t('adminPage.testSections.launchInfo.button.generate')}
                </Button>
              ) : (
                <Input
                  key="ac-loaded"
                  id="launchInfo-accessCode"
                  readOnly
                  value={accessCode}
                  onClick={selectAll}
                />
              )}
            </Col>
          </FormGroup>
        </ModalBody>
        <ModalFooter>
          <Button
            id="launchInfo-close-modal-btn"
            onClick={close}
          >
            {T.t('crudTable.modal.closeButton')}
          </Button>
        </ModalFooter>
      </Modal>
    );
  };

  const customFilters = [
    {
      property: 'archived',
      operator: 'eq',
      value: 'false',
      prefilter: true,
    },
  ];
  return (
    <React.Fragment>
      {renderLaunchInfo()}
      <ReactTable
        entity="testSections"
        autoComplete="off"
        columns={generateColumns()}
        defaultSortField="createTime"
        defaultSearchField="name"
        defaultSortOrder="desc"
        renderForm={renderForm}
        validateForm={validateForm}
        translations={T}
        setPortalAlertStatus={setPortalAlert}
        getButtons={getButtonInfo}
        trClassFormat={trClassFormat}
        openRow={openSection}
        headerExtra={headerExtra}
        footerExtra={footerExtra}
        onDismissModal={onDismissModal}
        initModal={!!initModal}
        multiSelect={true}
        multiDelete={true}
        afterCreateOrUpdate={afterCreateOrUpdate}
        customFilters={customFilters}
        ref={reactTable}
        refreshRef={r => (refreshTable.current = r)}
        createButton={!readOnly}
        updateButton={!readOnly}
        deleteButton={!readOnly}
      />
    </React.Fragment>
  );
};

export default withProjectFilter(TestSections, 'testSections');
