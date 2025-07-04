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
import React, { useState } from 'react';
import { useCollapse } from 'react-collapsed';
import NumericInput from 'react-numeric-input2';
import { useDispatch } from 'react-redux';
import { Link } from 'react-router-dom';
import { Button } from 'reactstrap';

import {
  getDurationInHoursAndMinutes,
  getMinutesByHoursAndMinutes,
} from '../components/DurationEditor';
import {
  autoSaveProjectGraphEdits,
  beginProjectGraphEdit,
  editProjectGraphEdgeData,
  editProjectGraphNodeData,
  useGraphEdits,
} from '../graphEdit';
import { useBranchId, useHomeNodeName, usePolyglot } from '../hooks';
import { Stornado } from '../story/badges/Stornado';
import { useProjectAccess } from '../story/hooks';
import { useIsStoryEditMode } from '../story/storyHooks';
import { NewAssetWithEdge } from '../types/edge';
import { Module } from '../types/typeIds';
import { TimelineKids } from './TimelineKids';

const TimelineModuleRow: React.FC<{
  module: NewAssetWithEdge<Module>;
  moduleIndex: number;
}> = ({ module, moduleIndex }) => {
  const homeNodeName = useHomeNodeName();
  const branchId = useBranchId();
  const dispatch = useDispatch();
  const [badHours, setBadHours] = useState(false);
  const polyglot = usePolyglot();
  const duration = module.data.duration;
  const invalidDuration = duration <= 0 && duration !== null;
  const projectAccess = useProjectAccess();
  const editMode = useIsStoryEditMode() && projectAccess.EditTimeline;
  const { generation } = useGraphEdits();

  const [stayOpen, setStayOpen] = useState(false);
  const { getCollapseProps, getToggleProps, isExpanded } = useCollapse({
    defaultExpanded: false,
    onTransitionStateChange: state => {
      if (state === 'expandStart') {
        setStayOpen(true);
      } else if (state === 'collapseEnd') {
        setStayOpen(false);
      }
    },
  });

  const editModuleDuration = minutes => {
    dispatch(beginProjectGraphEdit('Change module duration', module.name));
    dispatch(editProjectGraphNodeData(module.name, { duration: minutes || null }));
  };

  const [invalidOffset, setInvalidOffset] = useState<boolean>(false);
  const editModuleOffset = (value: number | null) => {
    setInvalidOffset(false);
    dispatch(beginProjectGraphEdit('Change module date offset', module.name));
    const data =
      value && Number.isInteger(value) ? { gate: { offset: value } } : { gate: undefined };
    dispatch(editProjectGraphEdgeData(module.edge.sourceName, module.edge.name, data));
  };

  return (
    <>
      <div
        key={module.name}
        className="story-index-item p-1 timeline-module-row d-flex justify-content-between"
      >
        <div className="d-flex align-items-center flex-shrink-1 minw-0">
          <Button
            size="small"
            color="transparent"
            className="ms-1 mini-button p-1 d-inline-flex align-items-center justify-content-center timeline-module-toggle"
            style={{ lineHeight: 1 }}
            {...getToggleProps()}
          >
            <i className="material-icons md-18">{isExpanded ? 'expand_more' : 'chevron_right'}</i>
          </Button>

          <div className="input-padding ps-2 d-flex minw-0 align-items-center">
            <Link
              to={`/branch/${branchId}/story/${module.name}?contextPath=${homeNodeName}`}
              className="text-truncate"
            >
              {module.data.title}
            </Link>
            <Stornado
              name={module.name}
              size="sm"
            />
          </div>
        </div>
        <div
          className="d-flex justify-content-between flex-shrink-0"
          style={{ flexBasis: '200px' }}
        >
          <div
            className="hide-nudge-arrows flex-shrink-0 timeline-module-duration me-1 pe-1"
            style={{ flexBasis: '100px' }}
          >
            {!editMode ? (
              <div
                className="text-muted input-padding text-right"
                style={{ width: '5rem' }}
              >
                {getDurationInHoursAndMinutes(duration) || '–'}
              </div>
            ) : (
              <NumericInput
                key={`dur-${generation}`}
                className={classNames(
                  'form-control secret-input bg-white text-right',
                  (invalidDuration || badHours) && 'is-invalid'
                )}
                name={'duration' + moduleIndex}
                step={60}
                min={0}
                defaultValue={getDurationInHoursAndMinutes(duration)}
                format={d => (!d ? 'unset' : getDurationInHoursAndMinutes(d))}
                parse={s => {
                  try {
                    setBadHours(false);
                    return getMinutesByHoursAndMinutes(s);
                  } catch (e) {
                    setBadHours(true);
                    return s;
                  }
                }}
                onChange={editModuleDuration}
                onBlur={() => dispatch(autoSaveProjectGraphEdits())}
                placeholder="unset"
              />
            )}
            {invalidDuration || badHours ? (
              <div className="form-text text-danger small text-right">
                {polyglot.t(
                  invalidDuration ? 'ASSET_DURATION_ERROR' : 'ASSET_DURATION_HOURS_EXTRA_HELP'
                )}
              </div>
            ) : null}
          </div>
          <div
            className="hide-nudge-arrows flex-shrink-0 timeline-module-offset pe-1"
            style={{ flexBasis: '100px' }}
          >
            {!editMode ? (
              <div
                className="text-muted input-padding text-right"
                style={{ width: '5rem' }}
              >
                {addDays(module.edge.data?.gate?.offset) ?? '–'}
              </div>
            ) : (
              <NumericInput
                key={`off-${generation}`}
                className={classNames('form-control secret-input bg-white text-right', {
                  'is-invalid': invalidOffset,
                })}
                name={'offset' + moduleIndex}
                step={1}
                min={0}
                defaultValue={addDays(module.edge.data?.gate?.offset)}
                format={o => (!o ? 'unset' : addDays(o))}
                parse={v => {
                  try {
                    setInvalidOffset(false);
                    return parseDays(v);
                  } catch (e) {
                    setInvalidOffset(true);
                    return v;
                  }
                }}
                onChange={editModuleOffset}
                onBlur={() => dispatch(autoSaveProjectGraphEdits())}
                placeholder="unset"
              />
            )}
            {invalidOffset ? (
              <div className="form-text text-danger small text-right">
                {polyglot.t('ASSET_OFFSET_ERROR')}
              </div>
            ) : null}
          </div>
        </div>
      </div>
      <div {...getCollapseProps()}>
        {(isExpanded || stayOpen) && (
          <TimelineKids
            key={`kids-${generation}`}
            parent={module.name}
            depth={1}
          />
        )}
      </div>
    </>
  );
};

export default TimelineModuleRow;

/** Private */

const parseDays = (s: string) => {
  if (s === '' || s === 'undefined' || s == null) {
    return;
  }
  const match = s.match(/^(\d{0,4})d?$/);
  if (!match) throw Error(`Invalid input`);
  const [days] = match;
  const parsedDays = Number.parseInt(days);
  if (parsedDays < 0) throw Error('Negative offset not allowed');
  return parsedDays;
};

const addDays = (s: number | undefined) => (s ? s + 'd' : s);
