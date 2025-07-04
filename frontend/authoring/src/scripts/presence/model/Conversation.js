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

import { last, reverse } from 'lodash';

/** The maximum age after which a new line, even from the same user, will create a new stanza. */
const AgeDeltaForNewStanza = 120000;
/** Show a timestamps if there is at least a 2 minute delta from the next stanza. */
export const ShowTimestampDelta = 120000;

const MaxStanzas = 50;

export const newConversation = (room, chatOpen) => ({
  room, // number - the room identifier
  chatOpen, // boolean - whether the chat is open
  stanzas: [], // array<stanza> - the stanzas
  unreadCount: 0, // number - count of unread messages
  historyLoaded: false, // boolean - whether history is loaded
  msgIds: {}, // map<number, number> - seen message ids
  typingUsers: {}, // map<number, date> - users actively typing
  localHistory: [], // array<event> - local message history to replay after loading remote history
});

/**
 * @description Update a conversation to indicate that it is now open.
 * @param conversation {object} the current conversation
 * @return the updated conversation
 */
export const openConversation = conversation => ({
  ...conversation,
  chatOpen: true,
  unreadCount: 0,
});

/**
 * @description Update a conversation to indicate that it is now closed.
 * @param conversation {object} the current conversation
 * @return the updated conversation
 */
export const closeConversation = conversation => ({
  ...conversation,
  chatOpen: false,
});

/**
 * @description Add a new message to this conversation.
 *
 * @params {object} the conversation
 * @params {number} id the message id
 * @params {string} sender the user who sent the message
 * @params {string} timestamp the message timestamp
 * @params {string} message the line to add
 * @params {boolean} typing whether the user is actively typing (the line should be blank)
 */
export const addMessage = (conversation, id, sender, timestamp, message, typing) => {
  const {
    chatOpen,
    historyLoaded,
    localHistory: oldLocalHistory,
    msgIds: oldMsgIds,
    stanzas: oldStanzas,
    typingUsers: oldTypingUsers,
    unreadCount: oldUnreadCount,
  } = conversation;
  if (id && oldMsgIds[id]) {
    return conversation;
  } else {
    const epoch = new Date(timestamp).getTime();
    // Record this message id, if set, as seen
    const msgIds = id ? { ...oldMsgIds, [id]: id } : oldMsgIds;
    // Remove sender from the list of active typists
    const { [sender]: _, ...typingUsers } = oldTypingUsers; // eslint-disable-line

    // If message is null then this is just a typist change
    if (!message) {
      return {
        ...conversation,
        msgIds,
        typingUsers: typing ? { ...typingUsers, [sender]: epoch } : typingUsers,
      };
    }

    const lastStanza = last(oldStanzas);
    let stanzas;

    // Append message to stanzas in either a new stanza or the current last stanza if appropriate
    if (
      !lastStanza ||
      lastStanza.sender !== sender ||
      epoch - lastStanza.finish >= AgeDeltaForNewStanza
    ) {
      // update previous last stanza if appropriate and add a new one
      const lastStanzaʹ =
        lastStanza && epoch - lastStanza.finish >= ShowTimestampDelta
          ? { ...lastStanza, showTimestampAfter: true }
          : lastStanza;
      stanzas = [
        ...oldStanzas.slice(oldStanzas.length >= MaxStanzas ? 1 : 0, -1),
        ...(lastStanzaʹ ? [lastStanzaʹ] : []),
        {
          sender,
          start: epoch,
          finish: epoch,
          lines: [message],
        },
      ];
    } else {
      // append to existing last stanza
      stanzas = [
        ...oldStanzas.slice(0, -1),
        {
          ...lastStanza,
          sender,
          finish: epoch,
          lines: [...lastStanza.lines, message],
        },
      ];
    }

    // Update unread count if chat is not open
    const unreadCount = chatOpen ? 0 : 1 + oldUnreadCount;

    // Record some SSE messages in a local history to replay if we load chat history from the server
    const localHistory = historyLoaded
      ? []
      : [...oldLocalHistory.slice(-9), { id, sender, timestamp, message }];

    return {
      ...conversation,
      msgIds,
      typingUsers,
      unreadCount,
      stanzas,
      localHistory,
    };
  }
};

/**
 * @description Update a conversation with chat history loaded from the server.
 * @param conversation the conversation
 * @param messages messages from the server
 * @return An updated conversation
 */
export const setHistory = (conversation, messages) => {
  const { localHistory, typingUsers, unreadCount } = conversation;
  // First remove all messages because we will replay them from history
  const conversationʹ = {
    ...conversation,
    msgIds: {},
    stanzas: [],
  };
  // Next replay the history
  const conversationʹʹ = reverse(messages)
    .concat(localHistory)
    .reduce(
      (c, { id, sender, timestamp, message }) =>
        addMessage(c, id, sender, timestamp, message, false),
      conversationʹ
    );
  // Finally restore the unread count and typists to before history loaded
  return {
    ...conversationʹʹ,
    historyLoaded: true,
    typingUsers,
    unreadCount,
  };
};
