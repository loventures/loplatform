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

package loi.cp.accesscode;

import com.learningobjects.cpxp.dto.*;
import com.learningobjects.cpxp.service.component.misc.AccessCodeConstants;
import com.learningobjects.cpxp.service.query.Function;
import com.learningobjects.cpxp.service.query.QueryBuilder;
import scala.Option;

import java.util.Optional;

@FacadeItem("*")
public interface AccessCodeParentFacade extends Facade {
    @FacadeComponent
    public Option<AccessCodeComponent> findAccessCode();
    public Optional<AccessCodeComponent> getAccessCode(Long id);
    public <T extends AccessCodeComponent> T addAccessCode(AccessCodeComponent.Init init);
    public QueryBuilder queryAccessCodes();
    @FacadeQuery(group = "AccessCodes", domain = true)
    public Optional<AccessCodeComponent> findAnyAccessCode(
      @FacadeCondition(value = AccessCodeConstants.DATA_TYPE_ACCESS_CODE, function = Function.LOWER)
      String code
    );

    @FacadeComponent
    public <T extends AccessCodeBatchComponent> T addBatch(Class<T> impl);
    public <T extends AccessCodeBatchComponent> T addBatch(T init);
    public Optional<AccessCodeBatchComponent> getBatch(Long id);
    @FacadeQuery(group = "Batchs")
    public QueryBuilder queryBatches();

    public void lock(boolean pessimistic);
}
