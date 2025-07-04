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

package loi.cp.storage;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.learningobjects.cpxp.component.annotation.*;
import com.learningobjects.cpxp.component.web.*;
import com.learningobjects.cpxp.component.web.util.JacksonUtils;
import com.learningobjects.cpxp.dto.DataTransfer;
import com.learningobjects.cpxp.dto.Ontology;
import com.learningobjects.cpxp.service.Current;
import com.learningobjects.cpxp.service.attachment.AttachmentFacade;
import com.learningobjects.cpxp.service.attachment.AttachmentWebService;
import com.learningobjects.cpxp.service.data.*;
import com.learningobjects.cpxp.service.domain.DomainConstants;
import com.learningobjects.cpxp.service.domain.DomainState;
import com.learningobjects.cpxp.service.domain.DomainWebService;
import com.learningobjects.cpxp.service.finder.ItemRelation;
import com.learningobjects.cpxp.service.item.Item;
import com.learningobjects.cpxp.service.item.ItemService;
import com.learningobjects.cpxp.service.item.ItemTypedef;
import com.learningobjects.cpxp.service.item.ItemWebService;
import com.learningobjects.cpxp.service.name.NameService;
import com.learningobjects.cpxp.service.query.*;
import com.learningobjects.cpxp.shale.JsonMap;
import com.learningobjects.cpxp.util.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import javax.inject.Inject;
import java.io.Writer;
import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
@ServletBinding(
  path = "/sys/storage"
)
@EnforceOverlord
public class StorageBrowser extends AbstractRpcServlet {

    @Inject
    private AttachmentWebService _attachmentWebService;

    @Inject
    private DataService _dataService;

    @Inject
    private DomainWebService _domainWebService;

    @Inject
    private ItemService _itemService;

    @Inject
    private ItemWebService _itemWebService;


    @Infer
    private ObjectMapper _mapper;

    @Inject
    private NameService _nameService;

    @Infer
    private Ontology _ontology;

    @Inject
    private QueryService _queryService;

    @Get
    public WebResponse get() {
        return HtmlResponse.apply(this, "storageBrowser.html");
    }

    @SuppressWarnings("unused") // .html
    public boolean getIsOverlord() {
        return Current.getDomainDTO().getType().equals(DomainConstants.DOMAIN_TYPE_OVERLORD);
    }

    @SuppressWarnings("unused") // .html
    public List<String> getJsonTypes() {
        return _ontology.getDataTypes().entrySet().stream()
          .filter(dt -> dt.getValue().value() == DataFormat.json)
          .map(Map.Entry::getKey)
          .map(typeName -> '"' + typeName + '"')
          .collect(Collectors.toList());
    }

    private static final int DEFAULT_LIMIT = 64;
    private static final int SUBLIMIT = 32;

    @Rpc
    public List<JsonMap> getChildren(@Parameter String node, @Parameter Integer limit, @Parameter Integer offset) {
        if (limit == null) {
            limit = DEFAULT_LIMIT;
        }
        if (offset == null) {
            offset = 0;
        }

        Matcher rootTypeMatcher = ROOT_TYPE_RE.matcher(node);

        List<JsonMap> response = new ArrayList<>();
        if ("root".equals(node)) {
            if ("overlord".equals(Current.getDomainDTO().getType())) {
                QueryBuilder qb = _queryService.queryAllDomains();
                qb.setCacheQuery(false);
                qb.setItemType(DomainConstants.ITEM_TYPE_DOMAIN);
                qb.addCondition(DomainConstants.DATA_TYPE_DOMAIN_STATE, Comparison.ne, DomainState.Deleted);
                qb.setOrder(DataTypes.DATA_TYPE_TYPE, Direction.DESC);
                qb.setOrder(DataTypes.DATA_TYPE_NAME, Direction.ASC);
                for (Item child : qb.getItems()) {
                    response.add(marshal(child, true));
                }
            } else {
                response.add(marshal(_itemService.get(Current.getDomain()), true));
            }
        } else if (rootTypeMatcher.matches()) {
            String itemType = rootTypeMatcher.group(1);
            Long rootId = Long.parseLong(rootTypeMatcher.group(2));

            QueryBuilder qb = _queryService.queryRoot(rootId, itemType);
            qb.setCacheQuery(false);
            qb.setFirstResult(offset);
            qb.setOrder(DataTypes.META_DATA_TYPE_ID, Direction.DESC);
            qb.setLimit(1 + limit);
            List<Item> children = qb.getItems();
            preloadFriendlyNameEntities(children);

            for (Item child : children) {
                response.add(marshal(child, false));
            }
            addLoadMore(limit, offset, response, node, children);
        } else {
            Item item = getItem(node);
            QueryBuilder qb = _queryService.queryParent(item);
            qb.setCacheQuery(false);
            qb.setFirstResult(offset);
            qb.setLimit(1 + limit);
            List<Item> children = qb.getItems();
            preloadFriendlyNameEntities(children);
            for (Item child : children) {
                response.add(marshal(child, true));
            }

            addLoadMore(limit, offset, response, item.getId() + "@" + offset, children);

            if (offset == 0) {
                for (String entityName : getItemTypesByRelation(ItemRelation.LEAF)) {
                    qb = _queryService.queryParent(item.getId(), entityName);
                    qb.setCacheQuery(false);

                    // sorting disabled for now because this introduces performance issues with large tables
                    //qb.setOrder(DataTypes.META_DATA_TYPE_ID, Direction.DESC);

                    qb.setLimit(1 + SUBLIMIT);
                    List<Item> subChildren = qb.getItems();
                    preloadFriendlyNameEntities(subChildren);
                    for (Item child : subChildren) {
                        response.add(marshal(child, false));
                    }
                    if (subChildren.size() > SUBLIMIT) {
                        //We added one past the limit, so remove the last one to be correct
                        response.remove(response.size() - 1);

                        qb.setLimit(-1);
                        Long count = qb.getAggregateResult(Function.COUNT) - subChildren.size();
                        JsonMap map = new JsonMap();
                        map.put("id", item.getId() + "/" + entityName);
                        map.put("name", entityName + " \u2a09 " + count);
                        map.put("type", "more-orphans");
                        map.put("disabled", true);
                        response.add(map);
                    }
                }

                if (DomainConstants.ITEM_TYPE_DOMAIN.equals(item.getItemType())) {
                    for (String entityName : getItemTypesByRelation(ItemRelation.DOMAIN)) {
                        JsonMap map = new JsonMap();
                        map.put("id", String.format("%s!%d", entityName, item.getId()));
                        map.put("name", entityName);
                        map.put("type", "Finder");
                        map.put("load_on_demand", true);
                        response.add(map);
                    }
                }
            }
        }
        return response;
    }

    private static void addLoadMore(@Parameter Integer limit, @Parameter Integer offset, List<JsonMap> response, String itemStr, List<Item> children) {
        if (children.size() > limit) {
            //We added one past the limit, so remove the last one to be correct
            response.remove(children.size() - 1);

            JsonMap map = new JsonMap();
            map.put("id", itemStr);
            map.put("name", "+ more");
            map.put("type", "more");
            map.put("disabled", true);
            map.put("load_on_demand", true);
            map.put("loadMore", true);
            map.put("count", offset + limit);
            response.add(map);
        }
    }

    private static final Pattern ITEM_RE = Pattern.compile("(([A-Za-z_.]+:)?[0-9]+|@.*)");
    private static final Pattern ROOT_TYPE_RE = Pattern.compile("([A-Za-z_.]+)!([0-9]+)");

    @Rpc
    public List<JsonMap> getPath(@Required @Parameter String node) throws Exception {
        Item item;
        if (ITEM_RE.matcher(node).matches()) {
            item = getItem(node);
            if (item == null) {
                item = findLeafItem(node);
            }
        } else {
            URL decoded = new URL((node.contains(":") ? "" : "http://localhost") + node);
            Long domain = "overlord".equals(Current.getDomainDTO().getType())
              ? _domainWebService.getDomainIdByHost(decoded.getHost()) : Current.getDomain();
            if (decoded.getFile().startsWith("//")) {
                Current.setDomainDTO(_domainWebService.getDomainDTO(domain));
                item = ObjectUtils.getFirstNonNullIn(_queryService.query(decoded.getFile()));
            } else {
                item = _nameService.getItem(_itemService.get(domain), StringUtils.removeEnd(decoded.getFile(), "/"));
            }
        }
        if (!getIsOverlord() && isCrossDomain(item)) {
            item = null;
        }
        List<JsonMap> response = new ArrayList<>();
        for (Item i = item, childItem = null; i != null; i = i.getParent()) {
            JsonMap map = getPathData(i, childItem);

            response.add(0, map);
            childItem = i;
        }
        return response;
    }

    private boolean isCrossDomain(Item item) {
        return (item != null) && !item.getRoot().getId().equals(Current.getDomain());
    }

    private JsonMap getPathData(Item item, Item childItem) {
        JsonMap map = new JsonMap();
        String nodeId = item.getType() + ":" + Math.abs(item.getId());
        map.put("id", nodeId);
        if (childItem == null) {
            map.put("data", getData(nodeId));
        } else {
            List<JsonMap> children = getChildren(nodeId, DEFAULT_LIMIT, 0);
            for (int index = 0; index < children.size(); ++ index) {
                if (children.get(index).containsKey("loadMore")) {
                    children.add(index, marshal(childItem, true));
                    break;
                }
                if (children.get(index).containsValue("more-orphans")) {
                    children.add(index, marshal(childItem, false)); //Never expandable
                    break;
                }
            }
            map.put("children", children);
        }
        return map;
    }

    @Rpc
    public JsonMap getData(@Parameter String node) {
        Item item = getItem(node);
        return marshalData(item);
    }

    private JsonMap marshalData(Item item) {
        // TODO: This should return a list of objects so we can return name, value and type
        JsonMap map = new JsonMap();
        for (Data data : DataTransfer.getCombinedData(item)) {
            try {
                Object value = map.get(data.getType());
                if (value != null) {
                    map.put(data.getType(), value + ", " + stringify(getValue(item, data)));
                } else {
                    map.put(data.getType(), stringify(getValue(item, data)));
                }
            } catch (Exception e) {
                map.put(data.getType(), "Error: Unknown type");
            }
        }
        return map;
    }

    private Object getValue(Item item, Data data) {
        return data.getValue(_ontology);
    }

    @Rpc
    public void deleteNode(@Parameter String node, @Parameter String jsonModel) {
        Item item = getItem(node);
        _itemService.delete(item);
    }

    @Rpc
    public void clearData(@Parameter String node, @Parameter String jsonModel, @Parameter String type) {
        Item item = getItem(node);
        _dataService.clear(item, type);
    }

    @Rpc
    public void setData(@Parameter String node, @Parameter String jsonModel, @Parameter String type, @Parameter String value, @Parameter Boolean set) throws Exception {
        Item item = getItem(node);
        switch (_ontology.getDataFormat(type)) {
            case string:
            case path:
            case tsvector:
            case DOUBLE: // string in the data table but.. double in a finder column?
                if (set) {
                    _dataService.setString(item, type, value);
                } else {
                    _dataService.createString(item, type, value);
                }
                break;
            case text:
                if (set) {
                    _dataService.setText(item, type, value);
                } else {
                    _dataService.createText(item, type, value);
                }
                break;
            case number:
                if (set) {
                    _dataService.setNumber(item, type, NumberUtils.parseLong(value));
                } else {
                    _dataService.createNumber(item, type, NumberUtils.parseLong(value));
                }
                break;
            case time:
                if (set) {
                    _dataService.setTime(item, type, FormattingUtils.parseTime(value));
                } else {
                    _dataService.createTime(item, type, FormattingUtils.parseTime(value));
                }
                break;
            case bool: {
                Boolean val = StringUtils.isEmpty(value) ? null : "true".equals(value);
                if (set) {
                    _dataService.setBoolean(item, type, val);
                } else {
                    _dataService.createBoolean(item, type, val);
                }
                break;
            }
            case item: {
                Item val = StringUtils.isEmpty(value) ? null : getItem(value);
                if ((val != null) && !val.getRoot().equals(item.getRoot())) {
                    throw new Exception("Crossdomain: " + val);
                }
                if (set) {
                    _dataService.setItem(item, type, val);
                } else {
                    _dataService.createItem(item, type, val);
                }
                break;
            }
            case json: {
                JsonNode val = StringUtils.isEmpty(value) ? null
                  : JacksonUtils.getMapper().readTree(value);
                _dataService.setType(item, type, val);
                break;
            }
        }
    }

    @Rpc
    @Direct
    public void downloadItem(@Parameter String node, @Parameter String jsonModel, HttpServletRequest request, HttpServletResponse response) throws Exception {
        Item item = getItem(node);
        response.setContentType(MimeUtils.MIME_TYPE_APPLICATION_JSON + MimeUtils.CHARSET_SUFFIX_UTF_8);
        response.addHeader(HttpUtils.HTTP_HEADER_CONTENT_DISPOSITION, HttpUtils.getDisposition(HttpUtils.DISPOSITION_ATTACHMENT, item.getType() + "_" + item.getId() + ".json"));
        JsonNode stuff = null; // no more available to us... _jsonModelService.freeze(item);
        try (Writer writer = HttpUtils.getWriter(request, response)) {
            _mapper
              .writer(SerializationFeature.INDENT_OUTPUT)
              .writeValue(writer, stuff);
        }
    }

    @Rpc
    @Direct
    public FileResponse<?> downloadAttachment(@Parameter String node, @Parameter String jsonModel) {
        Item item = getItem(node);
        AttachmentFacade attachment = _attachmentWebService.getAttachment(item);
        FileInfo info = _attachmentWebService.getAttachmentBlob(attachment.getId());
        info.setDisposition(HttpUtils.getDisposition(HttpUtils.DISPOSITION_ATTACHMENT, StringUtils.defaultIfEmpty(attachment.getFileName(), attachment.getId().toString() + ".dat")));
        return FileResponse.apply(info);
    }

    private Item getItem(String node) {
        if (node.startsWith("@")) {
            return _itemService.get(_itemWebService.getById(node.substring(1)));
        } else if (node.startsWith("//")) {
            return ObjectUtils.getFirstNonNullIn(_queryService.query(node));
        }
        int index = node.indexOf(':');
        String type = (index < 0) ? "" : node.substring(0, index);
        Long id = Long.valueOf((index < 0) ? node : node.substring(1 + index));
        return _itemService.get(id, type);
    }

    private Item findLeafItem(String pkStr) {
        for (String entityName : getItemTypesByRelation(ItemRelation.LEAF)) {
            Item entity = _itemService.get(Long.valueOf(pkStr), entityName);

            if (entity != null) {
                return entity;
            }
        }
        return null;
    }

    private JsonMap marshal(Item item, boolean expandable) {
        JsonMap map = new JsonMap();
        map.put("id", item.getType() + ":" + item.getId());
        StringBuilder sb = new StringBuilder(item.getType());
        sb.append(':').append(item.getId());
        sb.append(friendly(item));
        map.put("name", sb.toString());
        map.put("type", item.getType());
        if (expandable) { // && countChildren(item) > 0);
            map.put("load_on_demand", true);
        }
        return map;
    }

    private String stringify(Object value) {
        if (value instanceof Item) {
            Item item = (Item) value;
            StringBuilder sb = new StringBuilder(item.getType());
            sb.append(':').append(item.getId());
            sb.append(friendly(item));
            return sb.toString();
        } else if (value instanceof Date) {
            return FormattingUtils.formatTime((Date) value);
        } else if (value instanceof JsonNode) {
            try {
                return _mapper.writer(SerializationFeature.INDENT_OUTPUT).writeValueAsString(value);
            } catch (JsonProcessingException notLikely) {
                return value.toString(); // !!!
            }
        } else if (value != null) {
            return value.toString();
        } else {
            return null;
        }
    }

    private static final int CYCLES = 5;

    /**
     * Preload the entities needed to render the friendly names of the
     * specified items. This simply loads the related entities into the
     * L1 cache in order that the subsequent iterations used to extract
     * the friendly names can avoid repeatedly hitting the database.
     */
    private void preloadFriendlyNameEntities(final List<Item> children) {
        preloadFriendlyNameEntities(children, 0);
    }

    private void preloadFriendlyNameEntities(final List<Item> children, final int cycles) {
        // Group all the items by their type
        final Map<String, List<Item>> grouped =
          children.stream().collect(Collectors.groupingBy(Item::getType));
        final List<Item> friendlyItems = new ArrayList<>();
        final List<Item> dataItems = new ArrayList<>();
        // For each group of items of a given type
        for (final Map.Entry<String, List<Item>> entry : grouped.entrySet()) {
            final String itemType = entry.getKey();
            final List<Item> items = entry.getValue();
            final ItemTypedef itemTypedef = _ontology.getItemTypes().get(itemType);
            if ((itemTypedef == null) || (itemTypedef.itemRelation() == ItemRelation.PEER)) {
                dataItems.addAll(items);
            }
            // Is the item type finderized then...
            if (itemTypedef != null) {
                // Preload the peer finders of the items if they are peer-related.
                // For leaf and unrelated item types, the finder has already been
                // loaded.
                if (itemTypedef.itemRelation() == ItemRelation.PEER) {
                    _itemService.map(Ids.get(items), itemType); // preload the peer finders
                }
                // Find the friendly name type
                final String friendlyNameType = getFriendlyNameType(itemTypedef);
                DataTypedef dataTypedef = _ontology.getDataType(friendlyNameType);
                // If it is an entity relation then...
                if ((dataTypedef != null) && DataFormat.item.equals(dataTypedef.value())) {
                    final List<Long> friendlyPKs = new ArrayList<>();
                    // Accumulate the PKs of the related friendly name entities
                    for (final Item item : items) {
                        // This relies on Hibernate's lazy fetch behaviour avoiding a database
                        // hit to extract the PK
                        final Long friendlyPK = DataTransfer.getPKData(item, friendlyNameType);
                        if (friendlyPK != null) {
                            friendlyPKs.add(friendlyPK);
                        }
                    }
                    // Then bulk load the related entities and add them to our list to iterate
                    friendlyItems.addAll(_itemService.get(friendlyPKs, dataTypedef.itemType()));
                }
            }
        }
        // storage browser always asks for data (sigh) so preload it
        if (!dataItems.isEmpty()) {
            _itemService.preloadData(dataItems);
        }
        if (!friendlyItems.isEmpty() && (cycles <= CYCLES)) {
            preloadFriendlyNameEntities(friendlyItems, 1 + cycles);
        }
    }

    private String friendly(Item item){
        return friendly(item, 0);
    }

    private String friendly(Item item, int cycles) {
        StringBuilder sb = new StringBuilder();

        String id = DataTransfer.getStringData(item, DataTypes.DATA_TYPE_ID);
        if (StringUtils.isNotEmpty(id)) {
            sb.append(" @").append(id);
        }

        final ItemTypedef itemTypedef = _ontology.getItemTypes().get(item.getType());
        final String friendlyNameType = getFriendlyNameType(itemTypedef);
        Object friendlyVal = DataTransfer.getDataValue(item, friendlyNameType, "");

        // if data is an item reference, look for the friendly name of that item (but don't recurse forever)
        if ((friendlyVal instanceof Item) && (cycles <= CYCLES)){
            return friendly((Item)friendlyVal, 1 + cycles);
        }

        String friendly = (friendlyVal == null) ? "" : friendlyVal.toString();
        if (StringUtils.isNotBlank(friendly)) {
            sb.append(" \"").append(HtmlUtils.truncate(friendly, 255, true).getText()).append("\"");
        }

        return sb.toString();
    }

    private static final String getFriendlyNameType(final ItemTypedef itemTypedef) {
        return (itemTypedef == null) ? DataTypes.DATA_TYPE_NAME
          : StringUtils.defaultIfEmpty(itemTypedef.friendlyName(), DataTypes.DATA_TYPE_NAME);
    }

    private List<String> getItemTypesByRelation(ItemRelation relation) {
        return _ontology.getEntityDescriptors().entrySet().stream()
          .filter(e -> (e.getValue().getItemRelation() == relation) && !IGNORE_FINDERS.contains(e.getKey()))
          .map(Map.Entry::getKey)
          .collect(Collectors.toList());
    }

    private static final Set<String> IGNORE_FINDERS = new HashSet<>();

    static {
        IGNORE_FINDERS.add("Session");
        IGNORE_FINDERS.add("View");
        IGNORE_FINDERS.add("ActorDomain");
    }
}
