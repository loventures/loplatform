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

class LegacyIframe extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      loaded: false,
    };
  }

  componentDidMount() {
    this.ifr.onload = () => {
      this.setState({ loaded: true });
    };
  }

  render() {
    const { loaded } = this.state;
    const { src, location, params, title } = this.props;

    const qs = Object.keys(params)
      .map(key => `${encodeURIComponent(key)}=${encodeURIComponent(params[key])}`)
      .join('&');

    const iframeURL = `${src}${(location && location.search) || '?'}&${qs}`;

    return (
      <iframe
        id="legacy-frame"
        title={title}
        className={loaded ? 'show-me' : 'hide-me'}
        src={iframeURL}
        ref={f => {
          this.ifr = f;
        }}
      />
    );
  }
}

LegacyIframe.propTypes = {
  params: PropTypes.object,
  src: PropTypes.string.isRequired,
  title: PropTypes.string.isRequired,
};

LegacyIframe.defaultProps = {
  params: {},
};

export default LegacyIframe;
