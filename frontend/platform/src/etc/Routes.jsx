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
import { Breadcrumbs } from 'react-breadcrumbs';
import { connect } from 'react-redux';
import { Switch, withRouter } from 'react-router-dom';
import { bindActionCreators } from 'redux';

import CrumbRoute from '../components/crumbRoute';
import Error from '../components/Error';
import NavigationBar from '../components/navigationBar';
import Localmail from '../localmail';
import * as MainActions from '../redux/actions/MainActions';
import { isDevelopment } from '../services';
import { getPlatform, getTranslations } from '../services/';
import About from './About';
import EtcLoading from './EtcLoading';
import LoginRegister from './loginRegister';
import ResetPassword from './ResetPassword';

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
      props: { translations: T, lo_platform: lop },
    } = this;
    const naked = window.location.search.indexOf('naked') >= 0;
    return !loaded ? (
      <EtcLoading />
    ) : (
      <React.Fragment>
        {naked || (
          <React.Fragment>
            <NavigationBar nonAdmin />
            <Breadcrumbs
              className="breadcrumb admin-breadcrumb"
              separator="/"
            />
          </React.Fragment>
        )}
        <Switch>
          <CrumbRoute
            title={T.t('page.resetPassword.name')}
            path="/ResetPassword/:token"
            exact
            render={props => (
              <ResetPassword
                {...props}
                T={T}
              />
            )}
          />
          <CrumbRoute
            title={T.t('page.register.name')}
            path="/LoginRegister"
            exact
            render={props => (
              <LoginRegister
                {...props}
                T={T}
                lop={lop}
              />
            )}
          />
          <CrumbRoute
            title={T.t('about.page.name')}
            path="/About"
            exact
            render={props => (
              <About
                {...props}
                naked={naked}
                T={T}
              />
            )}
          />
          <CrumbRoute
            title={T.t('page.localmail.name')}
            path="/Localmail"
            exact
            render={props => (
              <Localmail
                {...props}
                T={T}
                lop={lop}
              />
            )}
          />
          <CrumbRoute
            title={T.t('page.localmail.name')}
            path="/Localmail/:account"
            exact
            render={props => (
              <Localmail
                {...props}
                T={T}
                lop={lop}
              />
            )}
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
      </React.Fragment>
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
