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

//lscache
import 'lscache';

//for touch events to click events
import 'fastclick';

// angular base
import angularAnimate from 'angular-animate';
import angularSanitize from 'angular-sanitize';
import angularAria from 'angular-aria';
import angularTranslate from 'angular-translate';

//angular UI
import angularScroll from 'angular-scroll';

import ngRedux from 'ng-redux';

export default [
  angularAnimate,
  angularSanitize,
  angularAria,
  angularTranslate,
  angularScroll,
  ngRedux,
];
