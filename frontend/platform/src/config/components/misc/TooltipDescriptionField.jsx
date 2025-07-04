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

import React, { useState } from 'react';
import { Tooltip } from 'reactstrap';

const TooltipDescriptionField = ({ id, description }) => {
  const [tooltipOpen, setTooltipOpen] = useState(false);
  const toggleTooltipOpen = () => setTooltipOpen(!tooltipOpen);

  if (!description) return null;

  return (
    <>
      <i
        id={id}
        className="material-icons config-help pe-1"
      >
        help_outline
      </i>
      <Tooltip
        placement="bottom"
        isOpen={tooltipOpen}
        target={id}
        toggle={toggleTooltipOpen}
      >
        {description}
      </Tooltip>
    </>
  );
};

export default TooltipDescriptionField;
