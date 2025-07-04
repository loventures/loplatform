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

import { CustomisableContent } from '../../api/customizationApi';
import { WithTranslate } from '../../i18n/translationContext';
import React from 'react';
import { withHandlers } from 'recompose';
import { Dispatch } from 'redux';

import { toggleContentOpen } from './courseCustomizerReducer';

type DropdownCaretOuterProps = {
  content: CustomisableContent;
  dispatch: Dispatch;
  isExpanded: boolean;
};

function caretIconClass(isClosed: boolean): string {
  return isClosed ? `icon ${'icon-chevron-right'}` : `icon ${'icon-chevron-down'}`;
}

const stopPropagation = (e: React.MouseEvent<HTMLButtonElement, MouseEvent>) => e.stopPropagation();

const DropdownCaretInner: React.FC<
  DropdownCaretOuterProps & {
    toggleOpen: () => void;
  }
> = ({ content, isExpanded, toggleOpen }) => (
  <WithTranslate>
    {translate => {
      const title = translate(isExpanded ? 'COLLAPSE_CONTENT' : 'EXPAND_CONTENT', {
        name: content.title,
      });
      return (
        <button
          title={title}
          className="dropdown-caret icon-btn me-2"
          onMouseDown={stopPropagation}
          onClick={toggleOpen}
        >
          <span
            style={{ width: '24px', fontSize: '1.65rem' }}
            className={caretIconClass(!isExpanded)}
          />
          <span className="sr-only">{title}</span>
        </button>
      );
    }}
  </WithTranslate>
);

export const DropdownCaret = withHandlers({
  toggleOpen: (props: DropdownCaretOuterProps) => () => {
    props.dispatch(toggleContentOpen(props.content.id));
  },
})(DropdownCaretInner);
