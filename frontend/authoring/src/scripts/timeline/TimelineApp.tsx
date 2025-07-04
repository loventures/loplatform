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
import React, { useMemo } from 'react';
import { GiSandsOfTime } from 'react-icons/gi';

import { getDurationInHoursAndMinutes } from '../components/DurationEditor';
import { useEditedTargets, useGraphEdits } from '../graphEdit';
import { usePolyglot } from '../hooks';
import useHomeNodeName from '../hooks/useHomeNodeName';
import NarrativePresence from '../story/NarrativeAsset/NarrativePresence';
import TimelineModuleRow from './TimelineModuleRow';

const TimelineApp: React.FC = () => {
  const polyglot = usePolyglot();
  const homeNodeName = useHomeNodeName();
  const { dirty } = useGraphEdits();
  const modules = useEditedTargets(homeNodeName, 'elements', 'module.1');

  const timelineTotal = useMemo(
    () =>
      getDurationInHoursAndMinutes(
        modules.reduce((acc, module) => acc + (module.data.duration || 0), 0)
      ),
    [modules]
  );

  return (
    <div className="timeline-app">
      <div style={{ display: 'grid', gridTemplateColumns: 'auto 1fr auto' }}>
        <div
          className={classNames(
            'button-spacer d-flex align-items-center justify-content-center actions-icon',
            dirty && 'dirty'
          )}
        >
          <GiSandsOfTime size="1.75rem" />
        </div>
        <h2 className="my-4 text-center">{polyglot.t('TIMELINE_MODULE_DURATIONS')}</h2>
        <NarrativePresence name="timeline">
          <div className="button-spacer d-flex align-items-center justify-content-center actions-icon"></div>
        </NarrativePresence>
      </div>
      <div className="m-4">{polyglot.t('TIMELINE_DURATION_HELP_TEXT')}</div>
      <div className="m-4">{polyglot.t('TIMELINE_OFFSET_HELP_TEXT')}</div>
      <div className="mx-4 my-3">
        <div
          className="fw-bold d-flex justify-content-between mb-1"
          style={{ borderBottom: '1px solid #ccc' }}
        >
          <div className="input-padding">{polyglot.t('TIMELINE_ASSET_NAME')}</div>
          <div
            className="d-flex justify-content-between flex-shrink-0"
            style={{ flexBasis: '206px' }}
          >
            <div className="input-padding ps-0">{polyglot.t('TIMELINE_DURATION')}</div>
            <div
              className="input-padding"
              style={{ paddingRight: '1.9rem' }}
            >
              {polyglot.t('TIMELINE_MODULE_DATE_OFFSET')}
            </div>
          </div>
        </div>
        {modules.map((module, idx) => {
          return (
            <TimelineModuleRow
              key={module.name}
              module={module}
              moduleIndex={idx}
            />
          );
        })}
        <div
          className="pt-1 mt-1 fw-bold d-flex justify-content-end"
          style={{ borderTop: '1px solid #ccc' }}
        >
          <div
            id="timeline-total"
            className="input-padding"
            style={{ paddingRight: '8.55rem' }}
          >
            {timelineTotal}
          </div>
        </div>
      </div>
    </div>
  );
};

export default TimelineApp;
