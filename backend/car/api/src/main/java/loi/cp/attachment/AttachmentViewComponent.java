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

package loi.cp.attachment;

import com.learningobjects.cpxp.component.ComponentDecorator;
import com.learningobjects.cpxp.component.annotation.Controller;
import com.learningobjects.cpxp.component.annotation.PathVariable;
import com.learningobjects.cpxp.component.annotation.RequestMapping;
import com.learningobjects.cpxp.component.query.ApiQuery;
import com.learningobjects.cpxp.component.query.ApiQueryResults;
import com.learningobjects.cpxp.component.web.ApiRootComponent;
import com.learningobjects.cpxp.component.web.Method;
import com.learningobjects.de.authorization.Secured;

import java.util.Optional;

/**
 * This API provides a read-only view onto the attachments beneath an entity.
 */
@Controller(value = "attachmentView")
@RequestMapping(path = "attachments")
@Secured // Parent enforces read access control
public interface AttachmentViewComponent extends ApiRootComponent, ComponentDecorator {
    /**
     * Get all attachment children of this component.
     */
    @RequestMapping(method = Method.GET)
    ApiQueryResults<AttachmentComponent> getAttachments(ApiQuery query);

    /**
     * Get a specific attachment child of this component.
     */
    @RequestMapping(path = "{id}", method = Method.GET)
    Optional<AttachmentComponent> getAttachment(@PathVariable("id") Long id);
}
