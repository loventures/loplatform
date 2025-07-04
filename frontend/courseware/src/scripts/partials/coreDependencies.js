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

/**
 *  This set of scripts that are truely global and ubiquitously used
 *  TODO: Move out any feature imports to the places
 *  where they are actually used.
 */

//TODO make these go
import modals from '../modals/index.js';
import directives from '../directives/index.js';
//end of go

import filters from '../filters/index.js';
import locales from '../bootstrap/i18n.jsx';
import lscacheExtend from '../utilities/lscacheExtend.jsx';

export default [lscacheExtend.name, directives.name, locales.name, filters.name, modals.name];
