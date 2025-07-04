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

import React from 'react';
import classNames from 'classnames';
import ContentLockPopover from './ContentLockPopover.jsx';

class ContentLockableContainer extends React.Component {
  state = {
    isFlipped: false,
    iconHover: false,
    overlayHover: false,
    tooltipHover: false,
  };

  setIconHover = show => {
    this.setState({ iconHover: show });
  };
  setOverlayHover = show => {
    this.setState({ overlayHover: show });
  };
  setTooltipHover = show => {
    this.setState({ tooltipHover: show });
  };

  render() {
    const { content, viewingAs, viewType, children } = this.props;

    if (!content.availability.isGated) {
      return <div className="content-lockable-container">{children}</div>;
    }

    const showPopover = this.state.iconHover || this.state.overlayHover || this.state.tooltipHover;

    const placement = viewType === 'card' ? 'top' : 'left';
    const containerTarget = `content-lockable-container-${content.id}`;
    const iconTarget = `content-lockable-icon-${content.id}`;
    const target = viewType === 'card' ? containerTarget : iconTarget;
    const showLockIcon =
      viewingAs.isInstructor || (content.availability.isLocked && viewType !== 'card');

    return (
      <div
        className={classNames(
          'content-lockable-container',
          viewType === 'card' && 'card-view',
          showLockIcon && 'show-lock-icon'
        )}
        id={containerTarget}
      >
        {children}

        {showLockIcon && (
          <div
            className="content-lock-icon icon-lock h3 m-0"
            role="tooltip"
            id={iconTarget}
            onMouseEnter={() => this.setIconHover(true)}
            onMouseLeave={() => this.setIconHover(false)}
          ></div>
        )}

        {content.availability.isLocked && (
          <div
            className="lock-popover-hover-area"
            role="tooltip"
            onMouseEnter={() => this.setOverlayHover(true)}
            onMouseLeave={() => this.setOverlayHover(false)}
          ></div>
        )}

        {(viewingAs.isInstructor || content.availability.isLocked) && (
          <ContentLockPopover
            content={content}
            viewingAs={viewingAs}
            target={target}
            placement={placement}
            showPopover={showPopover}
            setTooltipHover={this.setTooltipHover}
          ></ContentLockPopover>
        )}
      </div>
    );
  }
}

export default ContentLockableContainer;
