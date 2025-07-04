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

import classnames from 'classnames';
import { CustomisableContent } from '../../api/customizationApi';
import { useCourseSelector } from '../../loRedux';
import dayjs from 'dayjs';
import { useTranslation } from '../../i18n/translationContext';
import { getContentDisplayInfo } from '../../utilities/contentDisplayInfo';
import React from 'react';
import { useDrag } from 'react-dnd';
import { useDispatch } from 'react-redux';
import { Collapse } from 'reactstrap';

import { startDragging, updateActiveContentEditor } from './courseCustomizerReducer';
import { DropdownCaret } from './DropdownCaret';
import { DueDateEditor } from './editors/DueDateEditor';
import { ForCreditEditor } from './editors/ForCreditEditor';
import { NameEditor } from './editors/NameEditor';
import { PointsEditor } from './editors/PointsEditor';
import { PositionEditor } from './editors/PositionEditor';
import { VisibilityEditor } from './editors/VisibilityEditor';

type ContentNodeProps = {
  isHidden: boolean;
  content: CustomisableContent;
  childCount: number;
  indent: number;
  parentId: string;
  nodeCount: number;
  position: number;
  isExpanded: boolean;
  nextVisibleNodeId?: string;
};

export type DraggableItem = { id: string; position: number };

export const ContentNode: React.FC<ContentNodeProps> = ({
  position,
  nodeCount,
  indent,
  childCount,
  content,
  isExpanded,
  parentId,
  isHidden,
  nextVisibleNodeId,
}) => {
  const dispatch = useDispatch();
  const translate = useTranslation();
  const [, dragRef] = useDrag<DraggableItem>(() => {
    return {
      type: 'ContentNode',
      item: () => {
        alert('THIS IS NEVER CALLED. Switch to @dnd-kit/core or something');
        dispatch(
          startDragging({
            dragging: content.id,
            parent: parentId,
          })
        );
        return { id: content.id, position: position };
      },
    };
  }, [content.id, parentId, position]);

  const customizerState = useCourseSelector(state => state.courseCustomizations.customizerState);
  const { activeContentEditor, contentBeingEdited, outdatedMousePosition } = customizerState;

  const beingEdited = contentBeingEdited === content.id;
  const isActive = activeContentEditor ? activeContentEditor.id === content.id : false;

  const showEditors = beingEdited || isActive;
  const setFocus = activeContentEditor ? isActive && activeContentEditor.setFocus : false;

  // eslint-disable-next-line jsx-a11y/no-static-element-interactions
  return (
    <div
      ref={dragRef}
      id={content.id + '-node'}
      className={classnames([
        'py-2 content-node',
        isHidden && 'hidden',
        showEditors && 'show-editors',
      ])}
      style={{
        marginLeft: (indent - 1) * 1.5 + 'em',
      }}
      onFocusCapture={() => {
        dispatch(
          updateActiveContentEditor({
            id: content.id,
            setFocus: false,
          })
        );
      }}
      onMouseEnter={() => {
        if (!outdatedMousePosition && !showEditors && !beingEdited) {
          dispatch(
            updateActiveContentEditor({
              id: content.id,
              setFocus: false,
            })
          );
        }
      }}
      onMouseLeave={() => {
        if (!outdatedMousePosition && showEditors && !beingEdited) {
          dispatch(updateActiveContentEditor(void 0));
        }
      }}
    >
      <div className="content-node-row">
        <PositionEditor
          dispatch={dispatch}
          content={content}
          nodeCount={nodeCount}
          currentPosition={position}
          parentId={parentId}
          setFocus={setFocus}
        />
        {childCount > 0 ? (
          <DropdownCaret
            content={content}
            dispatch={dispatch}
            isExpanded={isExpanded}
          />
        ) : null}
        <span
          style={{
            fontSize: '1.5rem',
            marginLeft: childCount > 0 ? '' : 'calc(0.5rem + 24px)',
          }}
          className={`me-1 py-2 icon ${getContentDisplayInfo(content).displayIcon}`}
        />
        <NameEditor
          content={content}
          dispatch={dispatch}
          showEditors={showEditors}
        />
        <div className="right">
          <div className="summary">
            {!showEditors && content.gradable && (
              <>
                {content.dueDate ? (
                  <span className="summary-detail due-date-display">
                    {translate('DUE_DATES_DUE_DATE') +
                      ' ' +
                      dayjs(content.dueDate).format('MMM D, YYYY h:mm a')}
                  </span>
                ) : null}
                <span className="summary-detail points-possible-display">
                  {content.pointsPossible} {translate('POINTS_POSSIBLE')}
                </span>
                {content.isForCredit ? (
                  <span className="summary-detail for-credit-display">
                    {translate('FOR_CREDIT')}
                  </span>
                ) : null}
              </>
            )}
          </div>
          <div className="d-flex">
            {showEditors && content.gradable && (
              <>
                <PointsEditor
                  content={content}
                  dispatch={dispatch}
                />
                <ForCreditEditor
                  content={content}
                  dispatch={dispatch}
                />
              </>
            )}
            <VisibilityEditor
              content={content}
              hidden={isHidden}
              parentId={parentId}
              showEditors={showEditors}
              nextVisibleNodeId={nextVisibleNodeId}
            />
          </div>
        </div>
      </div>
      {content.gradable && (
        <Collapse isOpen={showEditors}>
          <div className="content-node-row justify-content-end mt-3">
            <DueDateEditor
              content={content}
              dispatch={dispatch}
            />
          </div>
        </Collapse>
      )}
    </div>
  );
};
