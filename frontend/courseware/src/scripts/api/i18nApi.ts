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
import { Loader } from '../api/commonTypes';
import { createUrl, loConfig } from '../bootstrap/loConfig';
import { mapValues } from 'lodash';

export type I18n = Record<string, string>;

/*
 *  If the locale is unknown to the server, we serve English. So
 *  we must ask the translations themselves to tell us who they are.
 *  i.e. translations.data['IETF language tag']
 * */
const getI18n = (locale: string, component: string): Promise<I18n> => {
  return axios
    .get<I18n>(createUrl(loConfig.i18n, { locale, component }))
    .then(response => {
      if (import.meta.env.PROD) {
        return response.data;
      } else {
        return getLocalI18nData(response.data, locale);
      }
    })
    .catch(() => {
      // swallow to make sure the app still renders.
      console.warn(`Translation file was not found for locale ${locale}`);
      return {};
    });
};

export const loaderForAngularTranslate = (): Loader<I18n> => {
  const lo_platform = window.lo_platform;
  const locale = lo_platform.i18n.locale;
  const component = lo_platform.identifier;
  return function () {
    return getI18n(locale, component);
  };
};

export const getLocalI18nData = (serverI18n: I18n, locale: string): Promise<I18n> => {
  const data = import.meta.glob('../../i18n/*.json');
  return data['../../i18n/Courseware_en.json']().then(({ default: localI18n }: any) => {
    const expandedI18n = mapValues(
      localI18n,
      val => val && val.replace(/``([^`]*)``/g, (m, g) => localI18n[g] || g).replace(/""/g, '"')
    );
    return { ...serverI18n, ...expandedI18n };
  });
};
