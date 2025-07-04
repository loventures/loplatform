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
import { connect } from 'react-redux';
import { bindActionCreators } from 'redux';

import * as MainActions from '../redux/actions/MainActions';
import { AdminFormCombobox } from './adminForm';
import { getSavedTableState } from './reactTable/ReactTable';

export const withProjectFilter = (Component, entity) => {
  class WithProjectFilter extends React.Component {
    state = {
      projects: [],
      project: null,
      loaded: false,
    };

    componentDidMount() {
      const savedFilter = {
        projectId: getSavedTableState(entity, 'project_id', 0),
      };
      const filter = this.props.initFilter || (savedFilter.projectId && savedFilter);
      axios
        .get('/api/v2/lwc/projects')
        .then(res => {
          const projects = res.data.objects;
          if (!filter) {
            this.setState({ loaded: true, projects });
          } else {
            this.setState({
              loaded: true,
              projects,
              project: projects.find(project => project.id === filter.projectId),
            });
          }
        })
        .catch(this.genericError);
    }

    genericError = e => {
      const { setPortalAlertStatus, translations: T } = this.props;
      console.log(e);
      setPortalAlertStatus(true, false, T.t('error.unexpectedError'));
    };

    toOptions = elts =>
      elts.map(o => (
        <option
          key={o.id}
          value={o.id}
        >
          {o.name}
        </option>
      ));

    projectOnChange = (e, currentFilters) => {
      const project = e.target.value;
      this.setState({ project: project });
      const projectId = project && project.id;
      const filters = [...currentFilters];
      const index = filters.findIndex(filter => filter.property === 'project_id');
      if (projectId) {
        filters[index < 0 ? filters.length : index] = {
          property: 'project_id',
          operator: 'eq',
          value: projectId,
        };
      } else if (index !== -1) {
        filters.splice(index, 1);
      }
      return filters;
    };

    getFilterProps = (field, elements, onChange) => {
      const { translations: T } = this.props;
      return {
        filterOptions: this.toOptions(elements),
        baseFilter: T.t(`withProjectFilter.${field}.filters.any`),
        onFilterChange: onChange,
      };
    };

    ProjectFilterInput = props => {
      const { translations: T } = this.props;
      const { baseProps } = props;
      const { project } = this.state;
      const matrixFilter = value => ({ property: 'displayString', operator: 'co', value });
      const matrixOrder = () => ({ property: 'displayString', direction: 'asc' });
      return (
        <AdminFormCombobox
          {...baseProps}
          key="project"
          entity={entity}
          field="project-filter"
          targetEntity="lwc/projects"
          matrixFilter={matrixFilter}
          matrixOrder={matrixOrder}
          onChange={project => baseProps.onChange({ target: { value: project } })}
          T={T}
          inputOnly={true}
          value={project}
          placeholder={T.t(`withProjectFilter.project.filters.any`)}
        />
      );
    };

    render() {
      const { projects, project, loaded } = this.state;
      if (!loaded) return null;
      const projectFilterProps = this.getFilterProps('project', projects, this.projectOnChange);
      const projectCol = {
        dataField: 'project_id',
        hidden: true,
        filterable: true,
        ...projectFilterProps,
        FilterInput: this.ProjectFilterInput,
        filterProperty: 'project_id',
      };
      const customFilters = project && [
        { property: 'project_id', operator: 'eq', value: project.id },
      ];
      return (
        <Component
          projectCol={projectCol}
          customFilters={customFilters}
          autoSelect={!!customFilters}
          {...this.props}
        />
      );
    }
  }

  WithProjectFilter.propTypes = {
    translations: PropTypes.object.isRequired,
    lo_platform: PropTypes.object.isRequired,
    setPortalAlertStatus: PropTypes.func.isRequired,
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

  return connect(mapStateToProps, mapDispatchToProps)(WithProjectFilter);
};
