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

import lscache from 'lscache';

import { stepLoad } from '../utilities/pure/stepLoader.ts';
import UrlBuilder from '../utilities/UrlBuilder.ts';
import { request } from '../utilities/request.ts';
import { userModel } from '../users/userModel.ts';
import { makeEnrolledUserService } from './pure/enrolledUserService.ts';

/**
 * Native (axios) enrolledUserService singleton for React/redux callers — replaces
 * `lojector.get('enrolledUserService')`. Built from the pure `makeEnrolledUserService` with the native
 * `request` and the (Settings-extended) `lscache` singleton (`lscache.userLoad` is monkey-patched in
 * utilities/lscacheExtend.jsx at bootstrap). Mirrors `progressService.ts` / `submissionActivityAPI.ts`.
 *
 * StepLoader is still wired thinly — the pure `stepLoad` over the native `request` (exactly as the
 * Angular adapter does). UserModel is now the pure `userModel` singleton (users/userModel.ts).
 */
const StepLoader = {
  stepLoad: (url: any) => stepLoad(request, typeof url === 'string' ? new UrlBuilder(url) : url),
};

// `lscache.userLoad` is a runtime monkey-patch (lscacheExtend.jsx), absent from the npm lscache types.
export const enrolledUserService = makeEnrolledUserService(StepLoader, userModel, request, lscache as any);

export default enrolledUserService;
