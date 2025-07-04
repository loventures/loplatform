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

package com.learningobjects.cpxp.service.script;

import com.learningobjects.cpxp.component.ComponentCollection;
import com.learningobjects.cpxp.component.ComponentEnvironment;
import com.learningobjects.cpxp.service.item.Item;

import javax.ejb.Local;
import java.io.File;
import java.util.Map;

@Local
public interface ScriptService {
    Item getDomainScriptFolder(Long domainId);
    ScriptSiteFacade getScriptFolder(Long id);
    void clusterRemoveComponentArchive(String identifier);
    void removeComponentArchive(String identifier, Long domainId);
    ComponentArchiveFacade installComponentArchive(Long scriptFolder, File scriptArchive, String downloadName) throws Exception;
    ComponentCollection getComponentCollection(Long domainId, boolean edit); // edit implies return real overlord env
    void setEnabledMap(Long domainId, Map<String, Boolean> map);
    void setConfigurationMap(Long domainId, Map<String, String> configurations);
    void setComponentConfiguration(Long domainId, String componentId, String configuration);

    void setJsonConfiguration(Long domainId, String componentId, Object configuration);
    /** This returns the naked configurations for this domain, it does not stack config hierarchy. */
    Map<String, String> getConfigurationMap(Long domainId);
    ComponentEnvironment initComponentEnvironment();
}
