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
import moment from 'moment-timezone';
import PropTypes from 'prop-types';
import React from 'react';
import { connect } from 'react-redux';
import { bindActionCreators } from 'redux';

import ReactTable from '../components/reactTable/ReactTable';
import LoPropTypes from '../react/loPropTypes';
import * as MainActions from '../redux/actions/MainActions';
import { inCurrTimeZone } from '../services/moment.js';
import EditAddUserEnrollment from './EditAddUserEnrollment';

class App extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      domainRoles: [],
      loaded: false,
    };
  }

  componentDidMount() {
    const { setLastCrumb, match, translations: T, controllerValue } = this.props;
    const { userId } = match.params;
    axios.get(`/api/v2/users/${userId}`).then(res => {
      setLastCrumb(T.t(`adminPage.${controllerValue}.enrollments.userEnrollments.name`, res.data));
    });
  }

  formatTime = t => {
    const T = this.props.translations;
    const dateTimeFormat = T.t('format.dateTime.compact');
    return t ? inCurrTimeZone(moment(t)).format(dateTimeFormat) : '';
  };

  columns = [
    { dataField: 'id', isKey: true },
    { dataField: 'role_name', sortable: false, searchable: false },
    { dataField: 'disabled', sortable: false, searchable: false },
    { dataField: 'startTime', sortable: false, searchable: false, dataFormat: this.formatTime },
    { dataField: 'stopTime', sortable: false, searchable: false, dataFormat: this.formatTime },
  ];

  renderForm = (row, validationErrors) => {
    const { translations: T, courseId } = this.props;
    return (
      <EditAddUserEnrollment
        T={T}
        row={row}
        validationErrors={validationErrors}
        courseId={courseId}
      />
    );
  };

  validateForm = form => {
    const { translations: T } = this.props;
    const invalidF = field => {
      const params = { field: T.t(`adminPage.enrollments.userEnrollments.fieldName.${field}`) };
      return {
        validationErrors: { [field]: T.t('adminForm.validation.fieldMustBeValid', params) },
      };
    };
    if (!form.role) {
      const params = { field: T.t(`adminPage.enrollments.fieldName.role`) };
      return { validationErrors: { role: T.t('adminForm.validation.fieldIsRequired', params) } };
    }
    if (form.startTime && !moment(form.startTime).isValid()) {
      return invalidF('startTime');
    }
    if (
      form.stopTime &&
      (!moment(form.stopTime).isValid() ||
        (form.startTime && !moment(form.stopTime).isAfter(form.startTime)))
    ) {
      return invalidF('stopTime');
    }
    const data = {
      roleId: parseInt(form.role.trim(), 10),
      startTime: form.startTime ? moment(form.startTime).toISOString() : null,
      stopTime: form.stopTime ? moment(form.stopTime).toISOString() : null,
      disabled: form.active !== 'on',
      id: parseInt(form.id, 10),
    };
    return { data };
  };

  createDeleteDTO = () => ({ data: {} });

  submitForm = ({ data, create }) => {
    const { id, ...payload } = data;
    const { match, courseId } = this.props;
    const { userId } = match.params;
    if (create) {
      return axios
        .post(`/api/v2/courses/${courseId}/enrollments/byUser/${userId}`, payload)
        .then(res => res);
    } else {
      return axios.put(`/api/v2/courses/${courseId}/enrollments/${id}`, payload).then(res => res);
    }
  };

  parseEntity = entity => {
    const { translations: T } = this.props;
    const status = entity.disabled ? 'inactive' : 'active';
    return {
      ...entity,
      disabled: T.t(`adminPage.enrollments.userEnrollments.status.${status}`),
    };
  };

  getDeleteUrl = id => {
    const { courseId } = this.props;
    return `/api/v2/courses/${courseId}/enrollments/${id}`;
  };

  render() {
    const { match, translations, setPortalAlertStatus, courseId, readOnly } = this.props;
    const handleDelete = {
      createDeleteDTO: this.createDeleteDTO,
      deleteMethod: 'delete',
      getDeleteUrl: this.getDeleteUrl,
    };

    const { userId } = match.params;
    return (
      <ReactTable
        entity="enrollments.userEnrollments"
        baseUrl={`/api/v2/courses/${courseId}/enrollments/byUser/${userId}`}
        columns={this.columns}
        parseEntity={this.parseEntity}
        defaultSortField=""
        defaultSearchField=""
        renderForm={this.renderForm}
        validateForm={this.validateForm}
        translations={translations}
        submitForm={this.submitForm}
        setPortalAlertStatus={setPortalAlertStatus}
        handleDelete={handleDelete}
        paginate={false}
        createButton={!readOnly}
        updateButton={!readOnly}
        deleteButton={!readOnly}
      />
    );
  }
}

App.propTypes = {
  translations: LoPropTypes.translations,
  setPortalAlertStatus: PropTypes.func.isRequired,
  match: PropTypes.object.isRequired,
  location: PropTypes.object.isRequired,
  setLastCrumb: PropTypes.func.isRequired,
  controllerValue: PropTypes.string.isRequired,
  courseId: PropTypes.string.isRequired,
  readOnly: PropTypes.bool.isRequired,
};

function mapStateToProps(state) {
  const rights = state.main.lo_platform.user.rights;
  const readOnly =
    !rights.includes('loi.cp.admin.right.CourseAdminRight') &&
    !rights.includes('loi.cp.course.right.ManageCoursesAdminRight');
  return {
    translations: state.main.translations,
    readOnly,
  };
}

function mapDispatchToProps(dispatch) {
  return bindActionCreators(MainActions, dispatch);
}

const UserEnrollments = connect(mapStateToProps, mapDispatchToProps)(App);

export default UserEnrollments;
