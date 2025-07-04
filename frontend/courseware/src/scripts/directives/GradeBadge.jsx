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

import { coloredGrade, coloredGradeBg } from '../utilities/colorGradients.js';
import { lojector } from '../loject.js';

const percentClass = percent => {
  switch (percent) {
    case 'full':
      return 'full-sized-percent';
    case 'half':
      return 'half-sized-percent';
    default:
      return '';
  }
};

const GradeBadge = ({
  grade,
  outline = false,
  className = '',
  //specify percent to force using 'percent' display regardless of default setting
  percent = '',
  display = percent !== '' && 'percent',
  coloredGradeClassName = outline ? coloredGrade(grade) : coloredGradeBg(grade),
  showEmptyPostfix = void 0, // cuz null == 0 and grade filter sucks,
  showEmptyGrade = false,
  isPending = false,
}) => (
  <span
    className={classNames(
      'grade-badge',
      className,
      grade && !isPending && percentClass(percent),
      !isPending && coloredGradeClassName
    )}
  >
    {(grade || showEmptyGrade) && !isPending ? (
      lojector.get('gradeFilter')(grade, display, showEmptyPostfix)
    ) : (
      <span className="material-icons pending-grade">pending_actions</span>
    )}
  </span>
);

export default GradeBadge;
