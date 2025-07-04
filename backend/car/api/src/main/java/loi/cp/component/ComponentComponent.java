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

package loi.cp.component;


import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.learningobjects.cpxp.component.ComponentInterface;
import com.learningobjects.cpxp.component.annotation.*;
import com.learningobjects.cpxp.component.web.Method;
import com.learningobjects.de.authorization.Secured;
import com.learningobjects.de.web.Queryable;
import com.learningobjects.de.web.QueryableProperties;
import loi.cp.admin.right.AdminRight;

import javax.validation.groups.Default;
import java.util.Map;

import static com.learningobjects.de.web.Queryable.Trait.NOT_SORTABLE;

/**
 * This is a meta component that describes a component.
 */
@QueryableProperties({
    @Queryable(name="interface", dataType = "interface", traits= NOT_SORTABLE)
})
@Controller(value = "component", category = Controller.Category.API_SUPPORT)
@Schema("component")
public interface ComponentComponent extends ComponentInterface {
    String PROPERTY_INTERFACE = "interface";
    String PROPERTY_IDENTIFIER = "identifier";

    @JsonView(Default.class)
    @Queryable(dataType = PROPERTY_IDENTIFIER, traits = NOT_SORTABLE)
    String getIdentifier();

    @JsonView(Default.class)
    String getName();

    @JsonView(Default.class)
    String getSchema();

    @JsonView(Default.class)
    Map<String, ComponentInterfaceJson> getInterfaces();

    @JsonView(Default.class)
    String getDescription();

    @JsonView(Default.class)
    ObjectNode getConfiguration();

    @RequestMapping(path = "instance", method = Method.GET)
    ComponentInterface getInstance();

    @RequestMapping(path = "config", method = Method.PUT)
    @Secured(AdminRight.class)
    JsonNode updateConfiguration(
      @RequestBody ObjectNode newConfig,
      @QueryParam(required = false) Boolean merge
    );
}
