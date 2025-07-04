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

import { removeBookmarkAction, selectBookmark } from '../../components/bookmarks/bookmarksReducer';
import { useCourseSelector } from '../../loRedux';
import { ContentWithRelationships } from '../../courseContentModule/selectors/assembleContentView';
import { useTranslation } from '../../i18n/translationContext';
import React, { useRef } from 'react';
import { FiBookmark } from 'react-icons/fi';
import { useDispatch } from 'react-redux';
import { Button, DropdownItem, UncontrolledPopover } from 'reactstrap';

type ERBookmarkButtonProps = {
  content: ContentWithRelationships;
  dropdown?: boolean;
  setOpen: (open: boolean) => void;
};

const ERBookmarkButton: React.FC<ERBookmarkButtonProps> = ({ content, dropdown, setOpen }) => {
  const buttonRef = useRef<HTMLButtonElement>(null);
  const translate = useTranslation();
  const bookmark = useCourseSelector(selectBookmark(content.id));
  const bookmarked = bookmark != null;
  const dispatch = useDispatch();

  return dropdown ? (
    <DropdownItem
      id="bookmark-button"
      innerRef={buttonRef}
      onClick={() => (bookmarked ? dispatch(removeBookmarkAction(content.id)) : setOpen(true))}
    >
      {translate(bookmarked ? 'REMOVE_BOOKMARK_ITEM' : 'ADD_BOOKMARK_ITEM')}
    </DropdownItem>
  ) : (
    <>
      <Button
        id="bookmark-button"
        innerRef={buttonRef}
        color="primary"
        outline
        className="border-white px-2"
        aria-label={translate(bookmarked ? 'REMOVE_BOOKMARK_TITLE' : 'ADD_BOOKMARK_TITLE')}
        title={translate(bookmarked ? 'REMOVE_BOOKMARK_TITLE' : 'ADD_BOOKMARK_TITLE')}
        onClick={() => (bookmarked ? dispatch(removeBookmarkAction(content.id)) : setOpen(true))}
      >
        <FiBookmark
          size="2rem"
          strokeWidth={1.1}
          fill={bookmark != null ? '#0c6e9e80' : 'transparent'}
          aria-hidden={true}
        />
      </Button>
      {bookmark && buttonRef.current && (
        <UncontrolledPopover
          target={buttonRef.current}
          placement="bottom"
          trigger="hover"
        >
          <div className="py-1 px-2">{bookmark}</div>
        </UncontrolledPopover>
      )}
    </>
  );
};

export default ERBookmarkButton;
