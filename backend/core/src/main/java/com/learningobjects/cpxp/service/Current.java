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

package com.learningobjects.cpxp.service;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.learningobjects.cpxp.service.domain.DomainDTO;
import com.learningobjects.cpxp.service.item.Item;
import com.learningobjects.cpxp.service.query.QueryService;
import com.learningobjects.cpxp.service.user.UserDTO;
import com.learningobjects.cpxp.service.user.UserType;
import com.learningobjects.cpxp.util.CacheFactory;
import com.learningobjects.cpxp.util.GuidUtil;
import com.learningobjects.cpxp.util.ManagedUtils;
import com.learningobjects.cpxp.util.ObjectUtils;
import com.learningobjects.cpxp.util.logging.StackTraceUtils;

import java.time.Instant;
import java.util.*;
import java.util.logging.Logger;

public class Current {
    private DomainDTO  domainDTO;
    private UserDTO userDTO;
    private Date time;
    private Long sessionPk;
    private String sessionId;
    private Map map = new HashMap();
    private Map<Class<?>, Object> thc = new HashMap<>();
    private Map cache = new HashMap();
    private Set<String> polluted = new HashSet<>();

    private static final Logger logger = Logger.getLogger(Current.class.getName());

    private Current() {
    }

    private static ThreadLocal<Current> current = ThreadLocal.withInitial(Current::new);

    public static Current getInstance() {
        return current.get();
    }

    public static boolean isAnonymous() {
        UserDTO user = getUserDTO();
        return (user == null) || UserType.Anonymous.equals(user.getUserType());
    }

    public static boolean isRoot() {
        UserDTO user = getUserDTO();
        return (user != null) && UserType.Overlord.equals(user.getUserType());
    }

    public static void setSessionPk(Long sessionPk) {
        current.get().sessionPk = sessionPk;
    }

    public static Long getSessionPk() {
        return current.get().sessionPk;
    }

    public static void setSessionId(String sessionId) {
        current.get().sessionId = sessionId;
    }

    public static String getSessionId() {
        return current.get().sessionId;
    }

    public static void setTime(Date time) {
        current.get().time = time;
    }

    public static Date getTime() {
        return current.get().time;
    }

    public static Instant getInstant() {
        return Optional.ofNullable(getTime()).map(Date::toInstant).orElse(null);
    }

    public static void setDomainDTO(DomainDTO domainDTO) {
        current.get().domainDTO = domainDTO;
    }

    public static DomainDTO getDomainDTO() {
        return current.get().domainDTO;
    }

    public static Long getDomain() {
        DomainDTO dto = current.get().domainDTO;
        return (dto == null) ? null : dto.getId();
    }

    public static void setUserDTO(UserDTO userDTO) {
        current.get().userDTO = userDTO;
    }

    public static Long getUser() {
        UserDTO dto = current.get().userDTO;
        return (dto == null) ? null : dto.getId();
    }

    public static UserDTO getUserDTO() {
        return current.get().userDTO;
    }

    public static void put(Object key, Object value) {
        current.get().map.put(key, value);
    }

    public static void remove(Object key) {
        current.get().map.remove(key);
    }

    public static <T> T get(Object key) {
        return (T) current.get().map.get(key);
    }

    public static <T> void putTypeSafe(Class<T> typeToken, T value) {
        current.get().thc.put(typeToken, value);
    }

    public static <T> T getTypeSafe(Class<T> typeToken) {
        return typeToken.cast(current.get().thc.get(typeToken));
    }
    //TODO TECH-70 overload Current.getTypeSafe to take an additional parameter of default Supplier<T>, to make population on first usage easy

    //TODO TECH-70 get Query specific stuff out of Current
    public static void clearPolluted() {
        current.get().polluted.clear();
    }

    //TODO TECH-70 get Query specific stuff out of Current
    public static void setPolluted(Item item) {
        Set<String> polluted = current.get().polluted;
        String rootKey = item.getRoot().getId() + "//" + item.getType();
        String parentKey = item.getParent().getId() + "/" + item.getType();
        polluted.add(rootKey);
        polluted.add(parentKey);
        ManagedUtils.getService(QueryService.class).invalidateQuery(parentKey);
    }

    //TODO TECH-70 get Query specific stuff out of Current
    public static boolean isPolluted(Item root, Item parent, String type) {
        if (parent != null) {
            return current.get().polluted.contains(parent.getId() + "/" + type);
        } else if (root != null) {
            return current.get().polluted.contains(root.getId() + "//" + type);
        } else {
            return false;
        }
    }

    private static final Object IN_PROGRESS = new Object();

    public static <T> T get(CacheFactory<T> factory) {
        Object key = factory.getKey();
        Current c = current.get();
        Object value = (key == null) ? null : c.cache.get(key);
        if (value == null) {
            c.cache.put(key, IN_PROGRESS);
            value = ObjectUtils.defaultIfNull(factory.create(), ObjectUtils.NULL);
            c.cache.put(key, value);
        } else if (value == IN_PROGRESS) {
            throw new RuntimeException("Cache fill in progress: " + key);
        }
        return (value == ObjectUtils.NULL) ? null : (T) value;
    }

    public static void clearCache() {
        Current c = current.get();
        c.thc.clear();
        c.cache.clear();
    }

    public static void clear() {
        Current c = current.get();
        c.time = null;
        c.domainDTO = null;
        c.userDTO = null;
        c.map.clear();
        c.thc.clear();
        c.cache.clear();
        c.polluted.clear();
    }

    private static final Object DELETE_GUID = "DELETE_GUID";

    public static String deleteGuid() {
        String guid = get(DELETE_GUID);
        if (guid == null) {
            guid = GuidUtil.deleteGuid(Current.getTime(), Current.getUser());
            put(DELETE_GUID, guid);
            logger.info("Creating Delete GUID: " + guid);// + " at " + StackTraceUtils.shortStackTrace());
        }
        return guid;
    }
}
