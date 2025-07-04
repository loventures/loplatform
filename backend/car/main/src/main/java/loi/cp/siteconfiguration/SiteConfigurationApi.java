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

package loi.cp.siteconfiguration;

import com.learningobjects.cpxp.component.AbstractComponent;
import com.learningobjects.cpxp.component.annotation.Component;
import com.learningobjects.cpxp.service.facade.FacadeService;

import javax.inject.Inject;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class SiteConfigurationApi extends AbstractComponent implements SiteConfigurationApiComponent {

    @Inject
    private FacadeService _facadeService;

    @Override
    public DomainLinkConfiguration createFooter(DomainLinkConfiguration configuration) {
        configuration.count = Long.valueOf(configuration.objects.size());
        getConfiguration().setFooterConfiguration(configuration);

        return configuration;
    }

    @Override
    public DomainLinkConfiguration getFooter() {
        DomainLinkConfiguration conf = getConfiguration().getFooterConfiguration();
        if (conf == null) {
            conf = new DomainLinkConfiguration();
            createFooter(conf);

        }
        return setDefaultNewWindow(conf);
    }

    @Override
    public DomainLinkConfiguration createHeader(DomainLinkConfiguration configuration) {
        configuration.count = Long.valueOf(configuration.objects.size());
        getConfiguration().setHeaderConfiguration(configuration);
        return configuration;
    }

    @Override
    public DomainLinkConfiguration getHeader() {
        DomainLinkConfiguration conf = getConfiguration().getHeaderConfiguration();
        if (conf == null) {
            conf = new DomainLinkConfiguration();
            createHeader(conf);
        }
        return setDefaultNewWindow(conf);
    }

    public SiteConfigurationFacade getConfiguration() {
        return _facadeService
          .getFacade("folder-domain", SiteConfigurationRootFacade.class)
          .getConfiguration();
    }

    private DomainLinkConfiguration setDefaultNewWindow(DomainLinkConfiguration configuration) {
        List<DomainLinkConfiguration.DomainLinkEntry> updatedConfigs =
          configuration.objects.stream()
                               .map(e -> {
                                   if (e.newWindow == null) {
                                       e.newWindow = true;
                                   }
                                   return e;
                               })
                               .collect(Collectors.toList());

        configuration.objects = updatedConfigs;
        return configuration;
    }
}
