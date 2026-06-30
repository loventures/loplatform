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
import React, { useEffect, useState } from 'react';
import { useDispatch } from 'react-redux';

import { setPortalAlertStatus } from '../redux/actions/MainActions';
import { useTranslations } from '../redux/state';
import { AdminFormCombobox } from './adminForm';
import { getSavedTableState } from './reactTable/ReactTable';

interface Project {
  id: number;
  name: string;
}

interface ProjectFilter {
  projectId: number;
}

interface WithProjectFilterProps {
  initFilter?: ProjectFilter;
  [key: string]: unknown;
}

export const withProjectFilter =
  (Component: React.ComponentType<any>, entity?: string): React.FC<WithProjectFilterProps> =>
  (props: WithProjectFilterProps) => {
    const dispatch = useDispatch();
    const T = useTranslations();
    const [projects, setProjects] = useState<Project[]>([]);
    const [project, setProject] = useState<Project | null>(null);
    const [loaded, setLoaded] = useState(false);

    const genericError = (e: unknown) => {
      console.log(e);
      dispatch(setPortalAlertStatus(true, false, T.t('error.unexpectedError')));
    };

    useEffect(() => {
      const savedFilter = {
        projectId: getSavedTableState(entity, 'project_id', 0),
      };
      const filter = props.initFilter || (savedFilter.projectId && savedFilter);
      axios
        .get('/api/v2/lwc/projects')
        .then(res => {
          const allProjects: Project[] = res.data.objects;
          if (!filter) {
            setLoaded(true);
            setProjects(allProjects);
          } else {
            setLoaded(true);
            setProjects(allProjects);
            setProject(allProjects.find(p => p.id === filter.projectId) ?? null);
          }
        })
        .catch(genericError);
      // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);

    const toOptions = (elts: Project[]) =>
      elts.map(o => (
        <option
          key={o.id}
          value={o.id}
        >
          {o.name}
        </option>
      ));

    const projectOnChange = (e: { target: { value: Project } }, currentFilters: any[]) => {
      const proj = e.target.value;
      setProject(proj);
      const projectId = proj && proj.id;
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

    const getFilterProps = (field: string, elements: Project[], onChange: any) => ({
      filterOptions: toOptions(elements),
      baseFilter: T.t(`withProjectFilter.${field}.filters.any`),
      onFilterChange: onChange,
    });

    const ProjectFilterInput: React.FC<{ baseProps: any }> = ({ baseProps }) => {
      const matrixFilter = (value: string) => ({
        property: 'displayString',
        operator: 'co',
        value,
      });
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
          onChange={(proj: Project) => baseProps.onChange({ target: { value: proj } })}
          T={T}
          inputOnly={true}
          value={project}
          placeholder={T.t(`withProjectFilter.project.filters.any`)}
        />
      );
    };

    if (!loaded) return null;
    const projectFilterProps = getFilterProps('project', projects, projectOnChange);
    const projectCol = {
      dataField: 'project_id',
      hidden: true,
      filterable: true,
      ...projectFilterProps,
      FilterInput: ProjectFilterInput,
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
        {...props}
      />
    );
  };
