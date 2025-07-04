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

import { useState } from 'react';
import { Card, CardHeader, Collapse, Button } from 'reactstrap';

let idCounter = 1;

const CollapseCard = ({
  id = 'collapse-card-' + idCounter++,
  className = '',
  headerClassName = 'bg-primary',
  title = '',
  initiallyOpen = true,
  renderHeader = () => <span className="flex-col-fluid word-wrap-all">{title}</span>,
  children,
}) => {
  const [isOpen, setIsOpen] = useState(initiallyOpen);
  return (
    <Card className={className}>
      <CardHeader
        tag={Button}
        className={headerClassName}
        color="primary"
        onClick={() => setIsOpen(!isOpen)}
        aria-expanded={isOpen}
        aria-controls={id}
      >
        <div className="flex-row-content">
          <span
            className="icon-chevron-right"
            style={{
              transition: 'all .3s',
              transform: isOpen ? 'rotate(90deg)' : 'rotate(0deg)',
            }}
            role="presentation"
          />
          {renderHeader(isOpen)}
        </div>
      </CardHeader>
      <Collapse
        id={id}
        isOpen={isOpen}
      >
        {children}
      </Collapse>
    </Card>
  );
};

export default CollapseCard;
