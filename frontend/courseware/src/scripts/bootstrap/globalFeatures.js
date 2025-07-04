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

import { extend } from 'lodash';
import defaultFeatures from '../utilities/defaultFeatures';
import { preferencesToSettings } from '../utilities/preferencesToSettings';
import settings from '../utilities/settings';
import User from '../utilities/User';

export default angular
  .module('ple.bootstrap.features', [settings.name, User.name])
  .constant('getFeatures', function () {
    var defaults = angular.copy(defaultFeatures);
    var server = preferencesToSettings(window.lo_platform.preferences);

    var features = extend(
      defaults,
      {
        GradebookExportRollup: { isEnabled: true },
        SkippingIsOK: { isEnabled: false },
      },
      server
    );

    return features;
  })
  .config([
    'SettingsProvider',
    'UserProvider',
    'RolesProvider',
    'getFeatures',
    '$animateProvider',
    function (SettingsProvider, UserProvider, RolesProvider, getFeatures, $animateProvider) {
      //User specific settings
      var lop = window.lo_platform;
      var user = lop.user || {};
      var indexPages = {
        student: 'index.html',
        instructor: 'instructor.html',
        administrator: 'admin.html',
      };

      var features = getFeatures();
      RolesProvider.init(lop.course_roles, indexPages, features);
      UserProvider.initUser(RolesProvider.getPrimaryRole(), user);
      SettingsProvider.init(user.id, user.role, features);

      //CBLPROD-1096, angular animate screws with the bootstrap UI carousels...
      //Turn it off for that
      $animateProvider.classNameFilter(/carousel/);
    },
  ]);
