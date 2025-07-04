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

import { useAssetInfo } from '../../resources/AssetInfo.ts';
import { trackOpenExternalLinkEvent } from '../../analytics/trackEvents.ts';
import { reportProgressActionCreator } from '../../courseActivityModule/actions/activityActions.ts';
import { ContentWithRelationships } from '../../courseContentModule/selectors/assembleContentView.ts';
import { useTranslation } from '../../i18n/translationContext.tsx';
import { CONTENT_TYPE_RESOURCE } from '../../utilities/contentTypes.ts';
import React from 'react';
import { useDispatch } from 'react-redux';

type ExternalLinkProps = {
  content: ContentWithRelationships<CONTENT_TYPE_RESOURCE>;
} & React.PropsWithChildren;

const ExternalLink: React.FC<ExternalLinkProps> = ({ content, children }) => {
  const dispatch = useDispatch();
  const translate = useTranslation();
  const assetInfo = useAssetInfo(content);

  return (
    <a
      href={assetInfo.embedCode}
      target="_blank"
      rel="noopener"
      onClick={() => {
        dispatch(reportProgressActionCreator(content, true));
        trackOpenExternalLinkEvent();
      }}
      title={translate('RESOURCE_NEW_WINDOW')}
    >
      {children}
    </a>
  );
};

export default ExternalLink;
