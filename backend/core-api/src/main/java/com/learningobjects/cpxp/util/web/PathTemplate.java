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

package com.learningobjects.cpxp.util.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.learningobjects.cpxp.component.query.PredicateOperator;
import com.learningobjects.de.web.util.UriTemplate;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class PathTemplate {

    private final UriTemplate uriTemplate;

    public PathTemplate(final String templateValue) {
        this(new UriTemplate(templateValue));
    }

    private PathTemplate(final UriTemplate uriTemplate) {
        this.uriTemplate = uriTemplate;
    }

    public UriTemplate getUriTemplate() {
        return uriTemplate;
    }

    public PathTemplate withEmbed(final String embedValue) {
        final String newTemplateValue = uriTemplate + ";embed=" + embedValue;
        return new PathTemplate(new UriTemplate(newTemplateValue));
    }

    public PathTemplate withFilter(final String propName, final PredicateOperator operator, final String value) {
        final String newTemplateValue = uriTemplate + String.format(";filter=%s:%s(%s)", propName, operator.toString(), value);
        return new PathTemplate(new UriTemplate(newTemplateValue));
    }

    public PathTemplate withPage(final int offset, final int limit) {
        final String newTemplateValue = uriTemplate + ";offset=" + offset + ";limit=" + limit;
        return new PathTemplate(new UriTemplate(newTemplateValue));
    }

    public PathTemplate extend(String extension) {
        final String newTemplateValue = uriTemplate + extension;
        return new PathTemplate(new UriTemplate(newTemplateValue));
    }

    public BasicPath expand(final Object... uriVariables) {

        final String[] vars = extractJsonNodeIds(uriVariables);

        final URI expanded = URI.create(uriTemplate.createURI(vars));
        return new BasicPath(URLDecoder.decode(expanded.toString(), StandardCharsets.UTF_8));
    }

    private String[] extractJsonNodeIds(final Object... uriVariables) {

        final List<String> vars = new ArrayList<>();
        for (final Object uriVariable : uriVariables) {
            if (uriVariable instanceof JsonNode) {
                final JsonNode nodeVariable = (JsonNode) uriVariable;
                final String nodeId = nodeVariable.path("id").asText();
                vars.add(nodeId);
            } else {
                vars.add(uriVariable.toString());
            }
        }

        // according to intellij, the `new String[]` is just to stuff in the class info
        return vars.toArray(new String[0]);
    }




}
