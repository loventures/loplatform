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

import { request } from '../utilities/request.ts';
import { makeCompetencyBreakdownService } from './pure/competencyBreakdownService.ts';

/**
 * Native (axios) CompetencyBreakdownService for React/redux callers — replaces
 * `lojector.get('CompetencyBreakdownService')`. The logic lives in the
 * unit-tested ./pure/competencyBreakdownService.ts; this passes the native
 * (axios) Request, which resolves via native Promises (no digest) — correct for
 * its redux/react-query consumers (the quiz-activity loader feeds redux via the
 * $ngRedux digest bridge).
 */
export const competencyBreakdownService = makeCompetencyBreakdownService(request);

export default competencyBreakdownService;
