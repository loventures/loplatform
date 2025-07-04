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

package com.learningobjects.cpxp.component;

import com.fasterxml.jackson.databind.JsonNode;
import com.learningobjects.cpxp.BaseWebContext;
import com.learningobjects.cpxp.component.annotation.Archive;
import com.learningobjects.cpxp.component.annotation.Component;
import com.learningobjects.cpxp.component.annotation.InheritResources;
import com.learningobjects.cpxp.component.annotation.Service;
import com.learningobjects.cpxp.component.archive.ArchiveContents;
import com.learningobjects.cpxp.component.archive.BaseArchiveCompiler;
import com.learningobjects.cpxp.component.archive.BaseArchiveContents;
import com.learningobjects.cpxp.component.compiler.ComponentClassLoader;
import com.learningobjects.cpxp.component.internal.ArchiveAnnotation;
import com.learningobjects.cpxp.component.internal.ComponentAnnotation;
import com.learningobjects.cpxp.component.internal.InheritResourcesAnnotation;
import com.learningobjects.cpxp.component.registry.Binding;
import com.learningobjects.cpxp.component.registry.ResourceBinding;
import com.learningobjects.cpxp.component.util.ComponentUtils;
import com.learningobjects.cpxp.filter.ReportableException;
import com.learningobjects.cpxp.util.FileUtils;
import com.learningobjects.cpxp.util.ParallelStartup;
import com.learningobjects.cpxp.util.StringUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.tuple.Pair;

import javax.tools.JavaFileObject;
import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

public class BaseComponentArchive  implements ComponentArchive {
    private static final Logger logger = Logger.getLogger(BaseComponentArchive.class.getName());
    private final ComponentSource _source;

    public BaseComponentArchive(ComponentSource source) {
        _source = source;
        VirtualArchiveAnnotation annotation = parseVirtualArchiveAnnotation();
        dependencyRefs = annotation.getDependencies();
        _archive = annotation;
        _archiveTimestamp = _source.getLastModified();
        _identifier = annotation.getIdentifier();
    }

    @Override
    public String getIdentifier() {
        return _identifier;
    }

    @Override
    public ComponentSource getSource() {
        return _source;
    }

    @Override
    public long getLastModified() {
        return _source.getLastModified();
    }

    private Archive _packageArchive = null;
    private List<String> dependencyRefs;
    private String _identifier;
    private ArchiveAnnotation _archive = new ArchiveAnnotation(); // dummy default
    private long _archiveTimestamp = -1L;
    private Optional<ComponentArchive> _implementation = Optional.empty();

    @Override
    public synchronized Archive getArchiveAnnotation() {
        // reload virtual archive annotation after front-end redeploy
        if (_archive instanceof VirtualArchiveAnnotation) {
            long modified = _source.getLastModified();
            if (_archiveTimestamp != modified) {
                _archive = parseVirtualArchiveAnnotation();
                applyPackageArchiveAnnotation();
                _archiveTimestamp = modified;
            }
        }
        return _archive;
    }

    private final Map<String, ComponentDescriptor> _aliasMap = new HashMap<>();
    private final Set<ComponentArchive> _dependencies = new HashSet<>();
    private final Set<ComponentArchive> _allDependencies = new HashSet<>();

    @Override
    public void addDependency(ComponentArchive archive) {
        if (archive == this) {
            return;
        }
        logger.log(Level.FINE, "Component dependency, {0}, {1}", new Object[]{getIdentifier(), archive.getIdentifier()});
        _dependencies.add(archive);
    }

    @Override
    public Set<ComponentArchive> getDependencies() {
        return _dependencies;
    }

    @Override
    public synchronized Set<ComponentArchive> getAllDependencies() {
        return _allDependencies;
    }

    @Override
    public Optional<ComponentArchive> getImplementation() {
        return _implementation;
    }

    @Override
    public void setImplementation(ComponentArchive impl) {
        _implementation = Optional.ofNullable(impl);
    }

    private ArchiveContents _archiveContents;

    @Override
    public ArchiveContents getArchiveContents() {
        return _archiveContents;
    }

    @Override
    public void scan(ComponentRing ring) {
        if (_archiveContents != null) {
            return;
        }
        logger.log(Level.FINE, "Scanning, {0}", _source.getIdentifier());
        try {
            _archiveContents = new BaseArchiveContents(_source);

            Optional<String> apiArchive = Optional.ofNullable(_archive.implementing()).filter(StringUtils::isNotEmpty);
            logger.log(Level.FINE, "Dependencies, {0}", dependencyRefs);
            for (String dependency : dependencyRefs) {
                ComponentArchive archive = ring.findArchive(dependency);
                if (archive != null) {
                    addDependency(archive);
                } else {
                    // We include non-CAR dependencies which should be ignored
                    logger.log(Level.FINE, "Unknown dependency: " + dependency + " (in " + _source.getIdentifier() + ")");
                }
            }
            apiArchive.ifPresent(apiId -> {
                ComponentArchive api = ring.findArchive(apiId);
                if (api != null) {
                    api.setImplementation(this);
                }
            });
        } catch (Exception ex) {
            logger.log(Level.WARNING, "Error scanning: " + getIdentifier(), ex);
            ring.setFailed();
        }
    }

    private VirtualArchiveAnnotation parseVirtualArchiveAnnotation() {
        try {
            return _source.getResource("car.json")
              .filter(Files::exists)
              .map(path -> {
                  try {
                      return Files.readAllBytes(path);
                  } catch (IOException e) {
                      throw new RuntimeException("Error parsing car.json", e);
                  }
              })
              .map(String::new)
              .map(json -> {
                  try {
                      return ComponentUtils.fromJson(json, VirtualArchiveAnnotation.class);
                  } catch (IOException e) {
                      throw new RuntimeException("Error parsing car.json", e);
                  }
              }).orElseThrow(() -> new RuntimeException("Error parsing car.json"));
        } catch (Exception e) {
            throw new RuntimeException("Error parsing car.json", e);
        }
    }

    private BaseArchiveCompiler _archiveCompiler;

    @Override
    public BaseArchiveCompiler getArchiveCompiler() {
        return _archiveCompiler;
    }

    @Override
    public ComponentClassLoader getClassLoader() {
        return _archiveCompiler.getClassLoader();
    }

    @Override
    public SortedMap<String, JavaFileObject> getClassFiles() {
        return _archiveCompiler.getClassFiles();
    }

    private Set<Class<?>> _loadedClasses;

    @Override
    public Iterable<Class<?>> getLoadedClasses() {
        return _loadedClasses;
    }

    private final List<ComponentDescriptor> _components = new ArrayList<>();

    @Override
    public List<ComponentDescriptor> getComponents() {
        return _components;
    }

    private final Set<Class<?>> _boundClasses = new HashSet<>();

    private final List<Pair<Annotation, Class<?>>> _rBoundClasses = new ArrayList<>();

    @Override
    public Set<Class<?>> getBoundClasses() {
        return _boundClasses;
    }

    @Override
    public List<Pair<Annotation, Class<?>>> getResourceBoundClasses() {
        return _rBoundClasses;
    }

    @Override
    public Map<String, ComponentDescriptor> getAliasMap() {
        return _aliasMap;
    }

    @Override
    public synchronized void load(ComponentRing ring) {
        if (_archiveCompiler != null) {
            return;
        }
        Function<Consumer<ComponentArchive>, Consumer<ComponentArchive>> hoAddDerps =
          addDerps -> archive -> {
            if (getAllDependencies().add(archive)) {
              archive.getDependencies().forEach(addDerps);
            }
          };
        Fix.apply(hoAddDerps).accept(this);

        for (ComponentArchive dependency : getDependencies()) {
            dependency.load(ring);
        }
        try {
            logger.log(Level.FINE, "Loading component archive, {0}", getIdentifier());

            _archiveCompiler = new BaseArchiveCompiler(this, ring);
            _packageArchive = _archiveCompiler.loadArchiveAnnotation();
            applyPackageArchiveAnnotation();

            if (_archive instanceof VirtualArchiveAnnotation) { // was loaded from car.json
                // meh
                VirtualArchiveAnnotation annotation = (VirtualArchiveAnnotation) _archive;
                for (VirtualComponentAnnotation component : annotation.getComponents()) {
                    try {
                        processVirtualComponent(component, ring);
                    } catch (Exception ex) {
                        logger.log(Level.WARNING, "Error processing virtual component: " + component.getIdentifier(), ex);
                    }
                }
            } else {
                Optional<File> manMf = _source.getResource("META-INF/MANIFEST.MF").map(Path::toFile);
                if (manMf.isPresent() && manMf.get().exists()) {
                    Properties props = FileUtils.loadProperties(manMf.get());
                    _archive.setName(StringUtils.defaultIfEmpty(_archive.name(), props.getProperty("Implementation-Title")));
                    _archive.setVersion(StringUtils.defaultIfEmpty(_archive.version(), props.getProperty("Implementation-Version")));
                    _archive.setBranch(props.getProperty("Implementation-Branch"));
                    _archive.setRevision(props.getProperty("Implementation-Revision"));
                    _archive.setBuildNumber(props.getProperty("Implementation-BuildNumber"));
                    _archive.setBuildDate(props.getProperty("Implementation-BuildDate"));
                }
            }

            _loadedClasses = _archiveCompiler.loadAllClasses();

            ParallelStartup.foreach(_loadedClasses, this::processClass);

        } catch (Exception ex) {
            ring.setFailed();
            HttpServletRequest web = BaseWebContext.getContext().getRequest();
            if (BaseWebContext.getDebug() || ((web != null) && ("/sys/reload".equals(web.getRequestURI()) || Boolean.TRUE.equals(web.getAttribute("reload"))))) { // /sys/deploy sets the latter
                throw new ReportableException("Error loading " + getIdentifier(), ex);
            }
            logger.log(Level.WARNING, "Error loading: " + getIdentifier(), ex);
        }
    }

    // This applies any package-level archive annotation. This is required for car.json-based
    // server cars (sbt) because they do not capture archive availability correctly...
    private void applyPackageArchiveAnnotation() {
        if (_packageArchive == null) {
            return;
        }
        if (StringUtils.isEmpty(_archive.name())) {
            _archive.setName(_packageArchive.name());
        }
        if (StringUtils.isEmpty(_archive.version())) {
            _archive.setVersion(_packageArchive.version());
        }
        if (_archive.available()) {
            _archive.setAvailable(_packageArchive.available());
        }
        if (_archive.dependencies().length == 0) {
            _archive.setDependencies(_packageArchive.dependencies());
        }
        if (_archive.suppresses().length == 0) {
            _archive.setSuppresses(_packageArchive.suppresses());
        }
    }

    public static class VirtualComponentAnnotation extends ComponentAnnotation {
        private String _identifier;

        public void setIdentifier(String identifier) {
            _identifier = identifier;
        }

        public String getIdentifier() {
            return _identifier;
        }

        private JsonNode _annotations;

        public void setAnnotations(JsonNode annotations) {
            _annotations = annotations;
        }

        public JsonNode getAnnotations() {
            return _annotations;
        }
    }

    public static class VirtualArchiveAnnotation extends ArchiveAnnotation {
        private String _identifier;

        public void setIdentifier(String identifier) {
            _identifier = identifier;
        }

        public String getIdentifier() {
            return _identifier;
        }

        private List<VirtualComponentAnnotation> _components;

        public void setComponents(List<VirtualComponentAnnotation> components) {
            _components = components;
        }

        public List<VirtualComponentAnnotation> getComponents() {
            return _components;
        }

        private List<String> _dependencies;

        public void setDependencies(List<String> dependencies) {
            _dependencies = dependencies;
        }

        public List<String> getDependencies() {
            return _dependencies;
        }

        // Metadata is a free-form hash of configuration data, primarily for debugging purposes
        private JsonNode _metadata;

        public void setMetadata(JsonNode metadata) {
            _metadata= metadata;
        }

        public JsonNode getMetadata() {
            return _metadata;
        }
    }

    private void processVirtualComponent(VirtualComponentAnnotation annotation, ComponentRing ring) throws Exception {
        String cn = annotation.implementation();
        // ffs
        final String className =
          _allDependencies.stream()
          .map(a -> a.getAliasMap().get(cn))
          .filter(Objects::nonNull)
          .findFirst()
          .map(ComponentDescriptor::getIdentifier)
          .orElse(cn);
        Class<?> clas = ring.getClass(this, className);
        if (clas == null) {
            throw new RuntimeException("Unknown class: " + className);
        }
        Map<Class<? extends Annotation>, Annotation> annotations = new HashMap<>();
        // implicitly inherit resources from my base
        InheritResourcesAnnotation ir = new InheritResourcesAnnotation();
        ir.setValue(clas);
        annotations.put(InheritResources.class, ir);
        if (annotation.getAnnotations() != null) {
            Iterator<Map.Entry<String, JsonNode>> iterator = annotation.getAnnotations().fields();
            while (iterator.hasNext()) {
                Map.Entry<String, JsonNode> entry = iterator.next();
                Class<? extends Annotation> annotationClass = (Class<? extends Annotation>) ring.getClass(this, entry.getKey());
                if (annotationClass == null) {
                    throw new RuntimeException("Unknown annotation class: " + entry.getKey());
                }
                Annotation impl = ComponentUtils.getJsonAnnotation((ClassLoader) getClassLoader(), annotationClass, entry.getValue());
                annotations.put(impl.annotationType(), impl);
            }
        }
        ComponentDescriptor cd = new BaseComponentDescriptor(annotation.getIdentifier(), annotation, clas, annotations, this);
        cd.loadMessages();
        _components.add(cd);
    }

    // for injectable singleton objects, don't look at their autogenerated cocompanion classes
    private boolean isCocompanion(final Class<?> clas) {
        final String companionName = clas.getName().concat("$");
        return (clas.getConstructors().length == 0) && _loadedClasses.stream().anyMatch(c ->
          companionName.equals(c.getName()) && c.isAnnotationPresent(Service.class));
    }

    private void processClass(Class<?> clas) {
        try {
            if ((clas.isAnnotationPresent(Component.class) || clas.isAnnotationPresent(Service.class))
                    && !clas.isInterface()
                    && !isCocompanion(clas)) {
                ComponentDescriptor cd = new BaseComponentDescriptor(clas, this);
                if (cd.getComponentAnnotation().i18n()) {
                    cd.loadMessages();
                }
                synchronized (_components) {
                    _components.add(cd);
                }
                for (String alias: cd.getComponentAnnotation().alias()) {
                    synchronized (_aliasMap) {
                        _aliasMap.put(alias, cd);
                    }
                }
            } else {
                for (Annotation a : clas.getDeclaredAnnotations()) {
                    Binding binding = a.annotationType().getAnnotation(Binding.class);
                    if ((binding != null) && !binding.component()) {
                        synchronized (_boundClasses) {
                            _boundClasses.add(clas);
                        }
                    }
                    ResourceBinding rBinding = a.annotationType().getAnnotation(ResourceBinding.class);
                    if (rBinding != null && !rBinding.component()) {
                        synchronized (_rBoundClasses) {
                            _rBoundClasses.add(Pair.of(a, clas));
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error processing: " + clas.getName(), e);
        }
    }

    @Override
    public boolean isStale() {
        return !(_archive instanceof VirtualArchiveAnnotation) // is this ever not the case?
          || !_archive.buildDate().equals(parseVirtualArchiveAnnotation().buildDate());
    }

    @Override
    public String toString() {
        return "ComponentArchive[" + getIdentifier() + "]";
    }

    interface HF<T> extends Function<HF<T>, T> {
    }

    // the Y combinator
    private static HF<Function<Function<Consumer<ComponentArchive>, Consumer<ComponentArchive>>, Consumer<ComponentArchive>>> Why =
      y -> f -> x -> f.apply(y.apply(y).apply(f)).accept(x);

    // the fixed point
    private static Function<Function<Consumer<ComponentArchive>, Consumer<ComponentArchive>>, Consumer<ComponentArchive>> Fix =
      Why.apply(Why);
}
