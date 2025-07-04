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

import { useAssetInfo } from '../../../resources/AssetInfo.ts';
import { ActivityProps } from '../ActivityProps.ts';
import ContentBlockInstructions from '../../parts/ContentBlockInstructions';
import { CONTENT_TYPE_RESOURCE } from '../../../utilities/contentTypes.ts';
import React from 'react';

const ResourceActivityInstructions: React.FC<ActivityProps<CONTENT_TYPE_RESOURCE>> = ({
  content,
}) => {
  const assetInfo = useAssetInfo(content);

  return assetInfo.instructions.renderedHtml ? (
    <ContentBlockInstructions
      instructions={assetInfo.instructions}
      className="mb-4"
    />
  ) : null;
};

export default ResourceActivityInstructions;
