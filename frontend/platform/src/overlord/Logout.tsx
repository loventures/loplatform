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

import axios from 'axios';
import React from 'react';
import { Button } from 'reactstrap';

import { clearSavedTableState } from '../components/reactTable/ReactTable';
import { LogoutUrl } from '../services/URLs';

const Logout: React.FC = () => {
  const onLogout = () => {
    clearSavedTableState();
    axios.post(LogoutUrl, {}).then(() => {
      document.body.classList.add('off');
      setTimeout(() => (window.location.href = '/'), 2000);
    });
  };

  return (
    <Button
      color="transparent"
      onClick={onLogout}
      className="glyphButton"
    >
      <i
        className="material-icons md-16"
        aria-hidden="true"
      >
        power_settings_new
      </i>
    </Button>
  );
};

export default Logout;
