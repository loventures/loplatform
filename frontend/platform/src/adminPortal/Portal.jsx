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
import { connect } from 'react-redux';

import AdminPageWidget from '../components/adminPageWidget';
import { clearSavedTableState } from '../components/reactTable/ReactTable';
import LoPropTypes from '../react/loPropTypes';
import getAvailableAdminPages from './pages';

class Portal extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      adminPages: [],
      loaded: false,
    };
    // TODO: undo this evil hack when we have a reactfous overlorde
    if (this.props.lo_platform.domain.type === 'overlord') {
      document.location.href = '/';
    }
  }

  addReactPagesToAdminPages = (rightfulReactAdminPages, adminPagesObj) => {
    const T = this.props.translations;
    for (const page of rightfulReactAdminPages) {
      const newPage = {
        ...page,
        name: T.t(`adminPage.${page.identifier}.name`),
        description: T.t(`adminPage.${page.identifier}.description`),
      };
      if (adminPagesObj[page.group]) {
        adminPagesObj[page.group].push(newPage);
      } else {
        adminPagesObj[page.group] = [newPage];
      }
    }
  };

  sortAndAddHeader = adminPagesObj => {
    const T = this.props.translations;
    Object.keys(adminPagesObj).forEach(key => {
      adminPagesObj[key].sort((page1, page2) => {
        const str1 = page1.name.toUpperCase();
        const str2 = page2.name.toUpperCase();
        if (str1 < str2) return -1;
        else if (str1 > str2) return 1;
        else return 0;
      });
      adminPagesObj[key].unshift(
        <h2
          key={`${key}.title`}
          className="group-header"
        >
          {T.t(`adminSection.${key}.name`)}
        </h2>
      );
    });
  };

  flattenAdminPagesObj = adminPagesObj => {
    return Object.keys(adminPagesObj)
      .sort()
      .reduce((array, key) => {
        const pages = adminPagesObj[key].map(this.mapToAdminPageWidget);
        return array.concat(
          <div
            key={key}
            className="admin-group"
          >
            {pages}
          </div>
        );
      }, []);
  };

  mapToAdminPageWidget = page => {
    if (page.identifier) {
      const identifier = page.identifier.split('.').slice(-1)[0];
      return (
        <AdminPageWidget
          key={identifier}
          identifier={identifier}
          icon={page.icon}
          iconName={page.iconName}
          href={page.href}
          link={page.link || `/${identifier}`}
          title={page.name}
          description={page.description}
          entity={page.entity}
        />
      );
    } else {
      return page;
    }
  };

  // If a react page "replaces" a legacy page then it only shows up if the legacy page component is disabled.
  // If a react page defines an `enabled` property then it must be true, but this is a minor hack.
  filterEnabled = (adminPagesObj, page) => {
    const group = adminPagesObj[page.group];
    const enabled = !page.hasOwnProperty('enabled') || page.enabled;
    return enabled && (!group || !group.find(legacy => legacy.identifier === page.replaces));
  };

  componentDidMount() {
    const adminPagesObj = JSON.parse(JSON.stringify(this.props.adminPages));
    const rightfulReactAdminPages = getAvailableAdminPages()
      .map(page => page.pageInfo)
      .filter(page => this.filterEnabled(adminPagesObj, page));
    this.addReactPagesToAdminPages(rightfulReactAdminPages, adminPagesObj);
    this.sortAndAddHeader(adminPagesObj);
    const adminPages = this.flattenAdminPagesObj(adminPagesObj);
    // If we deep link to the admin portal with a query string then clear any saved table state
    // so if we launch in from authoring we don't accidentally remember previous filtering stuff
    if (document.location.search) clearSavedTableState(); // wow, really?
    this.setState({ adminPages, loaded: true });
  }

  render() {
    const T = this.props.translations;
    if (this.state.adminPages.length === 0) {
      return (
        <div className="container-fluid">
          <h1 className="text-align-center">{T.t('adminPortal.noAdminPagesAvailable')}</h1>
        </div>
      );
    }
    return (
      <div
        id="adminPortal"
        className="container-fluid"
      >
        {this.state.loaded && (
          <div className="row">
            <div className="col admin-columns">{this.state.adminPages}</div>
          </div>
        )}
      </div>
    );
  }
}

Portal.propTypes = {
  adminPages: PropTypes.object.isRequired,
  lo_platform: LoPropTypes.lo_platform,
  translations: LoPropTypes.translations,
};

// Which part of the Redux global state does our component want to receive as props?
function mapStateToProps(state) {
  return {
    translations: state.main.translations,
    lo_platform: state.main.lo_platform,
  };
}

export default connect(mapStateToProps, null)(Portal);
