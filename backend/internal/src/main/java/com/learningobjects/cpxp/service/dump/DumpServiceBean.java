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

package com.learningobjects.cpxp.service.dump;

import com.google.common.collect.Multimap;
import com.learningobjects.cpxp.BaseServiceMeta;
import com.learningobjects.cpxp.dto.BaseOntology;
import com.learningobjects.cpxp.dto.DataTransfer;
import com.learningobjects.cpxp.dto.EntityDescriptor;
import com.learningobjects.cpxp.dto.Ontology;
import com.learningobjects.cpxp.service.BasicServiceBean;
import com.learningobjects.cpxp.service.attachment.AttachmentConstants;
import com.learningobjects.cpxp.service.attachment.AttachmentService;
import com.learningobjects.cpxp.service.data.*;
import com.learningobjects.cpxp.service.domain.DomainConstants;
import com.learningobjects.cpxp.service.group.GroupConstants;
import com.learningobjects.cpxp.service.integration.IntegrationConstants;
import com.learningobjects.cpxp.service.item.Item;
import com.learningobjects.cpxp.service.name.NameService;
import com.learningobjects.cpxp.service.query.QueryBuilder;
import com.learningobjects.cpxp.service.trash.TrashConstants;
import com.learningobjects.cpxp.service.user.UserConstants;
import com.learningobjects.cpxp.util.*;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.CloseShieldOutputStream;
import org.apache.commons.lang3.CharEncoding;

import javax.annotation.PostConstruct;
import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import javax.xml.XMLConstants;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.*;
import java.net.URL;
import java.security.DigestInputStream;
import java.security.DigestOutputStream;
import java.security.Key;
import java.security.MessageDigest;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Dump service implementation.
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class DumpServiceBean extends BasicServiceBean implements DumpService {
    private static final Logger logger = Logger.getLogger(DumpServiceBean.class.getName());

    public static final String XMLNS = "http://learningobjects.com/schema/ug/1.0/dump";
    private static final String SIGNING_KEY = "JDxHERNAFf8kVZ3Cd1qCUwnZ1zoFEhgsVFHNRz9fG7Y=";
    private static final String ENCRYPTION_KEY = "wpnZH62/UeC5a7cqm3FAuw==";
    private static final String HMAC_SHA_256 = "HmacSHA256";

    /** The attachment service. */
    @Inject
    private AttachmentService _attachmentService;

    /** The data service. */
    @Inject
    private DataService _dataService;

    /** The name service. */
    @Inject
    private NameService _nameService;

    @PostConstruct
    @SuppressWarnings("unused")
    private void init() throws Exception {
        SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        URL url = getClass().getResource("dump.xsd");
        Schema schema = schemaFactory.newSchema(url);
    }

    // TODO: How to handle parentless items

    @Override
    public void dump(Long id, String prefix, File file, boolean includeAttachments, boolean encrypt) throws Exception {
        dump(id, prefix, file, includeAttachments, encrypt, null);
    }

    public void dump(Long id, String prefix, File file, boolean includeAttachments, boolean encrypt, TempFileMap files) throws Exception {

        Item item = _itemService.get(id);

        OutputStream out = FileUtils.openOutputStream(file);
        try {
            ZipOutputStream2 zip = new ZipOutputStream2(new BufferedOutputStream(out));
//          File directory = new File(FileUtils.getTempDirectory(), "dump-" + System.currentTimeMillis());
//          log(Level.INFO, "Dump directory", directory);
//          zip.setDirectory(directory);

            DumpHandler dumper = new DumpHandler(zip, prefix, Collections.singleton(item), includeAttachments, encrypt, files);
            dumper.dump();

            zip.finish();
            zip.flush();
        } finally {
            out.close();
        }

    }

    public void restoreInto(Long id, String url, File file) throws Exception {

// TODO: fix me.. This fails on init filter bootstrap because
// there is no current user.
//        checkPermission(getCurrentDomain(), PERMISSION_MANAGE);

        restore(_itemService.get(id), url, file, true, false);

    }

    public void restoreReplace(Long id, File file) throws Exception {

        // TODO: FIXME; this prevents domain creation w/ upload
        // checkPermission(getCurrentDomain(), PERMISSION_MANAGE);

        Item item = _itemService.get(id);
        restore(item, _nameService.getPath(item), file, true, true);

    }

    private static class CipherInfo {
        Key key;
        IvParameterSpec iv;
    }

    private Item restore(Item item, String url, File file, boolean into, boolean wipe) throws Exception {
        TempFileMap files = new TempFileMap("dump", ".tmp");
        try {
            files.importZip(file);
            return restore(item, url, files, into, wipe, null);
        } finally {
            files.clear();
        }
    }

    private Item restore(Item item, String url, TempFileMap files, boolean into, boolean wipe, CipherInfo ci) throws Exception {
        long total = 0;
        String dumpEntry = getDumpEntry(files, ci != null);
        String prefix = dumpEntry.substring(0, dumpEntry.indexOf("dump."));

        if (into) {
            logger.info("Counting dump file");
            InputStream in = openDumpStream(files, dumpEntry, ci);
            try {
                XMLInputFactory fac = XMLInputFactory.newInstance();
                XMLStreamReader xr = fac.createXMLStreamReader(in, CharEncoding.UTF_8);
                while (xr.hasNext()) {
                    if ((xr.next() == XMLStreamReader.START_ELEMENT) && ((xr.getNamespaceURI() == null) || "Item".equals(xr.getLocalName()))) {
                        ++ total;
                    }
                }
            } finally {
                in.close();
            }
        }

        InputStream in = openDumpStream(files, dumpEntry, ci);
        try {
            XMLInputFactory fac = XMLInputFactory.newInstance();
            XMLStreamReader xr = fac.createXMLStreamReader(in, CharEncoding.UTF_8);
            RestoreHandler handler = new RestoreHandler(files, prefix, item, url, into, wipe);
            handler.initPercentage("Restore", total);
            handler.parse(xr);
            return handler.getRoot();
        } finally {
            in.close();
        }
    }

    private Map<String, String> readManifest(TempFileMap files, boolean validate) throws Exception {
        Map<String, String> manifestHeader;
        InputStream mfIn = files.getInputStream("META-INF/MANIFEST.MF");
        try {
            ManifestReader mf = new ManifestReader(mfIn);
            manifestHeader = mf.readChunk();
            if (validate) {
                InputStream sfIn = files.getInputStream("META-INF/CPF.SF");
                try {
                    ManifestReader sf = new ManifestReader(sfIn);
                    Map<String, String> sfHeader = sf.readChunk();
                    validate("Manifest Main Attributes", sfHeader.get("SHA256-Digest-Manifest-Main-Attributes"), mf.getInnerDigest());
                    while (true) {
                        Map<String, String> mfChunk = mf.readChunk(), sfChunk = sf.readChunk();
                        if (mfChunk == null) {
                            break;
                        }
                        String name = mfChunk.get("Name");
                        validate("Manifest " + name, sfChunk.get("SHA256-Digest"), mf.getInnerDigest());
                        InputStream in = files.getInputStream(name);
                        try {
                            validate(name, mfChunk.get("SHA256-Digest"), DigestUtils.sha256(in));
                        } finally {
                            in.close();
                        }
                    }
                    validate("Manifest", sfHeader.get("SHA256-Digest-Manifest"), mf.getOuterDigest());
                } finally {
                    sfIn.close();
                }

                File sfFile = files.get("META-INF/CPF.SF");
                Mac mac = Mac.getInstance(HMAC_SHA_256);
                mac.init(new SecretKeySpec(Base64.decodeBase64(SIGNING_KEY), HMAC_SHA_256));
                byte[] actual = mac.doFinal(FileUtils.readFileToByteArray(sfFile));

                File hmacFile = files.get("META-INF/CPF.HMAC");
                byte[] claimed = FileUtils.readFileToByteArray(hmacFile);

                if (!MessageDigest.isEqual(claimed, actual)) {
                    throw new Exception("HMAC check failed");
                }
            }
        } finally {
            mfIn.close();
        }
        return manifestHeader;
    }

    private void validate(String name, String claimed, byte[] actual) throws Exception {
        if (!MessageDigest.isEqual(Base64.decodeBase64(claimed), actual)) {
            throw new Exception("Digest check failed: " + name);
        }
    }

    private InputStream openDumpStream(TempFileMap files, String name, CipherInfo ci) throws Exception {
        InputStream in = new BufferedInputStream(files.getInputStream(name));
        if (ci != null) {
            try {
                Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
                cipher.init(Cipher.DECRYPT_MODE, ci.key, ci.iv);
                in = new GZIPInputStream(new CipherInputStream(in, cipher));
            } catch (Exception ex) {
                in.close();
                throw ex;
            }
        }
        return in;
    }

    private String getDumpEntry(TempFileMap files, boolean encrypted) throws Exception {
        String fname = encrypted ? "dump.raw" : "dump.xml";
        for (String name : files.keySet()) {
            if (name.equals(fname) || name.endsWith("/" + fname)) {
                return name;
            }
        }
        throw new FileNotFoundException(fname);
    }

    private String generateAttachmentFilename(final Item item) {
        final String itemName = com.google.common.collect.Iterables.getFirst(DataTransfer.findStringData(item, DataTypes.DATA_TYPE_NAME), null);
        final String suffix = StringUtils.defaultString(FilenameUtils.getExtension(itemName), "att");
        final String attachmentFilename = "attach/attachment-" + item.getId() + "." + suffix;
        return attachmentFilename;
    }

    private static class StackEntry {
        private ItemRef _item;

        private String _url;

        private String _attachment;

        public StackEntry(ItemRef item, String url, String attachment) {
            _item = item;
            _url = url;
            _attachment = attachment;
        }

        public ItemRef getItem() {
            return _item;
        }

        public String getURL() {
            return _url;
        }

        public String getAttachment() {
            return _attachment;
        }
    }

    private class RestoreHandler {
        private TempFileMap _files;
        private String _prefix;
        private ItemRef _item;
        private ItemRef _root;
        private String _url;
        private String _urlMatch;
        private boolean _into;
        private boolean _wipe;
        private Set<String> _wiped = new HashSet<String>();
        private DateFormat _fmt;
        private boolean _scratch;
        private Pattern integratedUserPattern = Pattern.compile("^//(.+)\\[@(.+)='(.+)'\\]\\/ancestor::(.+)$");
        private Pattern userOrGroupPattern = Pattern.compile("^//(.+)\\[@(.+)='(.+)'\\]");
        private long _then, _count, _total;
        private int _percent;
        private boolean _isDomain;

        public RestoreHandler(TempFileMap files, String prefix, Item item, String url, boolean into, boolean wipe) {
            _files = files;
            _prefix = prefix;
            _item = getItemRef(item);
            _url = url;
            _into = into;
            _wipe = wipe;
            _fmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
            _fmt.setTimeZone(TimeZone.getTimeZone("GMT"));
            _isDomain = into && DomainConstants.ITEM_TYPE_DOMAIN.equals(item.getType());
        }

        public void initPercentage(String type, long total) {
            _then = System.currentTimeMillis();
            _count = 0;
            _total = total;
            _percent = 0;
        }

        private void reportPercentage(String type) {
            if (_total == 0) {
                return;
            }
            int percent = NumberUtils.percent(_count, _total);
            if (percent != _percent) {
                _percent = percent;
                long ms = System.currentTimeMillis() - _then;
                long estimate = ms * (_total - _count) / _count;
                DateUtils.Unit unit = DateUtils.getDurationUnit(estimate, 1.5);
                DateUtils.Delta delta = new DateUtils.Delta(unit, (long) Math.round((double) estimate / unit.getValue()), false);
                logger.info(type + " " + percent + "% complete, e.t.a. " + delta);
            }
        }

        public Item getRoot() {
            return getItem(_root);
        }

        private Item getItem(ItemRef ref) {
            if (ref == null) {
                return null;
            } else if (ref.itemType != null) {
                return _itemService.get(ref.id, ref.itemType);
            } else {
                return getEntityManager().find(Item.class, ref.id);
            }
        }

        private Item getItem(Item item) {
            return _itemService.get(item.getId(), item.getItemType());
        }

        private Stack<StackEntry> _stack = new Stack<StackEntry>();
        private Map<String, ItemRef> _idMap = new HashMap<String, ItemRef>();
        // TODO: multivaluemap
        private Map<String, Collection<ItemAndDataType>> _itemData = new HashMap<String, Collection<ItemAndDataType>>();
        private Map<String, String> _textDataTypes = new HashMap<String, String>();
        private String _textDataType;
        private StringBuilder _text;
        private int _mapped;

        public void parse(XMLStreamReader xr) throws Exception {
            while (xr.hasNext()) {
                switch (xr.next()) {
                  case XMLStreamReader.START_DOCUMENT:
                      startDocument();
                      break;
                  case XMLStreamReader.END_DOCUMENT:
                      endDocument();
                      break;
                  case XMLStreamReader.START_ELEMENT:
                      startElement(xr);
                      break;
                  case XMLStreamReader.END_ELEMENT:
                      endElement(xr);
                      break;
                  case XMLStreamReader.CHARACTERS:
                  case XMLStreamReader.CDATA:
                  case XMLStreamReader.SPACE:
                      characters(xr);
                  default:
                      break;
                }
            }
        }

        private void startDocument() {
            if (!_into) {
                _stack.push(new StackEntry(_item, _url, null));
            }
        }

        private boolean isEl(XMLStreamReader xr, String name) {
            return XMLNS.equals(xr.getNamespaceURI()) && name.equals(xr.getLocalName());
        }

        public void startElement(XMLStreamReader xr) {
            if (isEl(xr, "Dump") || _scratch) {
                // nothing to do
            } else if (isEl(xr, "Scratch")) {
                _scratch = true;
            } else if (isEl(xr, "ItemRef")) {
                String id = xr.getAttributeValue(null, "Id");
                String itemType = xr.getAttributeValue(null, "ItemType");
                String dataType = xr.getAttributeValue(null, "DataType");
                // TODO: support non strings
                String string = xr.getAttributeValue(null, "String");
                QueryBuilder qb = queryRoot(getCurrentDomain(), itemType);
                qb.addCondition(dataType, "eq", string);
                Item item = qb.getItems().iterator().next();
                _idMap.put(id, getItemRef(item));
            } else if (isEl(xr, "Current")) {
                String user = xr.getAttributeValue(null, "User");
                if (user != null) {
                    _idMap.put(user, getItemRef(getCurrentUser()));
                }
            } else if (isEl(xr, "Item") || StringUtils.isEmpty(xr.getNamespaceURI())) {
                boolean isRaw = StringUtils.isEmpty(xr.getNamespaceURI());
                String type = isRaw ? xr.getLocalName() : xr.getAttributeValue(null, "Type");

                if (_wipe) {
                    Item xitem = getItem(_item);
                    if (!queryParent(xitem, null).getItems().isEmpty()) { // optimization for most cases where there's nothing there..
                        _itemService.destroy(xitem, false);
                    }
                    _wipe = false;
                }
                Item item;
                if (_into) {
                    _into = false;
                    if (!type.equals(_item.itemType)) {
                        throw new RuntimeException("Restore into type mismatch: " + type + "/" + _item.itemType);
                    }
                    item = getItem(_item);
                    _root = _item;
                    _urlMatch = xr.getAttributeValue(null, "url");
                } else if (_stack.isEmpty()) {
                    throw new RuntimeException("Multiple roots during restore into: " + type);
                } else {
                    item = _itemService.create(getItem(_stack.peek().getItem()), type);
                    if (_root == null) {
                        _root = getItemRef(item);
                    }
                }
                String id = xr.getAttributeValue(isRaw ? XMLNS : null, "Id");
                if (id != null) {
                    if (_idMap.containsKey(id)) {
                        throw new RuntimeException("Duplicate id during restore: " + id);
                    }
                    _idMap.put(id, getItemRef(item));
                }
                String baseUrl = _stack.isEmpty() ? _url : _stack.peek().getURL();
                String url = xr.getAttributeValue(isRaw ? XMLNS : null, "URL");
                String attachment = xr.getAttributeValue(isRaw ? XMLNS : null, "Attachment");
                String localId = xr.getAttributeValue(isRaw ? XMLNS : null, "LocalId");
                if (url != null) {
                    // TODO: handle url "" for restoring users...
                    String pattern = (attachment == null)
                        ? getBindingPattern(baseUrl, url, "Unknown")
                        : getFilenameBindingPattern(baseUrl , url, "Unknown");
                    baseUrl = _nameService.setBindingPattern(item, pattern);
                    EntityContext.flush(false); // prevent duplicate urls..
                }
                if (localId != null) {
                    // I can't stack/unstack these because an include may refer
                    // to previous siblings or previous siblings of an ancestor
                    // so they just have to stay valid. I could unset them when
                    // I popped the parent Item, I suppose.
                    _idMap.put(localId, getItemRef(item));
                }
                _stack.push(new StackEntry(getItemRef(item), baseUrl, attachment));
                if (++ _count % 1024 == 0) {
                    if (_isDomain) {
                        EntityContext.splitTransaction(true);
                    }
                    reportPercentage("Restore");
                }
                if (isRaw) {
                    boolean idSet = false;
                    for (int i = 0; i < xr.getAttributeCount(); ++ i) {
                        if (StringUtils.isEmpty(xr.getAttributeNamespace(i))) {
                            processData(xr.getAttributeLocalName(i), xr, true);
                            if (xr.getAttributeLocalName(i).equals(DataTypes.DATA_TYPE_ID)) {
                                idSet = true;
                            }
                        }
                    }
                    if (!idSet && id != null) {
                        _dataService.createString(item, DataTypes.DATA_TYPE_ID, id);
                    }
                }
            } else if (isEl(xr, "Data")) {
                String typeName = xr.getAttributeValue(null, "Type");
                if (StringUtils.isEmpty(_urlMatch) && StringUtils.isNotEmpty(_url) && "url".equals(typeName) && (_stack.size() == 1)) {
                    _urlMatch = xr.getAttributeValue(null, "String");
                }
                processData(typeName, xr, false);
            } else if (isEl(xr, "Text")) {
                _textDataType = _textDataTypes.remove(xr.getAttributeValue(null, "Id"));
                _text = new StringBuilder();
            } else {
                throw new RuntimeException("Invalid element: " + xr.getName());
            }
        }

        private void processData(String typeName, XMLStreamReader xr, boolean isRaw) {
            Ontology ontology = BaseOntology.getOntology();
            if (!ontology.getDataTypes().containsKey(typeName)) {
                return;
            }
            Item item = getItem(_wipe ? _item : _stack.peek().getItem());
            EntityDescriptor descriptor = BaseOntology.getOntology().getEntityDescriptor(item.getType());
            if ((descriptor != null) && !descriptor.getItemRelation().isPeered() && !descriptor.containsDataType(typeName)) {
                return;
            }
            String type = typeName;
            if (_wipe && !_wiped.contains(type)) {
                _dataService.clear(item, type);
                _wiped.add(type);
            }
            switch (ontology.getDataFormat(type)) {
              case string: {
                  String string = xr.getAttributeValue(null, isRaw ? typeName : "String");
                  if ("url".equals(type) && StringUtils.isNotEmpty(_urlMatch) && StringUtils.isNotEmpty(_url) && StringUtils.startsWith(string, _urlMatch)) { // hack
                      string = _url + string.substring(_urlMatch.length());
                  }
                  _dataService.createString(item, type, string);
                  break;
              }
              case text: {
                  if (isRaw) {
                      _textDataTypes.put(xr.getAttributeValue(null, typeName), type);
                  } else {
                      _textDataType = type;
                      _text = new StringBuilder(); // can't really represent null text without xsi:nil
                  }
                  break;
              }
              case number: {
                  String attr = xr.getAttributeValue(null, isRaw ? typeName : "Number");
                  Long number = StringUtils.isEmpty(attr) ? null : Long.valueOf(attr);
                  _dataService.createNumber(item, type, number);
                  break;
              }
              case time: {
                  String attr = xr.getAttributeValue(null, isRaw ? typeName : "Time");
                  Date time = null;
                  if (!StringUtils.isEmpty(attr)) {
                      try {
                          if ("current".equals(attr)) {
                              time = getCurrentTime();
                          } else if ("min".equals(attr)) {
                              time = DataSupport.MIN_TIME;
                          } else if ("max".equals(attr)) {
                              time = DataSupport.MAX_TIME;
                          } else {
                              time = _fmt.parse(attr);
                          }
                      } catch (ParseException ex) {
                          throw new RuntimeException("Illegal date: " + time, ex);
                      }
                  }
                  _dataService.createTime(item, type, time);
                  break;
              }
              case bool: {
                  String attr = xr.getAttributeValue(null, isRaw ? typeName : "Boolean");
                  Boolean bool = StringUtils.isEmpty(attr) ? null : Boolean.valueOf(attr);
                  _dataService.createBoolean(item, type, bool);
                  break;
              }
              case DOUBLE:
                  String attr = xr.getAttributeValue(null, isRaw ? typeName : "Double");
                  Double value = StringUtils.isEmpty(attr) ? null : Double.valueOf(attr);
                  _dataService.setType(item, type, value);
                  break;
              case item:
                  String id = xr.getAttributeValue(null, isRaw ? typeName : "Item");
                  if (!StringUtils.isEmpty(id)) {
                      ItemRef mapped = _idMap.get(id);
                      if (mapped != null) {
                          // If I can already resolve this id then remap it
                          // with a unique name. This is necessary because
                          // local ids are reused, and if i don't remap then
                          // all will map to their last usage.
                          id = "mapped-" + (++ _mapped);
                          _idMap.put(id, mapped);
                      }
                      Collection<ItemAndDataType> datas = _itemData.get(id);
                      if (datas == null) {
                          _itemData.put(id, datas = new ArrayList<ItemAndDataType>());
                      }
                      datas.add(new ItemAndDataType(getItemRef(item), type));
                  } else {
                      _dataService.createItem(item, type, null);
                  }
                  break;
            }
        }

        private void characters(XMLStreamReader xr) {
            if (_text != null) {
                _text.append(xr.getText());
            }
        }

        private void endElement(XMLStreamReader xr) {
            if (isEl(xr, "Scratch")) {
                _scratch = false;
            } else if (_scratch) {
                // Skip
            } else if (isEl(xr, "Item") || StringUtils.isEmpty(xr.getNamespaceURI())) {
                StackEntry stackEntry = _stack.pop();
                Item item = getItem(stackEntry.getItem());
                String attachment = stackEntry.getAttachment();
                if (attachment != null) {
                    File file = _files.get(_prefix + attachment);

                    if (null == file) {
                        file = _files.get(_prefix + generateAttachmentFilename(item));
                    }

                    if (file == null) {
                        DumpServiceBean.this.logger.warning("Missing attachment: " + attachment);
                    } else {
                        try {
                            _attachmentService.storeAttachment(item, file);
                        } catch (Exception ex) {
                            throw new RuntimeException("Error restoring attachment: " + attachment, ex);
                        }
                    }
                }
                logger.log(Level.FINE, "Restored, {0}", item);
            } else if (isEl(xr, "Data") || isEl(xr, "Text")) {
                if (_textDataType != null) {
                    String text = _text.toString();
                    if (DataTypes.DATA_TYPE_BODY.equals(_textDataType) && StringUtils.isNotEmpty(_urlMatch) && StringUtils.isNotEmpty(_url)) {
                        text = text.replaceAll(_urlMatch, _url); // Ugh
                    }
                    Item item = getItem(_wipe ? _item : _stack.peek().getItem());
                    _dataService.setText(item, _textDataType, text);
                    _textDataType = null;
                    _text = null;
                }
            }
        }

        private void endDocument() {
            getEntityManager().flush();


            int count = 0;
            for (Map.Entry<String, Collection<ItemAndDataType>> entry: _itemData.entrySet()) {
                count += entry.getValue().size();
            }
            initPercentage("Hookup", count);

            for (Map.Entry<String, Collection<ItemAndDataType>> entry: _itemData.entrySet()) {
                Item item = getItem(entry.getKey());
                for (ItemAndDataType idt: entry.getValue()) {
                    if (item != null) {
                        _dataService.createItem(getItem(idt.ref),idt.dataType, item);
                    }
                    if (++ _count % 1024 == 0) {
                        if (_isDomain) {
                            EntityContext.splitTransaction(true);
                            item = getItem(entry.getKey());
                        }
                        reportPercentage("Hookup");
                    }
                }
            }
        }

        // I always look in the map first, so if I'm restoring a replacement
        // domain I won't look up ids in the current domain
        private Item getItem(String id) {
            Item item = getItem(_idMap.get(id));
            if (item == null) {
                Matcher m = integratedUserPattern.matcher(id);
                if (m.matches()) {
                    // Query the integration of the user, get the user, then
                    // check to make sure the ancestor of the integration matches
                    //  the item type returned.
                    // TODO: query by path
                    String mItemType = m.group(1), mDataType = m.group(2), mDataValue = m.group(3);
                    QueryBuilder qb = queryRoot(getCurrentDomain(), mItemType);
                    qb.addCondition(mDataType, "eq", mDataValue);
                    for (Item integration : qb.getItems()) {
                        Item parent = integration.getParent();
                        if (StringUtils.equals(m.group(4), parent.getType())) {
                            return parent;
                        }
                    }
                    throw new RuntimeException("Unable to lookup integrated user: " + id);
                }

                Matcher matcher = userOrGroupPattern.matcher(id);
                if (matcher.matches()) {
                    String itemType = matcher.group(1), dataType = matcher.group(2), dataValue = matcher.group(3);
                    QueryBuilder qb = queryRoot(getCurrentDomain(), itemType);
                    qb.addCondition(dataType, "eq", dataValue);
                    Item found = com.google.common.collect.Iterables.getFirst(qb.getItems(), null);
                    if ("User".equals(itemType) && (found == null)) {
                        found = getDomainItemById(UserConstants.ID_USER_UNKNOWN);
                    } else if (found == null) {
                        logger.log(Level.WARNING, "Unknown reference: " + id);
                    }
                    return found;
                }

                if ("@domain".equals(id)) {
                    item = getItem(getCurrentDomain());
                } else if (id.startsWith("@")) {
                    item = getItem(getDomainItemById(id.substring(1)));
                } else if (id.startsWith("#")) {
                    Long pk = Long.valueOf(id.substring(1));
                    item = _itemService.get(pk);
                } else {
                    try {
                        String prefix = StringUtils.substringBefore(id, "-");
                        Long pk = Long.valueOf(StringUtils.substringAfter(id, "-"));
                        Item candidate = _itemService.get(pk);
                        if (candidate != null) {
                            if (prefix.equals(getItemTypePrefix(candidate.getType())) && candidate.getRoot().equals(getCurrentDomain())) {
                                item = candidate;
                            }
                        }
                    } catch (Exception ignored) {
                    }
                }
                if (item == null) {
                    throw new RuntimeException("Unable to lookup item: " + id);
                }
            }
            return item;
        }
    }

    // TODO: I could clear entity cache semiregularly except that the getAllChildren
    // loop is over items. If I changed it to loop over item ids then life would be
    // good.. Else I'd need to re-merge each item, which would stink.. But maybe
    // not be unworkable.
    private class DumpHandler {
        private ZipOutputStream _zip;
        private DigestOutputStream _md;
        private String _prefix;
        private Collection<Item> _roots;
        private Set<Long> _ids;
        private Set<Long> _idRefs;
        private Set<Item> _attachments; // TODO: this should be a set of Binary
        // object, once binaries are keyed by their hash code, to avoid duplicates.
        private DateFormat _fmt;
        private boolean _isDomain;
        private boolean _includeAttachments;
        protected Multimap<Long, String> _leafParents;
        private Map<String, byte[]> _digests = new LinkedHashMap<String, byte[]>();
        private long _then, _count, _total;
        private int _percent;

        private boolean _encrypt;
        private byte[] _key, _iv;
        private Cipher _cipher;

        private TempFileMap _files;

        private DumpHandler(ZipOutputStream2 zip, String prefix, Collection<Item> roots, Boolean includeAttachments, boolean encrypt, TempFileMap files) throws Exception {
            _zip = zip;
            _files = files;

            _md = new DigestOutputStream(zip, MessageDigest.getInstance("SHA-256"));
            if (!StringUtils.isEmpty(prefix) && !prefix.endsWith("/")) {
                _prefix = prefix + '/';
            } else {
                _prefix = prefix;
            }
            _roots = roots;
            _ids = new HashSet<Long>();
            _idRefs = new HashSet<Long>();
            _attachments = new HashSet<Item>();
            _fmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
            _fmt.setTimeZone(TimeZone.getTimeZone("GMT"));
            _isDomain = DomainConstants.ITEM_TYPE_DOMAIN.equals(ObjectUtils.getFirstNonNullIn(roots).getType());
            _includeAttachments = includeAttachments;
            _then = System.currentTimeMillis();
            _count = 0;
            _percent = 0;
            _encrypt = encrypt;

            for (Item root : roots) {
                Multimap<Long, String> points = _itemService.findLeafParents(root);
                if (_leafParents == null) {
                    _leafParents = points;
                } else {
                    _leafParents.putAll(points);
                }
                _total += _itemService.countDescendants(root);
            }
            DumpServiceBean.this.logger.info("Counted " + _total + " items for dump");

            if (encrypt) {
                KeyGenerator kgen = KeyGenerator.getInstance("AES");
                kgen.init(128);
                Key key = kgen.generateKey();

                Key masterKey = new SecretKeySpec(Base64.decodeBase64(ENCRYPTION_KEY), "AES");
                Cipher cipher = Cipher.getInstance("AESWrap");
                cipher.init(Cipher.WRAP_MODE, masterKey);
                _key = cipher.wrap(key);
                _iv = NumberUtils.getNonce(16);

                _cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
                _cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(_iv));
            }
        }

        private void reportPercentage(String type) {
            if (_total == 0) {
                return;
            }
            int percent = NumberUtils.percent(_count, _total);
            if (percent != _percent) {
                _percent = percent;
                long ms = System.currentTimeMillis() - _then;
                long estimate = ms * (_total - _count) / _count;
                DateUtils.Unit unit = DateUtils.getDurationUnit(estimate, 1.5);
                DateUtils.Delta delta = new DateUtils.Delta(unit, (long) Math.round((double) estimate / unit.getValue()), false);
                DumpServiceBean.this.logger.info(type + " " + percent + "% complete, e.t.a. " + delta);
            }
        }

        public void dump() throws Exception {
            if (!StringUtils.isEmpty(_prefix)) {
                ZipEntry prefixEntry = new ZipEntry(_prefix);
                _zip.putNextEntry(prefixEntry);
                _zip.closeEntry();
            }

            dumpModel();
            if (_includeAttachments) {
                dumpAttachments();
            }
            dumpManifest();

            if (_files != null) {
                try {
                    for (Map.Entry<String, File> entry : _files.entrySet()) {
                        ZipEntry dumpEntry = new ZipEntry(entry.getKey());
                        _zip.putNextEntry(dumpEntry);
                        _zip.write(FileUtils.readFileToByteArray(entry.getValue()));
                        _zip.closeEntry();
                    }
                } finally {
                    _files.clear();
                }
            }
        }

        private void dumpManifest() throws Exception {
            ZipEntry metaEntry = new ZipEntry("META-INF/");
            _zip.putNextEntry(metaEntry);
            _zip.closeEntry();

            ManifestBuilder mf = new ManifestBuilder();
            ManifestBuilder sf = new ManifestBuilder();

            mf.println("Manifest-Version", "1.0");
            mf.println("Created-By", "Difference Engine " + BaseServiceMeta.getServiceMeta().getVersion());
            if (_encrypt) {
                mf.println("Enc-Alg", "1");
                mf.println("Enc-Key", new String(Base64.encodeBase64(_key)));
                mf.println("Enc-IV", new String(Base64.encodeBase64(_iv)));
            }
            mf.println();

            sf.println("Signature-Version", "1.0");
            sf.println("Created-By", "Difference Engine " + BaseServiceMeta.getServiceMeta().getVersion());
            sf.println("SHA256-Digest-Manifest-Main-Attributes", mf.getInnerDigest());
            sf.println();

            for (String name : _digests.keySet()) {
                mf.println("Name", name);
                mf.println("SHA256-Digest", new String(Base64.encodeBase64(_digests.get(name))));
                mf.println();

                sf.println("Name", name);
                sf.println("SHA256-Digest", mf.getInnerDigest());
                sf.println();
            }

            sf.inject("SHA256-Digest-Manifest", mf.getOuterDigest());

            ZipEntry mfEntry = new ZipEntry("META-INF/MANIFEST.MF");
            _zip.putNextEntry(mfEntry);
            mf.writeTo(_zip);
            _zip.closeEntry();

            ZipEntry sfEntry = new ZipEntry("META-INF/CPF.SF");
            _zip.putNextEntry(sfEntry);
            sf.writeTo(_zip);
            _zip.closeEntry();

            Mac mac = Mac.getInstance(HMAC_SHA_256);
            mac.init(new SecretKeySpec(Base64.decodeBase64(SIGNING_KEY), HMAC_SHA_256));
            byte[] macBytes = mac.doFinal(sf.getBytes());

            ZipEntry hmacEntry = new ZipEntry("META-INF/CPF.HMAC");
            _zip.putNextEntry(hmacEntry);
            _zip.write(macBytes);
            _zip.closeEntry();
        }

        private void dumpModel() throws Exception {
            String name = _prefix + (_encrypt ? "dump.raw" : "dump.xml");

            ZipEntry dumpEntry = new ZipEntry(name);
            _zip.putNextEntry(dumpEntry);
            _zip.setMethod(ZipOutputStream.STORED);

            XMLOutputFactory fac = XMLOutputFactory.newInstance();

            OutputStream out = new CloseShieldOutputStream(_md);
            if (_encrypt) {
                out =  new GZIPOutputStream(new CipherOutputStream(out, _cipher));
            }

            XMLStreamWriter xw = fac.createXMLStreamWriter(out, CharEncoding.UTF_8);

            xw.writeStartDocument();
            xw.setDefaultNamespace(XMLNS);
            startEl(xw);
            xw.writeStartElement("cpf", "Dump", XMLNS);
            xw.writeNamespace("cpf", XMLNS);
            xw.writeAttribute(XMLNS, "Version", BaseServiceMeta.getServiceMeta().getVersion());
            for (Item root: _roots) {
                dumpItem(root, xw);
            }
            endEl(xw);
            xw.writeEndElement();
            xw.writeEndDocument();
            xw.close();
            out.close();

            _zip.closeEntry();
            _zip.setMethod(ZipOutputStream.DEFLATED);

            recordDigest(name);
        }

        private void recordDigest(String name) {
            _digests.put(name, _md.getMessageDigest().digest());
        }

        private void dumpAttachments() throws Exception {
            _then = System.currentTimeMillis();
            _count = 0;
            _percent = 0;
            _total = _attachments.size();
            DumpServiceBean.this.logger.info("Counted " + _total + " attachments for dump");

            ZipEntry attachEntry = new ZipEntry(_prefix + "attach/");
            _zip.putNextEntry(attachEntry);
            _zip.closeEntry();

            for (Item item: _attachments) {
                FileInfo attachmentBlob = _attachmentService.getAttachmentBlob(item);
                if (attachmentBlob != null) {
                    String attachmentName = _prefix + generateAttachmentFilename(item);
                    ZipEntry entry = new ZipEntry(attachmentName);
                    _zip.putNextEntry(entry);
                    InputStream in = attachmentBlob.openInputStream();
                    try {
                        IOUtils.copy(in, _md);
                    } finally {
                        in.close();
                    }
                    _zip.closeEntry();

                    recordDigest(attachmentName);

                    ++ _count;
                    reportPercentage("Attachment");
                }
            }
        }

        private void dumpItem(Item item, XMLStreamWriter xw) throws Exception {
            item = refresh(item);

            if (TrashConstants.ITEM_TYPE_TRASH_RECORD.equals(item.getType())) {
                return;
            }

            Ontology ontology = BaseOntology.getOntology();
            _ids.add(item.getId());
            Iterable<Item> children = getAllChildren(item, _leafParents);
            Iterable<Data> datas = DataTransfer.getCombinedData(item);
            Set<String> found = new HashSet<String>(), dup = new HashSet<String>();
            boolean hasChildren = children.iterator().hasNext();
            for (Data data: datas) {
                hasChildren |= DataFormat.text.equals(ontology.getDataFormat(data.getType())) && StringUtils.isNotEmpty(data.getText());
                String typeName = data.getType();
                if (found.contains(typeName)) {
                    dup.add(typeName);
                } else {
                    found.add(typeName);
                }
            }
            hasChildren |= !dup.isEmpty();
            if (hasChildren) {
                startEl(xw);
                xw.writeStartElement(item.getType());
            } else {
                emptyEl(xw);
                xw.writeEmptyElement(item.getType());
            }
            // I used to only emit these for non-orphans but now I can also reference orphans so...
            xw.writeAttribute("cpf", XMLNS, "Id", getId(item));
            // TODO: Not if the digest is null or broken...
            if (AttachmentConstants.ITEM_TYPE_ATTACHMENT.equals(item.getType()) && _includeAttachments) {
                _attachments.add(item);
                String attachmentFile = generateAttachmentFilename(item);
                xw.writeAttribute(XMLNS, "Attachment", attachmentFile);
            }
            EntityDescriptor descriptor = BaseOntology.getOntology().getEntityDescriptor(item.getType());
            for (Data data: datas) {
                String typeName = data.getType();
                if (_encrypt && "creatorSystem".equals(data.getType())) {
                    continue;
                }
                if (_encrypt && "prototype".equals(data.getType())) { // breaks cross domain imports
                    continue;
                }
                if (!dup.contains(typeName)) {
                    dumpData(data, xw, false);
                }
            }
            for (Data data: datas) {
                if (dup.contains(data.getType())) {
                    dumpData(data, xw, true);
                } else {
                    dumpTextData(data, xw);
                }
            }

            ++ _count;
            reportPercentage("Model");

            if (_count % 1024 == 0) {
                getEntityManager().flush();
                getEntityManager().clear();
            }

            for (Item child: children) {
                if (_encrypt && "Event".equals(child.getType())) {
                    continue;
                }
                dumpItem(child, xw);
            }
            if (hasChildren) {
                endEl(xw);
                xw.writeEndElement();
            }
        }

        private void dumpData(Data data, XMLStreamWriter xw, boolean el) throws Exception {
            Ontology ontology = BaseOntology.getOntology();
            if (data.getValue(BaseOntology.getOntology()) != null) {
                String value = null;
                switch (ontology.getDataFormat(data.getType())) {
                  case text:
                      value = StringUtils.isEmpty(data.getText()) ? "" : data.getType() + "-" + data.getOwner().getId();
                      break;
                  case string:
                      value = XmlUtils.clean(data.getString());
                      break;
                  case number:
                      value = data.getNumber().toString();
                      break;
                  case time:
                      if (DataSupport.isMinimal(data.getTime())) {
                          value = "min";
                      } else if (DataSupport.isMaximal(data.getTime())) {
                          value = "max";
                      } else {
                          value = _fmt.format(data.getTime());
                      }
                      break;
                  case bool:
                      value = data.getBoolean().toString();
                      break;
                  case item:
                      Item item = refresh(data.getItem());
                      if (item.getDeleted() == null) {
                          _idRefs.add(item.getId());
                          value = getId(item);
                      }
                      break;
                }
                if (value != null) {
                    if (el) {
                        emptyEl(xw);
                        xw.writeEmptyElement("cpf", "Data", XMLNS);
                        xw.writeAttribute("Type", data.getType());
                        switch (ontology.getDataFormat(data.getType())) {
                          case string:
                              xw.writeAttribute("String", value);
                              break;
                          case number:
                              xw.writeAttribute("Number", value);
                              break;
                          case time:
                              xw.writeAttribute("Time", value);
                              break;
                          case bool:
                              xw.writeAttribute("Boolean", value);
                              break;
                          case item:
                              xw.writeAttribute("Item", value);
                              break;
                        }
                    } else {
                        xw.writeAttribute(data.getType(), value);
                    }
                }
            }
        }

        private void dumpTextData(Data data, XMLStreamWriter xw) throws Exception {
            Ontology ontology = BaseOntology.getOntology();
            if ((ontology.getDataFormat(data.getType()) != DataFormat.text) || StringUtils.isEmpty(data.getText())) {
                return;
            }
            emptyEl(xw);
            xw.writeStartElement("cpf", "Text", XMLNS);
            xw.writeAttribute("Id", data.getType() + "-" + data.getOwner().getId());
            String text = data.getText();
            if (text != null) {
                // XMLStreamWriter should really handle this case..
                int index;
                while ((index = text.indexOf("]]>")) >= 0) {
                    xw.writeCData(XmlUtils.clean(text.substring(0, index + 2)));
                    text = text.substring(index + 2);
                }
                xw.writeCData(XmlUtils.clean(text));
            }
            xw.writeEndElement();
        }

        private String _ws = "\n";
        private int _depth;

        private void startEl(XMLStreamWriter xw) throws Exception {
            emptyEl(xw);
            ++ _depth;
        }

        private void emptyEl(XMLStreamWriter xw) throws Exception {
            int length = 1 + _depth * 2;
            while (_ws.length() < length) {
                _ws = _ws + "  ";
            }
            xw.writeCharacters(_ws.substring(0, length));
        }

        private void endEl(XMLStreamWriter xw) throws Exception {
            -- _depth;
            int length = 1 + _depth * 2;
            xw.writeCharacters(_ws.substring(0, length));
        }

        private boolean isOrphan(Item item) {
            return (item.getId() == null) || (item.getId().longValue() < 0);
        }

        private String getId(Item item) {
            String id;
            Data idData = null;
            // don't use find because that will populate the entire data type
            // map which is costly and unnecessary.
            item = refresh(item);
            for (Data data: DataTransfer.getData(item)) {
                if ("id".equals(data.getType())) {
                    idData = data;
                    break;
                }
            }
            if (idData != null) {
                id = (_isDomain ? "" : "@") + idData.getString();
            } else if (!_isDomain && "User".equals(item.getType())) {
                return "//User[@" + UserConstants.DATA_TYPE_USER_NAME + "='" + DataTransfer.getStringData(item,UserConstants.DATA_TYPE_USER_NAME) + "']";
            } else if (!_isDomain && "Group".equals(item.getType())){
                return "//Group[@" + GroupConstants.DATA_TYPE_GROUP_ID + "='" + DataTransfer.getStringData(item,GroupConstants.DATA_TYPE_GROUP_ID) + "']";
            } else if (!_isDomain && "System".equals(item.getType())){
                return "//System[@" + IntegrationConstants.DATA_TYPE_SYSTEM_ID +"='" + DataTransfer.getStringData(item, IntegrationConstants.DATA_TYPE_SYSTEM_ID) + "']";
            } else {
                id = getItemTypePrefix(item.getType()) + "-" + item.getId();
            }
            return id;
        }
    }

    static String getItemTypePrefix(String id) {
        id = id.toLowerCase();
        id = id.substring(1 + id.lastIndexOf("."));
        id = id.replaceAll("[^a-z]+", "-");
        return id;
    }

    static class ItemAndDataType {
        public ItemRef ref;
        public String dataType;

        public ItemAndDataType(ItemRef ref, String dataType) {
            this.ref = ref;
            this.dataType = dataType;
        }
    }

    static class ItemRef {
        public Long id;
        public String itemType;

        public ItemRef(Long id, String itemType) {
            this.id = id;
            this.itemType = itemType;
        }
    }

    ItemRef getItemRef(Item item) {
        EntityDescriptor descriptor = BaseOntology.getOntology().getEntityDescriptor(item.getType());
        if ((descriptor != null) && !descriptor.getItemRelation().isPeered()) {
            if (item.getFinder().getId() == null) {
                getEntityManager().merge(item.getFinder());
            }
            return new ItemRef(item.getFinder().getId(), item.getType());
        } else {
            if (item.getId() == null) {
                getEntityManager().merge(item);
            }
            return new ItemRef(item.getId(), item.getType());
        }
    }

    static class ManifestBuilder {
        private final ByteArrayOutputStream _bos;
        private final DigestOutputStream _outer, _inner;

        ManifestBuilder() throws Exception {
            _bos = new ByteArrayOutputStream();
            _inner = new DigestOutputStream(_bos, MessageDigest.getInstance("SHA-256"));
            _outer = new DigestOutputStream(_inner, MessageDigest.getInstance("SHA-256"));
        }

        public void println() throws Exception {
            println("");
        }

        public void println(String s, String b) throws Exception {
            println(s + ": " + b);
        }

        public void println(String s) throws Exception {
            for (int index = s.length() - (s.length() % 70); index > 0; index -= 70) {
                s = s.substring(0, index) + "\r\n " + s.substring(index);
            }
            s += "\r\n";
            _outer.write(s.getBytes("UTF-8"));
        }

        public void inject(String s, String b) throws Exception {
            // hack to inject the full manifest digest
            String str = _bos.toString("UTF-8");
            _bos.reset();
            int index = 2 + str.indexOf("\r\n\r\n");
            _bos.write(str.substring(0, index).getBytes("UTF-8"));
            println(s, b);
            _bos.write(str.substring(index).getBytes("UTF-8"));
        }

        public void writeTo(OutputStream out) throws Exception {
            _bos.writeTo(out);
        }

        public String getInnerDigest() { // resets inner digest
            return new String(Base64.encodeBase64(_inner.getMessageDigest().digest()));
        }

        public String getOuterDigest() {
            return new String(Base64.encodeBase64(_outer.getMessageDigest().digest()));
        }

        public byte[] getBytes() {
            return _bos.toByteArray();
        }
    }

    static class ManifestReader {
        private final DigestInputStream _outer, _inner;

        ManifestReader(InputStream in) throws Exception {
            _inner = new DigestInputStream(in, MessageDigest.getInstance("SHA-256"));
            _outer = new DigestInputStream(_inner, MessageDigest.getInstance("SHA-256"));
        }

        public Map<String, String> readChunk() throws IOException {
            Map<String, String> chunk = new HashMap<String, String>();
            String key = null;
            while (true) {
                String line = readLine();
                if (line == null) {
                    return null;
                } else if ("".equals(line)) {
                    break;
                } else if (line.startsWith(" ")) {
                    chunk.put(key, chunk.get(key) + line.trim());
                } else if (!line.startsWith("#")) {
                    key = StringUtils.substringBefore(line, ":");
                    chunk.put(key, line.substring(1 + key.length()).trim());
                }
            }
            return chunk;
        }

        public byte[] getInnerDigest() {
            return _inner.getMessageDigest().digest();
        }

        public byte[] getOuterDigest() {
            return _outer.getMessageDigest().digest();
        }

        private String readLine() throws IOException { // crude but functional
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            while (true) {
                int n = _outer.read();
                if (n == -1) {
                    return null;
                } else if (n == '\n') {
                    break;
                } else if (n != '\r') {
                    bos.write(n);
                }
            }
            return bos.toString("UTF-8");
        }
    }
}
