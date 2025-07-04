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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;

import javax.validation.groups.Default;
import java.util.Map;

public class ComponentInterfaceJson {
    @JsonView(Default.class)
    @JsonInclude(Include.NON_NULL)
    public final Boolean primary;

    @JsonView(Default.class)
    @JsonInclude(Include.NON_NULL)
    public final Map binding;

    @JsonCreator
    public ComponentInterfaceJson(@JsonProperty("primary") Boolean primary, @JsonProperty("binding") Map binding) {
        this.primary = primary;
        this.binding = binding;
    }

    @Override
    public String toString() {
        return (binding != null) ? "[primary=" + primary + ", binding=" + binding + "]" : "[primary=" + primary + "]";
    }
}
