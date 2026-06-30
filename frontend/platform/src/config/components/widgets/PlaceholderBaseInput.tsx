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
// Vite 8 CJS interop: unwrap the default export of this deep CJS module.
import BaseInputModule from 'react-jsonschema-form/lib/components/widgets/BaseInput';
const BaseInput = BaseInputModule.default ?? BaseInputModule;
import { Button } from 'reactstrap';

import { WidgetProps } from '../rjsfTypes';

/** The increasingly misnamed component responsible for putting the default value
 *  in as a placeholder. Also provides a reset and clear button.
 */
const PlaceholderBaseInput = (props: WidgetProps) => {
  const { id, value, registry, onChange, schema, formContext } = props;

  const placeholder = () => {
    const { defaults } = formContext;
    return typeof defaults === 'undefined' ||
      (typeof defaults === 'object' && Object.keys(defaults).length === 0)
      ? ''
      : value === null
        ? '<unset>'
        : defaults.toString();
  };

  const onFocus = (ev: React.FocusEvent<HTMLInputElement>) => {
    ev.preventDefault && ev.preventDefault();
    const { defaults } = registry.formContext;
    /* fill in defaults by default */
    const wasBlank = !value || (typeof value === 'object' && Object.keys(value).length === 0);
    if (wasBlank && defaults && typeof defaults !== 'object') {
      onChange(defaults);
    }
  };

  return (
    <div className="placeholder-input-container">
      <BaseInput
        {...props}
        value={value || ''} // hm.
        placeholder={placeholder()}
        registry={registry}
        onFocus={onFocus}
      />
      {!schema.required && (
        <Button
          id={`${id}-clear`}
          size="sm"
          className="margin-left5"
          onClick={() => onChange(null)}
        >
          Clear
        </Button>
      )}
      {!registry.formContext.hideResetButton && (
        <Button
          id={`${id}-reset`}
          size="sm"
          className="margin-left5"
          onClick={() => onChange()}
        >
          Reset
        </Button>
      )}
    </div>
  );
};

export default PlaceholderBaseInput;
