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

import React, { Fragment } from 'react';
import { connect } from 'react-redux';

import Navigation from './nav/Navigation';

const ErrorDcm = ({ polyglot, header = true }) => (
  <Fragment>
    {header && <Navigation />}
    <div className="container p-5 error-stuff">
      <h1>{polyglot.t('ERROR_PAGE_TITLE')}</h1>
      <p>{polyglot.t('ERROR_PAGE_TITLE_TEXT')}</p>
      <p>{polyglot.t('ERROR_PAGE_TITLE_CONTACT')}</p>
    </div>
  </Fragment>
);

const backupTranslations = {
  ERROR_PAGE_TITLE: "We're sorry. An error has occurred.",
  ERROR_PAGE_TITLE_CONTACT: 'If the problem persists, please contact the system administrator.',
  ERROR_PAGE_TITLE_TEXT: 'Please navigate back home and try again.',
};

const mapStateToProps = state => {
  if (
    state.configuration &&
    state.configuration.translations &&
    typeof state.configuration.translations.t === 'function'
  ) {
    return {
      polyglot: state.configuration.translations,
    };
  } else {
    return {
      polyglot: { t: key => backupTranslations[key] || key },
    };
  }
};

export default connect(mapStateToProps)(ErrorDcm);
