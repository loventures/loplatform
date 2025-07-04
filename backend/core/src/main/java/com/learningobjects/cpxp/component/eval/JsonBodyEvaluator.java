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

package com.learningobjects.cpxp.component.eval;

import com.learningobjects.cpxp.component.ComponentInstance;
import com.learningobjects.cpxp.component.annotation.JsonBody;
import com.learningobjects.cpxp.component.internal.DelegateDescriptor;
import com.learningobjects.cpxp.component.transform.JsonDecoder;
import com.learningobjects.cpxp.util.MimeUtils;
import com.learningobjects.cpxp.util.StringUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.Reader;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.logging.Level;
import java.util.logging.Logger;

@Evaluates(JsonBody.class)
public class JsonBodyEvaluator extends AbstractEvaluator {
    private static final Logger logger = Logger.getLogger(JsonBodyEvaluator.class.getName());
    private static final String JSON_BODY_ATTRIBUTE = "#json-body";
    private static final String JSON_ARRAY_ATTRIBUTE = "#json-array";

    private JsonDecoder _decoder;

    @Override
    public void init(DelegateDescriptor delegate, String name, Type type, Annotation[] annotations) {
        super.init(delegate, name, type, annotations);
        _decoder = new JsonDecoder(type);
    }

    protected void init(DelegateDescriptor delegate, String name, Class<?> cls, Type type, Annotation[] annotations) {
        super.init(delegate, name, type, annotations);
        _decoder = new JsonDecoder(cls);
    }

    @Override
    public final Object decodeValue(ComponentInstance instance, Object object, HttpServletRequest request) {
        try {
            return decodeValue(request);
        } catch (Exception e) {
            throw new RuntimeException("While decoding @JsonBody on " + instance, e);
        }
    }

    protected Object decodeValue(HttpServletRequest request) throws IOException {
        Object value = request.getAttribute(JSON_BODY_ATTRIBUTE);
        if ((value == null) && !"GET".equals(request.getMethod()) && !"HEAD".equals(request.getMethod())) { // This is to support JSON body decode within optimistic retry
            Reader reader = request.getReader();
            final int contentLength = request.getContentLength();
            if (reader == null || contentLength == 0 || contentLength == -1) {
                value = null;
            } else if (!isJson(request)) {
                throw new RuntimeException("Body is not JSON: " + request.getContentType());
            } else {
                String data = IOUtils.toString(reader); // meh
                logger.log(Level.WARNING, "Json body, {0}", data);
                value = _decoder.decode(data);
                request.setAttribute(JSON_ARRAY_ATTRIBUTE, data.startsWith("["));
            }
            request.setAttribute(JSON_BODY_ATTRIBUTE, value);
        }
        return value;
    }

    protected boolean isJson(HttpServletRequest request) {
        return StringUtils.startsWith(request.getContentType(), MimeUtils.MIME_TYPE_APPLICATION_JSON);
    }

    @Override
    public boolean isStateless() {
        return false;
    }
}
