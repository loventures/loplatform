/*
 * LO Platform copyright (C) 2007–2026 LO Ventures LLC.
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
import { ReactNode } from 'react';

import { grade as gradeFormat, makeGradeDisplayMethods } from '../filters/pure/grade.ts';
import { useTranslation } from '../i18n/translationContext.tsx';
import { coloredGrade, coloredGradeBg } from '../utilities/colorGradients.js';

const percentClass = (percent: string) => {
  switch (percent) {
    case 'full':
      return 'full-sized-percent';
    case 'half':
      return 'half-sized-percent';
    default:
      return '';
  }
};

interface GradeBadgeProps {
  grade?: any;
  outline?: boolean;
  className?: string;
  percent?: string;
  display?: any;
  coloredGradeClassName?: string;
  showEmptyPostfix?: any;
  showEmptyGrade?: boolean;
  isPending?: boolean;
}

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
}: GradeBadgeProps) => {
  const translate = useTranslation();
  return (
    <span
      className={classNames(
        'grade-badge',
        className,
        grade && !isPending && percentClass(percent),
        !isPending && coloredGradeClassName
      )}
    >
      {(grade || showEmptyGrade) && !isPending ? (
        (gradeFormat(
          makeGradeDisplayMethods(translate),
          grade,
          display,
          showEmptyPostfix
        ) as ReactNode)
      ) : (
        <span className="material-icons pending-grade">pending_actions</span>
      )}
    </span>
  );
};

export default GradeBadge;
