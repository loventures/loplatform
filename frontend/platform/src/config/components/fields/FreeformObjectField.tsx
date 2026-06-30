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

import { useState } from 'react';
// Vite 8 CJS interop: unwrap the default exports of these deep CJS modules.
import ObjectFieldModule from 'react-jsonschema-form/lib/components/fields/ObjectField';
import SchemaFieldModule from 'react-jsonschema-form/lib/components/fields/SchemaField';
const ObjectField = ObjectFieldModule.default ?? ObjectFieldModule;
const SchemaField = SchemaFieldModule.default ?? SchemaFieldModule;
import _ from 'underscore';

import { FieldProps } from '../rjsfTypes';
import { pushDefaults } from './util';

/**
 * A customized `ObjectField` that handles `additionalProperties` by presenting a map-like view.
 */
const FreeformObjectField = (props: FieldProps) => {
  // The original class set `defaultProps = ObjectField.defaultProps`, which
  // supplies `formData: {}`. Replicate that here so renderRows/renderRow don't
  // hit Object.keys(undefined) when the freeform object has no data yet.
  const { name, schema, formData = {}, registry, idSchema, onChange } = props;
  const [newFieldKey, setNewFieldKey] = useState('');

  const onChangeKey = (key: string, newVal?: any) => {
    const {
      formContext: { defaults },
    } = registry;
    if ((!defaults[key] && newVal === null) || typeof newVal === 'undefined') {
      /* if we are nulling out a key and there's nothing for it to override,
       * just remove the key. */
      onChange(_.omit(formData, key));
    } else {
      onChange({
        ...formData,
        [key]: newVal,
      });
    }
  };

  const onAdd = () => {
    if (newFieldKey) {
      onChange({
        ...formData,
        [newFieldKey]: '',
      });
      setNewFieldKey('');
    }
  };

  const renderRow = (key: string) => {
    const { $id } = idSchema!;
    const { defaults } = props.formContext;
    const hideResetButton = !defaults[key];
    return (
      <div
        key={key}
        className={defaults[key] ? 'defaults freeform-row' : 'freeform-row'}
      >
        <h5>
          {key}
          {defaults[key] && ' (by default)'}
          <span
            className="btn btn-sm btn-danger ms-2"
            onClick={() => onChangeKey(key, undefined)}
          >
            Remove
          </span>
        </h5>
        <div className="freeform-cell">
          <SchemaField
            name={key}
            required={false}
            schema={schema.additionalProperties}
            uiSchema={{ 'ui:field': true }} // this suppresses the field label
            idSchema={{ $id: `${$id}_${key.replace(/[^a-zA-Z]/g, '')}` }}
            errorSchema={{}} // ???
            formData={formData[key]}
            registry={pushDefaults(key, registry, { hideResetButton })}
            onChange={(newVal?: any) => onChangeKey(key, newVal)}
            disabled={false}
            readonly={false}
          />
        </div>
      </div>
    );
  };

  const renderRows = () => {
    const {
      formContext: { defaults },
    } = registry;
    return _.uniq([...Object.keys(formData), ...Object.keys(defaults)]).map(renderRow);
  };

  if (!schema.additionalProperties) {
    return <ObjectField {...props} />;
  }
  return (
    <div
      id={`additional_${name}`}
      className="freeform-object"
    >
      <div className="mb-2">
        <strong>{schema.title}</strong>

        <span
          className="btn btn-sm btn-secondary ms-2"
          onClick={() => onChange()}
        >
          Reset
        </span>
      </div>
      {schema.description && <p className="field-description">{schema.description}</p>}
      {renderRows()}
      <div className="freeform-footer">
        <span className="freeform-cell">
          <input
            type="text"
            className="form-control add-prop-input"
            value={newFieldKey}
            placeholder="New key"
            onChange={ev => setNewFieldKey(ev.target.value)}
          />
        </span>
        <span className="freeform-add-btn-container">
          <span
            className="btn btn-sm btn-secondary add-prop-btn"
            onClick={onAdd}
          >
            Add
          </span>
        </span>
      </div>
    </div>
  );
};

export default FreeformObjectField;
