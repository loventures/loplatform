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

import React from 'react';
import { connect } from 'react-redux';
import { Badge, ListGroup, ListGroupItem } from 'reactstrap';
import _ from 'underscore';

import { selectAccount, selectMessage } from './reducks';

const Account = connect(state => ({ currentAccount: state.localmail.currentAccount }), {
  selectAccount,
})(
  ({
    account,
    messages,

    currentAccount,
    selectAccount,
  }) => (
    <React.Fragment>
      <ListGroupItem
        className="justify-content-between"
        color={account === currentAccount ? 'info' : null}
        onClick={() => selectAccount(account)}
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
          <Message
            key={message.id}
            message={message}
          />
        ))}
    </React.Fragment>
  )
);

const Message = connect(
  state => ({
    currentMessage: state.localmail.currentMessage,
  }),
  { selectMessage }
)(
  ({
    message,

    currentMessage,
    selectMessage,
  }) => (
    <ListGroupItem
      active={message.id === currentMessage}
      onClick={() => selectMessage(message.id)}
      className="ps-5 localmail-item"
    >
      {message.subject}
    </ListGroupItem>
  )
);

const Selector = connect(state => ({
  messagess: state.localmail.messagess,
}))(({ messagess }) => (
  <ListGroup>
    {_.map(messagess, (messages, id) => (
      <Account
        key={id}
        account={id}
        messages={messages}
      />
    ))}
  </ListGroup>
));

export default Selector;
