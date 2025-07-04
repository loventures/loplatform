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

import { ContentLeafType } from '../propTypes.js';
import VariableContentLink from '../../parts/VariableContentLink.jsx';
import ContentLockableContainer from '../../parts/ContentLockableContainer.jsx';

import ContentDueDateAndGrade from '../../parts/ContentDueDateAndGrade.jsx';
import ContentIcon from '../../parts/ContentIcon.jsx';
import ContentSubscript from '../../parts/ContentSubscript.jsx';

const ContentLine = ({ content, viewingAs, className = '' }) => (
  <ContentLockableContainer
    content={content}
    viewingAs={viewingAs}
  >
    <div className={'content-views-line-item ' + className}>
      <div className="flex-row-content content-view-top-row">
        <ContentIcon content={content} />
        <div className="flex-col-fluid flex-row-content flex-wrap">
          <div className="flex-col-fluid no-fluid-xs due-date-effects">
            <VariableContentLink
              content={content}
              viewingAs={viewingAs}
            >
              {content.name}
            </VariableContentLink>
            &nbsp;
            <ContentSubscript content={content} />
          </div>
          <ContentDueDateAndGrade content={content} />
        </div>
      </div>
    </div>
  </ContentLockableContainer>
);

ContentLine.propTypes = ContentLeafType;

export default ContentLine;
