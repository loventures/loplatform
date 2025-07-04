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

import ERBasicTitle from '../../commonPages/contentPlayer/ERBasicTitle';
import { openModalActionCreator } from '../../commonPages/discussionList/actions/modalActions';
import ManageDiscussionsModal from '../../commonPages/discussionList/components/ManageDiscussionsModal';
import { selectDiscussionListPageComponent } from '../../commonPages/discussionList/selectors/discussionSelectors';
import ERContentContainer from '../../landmarks/ERContentContainer';
import { useCourseSelector } from '../../loRedux';
import { map } from 'lodash';
import DiscussionPreview from '../../contentPlayerComponents/contentViews/ContentPreview/DiscussionPreview';
import { useTranslation } from '../../i18n/translationContext';
import DiscussionListLoader from '../../loaders/DiscussionListLoader';
import React from 'react';
import { useDispatch } from 'react-redux';
import ERNonContentTitle from '../contentPlayer/ERNonContentTitle';

const ERDiscussionListPage: React.FC = () => {
  const translate = useTranslation();
  const dispatch = useDispatch();
  const { viewingAs, discussions, showDiscussionBoardSummaries } = useCourseSelector(
    selectDiscussionListPageComponent
  );

  const ManageDiscussionsModalToggle = () => (
    <div className="d-flex justify-content-center">
      <button
        className="btn btn-primary settings-toggle"
        onClick={() => dispatch(openModalActionCreator())}
      >
        {translate('DISCUSSION_SETTING_TOGGLE')}
      </button>
    </div>
  );

  return (
    <ERContentContainer title={translate('ER_DISCUSSIONS_TITLE')}>
      <div className="container p-0">
        <div className="card er-content-wrapper mb-2 m-md-3 m-lg-4">
          <div className="card-body">
            <ERNonContentTitle label={translate('ER_DISCUSSIONS_TITLE')} />
            {viewingAs.isInstructor ? <ManageDiscussionsModalToggle /> : null}
            <DiscussionListLoader viewingAs={viewingAs}>
              <ul className="discussion-preview-list m-0 m-md-3 m-lg-4">
                {map(discussions, discussion => (
                  <li key={discussion.id}>
                    <DiscussionPreview
                      content={discussion}
                      viewingAs={viewingAs}
                      showSummary={showDiscussionBoardSummaries}
                    />
                  </li>
                ))}
              </ul>

              {viewingAs.isInstructor && <ManageDiscussionsModal discussions={discussions} />}
            </DiscussionListLoader>
          </div>
        </div>
      </div>
    </ERContentContainer>
  );
};

export default ERDiscussionListPage;
