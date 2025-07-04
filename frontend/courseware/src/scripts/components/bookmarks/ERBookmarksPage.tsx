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

import { minimalise } from '../../commonPages/contentPlayer/ERContentTitle';
import ERNonContentTitle from '../../commonPages/contentPlayer/ERNonContentTitle';
import { removeBookmarkAction, useBookmarks } from '../../components/bookmarks/bookmarksReducer';
import ERContentContainer from '../../landmarks/ERContentContainer';
import { useLearningPathResource } from '../../resources/LearningPathResource';
import { useCourseSelector } from '../../loRedux';
import ContentLink from '../../contentPlayerComponents/parts/ContentLink';
import { useTranslation } from '../../i18n/translationContext';
import { selectCurrentUser } from '../../utilities/rootSelectors';
import React from 'react';
import { useDispatch } from 'react-redux';
import { Button } from 'reactstrap';

const ERBookmarksPage: React.FC = () => {
  const translate = useTranslation();
  const dispatch = useDispatch();
  const { modules } = useLearningPathResource();
  const bookmarks = useBookmarks();
  const viewingAs = useCourseSelector(selectCurrentUser);
  const bookmarkCount = Object.keys(bookmarks).length;
  return (
    <ERContentContainer title={translate('ER_BOOKMARKS')}>
      <div className="container p-0">
        <div className="card er-content-wrapper mb-2 m-md-3 m-lg-4">
          <div className="card-body">
            <ERNonContentTitle label={translate('ER_BOOKMARKS')} />
            <ul className="competency-list-nested ps-md-4 pe-md-2 pt-md-2">
              {bookmarkCount ? (
                modules.flatMap(module =>
                  module.elements
                    .filter(content => bookmarks[content.id] != null)
                    .map(content => (
                      <li
                        key={content.id}
                        className="d-flex align-items-center"
                      >
                        <div className="flex-grow-1">
                          <div className="small text-muted">
                            {minimalise(module.content.name, content.lesson?.name).join(' / ')}
                          </div>
                          <div>
                            <ContentLink
                              content={content}
                              viewingAs={viewingAs}
                              disableSummary
                            >
                              {content.name}
                            </ContentLink>
                          </div>
                          <div>{bookmarks[content.id]}</div>
                        </div>
                        <Button
                          color="danger"
                          outline
                          className="border-0 p-2"
                          style={{ lineHeight: 1 }}
                          title={translate('REMOVE_BOOKMARK')}
                          aria-label={translate('REMOVE_BOOKMARK')}
                          onClick={() => dispatch(removeBookmarkAction(content.id))}
                        >
                          <div
                            className="material-icons md-18"
                            style={{ verticalAlign: 'bottom' }}
                            aria-hidden
                          >
                            close
                          </div>
                        </Button>
                      </li>
                    ))
                )
              ) : (
                <li className="text-muted text-center">{translate('NO_BOOKMARKS')}</li>
              )}
            </ul>
          </div>
        </div>
      </div>
    </ERContentContainer>
  );
};

export default ERBookmarksPage;
