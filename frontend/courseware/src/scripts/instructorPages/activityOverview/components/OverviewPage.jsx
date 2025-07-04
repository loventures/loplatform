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

import DueDateBadge from '../../../contentPlayerComponents/parts/DueDateBadge';
import { map } from 'lodash';
import { withTranslation } from '../../../i18n/translationContext';
import { Component } from 'react';
import { connect } from 'react-redux';

import { selectActivityOverviewPageComponent } from '../services/selectors';
import OverviewListItem from './OverviewListItem';
import OverviewModal from './OverviewModal';

class OverviewPage extends Component {
  render() {
    const {
      translate,
      content,
      isObservation,
      listProps,
      withWork: withWorkItems,
      withoutWork: withoutWorkItems,
      WithWorkList,
      WithoutWorkList,
    } = this.props;

    return (
      <div className="container activity-overview-page">
        <header className="main-content-header">
          <h2
            id="maincontent"
            tabIndex="-1"
            className="h4"
          >
            {!isObservation
              ? translate('ASSIGNMENT_OVERVIEW_TITLE', { name: content.name })
              : translate('AUTHENTIC_ASSESSMENT_OVERVIEW_TITLE', {
                  name: content.name,
                })}
          </h2>
        </header>

        {content.dueDate && (
          <p>
            <DueDateBadge
              date={content.dueDate}
              exempt={content.dueDateExempt}
            />
          </p>
        )}

        <div className="assignment-overview-list has-submissions">
          <WithWorkList
            {...listProps}
            observation={isObservation}
          >
            <ul className="list-group">
              {map(withWorkItems, item => (
                <OverviewListItem
                  key={item.learner.id}
                  content={content}
                  item={item}
                  withWork={true}
                />
              ))}
            </ul>
          </WithWorkList>
        </div>

        <div className="assignment-overview-list not-started">
          <WithoutWorkList
            {...listProps}
            observation={isObservation}
          >
            <ul className="list-group">
              {map(withoutWorkItems, item => (
                <OverviewListItem
                  key={item.learner.id}
                  content={content}
                  item={item}
                  withWork={isObservation}
                />
              ))}
            </ul>
          </WithoutWorkList>
        </div>

        <OverviewModal />
      </div>
    );
  }
}

export default connect(selectActivityOverviewPageComponent)(withTranslation(OverviewPage));
