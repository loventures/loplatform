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

import { useEditedAsset } from '../../graphEdit';
import { NodeName, TypeId } from '../../types/asset';
import { NarrativeSettings } from '../story';
import { narrativeSettings } from './settings';

const NullSettings: NarrativeSettings<any> = () => null;

export const NarrativeSettingsProxy: React.FC<{
  name: NodeName;
  typeId: TypeId;
}> = ({ name, typeId }) => {
  const asset = useEditedAsset(name);
  const NarrativeSettings = narrativeSettings[typeId] ?? NullSettings;

  return <NarrativeSettings asset={asset} />;
};
