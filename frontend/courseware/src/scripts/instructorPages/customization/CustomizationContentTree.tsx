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

import { CustomisableContent, applyDay, applyTime } from '../../api/customizationApi';
import { CourseState } from '../../loRedux';
import { includes, sortBy } from 'lodash';
import { isForCredit } from '../../utilities/creditTypes';
import { move as arrayMove, equalWithOrder, toggle } from '../../types/arrays';
import React from 'react';
import { connect } from 'react-redux';
import { Dispatch } from 'redux';
import { createSelector } from 'reselect';

import { ContentEdit, match } from './contentEdits';
import { ContentNode } from './ContentNode';
import { Target } from './Target';
import { Tree } from './Tree';

/**
 * Props provided by the selector
 */
type CustomizationContentTreeInnerProps = CustomizationContentTreeProps & {
  dispatch: Dispatch;
  expanded: boolean;
  edits: ContentEdit[];
  hiddenItemsVisible: boolean;

  /**
   * True if we are dragging this item or a sibling
   */
  currentlyDragging: boolean;
};

type Changes = { content: CustomisableContent; childrenOrder: string[] };

const contentEditReducer = ({ content, childrenOrder }: Changes, edit: ContentEdit): Changes => {
  const modContent = (p: Partial<CustomisableContent>): Changes => ({
    content: { ...content, ...p },
    childrenOrder,
  });

  return match(edit, {
    rename: r => modContent({ title: r.name }),
    reinstruct: r => modContent({ instructions: r.instructions }),
    setForCredit: set => modContent({ isForCredit: isForCredit(set.type) }),
    changePointsPossible: change => modContent({ pointsPossible: change.newPointsPossible }),
    changeDueTime: change =>
      modContent({
        dueTime: change,
        dueDate: applyTime(change)(content.dueDate),
      }),
    changeDueDay: change =>
      modContent({
        dueDay: change,
        dueDate: applyDay(change)(content.dueDate),
      }),
    removeDueDate: () => modContent({ dueDate: null, dueDay: null, dueTime: null }),
    hideChild: hidden => modContent({ hide: toggle(content.hide || [], hidden.childId) }),
    move: move => {
      const oldPosition = childrenOrder.indexOf(move.childId);

      const newChildrenOrder = arrayMove(childrenOrder, oldPosition, move.newPosition);
      return {
        content,
        childrenOrder: newChildrenOrder,
      };
    },
  });
};

/**
 * Props provided by the parent
 */
type CustomizationContentTreeProps = {
  siblingCount: number;
  indent: number;
  parentId?: string;
  position: number;
  hidden: boolean;
  content: Tree<CustomisableContent>;
  isLast: boolean;
  nextVisibleNode?: Tree<CustomisableContent>;
};

const selectEdits = createSelector(
  (state: CourseState, { content: { value } }: CustomizationContentTreeProps) => {
    return state.courseCustomizations.customizerState.edits.filter(e => e.payload.id === value.id);
  },
  (state: CourseState, { content: { value } }: CustomizationContentTreeProps) =>
    state.courseCustomizations.customizerState.expandedContent.includes(value.id) ||
    value.id === '_root_',
  (state: CourseState, { parentId }: CustomizationContentTreeProps) => {
    const context = state.courseCustomizations.customizerState.currentDraggingContext;
    return !!context && parentId === context.parent;
  },
  (state: CourseState) => {
    return state.courseCustomizations.customizerState.hiddenItemsVisible;
  },
  (edits, expanded, currentlyDragging, hiddenItemsVisible) => ({
    edits,
    expanded,
    currentlyDragging,
    hiddenItemsVisible,
  })
);

const findNextVisibleNode = (
  childIndex: number,
  orderedChildren: Tree<CustomisableContent>[],
  content: any,
  nextVisibleNodeForParent: any
) => {
  let j = childIndex + 1;
  let nextVisibleNode;
  while (!nextVisibleNode && j < orderedChildren.length) {
    if (!includes(content.hide, orderedChildren[j].value.id)) {
      nextVisibleNode = orderedChildren[j];
    }
    j++;
  }
  return nextVisibleNode || nextVisibleNodeForParent;
};

function strict<A>(l: A, r: A): boolean {
  return l === r;
}

function forKeys<A extends Record<string, any>>(
  keys: (keyof A)[]
): (l: Readonly<A>, r: Readonly<A>) => boolean {
  return (l, r) => {
    const lKeys = Object.keys(l).filter(k => keys.includes(k as keyof A));
    const rKeys = Object.keys(r).filter(k => keys.includes(k as keyof A));

    if (lKeys.length !== rKeys.length) {
      return false;
    } else {
      return lKeys.every(key => l[key] === r[key]);
    }
  };
}

function isEq(
  l: Readonly<CustomizationContentTreeInnerProps>,
  r: Readonly<CustomizationContentTreeInnerProps>
): boolean {
  const editsEqual = equalWithOrder(l.edits, r.edits, strict);
  const keyEqual = forKeys([
    'expanded',
    'hidden',
    'position',
    'currentlyDragging',
    'hiddenItemsVisible',
  ])(l, r);
  return editsEqual && keyEqual;
}

/**
 * Renders the customization editor for the given content and all of it's children
 */
export const CustomizationContentTree = connect(selectEdits, d => ({ dispatch: d }))(
  React.memo((props: CustomizationContentTreeInnerProps) => {
    const { content, childrenOrder } = props.edits.reduce(contentEditReducer, {
      content: props.content.value,
      childrenOrder: props.content.children.map(ch => ch.value.id),
    });

    const orderedChildren = sortBy(props.content.children, ch =>
      childrenOrder.indexOf(ch.value.id)
    );
    if (!props.hiddenItemsVisible && props.hidden) {
      return null;
    } else {
      return (
        <>
          {content.id !== '_root_' && (
            <>
              <Target
                dispatch={props.dispatch}
                indent={props.indent}
                parentId={String(props.parentId)}
                position={props.position}
              />
              <ContentNode
                isHidden={props.hidden}
                content={content}
                childCount={props.content.children.length}
                indent={props.indent}
                // dispatch={props.dispatch}
                parentId={String(props.parentId)}
                nodeCount={props.siblingCount}
                position={props.position}
                isExpanded={props.expanded}
                nextVisibleNodeId={
                  props.nextVisibleNode ? props.nextVisibleNode.value.id : undefined
                }
              />
            </>
          )}
          {props.content.children.length > 0 && props.expanded && !props.hidden ? (
            <div
              id={props.content.value.id + '-children'}
              className="content-children"
              style={{
                overflow: !props.expanded || props.hidden ? 'hidden' : 'visible',
              }}
            >
              {orderedChildren.map((child, i) => {
                const nextVisibleSibling = findNextVisibleNode(
                  i,
                  orderedChildren,
                  props.content,
                  props.nextVisibleNode
                );
                return (
                  <CustomizationContentTree
                    key={child.value.id}
                    position={i}
                    content={child}
                    indent={props.indent + 1}
                    parentId={props.content.value.id}
                    siblingCount={props.content.children.length}
                    hidden={!!content.hide && content.hide.includes(child.value.id)}
                    isLast={i + 1 === props.content.children.length}
                    nextVisibleNode={nextVisibleSibling}
                  />
                );
              })}
            </div>
          ) : null}
          {props.currentlyDragging && props.isLast && props.parentId ? (
            <Target
              indent={props.indent}
              parentId={props.parentId}
              dispatch={props.dispatch}
              position={props.siblingCount}
            />
          ) : null}
        </>
      );
    }
  }, isEq)
);
