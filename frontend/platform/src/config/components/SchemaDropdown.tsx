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

import classnames from 'classnames';
import { Link } from 'react-router-dom';
import { Nav, NavItem } from 'reactstrap';

import { Schemata } from '../configApi';

interface SchemaDropdownProps {
  schemata: Schemata;
  current: string;
  path: string;
  search?: string;
}

const SchemaDropdown = ({ schemata, current, path, search }: SchemaDropdownProps) => {
  const renderItem = (key: string) => {
    const sch = schemata[key];
    return (
      <NavItem key={key}>
        <Link
          to={{ pathname: path + '/' + key, search }}
          id={`config-${key}`}
          className={classnames({ 'nav-link': true, active: key === current })}
        >
          {sch.title || key}
        </Link>
      </NavItem>
    );
  };

  return (
    <Nav
      pills
      className="schema-dropdown flex-col"
    >
      {Object.keys(schemata).map(renderItem)}
    </Nav>
  );
};

export default SchemaDropdown;
