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

import {
  DISCUSSION_WRITING_START,
  DISCUSSION_WRITING_SAVE_START,
  DISCUSSION_WRITING_SAVE_ERROR,
  DISCUSSION_WRITING_SAVE_SUCCESS,
  DISCUSSION_WRITING_DISCARD,
  DISCUSSION_WRITING_KEEP_WORKING,
} from '../actionTypes.js';

const writingReducer = (writingState = {}, action) => {
  switch (action.type) {
    case DISCUSSION_WRITING_START:
      return {
        ...writingState,
        editing: true,
        saving: false,
        replyToId: action.data.replyToId,
      };

    case DISCUSSION_WRITING_SAVE_START:
      return {
        ...writingState,
        saving: true,
      };

    case DISCUSSION_WRITING_SAVE_ERROR:
      return {
        ...writingState,
        saving: false,
        error: action.data.error,
      };

    case DISCUSSION_WRITING_SAVE_SUCCESS:
    case DISCUSSION_WRITING_DISCARD:
    case DISCUSSION_WRITING_KEEP_WORKING:
      return {
        ...writingState,
        editing: false,
        saving: false,
        replyToId: null,
        error: null,
      };

    default:
      return writingState;
  }
};

export default writingReducer;
