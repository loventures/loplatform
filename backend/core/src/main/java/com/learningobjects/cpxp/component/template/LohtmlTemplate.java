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

/*
 * HtmlTemplate
 *
 * Template (variable, conditional, loop) expansion for HTML; uses HtmlFilter
 * (a ContentHandler) to act on special content in the HTML, and Repeater (a
 * Proxy) to replay events to the HtmlFilter to support looping.
 */

package com.learningobjects.cpxp.component.template;

import com.learningobjects.cpxp.BaseWebContext;
import com.learningobjects.cpxp.component.ComponentDescriptor;
import com.learningobjects.cpxp.component.HtmlWriter;
import com.learningobjects.tagsoup.HTMLSchema;
import com.learningobjects.tagsoup.Parser;
import com.learningobjects.tagsoup.Schema;
import com.learningobjects.tagsoup.XMLWriter;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.helpers.LocatorImpl;
import org.xml.sax.helpers.NamespaceSupport;

import java.io.StringReader;
import java.io.Writer;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LohtmlTemplate {
    private static final Logger logger = Logger.getLogger(LohtmlTemplate.class.getName());

    private final URL _url;
    private final String _template;

    private LohtmlTemplate(URL url, String template) {
        _url = url;
        _template = template;
    }

    // Render the file in context.
    private void render(RenderContext context, Schema schema,
            boolean isFragment) throws Exception {
        HtmlWriter htmlWriter = context.getWriter();
        Writer rawWriter = htmlWriter.getWriter();
        XMLWriter xmlWriter = new XMLWriter(rawWriter) {
                @Override
                public void flush() {
                    // temporary hack, in the case of the legacy page layout tag
                    // override, flush causes an invalid operation error.
                }
            };
        xmlWriter.setOutputProperty(XMLWriter.OMIT_XML_DECLARATION, "yes");
        xmlWriter.setOutputProperty(XMLWriter.METHOD, "html");
        xmlWriter.setOutputProperty(XMLWriter.ENCODING, "UTF-8");

        Parser parser = new Parser();
        parser.setFeature(Parser.ignoreBogonsFeature, false);
        parser.setFeature(Parser.defaultAttributesFeature, false);
        if (isFragment) {
            // Disable auto-generation of enclosing <html> etc. so we
            // can process fragments.
            parser.setFeature(Parser.outerImplicitElementsFeature, false);
        }
        parser.setProperty(Parser.schemaProperty, schema);
        parser.setProperty(Parser.nonLowerCasedNamespacesProperty,
                           BaseWebContext.getContext().getComponentEnvironment().getIdentifiers());

        HtmlFilter realFilter = new HtmlFilter(context, xmlWriter, rawWriter, schema.getNamespaceSupport());
        ContentHandler filter = (ContentHandler) Repeater.newInstance(
                realFilter, new Repeater.InvocationArgumentProtector() {
            // Noting all of the methods on ContentHandler, there are two
            // cases where mutable args appear: firstly where we have a
            // char[] followed by a start-index and a length, and secondly
            // where the sole arg is a Locator.  Copy both.
            public void protect(Method m, Object[] args) {
                if (args[0] instanceof char[]) {
                    char[] old = (char[]) args[0];
                    int start = (Integer) args[1];
                    int len = (Integer) args[2];
                    args[0] = new char[len];
                    args[1] = 0;
                    System.arraycopy(old, start, args[0], 0, len);
                } else if (args[0] instanceof Locator) {
                    args[0] = new LocatorImpl((Locator) args[0]);
                }
            }
        });
        realFilter.setRepeater((Repeater)
                Proxy.getInvocationHandler(filter));
        parser.setContentHandler(filter);
        parser.setProperty("http://xml.org/sax/properties/lexical-handler", filter);
        var source = _url != null ? new InputSource(_url.openStream())
          : new InputSource(new StringReader(_template));
        parser.parse(source);
    }

    // Render the file in context.
    public void render(RenderContext context) throws Exception {
        render(context, getSchema(context), false);
    }

    // Render the file as a fragment, with the default HTML namespace
    // already assumed.
    public void renderFragment(RenderContext context) throws Exception {
        render(context, getSchema(context), true);
    }

    private static Schema getSchema(RenderContext context) {
        Schema schema = new HTMLSchema();
        NamespaceSupport ns = schema.getNamespaceSupport();
        ns.declarePrefix("ug", HtmlFilter.UGNS_NAME);
        ns.declarePrefix("self", context.getScope().getIdentifier());
        return schema;
    }

    // Get a (potentially, one day, cached) HtmlTemplate instance for a
    // file.
    public static LohtmlTemplate getInstance(URL url) {
        return new LohtmlTemplate(url, null);
    }

    // Get a (potentially, one day, cached) HtmlTemplate instance for a
    // resource.
    public static LohtmlTemplate getInstanceForResource(String resource) {
        return new LohtmlTemplate(LohtmlTemplate.class.getClassLoader().
                getResource(resource), null);
    }

    public static LohtmlTemplate forTemplate(String template) {
        return new LohtmlTemplate(null, template);
    }

    /** Do not use this. Prefer I18nMessage. */
    @Deprecated
    public static String expandString(String str, ComponentDescriptor scope, Map<String, ?> bindings) {
        RenderContext context = new RenderContext(scope, null, null, bindings, null);
        HtmlFilter filter = new HtmlFilter(context, null, null, null);
        try {
            return filter.expandString(str);
        } catch (Exception ex) {
            logger.log(Level.WARNING, "String expansion error: " + str, ex);
            return "String expanson error: " + ex.getMessage();
        }
    }
}
