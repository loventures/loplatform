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

import React from 'react';

/** Props passed by react-jsonschema-form to a `FieldTemplate`. */
interface LabellingTemplateProps {
  id: string;
  classNames?: string;
  label?: React.ReactNode;
  help?: React.ReactNode;
  description?: React.ReactNode;
  displayLabel?: boolean;
  errors?: React.ReactNode;
  children?: React.ReactNode;
  formContext: { changed?: boolean; defaults: any };
}

const defaultsString = (obj: any) =>
  typeof obj === 'object' || typeof obj === 'undefined' ? '' : ` (default ${obj.toString()})`;

/** A replacement `Template` for adding a stupid "(default 5)" string to field labels. */
const LabellingTemplate = ({
  id,
  classNames,
  label,
  help,
  description,
  displayLabel,
  errors,
  children,
  formContext,
}: LabellingTemplateProps) => {
  const customLabel = (
    <label
      htmlFor={id}
      className={
        formContext.changed ? 'label-changed config-schema-form-label' : 'config-schema-form-label'
      }
    >
      {label}
      {defaultsString(formContext.defaults)}
    </label>
  );
  return (
    <div className={classNames}>
      {displayLabel && customLabel}
      {displayLabel && description}
      {children}
      {errors}
      {help}
    </div>
  );
};

export default LabellingTemplate;
