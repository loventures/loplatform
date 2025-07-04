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

import PropTypes from 'prop-types';
import React from 'react';

import { AdminFormField } from '../../components/adminForm';
import LoPropTypes from '../../react/loPropTypes';
import EditAddLwSection from '../EditAddLwSection';

class EditAddForm extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      loaded: false,
    };
  }

  componentDidMount() {
    this.setState({ loaded: true });
  }

  genericError = e => {
    console.log(e);
    const T = this.props.translations;
    this.props.setPortalAlertStatus(true, false, T.t('error.unexpectedError'));
  };

  renderSectionName = () => {
    const { validationErrors, row, translations: T } = this.props;
    const field = 'name';
    return (
      <AdminFormField
        key={field}
        entity="testSections"
        field={field}
        value={row[field]}
        invalid={validationErrors[field]}
        required={true}
        autoFocus={row && !!row.id}
        T={T}
      />
    );
  };

  updateDefaultCourseName = (name, originName) => {
    // if the course name is blank or matches the previous origin course then update it
    const nameEl = document.getElementById('testSections-name');
    if (nameEl && (!nameEl.value || (originName && nameEl.value === originName))) {
      nameEl.value = name;
    }
  };

  render() {
    const { row, translations, validationErrors, setPortalAlertStatus, projectId } = this.props;
    const { loaded } = this.state;
    return (
      loaded && (
        <React.Fragment>
          {row.id && <div className="entity-id">{row.id}</div>}
          <EditAddLwSection
            entity="testSections"
            row={row}
            translations={translations}
            validationErrors={validationErrors}
            setPortalAlertStatus={setPortalAlertStatus}
            projectId={projectId}
            offeredOnly={false}
            updateDefaultSectionName={this.updateDefaultCourseName}
          />
          {this.renderSectionName()}
        </React.Fragment>
      )
    );
  }
}

EditAddForm.validateForm = (form, row, el, T) => {
  const parse = s => parseInt(s, 10) || null;
  const data = {
    fjœr: true,
    name: form.name,
    project_id: parse(form.project),
    version_id: parse(form.version),
    course_id: parse(form.course),
    groupId: row.groupId,
    externalId: row.externalId,
  };
  const nameMissing = !data.name && 'name';
  const addMissing = !data.project_id
    ? 'project'
    : !data.version_id
      ? 'version'
      : !data.course_id
        ? 'course'
        : nameMissing;
  const missing = row.id ? nameMissing : addMissing;
  if (missing) {
    const params = { field: T.t(`adminPage.testSections.fieldName.${missing}`) };
    return { validationErrors: { [missing]: T.t('adminForm.validation.fieldIsRequired', params) } };
  } else {
    return {
      data,
      extras: {
        roster: form.roster === 'on',
      },
    };
  }
};

EditAddForm.propTypes = {
  row: PropTypes.object,
  validationErrors: PropTypes.object,
  translations: LoPropTypes.translations,
  setPortalAlertStatus: PropTypes.func.isRequired,
  projectId: PropTypes.number,
};

export default EditAddForm;
