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
import { Redirect, Route, Switch, withRouter } from 'react-router-dom';
import { bindActionCreators } from 'redux';

import CrumbRoute from '../components/crumbRoute';
import NavigationBar from '../components/navigationBar';
import LoginRequired from '../errors/LoginRequired';
import * as MainActions from '../redux/actions/MainActions';
import { getPlatform, getTranslations } from '../services';
import CourseList from './CourseList';
import Profile from './Profile';
import { Nav, Navbar } from 'reactstrap';

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
        document.getElementById('domain-loading')?.remove();
        this.setState({ loaded: true });
      }
    );
  }

  render() {
    const {
      state: { loaded },
      props: { translations: T, lo_platform: lop, setLoPlatform },
    } = this;
    const {
      domain: { name, image },
      user,
    } = lop;
    return !loaded ? (
      <>
        {image && (
          <img
            className="domain-image fade"
            src={image.url}
            alt={name}
            aria-hidden
          />
        )}
        {user && (
          <div id="main-nav-bar">
            <Navbar
              id="main-nav-bar-base"
              light
              expand
              className="navbar-toggleable-xl px-3"
            >
              <Nav />
            </Navbar>
          </div>
        )}
      </>
    ) : (
      <React.Fragment>
        {image && (
          <img
            className="domain-image fade"
            src={image.url}
            alt={name}
            aria-hidden
          />
        )}
        {user ? (
          <React.Fragment>
            <NavigationBar
              nonAdmin
              domainApp
            />
            <Switch>
              <CrumbRoute
                key="courseList"
                title={T.t('page.courseList.name')}
                documentTitle={`${name} - ${T.t('page.courseList.name')}`}
                path="/"
                exact
                component={CourseList}
              />
              <CrumbRoute
                key="profile"
                title={T.t('page.profile.name')}
                documentTitle={`${name} - ${T.t('page.profile.name')}`}
                path="/Profile"
                exact
                component={Profile}
              />
              <Route render={() => <Redirect to="/" />} />
            </Switch>
          </React.Fragment>
        ) : (
          <LoginRequired
            T={T}
            lo_platform={lop}
            setLoPlatform={setLoPlatform}
          />
        )}
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
