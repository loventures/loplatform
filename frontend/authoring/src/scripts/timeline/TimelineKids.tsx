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

import classNames from 'classnames';
import React, { useMemo, useState } from 'react';
import NumericInput from 'react-numeric-input2';
import { useDispatch } from 'react-redux';

import {
  getDurationInHoursAndMinutes,
  getMinutesByHoursAndMinutes,
} from '../components/DurationEditor';
import { ELEMENT_TYPES } from '../editor/EdgeRuleConstants';
import {
  autoSaveProjectGraphEdits,
  beginProjectGraphEdit,
  editProjectGraphNodeData,
  useEditedTargets,
} from '../graphEdit';
import { useProjectAccess } from '../story/hooks';
import { useIsStoryEditMode } from '../story/storyHooks';
import { NodeName } from '../types/asset';
import { NewAssetWithEdge } from '../types/edge';

export const TimelineKids: React.FC<{ parent: NodeName; depth: number }> = ({ parent, depth }) => {
  const children = useEditedTargets(parent, 'elements');

  return (
    <>
      {children.map((content, idx) => (
        <React.Fragment key={`${idx}.${content.name}`}>
          <TimelineChildRow
            content={content}
            depth={depth}
          />
          {content.typeId === 'lesson.1' && (
            <TimelineKids
              parent={content.name}
              depth={1 + depth}
            />
          )}
        </React.Fragment>
      ))}
    </>
  );
};

const TimelineChildRow: React.FC<{
  content: NewAssetWithEdge<any>;
  depth: number;
}> = ({ content, depth }) => {
  const dispatch = useDispatch();
  const duration = content.data.duration;
  const [badHours, setBadHours] = useState(false);
  const invalidDuration = duration <= 0 && duration !== null;
  const projectAccess = useProjectAccess();
  const userCanEdit = useIsStoryEditMode() && projectAccess.EditTimeline;
  const editableType = useMemo(() => ELEMENT_TYPES.includes(content.typeId), [content]);

  const editActivityDuration = (minutes: number) => {
    dispatch(beginProjectGraphEdit('Change module duration', content.name));
    dispatch(editProjectGraphNodeData(content.name, { duration: minutes || null }));
  };

  return (
    <div className="timeline-content-row d-flex align-items-center justify-content-between flex-shrink-1 minw-0">
      <div
        style={{ paddingLeft: `${2.4 + 0.75 * depth}rem` }}
        className="py-1 timeline-content-title text-muted text-truncate minw-0 flex-shrink-1"
      >
        {content.data.title}
      </div>

      <div
        className="d-flex justify-content-end flex-shrink-0 hide-nudge-arrows"
        style={{ flexBasis: '100px', marginRight: '112px' }}
      >
        {!userCanEdit || !editableType ? (
          <div className="py-1 text-muted timeline-content-duration me-4">
            {content.data.duration == null
              ? '–'
              : getDurationInHoursAndMinutes(content.data.duration)}
          </div>
        ) : (
          <NumericInput
            className={classNames(
              'form-control secret-input bg-white text-right timeline-content-duration',
              (invalidDuration || badHours) && 'is-invalid'
            )}
            name={'duration_' + content.edge.name}
            step={1}
            min={0}
            defaultValue={getDurationInHoursAndMinutes(duration)}
            format={d => (d ? getDurationInHoursAndMinutes(d) : '–')}
            parse={s => {
              try {
                setBadHours(false);
                return getMinutesByHoursAndMinutes(s);
              } catch (e) {
                setBadHours(true);
                return s;
              }
            }}
            onChange={editActivityDuration}
            onBlur={() => dispatch(autoSaveProjectGraphEdits())}
            placeholder="–"
          />
        )}
      </div>
    </div>
  );
};
