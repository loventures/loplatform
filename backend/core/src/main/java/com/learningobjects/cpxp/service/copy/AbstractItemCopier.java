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

package com.learningobjects.cpxp.service.copy;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import com.learningobjects.cpxp.dto.BaseOntology;
import com.learningobjects.cpxp.dto.DataTransfer;
import com.learningobjects.cpxp.dto.EntityDescriptor;
import com.learningobjects.cpxp.dto.Ontology;
import com.learningobjects.cpxp.service.ServiceContext;
import com.learningobjects.cpxp.service.data.Data;
import com.learningobjects.cpxp.service.data.DataFormat;
import com.learningobjects.cpxp.service.data.DataTypes;
import com.learningobjects.cpxp.service.item.Item;
import com.learningobjects.cpxp.service.query.QueryBuilder;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The guts of item copying.
 */
public abstract class AbstractItemCopier  implements ItemCopier {
    private static final Logger logger = Logger.getLogger(AbstractItemCopier.class.getName());

    protected final ServiceContext _serviceContext;
    protected Item _source, _sourceRoot, _targetRoot;
    protected Map<Long, Long> _oldToNew;
    protected List<Data> _itemDataToCopy;
    protected String _sourceURLPrefix;
    protected String _destURLPrefix;
    protected Multimap<Long, String> _leafParents;
    private final String _idType;
    private final Ontology _ontology = BaseOntology.getOntology();

    /**
     * @param skipLeafDescendants if true, then leaf descendants of {@code source}
     *                            will not be copied. That would be a mistake,
     *                            so you only set this to true if you know
     *                            {@code source} has zero leaf descendants.
     *                            The benefit of skipping them is a performance
     *                            one: each leaf item type is one query during
     *                            this constructor. Today there are 58 leaf item
     *                            types, so skipping them saves you 58
     *                            select-where-path-like finder queries.
     */
    public AbstractItemCopier(Item source, ServiceContext serviceContext, boolean skipLeafDescendants) {
        _serviceContext = serviceContext;
        _source = source;
        if (_source != null) {
            _sourceRoot = _source.getRoot();
            if (skipLeafDescendants) {
                _leafParents = ArrayListMultimap.create();
            } else {
                _leafParents = _serviceContext.getItemService().findLeafParents(_source);
            }
        }
        _oldToNew = new HashMap<Long, Long>();
        _itemDataToCopy = new ArrayList<>();

        _idType = DataTypes.DATA_TYPE_ID;
    }

    public void setURLMapping(String from, String to) {
        _sourceURLPrefix = from;
        _destURLPrefix = to;
    }

    /**
     * pass 1: copy all the items and non-item data, building a map from
     * <old-item> to <new-item> and a list of item data that need to be
     * created.. pass 2: create all the item data; look in the map to see if an
     * item was part of the copied tree; if so, use the new item otherwise the
     * original
     *
     * @param destParent
     */
    public Item copyInto(Item destParent) {
        _targetRoot = destParent.getRoot();

        Item newItem = copyItem(_source, destParent);
        copyItemData();
        return newItem;
    }

    /**
     * Copies the source item over the destination item. Data types that are
     * already present on the destination item will not be copied over. For
     * finderized data, the datum is considered to exist if non-null.
     *
     * TODO: Fix the 'existing' concept, and specify non-copied data explicitly.
     */
    public void copyOver(Item destItem) {
        _oldToNew.put(_source.getId(), destItem.getId());

        _targetRoot = destItem.getRoot();

        // TODO: BARF on type mismatch

        Set<String> existingData = new HashSet<String>();
        for (Data data : DataTransfer.getCombinedData(destItem)) {
            existingData.add(data.getType());
        }
        for (Data data : DataTransfer.getCombinedData(_source)) {
            if (!existingData.contains(data.getType())) {
                copyData(data, destItem);
            }
        }

        for (Item child : getAllChildren(_source, _leafParents)) {
            copyItem(child, destItem);
        }

        copyItemData();
    }

    public void overwrite(Item destItem) {
        _oldToNew.put(_source.getId(), destItem.getId());

        _targetRoot = destItem.getRoot();

        Set<String> existingData = new HashSet<String>();
        Map<String, Item> existingChildren = new HashMap<String, Item>();
        for (Data data : DataTransfer.getCombinedData(destItem)) {
            existingData.add(data.getType());
            Item item = data.getItem();
            if ((item != null) && item.getParent().equals(destItem)) {
                existingChildren.put(data.getType(), item);
            }
        }

        for (Data data : DataTransfer.getCombinedData(_source)) {
            if (existingData.contains(data.getType())) {
                if (DataFormat.item.equals(_ontology.getDataFormat(data.getType()))) {
                    Item child = existingChildren.get(data.getType());
                    if (child != null) {
                        _serviceContext.getItemService().destroy(child);
                    }
                }
                _serviceContext.getDataService().clear(destItem, data.getType());
                copyData(data, destItem);
            }
        }

        for (Item child : getAllChildren(_source, _leafParents)) {
            copyItem(child, destItem);
        }

        copyItemData();
    }

    /**
     * Replace the data of the given item and its descendants with respective
     * data from the source. This method preserves externally referenced items
     * (i.e. items with an Id data) among destItem's descendants.
     *
     * All data, except for ids, will be removed from destItem and its
     * descendants.
     *
     * This does not copy item data from the source, but could be revised to do
     * so when source and dest are in the same domain or the item data is
     * referenceable within the destination domain.
     *
     * @param destItem
     */
    public void replace(Item destItem) {

        Item replacement = copyInto(destItem.getParent());

        QueryBuilder qb = queryBuilder();
        qb.setPath(destItem.getPath().concat("%"));
        qb.addCondition(_idType, "ne", null);

        for (Item idItem : qb.getItems()) {
            String idStr = DataTransfer.getStringData(idItem, _idType);
            QueryBuilder qb2 = queryBuilder();
            qb2.setPath(replacement.getPath().concat("%"));
            qb2.addCondition(_idType, "eq", idStr);
            Item replacementItem = (Item) qb2.getResult();
            if (replacementItem == null) {
                throw new RuntimeException("Missing replacement item: " + idStr);
            }
            _serviceContext.getItemService().replaceItemRefs(idItem, replacementItem);
        }

        _serviceContext.getItemService().destroy(destItem);

    }

    // TODO: If src and dest are different domains then I should
    // automap all identified things...
    //public void map(Item source, Item dest) {
    //}

    protected void copyItemData() {
        // Flushing is necessary so that items exist before references
        // are created in other entities. Hibernate sometimes otherwise
        // misorders persists and FK refs fail.
        _serviceContext.getEntityManager().flush();
        for (Data data : _itemDataToCopy) {
            copyItemData(data);
        }
    }

    protected Item copyItem(Item sourceItem, Item destParent) {
        String type = sourceItem.getType();
        Item destItem = null;
        Object filteredSource = filter(sourceItem);
        if (filteredSource != null) {
            if (filteredSource instanceof Item) {
                Item filteredItem = (Item) filteredSource;
                destItem = _serviceContext.getItemService().create(destParent, type);
                _oldToNew.put(sourceItem.getId(), destItem.getId());
                if (!filteredSource.equals(sourceItem)) {
                    _oldToNew.put(filteredItem.getId(), destItem.getId());
                }
                EntityDescriptor descriptor = _ontology.getEntityDescriptor(sourceItem.getType());
                for (Data data : DataTransfer.getCombinedData(filteredItem)) {
                    copyData(data, destItem);
                }
                if ((descriptor == null) || descriptor.getItemRelation().isPeered()) {
                    for (Item child : getAllChildren(filteredItem, _leafParents)) {
                        copyItem(child, destItem);
                    }
                }
            } else if (filteredSource instanceof Collection) {
                for (Item item: (Collection<Item>) filteredSource) {
                    copyItem(item, destParent);
                }
            }
        }
        return destItem;
    }

    @SuppressWarnings("deprecation")
    protected void copyData(Data data, Item destItem) {
        String type = data.getType();
        data = filter(destItem, data);
        if (data != null) {
            if (DataTypes.DATA_TYPE_URL.equals(type)) {
                String url = data.getString();
                if ((url != null) && (_sourceURLPrefix != null) && url.startsWith(_sourceURLPrefix)) {
                    url = _destURLPrefix + url.substring(_sourceURLPrefix.length());
                }
                _serviceContext.getDataService().setString(destItem, type, url);
            } else if (DataTypes.DATA_TYPE_BODY.equals(type)) {
                String text = data.getText();
                if ((text != null) && (_sourceURLPrefix != null)) {
                    text = text.replaceAll(_sourceURLPrefix, _destURLPrefix); // Ugh
                }
                _serviceContext.getDataService().createText(destItem, type, text);
            } else if (data.getItem() != null) {
                _itemDataToCopy.add(data);
            } else {
                _serviceContext.getDataService().copy(destItem, data);
            }
        }
    }

    private Item getMapped(Item item) {
        Long mapped = _oldToNew.get(item.getId());
        if (mapped == null) {
            return null;
        } else {
            return _serviceContext.getItemService().get(mapped, item.getType());
        }
    }

    protected void copyItemData(Data data) {
        if ((data.getItem() == null) || (data.getItem().getDeleted() != null)) {
            return;
        }
        // The dest will always be in the map because this data was
        // taken from an item that was copied
        Item destItem = getMapped(data.getOwner());
        try {
            Item dataItem = lookup(data.getItem());
            _serviceContext.getDataService().createItem(destItem, data.getType(), dataItem);
        } catch (Exception ex) {
            throw new RuntimeException("Error mapping: " + destItem + " / " + data, ex);
        }
    }

    protected Item lookup(Item item) {
        Item mapped = getMapped(item);
        // horrible hack for copying across domains
        if ((mapped == null) && ((_targetRoot != null) && (_targetRoot != _sourceRoot))) {
            Data id = Iterables.getFirst(DataTransfer.findRawData(item, DataTypes.DATA_TYPE_ID), null);
            if (id != null) {
                mapped = _serviceContext.findDomainItemById(_targetRoot.getId(), id.getString());
            }
            if (mapped == null) {
                Set<Long> keys = new HashSet<Long>();
                logger.log(Level.WARNING, "Mapping: " + item.getId() + " versus " + _oldToNew.keySet());
                throw new IllegalStateException("Invalid cross-domain reference: " + item);
            }
        }
        return (mapped == null) ? item : mapped;
    }

    public abstract Object filter (Item item);

    public abstract Data filter(Item owner, Data data);

    /**
     * Get all the children of a particular item including orphans.
     *
     * @param parent
     *            the parent item
     * @param leafParents
     *            the orphan points
     *
     * @return all the children, including orphans
     */
    private Iterable<Item> getAllChildren(Item parent,
            Multimap<Long, String> leafParents) {
        List<Iterable<Item>> childrens = new ArrayList<>();
        QueryBuilder iqb = queryParent(parent, null);
        iqb.setCacheQuery(false);
        childrens.add(iqb.getItems());
        for (String orphan : leafParents.get(parent.getId())) {
            // I don't want to cache this query
            QueryBuilder qb = queryParent(parent, orphan);
            qb.setCacheQuery(false);
            childrens.add(qb.getItems());
        }
        return Iterables.concat(childrens);
    }

    private QueryBuilder queryParent(Item parent, String itemType) {
        QueryBuilder qb = queryBuilder();
        qb.setParent(parent);
        if (itemType != null) {
            qb.setItemType(itemType);
        }
        return qb;
    }

    private QueryBuilder queryBuilder() {
        return ServiceContext.getContext().queryBuilder();
    }
}
