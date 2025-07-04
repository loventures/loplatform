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

class SseEventStream {
  presenceId = null;
  lastEventId = null;
  eventSource = null; // The connected event source

  onEvent = () => {};
  onError = () => {};

  constructor() {
    return {
      start: this.start,
      stop: this.stop,
      addEventType: this.addEventType,
      getLastEventId: () => this.lastEventId,
    };
  }
  /**
   * Add an event type to the list of handled event types.
   * Event types that are not specified in {@link start} or by calling
   * this method will not be delivered.
   * @param{string} type - the event type to listen for
   */
  addEventType = type => {
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
  start = ({ handledTypes, presenceId, lastEventId, onError, onEvent }) => {
    this.presenceId = presenceId;
    this.lastEventId = lastEventId;
    this.onEvent = onEvent;
    this.onError = onError;

    if (!this.eventSource || this.eventSource.readyState === 2) {
      // TODO: why?
      if (this.eventSource) {
        this.eventSource.close();
      }
      let url = `/api/v2/presence/sessions/${this.presenceId}/events`;
      this.eventSource = new window.EventSource(url);
      handledTypes.forEach(s => {
        // should I just use onmessage?
        this.eventSource.addEventListener(s, this.handleEvent, false);
      });
      this.eventSource.onerror = this.onEventSourceError;
    }
  };

  handleEvent = event => {
    // internal
    if (event.lastEventId) {
      this.lastEventId = parseInt(event.lastEventId, 10);
    }
    this.onEvent(event);
  };

  /**
   * Handle when an event source error occurs.
   */
  onEventSourceError = e => {
    // internal
    console.log('Event source error', e);
    if (this.eventSource.readyState === 2) {
      console.log('Shutting down presence due to event source error');
    }
    this.onError(e);
  };

  /**
   * Close the event source and presence session.
   * @param{Object} [error] - the error that is causing an abnormal termination
   */
  stop = () => {
    // internal
    if (this.eventSource) {
      this.eventSource.close();
      this.eventSource = null;
    }
  };
}

export default SseEventStream;
