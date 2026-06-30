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

import React, { useEffect, useRef, useState } from 'react';

interface LegacyIframeProps {
  params?: Record<string, string | number>;
  src: string;
  title: string;
  location?: { search?: string };
}

const LegacyIframe: React.FC<LegacyIframeProps> = ({ src, location, params = {}, title }) => {
  const [loaded, setLoaded] = useState(false);
  const ifr = useRef<HTMLIFrameElement>(null);

  useEffect(() => {
    if (ifr.current) {
      ifr.current.onload = () => setLoaded(true);
    }
  }, []);

  const qs = Object.keys(params)
    .map(key => `${encodeURIComponent(key)}=${encodeURIComponent(params[key])}`)
    .join('&');

  const iframeURL = `${src}${(location && location.search) || '?'}&${qs}`;

  return (
    <iframe
      id="legacy-frame"
      title={title}
      className={loaded ? 'show-me' : 'hide-me'}
      src={iframeURL}
      ref={ifr}
    />
  );
};

export default LegacyIframe;
