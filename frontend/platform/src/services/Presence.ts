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

import axios from 'axios';
import moment from 'moment-timezone';

import { addAnnouncement, disableAnnouncement } from '../redux/actions/AnnouncementActions.js';
import { setPresenceId, setPresenceState } from '../redux/actions/PresenceActions.js';
import { PresenceState } from '../redux/reducers/PresenceReducer';
import { RootState, store } from '../store.js';
import { NoSessionExtensionHdr } from './Headers';
import { SseEventStream } from './SseEventStream';

type PresenceEventCallback = (data: any, event: any) => void;

class PresenceService {
  IDLE_HEARTBEAT = 600;
  ACTIVE_HEARTBEAT = 45;
  EventStream = (() => {
    return new SseEventStream();
  })();

  globalState: RootState = store.getState();
  state: PresenceState = this.globalState.presence; // The state in presence reducer
  heartbeatState = 0; // 0 is normal, 1 is immediate resend, 2 is stop
  heartbeatTimeout: any = null; // The heartbeat timeout promise
  heartbeatRequest: any = null; // The heartbeat request promise

  registerTask: any = null; // in-flight session update request

  inScenes: string[] = []; // currently in these scenes
  inSceneChange = false; // have scenes changed
  followScenes: string[] = []; // currently following these scenes
  followSceneChange = false; // have follow scenes changed

  idleTimerRef: any; // the idle timer reference
  lastActive: any; // last active moment

  // Map from identifiers to lists of associated listeners
  listeners: Record<string, PresenceEventCallback[]> = {};
  // Current session info
  session: any = {
    visible: false, // Is the user visible to other users
  };

  constructor(idleTimerRef: any) {
    //Subscribe to redux store
    store.subscribe(this.listener);
    this.idleTimerRef = idleTimerRef;
    //Set initial presence state
    store.dispatch(
      setPresenceState({ online: false, offline: false, idling: false, started: false })
    );
  }

  listener = () => {
    const oldState = this.state;
    this.globalState = store.getState();
    this.state = this.globalState.presence;
    if (this.state.idling !== oldState.idling) {
      if (this.state.idling) {
        this.onIdleStart();
      } else {
        this.onIdleEnd();
      }
    }
  };

  /**
   * Watch browser events to keep the server informed of the user's actual presence.
   */
  watchActivity = () => {
    // Actively notify the server of the user leaving this page.
    window.addEventListener('beforeunload', this.deleteSessionSynchronously);

    // Periodically send an activity heartbeat to the server
    this.startHeartbeat(); // TODO: this should be scheduled for once you have a session
  };

  /**
   * Unwatch browser events.
   */
  unwatchActivity = () => {
    this.stopHeartbeat();
    window.removeEventListener('beforeunload', this.deleteSessionSynchronously);
  };

  /**
   * Schedule a heartbeat to be sent to the server. If an immediate resend has
   * been flagged then send now, otherwise set a timer depending on whether you
   * are active or idle.
   */
  scheduleHeartbeat = () => {
    if (this.heartbeatState === 0) {
      const timeout = this.state.idling ? this.IDLE_HEARTBEAT : this.ACTIVE_HEARTBEAT;
      this.heartbeatTimeout = setTimeout(this.heartbeat, timeout * 1000, false);
    } else if (this.heartbeatState === 1) {
      this.heartbeatState = 0;
      this.heartbeat();
    }
  };

  /**
   * Start a heartbeat that informs the server of your activity.
   */
  startHeartbeat = () => {
    this.heartbeatState = 0;
    this.scheduleHeartbeat();
  };

  /**
   * Stop the heartbeat.
   */
  stopHeartbeat = () => {
    this.heartbeatState = 2;
    clearTimeout(this.heartbeatTimeout);
  };

  /**
   * Execute the heartbeat.
   */
  heartbeat = () => {
    if (!this.state.presenceId) {
      return; // bail if you have no session
    }
    if (!this.state.idling) {
      // if you are active, update the last active timestamp
      const now = moment();
      this.session.millisSinceActive = now.diff(this.lastActive);
      this.lastActive = now;
    }

    const url = '/api/v2/presence/sessions/' + this.state.presenceId;
    return axios
      .put(url, this.presenceIn(), {
        headers: { [NoSessionExtensionHdr]: 'true' },
        hideProgress: true,
      } as any)
      .then(
        () => {
          this.heartbeatRequest = null;
          this.inSceneChange = false;
          this.followSceneChange = false;
          this.scheduleHeartbeat();
        },
        (response: any) => {
          this.heartbeatRequest = null;
          if (response.status === 404 || response.status === 403) {
            console.log(`Shutting down presence due to heartbeat status: ${response.status}`);
            this.stop();
          } else {
            console.log('Heartbeat error', response.status);
          }
        }
      );
  };

  /**
   * Compute the user's presence state to send to the server.
   */
  presenceIn = () => {
    const inUpdate = this.inSceneChange ? this.inScenes : null,
      followUpdate = this.followSceneChange ? this.followScenes : null;
    return {
      ...this.session,
      millisSinceActive: moment().diff(moment(this.idleTimerRef.getLastActiveTime())),
      inScenes: inUpdate,
      followScenes: followUpdate,
      lastEventId: this.EventStream.getLastEventId(),
    };
  };

  /**
   * If the user goes idle, reschedule the heartbeat to the slower idle timeout.
   */
  onIdleStart = () => {
    if (!this.heartbeatRequest) {
      clearTimeout(this.heartbeatTimeout);
      this.scheduleHeartbeat();
    }
  };

  /**
   * If the user becomes active, immediately notify the server.
   */
  onIdleEnd = () => {
    this.heartbeatNow();
  };

  heartbeatNow = () => {
    if (!this.state.presenceId) {
      return;
    }
    if (!this.heartbeatRequest) {
      // If no request is in flight, cancel the timer and run heartbeat
      clearTimeout(this.heartbeatTimeout);
      setTimeout(this.heartbeat, 0);
    } else {
      // If a request in in flight, schedule an immediate resend when it completes
      this.heartbeatState = 1;
    }
  };

  /**
   * Add a listener for server events.
   * @param{string} id - the event id
   * @param{presenceEventCB} callback - the listener.
   * @param{scope} [scope] - if specified, the listener will deregister when the scope is destroyed
   * @return a deregistration function
   */
  on = (id: string, callback: PresenceEventCallback, scope?: any) => {
    let callbacks = this.listeners[id];
    if (callbacks) {
      callbacks.push(callback);
    } else {
      this.listeners[id] = callbacks = [callback];

      this.EventStream.addEventType(id);
    }
    const deregister = () => {
      const index = callbacks.indexOf(callback);
      if (index >= 0) {
        callbacks.splice(index, 1);
      }
    };
    if (scope) {
      scope.addEventListener('$destroy', deregister);
    }
    return deregister;
  };

  /**
   * The listener callback used for presence events.
   * @callback presenceEventCB
   * @param data - A JSON blob containing event-specific data
   * @param{string} event - the event type (will be the same as the event type
   * specified when the listener was added.
   */

  /**
   * Handle an SSE event. Forwards it on to registered listeners for the event type.
   */
  handleEvent = (event: any) => {
    // internal
    const data = JSON.parse(event.data);
    const type = event.type;
    for (const listener of this.listeners[type] || []) {
      listener(data, event);
    }
  };

  thereCanBeOnlyOne = () => {
    const username = this.globalState.main.lo_platform.user.userName;
    window.localStorage.setItem(`sse-${username}`, this.state.presenceId as string);
    window.addEventListener('storage', evt => {
      const username = store.getState().main.lo_platform.user.userName;
      if (evt.key === `sse-${username}`) {
        this.stop();
      }
    });
  };

  /**
   * Open a presence session with the server.
   */
  createPresence = () => {
    if (!this.state.started || this.registerTask) {
      // TODO: if no change then skip. requires storing deep copy after success of api call.
      // not started, registration in flight or no change, do nothing
    } else if (this.state.presenceId) {
      const url = '/api/v2/presence/sessions/' + this.state.presenceId;
      this.registerTask = axios
        .post(url, this.session, { hideProgress: true } as any)
        .then(() => {
          this.registerTask = null;
        })
        .catch((err: any) => {
          console.log(err);
          this.onEventApiError(err);
        });
    } else {
      // connect event source
      const newSession = this.presenceIn(),
        url = '/api/v2/presence/sessions';
      this.registerTask = axios
        .post(url, newSession, {
          headers: { 'Cache-Control': 'max-age=0' },
          hideProgress: true,
        } as any)
        .then((response: any) => {
          if (response.status === 204) throw Error('Create presence timed out');
          this.registerTask = null;
          this.state.presenceId = response.data.presenceId;
          this.EventStream.start({
            presenceId: this.state.presenceId as string,
            onEvent: this.handleEvent,
            onError: this.onEventApiError,
            handledTypes: Object.keys(this.listeners),
          });
          this.thereCanBeOnlyOne();
          // Register the control channel listener
          this.on('Control', this.controlChannelListener);
          /* Shut down automatically when we're logged out */
          this.on('Logout', this.stop);
          this.state.online = true;
          this.state.offline = false;
          store.dispatch(setPresenceState(this.state));
          store.dispatch(setPresenceId(this.state.presenceId));
        })
        .catch((err: any) => {
          console.log(err);
          this.onEventApiError(err);
        });
    }
  };

  /**
   * Handle when a session API error occurs.
   */
  onEventApiError = (error?: any) => {
    // internal
    console.log('Events API error', error);
    this.registerTask = null;
    this.stop(error);
  };

  /**
   * Synchronously delete the browser session on the server.
   */
  deleteSessionSynchronously = () => {
    const id = this.state.presenceId;
    if (id) {
      this.state.presenceId = null;
      if (
        !navigator.sendBeacon ||
        !navigator.sendBeacon(`/api/v2/presence/sessions/${id}/delete`)
      ) {
        // In order to go offline in onbeforeunload the XHR must by *synchronous*
        // which is not currently possible with axios so just good old XMLHttpRequest
        const xhr = new XMLHttpRequest();
        xhr.open('DELETE', `/api/v2/presence/sessions/${id}`, false); // DO NOT DELETE `false`
        xhr.setRequestHeader('X-CSRF', 'true');
        xhr.onload = () => {
          if (xhr.readyState !== 4 || xhr.status !== 204) {
            console.error('delete session error', xhr.statusText);
          }
        };
        xhr.send();
      }
    }
  };

  /**
   * Start the server event framework.
   */
  start = () => {
    if (!this.state.started) {
      this.state.started = true;
      this.state.online = false;
      store.dispatch(setPresenceState(this.state));
      this.session.visible = true;
      this.lastActive = moment();
      this.createPresence();
      // Watch for browser activity to inform the server of your presence
      this.watchActivity();
    }
  };

  /**
   * Stop the server event framework.
   */
  stop = (error?: any) => {
    if (this.state.started) {
      this.state.started = false;
      this.state.online = false;
      this.state.offline = !!error;
      this.state.presenceId = null;
      store.dispatch(setPresenceState(this.state));
      store.dispatch(setPresenceId(this.state.presenceId));
      this.unwatchActivity();
      this.EventStream.stop();
    }
  };

  /**
   * A function that handles events on the control channel.
   */
  controlChannelListener = (data: any, event: any) => {
    if (data.type === 'Start' || data.type === 'Heartbeat') {
      // nothing to see here
    } else if (data.type === 'SessionEnded') {
      // the presence session was closed
      this.stop();
    } else if (data.type === 'Announcement') {
      //Add new announcement to my announcements
      const announcement = {
        id: data.id,
        startTime: data.startTime,
        endTime: data.endTime,
        message: data.message,
        style: data.style,
      };
      store.dispatch(addAnnouncement(announcement));
    } else if (data.type === 'AnnouncementEnd') {
      store.dispatch(disableAnnouncement(data.id));
    } else {
      console.log('Unknown control channel event', data, event);
    }
  };

  /**
   * Register to follow a scene.
   */
  followScene = (scene: string) => {
    if (this.followScenes.indexOf(scene) < 0) {
      this.followScenes.push(scene);
      this.followSceneChange = true;
      this.heartbeatNow();
    }
  };

  /**
   * Get the presence state.
   */
  getState = () => this.state;

  /**
   * Get the presence session.
   */
  getSession = () => this.session;
}

export { PresenceService };
