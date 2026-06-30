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

import { Address, Attachment, Message } from './reducks';

const fmtAddr = ({ name, address }: Address) => (name ? `${name} <${address}>` : address);

interface HeaderRowProps {
  name: string;
  children: React.ReactNode;
  id?: string;
}

const HeaderRow: React.FC<HeaderRowProps> = ({ name, children, id }) => (
  <React.Fragment>
    <dt className="col-sm-2">{name}</dt>
    <dl
      id={id}
      className="col-sm-10"
    >
      {children}
    </dl>
  </React.Fragment>
);

interface MessageBodyProps {
  body: string;
  id?: string;
}

const SuperUnsafeInsecureMessageBodyOhNoes: React.FC<MessageBodyProps> = ({ body, id }) => (
  <div
    id={id}
    className="localmail-body"
    dangerouslySetInnerHTML={{ __html: body }}
  />
);

interface AttachmentsProps {
  account: string;
  mid: number;
  attachments: Attachment[];
}

const Attachments: React.FC<AttachmentsProps> = ({ account, mid, attachments }) => (
  <>
    {attachments.map(att => (
      <div
        className="my-3"
        key={att.id}
      >
        <i
          className="material-icons md-24 me-2"
          aria-hidden="true"
        >
          attach_file
        </i>
        <a
          key={att.id}
          download
          className="attachment"
          href={`/api/v2/localmail/${account}/${mid}/attachments/${att.id}`}
        >
          {att.fileName}
        </a>
      </div>
    ))}
  </>
);

interface MessageDisplayProps {
  account: string;
  message: Message;
}

const MessageDisplay: React.FC<MessageDisplayProps> = ({ account, message }) => (
  <div className="localmail pt-3">
    <dl className="row">
      <HeaderRow
        id="localmail-from"
        name="From"
      >
        {fmtAddr(message.from)}
      </HeaderRow>
      <HeaderRow
        id="localmail-to"
        name="To"
      >
        {fmtAddr(message.to)}
      </HeaderRow>
      <HeaderRow
        id="localmail-subject"
        name="Subject"
      >
        {message.subject}
      </HeaderRow>
      <HeaderRow
        id="localmail-date"
        name="Date"
      >
        {message.date}
      </HeaderRow>
    </dl>
    <hr {...({ size: '1' } as React.HTMLAttributes<HTMLHRElement>)} />
    <SuperUnsafeInsecureMessageBodyOhNoes
      id="localmail-body"
      body={message.body}
    />
    <hr {...({ size: '3' } as React.HTMLAttributes<HTMLHRElement>)} />
    {!!message.attachments.length && (
      <Attachments
        account={account}
        mid={message.id}
        attachments={message.attachments}
      />
    )}
  </div>
);

export default MessageDisplay;
