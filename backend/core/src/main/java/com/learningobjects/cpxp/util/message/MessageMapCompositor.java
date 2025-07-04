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

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

/**
 * A factory for producing composite message maps for specific locales.
 */
public class MessageMapCompositor {
    private Map<Locale, MessageMap> _availableMessages = new HashMap<>();
    private Map<Locale, CompositeMessageMap> _localeMessages = new HashMap<Locale, CompositeMessageMap>();
    private Locale _defaultLocale;
    private CompositeMessageMap _defaultMessages;

    public synchronized void addMessageMap(MessageMap messageMap) {
        _availableMessages.put(messageMap.getLocale(), messageMap);
    }

    public synchronized MessageMap getOrCreateMessageMap(Locale locale, TimeZone timeZone) {
        return _availableMessages.computeIfAbsent(locale, (l) -> new BaseMessageMap(l, timeZone, new HashMap<>()));
    }

    // NOTE: This has to be called AFTER all the message maps
    // have been added.
    public synchronized void setDefaultLocale(Locale locale) {
        _defaultLocale = locale;
        _defaultMessages = createLocaleMap(locale);
    }

    public Locale getDefaultLocale() {
        return _defaultLocale;
    }

    public Map<Locale, MessageMap> getAvailableMessages() {
        return _availableMessages;
    }

    /**
     * This returns a composite map of...
     * language1_country1, language1, language2_country2, language2, en
     * Where language1 etc are from the specified locale, and language2 are
     * from the default locale.
     */
    public synchronized CompositeMessageMap getCompositeMap(Locale locale) {
        if (locale == null) {
            return _defaultMessages;
        }
        CompositeMessageMap messages = _localeMessages.get(locale);
        if (messages == null) {
            // For an external locale we create an appropriate messages map
            messages = createLocaleMap(locale);
            if (_defaultMessages != null) {
                // And then add the default messageMaps
                for (MessageMap map: _defaultMessages.getMessageMaps()) {
                    messages.addMessageMap(map);
                }
            }
            if (_availableMessages.containsKey(Locale.ENGLISH)) { // last fallback
                messages.addMessageMap(_availableMessages.get(Locale.ENGLISH));
            }
            messages.expand();
            // And then cache it
            _localeMessages.put(locale, messages);
        }
        return messages;
    }

    // This creates a composite map of...
    //   language_country, language
    private CompositeMessageMap createLocaleMap(Locale locale) {
        CompositeMessageMap messages = new CompositeMessageMap(locale);
        String language = locale.getLanguage();
        String country = locale.getCountry();
        for (Locale selector: new Locale[] { new Locale(language, country),
                                             new Locale(language) }) {
            // Is this particular selector supported
            MessageMap map = _availableMessages.get(selector);
            if (map != null) {
                messages.addMessageMap(map);
            }
        }

        if(country == ""){
            for (Locale selector : Locale.filter(Locale.LanguageRange.parse(locale.toLanguageTag()), _availableMessages.keySet())) {
                MessageMap map = _availableMessages.get(selector);
                if(map != null){
                    messages.addMessageMap(map);
                }
            }
        }

        return messages;
    }
}
