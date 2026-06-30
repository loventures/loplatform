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

import React, { Children, useState } from 'react';
import { Card, CardBody, CardHeader, Collapse } from 'reactstrap';

interface AccordionItemProps {
  title: string;
  children?: React.ReactNode;
  expanded?: boolean;
  toggle?: () => void;
}

const AccordionItem: React.FC<AccordionItemProps> = ({ children, expanded, title, toggle }) => (
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

interface AccordionProps {
  children?: React.ReactNode;
}

interface AccordionComponent extends React.FC<AccordionProps> {
  Item: typeof AccordionItem;
}

const Accordion: AccordionComponent = ({ children }) => {
  const [openChild, setOpenChild] = useState<React.Key | null>(null);

  const toggle = (newOpenChild: React.Key | null) => () => {
    setOpenChild(openChild !== newOpenChild ? newOpenChild : null);
  };

  const newChildren = Children.toArray(children).map(child => {
    const element = child as React.ReactElement<AccordionItemProps>;
    return React.cloneElement(element, {
      expanded: openChild === element.key,
      toggle: toggle(element.key),
    });
  });

  return <div role="tablist">{newChildren}</div>;
};

Accordion.Item = AccordionItem;

export default Accordion;
