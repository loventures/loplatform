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
import { connect } from 'react-redux';
import { Dropdown, DropdownItem, DropdownMenu, DropdownToggle, Input, Label } from 'reactstrap';
import { bindActionCreators } from 'redux';

import ReactTable, { clearSavedTableState } from '../../components/reactTable/ReactTable';
import { withProjectFilter } from '../../components/withProjectFilter';
import * as MainActions from '../../redux/actions/MainActions';
import { ConnectorNamesUrl, SubtenantNamesUrl } from '../../services/URLs';
import Fjœrich from '../Fjoerich';
import getAuthorBtn from '../services/authorBtn';
import EditAddForm from './EditAddForm';

class CourseSections extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      externalSystems: [],
      subtenantsMap: {},
      subtenants: [],
      loaded: false,
      fjœr: true,
      uniqueMenuOpen: false,
      uniqueIdentifier: 'groupId',
    };
    this.uniqueIdentifierCols = [
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
        dataFormat: this.formatUniqueId,
      },
    ];
  }

  componentDidMount() {
    const fetchen = [ConnectorNamesUrl, SubtenantNamesUrl].map(axios.get);
    axios
      .all(fetchen)
      .then(
        axios.spread((externalSystems, subtenants) => {
          this.setState({
            loaded: true,
            externalSystems: externalSystems.data.objects,
            subtenants: subtenants.data.objects,
            subtenantsMap: subtenants.data.objects.reduce(
              (o, sub) => ({ ...o, [sub.id]: sub }),
              {}
            ),
          });
        })
      )
      .catch(e => {
        console.log(e);
        const { translations: T, setPortalAlertStatus } = this.props;
        setPortalAlertStatus(true, false, T.t('error.unexpectedError'));
      });
  }

  formatCreateTime = t => {
    const { translations: T } = this.props;
    const fmt = T.t('adminPage.courseSections.momentFormat.createTime');
    return t ? moment(t).format(fmt) : '';
  };

  formatSubtenant = (s, row) =>
    row.subtenant_id ? this.state.subtenantsMap[row.subtenant_id].name : '';

  formatUniqueId = (u, row) => row.integrations.map(integration => integration.uniqueId).join(', ');

  formatName = (_, row) => {
    return row.name;
  };

  subtenantColPresent = () => {
    const { subtenants } = this.state;
    const { lo_platform } = this.props;
    return subtenants.length && !lo_platform.user.subtenant_id;
  };

  getSubtenantCol = () => {
    const { subtenants } = this.state;
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
      dataFormat: this.formatSubtenant,
      filterOptions: subtenantFilterOptions,
      baseFilter: 'Any Subtenant',
      filterProperty: 'subtenant_id',
      filterable: true,
    };
    return this.subtenantColPresent() ? [subtenantCol] : [];
  };

  onSearchFieldChange = field => {
    const uniqueIdentifiers = ['groupId', 'externalId', 'uniqueId'];
    if (uniqueIdentifiers.indexOf(field) !== -1) {
      this.setState({ uniqueIdentifier: field });
    }
  };

  renderUniqueIdCol = col => {
    const { translations: T } = this.props;
    const { uniqueIdentifier, uniqueMenuOpen } = this.state;
    const toggleMenu = e => {
      e.stopPropagation();
      this.setState(({ uniqueMenuOpen }) => ({ uniqueMenuOpen: !uniqueMenuOpen }));
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
            {['groupId', 'externalId', 'uniqueId'].map(field => (
              <DropdownItem
                key={field}
                onClick={() => this.onSearchFieldChange(field)}
              >
                {T.t(`adminPage.courseSections.fieldName.${field}`)}
              </DropdownItem>
            ))}
          </DropdownMenu>
        </Dropdown>
      )
    );
  };

  formatProject = name => {
    const { translations: T } = this.props;
    return name || T.t('adminPage.courseSections.projectName.noProject');
  };

  formatRevision = r => {
    const { translations: T } = this.props;
    return r ? T.t('adminPage.courseSections.cell.projectRevision', { revision: r }) : '';
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
        dataFormat: this.formatProject,
      },
      {
        dataField: 'name',
        sortable: true,
        searchable: true,
        required: true,
        searchOperator: 'ts',
        dataFormat: this.formatName,
      },
      { dataField: 'projectRevision', dataFormat: this.formatRevision },
      {
        dataField: 'createTime',
        sortable: true,
        defaultSort: 'desc',
        width: '12%',
        dataFormat: this.formatCreateTime,
        thStyle: { paddingLeft: '1.7rem' },
      },
      projectCol,
      { dataField: 'projectProductType' },
      ...this.uniqueIdentifierCols.map(col => ({
        ...col,
        prepend: this.renderUniqueIdCol(col),
        hidden: this.state.uniqueIdentifier !== col.dataField,
        thStyle: { overflow: 'visible' },
      })),
      ...this.getSubtenantCol(),
    ];
  };

  // fake setPortalAlertStatus that renders in the modal error bar instead
  setModalAlert = (_1, _2, msg) => this.reactTable.onError(msg);

  renderForm = (row, validationErrors) => {
    return (
      <EditAddForm
        translations={this.props.translations}
        row={row}
        setPortalAlertStatus={this.setModalAlert}
        validationErrors={validationErrors}
        subtenants={this.state.subtenants}
        externalSystems={this.state.externalSystems}
        fjœr={this.state.fjœr || row.fjœr}
        columns={this.generateColumns()}
        lo_platform={this.props.lo_platform}
      />
    );
  };

  validateForm = (form, row, el) =>
    EditAddForm.validateForm(form, row, el, this.props.translations);

  updateStatus = ({ id, disabled }) =>
    axios.put(`/api/v2/courseSections/${id}/status`, { disabled: !disabled });

  subPage =
    (page, entity) =>
    ({ id }) => {
      const { match } = this.props;
      if (entity) clearSavedTableState(entity);
      this.props.history.push(`${match.url}/${id}/${page}`);
      return Promise.resolve(false);
    };

  unenrolUser = course => {
    const { user } = this.props;
    axios
      .delete(`/api/v2/courses/${course.id}/enrollments/byUser/${user}`)
      .then(() => this.reactTable.refresh());
  };

  getButtonInfo = selectedRows => {
    const selectedRow = selectedRows.length === 1 && selectedRows[0];
    const {
      lo_platform: {
        user: { rights },
      },
      user,
      readOnly,
    } = this.props;
    const isUserCourses = !!user;
    const configAdmin = rights.includes('loi.cp.admin.right.ConfigurationAdminRight');
    const configurationsBtn =
      configAdmin && !isUserCourses
        ? [
            {
              name: 'configurations',
              iconName: 'settings',
              onClick: this.subPage('Configurations'),
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
              onClick: this.subPage('Announcements'),
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
              onClick: this.updateStatus,
              disabled: !selectedRow || !selectedRow.fjœr,
            },
          ]
        : [];

    const enrollmentsButton = !isUserCourses
      ? [
          {
            name: 'enrollments',
            iconName: 'supervisor_account',
            onClick: this.subPage('Enrollments', 'enrollments'),
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
              onClick: () => this.unenrolUser(selectedRow),
              disabled: !selectedRow,
              color: 'warning',
              lastButton: true,
            },
          ]),
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

  afterCreateOrUpdate = (res, extras) => {
    if (extras.roster) {
      this.props.history.push(`/CourseSections/${res.data.id}/Enrollments`);
      return false;
    } else {
      return res;
    }
  };

  render() {
    const { translations: T, setPortalAlertStatus, customFilters, user, readOnly } = this.props;
    const { loaded } = this.state;
    const crudButtons = !user && !readOnly;
    return !loaded ? (
      <div />
    ) : (
      <ReactTable
        entity="courseSections"
        autoComplete="off"
        ref={r => (this.reactTable = r)}
        columns={this.generateColumns()}
        defaultSortField="createTime"
        defaultSearchField="name"
        defaultSortOrder="desc"
        renderForm={this.renderForm}
        validateForm={this.validateForm}
        translations={T}
        setPortalAlertStatus={setPortalAlertStatus}
        getButtons={this.getButtonInfo}
        trClassFormat={this.trClassFormat}
        openRow={this.openSection}
        headerExtra={this.headerExtra}
        footerExtra={this.footerExtra}
        canUpdateRow={({ fjœr }) => fjœr}
        multiSelect={true}
        multiDelete={true}
        onSearchFieldChange={this.onSearchFieldChange}
        customFilters={customFilters}
        createButton={crudButtons}
        updateButton={crudButtons}
        deleteButton={crudButtons}
        afterCreateOrUpdate={this.afterCreateOrUpdate}
        filterWidth={this.subtenantColPresent() ? 3 : 3}
        searchBarWidth={this.subtenantColPresent() ? 8 : 6}
      />
    );
  }
}

CourseSections.propTypes = {
  translations: PropTypes.object.isRequired,
  lo_platform: PropTypes.object.isRequired,
  setPortalAlertStatus: PropTypes.func.isRequired,
  history: PropTypes.object.isRequired,
  projectCol: PropTypes.object.isRequired,
  readOnly: PropTypes.bool.isRequired,
  customFilters: PropTypes.array,
  user: PropTypes.number,
};

function mapStateToProps(state) {
  return {
    translations: state.main.translations,
    lo_platform: state.main.lo_platform,
  };
}

function mapDispatchToProps(dispatch) {
  return bindActionCreators(MainActions, dispatch);
}

export default withProjectFilter(
  connect(mapStateToProps, mapDispatchToProps)(CourseSections),
  'courseSections'
);
