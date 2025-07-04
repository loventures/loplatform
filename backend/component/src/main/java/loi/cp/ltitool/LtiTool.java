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

package loi.cp.ltitool;

import com.learningobjects.cpxp.component.AbstractComponent;
import com.learningobjects.cpxp.component.annotation.Component;
import com.learningobjects.cpxp.component.annotation.Instance;

@Component(
  name = "LTI Tool",
  description = "A Basic LTI Tool link",
  version = "0.7",
  context = "#inTemplatesGroup"
)
public class LtiTool extends AbstractComponent implements LtiToolComponent {

    @Instance
    private LtiToolFacade _self;

    @Override
    public Long getId() {
        return _self == null ? null : _self.getId();
    }

    @Override
    public String getName() {
        return _self.getName();
    }

    @Override
    public void setName(String name) {
        _self.setName(name);
    }

    @Override
    public String getToolId() {
        return _self.getToolId();
    }

    @Override
    public void setToolId(String toolId) {
        _self.setToolId(toolId);
    }

    @Override
    public Boolean getDisabled() {
        return _self.getDisabled();
    }

    @Override
    public void setDisabled(Boolean disabled) {
        _self.setDisabled(disabled);
    }

    @Override
    public Boolean isCopyBranchSection() {
        return _self.getCopyBranchSection();
    }

    @Override
    public void setCopyBranchSection(Boolean copyBranchSection) {
        _self.setCopyBranchSection(copyBranchSection);
    }

    @Override
    public LtiToolConfiguration getLtiConfiguration() {
        return _self.getLtiConfiguration();
    }

    @Override
    public void setLtiConfiguration(LtiToolConfiguration ltiConfiguration) {
        _self.setLtiConfiguration(ltiConfiguration);
    }

    @Override
    public void delete() {
        _self.delete();
    }
}
