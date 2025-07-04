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

package com.learningobjects.cpxp.service.trash;

import com.google.common.collect.Multimap;
import com.learningobjects.cpxp.dto.DataTransfer;
import com.learningobjects.cpxp.service.BasicServiceBean;
import com.learningobjects.cpxp.service.Current;
import com.learningobjects.cpxp.service.data.DataService;
import com.learningobjects.cpxp.service.data.DataTypes;
import com.learningobjects.cpxp.service.facade.FacadeService;
import com.learningobjects.cpxp.service.item.Item;
import com.learningobjects.cpxp.service.name.NameService;
import com.learningobjects.cpxp.service.query.Direction;
import com.learningobjects.cpxp.service.query.Projection;
import com.learningobjects.cpxp.service.query.QueryBuilder;
import com.learningobjects.cpxp.util.CacheFactory;
import com.learningobjects.cpxp.util.DateUtils;
import com.learningobjects.cpxp.util.StringUtils;
import com.typesafe.config.Config;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import jakarta.persistence.Query;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class TrashServiceBean extends BasicServiceBean implements TrashService {
    private static final Logger logger = Logger.getLogger(TrashServiceBean.class.getName());

    /** The data service. */
    @Inject
    private DataService _dataService;

    /** The facade service. */
    @Inject
    protected FacadeService _facadeService;

    /** The name service. */
    @Inject
    protected NameService _nameService;

    @Inject
    private Config config;

    private String getTrashId() {
        return Current.get(new TrashIdFactory());
    }

    @Override
    public String begin(Long id) {

        Item item = _itemService.get(id);
        String trashId = getTrashId();

        TrashRecordParentFacade parentFacade = _facadeService.getFacade(item, TrashRecordParentFacade.class);
        TrashRecordFacade trashFacade = parentFacade.addTrashRecord();
        trashFacade.setCreated(getCurrentTime());
        trashFacade.setCreator(getCurrentUser().getId());
        trashFacade.setTrashId(trashId);

        return trashId;
    }

    @Override
    public String trash(Long id) {

        Item item = _itemService.get(id);
        String trashId = begin(item.getParent().getId());
        _itemService.setDeleted(item, trashId);

        return trashId;
    }

    @Override
    public void trashMore(Long id) {

        Item item = _itemService.get(id);
        String trashId = getTrashId();
        _itemService.setDeleted(item, trashId);

    }

    private static final String DELETED_APEX_SQL = "SELECT i.id FROM Item i WHERE i.deleted = :trashId AND i.parent.deleted IS NULL";

    @Override
    public void restore(String trashId) {

        QueryBuilder qb = querySystem(TrashConstants.ITEM_TYPE_TRASH_RECORD);
        qb.addCondition(TrashConstants.DATA_TYPE_TRASH_ID, "eq", trashId);
        Item record = com.google.common.collect.Iterables.getFirst(qb.getItems(), null);
        _itemService.destroy(record);

        logger.log(Level.INFO, "Restoring " + record);

        Query query = createQuery(DELETED_APEX_SQL);
        query.setParameter("trashId", trashId);

        List<Long> apexes = query.getResultList();
        for (Long id : apexes) {
            Item item = getItem(id);

            fixUrlInTree(item);
        }

        getEntityManager().flush();
        getEntityManager().clear();

        _itemService.clearDeleted(trashId);

    }

    private Item getItem(Long id) { // ignores deleted
        Item item = getEntityManager().find(Item.class, id);
        _itemService.loadFinder(item);
        return item;
    }

    private void fixUrlInTree(Item item) {

        String path = DataTransfer.getStringData(item, DataTypes.DATA_TYPE_URL);
        if (!StringUtils.isEmpty(path) && (null != _nameService.getItem(item.getRoot(), path))) {
            String newPath = getNewPath(path, item);
            // TODO: switch to bulk url update when we don't need to rewrite bodies
            rewriteUrlInTextData(item, path, newPath);
            rewriteUrlInUrlData(item, path, newPath);
            rewriteUrlInDescendantData(item, path, newPath, _itemService.findLeafParents(item));
        }

    }

    private int rewriteUrlInDescendantData(Item item, String oldPath, String newPath, Multimap<Long, String> leafParents) {
        int modificationCount = 0;
        for (Item childItem : getAllChildren(item, leafParents, true)) {
            refresh(childItem);
            rewriteUrlInUrlData(childItem, oldPath, newPath);
            rewriteUrlInTextData(childItem, oldPath, newPath);
            modificationCount += 1 + rewriteUrlInDescendantData(childItem, oldPath, newPath, leafParents);
            if (modificationCount >= 1024) {
                // TODO: FIXME: This will break the restore codepath getEntityManager().flush();
                // TODO: FIXME: This will break the restore codepath getEntityManager().clear();
                modificationCount = 0;
            }
        }
        return modificationCount;
    }

    private String getNewPath(String oldPath, Item restoredItem) {
        // can't use getPath(item.getParent()) path because some things (like blogs) use custom parent paths
        String parentPath = StringUtils.substringBeforeLast(oldPath, "/");
        String oldSuffix = StringUtils.substringAfterLast(oldPath, "/");
        String name = DataTransfer.getStringData(restoredItem, DataTypes.DATA_TYPE_NAME);
        String pattern = getBindingPattern(parentPath, name, oldSuffix);

        return _nameService.getBindingPattern(restoredItem, pattern);
    }

    private void rewriteUrlInTextData(Item item, String oldURLPrefix, String newURLPrefix) {
        String text = DataTransfer.getTextData(item, DataTypes.DATA_TYPE_BODY);
        if (text != null) {
            text = text.replaceAll(oldURLPrefix, newURLPrefix); // Ugh
            _dataService.setText(item, DataTypes.DATA_TYPE_BODY, text);
        }
    }

    private void rewriteUrlInUrlData(Item item, String oldURLPrefix, String newURLPrefix) {
        String url = DataTransfer.getStringData(item, DataTypes.DATA_TYPE_URL);
        if ((url != null) && url.startsWith(oldURLPrefix)) {
            url = newURLPrefix + url.substring(oldURLPrefix.length());
            _dataService.setString(item, DataTypes.DATA_TYPE_URL, url);
        }
    }

    @Override
    public List<Item> getTrashRecords() {

        QueryBuilder qb = querySystem(TrashConstants.ITEM_TYPE_TRASH_RECORD);
        qb.setIncludeDeleted(true);
        qb.setOrder(TrashConstants.DATA_TYPE_CREATED, Direction.DESC);
        qb.setLimit(32);

        return qb.getItems();
    }

    public void cleanUpTrash(long age) {

        Date date = new Date(new Date ().getTime() - age);
        QueryBuilder qb = querySystem(TrashConstants.ITEM_TYPE_TRASH_RECORD);
        qb.setIncludeDeleted(true);
        qb.addCondition(TrashConstants.DATA_TYPE_CREATED, "le", date);
        qb.setOrder(TrashConstants.DATA_TYPE_CREATED, Direction.DESC);
        qb.setProjection(Projection.ID);
        List<Long> results = qb.getResultList();

        logger.log(Level.INFO, "Trash cleanup of {0} records", results.size());
        int count = 0;
        for (Long id: results) {
            Item item = getEntityManager().find(Item.class, id);
            if (item != null) {
                String trashId = DataTransfer.getStringData(item,TrashConstants.DATA_TYPE_TRASH_ID);
                if (trashId != null) {
                    // Find all the apex nodes assocated with this trash id
                    Query query = createQuery(DELETED_APEX_SQL);
                    query.setParameter("trashId", trashId);
                    List<Long> items = query.getResultList();
                    for (Long tbd : items) {
                        _itemService.destroy(_itemService.get(tbd));
                        ++ count;
                    }
                }
                _itemService.destroy(item);
            }
        }
        logger.log(Level.INFO, "Trash cleanup complete, {0}", count);

    }

    // @Scheduled(value = "1 hour", singleton = true)
    public void cleanUpTrash() {

        String purgeTimeout = config.getString("com.learningobjects.cpxp.trash.purgeAge");
        cleanUpTrash(DateUtils.parseDuration(purgeTimeout));

    }

    private static class TrashIdFactory extends CacheFactory<String> {
        public String getKey() {
            return "trashId";
        }

        public String create() {
            return Current.deleteGuid();
        }
    }
}
