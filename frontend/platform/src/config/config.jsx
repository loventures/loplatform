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
import { Redirect, Route, Switch } from 'react-router-dom';

import App from './components/App';
import { getSchemata } from './configApi';

class Config extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      schemata: {},
      loaded: false,
    };
  }

  componentDidMount() {
    getSchemata().then(res => {
      this.setState({ schemata: res.data, loaded: true });
    });
  }

  render = () => {
    const {
      match: { path },
      location: { search },
    } = this.props;
    return (
      <div className="container-fluid">
        {this.state.loaded && (
          <Switch>
            <Route
              path={path + '/:schema'}
              render={props => (
                <App
                  key={props.match.params.schema}
                  schema={props.match.params.schema}
                  schemata={this.state.schemata}
                  path={path}
                  search={search}
                />
              )}
            />
            <Route
              render={() => (
                <Redirect
                  to={{ pathname: path + '/' + Object.keys(this.state.schemata)[0], search }}
                />
              )}
            />
          </Switch>
        )}
      </div>
    );
  };
}

Config.propTypes = {};

export default Config;
