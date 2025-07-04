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

import { useCourseSelector } from '../loRedux';
import { selectActualUser } from '../utilities/rootSelectors';
import React from 'react';

const spaceRegex = / /g;

const ClientScriptingContext: React.FC = () => {
  const actualUser = useCourseSelector(selectActualUser);
  const rolesClassName = actualUser.roles
    .map(role => `role-${role.replace(spaceRegex, '_')}`)
    .join(' ');
  return (
    <div
      id="client-scripting-context"
      className="d-none"
    >
      <div
        id="csc-roles"
        className={rolesClassName}
      ></div>
    </div>
  );
};

export default ClientScriptingContext;
