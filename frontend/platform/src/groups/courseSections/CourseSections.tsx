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
import { useNavigate } from 'react-router-dom';
import { Dropdown, DropdownItem, DropdownMenu, DropdownToggle, Input, Label } from 'reactstrap';

import useRouteBasePath from '../../components/useRouteBasePath';

import ReactTable, {
  clearSavedTableState,
  ReactTableHandle,
} from '../../components/reactTable/ReactTable';
import { withProjectFilter } from '../../components/withProjectFilter';
import { setPortalAlertStatus } from '../../redux/actions/MainActions';
import { useLoPlatform, useTranslations } from '../../redux/state';
import { ConnectorNamesUrl, SubtenantNamesUrl } from '../../services/URLs';
import Fjœrich from '../Fjoerich';
import getAuthorBtn from '../services/authorBtn';
import EditAddForm from './EditAddForm';

interface NamedEntity {
  id: number;
  name: string;
}

interface CourseRow {
  id: number;
  url?: string;
  disabled?: boolean;
  fjœr?: boolean;
  subtenant_id?: number;
  integrations: { uniqueId: string }[];
  name?: string;
  project_id?: number;
  version_id?: number;
  project_homeNodeName?: string;
  [key: string]: unknown;
}

interface ColumnConfig {
  dataField: string;
  [key: string]: unknown;
}

interface Filter {
  property: string;
  operator?: string;
  value: any;
  prefilter?: boolean;
}

interface CourseSectionsProps {
  projectCol: ColumnConfig;
  customFilters?: Filter[];
  user?: number;
  readOnly: boolean;
}

const uniqueIdentifierFields = ['groupId', 'externalId', 'uniqueId'];

const CourseSections: React.FC<CourseSectionsProps> = props => {
  const { projectCol, customFilters, user, readOnly } = props;
  const navigate = useNavigate();
  const matchUrl = useRouteBasePath();
  const T = useTranslations();
  const lo_platform = useLoPlatform();
  const dispatch = useDispatch();

  const [externalSystems, setExternalSystems] = useState<NamedEntity[]>([]);
  const [subtenantsMap, setSubtenantsMap] = useState<Record<number, NamedEntity>>({});
  const [subtenants, setSubtenants] = useState<NamedEntity[]>([]);
  const [loaded, setLoaded] = useState(false);
  const [uniqueMenuOpen, setUniqueMenuOpen] = useState(false);
  const [uniqueIdentifier, setUniqueIdentifier] = useState('groupId');
  const reactTable = useRef<ReactTableHandle | null>(null);

  const setPortalAlert = (error: boolean, success: boolean, message: string) =>
    dispatch(setPortalAlertStatus(error, success, message));

  const formatUniqueId = (_u: unknown, row: CourseRow) =>
    row.integrations.map(integration => integration.uniqueId).join(', ');

  const uniqueIdentifierCols: ColumnConfig[] = [
    {
      dataField: 'groupId',
      sortable: true,
      searchable: true,
      required: true,
      searchOperator: 'sw',
    },
    {
      dataField: 'externalId',
      sortable: true,
      searchable: true,
      required: true,
      searchOperator: 'sw',
    },
    {
      dataField: 'uniqueId',
      searchable: true,
      required: true,
      searchOperator: 'sw',
      dataFormat: formatUniqueId,
    },
  ];

  useEffect(() => {
    const fetchen = [ConnectorNamesUrl, SubtenantNamesUrl].map(url => axios.get(url));
    axios
      .all(fetchen)
      .then(
        axios.spread((externalSystemsRes, subtenantsRes) => {
          setLoaded(true);
          setExternalSystems(externalSystemsRes.data.objects);
          setSubtenants(subtenantsRes.data.objects);
          setSubtenantsMap(
            subtenantsRes.data.objects.reduce(
              (o: Record<number, NamedEntity>, sub: NamedEntity) => ({ ...o, [sub.id]: sub }),
              {}
            )
          );
        })
      )
      .catch(e => {
        console.log(e);
        setPortalAlert(true, false, T.t('error.unexpectedError'));
      });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const formatCreateTime = (t?: string) => {
    const fmt = T.t('adminPage.courseSections.momentFormat.createTime');
    return t ? moment(t).format(fmt) : '';
  };

  const formatSubtenant = (_s: unknown, row: CourseRow) =>
    row.subtenant_id ? subtenantsMap[row.subtenant_id].name : '';

  const formatName = (_: unknown, row: CourseRow) => {
    return row.name;
  };

  const subtenantColPresent = () => {
    return subtenants.length && !lo_platform.user.subtenant_id;
  };

  const getSubtenantCol = () => {
    const subtenantFilterOptions = subtenants.map(sub => {
      return (
        <option
          key={sub.id}
          value={sub.id}
        >
          {sub.name}
        </option>
      );
    });
    const subtenantCol = {
      dataField: 'subtenant',
      dataFormat: formatSubtenant,
      filterOptions: subtenantFilterOptions,
      baseFilter: 'Any Subtenant',
      filterProperty: 'subtenant_id',
      filterable: true,
    };
    return subtenantColPresent() ? [subtenantCol] : [];
  };

  const onSearchFieldChange = (field: string) => {
    if (uniqueIdentifierFields.indexOf(field) !== -1) {
      setUniqueIdentifier(field);
    }
  };

  const renderUniqueIdCol = (col: ColumnConfig) => {
    const toggleMenu = (e: React.MouseEvent) => {
      e.stopPropagation();
      setUniqueMenuOpen(open => !open);
    };
    const id = `identifier-${col.dataField}`;
    return (
      uniqueIdentifier === col.dataField && (
        <Dropdown
          id={id}
          isOpen={uniqueMenuOpen}
          direction="down"
          style={{ float: 'left' }}
          toggle={toggleMenu}
        >
          <DropdownToggle
            id={`toggle-${id}`}
            tag="span"
          >
            <i
              className="material-icons md-18 col-header-menu"
              aria-hidden="true"
            >
              menu
            </i>
          </DropdownToggle>
          <DropdownMenu id={`menu-${id}`}>
            {uniqueIdentifierFields.map(field => (
              <DropdownItem
                key={field}
                onClick={() => onSearchFieldChange(field)}
              >
                {T.t(`adminPage.courseSections.fieldName.${field}`)}
              </DropdownItem>
            ))}
          </DropdownMenu>
        </Dropdown>
      )
    );
  };

  const formatProject = (name?: string) => {
    return name || T.t('adminPage.courseSections.projectName.noProject');
  };

  const formatRevision = (r?: string) => {
    return r ? T.t('adminPage.courseSections.cell.projectRevision', { revision: r }) : '';
  };

  const generateColumns = (): ColumnConfig[] => {
    return [
      { dataField: 'id', isKey: true },
      { dataField: 'projectCode' },
      {
        dataField: 'project_name',
        sortable: false,
        searchable: false,
        dataFormat: formatProject,
      },
      {
        dataField: 'name',
        sortable: true,
        searchable: true,
        required: true,
        searchOperator: 'ts',
        dataFormat: formatName,
      },
      { dataField: 'projectRevision', dataFormat: formatRevision },
      {
        dataField: 'createTime',
        sortable: true,
        defaultSort: 'desc',
        width: '12%',
        dataFormat: formatCreateTime,
        thStyle: { paddingLeft: '1.7rem' },
      },
      projectCol,
      { dataField: 'projectProductType' },
      ...uniqueIdentifierCols.map(col => ({
        ...col,
        prepend: renderUniqueIdCol(col),
        hidden: uniqueIdentifier !== col.dataField,
        thStyle: { overflow: 'visible' },
      })),
      ...getSubtenantCol(),
    ];
  };

  // fake setPortalAlertStatus that renders in the modal error bar instead
  const setModalAlert = (_1: boolean, _2: boolean, msg: string) => reactTable.current?.onError(msg);

  const renderForm = (row: CourseRow, validationErrors: Record<string, string>) => {
    return (
      <EditAddForm
        translations={T}
        row={row}
        setPortalAlertStatus={setModalAlert}
        validationErrors={validationErrors}
        subtenants={subtenants}
        externalSystems={externalSystems}
        fjœr={true}
        columns={generateColumns()}
        lo_platform={lo_platform}
      />
    );
  };

  const validateForm = (form: Record<string, any>, row: CourseRow, el: HTMLFormElement) =>
    EditAddForm.validateForm(form, row, el, T);

  const updateStatus = ({ id, disabled }: CourseRow) =>
    axios.put(`/api/v2/courseSections/${id}/status`, { disabled: !disabled });

  const subPage =
    (page: string, entity?: string) =>
    ({ id }: CourseRow) => {
      if (entity) clearSavedTableState(entity);
      navigate(`${matchUrl}/${id}/${page}`);
      return Promise.resolve(false);
    };

  const unenrolUser = (course: CourseRow) => {
    axios
      .delete(`/api/v2/courses/${course.id}/enrollments/byUser/${user}`)
      .then(() => reactTable.current?.refresh());
  };

  const getButtonInfo = (selectedRows: CourseRow[]) => {
    const selectedRow = selectedRows.length === 1 && selectedRows[0];
    const rights = lo_platform.user.rights || [];
    const isUserCourses = !!user;
    const configAdmin = rights.includes('loi.cp.admin.right.ConfigurationAdminRight');
    const configurationsBtn =
      configAdmin && !isUserCourses
        ? [
            {
              name: 'configurations',
              iconName: 'settings',
              onClick: subPage('Configurations'),
              disabled: !selectedRow || !selectedRow.fjœr,
            },
          ]
        : [];
    const announceAdmin = rights.includes('loi.cp.announcement.AnnouncementAdminRight');
    const announcementsButton =
      announceAdmin && !isUserCourses
        ? [
            {
              name: 'announcements',
              iconName: 'announcement',
              onClick: subPage('Announcements'),
              disabled: !selectedRow || !selectedRow.fjœr,
            },
          ]
        : [];

    const changeStateButton =
      !isUserCourses && !readOnly
        ? [
            {
              name: !selectedRow || !selectedRow.disabled ? 'suspend' : 'reinstate',
              iconName: !selectedRow || !selectedRow.disabled ? 'not_interested' : 'check',
              onClick: updateStatus,
              disabled: !selectedRow || !selectedRow.fjœr,
            },
          ]
        : [];

    const enrollmentsButton = !isUserCourses
      ? [
          {
            name: 'enrollments',
            iconName: 'supervisor_account',
            onClick: subPage('Enrollments', 'enrollments'),
          },
        ]
      : [];

    return [
      ...configurationsBtn,
      ...announcementsButton,
      ...changeStateButton,
      ...enrollmentsButton,
      ...getAuthorBtn(selectedRow, rights),
      {
        name: 'open',
        iconName: 'remove_red_eye',
        href: selectedRow ? selectedRow.url : null,
        disabled: !selectedRow || !selectedRow.fjœr,
        target: '_top',
      },
      ...(!isUserCourses
        ? []
        : [
            {
              name: 'unenrol',
              iconName: 'eject',
              onClick: () => selectedRow && unenrolUser(selectedRow),
              disabled: !selectedRow,
              color: 'warning',
              lastButton: true,
            },
          ]),
    ];
  };

  const trClassFormat = ({ disabled }: any) => (disabled ? 'row-disabled' : '');

  const openSection = ({ url }: any) => {
    window.top!.location.href = url;
  };

  const headerExtra = (_row: CourseRow, modalType: string) => {
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

  const footerExtra = (_row: CourseRow, modalType: string) => {
    return (
      modalType === 'create' && (
        <div
          style={{ flex: 1, paddingLeft: '2rem' }}
          className="form-check"
        >
          <Input
            id="courseSections-roster"
            type="checkbox"
            name="roster"
          />
          <Label
            check
            id="courseSections-roster-label"
            for="courseSections-roster"
          >
            {T.t('adminPage.courseSections.fieldName.roster')}
          </Label>
        </div>
      )
    );
  };

  const afterCreateOrUpdate = (res: { data: { id: number } }, extras: { roster?: boolean }) => {
    if (extras.roster) {
      navigate(`/CourseSections/${res.data.id}/Enrollments`);
      return false;
    } else {
      return res;
    }
  };

  const crudButtons = !user && !readOnly;
  return !loaded ? (
    <div />
  ) : (
    <ReactTable
      entity="courseSections"
      autoComplete="off"
      ref={reactTable}
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
      canUpdateRow={({ fjœr }: any) => !!fjœr}
      multiSelect={true}
      multiDelete={true}
      onSearchFieldChange={onSearchFieldChange}
      customFilters={customFilters}
      createButton={crudButtons}
      updateButton={crudButtons}
      deleteButton={crudButtons}
      afterCreateOrUpdate={afterCreateOrUpdate}
      filterWidth={subtenantColPresent() ? 3 : 3}
      searchBarWidth={subtenantColPresent() ? 8 : 6}
    />
  );
};

export default withProjectFilter(CourseSections, 'courseSections');
