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
import React, { useState } from 'react';
import { useDispatch } from 'react-redux';
import { useNavigate } from 'react-router-dom';
import { Button, Form, Input } from 'reactstrap';

import ReactTable from '../components/reactTable/ReactTable';
import { withProjectFilter } from '../components/withProjectFilter';
import { setPortalAlertStatus } from '../redux/actions/MainActions';
import { useLoPlatform, useTranslations } from '../redux/state';
import LtiLaunchInfo from './LwcLtiInfo';
import ScormPackage from './ScormPackage';

interface OfferingRow {
  id: number;
  disabled?: boolean;
  groupId: string;
  version_id?: number;
  asset_guid?: string;
  [key: string]: unknown;
}

type Filter = { property: string; operator: string; value: unknown; prefilter?: boolean };

type TogglePopover = (name: string, show: boolean, immediate?: boolean) => void;

interface CourseOfferingsProps {
  projectCol: { dataField: string; [key: string]: unknown };
  customFilters?: Filter[] | null;
  autoSelect?: boolean;
}

const CourseOfferings: React.FC<CourseOfferingsProps> = ({
  projectCol,
  customFilters,
  autoSelect,
}) => {
  const navigate = useNavigate();
  const T = useTranslations();
  const lo_platform = useLoPlatform();
  const dispatch = useDispatch();

  const [ltiLaunchInfoRow, setLtiLaunchInfoRow] = useState<OfferingRow | null>(null);
  const [scormPackageRow, setScormPackageRow] = useState<OfferingRow | null>(null);
  const [scormBatch, setScormBatch] = useState(false);

  const setPortalAlert = (error: boolean, success: boolean, message: string) =>
    dispatch(setPortalAlertStatus(error, success, message));

  const formatCreateTime = (t: string) => {
    const fmt = T.t('adminPage.lwc/courseOfferings.momentFormat.createTime');
    return t ? moment(t).format(fmt) : '';
  };

  const formatRevision = (r: unknown) => {
    return r ? T.t('adminPage.lwc/CourseOfferings.cell.projectRevision', { revision: r }) : '';
  };

  const downloadLtiLaunchInfo = (selectedRows: OfferingRow[], togglePopover: TogglePopover) => {
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

  const otheringAufforing = (selectedRow: OfferingRow | false, togglePopover: TogglePopover) => {
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

  const previewOffering = (selectedRow: OfferingRow | false, togglePopover: TogglePopover) => {
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
          value={(selectedRow && selectedRow.version_id) || ''}
        />
        <Input
          type="hidden"
          name="courseName"
          value={(selectedRow && selectedRow.asset_guid) || ''}
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

  const trClassFormat = ({ disabled }: { id: number; disabled?: boolean }) =>
    disabled ? 'row-disabled' : '';

  const updateStatus = ({ id, disabled }: OfferingRow) =>
    axios.put(`/api/v2/lwc/courseOfferings/${id}/status`, { disabled: !disabled });

  const ltiLaunchInfo = (row: OfferingRow | null) => {
    setLtiLaunchInfoRow(row);
    return Promise.resolve(false);
  };

  const scormPackage = (row: OfferingRow | null) => {
    setScormPackageRow(row);
    return Promise.resolve(false);
  };

  const triggerScormBatch = () => {
    setScormBatch(true);
    return Promise.resolve(false);
  };

  const subPage =
    (page: string) =>
    ({ id }: OfferingRow) => {
      navigate(`/CourseOfferings/${id}/${page}`);
      return Promise.resolve(false);
    };

  const getButtonInfo = (selectedRows: OfferingRow[], togglePopover: TogglePopover) => {
    const selectedRow = selectedRows.length === 1 && selectedRows[0];
    const rights = lo_platform.user.rights ?? [];
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
    const announceAdmin = rights.includes('loi.cp.announcement.AnnouncementAdminRight');
    const announcementsButton = announceAdmin
      ? [
          {
            name: 'announcements',
            iconName: 'announcement',
            onClick: subPage('Announcements'),
          },
        ]
      : [];
    return [
      ...configurationsBtn,
      ...announcementsButton,
      {
        name: !selectedRow || !selectedRow.disabled ? 'suspend' : 'reinstate',
        iconName: !selectedRow || !selectedRow.disabled ? 'not_interested' : 'check',
        onClick: updateStatus,
      },
      {
        name: 'ltiLaunch',
        iconName: 'settings_input_component',
        onClick: ltiLaunchInfo,
      },
      downloadLtiLaunchInfo(selectedRows, togglePopover),
      {
        name: 'scormPackage',
        iconName: 'category',
        onClick: scormPackage,
      },
      otheringAufforing(selectedRow, togglePopover),
      previewOffering(selectedRow, togglePopover),
      {
        name: 'scormBatch',
        iconName: 'save',
        onClick: triggerScormBatch,
        className: 'ms-3',
        alwaysEnabled: true,
        solo: true,
      },
    ];
  };

  const renderLaunchInfo = () => {
    return (
      ltiLaunchInfoRow && (
        <LtiLaunchInfo
          row={ltiLaunchInfoRow}
          T={T}
          setPortalAlertStatus={setPortalAlert}
          lo_platform={lo_platform}
          close={() => ltiLaunchInfo(null)}
        />
      )
    );
  };

  const renderScormPackage = () => {
    return scormPackageRow ? (
      <ScormPackage
        multi={false}
        row={scormPackageRow}
        T={T}
        setPortalAlertStatus={setPortalAlert}
        lo_platform={lo_platform}
        close={() => scormPackage(null)}
      />
    ) : scormBatch ? (
      <ScormPackage
        multi={true}
        T={T}
        setPortalAlertStatus={setPortalAlert}
        lo_platform={lo_platform}
        close={() => setScormBatch(false)}
      />
    ) : null;
  };

  const generateColumns = () => {
    return [
      { dataField: 'id', isKey: true },
      { dataField: 'projectCode', width: '6.25%' },
      { dataField: 'project_name', sortable: false, searchable: false },
      { dataField: 'name', hidden: true, sortable: true, searchable: true, searchOperator: 'ts' },
      { dataField: 'projectRevision', dataFormat: formatRevision, width: '9%' },
      {
        dataField: 'createTime',
        sortable: true,
        defaultSort: 'desc',
        searchable: false,
        dataFormat: formatCreateTime,
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

  return (
    <React.Fragment>
      <ReactTable
        entity="lwc/courseOfferings"
        columns={generateColumns()}
        getButtons={getButtonInfo}
        defaultSortField="createTime"
        defaultSortOrder="desc"
        defaultSearchField="name"
        translations={T}
        createButton={false}
        updateButton={false}
        multiSelect={true}
        trClassFormat={trClassFormat}
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
      {renderLaunchInfo()}
      {renderScormPackage()}
    </React.Fragment>
  );
};

export default withProjectFilter(CourseOfferings);
