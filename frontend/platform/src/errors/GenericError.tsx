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

import Polyglot from 'node-polyglot';
import React, { useEffect, useState } from 'react';

import NavigationBar from '../components/navigationBar';
import { LoPlatform } from '../types/loPlatform';
import FakeCrumb from './FakeCrumb';

interface GenericErrorProps {
  T: Polyglot;
  lo_platform: LoPlatform;
  error: string;
}

const GenericError: React.FC<GenericErrorProps> = ({ T, lo_platform, error }) => {
  const [opacity, setOpacity] = useState(0);

  useEffect(() => {
    const t = setTimeout(() => setOpacity(1), 0);
    return () => clearTimeout(t);
  }, []);

  const appearance = lo_platform.domain.appearance as Record<string, string>;
  const title = window.lo_error_title || T.t(`error.${error}`);
  const body = window.lo_error_body;
  return (
    <div
      id="error-page"
      className={error}
      style={{ opacity, transition: 'opacity 0.5s ease-out' }}
    >
      <NavigationBar />
      <FakeCrumb
        title={title}
        color={appearance['color-primary']}
      />
      <div className="container-fluid flex-col flex-center-vertical-horizontal">
        <h2
          id="error-message"
          className="mt-3"
        >
          {title}
        </h2>
        {!!body && (
          <p
            id="error-body"
            style={{ width: '75%', textAlign: 'center' }}
          >
            {body}
          </p>
        )}
        <i
          id="error-icon"
          className="material-icons"
          style={{ fontSize: '384px', color: '#666' }}
        >
          error_outline
        </i>
      </div>
    </div>
  );
};

export default GenericError;
