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
import Polyglot from 'node-polyglot';
import React, { useEffect, useState } from 'react';
import { Input } from 'reactstrap';

import { AdminFormCombobox, AdminFormField } from '../components/adminForm';

interface Project {
  id: number;
  branchId?: number;
  [key: string]: unknown;
}

interface Course {
  id: number;
  title?: string;
  [key: string]: unknown;
}

interface LwSectionRow {
  id?: number;
  project_id?: number;
  project_name?: string;
  projectCode?: string;
  projectProductType?: string;
  [key: string]: unknown;
}

interface EditAddLwSectionProps {
  entity: string;
  row: LwSectionRow;
  validationErrors: Record<string, string>;
  translations: Polyglot;
  setPortalAlertStatus: (error: boolean, success: boolean, message: string) => void;
  projectId?: number | null;
  offeredOnly?: boolean;
  updateDefaultSectionName?: (name?: string, originName?: string) => void;
}

// version id is branch id
const EditAddLwSection: React.FC<EditAddLwSectionProps> = props => {
  const {
    entity,
    row,
    validationErrors,
    translations: T,
    setPortalAlertStatus,
    projectId,
    offeredOnly,
    updateDefaultSectionName,
  } = props;
  const [loaded, setLoaded] = useState(false);
  const [project, setProject] = useState<Project | null>(null);
  const [course, setCourse] = useState<Course | null>(null);

  const genericError = (e: unknown) => {
    console.log(e);
    setPortalAlertStatus(true, false, T.t('error.unexpectedError'));
  };

  const updateSectionName = (crs: Course | null) => {
    if (updateDefaultSectionName) {
      updateDefaultSectionName(crs ? crs.title : undefined, course ? course.title : undefined);
    }
  };

  const getLonelyCourse = (proj: Project): Promise<Course | null> => {
    return axios
      .get(`/api/v2/lwc/projects/${proj.id}/course`)
      .then(({ data }) => data)
      .catch(genericError);
  };

  const onProjectChange = (proj: Project | null) => {
    updateSectionName(null);
    if (proj) {
      setProject(proj);
      setCourse(null);
      getLonelyCourse(proj).then(crs => {
        updateSectionName(crs);
        setCourse(crs);
      });
    } else {
      setCourse(null);
      setProject(null);
    }
  };

  useEffect(() => {
    const fetch = (en: string, id?: number | null) =>
      id ? axios.get(`/api/v2/${en}/${id}`) : Promise.resolve({ data: null });
    if (projectId) {
      const pId = row.id ? row.project_id : projectId;
      fetch('lwc/projects', pId)
        .then(pRes => {
          setLoaded(true);
          onProjectChange(pRes.data);
        })
        .catch(genericError);
    } else {
      setLoaded(true);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const formatProject = (r: LwSectionRow) => {
    if (!r.project_id) return T.t('adminPage.courseSections.projectName.noProject');
    const { project_name, projectCode, projectProductType } = r;
    return !projectCode || (project_name && project_name.includes(projectCode))
      ? project_name
      : !projectProductType
        ? `${projectCode}: ${project_name}`
        : `${projectCode} ${projectProductType}: ${project_name}`;
  };

  const prefilter = () => (offeredOnly ? ['offered()'] : []);

  const renderProject = () => {
    if (row.id) {
      return (
        <AdminFormField
          entity={entity}
          field={'project'}
          value={formatProject(row)}
          disabled={true}
          T={T}
        />
      );
    }

    const invalid = validationErrors.project;
    const matrixFilter = (value: string) => ({ property: 'displayString', operator: 'co', value });
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
          matrixPrefilter={prefilter()}
          value={project}
          readOnly={!!row.id}
          onChange={onProjectChange}
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

  if (!loaded) return null;
  return (
    <React.Fragment>
      {row.id && <div className="entity-id">{row.id}</div>}
      {renderProject()}
    </React.Fragment>
  );
};

export default EditAddLwSection;
