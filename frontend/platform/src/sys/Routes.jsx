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
import Polyglot from 'node-polyglot';
import React from 'react';
import { connect } from 'react-redux';
import { Redirect, Route, Switch, withRouter } from 'react-router-dom';
import { bindActionCreators } from 'redux';

import CrumbRoute from '../components/crumbRoute';
import Error from '../components/Error';
import OverlordBar from '../overlord/OverlordBar';
import * as MainActions from '../redux/actions/MainActions';
import SysScript from '../script/App';
import { getPlatform, getTranslations } from '../services/';
import SysLoading from './SysLoading';

class Routes extends React.Component {
  state = { loaded: false };

  componentDidMount() {
    Promise.all([getPlatform(), getTranslations(window.locale)]).then(
      ([platformRes, translationsRes]) => {
        this.props.setTranslations(
          new Polyglot({
            locale: platformRes.data.domain.locale,
            phrases: translationsRes.data,
          })
        );
        this.props.setLoPlatform(platformRes.data);
        this.setState({ loaded: true });
      }
    );
  }

  render() {
    const {
      state: { loaded },
      props: {
        translations: T,
        history,
        lo_platform: { user, clusterType },
      },
    } = this;
    if (!loaded) return <SysLoading />;
    const page = 'Script/Scala'; // Add behaviour once we have more pages...
    return (
      <div className={classnames('overlord', clusterType, { anonymous: !user })}>
        <div className="eye right-eye"></div>
        <div className="eye left-eye"></div>
        <div className="overlorde">
          <div className="overimg"></div>
        </div>
        <OverlordBar
          page={page}
          history={history}
          simple
        />
        <Switch>
          <Route
            path="/script"
            exact
            render={() => <Redirect to="/script/scala" />}
          />
          <CrumbRoute
            title={T.t('overlord.page.Script/Scala.name')}
            path="/script/scala"
            render={props => <SysScript {...props} />}
          />
          <CrumbRoute
            title={T.t('overlord.page.Script/Scala.name')}
            path="/script/sql"
            render={props => <SysScript {...props} />}
          />
          <CrumbRoute
            title={T.t('overlord.page.Script/Scala.name')}
            path="/script/redshift"
            render={props => <SysScript {...props} />}
          />
          <CrumbRoute
            title={T.t('error.page.name')}
            render={props => (
              <Error
                {...props}
                T={T}
              />
            )}
          />
        </Switch>
      </div>
    );
  }
}

function mapStateToProps(state) {
  return {
    translations: state.main.translations,
    lo_platform: state.main.lo_platform,
  };
}

function mapDispatchToProps(dispatch) {
  return bindActionCreators(MainActions, dispatch);
}

export default withRouter(connect(mapStateToProps, mapDispatchToProps)(Routes));
