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

import React from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { Badge, ListGroup, ListGroupItem } from 'reactstrap';
import _ from 'underscore';

import { LocalmailState, Message, selectAccount, selectMessage } from './reducks';

interface LocalmailRootState {
  localmail: LocalmailState;
}

interface AccountProps {
  account: string;
  messages: Message[];
}

const Account: React.FC<AccountProps> = ({ account, messages }) => {
  const dispatch = useDispatch();
  const currentAccount = useSelector((state: LocalmailRootState) => state.localmail.currentAccount);
  return (
    <React.Fragment>
      <ListGroupItem
        className="justify-content-between"
        color={account === currentAccount ? 'info' : undefined}
        onClick={() => dispatch(selectAccount(account))}
      >
        {account}
        <Badge
          pill
          className="ms-2"
        >
          {messages.length}
        </Badge>
      </ListGroupItem>
      {account === currentAccount &&
        messages.map(message => (
          <MessageItem
            key={message.id}
            message={message}
          />
        ))}
    </React.Fragment>
  );
};

interface MessageItemProps {
  message: Message;
}

const MessageItem: React.FC<MessageItemProps> = ({ message }) => {
  const dispatch = useDispatch();
  const currentMessage = useSelector((state: LocalmailRootState) => state.localmail.currentMessage);
  return (
    <ListGroupItem
      active={message.id === currentMessage}
      onClick={() => dispatch(selectMessage(message.id))}
      className="ps-5 localmail-item"
    >
      {message.subject}
    </ListGroupItem>
  );
};

const Selector: React.FC = () => {
  const messagess = useSelector((state: LocalmailRootState) => state.localmail.messagess);
  return (
    <ListGroup>
      {_.map(messagess, (messages, id) => (
        <Account
          key={id}
          account={id}
          messages={messages}
        />
      ))}
    </ListGroup>
  );
};

export default Selector;
