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

import { isEqual } from 'lodash';

import { INITIALIZE_DCM } from '../dcmStoreConstants';
import {
  addMessage,
  closeConversation,
  newConversation,
  openConversation,
  setHistory,
} from './model/Conversation';
import {
  BRANCH_REINDEXING,
  CLOSE_CHAT,
  HISTORY_LOADED,
  MESSAGE_RECEIVED,
  OPEN_CHAT,
  SET_IDLE_STATE,
  SET_PRESENCE_ID,
  SET_PRESENCE_STATE,
  SET_PRESENT_USERS,
  SET_PROFILES,
  SET_TAB_VISIBLE,
} from './PresenceActions';

type MinimalPresentUser = {
  handle: string;
  lastActive: number;
};

export type FullPresentUser = MinimalPresentUser & {
  id: number;
  location: string | null;
  fullName: string;
  givenName: string;
  familyName: string;
  thumbnailId: number;
  letter: string;
  color: string;
};

export type PresentUser = MinimalPresentUser | FullPresentUser;

export const isFullPresentUser = (u: PresentUser): u is FullPresentUser => !!(u as any)?.id;

export type PresenceState = {
  myself?: string;
  started: boolean; // Has this service been started; if so, it may connect to the server
  online: boolean; // Is presence online
  offline: boolean; // Is presence offline (broken),
  idling: boolean;
  tabVisible: boolean;
  presenceId: string | null;
  branchRooms: Record<string, string>;
  chatRoom: string | null;
  presentUsers: Array<[string, number, string | null]>;
  profiles: Record<string, PresentUser>;
  usersAtAsset: Record<string, string[]>; // handles in a location
  usersWithinAsset: Record<string, string[]>; // handles in a location
  usersOnField: Record<string, string>;
  roomConversations: any;
  unreadCount: number;
  reindexing: Record<number, boolean>; // Branches that are reindexing...
};

const initialState: PresenceState = {
  myself: undefined,
  started: false,
  online: false,
  offline: false,
  idling: false,
  tabVisible: true,
  presenceId: null,
  branchRooms: {},
  chatRoom: null,
  presentUsers: [],
  profiles: {},
  usersAtAsset: {},
  usersWithinAsset: {},
  usersOnField: {},
  roomConversations: {},
  unreadCount: 0,
  reindexing: {},
};

export default function presence(state = initialState, action) {
  switch (action.type) {
    case INITIALIZE_DCM: {
      const myself = action.user?.profile?.handle;
      return {
        ...state,
        myself,
      };
    }
    case SET_PRESENCE_STATE: {
      const { started, online, offline, idling } = action.state;
      return {
        ...state,
        started,
        online,
        offline,
        idling,
      };
    }
    case SET_TAB_VISIBLE: {
      return {
        ...state,
        tabVisible: action.visible,
      };
    }
    case SET_IDLE_STATE:
      return {
        ...state,
        idling: action.idling,
      };
    case SET_PRESENCE_ID:
      return {
        ...state,
        presenceId: action.presenceId,
      };
    case SET_PRESENT_USERS: {
      const profiles = { ...state.profiles };
      const usersAtAsset: Record<string, string[]> = {};
      const usersWithinAsset: Record<string, string[]> = {};
      const usersOnField: Record<string, string> = {};
      const contextPaths = action.contextPaths;
      for (const [handle, lastActive, fullLocation] of action.users) {
        const profile = profiles[handle];
        const idx = fullLocation?.indexOf(':') ?? -1;
        const location = idx < 0 ? fullLocation : fullLocation.substring(0, idx);
        profiles[handle] = profile ? { ...profile, lastActive, location } : { handle, lastActive };
        if (fullLocation && handle !== state.myself) {
          (usersAtAsset[location] ??= []).push(handle);
          (usersWithinAsset[location] ??= []).push(handle);
          const contextPath = contextPaths[location];
          if (contextPath) {
            for (const name of contextPath.split('.')) {
              (usersWithinAsset[name] ??= []).push(handle);
            }
          }
          if (idx > 0) usersOnField[fullLocation] = handle;
        }
      }
      // All this nonsense is to try to reduce the redux repaint churn because the
      // present user list for assets will not referentially change unless the users
      // change. We often receive last-active updates and don't want these to trigger
      // repaint.
      Object.values(usersAtAsset).forEach(v => v.sort());
      Object.values(usersWithinAsset).forEach(v => v.sort());
      for (const [k, v] of Object.entries(state.usersAtAsset)) {
        if (isEqual(v, usersAtAsset[k])) usersAtAsset[k] = v;
      }
      for (const [k, v] of Object.entries(state.usersWithinAsset)) {
        if (isEqual(v, usersWithinAsset[k])) usersWithinAsset[k] = v;
      }
      return {
        ...state,
        presentUsers: action.users,
        profiles,
        usersAtAsset,
        usersWithinAsset,
        usersOnField,
      };
    }
    case SET_PROFILES: {
      const profiles = { ...state.profiles };
      for (const p of action.profiles) {
        p.letter = (p.givenName || p.fullName || '?').charAt(0);
        p.color = 'hsl(' + (((p.id || 0) * 47) % 360) + ', 50%, 40%)';
        const profile = profiles[p.handle];
        profiles[p.handle] = profile ? { ...profile, ...p } : p;
      }
      return {
        ...state,
        profiles,
      };
    }
    case OPEN_CHAT: {
      if (state.chatRoom) return state; // no-op if chat already open...
      const { branchRooms, roomConversations } = state;
      const existingConversation = roomConversations[action.room];
      const conversation = existingConversation
        ? openConversation(existingConversation)
        : newConversation(action.room, true);
      return {
        ...state,
        branchRooms: { ...branchRooms, [action.branch]: action.room },
        chatRoom: action.room,
        roomConversations: {
          ...roomConversations,
          [action.room]: conversation,
        },
        unreadCount: 0,
      };
    }
    case CLOSE_CHAT: {
      const { chatRoom, roomConversations } = state;
      if (!chatRoom) {
        return state;
      } else {
        return {
          ...state,
          chatRoom: null,
          roomConversations: {
            ...roomConversations,
            [chatRoom]: closeConversation(roomConversations[chatRoom]),
          },
        };
      }
    }
    case HISTORY_LOADED: {
      const { roomConversations } = state;
      const { room, messages } = action;
      return {
        ...state,
        roomConversations: {
          ...roomConversations,
          [room]: setHistory(roomConversations[room], messages),
        },
      };
    }
    case MESSAGE_RECEIVED: {
      const { id, sender, room, timestamp, message, typing } = action;
      const { roomConversations } = state;
      const initialConversation = roomConversations[room] || newConversation(room, false);
      const conversation = addMessage(initialConversation, id, sender, timestamp, message, typing);
      return {
        ...state,
        roomConversations: {
          ...roomConversations,
          [room]: conversation,
        },
        unreadCount: conversation.unreadCount, // naïve but correct for one-chat authoring
      };
    }
    case BRANCH_REINDEXING: {
      const { branch, reindexing } = action;
      return {
        ...state,
        reindexing: {
          ...state.reindexing,
          [branch]: reindexing,
        },
      };
    }
    default:
      return state;
  }
}
