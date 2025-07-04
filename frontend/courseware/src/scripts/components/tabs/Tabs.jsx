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

import { Component, PureComponent, createContext } from 'react';

export const tabsContext = createContext({
  activeTab: null,
  setActiveTab: () => {
    throw 'No valid context';
  },
});

export class UncontrolledTabs extends Component {
  state = {
    activeTab: null,
  };

  setActiveTab = activeTab => this.setState({ activeTab });

  render() {
    return (
      <Tabs
        activeTab={this.state.activeTab}
        setActiveTab={this.setActiveTab}
      />
    );
  }
}

class Tabs extends PureComponent {
  render() {
    return (
      <tabsContext.Provider value={this.props}>
        <div className={this.props.className}>{this.props.children}</div>
      </tabsContext.Provider>
    );
  }
}

export default Tabs;
