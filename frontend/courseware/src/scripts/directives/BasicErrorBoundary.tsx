/*
 * LO Platform copyright (C) 2007–2026 LO Ventures LLC.
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

import { Component, ComponentType, PropsWithChildren } from 'react';

interface BasicErrorBoundaryState {
  error: Error | null;
}

class BasicErrorBoundary extends Component<PropsWithChildren, BasicErrorBoundaryState> {
  state: BasicErrorBoundaryState = { error: null };

  componentDidCatch(error: Error) {
    console.error(error.message);
    console.error(error.stack);
    this.setState({ error });
  }

  render() {
    if (this.state.error) {
      return (
        <div className="alert alert-danger">
          <div>Error</div>
          <p>{this.state.error.stack}</p>
        </div>
      );
    } else {
      return this.props.children;
    }
  }
}

export default BasicErrorBoundary;

export const withErrorBoundary = (ContentComponent: ComponentType<any>) => {
  const WithErrorBoundary = (props: any) => (
    <BasicErrorBoundary>
      <ContentComponent {...props} />
    </BasicErrorBoundary>
  );
  WithErrorBoundary.displayName = `WithErrorBoundary(${
    ContentComponent.displayName || ContentComponent.name
  })`;
  return WithErrorBoundary;
};
