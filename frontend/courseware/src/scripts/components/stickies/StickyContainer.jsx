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

import { Component } from 'react';
import { StickiesContext } from './StickiesManager';

const outerStickyStyles = ({ sticky, height }) => {
  if (sticky) {
    return {
      position: 'relative',
      height: height + 'px',
    };
  } else {
    return {};
  }
};

const innerStickyStyles = ({ sticky, order, offset }) => {
  if (sticky) {
    return {
      position: 'fixed',
      width: '100%',
      zIndex: 1050 - order, //1050 is global level
      top: offset,
    };
  } else {
    return {};
  }
};

let counter = 0;
const nextName = () => (counter += 1).toString();

class StickyContent extends Component {
  name = this.props.name || nextName();

  componentDidMount() {
    this.removeSticky = this.props.addSticky(this.name);
  }

  componentWillUnmount() {
    this.removeSticky();
  }

  render() {
    const ref = this.props.refs[this.name];
    const display = this.props.displays[this.name];

    if (!ref || !display) {
      return this.props.children;
    }

    return (
      <div
        className={display.sticky ? 'sticky-container-active' : ''}
        ref={ref}
        style={outerStickyStyles(display)}
      >
        <div style={innerStickyStyles(display)}>{this.props.children}</div>
      </div>
    );
  }
}

const StickyContainer = ({ name = null, children }) => (
  <StickiesContext.Consumer>
    {({ displays, refs, addSticky, removeSticky }) => (
      <StickyContent
        name={name}
        children={children}
        displays={displays}
        refs={refs}
        addSticky={addSticky}
        removeSticky={removeSticky}
      />
    )}
  </StickiesContext.Consumer>
);

export default StickyContainer;
