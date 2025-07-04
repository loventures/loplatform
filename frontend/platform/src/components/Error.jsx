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

class Error extends React.Component {
  state = {
    opacity: 0,
  };

  componentDidMount() {
    const { setLastCrumb, T } = this.props;
    setLastCrumb(T.t('error.page.name'));
    setTimeout(() => this.setState({ opacity: 1 }), 0);
  }

  render() {
    const {
      props: { message, T },
      state: { opacity },
    } = this;
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
  }
}

Error.propTypes = {
  T: PropTypes.object.isRequired,
  setLastCrumb: PropTypes.func.isRequired,
  message: PropTypes.string,
};

export default Error;
