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

import PropTypes from 'prop-types';
import React from 'react';
import DocumentTitle from 'react-document-title';
import { connect } from 'react-redux';
import { bindActionCreators } from 'redux';

import * as MainActions from '../redux/actions/MainActions';
import ScrollTopAlert from './ScrollTopAlert';

const adminPagePropTypes = {
  adminPageError: PropTypes.bool,
  adminPageMessage: PropTypes.string,
  adminPageSuccess: PropTypes.bool,
  match: PropTypes.object.isRequired,
  setPortalAlertStatus: PropTypes.func.isRequired,
};

const createAdminPage = (WrappedComponent, headerProps, wrappedProps) => {
  class AdminPage extends React.Component {
    constructor(props) {
      super(props);
      this.state = { opacity: 0 };
    }

    naked = () => this.props.location.search && this.props.location.search.indexOf('naked') >= 0;

    componentDidMount() {
      if (this.naked()) {
        document.body.classList.add('naked');
      }
      setTimeout(() => this.setState({ opacity: 1 }), 0);
    }

    componentWillUnmount() {
      if (this.naked()) {
        document.body.classList.remove('naked');
      }
      this.hideAlert();
    }

    hideAlert = () => {
      this.props.setPortalAlertStatus(false, false, '');
    };

    renderAlert = () => {
      if (this.props.adminPageError || this.props.adminPageSuccess) {
        return (
          <div className="container-fluid">
            <ScrollTopAlert
              id="admin-page-alert"
              color={this.props.adminPageError ? 'warning' : 'success'}
              toggle={this.hideAlert}
            >
              {this.props.adminPageMessage}
            </ScrollTopAlert>
          </div>
        );
      }
      return null;
    };

    renderWrappedComponent = () => {
      return <WrappedComponent {...wrappedProps} />;
    };

    render() {
      const { location, match, router } = this.props;
      const { opacity } = this.state;

      wrappedProps = {
        // sad effects initialization
        ...(wrappedProps || {}),
        location,
        match,
        router,
      };
      return (
        <DocumentTitle title={headerProps.headerStr}>
          <div className={headerProps.pageClass}>
            {this.renderAlert()}
            <div
              style={{ opacity, transition: 'opacity 0.5s ease-out' }}
              role="main"
            >
              {this.renderWrappedComponent()}
            </div>
          </div>
        </DocumentTitle>
      );
    }
  }

  AdminPage.propTypes = adminPagePropTypes;

  function mapStateToProps(state) {
    return {
      adminPageError: state.main.adminPageError,
      adminPageSuccess: state.main.adminPageSuccess,
      adminPageMessage: state.main.adminPageMessage,
      translations: state.main.translations,
    };
  }

  function mapDispatchToProps(dispatch) {
    return bindActionCreators(MainActions, dispatch);
  }

  return connect(mapStateToProps, mapDispatchToProps)(AdminPage);
};

export default createAdminPage;
