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
import PropTypes from 'prop-types';
import React from 'react';
import { Input } from 'reactstrap';

import { AdminFormCombobox, AdminFormField } from '../components/adminForm';
import LoPropTypes from '../react/loPropTypes';

// version id is branch id
class EditAddLwSection extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      loaded: false,
      project: null,
      course: null,
    };
  }

  formatProject = row => {
    const { translations: T } = this.props;
    if (!row.project_id) return T.t('adminPage.courseSections.projectName.noProject');
    const { project_name, projectCode, projectProductType } = row;
    return !projectCode || project_name.includes(projectCode)
      ? project_name
      : !projectProductType
        ? `${projectCode}: ${project_name}`
        : `${projectCode} ${projectProductType}: ${project_name}`;
  };

  componentDidMount() {
    const { row, projectId } = this.props;
    const fetch = (entity, id) =>
      id ? axios.get(`/api/v2/${entity}/${id}`) : Promise.resolve({ data: null });
    if (projectId) {
      const pId = row.id ? row.project_id : projectId;
      fetch('lwc/projects', pId)
        .then(pRes => {
          this.setState({
            loaded: true,
          });
          this.onProjectChange(pRes.data);
        })
        .catch(this.genericError);
    } else {
      this.setState({ loaded: true });
    }
  }

  genericError = e => {
    console.log(e);
    const T = this.props.translations;
    this.props.setPortalAlertStatus(true, false, T.t('error.unexpectedError'));
  };

  onProjectChange = project => {
    this.updateSectionName(null);
    if (project) {
      this.setState({
        project: project,
        course: null,
      });
      this.setState({ coursesLoading: true });
      this.getLonelyCourse(project).then(course => {
        this.updateSectionName(course);
        this.setState({
          course: course,
          coursesLoading: false,
        });
      });
    } else {
      this.setState({
        course: null,
        project: null,
      });
    }
  };

  updateSectionName = crs => {
    const { updateDefaultSectionName } = this.props;
    const { course } = this.state;
    if (updateDefaultSectionName) {
      updateDefaultSectionName(crs && crs.title, course && course.title);
    }
  };

  renderProject = () => {
    const { row, translations: T, validationErrors, entity } = this.props;
    if (row.id) {
      return (
        <AdminFormField
          entity={entity}
          field={'project'}
          value={this.formatProject(row)}
          disabled={true}
          T={T}
        />
      );
    }

    const { project, course } = this.state;
    const invalid = validationErrors.project;
    const matrixFilter = value => ({ property: 'displayString', operator: 'co', value });
    const matrixOrder = () => ({ property: 'displayString', direction: 'asc' });
    return (
      <>
        <AdminFormCombobox
          key="project"
          entity={entity}
          field="project"
          targetEntity="lwc/projects"
          matrixFilter={matrixFilter}
          matrixOrder={matrixOrder}
          matrixPrefilter={this.prefilter()}
          value={project}
          readOnly={!!row.id}
          onChange={this.onProjectChange}
          T={T}
          invalid={invalid || ''}
          required={!row.id}
          autoFocus={!row.id}
        />
        <Input
          type="hidden"
          name="version"
          value={project ? project.branchId : ''}
        />
        <Input
          type="hidden"
          name="course"
          value={course ? course.id : ''}
        />
      </>
    );
  };

  prefilter = () => (this.props.offeredOnly ? ['offered()'] : []);

  getLonelyCourse = project => {
    return axios
      .get(`/api/v2/lwc/projects/${project.id}/course`)
      .then(({ data }) => data)
      .catch(this.genericError);
  };

  render() {
    if (!this.state.loaded) return null;
    const { row } = this.props;
    return (
      <React.Fragment>
        {row.id && <div className="entity-id">{row.id}</div>}
        {this.renderProject()}
      </React.Fragment>
    );
  }
}

EditAddLwSection.propTypes = {
  entity: PropTypes.string.isRequired,
  row: PropTypes.object,
  validationErrors: PropTypes.object,
  translations: LoPropTypes.translations,
  setPortalAlertStatus: PropTypes.func.isRequired,
  projectId: PropTypes.number,
  offeredOnly: PropTypes.bool,
  updateDefaultSectionName: PropTypes.func,
};

export default EditAddLwSection;
