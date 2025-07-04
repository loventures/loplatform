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

import board from './DiscussionBoardActions.js';
import data from './DiscussionDataActions.js';
import loading from './DiscussionLoadingActions.js';

import post from './DiscussionPostActions.js';
import writing from './DiscussionWritingActions.js';

import search from './DiscussionSearchActions.js';
import sort from './DiscussionSortActions.js';

import view from './DiscussionViewActions.js';
import jumper from './DiscussionJumperActions.js';

angular.module('lo.discussion.actions', [
  board.name,
  data.name,
  loading.name,

  post.name,

  writing.name,

  sort.name,
  search.name,

  jumper.name,
  view.name,
]);

export default angular.module('lo.discussion.actions');
