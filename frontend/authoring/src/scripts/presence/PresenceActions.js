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

import gretchen from '../grfetchen/';
import { uniq } from 'lodash';

import { TOAST_TYPES, openToast } from '../toast/actions';

/*
 * action types
 */

export const SET_PRESENCE_STATE = 'SET_PRESENCE_STATE';
export const SET_TAB_VISIBLE = 'SET_TAB_VISIBLE';
export const SET_IDLE_STATE = 'SET_IDLE_STATE';
export const SET_PRESENCE_ID = 'SET_PRESENCE_ID';
export const SET_PRESENT_USERS = 'SET_PRESENT_USERS';
export const SET_PROFILES = 'SET_PROFILES';

export const OPEN_CHAT = 'OPEN_CHAT';
export const CLOSE_CHAT = 'CLOSE_CHAT';
export const MESSAGE_RECEIVED = 'MESSAGE_RECEIVED';
export const HISTORY_LOADED = 'HISTORY_LOADED';
export const BRANCH_REINDEXING = 'BRANCH_REINDEXING';

/*
 * action creators
 */

export const setPresenceState = state => ({
  type: SET_PRESENCE_STATE,
  state,
});

export const setTabVisible = visible => ({
  type: SET_TAB_VISIBLE,
  visible,
});

export const setIdleState = idling => ({
  type: SET_IDLE_STATE,
  idling,
});

export const setPresenceId = presenceId => ({
  type: SET_PRESENCE_ID,
  presenceId,
});

export const setProfiles = profiles => ({
  type: SET_PROFILES,
  profiles,
});

export const branchReindexing = (branch, reindexing) => dispatch => {
  dispatch(openToast(reindexing ? 'Reindex started.' : 'Reindex complete.', TOAST_TYPES.SUCCESS));
  dispatch({
    type: BRANCH_REINDEXING,
    branch,
    reindexing,
  });
};

export const fetchProfiles = handles => (dispatch, getState) => {
  const {
    presence: { profiles },
  } = getState();
  const unknown = uniq(handles).filter(handle => !profiles[handle]);
  if (unknown.length) {
    gretchen
      .get(`/api/v2/profiles;prefilter=handle:in(${unknown.join(',')})`)
      .exec()
      .then(({ objects }) => dispatch(setProfiles(objects)));
  }
};

export const setPresentUsers = users => (dispatch, getState) => {
  const contextPaths = getState().graphEdits.contentTree.contextPaths;
  dispatch(fetchProfiles(users.map(([handle]) => handle)));
  dispatch({
    type: SET_PRESENT_USERS,
    users,
    contextPaths,
  });
};

export const historyLoaded = (room, messages) => ({
  type: HISTORY_LOADED,
  room,
  messages,
});

export const loadHistory = room => (dispatch, getState) => {
  const {
    presence: { roomConversations },
  } = getState();
  const conversation = roomConversations[room];
  if (conversation && !conversation.historyLoaded) {
    gretchen
      .get('/api/v2/chats/:room/messages;offset=0;limit=50')
      .params({ room })
      .exec()
      .then(({ objects: messages }) => {
        dispatch(historyLoaded(room, messages));
        dispatch(fetchProfiles(messages.map(m => m.sender)));
      });
  }
};

export const openChat = (branch, room) => dispatch => {
  dispatch({
    type: OPEN_CHAT,
    branch,
    room,
  });
  dispatch(loadHistory(room));
};

export const closeChat = () => ({
  type: CLOSE_CHAT,
});

export const toggleBranchChat = branch => (dispatch, getState) => {
  const {
    presence: { branchRooms, chatRoom },
  } = getState();
  const room = branchRooms[branch];
  if (chatRoom) {
    dispatch(closeChat());
  } else if (room) {
    dispatch(openChat(branch, room));
  } else {
    gretchen
      .post('/api/v2/chats')
      .data({ _type: 'branch', branch })
      .exec()
      .then(({ id }) => dispatch(openChat(branch, id)));
  }
};

export const sendMessage = (room, message) => () => {
  gretchen.post('/api/v2/chats/:room/messages').params({ room }).data({ message }).exec();
};

export const sendTyping = (room, typing) => () => {
  gretchen.post('/api/v2/chats/:room/messages').params({ room }).data({ typing }).exec();
};

export const messageReceived =
  (id, sender, room, timestamp, message, typing) => (dispatch, getState) => {
    const {
      presence: { roomConversations },
      layout: { branchId },
    } = getState();
    dispatch({
      type: MESSAGE_RECEIVED,
      id,
      sender,
      room,
      timestamp,
      message,
      typing,
    });
    dispatch(fetchProfiles([sender]));
    if (message && (!roomConversations[room] || !roomConversations[room].historyLoaded)) {
      // If there were more chat rooms we would want to check this message was in the branch room
      dispatch(toggleBranchChat(branchId));
    }
  };
