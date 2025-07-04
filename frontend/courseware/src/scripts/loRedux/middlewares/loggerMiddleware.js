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

import { createLogger } from 'redux-logger';

const env = window.lo_platform.environment;

const transformSlicedAction = action => ({
  ...action,
  type: `${action.sliceName ? action.sliceName + '/' : ''}${action.id ? action.id + '/' : ''}${
    action.type
  }`,
});

const actionTransformer = action => {
  if (action.type === 'BATCHING_REDUCER.BATCH') {
    action.payload.type = action.payload.map(next => transformSlicedAction(next).type).join(' => ');
    return action.payload;
  }

  return transformSlicedAction(action);
};

const logger = env.isMock
  ? new Proxy(console, {
      //This modifies logging behavior to patch up
      //the fact that karma intercepts all logs and prints them as string
      //which loses all the formatting.
      //this kind of mimics the behavior by making objects into
      //its own collapsable group.
      get: function (obj, prop) {
        const notGroup = prop !== 'groupCollapsed' && prop !== 'group' && prop !== 'groupEnd';
        return (...args) => {
          if (notGroup) {
            console.groupCollapsed(...args);
          }
          obj[prop](
            ...args.map(arg => {
              if (typeof arg === 'object') {
                try {
                  //redux-ui-router's actions includes
                  //an event object with an attached scope
                  //which are both circular
                  return window.JSON.stringify(arg);
                } catch (e) {
                  return arg;
                }
              } else {
                return arg;
              }
            })
          );
          if (notGroup) {
            console.groupEnd(...args);
          }
        };
      },
    })
  : console;

export const createLoggerMiddleware = () =>
  createLogger({
    level: 'info',
    duration: true,
    diff: true,
    actionTransformer,
    logger,
  });
