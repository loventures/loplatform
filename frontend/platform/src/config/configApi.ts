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

import axios from 'axios';

/** A JSON schema describing a single configuration, as served by `/api/v2/config`. */
export type ConfigSchema = Record<string, any>;

/** Map of schema name to its JSON schema. */
export type Schemata = Record<string, ConfigSchema>;

/** The defaults/overrides payload for a single configuration. */
export interface ConfigData {
  defaults: any;
  overrides: any;
}

export const getSchemata = () => axios.get<Schemata>('/api/v2/config');

export const getConfig = (name: string, item?: number) =>
  axios.get<ConfigData>(
    item ? `/api/v2/config/${name}/item/${item}` : `/api/v2/config/${name}`
  );

export const putConfig = (name: string, item: number | undefined, data: string) =>
  axios.put(item ? `/api/v2/config/${name}/${item}` : `/api/v2/config/${name}`, data, {
    headers: { 'Content-Type': 'application/json' },
  });
