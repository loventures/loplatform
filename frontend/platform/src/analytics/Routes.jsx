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

import Polyglot from 'node-polyglot';
import React from 'react';
import { connect } from 'react-redux';
import { Switch, withRouter } from 'react-router-dom';
import { bindActionCreators } from 'redux';

import CrumbRoute from '../components/crumbRoute';
import NavigationBar from '../components/navigationBar';
import LoginRequired from '../errors/LoginRequired';
import EtcLoading from '../etc/EtcLoading';
import * as MainActions from '../redux/actions/MainActions';
import { isDevelopment, getPlatform, getTranslations } from '../services';
import MetabaseEmbed from './MetabaseEmbed';

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
        // for /etc/ pretend the api key user is not logged in
        const { user, ...lop } = platformRes.data;
        const loplat =
          isDevelopment && user && user.user_type === 'System' ? lop : { user, ...lop };
        this.props.setLoPlatform(loplat);
        this.setState({ loaded: true });
      }
    );
  }

  render() {
    const {
      state: { loaded },
      props: { translations: T, lo_platform: lop, setLoPlatform },
    } = this;
    if (!loaded) {
      return <EtcLoading />;
    } else if (!lop.user) {
      return (
        <LoginRequired
          T={T}
          lo_platform={lop}
          setLoPlatform={setLoPlatform}
        />
      );
    } else {
      const {
        domain: { name },
      } = lop;
      return (
        <React.Fragment>
          <NavigationBar
            nonAdmin
            domainApp
          />
          <Switch>
            <CrumbRoute
              title={T.t('page.analytics.name')}
              documentTitle={`${name} - ${T.t('page.analytics.name')}`}
              path="/:embedType/:id"
              render={props => (
                <MetabaseEmbed
                  {...props}
                  T={T}
                  lop={lop}
                />
              )}
            />
          </Switch>
        </React.Fragment>
      );
    }
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
