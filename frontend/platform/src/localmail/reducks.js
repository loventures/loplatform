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

// actions
export const LOADED_MESSAGES = 'LOADED_MESSAGES';
export const LOAD_ERROR = 'LOAD_ERROR';
export const SELECT_ACCOUNT = 'SELECT_ACCOUNT';
export const SELECT_MESSAGE = 'SELECT_MESSAGE';

// reducer
export const reducer = (
  state = {
    loaded: false,
    error: false,
    messagess: {},
    currentAccount: '',
    currentMessage: 0,
  },
  action
) => {
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
export const messagesLoaded = messagess => ({
  type: LOADED_MESSAGES,
  messagess,
});

export const loadError = msg => ({
  type: LOAD_ERROR,
  msg,
});

export const selectAccount = account => ({
  type: SELECT_ACCOUNT,
  account,
});

export const selectMessage = message => ({
  type: SELECT_MESSAGE,
  message,
});

import axios from 'axios';

export const loadLocalmail = account => dispatch => {
  axios
    .get(`/api/v2/localmail/${account}`)
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

export const loadLocalmails = () => dispatch => {
  axios
    .get('/api/v2/localmail')
    .then(({ data: messagess }) => {
      dispatch(messagesLoaded(messagess));
    })
    .catch(e => {
      console.log(e);
      dispatch(loadError('error.unexpectedError'));
    });
};
