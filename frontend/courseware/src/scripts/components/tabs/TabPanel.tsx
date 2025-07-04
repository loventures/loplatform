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

import React, { useEffect, useRef } from 'react';

import { tabsContext } from './Tabs';

const PanelContent: React.FC<{ isActive: boolean } & React.PropsWithChildren> = ({
  isActive,
  children,
}) => {
  const wasActive = useRef(isActive);
  useEffect(() => {
    if (isActive) wasActive.current = true;
  }, [isActive]);
  return isActive || wasActive.current ? <>{children}</> : null;
};

const TabPanel: React.FC<{ tabId: string } & React.HTMLAttributes<HTMLDivElement>> = ({
  tabId,
  children,
  ...attributes
}) => (
  <tabsContext.Consumer>
    {({ activeTab }) => {
      const isActive = activeTab === tabId;
      return (
        <div
          {...attributes}
          id={tabId + '-tabpanel'}
          aria-labelledby={tabId + '-tab'}
          hidden={!isActive}
          aria-hidden={!isActive}
          role="tabpanel"
          tabIndex={0}
        >
          <PanelContent
            isActive={isActive}
            children={children}
          />
        </div>
      );
    }}
  </tabsContext.Consumer>
);

export default TabPanel;
