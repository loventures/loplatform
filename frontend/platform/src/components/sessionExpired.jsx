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

import PropTypes from 'prop-types';
import React from 'react';

import { checkSession } from '../services/';

const OneMinute = 60000;

class SessionExpired extends React.Component {
  isSessionExpired = () =>
    checkSession().then(res => {
      if (res.valid) {
        this.schedule();
      } else if (res.err) {
        console.log(res.err);
        this.schedule();
      } else {
        this.props.onExpired();
      }
    });

  schedule = () => {
    if (this.mounted) {
      this.timeout = setTimeout(this.isSessionExpired, OneMinute);
    }
  };

  componentDidMount() {
    this.mounted = true;
    this.isSessionExpired();
  }

  componentWillUnmount() {
    this.mounted = false;
    clearTimeout(this.timeout);
  }

  render() {
    return this.props.children;
  }
}

SessionExpired.propTypes = {
  onExpired: PropTypes.func,
};

export default SessionExpired;
