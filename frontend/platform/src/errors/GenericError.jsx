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

import NavigationBar from '../components/navigationBar';
import FakeCrumb from './FakeCrumb';

class GenericError extends React.Component {
  state = {
    opacity: 0,
  };

  componentDidMount() {
    setTimeout(() => this.setState({ opacity: 1 }), 0);
  }

  render() {
    const {
      props: {
        T,
        lo_platform: {
          domain: { appearance },
        },
        error,
      },
      state: { opacity },
    } = this;
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
  }
}

GenericError.propTypes = {
  T: PropTypes.object.isRequired,
  lo_platform: PropTypes.object.isRequired,
  error: PropTypes.string.isRequired,
};

export default GenericError;
