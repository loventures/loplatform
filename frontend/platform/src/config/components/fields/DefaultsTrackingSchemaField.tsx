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

// Vite 8 CJS interop: unwrap the default export of this deep CJS module.
import SchemaFieldModule from 'react-jsonschema-form/lib/components/fields/SchemaField';
const SchemaField = SchemaFieldModule.default ?? SchemaFieldModule;

import { FieldProps } from '../rjsfTypes';
import { pushDefaults } from './util';

/** Augments the registry for each `SchemaField` with the default value
 *  for that field and a boolean indicating whether that field has been changed.
 *
 *  Hacko maximo indeed.
 */
const DefaultsTrackingSchemaField = ({ name, registry, formData, ...props }: FieldProps) => {
  const changed = typeof formData !== 'undefined';
  return (
    <div className="wrapper">
      <SchemaField
        {...props}
        formData={formData}
        name={name}
        registry={pushDefaults(name, registry, { changed })}
      />
    </div>
  );
};

export default DefaultsTrackingSchemaField;
