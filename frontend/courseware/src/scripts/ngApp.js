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

import frameworkDependencies from './partials/frameworkDependencies';
import coreDependencies from './partials/coreDependencies';

import bootstrap from './bootstrap';
import appContainer from './landmarks/ngAppContainer';
import instructorPages from './instructorPages/ng';
import commonPages from './commonPages/ng';
import { initLo } from './loject';

var app_dependencies = [
  ...frameworkDependencies,
  ...coreDependencies,
  bootstrap.name,
  appContainer.name,
  instructorPages.name,
  commonPages.name,
];

// angular.module('ple', app_dependencies);

export default angular.module('ple', app_dependencies).run([
  '$injector',
  '$q',
  function ($injector, $q) {
    initLo($injector, $q);
  },
]);
