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

import { isNumber } from 'lodash';
import classnames from 'classnames';
import { lojector } from '../loject.js';

const gradientClassNames = (percent = 0, className) =>
  classnames(className, `done-${isNumber(percent) ? Math.round(percent) : ''}`);

const gradeToPercent = grade => lojector.get('gradeFilter')(grade, 'percent', 0);

const progressToPercent = progress =>
  progress && isNumber(progress.weightedPercentage) ? progress.weightedPercentage : 0;
export const coloredGrade = grade => {
  return gradientClassNames(gradeToPercent(grade), 'colored-grade');
};

export const coloredGradeBg = grade => {
  return gradientClassNames(gradeToPercent(grade), 'colored-grade-bg');
};

export const coloredProgress = progress => {
  return gradientClassNames(progressToPercent(progress), 'colored-progress');
};

export const coloredProgressBg = progress => {
  return gradientClassNames(progressToPercent(progress), 'colored-progress-bg');
};
