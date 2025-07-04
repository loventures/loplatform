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
import { Button, Form, Input } from 'reactstrap';

import ReactTable from '../components/reactTable/ReactTable';
import { withProjectFilter } from '../components/withProjectFilter';
import LtiLaunchInfo from './LwcLtiInfo';
import ScormPackage from './ScormPackage';

class CourseOfferings extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      ltiLaunchInfo: null,
      scormPackage: null,
      scormBatch: false,
    };
  }

  genericError = e => {
    const { setPortalAlertStatus, translations: T } = this.props;
    console.log(e);
    setPortalAlertStatus(true, false, T.t('error.unexpectedError'));
  };

  formatCreateTime = t => {
    const { translations: T } = this.props;
    const fmt = T.t('adminPage.lwc/courseOfferings.momentFormat.createTime');
    return t ? moment(t).format(fmt) : '';
  };

  formatRevision = r => {
    const { translations: T } = this.props;
    return r ? T.t('adminPage.lwc/CourseOfferings.cell.projectRevision', { revision: r }) : '';
  };

  downloadLtiLaunchInfo = (selectedRows, togglePopover) => {
    const { translations: T } = this.props;
    const name = 'downloadLtiLaunchInfo';
    const ids = selectedRows.map(row => `id=${row.id}`).join('&');
    const href = ids ? `/api/v2/lwc/courseOfferings/ltiLaunchInfo?${ids}` : '';
    return (
      <Button
        key={`courseOfferings-${name}`}
        id={`react-table-${name}-button`}
        onMouseOver={() => togglePopover(name, true, true)}
        tag="a"
        href={href}
        onMouseOut={() => togglePopover(name, false)}
        className="glyphButton"
        disabled={!selectedRows.length}
        aria-label={T.t(`adminPage.lwc/courseOfferings.toolBar.${name}Button`)}
        download
      >
        <i
          className="material-icons md-18"
          aria-hidden="true"
        >
          file_download
        </i>
      </Button>
    );
  };

  otheringAufforing = (selectedRow, togglePopover) => {
    const { translations: T } = this.props;
    const name = 'authoring';
    const href = selectedRow
      ? `/Authoring/branch/${selectedRow.version_id}/launch/${selectedRow.asset_guid}`
      : '';
    return (
      <Button
        key={`courseOfferings-${name}`}
        id={`react-table-${name}-button`}
        onMouseOver={() => togglePopover(name, true, true)}
        tag="a"
        href={href}
        onMouseOut={() => togglePopover(name, false)}
        className="glyphButton"
        disabled={!selectedRow}
        aria-label={T.t(`adminPage.lwc/courseOfferings.toolBar.${name}Button`)}
      >
        <i
          className="material-icons md-18"
          aria-hidden="true"
        >
          eco
        </i>
      </Button>
    );
  };

  previewOffering = (selectedRow, togglePopover) => {
    const { translations: T } = this.props;
    const name = 'preview';
    return (
      <Form
        key={`courseOfferings-${name}`}
        method="POST"
        action="/api/v2/lwc/preview"
        className="mb-0"
      >
        <Input
          type="hidden"
          name="branch"
          value={selectedRow.version_id || ''}
        />
        <Input
          type="hidden"
          name="courseName"
          value={selectedRow.asset_guid || ''}
        />
        <Input
          type="hidden"
          name="edgeNames"
          value=""
        />
        <Input
          type="hidden"
          name="feedback"
          value="false"
        />
        <Button
          id={`react-table-${name}-button`}
          type="submit"
          onMouseOver={() => togglePopover(name, true, true)}
          onMouseOut={() => togglePopover(name, false)}
          style={{ borderTopLeftRadius: '0', borderBottomLeftRadius: '0', marginLeft: '-1px' }}
          className="glyphButton lastButton"
          disabled={!selectedRow}
          aria-label={T.t(`adminPage.lwc/courseOfferings.toolBar.${name}Button`)}
        >
          <i className="material-icons md-18">visibility</i>
        </Button>
      </Form>
    );
  };

  trClassFormat = ({ disabled }) => (disabled ? 'row-disabled' : '');

  updateStatus = ({ id, disabled }) =>
    axios.put(`/api/v2/lwc/courseOfferings/${id}/status`, { disabled: !disabled });

  ltiLaunchInfo = row => {
    this.setState({ ltiLaunchInfo: row });
    return Promise.resolve(false);
  };

  scormPackage = row => {
    this.setState({ scormPackage: row });
    return Promise.resolve(false);
  };

  scormBatch = () => {
    this.setState({ scormBatch: true });
    return Promise.resolve(false);
  };

  subPage =
    page =>
    ({ id }) => {
      this.props.history.push(`/CourseOfferings/${id}/${page}`);
      return Promise.resolve(false);
    };

  getButtonInfo = (selectedRows, togglePopover) => {
    const selectedRow = selectedRows.length === 1 && selectedRows[0];
    const {
      lo_platform: {
        user: { rights },
      },
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
    const announceAdmin = rights.includes('loi.cp.announcement.AnnouncementAdminRight');
    const announcementsButton = announceAdmin
      ? [
          {
            name: 'announcements',
            iconName: 'announcement',
            onClick: this.subPage('Announcements'),
          },
        ]
      : [];
    return [
      ...configurationsBtn,
      ...announcementsButton,
      {
        name: !selectedRow || !selectedRow.disabled ? 'suspend' : 'reinstate',
        iconName: !selectedRow || !selectedRow.disabled ? 'not_interested' : 'check',
        onClick: this.updateStatus,
      },
      {
        name: 'ltiLaunch',
        iconName: 'settings_input_component',
        onClick: this.ltiLaunchInfo,
      },
      this.downloadLtiLaunchInfo(selectedRows, togglePopover),
      {
        name: 'scormPackage',
        iconName: 'category',
        onClick: this.scormPackage,
      },
      this.otheringAufforing(selectedRow, togglePopover),
      this.previewOffering(selectedRow, togglePopover),
      {
        name: 'scormBatch',
        iconName: 'save',
        onClick: this.scormBatch,
        className: 'ms-3',
        alwaysEnabled: true,
        solo: true,
      },
    ];
  };

  renderLaunchInfo = () => {
    const { translations: T, setPortalAlertStatus, lo_platform } = this.props;
    const { ltiLaunchInfo } = this.state;
    return (
      ltiLaunchInfo && (
        <LtiLaunchInfo
          row={ltiLaunchInfo}
          T={T}
          setPortalAlertStatus={setPortalAlertStatus}
          lo_platform={lo_platform}
          close={() => this.ltiLaunchInfo(null)}
        />
      )
    );
  };

  renderScormPackage = () => {
    const { translations: T, setPortalAlertStatus, lo_platform } = this.props;
    const { scormPackage, scormBatch } = this.state;
    return scormPackage ? (
      <ScormPackage
        multi={false}
        row={scormPackage}
        T={T}
        setPortalAlertStatus={setPortalAlertStatus}
        lo_platform={lo_platform}
        close={() => this.scormPackage(null)}
      />
    ) : scormBatch ? (
      <ScormPackage
        multi={true}
        T={T}
        setPortalAlertStatus={setPortalAlertStatus}
        lo_platform={lo_platform}
        close={() => this.setState({ scormBatch: false })}
      />
    ) : null;
  };

  generateColumns = () => {
    const { projectCol } = this.props;

    return [
      { dataField: 'id', isKey: true },
      { dataField: 'projectCode', width: '6.25%' },
      { dataField: 'project_name', sortable: false, searchable: false },
      { dataField: 'name', hidden: true, sortable: true, searchable: true, searchOperator: 'ts' },
      { dataField: 'projectRevision', dataFormat: this.formatRevision, width: '9%' },
      {
        dataField: 'createTime',
        sortable: true,
        defaultSort: 'desc',
        searchable: false,
        dataFormat: this.formatCreateTime,
        width: '9%',
      },
      projectCol,
      { dataField: 'projectProductType', width: '6.25%' },
      { dataField: 'projectCategory' },
      { dataField: 'projectSubCategory' },
      {
        dataField: 'groupId',
        sortable: false,
        searchable: true,
        searchOperator: 'sw',
        width: '360px',
      },
    ];
  };

  render() {
    const { translations, setPortalAlertStatus, customFilters, autoSelect } = this.props;
    return (
      <React.Fragment>
        <ReactTable
          entity="lwc/courseOfferings"
          columns={this.generateColumns()}
          getButtons={this.getButtonInfo}
          defaultSortField="createTime"
          defaultSortOrder="desc"
          defaultSearchField="name"
          translations={translations}
          setPortalAlertStatus={setPortalAlertStatus}
          createButton={false}
          updateButton={false}
          multiSelect={true}
          trClassFormat={this.trClassFormat}
          deleteButton={false}
          autoSelect={autoSelect}
          customFilters={[
            {
              property: 'archived',
              operator: 'eq',
              value: 'false',
              prefilter: true,
            },
            ...(customFilters || []),
          ]}
        />
        {this.renderLaunchInfo()}
        {this.renderScormPackage()}
      </React.Fragment>
    );
  }
}

CourseOfferings.propTypes = {
  translations: PropTypes.object.isRequired,
  lo_platform: PropTypes.object.isRequired,
  setPortalAlertStatus: PropTypes.func.isRequired,
  history: PropTypes.object.isRequired,
  projectCol: PropTypes.object.isRequired,
  customFilters: PropTypes.array,
  autoSelect: PropTypes.bool,
};

export default withProjectFilter(CourseOfferings);
