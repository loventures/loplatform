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

import axios from 'axios';
import cookies from 'browser-cookies';
import React from 'react';
import { mapObject } from 'underscore';

import { NoSessionExtensionHdr } from './Headers';
import { LoginUrl } from './URLs';

const isDevelopment = (process.env as any).NODE_ENV === 'development';

const checkSession = () => {
  return axios
    .get('/api/v0/session', {
      headers: { [NoSessionExtensionHdr]: 'true' },
      hideProgress: true,
    } as any)
    .then(res => {
      return { err: null, valid: res.data.valid };
    })
    .catch(err => {
      return { err: err };
    });
};

const getAdminPages = () => {
  return axios.get('/api/v2/adminPages');
};

const getPlatform = (force?: boolean) => {
  return window.lo_platform && !force
    ? Promise.resolve({ data: window.lo_platform })
    : axios.get('/api/v2/lo_platform');
};

const getTranslations = (locale: any) => {
  if (isDevelopment) {
    return addLocalI18nData({}, locale);
  }
  return axios.get('/api/v2/i18n/' + locale + '/loi.platform.domain.Domain').then(res => {
    if (!isDevelopment) {
      return res;
    } else {
      return addLocalI18nData(res.data, locale);
    }
  });
};

// locale 2nd parameter ignored
const addLocalI18nData = (serverI18n: any, _locale: any) => {
  try {
    const data = import.meta.glob('../i18n/*.json');
    return data['../i18n/Platform_en.json']().then(({ default: localI18n }: any) => {
      const expandedI18n = mapObject(
        localI18n,
        (val: string) =>
          val &&
          val
            .replace(/``([^`]*)``/g, (_m: string, g: string) => localI18n[g] || g)
            .replace(/""/g, '"')
      );
      return { data: { ...serverI18n, ...expandedI18n } };
    });
  } catch (errWhichProbablyMeansOurLanguageIsNotThere) {
    console.log(errWhichProbablyMeansOurLanguageIsNotThere);
    return { data: serverI18n };
  }
};

const asjax = (url: string, data: any, progress: any, started: () => void) => {
  return axios.post(url, data).then(response => {
    if (response.status === 202 && response.data.status === 'async') {
      if (started) started();
      return new Promise((resolve, reject) => {
        const channel = response.data.channel;
        const msgs = new EventSource(`/event${channel}`);
        let timestamp = new Date(0);
        msgs.addEventListener(channel, event => {
          const data = JSON.parse(event.data),
            body = data.body;
          if (data.status === 'ok') {
            msgs.close();
            resolve(body);
          } else if (data.status === 'progress') {
            const eventDate = new Date(data.timestamp);
            if (eventDate > timestamp) {
              timestamp = eventDate;
              progress(body.description, body.done, body.todo);
            }
          } else if (data.status === 'warning') {
            progress(body.message);
          } else {
            msgs.close();
            reject(body);
          }
        });
      });
    } else {
      return Promise.reject('Unexpected success.');
    }
  });
};

// dynamically inject the csrf cookie value into every axios request
const initCsrf = () => {
  const injectCsrfConfig = (config: any) => ({
    ...config,
    headers: { ...(config.headers || {}), 'X-CSRF': cookies.get('CSRF') || 'true' },
  });
  axios.interceptors.request.use(injectCsrfConfig, Promise.reject);
};

const getAnnouncements = () => {
  return axios.get('/api/v2/announcements/active');
};

const ContentTypeURLEncoded = { headers: { 'Content-Type': 'application/x-www-form-urlencoded' } };

const ContentTypeMultipart = { headers: { 'Content-Type': 'multipart/form-data' } };

const ponyName = (version: string) => version.replace(/_/g, ' ');

const staticUrl = (url: string) => url.replace('$$url', window.lo_static_url);

const trim = (s: string | undefined) => (s ? s.trim() : '');

const ponyImageName = (version: string) => `${version.replace(/_\d+$/, '').replace(/_/g, '')}.svg`;

/* Renders an <a> tag for a translated message where the clickable
 * part may be a part of the message. For example:
 * "Foo: <a>bar</a>" as a translation message would work. */
const Hyperlink = ({ label, ...props }: any) => {
  const bits = label.match(/(.*)<a>(.*)<\/a>(.*)/) || ['', '', label, ''];
  return (
    <React.Fragment>
      {bits[1]}
      <a {...props}>{bits[2]}</a>
      {bits[3]}
    </React.Fragment>
  );
};

const login = (username: string, password: string, remember: any, overlord: boolean) => {
  const post = `username=${encodeURIComponent(username)}&password=${encodeURIComponent(
    password
  )}&remember=${!!remember}`;
  const config: { headers: Record<string, string> } = ContentTypeURLEncoded;
  if (isDevelopment && overlord) {
    config.headers = { ...config.headers, 'X-DomainId': 'overlord' };
  }
  return axios.post(LoginUrl, post, config);
};

const EmailRE = /\S+@(?:localmail|\S+\.\S+)/;

const contrast = (hex: string) => {
  const r = parseInt(hex.substring(1, 3), 16);
  const g = parseInt(hex.substring(3, 5), 16);
  const b = parseInt(hex.substring(5, 7), 16);
  const v = (r * 299 + g * 587 + b * 114) / 1000;
  return v > 127 ? '#111' : '#eee';
};

export {
  asjax,
  checkSession,
  contrast,
  ContentTypeMultipart,
  ContentTypeURLEncoded,
  EmailRE,
  getAdminPages,
  getAnnouncements,
  getPlatform, // yeesh
  getTranslations,
  initCsrf,
  isDevelopment,
  login,
  ponyName,
  ponyImageName,
  staticUrl,
  trim,
  Hyperlink,
};
