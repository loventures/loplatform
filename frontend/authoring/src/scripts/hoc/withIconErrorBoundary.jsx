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

import React, { Component } from 'react';

class ErrorBoundary extends Component {
  state = { error: null };

  static getDerivedStateFromError(error) {
    return { error };
  }

  componentDidCatch(error) {
    console.error(error.message, error.stack);
  }

  render() {
    return this.state.error ? <i className="material-icons">error</i> : this.props.children;
  }
}

const withIconErrorBoundary = ContentComponent => {
  const WithErrorBoundary = props => (
    <ErrorBoundary>
      <ContentComponent {...props} />
    </ErrorBoundary>
  );
  WithErrorBoundary.displayName = `WithErrorBoundary(${
    ContentComponent.displayName || ContentComponent.name
  })`;
  return WithErrorBoundary;
};

export default withIconErrorBoundary;
