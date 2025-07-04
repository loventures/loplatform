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
import PropTypes from 'prop-types';
import React from 'react';
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

import ReactTable, { clearSavedTableState } from '../../components/reactTable/ReactTable';
import { withProjectFilter } from '../../components/withProjectFilter';
import { putConfig } from '../../config/configApi';
import Fjœrich from '../Fjoerich';
import getAuthorBtn from '../services/authorBtn';
import EditAddForm from './EditAddForm';

class TestSections extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      initModal: this.props.initModal,
      launchInfo: null,
      accessCodeLoaded: false,
      accessCode: null,
    };
  }

  formatCreateTime = t => {
    const fmt = this.props.translations.t('adminPage.testSections.momentFormat.createTime');
    return t ? moment(t).format(fmt) : '';
  };

  formatRevision = r => {
    const { translations: T } = this.props;
    return r ? T.t('adminPage.testSections.cell.projectRevision', { revision: r }) : '';
  };

  generateColumns = () => {
    const { projectCol } = this.props;
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
      { dataField: 'projectRevision', dataFormat: this.formatRevision },
      {
        dataField: 'createTime',
        sortable: true,
        defaultSort: 'desc',
        width: '10%',
        dataFormat: this.formatCreateTime,
      },
      projectCol,
      { dataField: 'projectProductType' },
    ];
  };

  // fake setPortalAlertStatus that renders in the modal error bar instead
  setModalAlert = (_1, _2, msg) => this.reactTable.onError(msg);

  refresh = () => this.refreshTable();

  renderForm = (row, validationErrors) => {
    const { initModal } = this.state;
    return (
      <EditAddForm
        translations={this.props.translations}
        row={row}
        projectId={!row.id && initModal ? initModal.projectId : null}
        validationErrors={validationErrors}
        setPortalAlertStatus={this.setModalAlert}
        columns={this.generateColumns()}
        lo_platform={this.props.lo_platform}
      />
    );
  };

  validateForm = (form, row, el) =>
    EditAddForm.validateForm(form, row, el, this.props.translations);

  updateStatus = ({ id, disabled }) =>
    axios.put(`/api/v2/testSections/${id}/status`, { disabled: !disabled });

  updateContent = row =>
    axios.post(`/api/v2/lwc/${row.id}/update`).then(() => {
      const { setPortalAlertStatus, translations: T } = this.props;
      setPortalAlertStatus(false, true, T.t('adminPage.testSections.syncedAlert', row));
      this.refresh();
    });

  subPage =
    (page, entity) =>
    ({ id }) => {
      if (entity) clearSavedTableState(entity);
      this.props.history.push(`/TestSections/${id}/${page}`);
      return Promise.resolve(false);
    };

  launchInfo = row => {
    this.setState({ launchInfo: row, accessCodeLoaded: false });
    axios
      .get(`/api/v2/lwc/${row.id}/accessCode`)
      .then(({ data: accessCode }) => {
        this.setState({ accessCodeLoaded: true, accessCode });
      })
      .catch(resp => {
        console.log(resp.statusCode, resp);
      });
    return Promise.resolve(false);
  };

  getButtonInfo = selectedRows => {
    const selectedRow = selectedRows.length === 1 && selectedRows[0];
    const {
      lo_platform: {
        user: { rights },
      },
      readOnly,
    } = this.props;
    const configAdmin = rights.includes('loi.cp.admin.right.ConfigurationAdminRight');
    const configurationsBtn = configAdmin
      ? [
          {
            name: 'configurations',
            iconName: 'settings',
            onClick: this.subPage('Configurations'),
          },
        ]
      : [];

    const changeStateButton = readOnly
      ? []
      : [
          {
            name: !selectedRow || !selectedRow.disabled ? 'suspend' : 'reinstate',
            iconName: !selectedRow || !selectedRow.disabled ? 'not_interested' : 'check',
            onClick: this.updateStatus,
          },
        ];

    const updateContentButton = readOnly
      ? []
      : [
          {
            name: 'sync',
            iconName: 'update',
            onClick: this.updateContent,
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
        onClick: this.subPage('Enrollments', 'enrollments'),
      },
      {
        name: 'launchInfo',
        iconName: 'settings_input_component',
        onClick: this.launchInfo,
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

  trClassFormat = ({ disabled }) => (disabled ? 'row-disabled' : '');

  openSection = ({ url }) => {
    window.top.location.href = url;
    return false;
  };

  headerExtra = (row, modalType) => {
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

  footerExtra = (row, modalType) => {
    const { translations: T } = this.props;
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

  onDismissModal = () => this.setState({ initModal: null });

  afterCreateOrUpdate = (res, extras) => {
    const configurePromise = extras.configuration
      ? putConfig('coursePreferences', res.data.id, extras.configuration)
      : Promise.resolve();
    return configurePromise.then(() => {
      if (extras.roster) {
        this.props.history.replace({ search: '' }); // so back doesn't reopen
        this.props.history.push(`/TestSections/${res.data.id}/Enrollments`);
        return false;
      } else {
        this.onDismissModal();
        return res;
      }
    });
  };

  baseUrl = () => {
    const {
      lo_platform: {
        domain: { hostName },
      },
    } = this.props;
    return `https://${hostName}/`;
  };

  generateAccessCode = ({ id }) =>
    axios
      .post(`/api/v2/lwc/${id}/accessCode`)
      .then(({ data: accessCode }) => this.setState({ accessCode }));

  renderLaunchInfo = () => {
    const {
      props: { translations: T },
      state: { launchInfo: row, accessCode, accessCodeLoaded },
    } = this;
    if (!row) return null;

    const launchUrl = `${this.baseUrl()}lwlti/testSection/${row.externalId}`;
    const close = () => this.setState({ launchInfo: null });
    const selectAll = e => e.target.select();

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
                  onClick={() => this.generateAccessCode(row)}
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

  render() {
    const { translations, setPortalAlertStatus, readOnly } = this.props;
    const { initModal } = this.state;
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
        {this.renderLaunchInfo()}
        <ReactTable
          entity="testSections"
          autoComplete="off"
          columns={this.generateColumns()}
          defaultSortField="createTime"
          defaultSearchField="name"
          defaultSortOrder="desc"
          renderForm={this.renderForm}
          validateForm={this.validateForm}
          translations={translations}
          setPortalAlertStatus={setPortalAlertStatus}
          getButtons={this.getButtonInfo}
          trClassFormat={this.trClassFormat}
          openRow={this.openSection}
          headerExtra={this.headerExtra}
          footerExtra={this.footerExtra}
          onDismissModal={this.onDismissModal}
          initModal={!!initModal}
          multiSelect={true}
          multiDelete={true}
          afterCreateOrUpdate={this.afterCreateOrUpdate}
          customFilters={customFilters}
          ref={r => (this.reactTable = r)}
          refreshRef={r => (this.refreshTable = r)}
          createButton={!readOnly}
          updateButton={!readOnly}
          deleteButton={!readOnly}
        />
      </React.Fragment>
    );
  }
}

TestSections.propTypes = {
  translations: PropTypes.object.isRequired,
  lo_platform: PropTypes.object.isRequired,
  setPortalAlertStatus: PropTypes.func.isRequired,
  history: PropTypes.object.isRequired,
  readOnly: PropTypes.bool.isRequired,
  initModal: PropTypes.object,
};

export default withProjectFilter(TestSections, 'testSections');
