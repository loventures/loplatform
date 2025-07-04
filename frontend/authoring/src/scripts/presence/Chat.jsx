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

import React, { useEffect, useRef, useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';

import { formatFullDate, fromNow } from '../dateUtil';
import { withIconErrorBoundary } from '../hoc';
import { usePolyglot } from '../hooks';
import { ShowTimestampDelta } from './model/Conversation';
import { closeChat, sendMessage, sendTyping } from './PresenceActions';

const urlRe =
  /\b((?:[a-z][\w-]+:(?:\/{1,3}|[a-z0-9%])|www\d{0,3}[.]|[a-z0-9.-]+[.][a-z]{2,4}\/)(?:[^\s()<>]+|\(([^\s()<>]+|(\([^\s()<>]+\)))*\))+(?:\(([^\s()<>]+|(\([^\s()<>]+\)))*\)|[^\s`!()[\]{};:'".,<>?«»“”‘’]))/gi;

const matchAll = (
  str,
  re // TODO: This should just be [...str.matchAll(re)] which works in the console but not in this app...
) =>
  (fn => fn([], re.exec(str), fn))(
    (fa, a, self) => (a ? self([...fa, a], re.exec(str), self) : fa),
    (re.lastIndex = 0)
  );

const branchUrl = document.location.href.replace(/\/asset.*/, '');

const formatLine = line => {
  if (!urlRe.test(line)) return line;
  const { index, parts } = matchAll(line, urlRe).reduce(
    ({ index, parts }, match) => ({
      index: match.index + match[0].length,
      parts: [
        ...parts,
        line.substring(index, match.index),
        match[0].startsWith(branchUrl) ? (
          <a
            key={index}
            href={match[0].replace(/.*#/, '#')}
          >
            {match[0]}
          </a>
        ) : (
          <a
            key={index}
            target="_blank"
            rel="noreferrer"
            href={match[0]}
          >
            {match[0]}
          </a>
        ),
      ],
    }),
    { index: 0, parts: [] }
  );
  return [...parts, line.substring(index)];
};

const Chat = () => {
  const dispatch = useDispatch();
  const polyglot = usePolyglot();
  const user = useSelector(state => state.user.profile);
  const { chatRoom, profiles, roomConversations } = useSelector(state => state.presence);
  const conversation = roomConversations[chatRoom];

  // Keep the timestamps alive.
  const [generation, setGeneration] = useState(0);
  useEffect(() => {
    const interval = setInterval(() => setGeneration(1 + generation), 600000);
    return () => clearInterval(interval);
  }, []);

  // Send primitive typing notifications.
  const [typing, setTyping] = useState(false);
  const onChange = e => {
    const nonEmpty = e.target.value !== '';
    if (typing !== nonEmpty) {
      setTyping(nonEmpty);
      dispatch(sendTyping(chatRoom, nonEmpty));
    }
  };
  // Clear them when we close up shop.
  useEffect(() => () => typing && dispatch(sendTyping(chatRoom, false)), [typing]);

  // Autofocus (using autofocus attribute causes undesirable scrolling)
  const myInput = useRef();
  useEffect(() => {
    const timeout = setTimeout(() => myInput.current && myInput.current.focus(), 333); // css transition time
    return () => clearTimeout(timeout);
  }, []);

  // Autoscroll
  const myDiv = useRef();
  useEffect(() => {
    if (myDiv.current) myDiv.current.scrollTop = myDiv.current.scrollHeight;
  }, [conversation]);

  const closePanel = e => {
    e.preventDefault();
    dispatch(closeChat());
  };

  const onSubmit = e => {
    e.preventDefault();
    const input = e.target.elements.chat;
    const value = input.value;
    if (value) {
      dispatch(sendMessage(chatRoom, value));
      setTyping(false);
      input.value = '';
    }
  };

  const now = new Date().getTime();
  const colorOf = handle =>
    'hsl(' + ((((profiles[handle] && profiles[handle].id) || 0) * 47) % 360) + ', 50%, 40%)';
  const nameOf = handle => (profiles[handle] && profiles[handle].givenName) || '???';
  const dateOf = m => {
    const hours = (now - new Date(m).getTime()) / 3600000;
    return hours >= 12 ? formatFullDate(m) : fromNow(m);
  };

  const { stanzas, typingUsers } = conversation;
  const lastSender = stanzas.length && stanzas[stanzas.length - 1].sender;
  const typists = Object.keys(typingUsers).filter(
    sender => sender !== user.handle && sender !== lastSender
  );
  return (
    <div className={`branch-chat ${generation ? 'open' : ''}`}>
      <div className="chat-header">
        <div className="chat-title">{polyglot.t('CHAT_TITLE')}</div>
        <a
          className="chat-close"
          href=""
          onClick={closePanel}
          title={polyglot.t('CLOSE')}
        >
          <i className="material-icons">close</i>
        </a>
      </div>
      <div
        className="chat-body"
        ref={myDiv}
      >
        {stanzas.map(({ finish, lines, start, sender, showTimestampAfter }, index) => {
          const isLastStanza = index === stanzas.length - 1;
          const showTrailingTimestamp = isLastStanza && now - finish >= ShowTimestampDelta;
          return (
            <div
              key={`${sender}-${start}`}
              className="chat-stanza"
            >
              {lines.map((line, jndex) => (
                <div
                  key={jndex}
                  className="chat-line"
                >
                  {!jndex && (
                    <span
                      className="name"
                      style={{ color: colorOf(sender) }}
                    >
                      {sender === user.handle ? polyglot.t('CHAT_YOU') : nameOf(sender)}:{' '}
                    </span>
                  )}
                  <span>{formatLine(line)}</span>
                </div>
              ))}
              {sender !== user.handle && typingUsers[sender] && isLastStanza && (
                <div className="loading-ellipsis">
                  <span>.</span>
                  <span>.</span>
                  <span>.</span>
                </div>
              )}
              {(showTimestampAfter || showTrailingTimestamp) && (
                <div className="timestamp">{dateOf(finish)}</div>
              )}
            </div>
          );
        })}
        {typists.map(sender => (
          <div
            key={`typing-${sender}`}
            className="chat-stanza"
          >
            <span
              className="name"
              style={{ color: colorOf(sender) }}
            >
              {nameOf(sender)}{' '}
            </span>
            <span className="loading-ellipsis">
              <span>.</span>
              <span>.</span>
              <span>.</span>
            </span>
          </div>
        ))}
      </div>
      <form
        className="chat-footer"
        onSubmit={onSubmit}
      >
        <input
          name="chat"
          autoComplete="off"
          className="chat-input form-control"
          type="text"
          ref={myInput}
          onChange={onChange}
          placeholder={polyglot.t('CHAT_MESSAGE_OTHER_AUTHORS')}
        />
      </form>
    </div>
  );
};

export default withIconErrorBoundary(Chat);
