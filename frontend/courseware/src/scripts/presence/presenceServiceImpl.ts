/*
 * LO Platform copyright (C) 2007–2026 LO Ventures LLC.
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
  EVENT_TYPE_CONTROL,
  EVENT_DATA_TYPE_START,
  EVENT_DATA_TYPE_HEARTBEAT,
  EVENT_DATA_TYPE_ENDED,
  EVENT_TYPE_LOGOUT,
  EVENT_TYPE_PRESENCE_OPENED,
  EVENT_TYPE_PRESENCE_CLOSED,
} from '../utilities/presenceEvents.js';

import settings from '../utilities/settingsService';
import { courseReduxStore } from '../loRedux/index.ts';
import {
  setInitializedAction,
  setErrorAction,
  receiveMessageAction,
} from '../events/eventsReducer.ts';
import { disablePresence } from '../utilities/preferences.ts';

import { sseEventStream } from './streams/sseEventStreamImpl.ts';
import { pollingEventStream } from './streams/pollingEventStreamImpl.ts';
import { presenceHeartbeat } from './presenceHeartbeatImpl.ts';
import { presenceSession } from './presenceSessionImpl.ts';
import { presenceAPI } from '../services/presenceAPI.ts';
import type { SceneInfo } from '../events/eventsApi.ts';

/**
 * Pure-TS singleton port of the AngularJS `PresenceService`: the realtime presence
 * hub. Owns the custom event bus (`listeners[type][]` + `on(type, cb)` returning a
 * deregister fn) and orchestrates the heartbeat, session and event stream.
 *
 * Angular runtime deps replaced:
 *  - `$ngRedux.dispatch(x)`      → `courseReduxStore.dispatch(x)`
 *  - `$q.reject(...)`            → `Promise.reject(...)`
 *  - `$rootScope` idle wiring    → IdleService calls `onIdleStart`/`onIdleEnd` directly
 *
 * `PresenceAPI` is now the native `presenceAPI` singleton (direct import). The
 * event-stream choice (SSE vs polling) reads `Settings` lazily on first use so the
 * lo bootstrap is ready.
 */

const Settings = {
  get: () => settings,
};

export type PresenceEventListener = (data: any, event?: any) => void;

export interface PresenceState {
  started: boolean;
  online: boolean;
  offline: boolean;
}

class PresenceServiceImpl {
  openSessionRequest: Promise<any> | null = null;
  listeners: Record<string, PresenceEventListener[]> = {};
  state: PresenceState = {
    started: false, // whether the presence machinery is initialied
    online: false, // whether presence is successfully connected
    offline: false, // whether presene encountered errors that stopped connection
  };
  presenceId: any;

  private _eventStream: typeof sseEventStream | typeof pollingEventStream | null = null;

  // Choose SSE vs polling lazily (Settings is reached via lojector, not ready at import).
  private get EventStream(): typeof sseEventStream | typeof pollingEventStream {
    if (!this._eventStream) {
      const { useSse } = Settings.get().getSettings('Presence') || { useSse: true };
      this._eventStream = useSse ? sseEventStream : pollingEventStream;
    }
    return this._eventStream;
  }

  private serverLog = (_message: string, _payload?: any): void => {
    // try {
    //   $http
    //     .post('/api/v2/log/info', { message, payload })
    //     .then(() => {})
    //     .catch(e => console.log(e));
    // } catch (e) {
    //   console.log(e);
    // }
  };

  onIdleStart = (): void => {
    presenceHeartbeat.beatSlow();
  };

  onIdleEnd = (): void => {
    presenceHeartbeat.beatFast();
  };

  init = ({ presenceId, course }: { presenceId?: any; course?: any } = {}): void => {
    presenceSession.setScenes([{ context: course }]);
    this.openPresence(presenceId);
  };

  reconnectPresence = (): void => {
    this.openPresence(this.presenceId);
  };

  openPresence = (presenceId?: any): Promise<any> => {
    if (disablePresence) {
      return Promise.reject('Presence disabled');
    }

    if (this.openSessionRequest) {
      return this.openSessionRequest;
    }

    const sessionInfo = presenceSession.getSummary();

    this.state.started = true;

    this.openSessionRequest = presenceId
      ? presenceAPI.reconnectSession(presenceId, sessionInfo)
      : presenceAPI.openSession(sessionInfo);

    this.openSessionRequest!.then(
      (response: any) => {
        this.serverLog('Presence id', response.data);
        try {
          courseReduxStore.dispatch(setInitializedAction());
          this.state.online = true;
          this.presenceId = response.data.presenceId;
          this.startServices(this.presenceId);
          this.handleEvent({
            type: EVENT_TYPE_PRESENCE_OPENED,
          });
        } catch (e) {
          this.serverLog('Presence init error', e);
        }
      },
      (err: any) => {
        courseReduxStore.dispatch(setErrorAction({ err }));
        this.state.offline = true;
        this.openSessionRequest = null;
        this.serverLog('Presence open error', err);
      }
    );

    return this.openSessionRequest!;
  };

  closePresence = (): void => {
    this.state.started = false;
    this.state.online = false;
    this.handleEvent({
      type: EVENT_TYPE_PRESENCE_CLOSED,
    });
    this.stopServices();
  };

  deleteSessionSynchronously = (): void => {
    if (this.presenceId) {
      presenceAPI.deleteSessionSynchronously(this.presenceId);
    }
  };

  startServices = (presenceId: any): void => {
    presenceHeartbeat.startHeartbeat(
      presenceId,
      () => {},
      () => this.closePresence()
    );

    this.EventStream.start({
      presenceId: presenceId,
      onEvent: (event: any) => {
        presenceSession.updateLastEventId(event.id);
        this.handleEvent(event);
      },
      onError: (error: any) => this.handleEventError(error),
    });

    // Register every event type accumulated before the stream started (e.g. ChatMessage / ScenePresence
    // auto-registered by presentConversations / presentUsers at construction).
    Object.keys(this.listeners).forEach(type => this.EventStream.addEventType(type));

    this.on(EVENT_TYPE_CONTROL, this.controlChannelListener);
    this.on(EVENT_TYPE_LOGOUT, () => this.closePresence());

    window.addEventListener('beforeunload', this.deleteSessionSynchronously);
  };

  stopServices = (error?: any): void => {
    this.EventStream.stop(error);

    this.listeners = {};

    presenceHeartbeat.stopHeartbeat();

    window.removeEventListener('beforeunload', this.deleteSessionSynchronously);
  };

  controlChannelListener = (data: any, event?: any): void => {
    switch (data.type) {
      case EVENT_DATA_TYPE_START:
      case EVENT_DATA_TYPE_HEARTBEAT:
        return;
      case EVENT_DATA_TYPE_ENDED:
        this.closePresence();
        return;
      default:
        console.log('Unknown control channel event', data, event);
        return;
    }
  };

  handleEvent = (event: any = {}): void => {
    const { type, data = '{}' } = event;
    const parsedData = window.JSON.parse(data);
    courseReduxStore.dispatch(receiveMessageAction(parsedData));
    console.log('Event: ', type, data);
    if (this.listeners[type] && this.listeners[type].length) {
      for (const listener of this.listeners[type]) {
        listener(parsedData, event);
      }
    }
  };

  handleEventError = (err: any): void => {
    courseReduxStore.dispatch(setErrorAction({ err }));
    console.error('Event Error: ', err);
    console.log('Shutting down presence due to event source error');
    this.closePresence();
  };

  /**
   * Add a listener for server events.
   * @param type - the event type
   * @param callback - the listener.
   * @return a deregistration function
   */
  on = (type: string, callback: PresenceEventListener): (() => void) => {
    if (!this.listeners[type]) {
      this.listeners[type] = [];
      // Only touch the (SSE) stream once presence is running; before that the EventStream getter reads
      // Settings via lojector, which isn't wired yet when presentUsers / presentConversations
      // auto-register at module-load time. startServices() registers all accumulated types on start.
      if (this.state.started) {
        this.EventStream.addEventType(type);
      }
    }

    this.listeners[type].push(callback);

    const deregister = () => {
      const callbacks = this.listeners[type];
      if (callbacks) {
        const index = callbacks.indexOf(callback);
        if (index >= 0) {
          callbacks.splice(index, 1);
        }
      }
    };

    return deregister;
  };

  onForScope = (type: string, callback: PresenceEventListener, scope: any): void => {
    const deregister = this.on(type, callback);

    scope.$on('$destroy', deregister);
  };

  onForCtrl = (type: string, callback: PresenceEventListener, ctrl: any): void => {
    const deregister = this.on(type, callback);

    const oldDestroy = ctrl.$onDestroy || (() => {});
    ctrl.$onDestroy = () => {
      deregister();
      oldDestroy.apply(ctrl);
    };
  };

  setScenes = (scene: SceneInfo): void => {
    if (presenceSession.setScenes(scene) && presenceHeartbeat.presenceId) {
      presenceHeartbeat.restartHeartbeatSchedule();
    }
  };

  followScene = (scene: any): void => {
    if (presenceSession.followScene(scene) && presenceHeartbeat.presenceId) {
      presenceHeartbeat.restartHeartbeatSchedule();
    }
  };

  setVisibleToOthers = (visibility?: boolean): void => {
    presenceSession.setVisibleToOthers(visibility as boolean);
    if (presenceHeartbeat.presenceId) {
      presenceHeartbeat.restartHeartbeatSchedule();
    }
  };
}

export type PresenceService = PresenceServiceImpl;

/** The interface React consumers type the singleton with (was the sibling .d.ts NgPresenceService). */
export interface NgPresenceService {
  state: PresenceState;
  reconnectPresence: () => void;
  setScenes(scene: SceneInfo): void;
  on(type: string, callback: PresenceEventListener): () => void;
  setVisibleToOthers(visibility?: boolean): void;
}

export const presenceService = new PresenceServiceImpl();
