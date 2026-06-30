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

import { Registry } from './fields/util';

/**
 * react-jsonschema-form ships no usable types in this tree, so we narrow the
 * widget/field props to the members this app actually reads. Everything else
 * is forwarded through `...props`, so an index signature keeps spreads valid.
 */
export interface RjsfSchema {
  description?: string;
  required?: boolean;
  title?: string;
  additionalProperties?: any;
  [key: string]: any;
}

export interface WidgetProps {
  id: string;
  value?: any;
  label?: React.ReactNode;
  schema: RjsfSchema;
  registry: Registry;
  formContext: { defaults: any; [key: string]: any };
  onChange: (value?: any) => void;
  [key: string]: any;
}

export interface FieldProps {
  name: string;
  schema: RjsfSchema;
  registry: Registry;
  formData?: any;
  formContext: { defaults: any; [key: string]: any };
  idSchema?: { $id: string; [key: string]: any };
  onChange: (value?: any) => void;
  [key: string]: any;
}
