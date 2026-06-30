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

import React, { useState } from 'react';

import { useTranslation } from '../../i18n/translationContext.tsx';

/**
 * React port of the `gradingPanelSection` Angular component: a collapsible card with a clickable header
 * (title + chevron) and a body holding the section content. DOM preserved from gradingPanelSection.html
 * for the Selenide grader page objects: `section.card.grading-panel-section` > `.card-header` + a
 * `.card-body` holding the children. The `uib-collapse` animation becomes a display toggle; the children
 * stay mounted (so their React state survives a collapse) and are hidden when collapsed.
 */
export const GradingPanelSection: React.FC<{
  sectionTitle: string;
  description: string;
  children?: React.ReactNode;
}> = ({ sectionTitle, description, children }) => {
  const translate = useTranslation();
  const [collapsed, setCollapsed] = useState(false);

  return (
    <section className="card grading-panel-section border-bottom-0">
      <header
        className="card-header"
        onClick={() => setCollapsed(c => !c)}
      >
        <div className="flex-row-content">
          <span
            className="flex-col-fluid"
            title={translate(description)}
          >
            {translate(sectionTitle)}
          </span>
          <span className={`icon ${collapsed ? 'icon-chevron-down' : 'icon-chevron-up'}`} />
        </div>
      </header>

      <div
        className="card-body"
        style={collapsed ? { display: 'none' } : undefined}
      >
        {children}
      </div>
    </section>
  );
};

export default GradingPanelSection;
