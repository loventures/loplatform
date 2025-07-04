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

import { uniq, map } from 'lodash';

export default angular
  .module('lo.presence.Conversation', [])
  .factory('Conversation', function ConversationService() {
    /** The maximum age after which a new line, even from the same user, will create a new stanza. */
    const AgeDeltaForNewStanza = 120000;

    const MaxStanzas = 64;

    let globalId = 0;

    /**
     * Representation of a sequence of lines of chat from a particular
     * user along with an indication of whether they are actively typing.
     * If the user is actively typing, the last line will be blank.
     *
     * Properties:
     * user - the user who wrote the lies
     * lines - the conversation lines
     * typing - whether the user is typing
     */
    class Stanza {
      /**
       * @description Create a new stanza for a given user.
       * @params {string} user the user who created this stanza
       */
      constructor(user) {
        this.user = user;
        this.start = Number.MAX_VALUE;
        this.finish = 0;
        this.lines = [];
        this.timestamps = [];
        this.id = globalId++;
      }

      /**
       * @description Add a line to this stanza.
       * @params {string} line the line to add
       * @params {object} timestamp the line timestamp
       */
      addLine(line, timestamp) {
        const find = this.timestamps.findIndex(t => t > timestamp);
        const index = find < 0 ? this.timestamps.length : find;
        this.lines.splice(index, 0, line);
        this.timestamps.splice(index, 0, timestamp);
        this.start = Math.min(this.start, timestamp);
        this.finish = Math.max(this.finish, timestamp);
      }
    }

    /**
     * Representation of a conversation.
     *
     * Properties:
     * stanzas - the stanzas of the conversation
     * unreadCount - the number of unread lines of conversation
     * seenCount - the number of unread lines of conversation that the user has been notified of
     * chatOpen - whether this a chat open on this conversation
     */
    class Conversation {
      /**
       * @description Create a new conversation.
       */
      constructor(roomId) {
        this.roomId = roomId;
        this.stanzas = [];
        this.unreadCount = 0;
        this.changeCount = 0;
        this.seenCount = 0; // the count of unread messages 'seen'
        this.chatOpen = false;
        this.historyLoaded = false;
        this.msgIds = {};
        this.typingUsers = {};
      }

      hasNew() {
        return this.unreadCount > this.seenCount;
      }

      getUniqueUserHandles() {
        return uniq(map(this.stanzas, 'user'));
      }

      duplicateCheck(id) {
        if (this.msgIds[id]) {
          return true;
        }
        this.msgIds[id] = id;
      }

      /**
         * @description Add a new message to this conversation.
         *
         @ @params {number} id the message id
         * @params {string} user the user who sent the message
         * @params {object} timestamp the message timestamp
         * @params {string} line the line to add
         * @params {boolean} typing whether the user is actively typing (the line should be blank)
         */
      addLine(id, user, timestamp, line, typing) {
        if (typing) {
          this.typingUsers[user] = timestamp;
          //the line should be blank
          return;
        }

        if (id && this.duplicateCheck(id)) {
          return;
        }

        if (this.typingUsers[user]) {
          delete this.typingUsers[user];
        }

        if (!line) {
          return;
        }

        if (!this.lastStanza || timestamp >= this.lastStanza.finish) {
          if (
            !this.lastStanza ||
            this.lastStanza.user !== user ||
            timestamp - this.lastStanza.finish >= AgeDeltaForNewStanza
          ) {
            this.lastStanza = new Stanza(user);
            this.stanzas.push(this.lastStanza);
          }
          this.lastStanza.addLine(line, timestamp);
        } else {
          // find the stanza this message should go within or before
          const index = this.stanzas.findIndex(s => s.start > timestamp || s.finish > timestamp);
          const stanza = this.stanzas[index];
          const previous = index > 0 && this.stanzas[index - 1];
          if (stanza.user === user) {
            // insert into the stanza
            stanza.addLine(line, timestamp);
          } else if (stanza.start < timestamp) {
            // split the stanza
            const split = stanza.timestamps.findIndex(t => t > timestamp);
            const prev = new Stanza(stanza.user);
            for (let i = 0; i < split; ++i) prev.addLine(stanza.lines[i], stanza.timestamps[i]);
            const curr = new Stanza(user);
            curr.addLine(line, timestamp);
            const next = new Stanza(stanza.user);
            for (let i = split; i < stanza.lines.length; ++i)
              next.addLine(stanza.lines[i], stanza.timestamps[i]);
            this.stanzas.splice(index, 1, prev, curr, next);
          } else if (
            previous &&
            previous.user === user &&
            timestamp - previous.finish < AgeDeltaForNewStanza
          ) {
            // append to the previous stanza
            previous.addLine(line, timestamp);
          } else {
            // insert a stanza before
            const newStanza = new Stanza(user);
            newStanza.addLine(line, timestamp);
            this.stanzas.splice(index, 0, newStanza);
          }
        }

        // truncate the chat history
        if (this.stanzas.length > MaxStanzas) {
          this.stanzas.shift();
        }

        // increment the unread count if no chat is open
        if (!this.chatOpen) {
          this.unreadCount += 1;
        }

        this.changeCount += 1;
      }
    }

    return Conversation;
  });
