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

import classNames from 'classnames';
import PropTypes from 'prop-types';
import React from 'react';

const Loadable = ({ loading, children, style, className = '', size = '40px' }) => {
  if (!loading && children) {
    return children();
  }

  return (
    <div
      className={classNames({ loading: loading }, className)}
      style={style}
    >
      <svg
        width={size}
        height={size}
        viewBox="0 0 50 50"
        version="1.1"
        className="load-bars"
      >
        <g transform="rotate(180 25 25)">
          <rect
            x="0"
            y="0"
            width="10"
            height="0"
          >
            <animate
              attributeType="XML"
              attributeName="height"
              values="10; 50; 10"
              keyTimes="0; 0.5; 1"
              begin="0"
              dur="0.8s"
              repeatCount="indefinite"
            />
          </rect>
          <rect
            x="20"
            y="0"
            width="10"
            height="0"
          >
            <animate
              attributeType="XML"
              attributeName="height"
              values="10; 50; 10"
              keyTimes="0; 0.5; 1"
              dur="0.8s"
              begin="0.1s"
              repeatCount="indefinite"
            />
          </rect>
          <rect
            x="40"
            y="0"
            width="10"
            height="0"
          >
            <animate
              attributeType="XML"
              attributeName="height"
              values="10; 50; 10"
              keyTimes="0; 0.5; 1"
              dur="0.8s"
              begin="0.2s"
              repeatCount="indefinite"
            />
          </rect>
        </g>
      </svg>
    </div>
  );
};

Loadable.propTypes = {
  loading: PropTypes.bool,
  className: PropTypes.string,
  size: PropTypes.string,
  children: PropTypes.func,
  style: PropTypes.object,
};

export default Loadable;
