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

import classnames from 'classnames';
import React, { Component } from 'react';
import { Link } from 'react-router-dom';
import { Nav, NavItem } from 'reactstrap';

export default class SchemaDropdown extends Component {
  //= ({ schemata, current, onChange }) => {
  render() {
    const { schemata } = this.props;
    return (
      <Nav
        pills
        className="schema-dropdown flex-col"
      >
        {Object.keys(schemata).map(this.renderItem)}
      </Nav>
    );
  }

  renderItem = key => {
    const { current, path, search, schemata } = this.props;
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
}
