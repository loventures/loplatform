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

import { loConfig } from '../../bootstrap/loConfig.ts';
import UrlBuilder from '../../utilities/UrlBuilder.js';

/** The lscache surface this service uses (the custom userLoad extension). */
export interface LsCacheLike {
  userLoad(url: any, params: any, method: any, lifespan: any): PromiseLike<any>;
}

/**
 * Course-context fetch API, migrated verbatim from the AngularJS `CoursesAPI`
 * service to plain TS taking the injected (extended) `lscache`. The result is
 * cached per-user via lscache.userLoad with DEFAULT_LIFESPAN minutes.
 */
export const makeCoursesAPI = (lscache: LsCacheLike) => {
  const service: any = {
    DEFAULT_LIFESPAN: 3,
  };

  service.getCourse = function (courseId: any) {
    const url = new (UrlBuilder as any)(loConfig.course.context, {
      courseId: courseId,
    });

    return lscache.userLoad(url, null, 'get', service.DEFAULT_LIFESPAN);
  };

  return service;
};

export type CoursesAPI = ReturnType<typeof makeCoursesAPI>;
