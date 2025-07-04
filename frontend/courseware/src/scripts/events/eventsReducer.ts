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

import { flatMap, isBoolean, last } from 'lodash';
import lscache from 'lscache';
import { Dispatch } from 'react';
import { AnyAction, Reducer } from 'redux';
import { createAction, isActionOf } from 'typesafe-actions';

import { Announcement } from '../api/announcementsApi';
import { CourseState } from '../loRedux';
import { fetchPresenceId } from './eventsApi';

export type PresenceEvent =
  | UnclassifiedEvent
  | StartEvent
  | HeartbeatEvent
  | ChatMessageEvent
  | ChatTypingEvent
  | AnnouncementEvent
  | AnnouncementEndEvent;

export type UnclassifiedEvent = Record<string, unknown>;

export type StartEvent = {
  type: 'Start';
};

export type HeartbeatEvent = {
  type: 'Heartbeat';
};

export type ChatMessageEvent = {
  id: number;
  message: string;
  room: number;
  sender: string;
  timestamp: string;
  typing: null;
};

export type ChatTypingEvent = {
  id: null;
  message: null;
  room: number;
  sender: string;
  timestamp: string;
  typing: true;
};

export type AnnouncementEvent = {
  type: 'Announcement';
} & Announcement;

export type AnnouncementEndEvent = {
  type: 'AnnouncementEnd';
} & Announcement;

export function isAnnouncementEvent(event: PresenceEvent): event is AnnouncementEvent {
  return (event as AnnouncementEvent).type === 'Announcement';
}

export function isAnnouncementEndEvent(event: PresenceEvent): event is AnnouncementEndEvent {
  return (event as AnnouncementEndEvent).type === 'AnnouncementEnd';
}

export type PresenceEventsState =
  | {
      initialized: false;
      visible: boolean;
      error?: any;
    }
  | {
      initialized: true;
      lastActive: number; //updated via an idleService, used to start a session, update heartbeat
      messages: PresenceEvent[];
      stopped: boolean;
      visible: boolean;
      error?: any;
    };

export type EventStreamOptions = {
  presenceId: string;
  onEvent: (event: PresenceEvent) => void;
  onError: (err: any) => void;
};

export const setInitializedAction = createAction('PRESENCE_SET_INITIALIZED')();
export const receiveMessageAction = createAction('PRESENCE_RECEIVE_MESSAGE')<PresenceEvent>();

export const stopAction = createAction('PRESENCE_STOP')();
export const startAction = createAction('PRESENCE_START')();

export const updateLastActiveAction = createAction('PRESENCE_UPDATE_LAST_ACTIVE')();

export const setErrorAction = createAction('PRESENCE_SET_ERROR')<{
  err: any;
}>();

const getPresenceVisibilityKey = (id: any) => `PresenceVisibility-${id}`;

const getStoredVisibility = () => {
  const { user } = window.lo_platform;
  return user && isBoolean(lscache.get(getPresenceVisibilityKey(user.id)))
    ? lscache.get(getPresenceVisibilityKey(user.id))
    : true;
};

function isChatMessageEvent(event: PresenceEvent): event is ChatMessageEvent {
  return typeof (event as ChatMessageEvent).id === 'number';
}

function isChatTypingEvent(event: PresenceEvent): event is ChatTypingEvent {
  return (event as ChatTypingEvent).typing === true;
}

const reducer: Reducer<PresenceEventsState, AnyAction> = (
  state = { initialized: false, visible: getStoredVisibility() },
  action
) => {
  if (isActionOf(setInitializedAction, action)) {
    return {
      ...state,
      initialized: true,
      messages: [],
      stopped: false,
      lastActive: Date.now(),
    };
  } else if (isActionOf(receiveMessageAction, action) && state.initialized) {
    if (isChatMessageEvent(action.payload) || isChatTypingEvent(action.payload)) {
      return {
        ...state,
        messages: [...state.messages, action.payload],
      };
    }
    return state;
  } else if (isActionOf(stopAction, action) && state.initialized) {
    return {
      ...state,
      stopped: false,
    };
  } else if (isActionOf(startAction, action) && state.initialized) {
    return {
      ...state,
      stopped: true,
    };
  } else if (isActionOf(updateLastActiveAction, action) && state.initialized) {
    return {
      ...state,
      lastActive: Date.now(),
    };
  } else if (isActionOf(setErrorAction, action)) {
    return {
      ...state,
      error: action.payload.err,
    };
  } else {
    return state;
  }
};

export const eventsReducer = reducer;

export const startPresenceListenerAction =
  (eventStream: (opts: EventStreamOptions) => void) =>
  (dispatch: Dispatch<any>, getState: () => CourseState) => {
    const millisSinceActive = 0;
    const {
      course: { id: context },
    } = window.lo_platform;
    const inScenes = [{ context }];
    const visible = getState().events.visible;
    const { events } = getState();
    const lastMessageId = () => {
      if (events.initialized) {
        const lastMessage = last(
          flatMap(events.messages, event => (isChatMessageEvent(event) ? [event] : []))
        );
        return lastMessage ? lastMessage.id : null;
      } else {
        return null;
      }
    };

    fetchPresenceId({
      visible,
      millisSinceActive,
      inScenes,
      lastEventId: lastMessageId(),
    })
      .then(presenceId => {
        dispatch(setInitializedAction());
        eventStream({
          presenceId,
          onEvent: event => {
            dispatch(receiveMessageAction(event));
          },
          onError: err => {
            console.warn('failed to initialized event stream:', err);
            dispatch(setErrorAction({ err }));
          },
        });
      })
      .catch(err => {
        console.warn('failed to fetch presence id, cancelling event subscription:', err);
        dispatch(setErrorAction({ err }));
      });
  };
