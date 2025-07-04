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

import React from 'react';
import { useDrop } from 'react-dnd';
import { Dispatch } from 'redux';

import { move } from './contentEdits';
import { addEdit, stopDragging } from './courseCustomizerReducer';
import { DraggableItem } from './ContentNode';

type TargetProps = {
  parentId: string;
  indent: number;
  position: number;
  dispatch: Dispatch;
};

export const Target: React.FC<TargetProps> = ({ parentId, indent, position, dispatch }) => {
  const [{ isOver }, dropRef] = useDrop<DraggableItem, unknown, { isOver: boolean }>(() => ({
    accept: 'ContentNode',
    drop: item => {
      const itemId: string = item.id;
      const oldPosition: number = item.position;

      const newPosition = oldPosition < position ? position - 1 : position;
      dispatch(stopDragging());
      if (oldPosition != newPosition) {
        dispatch(addEdit(move({ id: parentId, childId: itemId, newPosition })));
      }
      return { position };
    },
    collect: monitor => ({ isOver: monitor.isOver() }),
  }));

  return (
    <div
      ref={dropRef}
      id={`drop-target-${parentId}-${position}`}
      className={'content-tree-drop-target-outer'}
      style={{ marginLeft: (indent - 1) * 1.5 + 'em' }}
    >
      <div className={'content-tree-drop-target' + (isOver ? ' is-over' : '')} />
    </div>
  );
};
