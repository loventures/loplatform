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

import { grade, makeGradeDisplayMethods, type GradeDisplayMethods } from '../filters/pure/grade.ts';
import { instant } from '../i18n/pure/i18n.ts';

/**
 * Pure `gradeFilter` for React/redux callers — replaces the `lojector.get('gradeFilter')` reach-in (the
 * AngularJS `grade` filter / `gradeFilter` injectable). The display logic is the pure, unit-tested
 * `filters/pure/grade.ts`; the only translation dependency is the pure `instant` (used for the no-grade /
 * pass-fail labels). The methods are memoised so they're built at most once. The Angular adapter
 * (`filters/gradeFilter.jsx`) still registers the `grade` filter + `GradeDisplayMethods` for any `| grade`
 * template usage.
 */
let methods: GradeDisplayMethods | undefined;
const getMethods = (): GradeDisplayMethods =>
  (methods ??= makeGradeDisplayMethods((key: string) => instant(key)));

export const gradeFilter = (score: any, ...rest: any[]): unknown => grade(getMethods(), score, ...rest);
