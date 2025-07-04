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

import { createKeyedReducer } from './createKeyedReducer';

/*
 * Event:  GradeUpdate {
 *   "courseId" : 1574079299,
 *   "userId" : 1271829970,
 *   "edgePath" : "_root_",
 *   "grade" : {
 *     "grade" : 296.0,
 *     "max" : 600.0,
 *     "latestChange" : "2020-06-18T20:36:54.533Z"
 *   }
 * }
 */

export type OverallGrade = {
  user_id: number;
  grade?: number;
  latestChange?: string; // a date
  max: number;
};

export const [overallGradeReducer, overallGradeMerge, overallGradeReplace, overallGradeInvalidate] =
  createKeyedReducer<OverallGrade>('OverallGrade');
