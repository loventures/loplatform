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

package loi.cp.ui;

import com.google.common.base.Strings;
import com.learningobjects.cpxp.BaseWebContext;
import com.learningobjects.cpxp.component.HtmlWriter;
import com.learningobjects.cpxp.component.element.AbstractComponentElement;
import com.learningobjects.cpxp.component.element.ComponentElement;
import com.learningobjects.cpxp.component.element.CustomTag;
import com.learningobjects.cpxp.component.util.EmptyHtml;
import com.learningobjects.cpxp.component.util.Html;
import com.learningobjects.cpxp.util.StringUtils;
import org.xml.sax.Attributes;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

public class CoalesceTag implements CustomTag {
    private List<String> _urls = new ArrayList<>();
    private boolean _css, _js;
    private Callable _body;

    public CoalesceTag(Callable body) {
        _body = body;
    }

    @Override
    public void attribute(String name, Object value) {
    }

    @Override
    public void content(Object o, boolean raw) {
    }

    @Override
    public ComponentElement getChild(String name) {
        return new ScriptElement();
    }

    @Override
    public void startElement(String namespaceURI, String localName, Attributes atts) {
        int index = atts.getIndex("src");
        if (index >= 0) {
            String val = atts.getValue(index);
            if (CoreTags.jsDupCheck(val)) {
                _urls.add(val);
            }
            _js = true;
        }
        int jndex = atts.getIndex("href");
        if (jndex >= 0) {
            String val = atts.getValue(jndex);
            if (CoreTags.cssDupCheck(val)) {
                _urls.add(val);
            }
            _css = true;
        }
    }

    public Html render() throws Exception {
        if (_body != null) { // lohtml
            _body.call();
        }
        if (BaseWebContext.getDebug() && false) {
            for (String url : _urls) {
                writeScript(url);
            }
        } else if (!_urls.isEmpty()) {
            String base = null;
            for (String url : _urls) {
                if (base == null) {
                    base = url;
                } else {
                    base = Strings.commonPrefix(base, url);
                }
            }
            base = base.substring(0, 1 + base.lastIndexOf('/')); // in case foo/barone.js, foo/bartwo.js
            writeScript(base + StringUtils.replace(StringUtils.join(_urls, "%7C"), base, "").replace("/", "%5E"));
        }
        return EmptyHtml.instance(); // meh
    }

    private void writeScript(String url) {
        HtmlWriter writer = BaseWebContext.getContext().getHtmlWriter();
        if (_js) {
            writer.startElement("script");
            writer.startAttribute("type");
            writer.write("text/javascript");
            writer.startAttribute("src");
            writer.write(url);
            writer.closeElement();
            writer.endElement("script");
        } else if (_css) {
            writer.startElement("link");
            writer.startAttribute("rel");
            writer.write("stylesheet");
            writer.startAttribute("href");
            writer.write(url);
            writer.closeEndElement();
        }
    }

    private class ScriptElement extends AbstractComponentElement {
        @Override
        public void attribute(String name, Object value) {
            if ("src".equals(name)) {
                _js = true;
                _urls.add((String) value);
            } else if ("href".equals(name)) {
                _css = true;
                _urls.add((String) value);
            }
        }
    }
}

