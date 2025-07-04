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

import { CourseState } from '../../loRedux';
import dayjs from 'dayjs';
import { map } from 'lodash';
import NgPresentConversations from '../../presence/PresentConversations';
import React, { useEffect, useRef, useState } from 'react';
import { connect } from 'react-redux';

import ChatLine from './ChatLine';
import { lojector } from '../../loject';

/** Show a timestamps if there is at least a 2 minute delta from the next stanza. */
const ShowTimestampDelta = 120000;

const showTimestampBefore = (conversation: any, index: number) => {
  // show timestamp before a stanza if the first and it finished >= 2m ago
  // else if it started >= 2m after the previous finished
  const stanza = conversation.stanzas[index];
  const delta =
    index == 0
      ? dayjs().diff(stanza.finish)
      : stanza.start - conversation.stanzas[index - 1].finish;
  return delta >= ShowTimestampDelta;
};

const formatTimeAgo = (timestamp: any) => {
  const now = dayjs();
  const time = dayjs(timestamp);
  const language = window.lo_platform.i18n.language || 'en';

  if (!time.isSame(now, 'day')) {
    return time.locale(language).format('LLLL');
  } else if (now.diff(time) >= 3600000) {
    // more than an hour
    return time.locale(language).format('LT');
  } else {
    return time.fromNow();
  }
};

export type ChatOutputStateProps = {
  lastUpdated: number;
};

export type ChatOwnProps = {
  roomId: number;
  showFullName: boolean;
};

export type ChatOutputProps = ChatOutputStateProps & ChatOwnProps;

const ChatOutput = ({ lastUpdated, roomId, showFullName }: any) => {
  const PresentConversations: NgPresentConversations = lojector.get('PresentConversations');

  const bottomRef = useRef<HTMLDivElement>(null);
  const [, setUpdateTime] = useState(lastUpdated); // todo: remove?
  const [conversation, setConversation] = useState<any>(
    PresentConversations.getConversationByRoom(roomId)
  );

  useEffect(() => {
    const conversation = PresentConversations.getConversationByRoom(roomId);
    PresentConversations.ensureHistoryForChatRoom(roomId);
    setConversation(conversation);
  }, [roomId]);

  useEffect(() => {
    const updateInterval = window.setInterval(() => {
      setUpdateTime(Date.now());
    }, 1000 * 60);

    const scrollTimeout = window.setTimeout(() => {
      if (bottomRef.current) {
        bottomRef.current.scrollIntoView({ behavior: 'smooth' });
      }
    }, 100);

    return () => {
      clearInterval(updateInterval);
      clearTimeout(scrollTimeout);
    };
  }, [lastUpdated]);

  return (
    <div className="chat-output">
      {map(conversation.stanzas, (stanza, stanzaIndex: number) => (
        <div
          key={`stanza-${stanzaIndex}`}
          className="chat-stanza"
        >
          {showTimestampBefore(conversation, stanzaIndex) && (
            <div className="timestamp">{formatTimeAgo(stanza.start)}</div>
          )}

          {map(stanza.lines, (line, lineIndex: number) => (
            <ChatLine
              key={`stanza-${stanzaIndex}-line-${lineIndex}`}
              userHandle={lineIndex === 0 ? stanza.user : null}
              displayFullName={showFullName}
              line={line}
            />
          ))}
        </div>
      ))}
      {map(conversation.typingUsers, (_, user) => (
        <div
          key={`typeUsers-${user}`}
          className="chat-stanza"
        >
          <ChatLine
            userHandle={user}
            displayFullName={showFullName}
          />
        </div>
      ))}
      <div
        className="chat-output-bottom"
        role="presentation"
        style={{ float: 'left', clear: 'both' }}
        ref={bottomRef}
      />
    </div>
  );
};

function mapStateToProps(state: CourseState, ownProps: ChatOwnProps): ChatOutputProps {
  const room = state.ui.chat.rooms[ownProps.roomId];
  return {
    ...ownProps,
    lastUpdated: room ? room.lastUpdated : Date.now(),
  };
}

export default connect(mapStateToProps)(ChatOutput);
