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

import { describeArc } from './circleUtils';

interface CircleProps {
  normalizedPercent: number;
}

const ERCircleGraphic: React.FC<CircleProps> = ({ normalizedPercent }) => {
  const progressPercent = Math.floor(normalizedPercent * 100);
  const endAngle = normalizedPercent * 360;
  //with 100% progress the start and the end are the same
  //and so you won't get any arc
  //359.99deg has no pixel difference than a full circle
  //alternatively we could use a circle instead of a path
  const adjustedEndAngle = endAngle === 360 ? 359.99 : endAngle;
  const d = describeArc(100, 100, 90, 0, adjustedEndAngle);
  return (
    <svg
      className="prog-circle-arc"
      width="100%"
      height="100%"
      viewBox="0 0 200 200"
      version="1.1"
      xmlns="http://www.w3.org/2000/svg"
    >
      <g
        strokeWidth="18"
        fill="none"
      >
        <circle
          className="progress-background"
          cx="100"
          cy="100"
          r="90"
        />
        {progressPercent && (
          <path
            className={'progress-arc done-' + progressPercent}
            d={d}
          />
        )}
      </g>
    </svg>
  );
};

type ERProgressCircleProps = {
  progress?: number;
  explanation: string;
} & React.PropsWithChildren;

const ERProgressCircle: React.FC<ERProgressCircleProps> = ({ progress, explanation, children }) => {
  return (
    <div className="er-prog-circle m-1 m-md-2">
      <div className="sr-only">{explanation}</div>
      <div aria-hidden={true}>
        <ERCircleGraphic normalizedPercent={progress ?? 0} />
        <div className="prog-circle-content">{children}</div>
      </div>
    </div>
  );
};

export default ERProgressCircle;
