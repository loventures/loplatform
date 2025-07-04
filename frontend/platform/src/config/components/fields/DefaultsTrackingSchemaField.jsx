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

import React from 'react';
import SchemaField from 'react-jsonschema-form/lib/components/fields/SchemaField';

import { pushDefaults } from './util';

/** Augments the registry for each `SchemaField` with the default value
 *  for that field and a boolean indicating whether that field has been changed.
 *
 *  Hacko maximo indeed.
 */
const DefaultsTrackingSchemaField = ({ name, registry, formData, ...props }) => {
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
DefaultsTrackingSchemaField.propTypes = SchemaField.propTypes;
DefaultsTrackingSchemaField.defaultProps = SchemaField.defaultProps;

export default DefaultsTrackingSchemaField;
