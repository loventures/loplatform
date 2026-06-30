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

import { Component, ReactNode, createRef } from 'react';
import { keyCodes } from 'reactstrap/lib/utils';
import { tabsContext } from './Tabs';

(keyCodes as any).left = 37;
(keyCodes as any).right = 39;

interface TabProps {
  tabId: string;
  className?: string;
  activeClassName?: string;
  disabledClassName?: string;
  disabled?: boolean;
  children?: ReactNode;
  ref?: any;
  [key: string]: any;
}

/*
  check spec for url matching active tab
*/
class Tab extends Component<TabProps> {
  tabRef = createRef<HTMLButtonElement>();

  moveFocusToTab(direction: number) {
    const currentTab = this.tabRef.current;
    if (!currentTab) {
      return;
    }
    const tabs = currentTab.parentElement!.querySelectorAll('[role="tab"]');
    const currentIndex = [].indexOf.call(tabs, currentTab as any);
    const nextIndex = (currentIndex + direction + tabs.length) % tabs.length;
    //check spec to see what to do for disabled tab
    //check with nividan to see what is best
    (tabs[nextIndex] as HTMLElement).focus();
  }

  handleKeyDown(e: any, setActiveTab: (tabId: any) => void) {
    if (
      [keyCodes.left, keyCodes.right, keyCodes.up, keyCodes.down, keyCodes.space].indexOf(
        e.which
      ) === -1 ||
      /input|textarea/i.test(e.target.tagName)
    ) {
      return;
    }

    e.preventDefault();

    if (e.which === keyCodes.left || e.which === keyCodes.up) {
      this.moveFocusToTab(-1);
    }
    if (e.which === keyCodes.right || e.which === keyCodes.down) {
      this.moveFocusToTab(1);
    }

    if (this.props.disabled) return;

    if (e.which === keyCodes.space) {
      setActiveTab(this.props.tabId);
    }
  }

  render() {
    const {
      tabId,
      className,
      activeClassName = 'active',
      disabledClassName = 'disabled',
      disabled,
      children,
      ref = this.tabRef,
      ...attributes
    } = this.props;

    return (
      <tabsContext.Consumer>
        {({ activeTab, setActiveTab }) => {
          const isActive = activeTab === tabId;
          return (
            <button
              className={
                className +
                ' ' +
                (isActive ? activeClassName : '') +
                ' ' +
                (disabled ? disabledClassName : '')
              }
              id={tabId + '-tab'}
              aria-controls={tabId + '-tabpanel'}
              ref={ref}
              {...attributes}
              role="tab"
              disabled={disabled}
              tabIndex={isActive ? 0 : -1}
              onKeyDown={e => this.handleKeyDown(e, setActiveTab)}
              onClick={() => setActiveTab(tabId)}
            >
              {children}
            </button>
          );
        }}
      </tabsContext.Consumer>
    );
  }
}

export default Tab;
