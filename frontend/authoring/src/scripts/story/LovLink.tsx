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

/* eslint-disable react/jsx-no-target-blank */

import React from 'react';

import { LOV } from './icons/LOV';

const LovLink: React.FC = () => (
  <>
    <div id="lo-copyright">
      LO Platform &copy; 2007–2025{' '}
      <a
        href="https://learningobjects.com/"
        target="_blank"
        rel="noopener"
      >
        LO Ventures LLC
      </a>
    </div>
    <a
      id="lo-link"
      className="d-none d-sm-block"
      href="https://learningobjects.com/"
      target="_blank"
      rel="noopener"
      title="Powered by LO Ventures"
      tabIndex={-1}
    >
      <LOV />
    </a>
  </>
);

export default LovLink;
