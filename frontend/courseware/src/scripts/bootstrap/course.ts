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

import dayjs from 'dayjs';
import { throttle } from 'lodash';

import { Course as CourseT } from '../../loPlatform';

let Course: CourseT;

if (window.lo_platform?.course) {
  Course = window.lo_platform.course;
  Course.LTI = false;
} else {
  Course = {} as CourseT;
}

if (window.lo_platform?.session?.customTitle) {
  Course.name = window.lo_platform.session.customTitle;
  console.log(`Custom Course Title: ${window.lo_platform.session.customTitle}`);
}

/** used only by debugbear */
const effectiveStartDate = () => {
  /* The effective start date is considered to be the first "full" day,
   * or so sayeth the content date utils. */
  const courseStartDate = Course.startDate || Course.createTime;
  if (/*!!*/ courseStartDate) {
    const actualStartDate = new Date(courseStartDate);
    const truncatedStartDate = new Date(courseStartDate); // cow desk
    truncatedStartDate.setHours(0, 0, 0, 0);
    if (actualStartDate.getTime() !== truncatedStartDate.getTime()) {
      truncatedStartDate.setDate(truncatedStartDate.getDate() + 1);
      return truncatedStartDate;
    } else {
      return actualStartDate;
    }
  }
};
Course.effectiveStartDate = effectiveStartDate();

/** formerly located in meekMode.js. Used by headerPageSelectors and only need to happen once. */
const contentItemRootPattern = /(?:&|\?)contentItemRoot=([a-z0-9_]+)(?:$|&)/;
const noHeaderPattern = /(?:&|\?)noHeader(?:$|&)/;
const rootMatch = contentItemRootPattern.exec(window.location.toString());
if (rootMatch && rootMatch[1]) {
  Course.contentItemRoot = rootMatch[1];
}
const noHdrMatch = noHeaderPattern.exec(window.location.toString());
if (noHdrMatch) {
  Course.noHeader = true;
}

/** From globalFeatures.js config block */
/*
* CourseProvider.Course.id = lop.course
      ? lop.course.id || lop.global_course_id
      : null;
* */
if (!Course.id) {
  if (window?.lo_platform?.global_course_id) {
    Course.id = window.lo_platform.global_course_id;
  } else {
    Course.id = null as unknown as number;
  }
}

const throttledEndDate = throttle(() => Course.endDate && dayjs().isAfter(Course.endDate), 1000);

const Course0 = {
  ...Course,
  get hasEnded(): boolean {
    return Boolean(throttledEndDate());
  },
};

export default Course0;
