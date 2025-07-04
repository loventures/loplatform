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

import com.learningobjects.cpxp.component.AbstractComponent;
import com.learningobjects.cpxp.component.annotation.Component;
import com.learningobjects.cpxp.component.annotation.Instance;
import com.learningobjects.cpxp.component.query.ApiQuery;
import com.learningobjects.cpxp.component.query.ApiQueryResults;
import com.learningobjects.cpxp.component.query.ApiQuerySupport;
import com.learningobjects.cpxp.service.query.QueryBuilder;

import java.util.Optional;

@Component
@SuppressWarnings("unused")
public class AttachmentView extends AbstractComponent implements AttachmentViewComponent {
    @Instance
    protected AttachmentParentFacade _context;

    @Override
    public ApiQueryResults<AttachmentComponent> getAttachments(ApiQuery query) {
        QueryBuilder qb = _context.queryAttachments();
        return ApiQuerySupport.query(qb, query, AttachmentComponent.class);
    }

    @Override
    public Optional<AttachmentComponent> getAttachment(Long id) {
        return _context.getAttachment(id);
    }
}
