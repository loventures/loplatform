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

import Polyglot from 'node-polyglot';
import React, { useEffect, useState } from 'react';

import { AdminFormField } from '../../components/adminForm';
import { LoPlatform } from '../../types/loPlatform';
import EditAddLwSection from '../EditAddLwSection';

interface TestSectionRow {
  id?: number;
  name?: string;
  groupId?: string;
  externalId?: string;
  [key: string]: unknown;
}

interface EditAddFormProps {
  row: TestSectionRow;
  validationErrors: Record<string, string>;
  translations: Polyglot;
  setPortalAlertStatus: (error: boolean, success: boolean, message: string) => void;
  projectId?: number | null;
  columns?: unknown[];
  lo_platform?: LoPlatform;
}

interface EditAddFormComponent extends React.FC<EditAddFormProps> {
  validateForm: (
    form: Record<string, any>,
    row: TestSectionRow,
    _el: HTMLFormElement,
    T: Polyglot
  ) => any;
}

const EditAddForm: EditAddFormComponent = ({
  row,
  translations,
  validationErrors,
  setPortalAlertStatus,
  projectId,
}) => {
  const [loaded, setLoaded] = useState(false);

  useEffect(() => {
    setLoaded(true);
  }, []);

  const renderSectionName = () => {
    const field = 'name';
    return (
      <AdminFormField
        key={field}
        entity="testSections"
        field={field}
        value={row[field] as string}
        invalid={validationErrors[field]}
        required={true}
        autoFocus={!!(row && row.id)}
        T={translations}
      />
    );
  };

  const updateDefaultCourseName = (name?: string, originName?: string) => {
    // if the course name is blank or matches the previous origin course then update it
    const nameEl = document.getElementById('testSections-name') as HTMLInputElement | null;
    if (nameEl && (!nameEl.value || (originName && nameEl.value === originName))) {
      nameEl.value = name || '';
    }
  };

  if (!loaded) return null;
  return (
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
        updateDefaultSectionName={updateDefaultCourseName}
      />
      {renderSectionName()}
    </React.Fragment>
  );
};

EditAddForm.validateForm = (form, row, _el, T) => {
  const parse = (s: string) => parseInt(s, 10) || null;
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

export default EditAddForm;
