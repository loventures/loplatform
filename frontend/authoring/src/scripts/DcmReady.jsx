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

import gretchen from './grfetchen/';
import { fromPairs, mapValues } from 'lodash';
import PropTypes from 'prop-types';
import React, { Component } from 'react';
import { connect } from 'react-redux';
import { withRouter } from 'react-router-dom';
import { compose } from 'recompose';

import DcmApp from './DcmApp';
import { initializeStore, noBranch } from './dcmStore';
import reactRouterService from './router/ReactRouterService';
import { checkSessionTime, fetchUserSession } from './user/userActions';
import { fetchAnnouncements } from './announcement/AnnouncementActions.js';

class DcmReady extends Component {
  constructor(props) {
    super(props);

    this.state = {
      ready: false,
      domain: null,
      authoring: null,
      translations: null,
      loggedOut: false,
    };
  }

  componentDidMount() {
    this.bootstrapApplication();
  }

  componentDidUpdate(prevProps) {
    const { match } = this.props;
    if (match.params.branchId !== prevProps.match.params.branchId) {
      // const { dispatch } = this.props;
      // const branchId = parseInt(match.params.branchId, 10);
      // if (branchId > 0) {
      //   this.setState({ ready: false });
      //   gretchen
      //     .get(`/api/v2/authoring/branches/${branchId}`)
      //     .exec()
      //     .then(branch => {
      //       dispatch(updateBranch(branch));
      //       this.setState({ ready: true });
      //     });
      // } else {
      //   dispatch(updateBranch(noBranch));
      // }
      this.bootstrapApplication();
    }
  }

  bootstrapApplication() {
    const { loPlatform, match, dispatch } = this.props;

    const domainPromise = this.state.domain ?? gretchen.get('/api/v2/lo_platform').exec();
    const authoringPromise =
      this.state.authoring ?? gretchen.get('/api/v2/config/authoring').exec();
    const translationPromise =
      this.state.translations ??
      gretchen
        .get(`/api/v2/i18n/${loPlatform.i18n.locale}/${loPlatform.identifier}`)
        .exec()
        .then(translations => {
          if (process.env.NODE_ENV !== 'development') {
            return translations;
          } else {
            return getLocalI18nData(translations, 'en');
          }
        })
        .catch(() => {
          if (process.env.NODE_ENV === 'development') {
            return getLocalI18nData({}, 'en');
          }
        });
    const branchId = match.params['branchId'];
    const branchPromise = branchId
      ? gretchen.get(`/api/v2/authoring/branches/${branchId}`).exec()
      : noBranch;

    Promise.all([domainPromise, authoringPromise, translationPromise, branchPromise])
      .then(([domain, authoring, translations, branch]) => {
        this.setState({ ...this.state, domain, authoring, translations });
        dispatch(initializeStore(domain, authoring, translations, branch, loPlatform));
        if (!this.state.ready) {
          dispatch(fetchUserSession());
          dispatch(fetchAnnouncements());
          this.setState({ ready: true });
        }
        if (this.state.sessionInterval) {
          clearInterval(this.state.sessionInterval);
        }
        const sessionInterval = setInterval(() => {
          dispatch(checkSessionTime());
        }, 1000 * 60); // one minute
        this.setState({ sessionInterval });
        document.getElementById('splashOrbitals')?.remove();
      })
      .catch(e => {
        // The assumption here is that we are not logged in.
        console.error('Not logged in.', e);
        if (window.lo_platform.isDev) {
          this.setState({ loggedOut: true });
        } else {
          window.location = '/';
        }
      });
  }

  static getDerivedStateFromError() {
    return { ready: true };
  }

  componentDidCatch(err) {
    reactRouterService.goToRootError(err);
  }

  render() {
    return this.state.loggedOut ? (
      <div className="mt-4 mx-4 text-center alert alert-danger">Not logged in.</div>
    ) : this.state.ready && this.props.ready ? (
      <DcmApp />
    ) : null;
  }
}

DcmReady.propTypes = {
  loPlatform: PropTypes.object.isRequired,
  ready: PropTypes.bool.isRequired,
  match: PropTypes.object.isRequired,
  dispatch: PropTypes.func.isRequired,
};

const mapStateToProps = state => {
  return {
    ready: !!state.layout.branchId,
  };
};

export default compose(withRouter, connect(mapStateToProps))(DcmReady);

/* Private translation helper */

export const getLocalI18nData = (serverI18n, locale) => {
  try {
    /* Use dynamic imports to not pull this stuff in for the prod build */
    return Promise.all([
      require(`!!raw-loader!../../i18n/Authoring_${locale.substring(0, 2)}.csv`),
      require('csvjson'),
    ]).then(([{ default: i18nCsv }, csvjson]) => {
      try {
        const localI18n = fromPairs(csvjson.toArray(i18nCsv, { quote: '"' }));
        const expandedI18n = mapValues(localI18n, val => {
          return val.replace(/``([^`]*)``/g, (m, g) => localI18n[g]).replace(/""/g, '"');
        });
        return { ...serverI18n, ...expandedI18n };
      } catch (expandError) {
        console.error(expandError);
        throw 'Error expanding local translations. There may be incomplete translations';
      }
    });
  } catch (errWhichProbablyMeansOurLanguageIsNotThere) {
    return serverI18n;
  }
};
