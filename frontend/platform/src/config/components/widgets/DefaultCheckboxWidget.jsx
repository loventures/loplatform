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
import { Button, FormGroup, Input, Label } from 'reactstrap';
import TooltipDescriptionField from '../misc/TooltipDescriptionField';

/** Checkboxen get no field label, so we add the default value to the label.. */
const DefaultCheckboxWidget = function (props) {
  /* this is *NOT* `!props.value` since `false` is still set! */
  const isUnset = props.value === null || typeof props.value === 'undefined';
  const checked = isUnset ? !!props.formContext.defaults : !!props.value;
  return (
    <div className="flex-row">
      <FormGroup
        className="flex-row"
        check
      >
        <Label
          check
          className={'config-schema-form-label ' + (!props.value ? '' : 'label-changed')}
        >
          {props.label}
          <Input
            id={props.id}
            type="checkbox"
            checked={checked}
            onChange={() => props.onChange(!checked)}
          />{' '}
        </Label>
        <TooltipDescriptionField
          id={props.id + '-description'}
          description={props.schema.description}
        />
        <Label
          check
          className="margin-left5"
        >
          {'(default '}
          <span className="plain-font ms-1">{props.formContext.defaults ? '☑' : '☐'}</span>
          {')'}
        </Label>
        <Button
          id={`${props.id}-reset`}
          size="sm"
          className="margin-left5"
          onClick={() => props.onChange()}
        >
          Reset
        </Button>{' '}
      </FormGroup>
    </div>
  );
};

//DefaultCheckboxWidget.propTypes = CheckboxWidget.propTypes;
//DefaultCheckboxWidget.defaultProps = CheckboxWidget.defaultProps;

export default DefaultCheckboxWidget;
