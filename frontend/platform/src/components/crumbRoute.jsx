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
import { Breadcrumb } from 'react-breadcrumbs';
import DocumentTitle from 'react-document-title';
import { Route } from 'react-router-dom';

class CrumbRoute extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      title: props.title || '',
      documentTitle: props.documentTitle || '',
    };
  }

  setLastCrumb = (title, documentTitle) => {
    this.setState({ title, documentTitle });
  };

  render() {
    const { includeSearch, render } = this.props;
    const { component: Component, ...rest } = this.props;
    const { title, documentTitle } = this.state;
    return (
      <Route
        {...rest}
        render={routeProps => (
          <DocumentTitle title={documentTitle || title}>
            <Breadcrumb
              hidden={!title}
              data={{
                title: title,
                pathname: routeProps.match.url,
                search: includeSearch ? routeProps.location.search : null,
              }}
            >
              {Component ? (
                <Component
                  {...routeProps}
                  setLastCrumb={this.setLastCrumb}
                />
              ) : (
                render({ ...routeProps, setLastCrumb: this.setLastCrumb })
              )}
            </Breadcrumb>
          </DocumentTitle>
        )}
      />
    );
  }
}

CrumbRoute.propTypes = {
  includeSearch: PropTypes.bool,
  render: PropTypes.func,
  title: PropTypes.string,
  documentTitle: PropTypes.string,
  component: PropTypes.object,
};

export default CrumbRoute;
