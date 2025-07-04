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

import React from 'react';

import LoPropTypes from '../react/loPropTypes';
import Octo from './Octo';

class ClusterLäbel extends React.Component {
  render() {
    return (
      <div
        className={this.props.lo_platform.isProduction ? 'prod-cluster' : 'dev-cluster'}
        id="cluster"
      >
        <a
          id="cluster-anchor"
          href="/Administration"
        >
          {this.props.lo_platform.clusterType} &ndash; {this.props.lo_platform.domain.name}
        </a>
        <Octo />
      </div>
    );
  }
}

ClusterLäbel.propTypes = {
  lo_platform: LoPropTypes.lo_platform,
};

export default ClusterLäbel;
