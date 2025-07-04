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

package com.learningobjects.cpxp.component.util;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.learningobjects.cpxp.BaseServiceMeta;
import com.learningobjects.cpxp.BaseWebContext;
import com.learningobjects.cpxp.Url;
import com.learningobjects.cpxp.component.*;
import com.learningobjects.cpxp.component.internal.FunctionDescriptor;
import com.learningobjects.cpxp.component.web.util.ExtendedISO8601DateFormat;
import com.learningobjects.cpxp.controller.domain.DomainAppearance;
import com.learningobjects.cpxp.dto.Facade;
import com.learningobjects.cpxp.jackson.ExceptionModule;
import com.learningobjects.cpxp.json.NashornModule;
import com.learningobjects.cpxp.scala.json.DeScalaModule;
import com.learningobjects.cpxp.scala.json.OptionalFieldModule;
import com.learningobjects.cpxp.util.*;
import com.learningobjects.cpxp.util.lang.OptionLike;
import com.learningobjects.cpxp.util.lang.ProviderLike;
import com.learningobjects.cpxp.util.message.MessageMap;
import org.apache.commons.lang3.ArrayUtils;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ComponentUtils {
    private static final Logger logger = Logger.getLogger(ComponentUtils.class.getName());

    public static Object dereference0(Object bean, String property) throws
            PropertyAccessorException {
        if (null == bean) {
            return null;
        }
        if (OptionLike.isOptionLike(bean.getClass())) {
            return dereference0(OptionLike.getOrNull(bean), property);
        }
        if (ProviderLike.isProviderLike(bean.getClass())) {
            return dereference0(ProviderLike.gimme(bean), property);
        }
        return PropertyAccessor.getInstance(bean.getClass(), property).get(bean, property);
    }

    public static Object  dereference(Object bean, String... properties) throws
            PropertyAccessorException {
        for (String property : properties) {
            bean = dereference0(bean, property);
        }
        return bean;
    }

    /**
     * Convert a value into a token that will not be JSON encoded when emitted
     * in a verbatim section.
     */
    public static ScriptToken asToken(Object value) {
        return new ScriptToken(value);
    }

    public static HtmlWriter getHtmlWriter() {
        return BaseWebContext.getContext().getHtmlWriter();
    }

    public static String expandMessage(String value, ComponentDescriptor scope) { // $ffoo=bar
        if (StringUtils.isEmpty(value)) {
            return null;
        }
        int i = value.indexOf('=', 2);
        String msg = value.substring(2, (i < 0) ? value.length() : i);
        return getMessage(msg, scope);
    }


    public static String getMessageKey(String msg, ComponentDescriptor scope) {
        return scope.getIdentifier() + '/' + msg;
    }

    public static String getMessage(String msg, ComponentDescriptor scope) {
        return getMessage(msg, null, scope);
    }

    public static String getMessage(String msg, String val, ComponentDescriptor scope) {
        if (msg.startsWith("url/") || msg.startsWith("localUrl/") || msg.startsWith("cdn/") || msg.startsWith("static/") || msg.startsWith("rpc/") || msg.startsWith("api/")) {
            int slash = msg.indexOf('/');
            return formatMessage(msg.substring(0, slash), scope, msg.substring(1 + slash));
        }
        MessageMap messages = InternationalizationUtils.getMessages();
        String qualified = scope.getIdentifier() + '/' + msg;
        if ("Language".equals(msg)) { // support for $$Language expanding current locale language
            return messages.getLocale().getLanguage();
        } else if ("Country".equals(msg)) { // support for $$Country expanding current locale language
            return messages.getLocale().getCountry();
        } else if ("Locale".equals(msg)) {
            /* approximately $$Language-$$Country but more precise */
            return messages.getLocale().toLanguageTag();
        } else {
            String message = messages.getMessage(qualified); // loi.cp.ui.UI/foobar_action_delete
            if (message == null) {
                message = messages.getMessage(msg); // foobar_action_delete.. meh
                if (message == null) {
                    message = scope.getMessage(messages.getLocale(), msg); // foobar_action_delete
                    if (message == null) {
                        message = val;
                        if (message == null) {
                            message = "???" + msg + "???";
                        }
                    }
                }
            }
            return message;
        }
    }

    // This looks up i18n of "The Wiki: {0}" returning either the input
    // or the translated "Blah {0} blah".
    public static String i18n(String msg, ComponentDescriptor scope) { // TODO: this is horribly inefficient
        String key = msg.replaceAll("[\\s]+", "_");
        String qualified = scope.getIdentifier() + '/' + key;
        MessageMap messages = InternationalizationUtils.getMessages();
        String message = null;
        if (messages != null) {
            message = messages.getMessage(qualified); // loi.cp.ui.UI/This_is_a_message.
            if (message == null) {
                message = messages.getMessage(key); // This_is_a_message from core.
                if (message == null) {
                    final Locale locale = messages.getLocale();
                    message = scope.getMessage(locale, key); // This_is_a_message from scope.
                    if (message == null) {
                        message = BaseServiceMeta.getServiceMeta().isProdLike() ? null : Encheferize.translate(msg, locale);
                    }
                }
            }
        }
        return StringUtils.defaultString(message, msg);
    }

    public static String formatMessage(String msg, ComponentDescriptor scope, Object... parameters) {
        if ("url".equals(msg) || "localUrl".equals(msg)) {
            String name;
            if (parameters.length == 1) {
                name = (String) parameters[0];
            } else if (parameters[0] instanceof Class<?>) {
                Class<?> componentClass = (Class<?>) parameters[0];
                scope = ComponentSupport.getComponentDescriptor(componentClass.getName());
                name = (String) parameters[1];
            } else {
                String identifier = (String) parameters[0];
                scope = ComponentSupport.getComponentDescriptor(identifier);
                name = (String) parameters[1];
            }
            return resourceUrl(name, scope, "url".equals(msg));
        } else if ("cdn".equals(msg)) {
            ComponentDescriptor component = ComponentSupport.getComponentDescriptor("cdn.Cdn");
            String name = (String) parameters[0];
            return resourceUrl(name, component);
        } else if ("rpc".equals(msg)) {
            if (parameters.length == 1) {
                String name = (String) parameters[0];
                return rpcUrl(scope, name);
            } else if (parameters[0] instanceof Class<?>) {
                Class<?> componentClass = (Class<?>) parameters[0];
                ComponentDescriptor component = ComponentSupport.getComponentDescriptor(componentClass.getName());
                return rpcUrl(component, (String) parameters[1]);
            } else if (parameters[0] instanceof String) {
                String identifier = (String) parameters[0];
                ComponentDescriptor component = ComponentSupport.getComponentDescriptor(identifier);
                return rpcUrl(component, (String) parameters[1]);
            } else {
                String url = ((Url) parameters[0]).getUrl(), name = (String) parameters[1];
                if (url == null) {
                    return rpcUrl(scope, name);
                }
                return url + "!" + name;
            }
        } else if ("json_api".equals(msg)) {
            // TODO: Make these functions component registered rather than this hack..
            try {
                return FormattingUtils.voidTags((String) ComponentSupport.getFn("srs", "jsonapi").invoke(StringUtils.join(parameters)));
            } catch (Exception ex) {
                logger.log(Level.WARNING, "API error", ex);
                return "{ \"status\": \"error\" }"; // omg
            }
        } else if ("static".equals(msg)) {
            return FormattingUtils.staticUrl((String) parameters[0]);
        } else if ("json".equals(msg)) {
            return json(parameters[0]);
        } else if ("i18n".equals(msg)) {
            return i18n((String) parameters[0], scope);
        } else if ("jsoni18n".equals(msg)) {
            return json(i18n((String) parameters[0], scope));
        } else if ("dexss".equals(msg)) {
            return dexss((String) parameters[0]);
        } else if ("html".equals(msg)) {
            return html((String) parameters[0]);
        } else if ("disabled".equals(msg)) {
            return asDisabled(parameters[0]);
        } else if ("selected".equals(msg)) {
            return asSelected(parameters[0]);
        } else if ("checked".equals(msg)) {
            return asChecked(parameters[0]);
        } else if ("readonly".equals(msg)) {
            return asReadonly(parameters[0]);
        } else if ("contentName".equals(msg)) {
            return FormattingUtils.contentName((Facade) parameters[0]);
        } else {
            int star;
            while ((star = msg.indexOf('*')) >= 0) { // ok, so it's not a star.
                msg = msg.substring(0, star) + parameters[0] + msg.substring(1 + star);
                parameters = ArrayUtils.remove(parameters, 0);
            }
            return InternationalizationUtils.format(getMessage(msg, scope), parameters);
        }
    }

    public static String json(Object param) {
        try {
            return FormattingUtils.voidTags(toJson(param));
        } catch (Exception ex) {
            throw new RuntimeException("Json error", ex);
        }
    }

    public static String dexss(String s) {
        try {
            return DeXssSupport.deXss(s, "$$dexss");
        } catch (Exception ex) {
            throw new RuntimeException("Dexss error", ex);
        }
    }

    public static String html(String s) { // rewrites html to well-formed for embedding in valid html
        try {
            return DeXssSupport.wellFormed(s);
        } catch (Exception ex) {
            throw new RuntimeException("Well formed error", ex);
        }
    }

    private static String rpcUrl(ComponentDescriptor scope, String name) {
        if ("".equals(name)) {
            return "/control/component/" + scope.getIdentifier() + "/";
        }
        // TODO: This looks up GET only... How to lookup anything???
        FunctionDescriptor rpc =
          ComponentSupport.getEnvironment().getRegistry().getFunction(scope, RpcInstance.class, name);
        if ((rpc != null) && StringUtils.isNotEmpty(rpc.getBinding())) {
            return rpc.getBinding();
        } else {
            return "/control/component/" + scope.getIdentifier() + "/" + name;
        }
    }

    public static String resourceUrl(String resource, ComponentDescriptor scope) {
        return resourceUrl(resource, scope, true, false, false);
    }

    public static String resourceUrl(String resource, ComponentDescriptor scope, boolean cdn) {
        return resourceUrl(resource, scope, cdn, false, false);
    }

    public static String resourceUrl(String resource, ComponentDescriptor scope, boolean cdn, boolean localize, boolean gzip) {
        if (resource == null ||  scope == null) {
            return null;
        } else {
            StringBuilder sb = FormattingUtils.staticPrefix(cdn);
            sb.append(scope.getIdentifier())
              .append('_')
              .append(CdnUtils.cdnSuffix(BaseServiceMeta.getServiceMeta(), ManagedUtils.getEntityContext()));
            if (resource.endsWith(".sass") || resource.endsWith(".scss")) {
                sb.append("-").append(ComponentSupport.lookupService(DomainAppearance.class).getStyleHash());
            }
            sb.append('-').append(scope.getArchive().getLastModified()); // TODO: always? md5/truncate it?
            localize = localize || resource.endsWith(".js") && !"cdn.Cdn".equals(scope.getIdentifier()); // cdn hack
            gzip = gzip || resource.endsWith(".js") || resource.endsWith(".css");
            if (gzip && HttpUtils.supportsCompression(BaseWebContext.getContext().getRequest())) {
                sb.append("-gz");
            }
            if (localize) {
                sb.append('-');
                sb.append(InternationalizationUtils.getLocaleSuffix());
            }
            if (!resource.startsWith("/")) {
                sb.append('/');
            }
            sb.append(resource);
            return sb.toString();
        }
    }

    public static void propertySort(List<?> items, String properties) {
        Collections.sort(items, new PropertyIgnoreCaseComparator<>(properties, true));
    }

    public static ScriptIterator<?> iterator(Object o) {
        return ScriptIterator.getInstance(o);
    }

    public static List<String> split(String str) {
        return (str == null) ? null : Arrays.asList(str.split(","));
    }

    // TODO: This will likely leak if it uses non-weak maps to cache its stuff. Use an archive-specific mapper.
    private static final ObjectMapper __mapper;

    private static final ObjectMapper __mapperIgnoreUnknownProperties;

    static {
        __mapper = new ObjectMapper();
        __mapper.configure(SerializationFeature.CLOSE_CLOSEABLE, false);
        __mapper.configure(SerializationFeature.INDENT_OUTPUT, BaseWebContext.getDebug());
        __mapper.setDateFormat(new ExtendedISO8601DateFormat());
        __mapper.registerModule(new Jdk8Module());
        __mapper.registerModule(new GuavaModule());
        __mapper.registerModule(new DeScalaModule());
        __mapper.registerModule(new NashornModule());
        __mapper.registerModule(new OptionalFieldModule());
        __mapper.registerModule(new JavaTimeModule());
        __mapper.registerModule(new ExceptionModule(!BaseServiceMeta.getServiceMeta().isProdLike()));

        __mapperIgnoreUnknownProperties = __mapper.copy();
        __mapperIgnoreUnknownProperties.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public static ObjectMapper getObjectMapper() {
        return __mapper;
    }

    public static ObjectMapper getObjectMapperIup() {
        return __mapperIgnoreUnknownProperties;
    }

    public static void toJson(Object o, Writer writer) throws Exception {
        getObjectMapper().writeValue(writer, o);
    }

    public static String toJson(Object o) {
        try {
            StringWriter sw = new StringWriter();
            getObjectMapper().writeValue(sw, o);
            return sw.toString();
        } catch (Exception ex) {
            throw new RuntimeException("JSON error", ex);
        }
    }

    public static JsonNode toJsonNode(Object o) throws Exception {
        //Also, see FacadeJsonHandler.invoke
        //this funnyness is because mapper.valueToTree uses the TokenBuffer implementation
        //  of JsonGenerator, which doesn't support the .writeRaw(String) method, which is used by JsonLiftModule
        //  JsonLiftModule could be changed to use supported methods, but it will be complicated.
        //  so for now, we just change to string first, then to tree
        //It also may be possible in the future to use JsonEncode if/when that is implemented as a
        // Jackson Module that gets configured with our default mapper
        String jsonValue = getObjectMapper().writeValueAsString(o);
        return getObjectMapper().readTree(jsonValue);
    }

    public static <T> T fromJson(String s, JavaType type) throws IOException {
        return __mapper.readValue(s, type);
    }

    public static <T> T fromJson(JsonNode node, JavaType type) throws IOException {
        return __mapper.readValue(__mapper.treeAsTokens(node), type);
    }

    public static <T> T fromJson(JsonNode node, Class<T> clas) throws IOException {
        return getObjectMapper().treeToValue(node, clas);
    }

    public static <T> T fromJson(String json, Class<T> clas) throws IOException, JsonParseException, JsonMappingException {
        return (json == null) ? null : getObjectMapper().readValue(json, clas);
    }

    /**
     * @param defaultInstance  used if the json string is blank and used to get the class for the ObjectMapper
     */
    public static <T> T fromJsonWithDefault(String json, Class<T> clas, T defaultInstance) throws IOException, JsonParseException, JsonMappingException {
        return StringUtils.isBlank(json) ? defaultInstance : getObjectMapper().readValue(json, clas);
    }

    public static <T> T fromJson(Reader json, Class<T> clas) throws IOException, JsonParseException, JsonMappingException {
        return (json == null) ? null : getObjectMapper().readValue(json, clas);
    }

    public static <T extends Annotation> T getJsonAnnotation(final ClassLoader cl, final Class<T> annotationClass, final JsonNode value) {
        return annotationClass.cast(Proxy.newProxyInstance(cl, new Class[] { annotationClass }, (proxy, method, args) -> {
            switch (method.getName()) {
              case "equals":
                  return proxy.equals(args[0]);
              case "hashCode":
                  return proxy.hashCode();
              case "toString":
                  return value.toString();
              case "annotationType":
                  return annotationClass;
              default:
                  JsonNode node = value.get(method.getName());
                  Class<?> type = method.getReturnType();
                  Object dflt = getAnnotationDefault(annotationClass, method.getName());
                  if (node == null && dflt != null) {
                      return dflt;
                  } else if (Boolean.TYPE == type) {
                      return (node != null) && node.asBoolean();
                  } else if (Integer.TYPE == type) {
                      return (node == null) ? 0 : node.asInt();
                  } else if (Long.TYPE == type) {
                      return (node == null) ? 0L : node.asLong();
                  } else if (Double.TYPE == type) {
                      return (node == null) ? 0d : node.asDouble();
                  } else if (Class.class.equals(type)) {
                      return (node == null) ? null : ComponentSupport.loadClass(node.asText());
                  } else if (Enum.class.isAssignableFrom(type)) {
                      return (node == null) ? null : Enum.valueOf((Class<? extends Enum>) type, node.asText());
                  } else if (type.isArray()) {
                      Class<?> subtype = type.getComponentType();
                      int size = (node == null) ? 0 : node.size();
                      Object array = Array.newInstance(subtype, size);
                      for (int i = 0; i < size; ++i) {
                          JsonNode inode = node.get(i);
                          Object value1 = Class.class.equals(subtype) ? ComponentSupport.loadClass(inode.asText()) : inode.asText();
                          Array.set(array, i, value1);
                      }
                      return array;
                  } else if (Annotation.class.isAssignableFrom(type)) {
                      return (node == null) ? null : getJsonAnnotation(cl, (Class<? extends Annotation>) type, node);
                  } else {
                      return (node == null) ? null : node.asText();
                  }
            }
        }));
    }

    private static Object getAnnotationDefault(Class<?> annotationClass, String propertyName) {
        try {
            return annotationClass.getDeclaredMethod(propertyName).getDefaultValue();
        } catch (NoSuchMethodException nsme) {
            return null; // meh
        }
    }

    /** Test if an object is truthy - if a Boolean, that it is true;
     * if it is a Number, that it is nonzero; if a String, that it
     * is nonempty; if it is a celloction, that it is empty; or
     * otherwise if it is non-null. */
    public static boolean test(Object o) {
        return o != null && ((o instanceof Boolean) ? Boolean.TRUE.equals(o)
          : (o instanceof Number) ? (((Number) o).longValue() != 0L)
          : (o instanceof CharSequence) ? (((CharSequence) o).length() > 0)
          : (o instanceof Collection) ? (!((Collection) o).isEmpty())
          : (o instanceof Map) ? (!((Map) o).isEmpty())
          : (o instanceof ScriptIterator) ? (((ScriptIterator) o).hasNext())
          : (o instanceof ScriptToken) ? (!((ScriptToken) o).isBlank())
          : !o.getClass().isArray() || (Array.getLength(o) > 0));
    }

    public static String asDisabled(Object object) {
        return test(object) ? "disabled" : null;
    }

    public static String asChecked(Object object) {
        return test(object) ? "checked" : null;
    }

    public static String asReadonly(Object object) {
        return test(object) ? "readonly" : null;
    }

    public static String asSelected(Object object) {
        return test(object) ? "selected" : null;
    }

    /** Returns the first object that is truthy, or null. In the case of strings, that is the
     * first that is nonempty. */
    public static <T> T or(T... os) {
        for (T o : os) {
            if (test(o)) {
                return o;
            }
        }
        return null;
    }

    public static String render(final Renderable renderable) {
        return HtmlOps.render(new DynamicHtml(renderable));
    }

    /**
     * Searches for an annotation of the given annotation type on {@code component} and its super types. The first
     * annotation found is returned, or {@link Optional#empty()} if no such annotation exists. The search begins at the
     * narrowest type of {@code component} as defined by {@link ComponentSupport#getNarrowestComponentInterface} and
     * proceeds in the order of {@link ClassUtils#findAnnotation(Class, Class)}.
     *
     * @param component the component whose super types are searched (the narrowest type is used to start the search)
     * @param annotationType the type of annotation to find
     * @param <A> the type of annotation to find
     * @return the first annotation of the given type in {@code component}'s inheritance tree,
     * or {@link Optional#empty()} if no such annotation exists.
     */
    public static <A extends Annotation> Optional<A> findAnnotation(@Nonnull final ComponentInterface component,
            @Nonnull final Class<A> annotationType) {

        final Class<? extends ComponentInterface> narrowestType = ComponentSupport.getNarrowestComponentInterface
                (component);

        return ClassUtils.findAnnotation(narrowestType, annotationType);

    }
}
