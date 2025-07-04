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

export type LaunchStyle = 'FRAMED' | 'NEW_WINDOW';

export interface LtiToolConfiguration {
  url?: string;
  key?: string;
  secret?: string;
  launchStyle?: LaunchStyle;
  includeUsername?: boolean;
  includeRoles?: boolean;
  includeEmailAddress?: boolean;
  includeContextTitle?: boolean;
  useExternalId?: boolean;
  isGraded?: boolean;
  customParameters: Record<string, string>;
  ltiVersion?: string;
  deepLinkUrl?: string;
}

export interface LtiTool {
  toolId: string;
  name: string;
  toolConfiguration: LtiToolConfiguration;
}

export type LtiToolApiResponse = {
  copyBranchSection: boolean;
  disabled: boolean;
  toolId: string;
  id: number;
  ltiConfiguration: {
    defaultConfiguration: LtiToolConfiguration;
    instructorEditable: Record<keyof LtiToolConfiguration, boolean> & {
      editableCustomParameters: string[];
    };
  };
  name: string;
  _type: 'ltiTool';
};
