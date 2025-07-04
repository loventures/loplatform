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

import au.com.bytecode.opencsv.CSVReader;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learningobjects.cpxp.BaseServiceMeta;
import com.learningobjects.cpxp.BaseWebContext;
import com.learningobjects.cpxp.IdType;
import com.learningobjects.cpxp.component.annotation.*;
import com.learningobjects.cpxp.component.internal.BaseDelegateDescriptor;
import com.learningobjects.cpxp.component.internal.ComponentAnnotation;
import com.learningobjects.cpxp.component.internal.DelegateDescriptor;
import com.learningobjects.cpxp.component.internal.FunctionDescriptor;
import com.learningobjects.cpxp.component.registry.BaseResourceRegistryContainer;
import com.learningobjects.cpxp.component.registry.ResourceRegistry;
import com.learningobjects.cpxp.component.registry.ResourceRegistryContainer;
import com.learningobjects.cpxp.component.util.ComponentUtils;
import com.learningobjects.cpxp.util.Encheferize;
import com.learningobjects.cpxp.util.message.BaseMessageMap;
import com.learningobjects.cpxp.util.message.MessageMapCompositor;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.BinaryOperator;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.learningobjects.cpxp.util.StringUtils.defaultIfEmpty;
import static com.learningobjects.cpxp.util.StringUtils.toLowerCaseFirst;
import static org.apache.commons.lang3.StringUtils.stripStart;
import static org.apache.commons.lang3.StringUtils.substringBeforeLast;

public class BaseComponentDescriptor  implements ComponentDescriptor {
    private static final Logger logger = Logger.getLogger(BaseComponentDescriptor.class.getName());

    private final ComponentArchive _archive;
    private final String _identifier;
    private final Class<?> _class;
    private final Component _component;
    private final boolean _stateless;
    private final Class<? extends ComponentInterface> _category;
    private final DelegateDescriptor _delegate;

    /**
     * Powers the transmutation from one component type to another via {@link ComponentInterface#asComponent(Class, Object...)}. The keyset is the set of component
     * types that this descriptor can be {@code asComponent}-ed to. The values are the descriptors used to back the {@link
     * ComponentInvocationHandler}s of the proxies created by {@code asComponent}.
     */
    private final Map<Class<? extends ComponentInterface>, DelegateDescriptor> _interfaces = new HashMap<>();
    private final Map<String, ConfigurationDescriptor> _configurations = new LinkedHashMap<>();
    private BaseMessageMap _defaultMessages;
    private MessageMapCompositor _messages;
    private final List<FunctionDescriptor> _functionList = new ArrayList<>();

    /**
     * Fake annotations associated with a virtual component.
     */
    private final Map<Class<? extends Annotation>, Annotation> _annotations;

    /**
     * Resources related to this component, see {@link BaseResourceRegistryContainer}
     */
    private final ResourceRegistryContainer _resourceRegistryContainer = new BaseResourceRegistryContainer();

    public BaseComponentDescriptor(Class<?> clas, ComponentArchive archive) {
        this(clas.getName(), getComponentAnnotation(clas), clas, null, archive);
    }

    public BaseComponentDescriptor(String identifier, Component component, Class<?> clas,
                                   Map<Class<? extends Annotation>, Annotation> annotations, ComponentArchive archive) {
        _identifier = identifier;
        _class = clas;
        _archive = archive;
        _component = component;
        _category = getCategory(clas);
        _annotations = annotations;
        _delegate = new BaseDelegateDescriptor(this, clas);
        _stateless = _class.isAnnotationPresent(Stateless.class) ||
          ((_category != null) && _category.isAnnotationPresent(Stateless.class));
    }

    @Override
    public Class<? extends ComponentInterface> getCategory() {
        return _category;
    }

    @Override
    public Class<?> getComponentClass() {
        return _class;
    }

    @Override
    public ComponentArchive getArchive() {
        return _archive;
    }

    @Override
    public Component getComponentAnnotation() {
        return _component;
    }

    @Override
    public Set<Class<? extends ComponentInterface>> getInterfaces() {
        return _interfaces.keySet();
    }

    @Override
    public boolean isStateless() {
        return _stateless;
    }

    @Override @Nonnull
    public DelegateDescriptor getDelegate() {
        return _delegate;
    }

    @Override
    public Collection<FunctionDescriptor> getFunctionDescriptors() {
        return Collections.unmodifiableList(_functionList);
    }

    @Override
    public ResourceRegistryContainer getResourceRegistryContainer() {
        return _resourceRegistryContainer;
    }

    @Override
    public Map<String, ConfigurationDescriptor> getConfigurations() {
        return _configurations;
    }

    @Override
    public Iterable<Annotation> getVirtualAnnotations() {
        return (_annotations == null) ? new ArrayList<>() : _annotations.values();
    }

    @Override
    public <T extends Annotation> T getAnnotation(Class<T> type) {
        if ((_annotations != null) && _annotations.containsKey(type)) {
            return type.cast(_annotations.get(type));
        }
        return _class.getAnnotation(type);
    }

    @Override
    public String getIdentifier() {
        return _identifier;
    }

    @Override
    public Iterable<String> getIdentifiers() {
        List<String> identifiers = new ArrayList<>();
        identifiers.add(_identifier);
        Collections.addAll(identifiers, _component.alias());
        return identifiers;
    }

    @Override
    public String getVersion() {
        return _component.version();
    }

    @Override
    public URL getResource(String path) {
        try {
            // I don't use the classloader because that does not see hot-deployed assets.
            String packageName = substringBeforeLast(_identifier, ".");
            String global = packageName.replace('.', '/') + '/' + path;
            Optional<Path> resource = _archive.getSource().getResource(global);
            if (resource.isPresent() && Files.exists(resource.get())) {
                URL origURL = resource.get().toUri().toURL();
                //Hack to work around this: https://bugs.openjdk.java.net/browse/JDK-8172846
                //Cousier stores jars with a URL encoded path segment, ends up being broken
                //Shouldn't affect production. Burn everything down if it does.
                if(origURL.getProtocol().equals("jar")) {
                    return new URL(URLDecoder.decode(origURL.toExternalForm(), StandardCharsets.UTF_8));
                } else {
                    return origURL;
                }
            }
            // TODO: lookup the inherited component itself, don't use its class...
            // but what about components that are off or suppressed or...
            InheritResources ir = getAnnotation(InheritResources.class);
            while (ir != null) {
                Class<?> clas = ir.value();
                URL url = clas.getResource(path);
                if (url != null) {
                    return url;
                }
                ir = clas.getAnnotation(InheritResources.class);
            }
            return null;
        } catch (Exception ex) {
            throw new RuntimeException("Resource error: " + path, ex);
        }
    }

    @Override
    public String getMessage(Locale locale, String key) {
        /*if (Encheferize.SWEDISH_CHEF().equals(locale)) {
            return Encheferize.apply(key);
        } else if (Encheferize.ENGLISH_REVERSE().equals(locale)) {
            return Encheferize.reverse(key);
        } else*/ if (_messages != null) {
            return _messages.getCompositeMap(locale).getMessage(key);
        } else {
            return null;
        }
    }

    @Override
    public Map<Locale, ? extends Map<String, String>> getAvailableMessages() {
        return (_messages == null) ? Collections.emptyMap() : _messages.getAvailableMessages();
    }

    @Override
    public boolean isSupported(Class<? extends ComponentInterface> iface) {
        if (iface.isInterface()) {
            return _interfaces.containsKey(iface);
        } else {
            DelegateDescriptor delegate = getDelegate();
            return iface.isAssignableFrom(delegate.getDelegateClass());
        }
    }

    @Override
    public ComponentInstance getInstance(ComponentEnvironment env, IdType item, Long context, Object... args) {
        return new ComponentInstanceImpl(env, this, item, context, args);
    }

    @Override
    public ComponentInstance getInstance(IdType item, Long context, Object... args) {
        return getInstance(BaseWebContext.getContext().getComponentEnvironment(), item, context, args);
    }

    // Initial component analysis

    private static final Pattern MESSAGE_RE = Pattern.compile("(?s)\\$\\$([a-zA-Z_][a-zA-Z0-9_.]*)=(.*)");

    private void createCompositor() {
        if (_messages == null) {
            _messages = new MessageMapCompositor();
            _defaultMessages = new BaseMessageMap(Locale.ENGLISH, TimeZone.getDefault(), new Properties());
            _messages.addMessageMap(_defaultMessages);
            _messages.setDefaultLocale(Locale.ENGLISH);
        }
    }

    @Override
    public void addMessage(String message)  {
        Matcher matcher = MESSAGE_RE.matcher(message);
        if (matcher.matches()) { // allow non-internationalized strings in message fields
            createCompositor();
            String key = matcher.group(1), value = matcher.group(2);
            if (!_defaultMessages.containsKey(key)) {
                // this is to allow a component to override a foreign delegate's messages. it should throw on dup.
                _defaultMessages.put(key, value);
                enchef(key, value);
            }
        }
    }

    private void enchef(String key, String value) {
        if (!BaseServiceMeta.getServiceMeta().isProdLike()) {
            Encheferize.translate(key, value, (locale, translated) -> {
                _messages.getOrCreateMessageMap(locale, TimeZone.getDefault()).put(key, translated);
                return null;
            });
        }
    }

    @Override
    public void loadMessages() {
        createCompositor();
        String i18nDirName = substringBeforeLast(_identifier, ".").replace(".", "/");
        try {
            Path dir = _archive.getSource().getResource(i18nDirName).orElse(null);
            if (dir == null || !Files.isDirectory(dir)) {
                logger.log(Level.FINE, "Skipping i18n for " + this + " as " + dir + " is not a directory");
                return;
            }

            Files.find(dir, 1, (f, o) -> Files.isRegularFile(f))
              .map(f -> {
                  if (f.isAbsolute()) {
                      return dir.toAbsolutePath().relativize(f);
                  } else {
                      return dir.relativize(f);
                  }
              })
              .forEach(path -> {
                  Matcher m = I18N_RE.matcher(path.getFileName().toString());
                  if (m.matches()) {
                      logger.log(Level.INFO, "Adding i18n file " + dir.resolve(path));
                      final String languageTag = m.group(1);
                      final Locale locale = Locale.forLanguageTag(languageTag);
                      try {
                          final Map<String, String> messages = loadMessages(i18nDirName + "/" + path);
                          addMessages(locale, messages);
                      } catch (Exception ex) {
                          logger.log(Level.WARNING, "I18n error", ex);
                      }
                  }
              });
        } catch (Exception ex) {
            logger.log(Level.WARNING, "I18n error", ex);
        }
    }

    /**
     * Find messages by file extension or by attempting to deserialize the contents from JSON.
     */
    private Map<String, String> loadMessages(final String relative) throws Exception {
        final Optional<Map<String, String>> messages =
          _archive.getSource().getResource(relative)
            .flatMap(resource -> {
                if (resource.toString().endsWith(".properties")) {
                    return loadProps(resource);
                } else if (resource.toString().endsWith(".csv")) {
                    return loadCsv(resource);
                } else {
                    return loadJson(resource);
                }
            });
        return messages.orElseGet(Collections::emptyMap);
    }

    private Optional<Map<String, String>> loadProps(Path path) {
        Properties props = new Properties();
        try (InputStream inputStream = Files.newInputStream(path)) {
            props.load(inputStream);
            @SuppressWarnings("unchecked")
            Map<String, String> propMap = (Map<String, String>) (Map) props;
            return Optional.of(propMap);
        } catch (IOException ioe) {
            logger.log(Level.SEVERE, "Can't load translations from " + path, ioe);
            return Optional.empty();
        }
    }

    private Optional<Map<String, String>> loadCsv(final Path path) {
        try (CSVReader reader = new CSVReader(new InputStreamReader(Files.newInputStream(path), StandardCharsets.UTF_8))) {
            BinaryOperator<String> rightMerger = (l, r) -> r;
            //noinspection unchecked
            return Optional.of(reader.readAll().stream()
              .filter(a -> a.length >= 2 && !StringUtils.isEmpty(a[0]))
              .collect(Collectors.toMap(a -> a[0], a -> a[1], rightMerger)));
        } catch (IOException ioe) {
            logger.log(Level.SEVERE, "Can't load translations from " + path, ioe);
            return Optional.empty();
        }
    }

    @SuppressWarnings("unchecked")
    private Optional<Map<String, String>> loadJson(Path path) {
        try {
            ObjectMapper mapper = ComponentUtils.getObjectMapper();
            return Optional.of((Map<String, String>) mapper.readValue(Files.readAllBytes(path), Map.class));
        } catch (IOException ioe) {
            logger.log(Level.SEVERE, "Can't load translations from " + path, ioe);
            return Optional.empty();
        }
    }

    private <T> Optional<T> or(Optional<T> first, Supplier<Optional<T>> other) {
        if(first.isPresent()) {
            return first;
        } else {
            return other.get();
        }
    }

    private void addMessages(final Locale locale, final Map<String, String> messages) {
        _messages.getOrCreateMessageMap(locale, TimeZone.getDefault()).putAll(messages);
        if (locale.equals(Locale.ENGLISH)) {
            for (Map.Entry<String, String> entry : messages.entrySet()) {
                enchef(entry.getKey(), entry.getValue());
            }
        }
    }

    private static final Pattern I18N_RE = Pattern.compile(".*_([a-z][a-z](?:-[A-Z]+)?).(csv|json|properties)");

    // should be private, but has to be called by {@link DelegateDescriptor}
    @Override
    public void addFunction(FunctionDescriptor function) {
        _functionList.add(function);
    }


    /**
     * Called by introspection code of delegates when a delegate expresses a resource to add to this
     * namespace/componentDescriptor.
     */
    @Override
    public void addResource(final Annotation annotation, final Class<?> clazz) {

        final Class<? extends Annotation> annotationType = annotation.annotationType();

        final ResourceRegistry<?> registry = _resourceRegistryContainer.getRegistry(annotationType);

        registry.register(annotation, clazz);
    }

    @Override
    public void addConfiguration(Method member) { // Internal
        Configuration configuration = member.getAnnotation(Configuration.class);
        ConfigurationDescriptor descriptor = ConfigurationDescriptor.apply(
          configuration,
          defaultIfEmpty(configuration.name(), toLowerCaseFirst(stripStart(member.getName(), "get"))),
          defaultIfEmpty(configuration.type(), member.getReturnType().getSimpleName())
        );
        if (_configurations.containsKey(descriptor.getName())) {
            throw new RuntimeException("Duplicate configuration definition: " + member);
        }
        _configurations.put(descriptor.getName(), descriptor);
    }

    @Override
    public boolean addInterface(Class<? extends ComponentInterface> iface, DelegateDescriptor delegate) {
        if (!_interfaces.containsKey(iface)) {
            _interfaces.put(iface, delegate);
            for (Class<?> i : iface.getInterfaces()) {
                if (ComponentInterface.class.isAssignableFrom(i)) {
                    addInterface((Class<? extends ComponentInterface>) i, delegate);
                }
            }
            return true;
        }
        return false;
    }

    // your category is the first component interface you or an ancestor implements
    private static Class<? extends ComponentInterface> getCategory(Class<?> clas) {
        for (Class<?> c = clas; c != null; c = c.getSuperclass()) {
            for (Class<?> iface : c.getInterfaces()) {
                if (ComponentInterface.class.isAssignableFrom(iface)) {
                    return (Class<? extends ComponentInterface>) iface;
                }
            }
        }
        return null;
    }

    private static Component getComponentAnnotation(Class<?> clas) {
        Component component = clas.getAnnotation(Component.class);
        if (component == null) {
            Service service = clas.getAnnotation(Service.class);
            if (service != null) {
                // for standalone services we return a dummy annotation
                ComponentAnnotation annotation = new ComponentAnnotation();
                annotation.setDependencies(service.dependencies());
                annotation.setSuppresses(service.suppresses());
                annotation.setEnabled(service.enabled());
                component = annotation;
            }
        }
        return component;
    }

    @Override
    public String toString() {
        return "ComponentDescriptor[" + _identifier + "]";
    }

}

