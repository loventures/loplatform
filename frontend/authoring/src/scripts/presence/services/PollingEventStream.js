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

class PollingEventStream {
  presenceId = null;
  lastEventId = null;
  currentPoll = null;
  pollCount = 0;
  store = null;

  onError = () => {};
  onEvent = () => {};

  constructor(store) {
    this.store = store;
    return {
      start: this.start,
      stop: this.stop,
      addEventType: () => {},
      getLastEventId: () => this.lastEventId,
    };
  }

  /**
   * Connect an polling event source to the server.
   * Parameters:
   * @param{string} presenceId - the server-side configured presence to listen to
   * @param{func} onEvent - the callback for when an event is received
   * @param{func} onError - the callback for when an error is encountered
   */
  start = ({ presenceId, lastEventId, onError, onEvent }) => {
    this.presenceId = presenceId;
    this.lastEventId = lastEventId;
    this.onEvent = onEvent;
    this.onError = onError;
    this.scheduleNext();
  };

  /**
   * Stop this event source.
   * @param{Object} error - the error that caused the termination
   * @param{number} error.status - the http status code
   */
  stop = () => {
    if (this.currentPoll) {
      clearTimeout(this.currentPoll);
    }
  };

  pollThenScheduleNext = () => {
    this.doPoll().then(this.scheduleNext);
  };

  scheduleNext = () => {
    const idling = this.store.getState().presence.idling;
    this.pollCount = idling ? 1 + this.pollCount : 0;
    const pollInterval = idling ? Math.min(150, 30 + this.pollCount) : 5;
    this.currentPoll = setTimeout(this.pollThenScheduleNext, pollInterval * 1000);
  };

  doPoll = () => {
    const url = `/api/v2/presence/sessions/${this.presenceId}/poll`;
    return gretchen
      .get(url)
      .headers({
        'X-No-Session-Extension': true,
        'Last-Event-ID': this.lastEventId,
      })
      .exec()
      .then(events => {
        for (let event of events) {
          if (event.id) {
            this.lastEventId = parseInt(event.id, 10);
          }
          this.onEvent(event);
        }
      })
      .catch(error => {
        if (error && error.status === 404) {
          this.onError(error);
          throw error;
        } else {
          // keep on going, assuming a transient error
          console.log(error);
        }
      });
  };
}

export default PollingEventStream;
