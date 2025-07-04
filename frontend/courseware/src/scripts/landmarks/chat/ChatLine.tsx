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

import { get } from 'lodash';
import { TranslationContext } from '../../i18n/translationContext';
import React, { useContext } from 'react';

import LoadingEllipsis from './LoadingEllipsis';
import { lojector } from '../../loject';

export type ChatLineProps = {
  userHandle?: string;
  displayFullName?: boolean;
  line?: string;
};

const ChatLine: React.FunctionComponent<ChatLineProps> = ({
  userHandle,
  displayFullName,
  line,
}) => {
  const translate = useContext(TranslationContext);
  const isMyself = userHandle === get(window, 'lo_platform.user.handle', null);
  const getUserName = (handle: string) => {
    if (!handle) {
      return;
    }
    const profile = get<any>(lojector.get('PresentUsers'), `presentUsers[${handle}]`, null);
    if (profile) {
      return displayFullName ? profile.fullName : profile.givenName;
    }
  };

  return (
    <div className="chat-line">
      {userHandle && isMyself && (
        <span className="you">
          {translate('CHAT_SENDER_YOU', { name: getUserName(userHandle) })}
        </span>
      )}
      {userHandle && !isMyself && (
        <span className="them">
          {translate('CHAT_SENDER_THEM', { name: getUserName(userHandle) })}
        </span>
      )}
      {line ? (
        <span className="msg">&nbsp;{line}</span>
      ) : (
        <span>
          &nbsp;<LoadingEllipsis></LoadingEllipsis>
        </span>
      )}
    </div>
  );
};

export default ChatLine;
