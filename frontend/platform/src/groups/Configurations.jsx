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

import axios from 'axios';
import PropTypes from 'prop-types';
import React from 'react';

import ConfigApp from '../config/components/App';
import { getSchemata } from '../config/configApi';
import { Alert } from 'reactstrap';

class Configurations extends React.Component {
  state = {
    schemata: {},
    loaded: false,
  };

  componentDidMount() {
    const { match, setLastCrumb, T, controllerValue } = this.props;
    const { courseId } = match.params;
    axios.get(`/api/v2/${controllerValue}/${courseId}`).then(res => {
      setLastCrumb(T.t(`adminPage.${controllerValue}.configurations.name`, res.data));
    });
    getSchemata().then(({ data: { coursePreferences } }) => {
      this.setState({
        schemata: { coursePreferences },
        loaded: true,
      });
    });
  }
  render() {
    const {
      match: {
        path,
        params: { courseId },
      },
      location: { search },
      warning,
    } = this.props;
    const { loaded, schemata } = this.state;
    return (
      loaded && (
        <div className="container">
          {warning && <Alert color="warning">{warning}</Alert>}
          <ConfigApp
            schema="coursePreferences"
            schemata={schemata}
            path={path}
            search={search}
            item={parseInt(courseId, 10)}
          />
        </div>
      )
    );
  }
}

Configurations.propTypes = {
  T: PropTypes.object.isRequired,
  controllerValue: PropTypes.string.isRequired,
  match: PropTypes.object.isRequired,
  setLastCrumb: PropTypes.func.isRequired,
  warning: PropTypes.string,
};

export default Configurations;
