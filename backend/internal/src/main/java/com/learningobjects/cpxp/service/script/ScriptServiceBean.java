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

package com.learningobjects.cpxp.service.script;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Function;
import com.google.common.collect.Maps;
import com.learningobjects.cpxp.component.BaseComponentArchive.VirtualArchiveAnnotation;
import com.learningobjects.cpxp.component.BaseComponentArchive.VirtualComponentAnnotation;
import com.learningobjects.cpxp.component.*;
import com.learningobjects.cpxp.component.util.ComponentUtils;
import com.learningobjects.cpxp.component.util.ConfigUtils;
import com.learningobjects.cpxp.operation.PrivilegedOperation;
import com.learningobjects.cpxp.service.BasicServiceBean;
import com.learningobjects.cpxp.service.Current;
import com.learningobjects.cpxp.service.attachment.AttachmentFacade;
import com.learningobjects.cpxp.service.attachment.AttachmentWebService;
import com.learningobjects.cpxp.service.attachment.Disposition;
import com.learningobjects.cpxp.service.data.DataTypes;
import com.learningobjects.cpxp.service.facade.FacadeService;
import com.learningobjects.cpxp.service.item.Item;
import com.learningobjects.cpxp.service.name.NameService;
import com.learningobjects.cpxp.service.operation.OperationService;
import com.learningobjects.cpxp.service.overlord.OverlordWebService;
import com.learningobjects.cpxp.service.query.Comparison;
import com.learningobjects.cpxp.service.query.QueryService;
import com.learningobjects.cpxp.util.*;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.CharEncoding;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import javax.inject.Provider;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@Stateless
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class ScriptServiceBean extends BasicServiceBean implements ScriptService {
    private static final Logger logger = Logger.getLogger(ScriptServiceBean.class.getName());

    // This is a short-to-medium term workaround for the current state of domain configurations.
    // We should perhaps just wipe all overlord component settings.
    private static final Set<String> OVERLORD_CARS =
      Set.of("lo.ventures.overlordApi", "lo.ventures.overlordInternal");
    // This is far less conscionable than the above but uniquely expedient
    private static final Set<String> OVERLORD_COMPONENTS =
      Set.of("loi.platform.overlord.Overlord", "loi.platform.overlord.OverlordErrors");

    /** The attachment web service. */
    @Inject
    private AttachmentWebService _attachmentWebService;

    /** The facade service. */
    @Inject
    private FacadeService _facadeService;

    /** The name service. */
    @Inject
    private NameService _nameService;

    /** The operation service. */
    @Inject
    private OperationService _operationService;

    /** The overlord web service. */
    @Inject
    private OverlordWebService _overlordWebService;

    /** The query service. */
    @Inject
    private QueryService _queryService;

    /* Our beans are singletons.
     * Our environments are not.
     * This causes sadness.
     * Provider lets us look up a new ComponentEnvironment every time.
     * Optional accounts for maybe there isn't one.
     */
    @Inject
    private Provider<Optional<ComponentEnvironment>> _environment;

    @Override
    public Item getDomainScriptFolder(Long domainId) {

        Item usersFolder = getDomainItemById(domainId, ScriptConstants.ID_FOLDER_SCRIPTS);

        return usersFolder;

    }

    @Override
    public ScriptSiteFacade getScriptFolder(Long id) {

        Item item = _itemService.get(id);
        ScriptSiteFacade folder = _facadeService.getFacade(item, ScriptSiteFacade.class);

        return folder;
    }

    @Override
    public void clusterRemoveComponentArchive(final String identifier) {
        if (!isOverlord()) {
            throw new IllegalStateException();
        }
        _queryService.queryAllDomains(ScriptConstants.ITEM_TYPE_SCRIPT_ARCHIVE)
          .addCondition(ScriptConstants.DATA_TYPE_SCRIPT_ARCHIVE_IDENTIFIER, Comparison.eq, identifier)
          .addCondition(DataTypes.META_DATA_TYPE_ROOT_ID, Comparison.ne, _overlordWebService.findOverlordDomainId())
          .getFacadeList(ComponentArchiveFacade.class)
          .forEach(car -> {
              invalidateScriptFolder(car.getParent());
              car.delete();
          });
    }

    @Override
    public void removeComponentArchive(String identifier, Long domainId) {
        ScriptSiteFacade scriptFolder = getScriptFolder(getDomainScriptFolder(domainId).getId());
        ComponentArchiveFacade archive = scriptFolder.findComponentArchiveByIdentifier(identifier);
        if (archive != null) {
            invalidateScriptFolder(scriptFolder);

            Set<String> identifiers = new HashSet<>();
            loadCarJson(archive).ifPresent(node -> {
                for (JsonNode component : node.path("components")) {
                    identifiers.add(component.path("identifier").asText());
                }
              });

            Map<String, String> configMap = new HashMap<>(getConfigurationMap(domainId));
            Map<String, Boolean> enabledMap = new HashMap<>(getEnabledMap(domainId));
            identifiers.forEach(id -> {
                configMap.remove(id);
                enabledMap.remove(id);
            });
            setConfigurationMap(domainId, configMap);
            setEnabledMap(domainId, enabledMap);

            scriptFolder.removeComponentArchive(archive);
        }
    }

    @Override
    public ComponentArchiveFacade installComponentArchive(Long scriptFolderId, File scriptArchive, String downloadName) throws Exception {

        ComponentEnvironment env = _environment.get().orElseThrow(() ->
          new IllegalStateException("No environment"));

        Map<String, String> properties = getArchiveProperties(scriptArchive);

        String archiveName = properties.get("name");
        String archiveVersion = properties.get("version");
        String identifier = properties.get("identifier");
        String prefix = properties.get("prefix");
        String strip = properties.get("strip");

        if (StringUtils.isEmpty(identifier) || StringUtils.isEmpty(archiveName)) {
            throw new RuntimeException("Invalid archive");
        }

        ScriptSiteFacade folder = getScriptFolder(scriptFolderId);

        ComponentArchiveFacade archive = folder.findComponentArchiveByIdentifier(identifier);

        boolean structural = true; // is an environment rebuild necessary....

        if (archive == null) {
            archive = folder.addComponentArchive();
            archive.setGeneration(0L);
        } else {
            String oldComponents = properties.get("components");

            // check to see if the "components" property of car.json has changed and only
            // trigger a component environment rebuild if so...
            structural = loadCarJson(archive).map(node -> {
                String newComponents = ComponentUtils.toJson(node.get("components"));
                return !newComponents.equals(oldComponents);
            }).orElse(true);

            invalidateArchive(archive);
        }

        AttachmentFacade attachment = _attachmentWebService.addAttachment(archive.getId(), scriptArchive);
        attachment.setFileName(downloadName);
        attachment.setDisposition(Disposition.attachment);

        // the component archive has no url so can't just bindUrl()
        String path = folder.getUrl();
        String pattern = getFilenameBindingPattern(path, downloadName, "script.zip");
        _nameService.setBindingPattern(attachment.getId(), pattern);

        archive.setName(archiveName);
        archive.setVersion(archiveVersion);
        archive.setIdentifier(identifier);
        archive.setPrefix(prefix);
        archive.setStrip(strip);
        archive.setArchiveFile(attachment.getId());

        if (structural) {
            // force component environment reload
            invalidateScriptFolder(folder);
        } else {
            // reload i18n
            for (ComponentArchive ca : env.getAvailableArchives()) {
                if (identifier.equals(ca.getIdentifier())) {
                    for (ComponentDescriptor cd : ca.getComponents()) {
                        cd.loadMessages();
                    }
                }
            }
        }

        return archive;
    }

    private Optional<JsonNode> loadCarJson(ComponentArchiveFacade archive) {
        return Optional.ofNullable(archive.getArchiveFile()).map(fileId -> {
            try {
                File car = _attachmentWebService.getZipAttachmentFile(fileId, archive.getPrefix() + "car.json");
                if (car != null && car.exists()) {
                    String json = FileUtils.readFileToString(car, "UTF-8");
                    return ComponentUtils.getObjectMapper().readTree(json);
                } else {
                    return null;
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> getArchiveProperties(File scriptArchive) throws Exception {
        ZipFile zip = new ZipFile(scriptArchive);
        try {
            ZipEntry carJsonEntry = ZipUtils.findEntry(zip, "car.json");
            String prefix = StringUtils.substringBefore(carJsonEntry.getName(), "car.json");
            if ((carJsonEntry == null) || carJsonEntry.isDirectory()) {
                throw new RuntimeException("No car.json entry");
            }

            InputStream in = zip.getInputStream(carJsonEntry);
            String json = IOUtils.toString(in, CharEncoding.UTF_8);
            in.close();

            JsonNode node = ComponentUtils.getObjectMapper().readTree(json);
            VirtualArchiveAnnotation archive = ComponentUtils.fromJson(node, VirtualArchiveAnnotation.class);

            // Check to see whether the content is in the loi/cp/bar/ subdirectory
            // or at the top level already.
            List<String> cmpList = new ArrayList<>();
            for (VirtualComponentAnnotation cmp : archive.getComponents()) {
                cmpList.add(cmp.getIdentifier());
            }
            String[] array = cmpList.toArray(new String[cmpList.size()]);
            Arrays.sort(array);
            int prefixLen = 1 + array[0].lastIndexOf('.', StringUtils.getCommonPrefix(array).length() - 1);
            String common = array[0].substring(0, prefixLen).replace('.', '/');
            // For a component loi.cp.bar.Baz, check if the directory
            // loi/cp/bar/ exists. If it does not then presume that the
            // component assets are at the top of the zipfile instead.
            boolean inProperDir = zip.stream().anyMatch(e -> e.getName().startsWith(prefix + common));
            String strip = ((prefixLen > 0) && !inProperDir) ? common : "";

            Map<String, String> properties = new HashMap<>();
            properties.put("name", archive.name());
            properties.put("version", archive.version());
            properties.put("identifier", archive.getIdentifier());
            properties.put("prefix", prefix);
            properties.put("strip", strip);
            properties.put("components", ComponentUtils.toJson(node.get("components")));

            return properties;
        } finally {
            zip.close();
        }
    }

    @Override
    public ComponentEnvironment initComponentEnvironment() {
        final Long domainId = Current.getDomain();

        class InitComponentEnvironmentOperation implements Operation<ComponentEnvironment> {
            @Override
            public ComponentEnvironment perform() {
                return ComponentManager.initComponentEnvironment((domainId == null) ? ComponentCollection.NONE : getComponentCollection(domainId, false));
            }
        };

        final Operation<ComponentEnvironment> op;
        if(domainId == null) {
            op = new InitComponentEnvironmentOperation();
        }
        else {
            op = new PrivilegedOperation<>(new InitComponentEnvironmentOperation());
        }

        return _operationService.perform(op);
    }

    class OverlordComponentCollection implements ComponentCollection {
        private boolean ready = false;
        private final long folderId;
        private final long _version;
        final List<ComponentSource> sources = new ArrayList<>();

        OverlordComponentCollection(Long folderId, long version) {
            this.folderId = folderId;
            _version = version;
        }

        @Override
        public String getIdentifier() {
            return "Overlord";
        }

        @Override
        public long getLastModified() {
            return _version;
        }

        @Override
        public void init() {
            if (ready) {
                return;
            }
            for (ComponentArchiveFacade scriptArchive : getScriptFolder(folderId).getComponentArchives()) {
                if (scriptArchive.getArchiveFile() != null) {
                    sources.add(new ComponentArchiveSource(ComponentCollection.CLUSTER, scriptArchive));
                }
            }
            ready = true;
        }

        @Override
        public Map<String, Boolean> getEnabledMap() {
            return Collections.emptyMap();
        }

        @Override
        public Map<String, String> getConfigurationMap() {
            return Collections.emptyMap();
        }

        @Override
        public String getComponentConfiguration(String identifier) {
            return null;
        }

        @Override
        public Boolean getArchiveEnabled(String identifier) {
            boolean isOverlordCar = OVERLORD_CARS.contains(identifier);
            boolean isVirtualDomain = identifier.equals("com.learningobjects.cbldomain"); // OMFG
            return isOverlordCar ? Boolean.TRUE : (isVirtualDomain ? Boolean.FALSE : null); // excellent use of trivalue logic here
        }

        @Override
        public Boolean getComponentEnabled(String identifier) {
            return OVERLORD_COMPONENTS.contains(identifier) ? true : null;
        }

        @Override
        public List<ComponentSource> getSources() {
            return sources;
        }
    }

    public ComponentCollection getComponentCollection(final Long domainId, boolean edit) {

        Item scriptFolder = getDomainScriptFolder(domainId);
        final Long domainFolderId = scriptFolder.getId();
        final long domainFolderVersion = NumberUtils.longValue(getScriptFolder(scriptFolder.getId()).getGeneration());

        // cluster-wide web components could be modeled as a separate ring but this is expedient
        // considering the inbound rewrite.
        Long overlordDomain = _overlordWebService.findOverlordDomainId();
        final Item overlordDomainFolder = getDomainScriptFolder(overlordDomain);
        final long overlordDomainFolderVersion = NumberUtils.longValue(getScriptFolder(overlordDomainFolder.getId()).getGeneration());

        if (!edit && isOverlord()) {
            return new OverlordComponentCollection(overlordDomainFolder.getId(), overlordDomainFolderVersion);
        }

        final ComponentCollection domainCollection = new ComponentCollection() {
            private boolean ready = false;

            @Override
            public String getIdentifier() {
                return "domain:" + domainId;
            }

            @Override
            public long getLastModified() {
                return domainFolderVersion + overlordDomainFolderVersion;
            }

            final Map<String, Boolean> enabledMap = new HashMap<>();
            final Map<String, String> configurationMap = new HashMap<>();
            final List<ComponentSource> sources = new ArrayList<>();

            @Override
            public void init() {
                if (ready) {
                    return;
                }

                // this delayed init is needed because he input to ComponentManager#getComponentEnv
                // is the actual ComponentCollection so we create this on every request and then
                // look to see if a reload is needed. That should be fixed so that init component
                // env is cheaper.

                enabledMap.putAll(ScriptServiceBean.this.getEnabledMap(overlordDomain));
                enabledMap.putAll(ScriptServiceBean.this.getEnabledMap(domainId));
                OVERLORD_CARS.forEach(e -> enabledMap.put(e, false));
                OVERLORD_COMPONENTS.forEach(e -> enabledMap.put(e, false)); // ??? these cohabitate the map?

                configurationMap.putAll(ScriptServiceBean.this.getConfigurationMap(domainId));
                ScriptServiceBean.this.getConfigurationMap(overlordDomain).entrySet().forEach(entry -> {
                    String identifier = entry.getKey(), config = entry.getValue(),  existing = configurationMap.get(identifier);
                    if (existing == null) {
                        configurationMap.put(identifier, config);
                    } else if (existing.startsWith("{") && config.startsWith("{")) {
                        try {
                            configurationMap.put(identifier, applyDefaults(existing, config));
                        } catch (Exception ex) {
                            logger.log(Level.WARNING, "Error merging json config: " + identifier, ex);
                        }
                    }
                });

                for (ComponentArchiveFacade scriptArchive : getScriptFolder(overlordDomainFolder.getId()).getComponentArchives()) {
                    if (scriptArchive.getArchiveFile() != null) {
                        sources.add(new ComponentArchiveSource(ComponentCollection.CLUSTER, scriptArchive));
                    }
                }
                if (!isOverlord()) {
                    // TODO: handle domain overrides better; if installed in a domain the cluster one should be ignored
                    for (ComponentArchiveFacade scriptArchive : getScriptFolder(domainFolderId).getComponentArchives()) {
                        if (scriptArchive.getArchiveFile() != null) {
                            sources.add(new ComponentArchiveSource(this, scriptArchive));
                        }
                    }
                }

                ready = true;
            }

            @Override
            public Map<String,Boolean> getEnabledMap() {
                return enabledMap;
            }

            @Override
            public Map<String, String> getConfigurationMap() {
                return configurationMap;
            }

            @Override
            public String getComponentConfiguration(String identifier) {
                return configurationMap.get(identifier);
            }

            @Override
            public Boolean getArchiveEnabled(String identifier) {
                return enabledMap.get(identifier);
            }

            @Override
            public Boolean getComponentEnabled(String identifier) {
                return enabledMap.get(identifier);
            }

            @Override
            public List<ComponentSource> getSources() {
                return sources;
            }

            @Override
            public String toString() {
                return "DomainComponentCollection[" + domainId + "]";
            }

        };

        if (edit) {
            domainCollection.init();
        }

        return domainCollection;
    }

    private Map<String, Boolean> getEnabledMap(Long domainId) {

        ScriptSiteFacade scriptFolder = getScriptFolder(getDomainScriptFolder(domainId).getId());

        Map<String, Boolean> adminAvailability = new HashMap<>();
        if (StringUtils.isNotBlank(scriptFolder.getEnabledMap())) {
            adminAvailability = decodeEnabledMap(scriptFolder.getEnabledMap());
        }

        return adminAvailability;
    }

    @Override
    public void setEnabledMap(Long domainId, Map<String, Boolean> map) {
        ScriptSiteFacade scriptFolder = getScriptFolder(getDomainScriptFolder(domainId).getId());
        invalidateScriptFolder(scriptFolder);
        scriptFolder.setEnabledMap(encodeEnabledMap(map));
    }

    @Override
    public void setComponentConfiguration(Long domainId, String componentId, String configuration) {
        Map<String, String> map = new HashMap<>(getConfigurationMap(domainId));
        map.put(componentId, configuration);
        setConfigurationMap(domainId, map);
    }

    @Override
    public void setJsonConfiguration(Long domainId, String componentId, Object configuration) {
        setComponentConfiguration(domainId, componentId, ComponentUtils.toJson(configuration));
    }

    private void invalidateScriptFolder(ScriptSiteFacade scriptFolder) {
        EntityContext.flush();
        scriptFolder.refresh(true);
        scriptFolder.setGeneration(1L + NumberUtils.longValue(scriptFolder.getGeneration()));
        scriptFolder.invalidate();
    }

    private void invalidateArchive(ComponentArchiveFacade archive) {
        EntityContext.flush();
        archive.refresh(true);
        archive.setGeneration(1L + NumberUtils.longValue(archive.getGeneration()));
    }

    @Override
    public Map<String, String> getConfigurationMap(Long domainId) {
        ScriptSiteFacade scriptFolder = getScriptFolder(getDomainScriptFolder(domainId).getId());
        final String map = scriptFolder.getConfigurationMap();
        if (StringUtils.isBlank(map)) {
            return new HashMap<>();
        }
        return decodeConfigurationMap(map);
    }

    @Override
    public void setConfigurationMap(Long domainId, Map<String, String> map) {
        ScriptSiteFacade scriptFolder = getScriptFolder(getDomainScriptFolder(domainId).getId());
        invalidateScriptFolder(scriptFolder);
        scriptFolder.setConfigurationMap(encodeConfigurationMap(map));
    }

    private static Map<String, Boolean> decodeEnabledMap(String textMap) {
        return Maps.transformValues(ConfigUtils.decodeConfiguration(textMap), new Function<String[], Boolean> () {
            public Boolean apply(String[] value) {
                if (null == value || value.length < 1) {
                    return null;
                } else if ("true".equals(value[0])) {
                    return true;
                } else if ("false".equals(value[0])) {
                    return false;
                } else {
                    return null;
                }
            }
        });
    }

    private static Map<String, String> decodeConfigurationMap(String textMap) {
        return Maps.transformValues(ConfigUtils.decodeConfiguration(textMap), new Function<String[], String> () {
            public String apply(String[] value) {
                if (null != value && value.length > 0) {
                    return value[0];
                }
                return null;
            }
        });
    }

    private static String encodeEnabledMap(Map<String, Boolean> availMap) {
        return ConfigUtils.encodeConfiguration(availMap);
    }

    private static String encodeConfigurationMap(Map<String, String> staticConfigurations) {
        return ConfigUtils.encodeConfiguration(staticConfigurations);
    }

    private static String applyDefaults(String config, String defaults) throws Exception {
        JsonNode configJson = ComponentUtils.getObjectMapper().readTree(config);
        JsonNode defaultsJson = ComponentUtils.getObjectMapper().readTree(defaults);
        JsonNode merged = ConfigUtils.applyDefaults(configJson, defaultsJson);
        return ComponentUtils.toJson(merged);
    }

    private boolean isOverlord() {
        return Current.getDomain().equals(_overlordWebService.findOverlordDomainId());
    }
}

