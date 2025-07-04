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
import IframeResizer from 'iframe-resizer-react';
import Polyglot from 'node-polyglot';
import React, { useEffect, useState } from 'react';
import { useParams } from 'react-router';

import WaitDotGif from '../components/WaitDotGif';

const Metabase: React.FC<{
  T: Polyglot;
}> = ({ T }) => {
  const [loading, setLoading] = useState(true);
  const { embedType, id } = useParams<{ embedType: string; id: string }>();
  const [embedUrl, setEmbedUrl] = useState<string>();
  const [error, setError] = useState<string>();

  useEffect(() => {
    axios
      .post<string>(`/api/v2/metabase/${embedType}/${id}`, {})
      .then(({ data: url }) => setEmbedUrl(url))
      .catch((error: AxiosError) => {
        setError(error.message);
        setLoading(false);
      });
  }, [embedType, id]);

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
    <div className="container-fluid d-flex flex-column align-items-center">
      {loading && (
        <WaitDotGif
          className="m-5"
          color="secondary"
          size={100}
        />
      )}
      {embedUrl && (
        <>
          {/* iframe resizer only supported for dashboards */}
          {embedType === 'dashboard' ? (
            <IframeResizer {...iframeParams}></IframeResizer>
          ) : (
            <iframe {...iframeParams}></iframe>
          )}
        </>
      )}
      {error && <div>{T.t('error.unexpectedError')}</div>}
    </div>
  );
};

export default Metabase;
