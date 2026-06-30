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

import axios from 'axios';
import { Dispatch } from 'redux';

export interface Address {
  address: string;
  name: string;
}

export interface Attachment {
  id: number;
  fileName: string;
}

export interface Message {
  id: number;
  from: Address;
  to: Address;
  subject: string;
  body: string;
  date: string;
  attachments: Attachment[];
}

export type Messages = Record<string, Message[]>;

export interface LocalmailState {
  loaded: boolean;
  error: boolean | string;
  messagess: Messages;
  currentAccount: string;
  currentMessage: number;
}

// actions
export const LOADED_MESSAGES = 'LOADED_MESSAGES';
export const LOAD_ERROR = 'LOAD_ERROR';
export const SELECT_ACCOUNT = 'SELECT_ACCOUNT';
export const SELECT_MESSAGE = 'SELECT_MESSAGE';

interface MessagesLoadedAction {
  type: typeof LOADED_MESSAGES;
  messagess: Messages;
  [extraProps: string]: unknown;
}

interface LoadErrorAction {
  type: typeof LOAD_ERROR;
  msg: string;
  [extraProps: string]: unknown;
}

interface SelectAccountAction {
  type: typeof SELECT_ACCOUNT;
  account: string;
  [extraProps: string]: unknown;
}

interface SelectMessageAction {
  type: typeof SELECT_MESSAGE;
  message: number;
  [extraProps: string]: unknown;
}

type LocalmailAction =
  | MessagesLoadedAction
  | LoadErrorAction
  | SelectAccountAction
  | SelectMessageAction;

const initialState: LocalmailState = {
  loaded: false,
  error: false,
  messagess: {},
  currentAccount: '',
  currentMessage: 0,
};

// reducer
export const reducer = (
  state: LocalmailState = initialState,
  action: LocalmailAction
): LocalmailState => {
  switch (action.type) {
    case LOADED_MESSAGES:
      return {
        ...state,
        loaded: true,
        messagess: action.messagess,
      };
    case LOAD_ERROR:
      return {
        ...state,
        error: action.msg,
      };
    case SELECT_ACCOUNT:
      return {
        ...state,
        currentAccount: action.account,
        currentMessage: action.account === state.currentAccount ? state.currentMessage : 0,
      };
    case SELECT_MESSAGE:
      return {
        ...state,
        currentMessage: action.message,
      };
    default:
      return state;
  }
};

// creators
export const messagesLoaded = (messagess: Messages): MessagesLoadedAction => ({
  type: LOADED_MESSAGES,
  messagess,
});

export const loadError = (msg: string): LoadErrorAction => ({
  type: LOAD_ERROR,
  msg,
});

export const selectAccount = (account: string): SelectAccountAction => ({
  type: SELECT_ACCOUNT,
  account,
});

export const selectMessage = (message: number): SelectMessageAction => ({
  type: SELECT_MESSAGE,
  message,
});

export const loadLocalmail = (account: string) => (dispatch: Dispatch) => {
  axios
    .get<Message[]>(`/api/v2/localmail/${account}`)
    .then(({ data: messages }) => {
      dispatch(messagesLoaded(messages.length ? { [account]: messages } : {}));
      dispatch(selectAccount(account));
      if (messages.length) {
        dispatch(selectMessage(messages[0].id));
      }
    })
    .catch(e => {
      console.log(e);
      dispatch(loadError('error.unexpectedError'));
    });
};

export const loadLocalmails = () => (dispatch: Dispatch) => {
  axios
    .get<Messages>('/api/v2/localmail')
    .then(({ data: messagess }) => {
      dispatch(messagesLoaded(messagess));
    })
    .catch(e => {
      console.log(e);
      dispatch(loadError('error.unexpectedError'));
    });
};
