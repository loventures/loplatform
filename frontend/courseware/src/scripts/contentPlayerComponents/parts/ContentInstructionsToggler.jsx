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

import classNames from 'classnames';
import { withTranslation } from '../../i18n/translationContext.js';
import React from 'react';

class ContentInstructionsToggler extends React.Component {
  constructor(props, ...args) {
    super(props, ...args);
    this.state = {
      showInstructions: props.initiallyOpen,
    };
  }

  render() {
    const { translate, children, className } = this.props;
    const { showInstructions } = this.state;
    const chevron = showInstructions ? 'icon-chevron-down' : 'icon-chevron-right';
    return (
      <div className="content-instructions-toggled mb-3 mb-lg-4">
        <div
          className={className ?? 'h4'}
          role="button"
          tabIndex="0"
          aria-expanded={showInstructions}
          onClick={() => this.setState({ showInstructions: !showInstructions })}
          onKeyPress={evt =>
            evt.charCode === 13 && this.setState({ showInstructions: !showInstructions })
          }
        >
          <span
            className={classNames('icon d-print-none me-1', chevron)}
            aria-hidden
          />
          <span>{translate('ASSIGNMENT_INSTRUCTIONS')}</span>
        </div>
        {showInstructions && <div>{children}</div>}
      </div>
    );
  }
}

export default withTranslation(ContentInstructionsToggler);
