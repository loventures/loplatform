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
import Polyglot from 'node-polyglot';

/** A single configuration field descriptor returned by the connector config API. */
export interface ConnectorConfig {
  id: string;
  name?: string;
  type?: string;
}

/** A connector row as edited in the table; shape varies by connector type. */
export type ConnectorRow = Record<string, any>;

/** Props passed to every connector-type sub-component. */
export interface ConnectorComponentProps {
  T: Polyglot;
  row: ConnectorRow;
  configs: ConnectorConfig[];
  renderField: (config: ConnectorConfig) => React.ReactNode;
}

/** The parsed form passed to a connector's validateForm. */
export type ParsedForm = Record<string, any>;

/** Result of a connector's validateForm. */
export interface ValidateFormResult {
  dto: { data?: Record<string, any>; validationErrors?: any };
  parsedForm?: ParsedForm;
}

/** A single entry in the ConnectorTypes registry. */
export interface ConnectorTypeEntry {
  component?: React.ComponentType<ConnectorComponentProps>;
  validateForm?: (parsedForm: ParsedForm) => ValidateFormResult;
  unaddable?: boolean;
}
