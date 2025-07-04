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

import axios, { AxiosError } from 'axios';
import LoadingSpinner from '../../directives/loadingSpinner';
import IframeResizer from 'iframe-resizer-react';
import { useTranslation } from '../../i18n/translationContext';
import { learnerDashboardId, sectionDashboardId } from '../../utilities/preferences';
import React, { useEffect, useState } from 'react';

const MetabaseEmbed: React.FC<{ section: number; learner?: number }> = ({ section, learner }) => {
  const translate = useTranslation();
  const [loading, setLoading] = useState(true);
  const [embedUrl, setEmbedUrl] = useState<string | undefined>(undefined);
  const [error, setError] = useState<string | undefined>(undefined);

  useEffect(() => {
    const dashboardId = learner ? learnerDashboardId : sectionDashboardId;
    axios
      .post<string>(`/api/v2/metabase/dashboard/${dashboardId}`, {
        section,
        ...(learner ? { learner } : {}),
      })
      .then(({ data: url }) => setEmbedUrl(url))
      .catch((error: AxiosError) => {
        setError(error.message);
        setLoading(false);
      });
  }, [section, learner]);

  const iframeParams = {
    src: embedUrl,
    style: {
      border: 0,
      minHeight: 600,
    },
    width: '100%',
    onLoad: () => setLoading(false),
  };

  return (
    <div className="container-fluid d-flex flex-column align-items-center p-3">
      {loading && <LoadingSpinner message="LOADING" />}
      {embedUrl && <IframeResizer {...iframeParams}></IframeResizer>}
      {error && <div>{translate('error.unexpectedError')}</div>}
    </div>
  );
};

export default MetabaseEmbed;
