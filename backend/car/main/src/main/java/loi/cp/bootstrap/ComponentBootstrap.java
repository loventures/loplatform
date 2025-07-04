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

package loi.cp.bootstrap;

import com.fasterxml.jackson.databind.JsonNode;
import com.learningobjects.cpxp.component.*;
import com.learningobjects.cpxp.component.annotation.Component;
import com.learningobjects.cpxp.controller.upload.UploadInfo;
import com.learningobjects.cpxp.service.Current;
import com.learningobjects.cpxp.service.script.ScriptService;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@Component
public class ComponentBootstrap extends AbstractComponent {
    private static final Logger logger = Logger.getLogger(ComponentBootstrap.class.getName());

    @Inject
    private ScriptService _scriptService;

    @Bootstrap("core.car.enable")
    public void enableCars(List<String> cars) throws Exception {

        ComponentCollection collection = _scriptService.getComponentCollection(Current.getDomain(), true);
        Map<String, Boolean> availability = new HashMap<>(collection.getEnabledMap());
        ComponentRing ring = ComponentManager.getComponentRing();
        for (String car : cars) {
            if (car.indexOf('.') < 0) {
                car = "com.learningobjects." + car;
            }
            ComponentArchive archive = ring.findArchive(car);
            if (archive == null) {
                throw new Exception("Unknown archive: " + car);
            }
            availability.put(archive.getIdentifier(), Boolean.TRUE);
            for (ComponentArchive dep : archive.getAllDependencies()) {
                availability.put(dep.getIdentifier(), true);
            }
        }
        _scriptService.setEnabledMap(Current.getDomain(), availability);
    }

    @Bootstrap("core.car.disable")
    public void disableCars(List<String> cars) throws Exception {

        ComponentCollection collection = _scriptService.getComponentCollection(Current.getDomain(), true);
        Map<String, Boolean> availability = new HashMap<>(collection.getEnabledMap());
        ComponentRing ring = ComponentManager.getComponentRing();
        for (String car : cars) {
            if (car.indexOf('.') < 0) {
                car = "com.learningobjects." + car;
            }
            ComponentArchive archive = ring.findArchive(car);
            if (archive == null) {
                throw new Exception("Unknown archive: " + car);
            }
            availability.put(archive.getIdentifier(), false);
            // TODO: Technically I should turn off anything that depends on this as well but...
        }
        _scriptService.setEnabledMap(Current.getDomain(), availability);
    }

    @Bootstrap("core.car.install")
    public void installCar(UploadInfo upload) throws Exception {
        logger.log(Level.INFO, "Install car {0}", upload.getFileName());

        Long folder = _scriptService.getDomainScriptFolder(Current.getDomain()).getId();
        _scriptService.installComponentArchive(folder, upload.getFile(), upload.getFileName());
    }

    @Bootstrap("core.component.toggle")
    public void toggleComponent(ComponentToggleJson json) {
        ComponentCollection componentCollection = _scriptService.getComponentCollection(Current.getDomain(), true);
        Map<String, Boolean> availability = new HashMap<>(componentCollection.getEnabledMap());
        availability.put(json.identifier, json.enabled);
        _scriptService.setEnabledMap(Current.getDomain(), availability);
        // ComponentManager.initComponentEnvironment(componentCollection);
    }

    @Bootstrap("core.component.configure")
    public void configureComponent(JsonComponent json) {
        logger.log(Level.INFO, "Configure component {0}", json.identifier);
        _scriptService.setJsonConfiguration(Current.getDomain(), json.identifier, json.configuration);
    }

    public static class ComponentToggleJson {
        public String identifier;
        public Boolean enabled;
    }
    public static class JsonComponent {
        public String identifier;
        public JsonNode configuration;
    }

    @Bootstrap("core.component.init")
    public void initEnvironment() {
        ComponentCollection collection = _scriptService.getComponentCollection(Current.getDomain(), false);
        ComponentManager.initComponentEnvironment(collection);
    }
}
