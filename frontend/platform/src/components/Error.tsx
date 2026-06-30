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

interface ErrorProps {
  T: Polyglot;
  setLastCrumb: (crumb: string) => void;
  message?: string;
}

const Error: React.FC<ErrorProps> = ({ message, T, setLastCrumb }) => {
  const [opacity, setOpacity] = useState(0);

  useEffect(() => {
    setLastCrumb(T.t('error.page.name'));
    setTimeout(() => setOpacity(1), 0);
  }, []);

  return (
    <div
      id="error-page"
      className="container-fluid flex-col flex-center-vertical-horizontal"
      style={{ opacity, transition: 'opacity 0.5s ease-out' }}
    >
      <h2
        id="error-message"
        className="mt-3"
      >
        {message || T.t('error.notFound')}
      </h2>
      <i
        className="material-icons"
        style={{ fontSize: '384px' }}
      >
        error_outline
      </i>
    </div>
  );
};

export default Error;
