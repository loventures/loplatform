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

package com.learningobjects.cpxp.util;

import com.learningobjects.cpxp.BaseWebContext;
import com.learningobjects.cpxp.WebContext;
import com.learningobjects.cpxp.service.Current;
import com.learningobjects.cpxp.service.internationalization.VersionedMessageMapCompositor;
import com.learningobjects.cpxp.util.message.BaseMessageMap;
import com.learningobjects.cpxp.util.message.MessageMap;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import java.text.DateFormat;
import java.text.Format;
import java.text.MessageFormat;
import java.text.ParsePosition;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// TODO: The Faces bits should really be moved to a .jsf class
// maybe..

/**
 * Internationalization utils.
 */
public class InternationalizationUtils {
    private static final Logger logger = Logger.getLogger(InternationalizationUtils.class.getName());
    public static final String REQUEST_ATTRIBUTE_MSG = "msg";
    public static final String REQUEST_ATTRIBUTE_CONSTANT = "constant";
    public static final String REQUEST_ATTRIBUTE_MSG_VER = "msgVer";
    public static final String REQUEST_ATTRIBUTE_TIME_ZONE = "timezone";

    /**
     * This returns the primary locale in which the response is being rendered.
     */
    public static Locale getLocale() {
        return getMessages().getLocale();
    }

    /**
     * This returns the primary time zone in which the response is being
     * rendered.
     */
    public static TimeZone getTimeZone() {
        TimeZone timeZone = null;

        HttpServletRequest request = BaseWebContext.getContext().getRequest();
        if (request != null) {
            HttpSession session = request.getSession(false);
            if (session != null) {
                timeZone = (TimeZone) session.getAttribute(REQUEST_ATTRIBUTE_TIME_ZONE);
                if (timeZone != null) {
                    return timeZone;
                }
            }
        }

        timeZone = getMessages().getTimeZone();
        if (timeZone != null) {
            return timeZone;
        }

        return TimeZone.getTimeZone(Current.getDomainDTO().getTimeZone());
    }

    public static void setTimeZone(TimeZone timeZone) {
        try {
            HttpServletRequest request = BaseWebContext.getContext().getRequest();
            if (request != null) {
                HttpSession session = request.getSession(true);
                if (session != null) {
                    session.setAttribute(REQUEST_ATTRIBUTE_TIME_ZONE, timeZone);
                }
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to set time zone", e);
        }
    }

    public static boolean isSessionTimeZoneSet() {
        HttpServletRequest request = BaseWebContext.getContext().getRequest();
        if (request != null) {
            HttpSession session = request.getSession(false);
            if (session != null) {
                TimeZone timeZone = (TimeZone) session.getAttribute(REQUEST_ATTRIBUTE_TIME_ZONE);
                if (timeZone != null) {
                    return true;
                }
            }
        }
        return false;
    }

    public static Calendar getCalendar() {
        MessageMap messages = getMessages();
        return Calendar.getInstance(getTimeZone(), messages.getLocale());
    }

    public static String formatMessage(String key, Object... params) {
        MessageMap messages = getMessages();
        return formatMessage(messages, key, params);
    }

    public static String formatMessage(MessageMap messages, String key, Object... params) {
        String fmt = (String) messages.get(key);
        return format(fmt, params, messages.getLocale(), getTimeZone());
    }

    public static String getConstant(String key) {
        return (String) getConstants().getMessage(key);
    }

    public static String format(String fmt, Object... params) {
        MessageMap messages = getMessages();
        Locale locale = (messages == null) ? Locale.getDefault() : messages.getLocale();
        TimeZone timeZone = getTimeZone();
        return format(fmt, params, locale, timeZone);
    }

    private static final Pattern FORMAT_SIZE_RE = Pattern.compile("(\\d),number,(size|rate)");

    private static String format(String msg, Object[] params, Locale locale, TimeZone timeZone) {
        Matcher matcher = FORMAT_SIZE_RE.matcher(msg);
        if (matcher.find()) {
            // Hack in support for {0,number,size} and {0,number,rate}..
            // I explicitly change the params array which is evil, but..
            StringBuffer buf = new StringBuffer();
            do {
                int index = Integer.parseInt(matcher.group(1));
                String type = matcher.group(2);
                matcher.appendReplacement(buf, String.valueOf(index));
                if ("size".equals(type)) {
                    params[index] = formatSize((Long) params[index]);
                } else {
                    params[index] = formatRate((Long) params[index]);
                }
            } while (matcher.find());
            matcher.appendTail(buf);
            msg = buf.toString();
        }
        if (params.length > 0) {
            MessageFormat formatter = new MessageFormat(msg, locale);
            for (Format format: formatter.getFormats()) {
                if (format instanceof DateFormat) {
                    ((DateFormat) format).setTimeZone(timeZone);
                }
            }
            msg = formatter.format(params, new StringBuffer(), null).toString();
        }
        return msg;
    }

    public static String formatSize(Number size) {
        if (size == null) {
            return "";
        }
        NumberUtils.Unit unit = NumberUtils.getDataSizeUnit(size.longValue(), 1);
        double scaled = NumberUtils.divide(size.longValue(), unit.getValue(), 1);
        return formatMessage("function_FormatFunction_formatSize_" + unit, scaled, scaled); // I need to duplicate the parameter so that the choice can be parsed
    }

    public static String formatDuration(Number duration) {
        if (duration == null) {
            return "";
        }
        DateUtils.Unit unit = DateUtils.getDurationUnit(duration.longValue(), 1.5);
        double scaled = NumberUtils.divide(duration.longValue(), unit.getValue(), 0);
        return formatMessage("function_FormatFunction_formatDuration_" + unit, scaled, scaled); // I need to duplicate the parameter so that the choice can be parsed
    }

    public static String formatRate(Number rate) {
        if (rate == null) {
            return "";
        }
        NumberUtils.Unit unit = NumberUtils.getDataSizeUnit(rate.longValue(), 1);
        long scaled = (long) NumberUtils.divide(rate.longValue(), unit.getValue(), 1);
        return formatMessage("function_FormatFunction_formatRate_" + unit, scaled);
    }

    public static MessageMap getMessages() {
        return BaseWebContext.getContext().getMessages();
    }

    public static MessageMap getConstants() {
        return BaseWebContext.getContext().getConstants();
    }

    public static Long getMessageVersion() {
        WebContext webContext = BaseWebContext.getContext();
        if (webContext == null) {
            return null;
        }
        return (Long) webContext.getRequest().getAttribute(REQUEST_ATTRIBUTE_MSG_VER);
    }

    /**
     * Parse a size.
     *
     * @param size the size
     *
     * @return the parsed size
     */
    public static Long parseSize(String size) {
        size = StringUtils.deleteWhitespace(size); // is this valid?
        if (StringUtils.isEmpty(size)) {
            return null;
        }
        for (NumberUtils.Unit unit : NumberUtils.Unit.values()) {
            String key = "function_FormatFunction_formatSize_" + unit;
            String msg = (String) InternationalizationUtils.getMessages().get(key);
            msg = StringUtils.deleteWhitespace(msg); // is this valid?
            Number value = parseSize(size, msg);
            if (value != null) {
                return unit.getValue(value.doubleValue());
            }
        }
        Number value = parseSize(size, "{0,number}");
        return (value == null) ? null : value.longValue();
    }

    private static Number parseSize(String size, String msg) {
        MessageFormat fmt = new MessageFormat(msg);
        ParsePosition pp = new ParsePosition(0);
        Object value[] = (Object[]) fmt.parseObject(size, pp);
        if ((value == null) || (pp.getIndex() != size.length())) {
            return null;
        }
        return (Number) value[0];
    }

    /**
     * Parse a duration.
     *
     * @param duration the duration
     *
     * @return the parsed duration
     */
    public static Long parseDuration(String duration) {
        duration = StringUtils.deleteWhitespace(duration); // is this valid?
        if (StringUtils.isEmpty(duration)) {
            return null;
        }
        for (DateUtils.Unit unit : DateUtils.Unit.values()) {
            String key = "function_FormatFunction_formatDuration_" + unit;
            String msg = (String) InternationalizationUtils.getMessages().get(key);
            msg = StringUtils.deleteWhitespace(msg); // is this valid?
            Number value = parseDuration(duration, msg);
            if (value != null) {
                return unit.getValue(value.doubleValue());
            }
        }
        Number value = parseDuration(duration, "{0,number}");
        return (value == null) ? null : value.longValue();
    }

    private static Number parseDuration(String duration, String msg) {
        MessageFormat fmt = new MessageFormat(msg);
        ParsePosition pp = new ParsePosition(0);
        Object value[] = (Object[]) fmt.parseObject(duration, pp);
        if ((value == null) || (pp.getIndex() != duration.length())) {
            return null;
        }
        return (Number) value[0];
    }

    /**
     * This initializes the default internationalizations that ship with
     * the product. These are only used for rendering domain-less pages:
     * 1. The domain initialization page
     * 2. Errors that occurs before the current domain is set
     */
    public static VersionedMessageMapCompositor defaultMessages(TimeZone timeZone) {
        try {
            VersionedMessageMapCompositor messages = new VersionedMessageMapCompositor(0L);
            logger.log(Level.FINE, "Initializating default internationalization");
            Locale defaultLocale = Locale.getDefault();
            boolean isDefaultLocaleAvailable = false;

            for (String resource: ClassUtils.getResources(ClassUtils.class, "/languages/*.properties")) {
                Properties properties = ClassUtils.loadResourceAsProperties(ClassUtils.class, resource);
                String languageTag = StringUtils.substringBefore(StringUtils.substringAfterLast(resource, "/"), ".properties");
                Locale locale = Locale.forLanguageTag(languageTag);
                logger.log(Level.FINE, "Loaded messages, {0}", locale);
                messages.addMessageMap(new BaseMessageMap(locale, timeZone, properties));
                isDefaultLocaleAvailable |= locale.equals(defaultLocale);
            }

            // If the server's default domain is unavailable, use en-US
            if (!isDefaultLocaleAvailable) {
                defaultLocale = Locale.ENGLISH;
            }
            logger.log(Level.FINE, "Default locale, {0}", defaultLocale);
            messages.setDefaultLocale(defaultLocale);
            return messages;
        } catch (Exception ex) {
            throw new IllegalStateException("Messages error", ex);
        }
    }

    public static String getLocaleSuffix() {
        Long ver = (Long) BaseWebContext.getContext().getRequest().getAttribute("msgVer"); // MessageFilter.REQUEST_ATTRIBUTE_MSG_VER
        return ver + "-" + BaseWebContext.getContext().getResponse().getLocale().toLanguageTag();
    }

    public static String getTimezoneName() {
        TimeZone timeZone = getTimeZone();
        return timeZone.getDisplayName(timeZone.inDaylightTime(new Date()), TimeZone.SHORT, InternationalizationUtils.getLocale());
    }

    public static String getTimezoneOffset() {
        int offsetMs = InternationalizationUtils.getTimeZone().getOffset(new Date().getTime());
        int absOffsetMs = Math.abs(offsetMs);
        String offsetSign = "+";
        if (offsetMs < 0) {
            offsetSign = "-";
        }
        int offsetMinutes = absOffsetMs / (60 * 1000);
        int minutes = offsetMinutes % 60;
        int hours = offsetMinutes / 60;
        return String.format("%s%02d:%02d", offsetSign, hours, minutes);
    }
}
