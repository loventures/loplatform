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

import { ActivityProps } from '../ActivityProps.ts';
import { useTranslation } from '../../../i18n/translationContext.tsx';
import { CONTENT_TYPE_RESOURCE } from '../../../utilities/contentTypes.ts';
import React from 'react';

import ExternalLink from '../../parts/ExternalLink.tsx';

const ResourceActivityLink: React.FC<ActivityProps<CONTENT_TYPE_RESOURCE>> = ({ content }) => {
  const translate = useTranslation();
  return (
    <div className="external-link-activity card">
      <div className="card-body">
        <ExternalLink content={content}>{translate('RESOURCE_DIRECT_LINK')}</ExternalLink>
      </div>
    </div>
  );
};

export default ResourceActivityLink;
