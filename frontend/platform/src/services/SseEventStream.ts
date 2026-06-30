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

import { setPresenceId } from '../redux/actions/PresenceActions.js';
import { PresenceState } from '../redux/reducers/PresenceReducer';
import { RootState, store } from '../store.js';

interface SseStartOptions {
  presenceId: string;
  onEvent: (event: any) => void;
  onError: (error: any) => void;
  handledTypes: string[];
}

class SseEventStream {
  onEvent: ((event: any) => void) | null = null;
  onError: ((error: any) => void) | null = null;
  lastEventId: number | null = null;
  registerTask: any = null; // A registration request in flight to the server
  eventSource: EventSource | null = null; // The connected event source
  globalState: RootState = store.getState();
  presenceState: PresenceState = this.globalState.presence;

  constructor() {
    //Subscribe to redux store
    store.subscribe(this.listener);
  }

  getLastEventId = (): number | null => this.lastEventId;

  listener = (): void => {
    this.globalState = store.getState();
    this.presenceState = this.globalState.presence;
  };

  /**
   * Add an event type to the list of handled event types.
   * Event types that are not specified in {@link start} or by calling
   * this method will not be delivered.
   * @param{string} type - the event type to listen for
   */
  addEventType = (type: string): void => {
    /* For SSE we need to explicitly tell it to listen for this type of event */
    if (this.eventSource) {
      this.eventSource.addEventListener(type, this.handleEvent, false);
    }
  };

  /**
   * Connect an polling event source to the server.
   * Parameters:
   * @param{Object} o - initial configuration options for the event source
   * @param{string} o.presenceId - the server-side configured presence to listen to
   * @param{function} o.onEvent - the callback for when an event is received
   * @param{function} o.onError - the callback for when an error is encountered
   * @param{string} [o.handledTypes] - existing event types which the callback knows how to handle
   */
  start = (o: SseStartOptions): void => {
    this.presenceState.presenceId = o.presenceId;
    store.dispatch(setPresenceId(this.presenceState.presenceId));
    this.onEvent = o.onEvent;
    this.onError = o.onError;

    if (!this.eventSource || this.eventSource.readyState === 2) {
      // if a registration is in-flight, wait for it to succeed and call this method
      if (this.registerTask || this.globalState.main.lo_platform.mock) {
        return;
      }
      if (this.eventSource) {
        this.eventSource.close();
      }
      const url = '/api/v2/presence/sessions/' + this.presenceState.presenceId + '/events';
      const eventSource = new window.EventSource(url);
      this.eventSource = eventSource;
      o.handledTypes.forEach(s => {
        // should I just use onmessage?
        eventSource.addEventListener(s, this.handleEvent, false);
      });
      eventSource.onerror = this.onEventSourceError;
    }
  };

  handleEvent = (event: any): void => {
    // internal
    if (event.lastEventId) {
      this.lastEventId = parseInt(event.lastEventId, 10);
    }
    this.onEvent!(event);
  };

  /**
   * Handle when an event source error occurs.
   */
  onEventSourceError = (e: any): void => {
    // internal
    console.log('Event source error', e);
    if (this.eventSource!.readyState === 2) {
      console.log('Shutting down presence due to event source error');
    }
    this.onError!(e);
  };

  /**
   * Close the event source and presence session.
   * @param{Object} [error] - the error that is causing an abnormal termination
   */
  stop = (): void => {
    // internal
    if (this.eventSource) {
      this.eventSource.close();
      this.eventSource = null;
    }
    if (this.presenceState.presenceId) {
      axios.delete('/api/v2/sessions/' + this.presenceState.presenceId, {
        hideProgress: true,
      } as any);
      this.presenceState.presenceId = null;
      store.dispatch(setPresenceId(this.presenceState.presenceId));
    }
  };
}
export { SseEventStream };
