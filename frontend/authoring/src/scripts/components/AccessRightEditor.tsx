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

import * as React from 'react';
import { FormGroup, Label } from 'reactstrap';

import { CONTAINER_AND_ELEMENT_TYPES } from '../editor/EdgeRuleConstants';
import { useAssetDataField, usePolyglot } from '../hooks';
import { TypeId } from '../types/asset';

export const accessRightsMap = {
  '': 'NONE',
  'loi.cp.course.right.ContentCourseRight': 'INSTRUCTOR',
  'loi.authoring.security.right$AccessAuthoringAppRight': 'AUTHOR',
} as const;

export const accessRights = Object.entries(accessRightsMap);

export const accessRightable = new Set<TypeId>(CONTAINER_AND_ELEMENT_TYPES);

export const accessRightI18n = (accessRight: string) =>
  `ACCESS_RIGHT_${accessRightsMap[accessRight]}`;

const AccessRightEditor: React.FC<{ disabled: boolean }> = ({ disabled }) => {
  const accessRightField = useAssetDataField('accessRight');
  const polyglot = usePolyglot();

  return (
    <FormGroup className="accessRight-selector row">
      <Label
        for="accessRightField"
        className="col-2 pt-2"
      >
        {polyglot.t('ACCESS_RIGHT')}
      </Label>
      <div className="col-10">
        <select
          id="accessRightField"
          name="accessRight"
          className="form-control"
          value={accessRightField.value ?? ''}
          onChange={e => accessRightField.onChange(e.target.value || null)}
          disabled={disabled}
        >
          {accessRights.map(([key, label]) => (
            <option
              key={key}
              value={key}
            >
              {polyglot.t(`ACCESS_RIGHT_${label}`)}
            </option>
          ))}
        </select>
      </div>
    </FormGroup>
  );
};

export default AccessRightEditor;
