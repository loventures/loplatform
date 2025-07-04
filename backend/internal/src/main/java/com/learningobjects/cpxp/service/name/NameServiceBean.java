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

package com.learningobjects.cpxp.service.name;

import com.learningobjects.cpxp.dto.DataTransfer;
import com.learningobjects.cpxp.service.BasicServiceBean;
import com.learningobjects.cpxp.service.data.DataService;
import com.learningobjects.cpxp.service.item.Item;
import com.learningobjects.cpxp.service.query.Comparison;
import com.learningobjects.cpxp.service.query.Function;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The name service. Provides facilities for binding items to paths.
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class NameServiceBean extends BasicServiceBean implements NameService {
    private static final Logger logger = Logger.getLogger(NameServiceBean.class.getName());

    /** The data service. */
    @Inject
    private DataService _dataService;

    /**
     * Look up the Item at the specified absolute path.
     */
    public Item getItem(String path) {

        Item item = getItem(getCurrentDomain(), path);

        return item;
    }

    public Item getItem(Item domain, String path) {
        return queryRoot(domain, null)
          .setCacheQuery(true)
          .setCacheNothing(false)
          .addCondition(DATA_TYPE_PATH, Comparison.eq, path, Function.LOWER)
          .getResult();
    }

    public Long getItemId(String path) {

        Long id = getId(getItem(path));

        return id;
    }

    public Long getItemId(Long domainId, String path) {

        Item domain = _itemService.get(domainId);

        Long id = getId(getItem(domain, path));

        return id;
    }

    /**
     * Get the path binding of an item.
     */
    public String getPath(Item item) {

        String path = DataTransfer.getStringData(item, DATA_TYPE_PATH);

        return path;
    }

    /**
     * Get the path binding of an item.
     */
    public String getPath(Long id) {

        Item item = _itemService.get(id);
        String path = DataTransfer.getStringData(item, DATA_TYPE_PATH);

        return path;
    }

    /**
     * Set the binding of an item. Replaces any existing binding.
     */
    public void setBinding(Item item, String path) {

        Item current = getItem(path);
        if ((current != null) && !current.equals(item)) {
            throw new RuntimeException("Path binding already used: " + path + " on " + item + " / " + current);
        }

        _dataService.setString(item, DATA_TYPE_PATH, path);
    }

    public void setBinding(Long itemId, String path) {

        setBinding(_itemService.get(itemId), path);

    }

    /** The pattern pattern. */
    private static final Pattern PATTERN_PATTERN = Pattern.compile("([^\\$%]*)\\$([^%]*)%([^\\$]*)\\$([^\\$%]*)");
    private static final Pattern PATTERN2_PATTERN = Pattern.compile("([^\\$%]*)%([^\\$%]*)");

    /**
     * Set the binding of an item based on a pattern.
     */
    public String setBindingPattern(Item item, String pattern) {

        String path = getBindingPattern(item, pattern);
        _dataService.setString(item, DATA_TYPE_PATH, path);

        return path;
    }

    public String setBindingPattern(Long itemId, String pattern) {
        Item item = _itemService.get(itemId);
        String path = setBindingPattern(item, pattern);

        return path;
    }

    public String getBindingPattern(Long itemId, String pattern) {
        Item item = _itemService.get(itemId);
        return getBindingPattern(item,pattern);
    }

    public String getBindingPattern(Item item, String pattern) {
        String prefix, suffix, tokpre, toksuf;
        boolean cond;
        Matcher matcher;
        if ((matcher = PATTERN_PATTERN.matcher(pattern)).matches()) {
            prefix = matcher.group(1);
            tokpre = matcher.group(2);
            toksuf = matcher.group(3);
            suffix = matcher.group(4);
            cond = true;
        } else if ((matcher = PATTERN2_PATTERN.matcher(pattern)).matches()) {
            prefix = matcher.group(1);
            tokpre = toksuf = "";
            suffix = matcher.group(2);
            cond = false;
        } else {
            throw new IllegalArgumentException("Invalid path pattern: " + pattern);
        }

        int count = 0;

        // If the item already has a matching binding then move to the next
        // index up, so we don't re-use the path. This is intended to prevent
        // updated items from re-using the same path as before. UG-1373.
        String old = DataTransfer.getStringData(item, DATA_TYPE_PATH);
        String start = prefix + tokpre, end = toksuf + suffix;
        if (old != null) {
            if (old.equals(prefix + suffix)) { // no count
                cond = false; // no count so force to use count0
                logger.log(Level.FINE, "Forcing path binding to index, {0}, {1}", new Object[]{old, count});
            } else if (old.startsWith(start) && old.endsWith(end)) {
                try {
                    cond = false;
                    count = 1 + Integer.parseInt(old.substring(start.length(), old.length() - end.length()));
                    logger.log(Level.FINE, "Forcing path binding to index, {0}, {1}", new Object[]{old, count});
                } catch (Exception ignored) {
                }
            }
        }
        String path;
        if (cond) {
            path = prefix + suffix;
        } else {
            path = start + count + end;
            ++ count;
        }

        Item match;
        while (((match = getItem(item.getRoot(), path)) != null) && (match != item)) {
            path = start + count + end;
            ++ count;
        }
        return path;
    }
}
