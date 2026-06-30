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

import CourseStructureUpload from './CourseStructureUpload';
import LtiProvider from './LtiProvider';
import Scorm from './Scorm';
import {
  ConnectorTypeEntry,
  ParsedForm,
  ValidateFormResult,
} from '../types';

/**
 * Registry keyed by connector componentId. Most values are connector entries;
 * `DefaultFormValidator` is a bare fallback validator function.
 */
type ConnectorTypesRegistry = Record<
  string,
  ConnectorTypeEntry | ((parsedForm: ParsedForm) => ValidateFormResult)
>;

const ConnectorTypes: ConnectorTypesRegistry = {
  [LtiProvider.componentId]: {
    component: LtiProvider.component,
    validateForm: LtiProvider.validateForm,
  },
  [Scorm.componentId]: {
    component: Scorm.component,
    validateForm: Scorm.validateForm,
  },
  [CourseStructureUpload.componentId]: {
    component: CourseStructureUpload.component,
    validateForm: CourseStructureUpload.validateForm,
  },
  'loi.cp.integration.UnknownSystem': { unaddable: true },
  'loi.authoring.index.EsSystemImpl': { unaddable: true },
  DefaultFormValidator: (parsedForm: ParsedForm): ValidateFormResult => ({
    parsedForm,
    dto: {},
  }),
};

export default ConnectorTypes;
