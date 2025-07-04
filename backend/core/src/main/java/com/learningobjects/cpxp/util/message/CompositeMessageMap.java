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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A composite message map. Composites a list of message maps.
 */
public class CompositeMessageMap extends BaseMessageMap {
    private List<MessageMap> _messageMaps = new ArrayList<MessageMap>();
    private Locale _locale;

    public CompositeMessageMap(Locale locale){
        _locale = locale;
    }

    public void addMessageMap(MessageMap map) {
        _messageMaps.add(map);
    }

    public List<MessageMap> getMessageMaps() {
        return _messageMaps;
    }

    /**
     * Prefixes a new message map that contains expansions of all message references
     * in the language packs.
     */
    public void expand() {
        if (!_messageMaps.isEmpty()) {
            BaseMessageMap expanded = new BaseMessageMap(_messageMaps.get(0).getLocale(), _messageMaps.get(0).getTimeZone(), new Properties());
            for (MessageMap map: _messageMaps) {
                for (String key: map.keySet()) {
                    expand(key, expanded);
                }
            }
            expanded.put("Country", expanded.getLocale().getCountry());
            expanded.put("Language", expanded.getLocale().getLanguage());
            expanded.put("Extension", Optional.ofNullable(expanded.getLocale().getExtension(Locale.PRIVATE_USE_EXTENSION)).map("x-"::concat).orElse(""));
            _messageMaps.add(0, expanded);
        }
    }

    private static final Pattern TOKEN_RE = Pattern.compile("``([^`]+)``"); // capture ``foo``

    private String expand(String key, BaseMessageMap expanded) {
        if (expanded.containsKey(key)) {
            return expanded.get(key);
        }
        String value = getMessage(key);
        if (value == null) {
            java.util.logging.Logger.getLogger(CompositeMessageMap.class.getName()).warning("UNKNOWN MESSAGE: " + key);
            return "???" + key + "???";
        }
        Matcher matcher = TOKEN_RE.matcher(value);
        if (!matcher.find()) {
            return value;
        }
        expanded.put(key, "!!!" + key + "!!!"); // catch oo recursion
        StringBuffer sb = new StringBuffer();
        do {
            String[] tokens = matcher.group(1).split(",");
            String replacement = expand(tokens[0], expanded);
            // {foo,2,3} replaces {0} and {1} in foo with {2} and {3}
            // TODO: cope with more cases - specifically
            // on = on {0}; byOn = by {0} {on,1}; msg = blah {byOn,2,3}
            // see msg_onDateAtTime
            for (int i = 1; i < tokens.length; ++ i) {
                replacement = replacement.replace("{" + (i - 1), "{" + tokens[i]);
            }
            matcher.appendReplacement(sb, replacement);
        } while (matcher.find());
        matcher.appendTail(sb);
        value = sb.toString();
        expanded.put(key, value);
        return value;
    }

    @Override
    public Locale getLocale() {
        return _locale != null ? _locale : (_messageMaps.isEmpty() ? Locale.getDefault() : _messageMaps.get(0).getLocale());
    }

    @Override
    public TimeZone getTimeZone() {
        return _messageMaps.isEmpty() || (_messageMaps.get(0).getTimeZone() == null) ? TimeZone.getDefault() : _messageMaps.get(0).getTimeZone();
    }

    @Override
    public String getMessage(String key) {
        for (MessageMap map: _messageMaps) {
            String value = map.getMessage(key);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    @Override
    public int size() {
        int size = 0;
        for (MessageMap map: _messageMaps) {
            size += map.size();
        }
        return size;
    }

    @Override
    public String toString() {
        return _messageMaps.toString();
    }
}
