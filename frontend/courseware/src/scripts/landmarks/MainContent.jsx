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

import { createContext, createRef, Component } from 'react';

import { withErrorBoundary } from '../directives/BasicErrorBoundary';

export const MainContentRefContext = createContext(createRef());

/**
 * NOTE: I suspect this is not used except to decide between wide/skinny which is rarely used
 * */
class MainContent extends Component {
  mainContentRef = createRef();

  componentDidMount() {
    this.mainContentRef.current.focus();
  }

  render() {
    const { wide, children } = this.props;
    return (
      <div
        className={wide ? 'container-fluid-gutters' : 'container'}
        ref={this.mainContentRef}
      >
        <MainContentRefContext.Provider value={this.mainContentRef}>
          {children}
        </MainContentRefContext.Provider>
      </div>
    );
  }
}

export default withErrorBoundary(MainContent);
