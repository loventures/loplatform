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
import React, { Children, Component, PureComponent } from 'react';
import { Card, CardBody, CardHeader, Collapse } from 'reactstrap';

class Accordion extends Component {
  constructor(props) {
    super(props);
    this.state = {
      openChild: null,
    };
  }

  _toggle = newOpenChild => () => {
    this.setState({
      openChild: this.state.openChild !== newOpenChild && newOpenChild,
    });
  };

  render() {
    const { children } = this.props;
    const { openChild } = this.state;

    const newChildren = Children.toArray(children).map(child => {
      return React.cloneElement(child, {
        expanded: openChild === child.key,
        toggle: this._toggle(child.key),
      });
    });

    return <div role="tablist">{newChildren}</div>;
  }
}

class AccordionItem extends PureComponent {
  render() {
    const { children, expanded, title, toggle } = this.props;

    return (
      <Card>
        <CardHeader
          role="tab"
          onClick={toggle}
        >
          {title}
        </CardHeader>
        <Collapse isOpen={expanded}>
          <CardBody>{children}</CardBody>
        </Collapse>
      </Card>
    );
  }
}

Accordion.propTypes = {
  /* 💩 no propTypes yet! */
};

AccordionItem.propTypes = {
  title: PropTypes.string.isRequired,
};

Accordion.Item = AccordionItem;

export default Accordion;
