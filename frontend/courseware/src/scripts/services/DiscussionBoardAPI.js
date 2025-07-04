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

import DiscussionBoardAPILight from './DiscussionBoardAPILight.js';
import DiscussionPostAPI from './DiscussionPostAPI.js';
import DiscussionPostReplyAPI from './DiscussionPostReplyAPI.js';
import DiscussionPostStateAPI from './DiscussionPostStateAPI.js';

export default angular
  .module('lo.services.DiscussionBoardAPI', [
    DiscussionBoardAPILight.name,
    DiscussionPostAPI.name,
    DiscussionPostReplyAPI.name,
    DiscussionPostStateAPI.name,
  ])
  .service('DiscussionBoardAPI', [
    'DiscussionBoardAPILight',
    'DiscussionPostAPI',
    'DiscussionPostReplyAPI',
    'DiscussionPostStateAPI',
    function (
      DiscussionBoardAPILight,
      DiscussionPostAPI,
      DiscussionPostReplyAPI,
      DiscussionPostStateAPI
    ) {
      return {
        ...DiscussionBoardAPILight,
        ...DiscussionPostAPI,
        ...DiscussionPostReplyAPI,
        ...DiscussionPostStateAPI,
      };
    },
  ]);
