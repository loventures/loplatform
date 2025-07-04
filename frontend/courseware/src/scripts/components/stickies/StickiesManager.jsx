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

/*
  So we have (potentially) 5 stickies situations.
  1. a system bar, e.g. anouncements
  2. the stickiness required for page header
  3. the stickiness required for review period banner
  4. the stickiness for discussion threads

  And instead of trying to come up with clever ploys,
  there is just going to be this global thing
  that manages all of them
*/

import { reduce, isEqual, without } from 'lodash';
import { createRef, createContext, Component } from 'react';

export const StickiesContext = createContext({
  displays: {},
  refs: {},
  orderedStickies: [],
  addSticky: () => () => {},
});

class StickiesManager extends Component {
  state = {
    displays: {},
    refs: {},
    orderedStickies: [],
    addSticky: name => {
      this.addSticky(name);
      return () => this.removeSticky(name);
    },
  };

  /* eslint-disable react/no-direct-mutation-state */
  /*
    multiple instances of the sticky container
    may try to subscribe at the same time
    and normal setState calls will overwrite eachother
  */
  addSticky = name => {
    this.state.orderedStickies.push(name);
    this.state.refs[name] = createRef();
    this.state.displays[name] = {
      sticky: false,
      order: this.state.orderedStickies.length,
    };
    this.forceUpdate();
  };

  removeSticky = name => {
    this.state.orderedStickies = without(this.state.orderedStickies, name);
    delete this.state.refs[name];
    delete this.state.displays[name];
    this.forceUpdate();
  };
  /* eslint-enable react/no-direct-mutation-state */

  componentDidMount() {
    window.addEventListener('scroll', this.scrollListener);
    window.addEventListener('load', this.scrollListener);
  }

  componentWillUnmount() {
    window.removeEventListener('scroll', this.scrollListener);
    window.removeEventListener('load', this.scrollListener);
  }

  scrollListener = () => {
    window.requestAnimationFrame(() => {
      this.recalculateStickies();
    });
  };

  recalculateStickies = () => {
    const { displays } = reduce(
      this.state.orderedStickies,
      (cumulative, name) => {
        const current = this.state.refs[name].current;
        const display = this.state.displays[name];

        if (current && current.getBoundingClientRect().top < cumulative.offset) {
          cumulative.displays[name] = {
            ...display,
            sticky: true,
            offset: cumulative.offset,
            height: current.children[0].getBoundingClientRect().height,
          };
          cumulative.offset += cumulative.displays[name].height;
        } else {
          cumulative.displays[name] = {
            ...display,
            sticky: false,
          };
        }
        return cumulative;
      },
      { displays: {}, offset: 0 }
    );

    if (!isEqual(this.state.displays, displays)) {
      this.setState({ ...this.state, displays });
    }
  };

  render() {
    return (
      <StickiesContext.Provider value={this.state}>{this.props.children}</StickiesContext.Provider>
    );
  }
}

export default StickiesManager;
