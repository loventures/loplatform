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

import { cloneDeep, extend } from 'lodash';

import defaultFeatures from '../utilities/defaultFeatures';
import { preferencesToSettings } from '../utilities/preferencesToSettings';
import { Roles } from '../utilities/pure/roles.ts';
import settings from '../utilities/settingsService';

/**
 * Eager bootstrap initialization of the pure `Roles` (utilities/pure/roles.ts) and `settings`
 * (utilities/settingsService.ts) singletons. This re-homes the only live initializer of those
 * singletons — the former AngularJS `bootstrap/globalFeatures.js` `.config` block — out of the
 * Angular injector.
 *
 * It runs at MODULE LOAD (as a top-level side effect), so this module must be imported FIRST in
 * index.tsx, before anything that reads the singletons at its own module load. In particular,
 * `utilities/currentUserData.ts` reads `Roles.getPrimaryRole()` at module load, so `Roles` must be
 * initialized before `currentUserData` is first imported.
 *
 * The dropped pieces of the old `.config` block were Angular-only: `UserProvider.initUser(...)` (the
 * pure currentUser self-derives from `Roles` + `window.lo_platform.user`) and
 * `$animateProvider.classNameFilter(/carousel/)` (ngAnimate goes away with AngularJS).
 */

/** Mirrors globalFeatures' `getFeatures`: defaultFeatures + the two hard overrides + server prefs. */
const getFeatures = () => {
  const defaults = cloneDeep(defaultFeatures);
  const server = preferencesToSettings(window.lo_platform.preferences);

  return extend(
    defaults,
    {
      GradebookExportRollup: { isEnabled: true },
      SkippingIsOK: { isEnabled: false },
    },
    server
  );
};

if (typeof window !== 'undefined' && window.lo_platform) {
  const lop = window.lo_platform;
  const indexPages = {
    student: 'index.html',
    instructor: 'instructor.html',
    administrator: 'admin.html',
  };

  const features = getFeatures();

  Roles.init(lop.course_roles, indexPages, features);
  // `user.role` mirrors the original globalFeatures config: `UserInfo` has no `role` field, so this
  // was — and stays — `undefined` at runtime (Settings only ever uses `userRole` as a cache-key
  // suffix, defaulting to 'student' when nullish).
  settings.init(lop.user?.id, (lop.user as { role?: string } | undefined)?.role, features);
}
