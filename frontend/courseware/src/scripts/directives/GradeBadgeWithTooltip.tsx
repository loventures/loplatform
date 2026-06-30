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

import classNames from 'classnames';
import GradeBadge from './GradeBadge';
import { Component, createRef } from 'react';
import { UncontrolledTooltip as Tooltip } from 'reactstrap';

interface GradeBadgeWithTooltipProps {
  grade?: any;
  tooltipText?: any;
  className?: string;
  tooltipPlacement?: any;
  tooltipColor?: string;
  [key: string]: any;
}

class GradeBadgeWithTooltip extends Component<GradeBadgeWithTooltipProps> {
  badgeRef = createRef<any>();

  render() {
    const { grade, tooltipText, className, tooltipPlacement, tooltipColor } = this.props;

    return (
      <div className="grade-badge-with-tooltip">
        <div ref={this.badgeRef}>
          {grade ? (
            <GradeBadge {...this.props} />
          ) : (
            <span
              role="presentation"
              className={classNames(className, 'material-icons pending-grade')}
            >
              pending_actions
            </span>
          )}
        </div>
        <Tooltip
          className={`tooltip-${tooltipColor ?? 'info'}`}
          placement={tooltipPlacement ?? 'left'}
          target={(() => this.badgeRef.current) as any}
          container={(() => this.badgeRef.current) as any}
        >
          <span className="grade-badge-tooltip-text">{tooltipText}</span>
        </Tooltip>
      </div>
    );
  }
}

export default GradeBadgeWithTooltip;
