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

import com.learningobjects.cpxp.BaseServiceMeta;
import com.learningobjects.cpxp.BaseWebContext;
import com.learningobjects.cpxp.component.ComponentSupport;
import com.learningobjects.cpxp.component.util.ComponentUtils;
import com.learningobjects.cpxp.dto.Facade;
import com.learningobjects.cpxp.service.Current;
import com.learningobjects.cpxp.service.ServiceContext;
import com.learningobjects.cpxp.service.attachment.AttachmentConstants;
import com.learningobjects.cpxp.service.attachment.ImageFacade;
import com.learningobjects.cpxp.service.aws.AwsService;
import com.learningobjects.cpxp.service.data.DataSupport;
import com.learningobjects.cpxp.service.group.GroupConstants;
import com.learningobjects.cpxp.service.group.GroupConstants.GroupType;
import com.learningobjects.cpxp.service.group.GroupFacade;
import com.learningobjects.cpxp.service.mime.MimeWebService;
import com.learningobjects.cpxp.service.name.UrlFacade;
import com.learningobjects.cpxp.service.portal.NameFacade;
import com.learningobjects.cpxp.service.relationship.RoleFacade;
import com.learningobjects.cpxp.service.user.UserConstants;
import com.learningobjects.cpxp.service.user.UserDTO;
import com.learningobjects.cpxp.service.user.UserFacade;
import com.learningobjects.cpxp.util.message.MessageMap;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * JSP utility functions for formatting things.
 */
public class FormattingUtils {
    public static <T> Map<T, Boolean> mapify(final Set<T> set) {
        return new AbstractMap<T, Boolean>() {
            @Override
            public Boolean get(Object key) {
                return set.contains(key);
            }

            @Override
            public Set<Entry<T, Boolean>> entrySet() {
                return null;
            }
        };
    }

    public static Long max(Long a, Long b) {
        return ((a == null) || ((b != null) && (b > a))) ? b : a;
    }

    public static Long min(Long a, Long b) {
        return ((a == null) || ((b != null) && (b < a))) ? b : a;
    }

    public static Boolean isBlank(String s) {
        return StringUtils.isBlank(s);
    }

    public static Object first(Collection c) {
        return c.isEmpty() ? null : c.iterator().next();
    }

    public static Boolean isFirst(Object o, Collection c) {
        return !c.isEmpty() && ObjectUtils.equals(o, c.iterator().next());
    }

    public static Boolean isMinimal(Date date) {
        return DataSupport.isMinimal(date);
    }

    public static Boolean isMaximal(Date date) {
        return DataSupport.isMaximal(date);
    }

    public static Boolean isContained(Object o, Collection c) {
        return c.contains(o);
    }

    public static Boolean imageFits(ImageFacade image, Integer width, Integer height) {
        return (image != null) && (image.getHeight() != null) &&
          (image.getWidth().intValue() <= width) &&
          (image.getHeight().intValue() <= height);
    }

    public static String[] rootCauseStackTrace(Throwable th) {
        return ExceptionUtils.getRootCauseStackTrace(th);
    }

    /**
     * Encode a static URL as /static/&lt;version&gt;/&lt;href&gt;.
     *
     * @param href the href
     * @return the URL
     */
    public static String staticUrl(String href) {
        return staticUrl(false, false, true, href);
    }

    public static String localizedUrl(String href) {
        return staticUrl(true, false, true, href);
    }

    /**
     * Encode an image URL as /static/&lt;version&gt;/images/&lt;image&gt;.
     *
     * @param image the image
     * @return the image URL
     */
    public static String imageUrl(String themeUrl, String image) { // TODO: KILL ME, use staticUrl
        if (StringUtils.isEmpty(themeUrl)) {
            return staticUrl("images/" + image);
        }
        return themeUrl + "/images/" + image;
    }

    /**
     * Returns the first non-empty string.
     *
     * @param string1 the first string
     * @param string2 the second string
     * @return the first non-empty string
     */
    public static String or2(String string1, String string2) {
        return StringUtils.isEmpty(string1) ? string2 : string1;
    }

    /**
     * Format a message key with one parameter.
     *
     * @param key   the message key
     * @param param the parameter
     * @return the message key with the first * replaced by the parameter
     */
    public static String msgkey1(String key, Object param) {
        return key.replace("*", String.valueOf(param));
    }

    /**
     * Format a string with one parameter.
     *
     * @param fmt   the message format
     * @param param the parameter
     * @return the formatted message
     */
    public static String format1(String fmt, Object param) {
        return InternationalizationUtils.format(fmt, param);
    }

    /**
     * Format a string with two parameters.
     *
     * @param fmt    the message format
     * @param param1 the first parameter
     * @param param2 the second parameter
     * @return the formatted message
     */
    public static String format2(String fmt, Object param1, Object param2) {
        return InternationalizationUtils.format(fmt, param1, param2);
    }

    /**
     * Format a string with three parameters.
     *
     * @param fmt    the message format
     * @param param1 the first parameter
     * @param param2 the second parameter
     * @param param3 the third parameter
     * @return the formatted message
     */
    public static String format3(String fmt, Object param1, Object param2, Object param3) {
        return InternationalizationUtils.format(fmt, param1, param2, param3);
    }

    /**
     * Format a string with four parameters.
     *
     * @param fmt    the message format
     * @param param1 the first parameter
     * @param param2 the second parameter
     * @param param3 the third parameter
     * @param param4 the fourth parameter
     * @return the formatted message
     */
    public static String format4(String fmt, Object param1, Object param2, Object param3, Object param4) {
        return InternationalizationUtils.format(fmt, param1, param2, param3, param4);
    }

    /**
     * Format a time in standard interop format.
     *
     * @param time the time
     * @return the formatted message
     */
    public static String formatTime(Date time) {
        if (isMaximal(time) || isMinimal(time)) {
            return "";
        } else {
            SimpleDateFormat fmt = new SimpleDateFormat(InternationalizationUtils.formatMessage("dateFormat_java"));
            fmt.setTimeZone(InternationalizationUtils.getTimeZone());
            return fmt.format(time);
        }
    }

    public static Date parseTime(String time) {
        if (!StringUtils.isEmpty(time)) {
            try {
                SimpleDateFormat fmt = new SimpleDateFormat(InternationalizationUtils.formatMessage("dateFormat_java"));
                fmt.setTimeZone(InternationalizationUtils.getTimeZone());
                return fmt.parse(time);
            } catch (Exception ex) {
                throw new RuntimeException("Date parse error: " + ex);
            }
        }
        return null;
    }

    /**
     * Format a size nicely.
     *
     * @param size the size
     * @return the formatted size
     */
    public static String formatSize(Long size) {
        return InternationalizationUtils.formatSize(size);
    }

    /**
     * Format a rate nicely.
     *
     * @param rate the byte/s rate
     * @return the formatted rate
     */
    public static String formatRate(Long rate) {
        return InternationalizationUtils.formatRate(rate);
    }

    public static String formatSizeRatio(Long numerator, Long denominator) {
        if ((numerator == null) || (denominator == null)) {
            return "";
        }
        NumberUtils.Unit unit = NumberUtils.getDataSizeUnit(denominator, 0);
        long denominatorScaled = (long) NumberUtils.divide(denominator, unit.getValue(), 0);
        long numeratorScaled = (long) NumberUtils.divide(numerator, unit.getValue(), 0);
        return numeratorScaled + " / " + InternationalizationUtils.formatMessage("function_FormatFunction_formatSize_" + unit, denominatorScaled);
    }

    /**
     * Format a relative date.
     *
     * @param date the date
     * @return the relative date.
     */
    public static String formatRelativeDate(Date date) {
        if (date == null) {
            return "";
        }
        Date now = Current.getTime();
        if (now == null) {
            now = new Date();
        }
        DateUtils.Delta delta = DateUtils.getDelta(date, now);
        // it just doesn't feel right to round dates up. 11 months is 11 months, not 1 year.
        double roundedScaled = Math.floor(delta.getScaled());
        String fmt = InternationalizationUtils.formatMessage("function_FormatFunction_formatDuration_" + delta.getUnit(), roundedScaled, roundedScaled);
        boolean ago = delta.isNegative() || (delta.getScaled() == 0.0);
        String relative = InternationalizationUtils.formatMessage("function_FormatFunction_formatDuration_" + (ago ? "ago" : "in"), fmt);
        if (delta.getUnit().compareTo(DateUtils.Unit.week) >= 0) {
            relative = relative + InternationalizationUtils.formatMessage("function_FormatFunction_formatDuration_dateSuffix", date);
        }
        return relative;
    }

    /**
     * Format a relative date after adding an offset in days.
     *
     * @param date the date
     * @param days offset in days from the relative date
     * @return the relative date.
     */
    public static String formatRelativeDateWithOffset(Date date, Integer days) {
        Calendar adjusted = Calendar.getInstance();
        adjusted.setTime(date);
        adjusted.add(Calendar.DATE, days);
        return formatRelativeDate(adjusted.getTime());
    }

    /**
     * Format a duration.
     */
    public static String formatDuration(Long ms) {
        return InternationalizationUtils.formatDuration(ms);
    }

    /**
     * Format an object in JSON.
     *
     * @param object the object
     * @return the formatted string
     */
    public static String json(Object object) throws IOException {
        return voidTags(JsonUtils.toJson(object));
    }

    /**
     * Escapes less-than characters in the passed {@code value} so that the
     * literal is unchanged, but can be safely printed within other markup.
     */
    public static String voidTags(final String value) {
        if (null == value) {
            return null;
        }
        return value.replaceAll("<", "\\\\x3c").replaceAll(">", "\\\\x3e");
    }

    /**
     * Format an object in JSON, escaping double quotes.
     *
     * @param object the object
     * @return the formatted string
     */
    public static String jsonAttr(Object object) throws IOException {
        return JsonUtils.toJson(object).replaceAll("\"", "&#34;");
    }

    /**
     * Format an attribute value.
     *
     * @param value the value
     * @return the formatted string
     */
    public static String attr(String value) {
        return StringEscapeUtils.escapeHtml4(value);
    }

    /**
     * Truncate a string.
     *
     * @param value the value
     * @return the truncated string, le 24 chars
     */
    public static String trunc(String value) {
        return trunc(value, 24);
    }

    public static String trunc(String value, int n) {
        return StringUtils.abbreviate(value, n);
    }

    public static String contentName(Facade facade) {
        return contentName2(facade, InternationalizationUtils.getMessages());
    }

    public static String contentName2(Facade facade, MessageMap messages) {
        if (facade == null) {
            return null;
        } else if (UserConstants.ITEM_TYPE_USER.equals(facade.getItemType())) {
            return userStr(facade);
        } else if (GroupConstants.ITEM_TYPE_GROUP.equals(facade.getItemType())) {
            return groupStr(facade);
        } else if (facade instanceof NameFacade) {
            NameFacade nf = (NameFacade) facade;
            if (AttachmentConstants.ITEM_TYPE_ATTACHMENT.equals(nf.getItemType())) {
                return nf.getFileName();
            } else {
                String name = nf.getName();
                if (StringUtils.isEmpty(name)) {
                    String msg = nf.getMsg();
                    if (!StringUtils.isEmpty(msg)) {
                        name = InternationalizationUtils.formatMessage(messages, (msg + ".name"), new Object[]{});
                    } else {
                        name = InternationalizationUtils.formatMessage(messages, "function_FormatFunction_contentName_untitled", new Object[]{});
                    }
                }
                return name;
            }
        } else {
            return null;
        }
    }

    public static Boolean isValidIFrameUrl(String url) {
        try {
            URI uri = new URI(url);
            String host = uri.getHost();
            return !StringUtils.isEmpty(host) && !StringUtils.equalsIgnoreCase(host, getRequest().getServerName());
        } catch (Exception ex) {
            return false;
        }
    }

    private static boolean equalsIgnoreCaseAndWhitespace(String a, String b) {
        return StringUtils.equalsIgnoreCase(StringUtils.deleteWhitespace(a), StringUtils.deleteWhitespace(b));
    }

    public static String groupStr(Facade facade) {
        String name, groupId;
        if (facade instanceof GroupFacade) {
            GroupFacade group = (GroupFacade) facade;
            name = group.getName();
            groupId = group.getGroupId();
            final GroupType groupType = group.getGroupType();
            if (GroupType.USER_CREATED.equals(groupType) || GroupType.GLOBAL_COURSE.equals(groupType)) {
                return name;
            }
        } else {
            return InternationalizationUtils.formatMessage("group_unknownGroup");
        }
        boolean hasGroupId = !StringUtils.isEmpty(groupId) && !groupId.startsWith("URN:") && !equalsIgnoreCaseAndWhitespace(groupId, name); // TODO:ARSE: This is in order to not show webct GUIDs or group ids that =~ group names
        if (hasGroupId) { // Mathematics 101 (Math101/2008/1)
            StringBuilder sb = new StringBuilder();
            sb.append(name);
            sb.append(" (");
            if (hasGroupId) {
                sb.append(groupId);
            }
            sb.append(")");
            name = sb.toString();
        }
        return name;
    }

    public static String userThumbnail(UserFacade user) {
        return userImage(user, 48);
    }

    public static String userImage(UserFacade user, Number size) {
        UrlFacade image = (user == null) ? null : user.getImage();
        String url = (image == null) ? null : image.getUrl();
        String suffix = (size.intValue() <= 48) ? "Thumbnail" : "Image";
        return (url != null) ? url + "!" + size
          : staticUrl("images/user" + suffix + ".png"); // TODO: More sizes
    }

    public static String groupThumbnail(GroupFacade group) {
        return groupImage(group, 48);
    }

    public static String groupImage(GroupFacade group, Number size) {
        // TODO: courseThumbnail support
        UrlFacade image = (group == null) ? null : group.getImage();
        String url = (image == null) ? null : image.getUrl();
        String suffix = (size.intValue() <= 48) ? "Thumbnail" : "Image";
        return (url != null) ? url + "!" + size
          : staticUrl("images/group" + suffix + ".png"); // TODO: More sizes
    }

    /**
     * Display a user by a given referencing policy.
     *
     * @param facade
     * @return
     */
    public static String userStr(Facade facade) {
        String givenName, familyName, middleName, userName;
        if (facade instanceof UserFacade) {
            UserFacade user = (UserFacade) facade;
            givenName = user.getGivenName();
            familyName = user.getFamilyName();
            middleName = user.getMiddleName();
            userName = user.getUserName();
            return userStr(userName, givenName, middleName, familyName);
        } else {
            return InternationalizationUtils.formatMessage("user_unknownUser");
        }
    }

    public static String userStr(UserDTO user) {
        return userStr(user.userName(), user.givenName(), user.middleName(), user.familyName());
    }

    public static String userStr(String userName, String givenName, String middleName, String familyName) {
        if (StringUtils.isEmpty(givenName) && StringUtils.isEmpty(familyName)) {
            return StringUtils.isEmpty(userName) ? "Unknown" : userName; // important for invitees
        } else if (StringUtils.isEmpty(givenName)) {
            return familyName;
        } else if (StringUtils.isEmpty(familyName)) {
            return givenName; // archetypes
        } else if (StringUtils.isEmpty(middleName)) {
            // FIXME: use something like UserUrlFormat with tokens specified by the language pack.
            if ("zh".equals(InternationalizationUtils.getLocale().getLanguage())) {
                return familyName + givenName;
            } else {
                return givenName + " " + familyName;
            }
        } else {
            return givenName + " " + middleName + " " + familyName;
        }
    }

    public static String userStr(String givenName, String middleName, String familyName) {
        return userStr("", givenName, middleName, familyName);
    }

    public static String roleStr(Facade role) {
        if (role == null) {
            return InternationalizationUtils.formatMessage("role_unknownRole");
        } else {
            return contentName(role);
        }
    }

    public static String allRoleStr(RoleFacade role) {
        if (role == null) {
            return InternationalizationUtils.formatMessage("role_unknownRole");
        } else {
            String all = InternationalizationUtils.getMessages().getMessage("role_allRole/" + role.getRoleId());
            if (all == null) {
                all = InternationalizationUtils.formatMessage("role_allRole", contentName(role));
            }
            return all;
        }
    }

    /**
     * @param facade the item to be formatted
     * @param format a string of tokens and/or plain text used to generate the
     *               final string. A token consists of a field from the item and
     *               any number of modifiers on that field, surrounded by curly
     *               braces (e.g. {givenName:initial:upperCase}).
     * @return the formatted name
     */
    public static String formatName(Facade facade, String format) {
        StringBuilder result = new StringBuilder();

        try {
            Class<? extends Facade> clas = facade.getClass();

            for (String token : StringUtils.split(format, "{}")) {
                String[] arguments = token.split(":");
                if (arguments.length < 1) {
                    throw new IllegalArgumentException("Invalid formatting token: " + token);
                }

                String field = StringUtils.capitalize(arguments[0]);
                String value = null;

                // check the item for the field
                try {
                    Method method = clas.getMethod("get" + field);
                    value = (String) method.invoke(facade);
                } catch (NoSuchMethodException ignored) {
                    // fall back to the field name in case it wasn't really a field
                    value = field;
                }

                if (value != null) {
                    for (int i = 1; i < arguments.length; i++) {
                        // apply each of the modifiers in the order they were declared
                        StringModifier modifier = StringModifier.valueOf(StringUtils.upperCase(arguments[i]));
                        value = modifier.applyTo(value);
                    }
                    result.append(value);
                }
            }
        } catch (Exception ex) {
            throw new IllegalArgumentException("Failed to parse or apply format string", ex);
        }

        return result.toString();
    }

    public static String defaultString(String one, String two) {
        return StringUtils.isEmpty(one) ? two : one;
    }

    public static HttpServletRequest getRequest() {
        return BaseWebContext.getContext().getRequest();
    }

    public static Object getRequestAttribute(String key) {
        return getRequest().getAttribute(key);
    }

    public static Object getSessionAttribute(String key) {
        HttpSession session = getRequest().getSession(false);
        if (session == null) {
            return null;
        } else {
            return session.getAttribute(key);
        }
    }

    public static String httpsUrl(String path) {
        return HttpUtils.getHttpsUrl(getRequest(), path);
    }

    public static String httpUrl(String path) {
        return HttpUtils.getHttpUrl(getRequest(), path);
    }

    public static String gzStaticUrl(String path) {
        return staticUrl(false, true, true, path);
    }

    public static String gzLocalizedUrl(String path) {
        return staticUrl(true, true, true, path);
    }

    public static String localLocalizedUrl(String path) {
        return staticUrl(true, false, false, path);
    }

    public static StringBuilder staticPrefix(boolean cdn) {
        HttpServletRequest request = getRequest();
        String host = (request == null) ? null : StringUtils.lowerCase(request.getServerName());
        StringBuilder sb = new StringBuilder();
        String staticHost = BaseServiceMeta.getServiceMeta().getStaticHost();
        final AwsService aws = ComponentSupport.lookupService(AwsService.class);
        final boolean cdnDisabled = (aws != null) && aws.cfDisabled();
        if (cdnDisabled || !cdn || (staticHost == null) || (Current.getDomain() == null)
          || host == null || !host.equals(Current.getDomainDTO().getHostName())) {
            sb.append("/static/");
        } else {
            String suffix = BaseServiceMeta.getServiceMeta().getStaticSuffix();
            if (host.endsWith(suffix)) {
                host = host.substring(0, host.length() - suffix.length());
            }
            sb.append(request.isSecure() ? "https://" : "http://").append(staticHost).append("/static/cdn/").append(host).append('/');
        }
        return sb;
    }

    private static String staticUrl(boolean localize, boolean gzip, boolean cdn, String path) {
        // TODO: much, much better.. probably a cached init param?
        StringBuilder sb = staticPrefix(cdn);
        sb.append(CdnUtils.cdnSuffix(BaseServiceMeta.getServiceMeta(), ManagedUtils.getEntityContext()));
        if (!gzip) {
            gzip |= path.endsWith(".js") || path.endsWith(".css");
        }
        if (gzip && HttpUtils.supportsCompression(getRequest())) {
            sb.append("-gz");
        }
        if (!localize) {
            localize |= path.endsWith(".js"); // hmm.. i normal skip ext..
        }
        if (localize) {
            sb.append('-');
            sb.append(InternationalizationUtils.getLocaleSuffix());
        }
        sb.append('/');
        sb.append(path);
        return sb.toString();
    }

    /**
     * Produces the fully-qualified URL for the given path.
     */
    public static String fqUrl(String path) {
        HttpServletRequest request = getRequest();
        return (path.indexOf(':') > 0) ? path : HttpUtils.getUrl(request, path, request.isSecure());
    }

    public static Boolean not(final Object value) {
        return !ComponentUtils.test(value);
    }

    public static String mimeType(ImageFacade attachment) {
        return mimeType((attachment == null) ? null : attachment.getFileName());
    }

    public static String mimeType(String fileName) {
        MimeWebService mimeWebService = ServiceContext.getContext().getService(MimeWebService.class);
        return StringUtils.defaultString(mimeWebService.getMimeType(fileName), MimeUtils.MIME_TYPE_APPLICATION_UNKNOWN);
    }

    // supports parsing and application of certain string modification commands
    private static enum StringModifier {
        INITIAL,
        LOWERCASE,
        UPPERCASE;

        public String applyTo(String string) {
            switch (StringModifier.values()[this.ordinal()]) {
                case INITIAL:
                    return string.substring(0, 1);
                case LOWERCASE:
                    return StringUtils.lowerCase(string);
                case UPPERCASE:
                    return StringUtils.upperCase(string);
                default:
                    return string;
            }
        }
    }
}
