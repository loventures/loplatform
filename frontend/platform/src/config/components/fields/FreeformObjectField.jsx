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

import React, { Component } from 'react';
import ObjectField from 'react-jsonschema-form/lib/components/fields/ObjectField';
import SchemaField from 'react-jsonschema-form/lib/components/fields/SchemaField';
import _ from 'underscore';

import { pushDefaults } from './util';

/**
 * A customized `ObjectField` that handles `additionalProperties` by presenting a map-like view.
 */
export default class FreeformObjectField extends Component {
  constructor() {
    super();
    this.state = { newFieldKey: '' };
  }

  render() {
    const { name, schema } = this.props;
    if (!schema.additionalProperties) {
      return <ObjectField {...this.props} />;
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
            onClick={() => this.props.onChange()}
          >
            Reset
          </span>
        </div>
        {schema.description && <p className="field-description">{schema.description}</p>}
        {this.renderRows()}
        <div className="freeform-footer">
          <span className="freeform-cell">
            <input
              type="text"
              className="form-control add-prop-input"
              value={this.state.newFieldKey}
              placeholder="New key"
              onChange={ev => this.setState({ newFieldKey: ev.target.value })}
            />
          </span>
          <span className="freeform-add-btn-container">
            <span
              className="btn btn-sm btn-secondary add-prop-btn"
              onClick={this.onAdd.bind(this)}
            >
              Add
            </span>
          </span>
        </div>
      </div>
    );
  }

  renderRows() {
    const {
      formData,
      registry: {
        formContext: { defaults },
      },
    } = this.props;

    return _.uniq([...Object.keys(formData), ...Object.keys(defaults)]).map(
      this.renderRow.bind(this)
    );
  }

  renderRow(key) {
    const {
      schema,
      idSchema: { $id },
      formData,
      registry,
      formContext: { defaults },
    } = this.props;
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
            onClick={() => this.onChange(key, undefined)}
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
            onChange={this.onChange.bind(this, key)}
            disabled={false}
            readonly={false}
          />
        </div>
      </div>
    );
  }

  onAdd() {
    if (this.state.newFieldKey) {
      this.props.onChange({
        ...this.props.formData,
        [this.state.newFieldKey]: '',
      });
      this.setState({ newFieldKey: '' });
    }
  }

  onChange(key, newVal) {
    const {
      onChange,
      formData,
      registry: {
        formContext: { defaults },
      },
    } = this.props;
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
  }
}

FreeformObjectField.propTypes = ObjectField.propTypes;
FreeformObjectField.defaultProps = ObjectField.defaultProps;
