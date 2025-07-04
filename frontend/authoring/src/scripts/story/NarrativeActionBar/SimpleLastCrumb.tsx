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

import { useEditedAssetTitle, useEditedAssetTypeId, useGraphEditSelector } from '../../graphEdit';
import { useDocumentTitle, useRouterPathVariable, useRouterQueryParam } from '../../hooks';
import { NodeName } from '../../types/asset';
import { isQuestion } from '../questionUtil';
import { subPageNames } from '../story';

export const SimpleLastCrumb: React.FC<{ name: NodeName; contextPath: string }> = ({
  name,
  contextPath,
}) => {
  const current = useRouterPathVariable('name');
  const searchParam = useRouterQueryParam('search');
  const typeId = useEditedAssetTypeId(name);
  const questionType = isQuestion(typeId);
  const title = useEditedAssetTitle(name);
  const fullPath = contextPath ? `${contextPath}.${name}` : name;
  const page = useGraphEditSelector(graphEdits => graphEdits.contentTree.page[fullPath]);

  const pageTitle =
    searchParam && current === 'search'
      ? `Search: ${searchParam}`
      : subPageNames[current]
        ? subPageNames[current]
        : questionType
          ? `Question ${page}`
          : title;
  useDocumentTitle(pageTitle);

  return <span className="text-truncate minw-0 final-crumb">{pageTitle}</span>;
};
