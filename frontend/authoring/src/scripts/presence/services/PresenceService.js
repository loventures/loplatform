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

import gretchen from '../../grfetchen/';

import { addAnnouncement, disableAnnouncement } from '../../announcement/AnnouncementActions.js';
import { remoteFeedbackUpdate } from '../../feedback/feedbackActions';
import { setProjectGraphRemote } from '../../graphEdit';
import {
  branchReindexing,
  messageReceived,
  setPresenceId,
  setPresenceState,
  setPresentUsers,
  setTabVisible,
} from '../PresenceActions.js';
import PollingEventStream from './PollingEventStream';
import SseEventStream from './SseEventStream';
import { dcmStore } from '../../dcmStore.js';

class PresenceService {
  IDLE_HEARTBEAT = 600;
  ACTIVE_HEARTBEAT = 45;
  EventStream = null;

  globalState = null;
  presenceState = null; // The state in presence reducer
  heartbeatState = 0; // 0 is normal, 1 is immediate resend, 2 is stop
  heartbeatTimeout = null; // The heartbeat timeout promise
  heartbeatRequest = null; // The heartbeat request promise

  registerTask = null; // in-flight session update request

  // Map from identifiers to lists of associated listeners
  listeners = {};
  // Current branch
  branchId = null;
  asset = null;
  assetField = null;
  // Current session info
  session = {
    visible: false, // Is the user visible to other users
  };

  idleTimerRef = null;
  lastActive = null;

  lastBranchId = null;
  lastAsset = null;
  lastAssetField = null;

  init(idleTimerRef) {
    this.store = dcmStore;
    this.globalState = this.store.getState();
    this.presenceState = this.globalState.presence; // The state in presence reducer
    this.configureEventStream();
    //Subscribe to redux store
    this.store.subscribe(this.storeListener);
    this.idleTimerRef = idleTimerRef;
    //Set initial presence state
    this.store.dispatch(
      setPresenceState({ online: false, offline: false, idling: false, started: false })
    );
    this.store.dispatch(setTabVisible(!document.hidden));

    document.addEventListener('visibilitychange', this.onVisibilityChange, false);
  }

  configureEventStream = () => {
    let lastEventId = null;
    if (this.EventStream) {
      lastEventId = this.EventStream.getLastEventId();
      this.EventStream.stop();
    }
    this.EventStream = this.presenceState.tabVisible
      ? new SseEventStream(this.store)
      : new PollingEventStream(this.store);
    if (this.presenceState.presenceId) {
      this.EventStream.start({
        presenceId: this.presenceState.presenceId,
        lastEventId,
        onEvent: this.handleEvent,
        onError: this.onEventApiError,
        handledTypes: Object.keys(this.listeners),
      });
    }
  };

  onVisibilityChange = () => {
    this.store.dispatch(setTabVisible(!document.hidden));
  };

  storeListener = () => {
    const oldState = this.presenceState;
    this.globalState = this.store.getState();
    this.presenceState = this.globalState.presence;
    if (this.presenceState.idling !== oldState.idling) {
      if (this.presenceState.idling) {
        this.onIdleStart();
      } else {
        this.onIdleEnd();
      }
    }
    if (this.presenceState.tabVisible !== oldState.tabVisible) {
      this.configureEventStream();
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
      let timeout = this.presenceState.idling ? this.IDLE_HEARTBEAT : this.ACTIVE_HEARTBEAT;
      this.heartbeatTimeout = setTimeout(this.heartbeat, timeout * 1000, false);
    } else if (this.heartbeatState === 1) {
      this.heartbeatState = 0;
      this.heartbeatTimeout = setTimeout(this.heartbeat, 10, false);
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
    if (!this.presenceState.presenceId) {
      return; // bail if you have no session
    }
    if (!this.presenceState.idling) {
      // if you are active, update the last active timestamp
      const now = new Date().getTime();
      this.session.millisSinceActive = now - this.lastActive;
      this.lastActive = now;
    }

    const branchId = this.branchId;
    const asset = this.asset;
    const assetField = this.assetField;
    const url = '/api/v2/presence/sessions/' + this.presenceState.presenceId;
    return gretchen
      .put(url)
      .data(this.presenceIn())
      .headers({ 'X-No-Session-Extension': true })
      .silent()
      .exec()
      .then(
        () => {
          this.heartbeatRequest = null;
          this.lastBranchId = branchId;
          this.lastAsset = asset;
          this.lastAssetField = assetField;
          this.scheduleHeartbeat();
        },
        response => {
          this.heartbeatRequest = null;
          if (response.status === 404 || response.status === 403) {
            console.log(`Shutting down presence due to heartbeat status: ${response.status}`);
            this.stop();
          } else {
            console.log('Heartbeat error', response.status);
          }
        }
      )
      .catch(error => {
        if (error && error.status === 404) {
          this.stop(error);
        } else {
          console.log(error);
        }
      });
  };

  /**
   * Compute the user's presence state to send to the server.
   */
  presenceIn = () => {
    const millisSinceActive = new Date().getTime() - this.idleTimerRef.getLastActiveTime();

    let followScenes = null;
    if (this.branchId !== this.lastBranchId) followScenes = [{ branch: this.branchId }];

    let inScenes = null;
    if (
      this.branchId !== this.lastBranchId ||
      this.asset !== this.lastAsset ||
      this.assetField !== this.lastAssetField
    ) {
      inScenes = [{ branch: this.branchId }];
      if (this.asset || this.assetField)
        inScenes.push({ branch: this.branchId, asset: this.assetField ?? this.asset });
    }

    return {
      ...this.session,
      millisSinceActive,
      inScenes,
      followScenes,
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
    if (!this.presenceState?.presenceId) {
      return;
    }
    if (!this.heartbeatRequest) {
      // If no request is in flight, cancel the timer and run heartbeat
      clearTimeout(this.heartbeatTimeout);
      // Schedule this asynchronously so the react-idle-timer state has time to update first
      this.heartbeatTimeout = setTimeout(this.heartbeat, 10, false);
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
  on = (id, callback, scope) => {
    let callbacks = this.listeners[id];
    if (callbacks) {
      callbacks.push(callback);
    } else {
      this.listeners[id] = callbacks = [callback];
      this.EventStream.addEventType(id);
    }
    let deregister = () => {
      let index = callbacks.indexOf(callback);
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
  handleEvent = event => {
    // internal
    let data = JSON.parse(event.data);
    let type = event.type;
    for (let listener of this.listeners[type] || []) {
      listener(data, event);
    }
  };

  /**
   * Open a presence session with the server.
   */
  createPresence = () => {
    if (!this.presenceState.started || this.registerTask) {
      // TODO: if no change then skip. requires storing deep copy after success of api call.
      // not started, registration in flight or no change, do nothing
    } else if (this.presenceState.presenceId) {
      let url = '/api/v2/presence/sessions/' + this.presenceState.presenceId;
      this.registerTask = gretchen
        .post(url)
        .data(this.session)
        .exec()
        .then(() => {
          this.registerTask = null;
        })
        .catch(err => {
          console.log(err);
          this.onEventApiError(err);
        });
    } else {
      // connect event source
      const newSession = this.presenceIn();
      const url = '/api/v2/presence/sessions';
      this.registerTask = gretchen
        .post(url)
        .data(newSession)
        .headers({ 'Cache-Control': 'max-age=0' })
        .exec()
        .then(o => {
          if (!o) throw Error('Create presence timeout out'); // grrrrretchen
          return o;
        })
        .then(({ presenceId }) => {
          this.registerTask = null;
          this.presenceState.presenceId = presenceId;
          this.lastBranchId = this.branchId;
          this.lastAsset = this.asset;
          this.lastAssetField = this.assetField;
          this.EventStream.start({
            presenceId: this.presenceState.presenceId,
            onEvent: this.handleEvent,
            onError: this.onEventApiError,
            handledTypes: Object.keys(this.listeners),
          });
          // Register the channel listeners
          this.on('Control', this.controlChannelListener);
          this.on('ScenePresence', this.scenePresenceListener);
          this.on('ChatMessage', this.chatMessageListener);
          this.on('ReindexComplete', this.reindexListener);
          this.on('BranchFeedback', this.feedbackListener);
          this.on('BranchHeadChanged', this.headListener);
          this.on('BranchFeedback', this.feedbackListener);
          /* Shut down automatically when we're logged out */
          this.on('Logout', this.stop);
          this.presenceState.online = true;
          this.presenceState.offline = false;
          this.store.dispatch(setPresenceState(this.presenceState));
          this.store.dispatch(setPresenceId(this.presenceState.presenceId));
        })
        .catch(err => {
          console.log(err);
          this.onEventApiError(err);
        });
    }
  };

  /**
   * Handle when a session API error occurs.
   */
  onEventApiError = error => {
    // internal
    console.log('Events API error', error);
    this.registerTask = null;
    this.stop(error);
  };

  /**
   * Synchronously delete the browser session on the server.
   */
  deleteSessionSynchronously = () => {
    const id = this.presenceState.presenceId;
    if (id) {
      this.presenceState.presenceId = null;
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
  start = o => {
    if (!this.presenceState.started) {
      this.presenceState.started = true;
      this.presenceState.online = false;
      this.store.dispatch(setPresenceState(this.presenceState));
      this.branchId = o.branchId;
      this.asset = null;
      this.assetField = null;
      this.lastBranchId = null;
      this.lastAsset = null;
      this.lastAssetField = null;
      this.session.visible = o.visible;
      this.lastActive = new Date().getTime();
      this.createPresence();
      // Watch for browser activity to inform the server of your presence
      this.watchActivity();
    }
  };

  onAsset = asset => {
    if (asset !== this.asset) {
      this.asset = asset;
      this.heartbeatNow();
    }
  };

  onAssetField = (assetField, active) => {
    if ((assetField !== this.assetField) === !!active) {
      this.assetField = active ? assetField : null;
      this.heartbeatNow();
    }
  };

  /**
   * Stop the server event framework.
   */
  stop = error => {
    if (this.presenceState?.started) {
      const presenceId = this.presenceState.presenceId;
      this.presenceState.started = false;
      this.presenceState.online = false;
      this.presenceState.offline = !!error;
      this.presenceState.presenceId = null;
      this.store.dispatch(setPresenceState(this.presenceState));
      this.store.dispatch(setPresenceId(null));
      this.store.dispatch(setPresentUsers([]));
      this.unwatchActivity();
      this.EventStream.stop(error);
      if (presenceId && (!error || error.status !== 404)) {
        // 404 errors means the presence session was unknown
        gretchen
          .delete(`/api/v2/presence/sessions/${presenceId}`)
          .exec()
          .catch(error => console.log(error));
      }
    }
  };

  /**
   * A function that handles events on the control channel.
   */
  controlChannelListener = (data, event) => {
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
      this.store.dispatch(addAnnouncement(announcement));
    } else if (data.type === 'AnnouncementEnd') {
      this.store.dispatch(disableAnnouncement(data.id));
    } else {
      console.log('Unknown control channel event', data, event);
    }
  };

  reindexListener = data => {
    this.store.dispatch(branchReindexing(data.branch, false));
  };

  feedbackListener = data => {
    this.store.dispatch(remoteFeedbackUpdate(data.action, data.user, data.feedback));
  };

  headListener = data => {
    this.store.dispatch(setProjectGraphRemote(data.commit));
  };

  feedbackListener = data => {
    this.store.dispatch(remoteFeedbackUpdate(data.action, data.user, data.feedback));
  };

  scenePresenceListener = ({ users }) => {
    // scene: { branch: 1234 }
    // users: [ [ "handle", idleTime, "location" ], ... ]
    const lastActive = idle => new Date().getTime() - idle;
    const usersLastActive = users.map(([handle, idle, location]) => [
      handle,
      lastActive(idle),
      location,
    ]);
    this.store.dispatch(setPresentUsers(usersLastActive));
  };

  chatMessageListener = ({ id, sender, room, timestamp, message, typing }) => {
    this.store.dispatch(messageReceived(id, sender, room, timestamp, message, typing));
  };

  /**
   * Get the presence state.
   */
  getState = () => this.presenceState;

  /**
   * Get the presence session.
   */
  getSession = () => this.session;
}

export default new PresenceService();
