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

package com.learningobjects.cpxp.util.message;

import java.util.*;

/**
 * A message map. Consumes a locale key name and properties.
 */
public class BaseMessageMap extends HashMap<String, String> implements MessageMap {
    private Locale _locale;

    private TimeZone _timeZone;

    protected BaseMessageMap() {
    }

    public BaseMessageMap(Locale locale, TimeZone timeZone, Properties properties) {
        for (String s: properties.stringPropertyNames()) {
            put(s, properties.getProperty(s));
        }
        _locale = locale;
        _timeZone = timeZone;
    }

    public BaseMessageMap(Locale locale, TimeZone timeZone, Map<String, String> messages) {
        super(messages);
        _locale = locale;
        _timeZone = timeZone;
    }

    @Override
    public String get(Object key) {
        String msg = getMessage((String) key);
        if (msg == null) {
            java.util.logging.Logger.getLogger(BaseMessageMap.class.getName()).warning("UNKNOWN MESSAGE: " + key);
        }
        return (msg == null) ?  "???" + key + "???" : msg;
    }

    @Override
    public String getMessage(String key) {
        return super.get(key);
    }

    @Override
    public Locale getLocale() {
        return _locale;
    }

    @Override
    public TimeZone getTimeZone() {
        return _timeZone;
    }

    @Override
    public String toString() {
        return _locale.toString();
    }
}
