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

package loi.cp.domain;

import com.learningobjects.cpxp.component.annotation.Service;
import com.learningobjects.cpxp.component.web.util.JacksonUtils;
import com.learningobjects.cpxp.controller.domain.DomainAppearance;
import com.learningobjects.cpxp.service.Current;
import com.learningobjects.cpxp.util.DigestUtils;
import com.learningobjects.cpxp.util.StringUtils;
import loi.cp.config.ConfigurationService;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

@Service
public class DomainAppearanceImpl implements DomainAppearance {

    /**
     * Brand Primary is the primary color for bootstrap 4
     */
    public static final String BRAND_COLOR_KEY = "brand-primary";

    /**
     * TODO: delete primary/secondary/accent when all projects move to bootstrap 4
     * Primary/secondary/accent colors are for bootstrap 3
     */
    public static final String PRIMARY_COLOR_KEY = "color-primary";
    public static final String SECONDARY_COLOR_KEY = "color-secondary";
    public static final String ACCENT_COLOR_KEY = "color-accent";

    private final ConfigurationService _configurationService;

    public DomainAppearanceImpl(ConfigurationService configurationService) {
        _configurationService = configurationService;
    }


    /**
     * This implementation will configure the colors on the domain configuration on the <code>variables</code>
     * attribute of the <code>style</code> object on the domainConfiguration.
     * i.e. if we have a configuration like:
     * <pre>
     * {
     *   'something':'else',
     *   'style':{
     *     'variables':{
     *       'color-primary':'#FFF'
     *       'pre-existing-value':'same_value'
     *     }
     *   }
     * }
     * </pre>
     * The resulting configuration will be:
     * <pre>
     * {
     *   'something':'else',
     *   'style':{
     *     'variables':{
     *       'color-primary':<strong>primaryColor</strong>
     *       'color-secondary':<strong>secondaryColor</strong>
     *       'color-accent':<strong>accentColor</strong>
     *       'pre-existing-value':'same_value'
     *     }
     *   }
     * }
     * </pre>
     *
     * @param primaryColor   the value to be used as the <strong>color-primary</string> variable.
     * @param secondaryColor the value to be used as the <strong>color-secondary</string> variable.
     * @param accentColor    the value to be used as the <strong>color-accent</string> variable.
     */
    @Override
    public void setColors(String primaryColor, String secondaryColor, String accentColor) {
        Map<String, String> styleVariables = getStyleConfigurations();
        if (StringUtils.isNotEmpty(primaryColor)) {
            styleVariables.put(PRIMARY_COLOR_KEY, primaryColor);
            styleVariables.put(BRAND_COLOR_KEY, primaryColor);
        } else {
            styleVariables.remove(PRIMARY_COLOR_KEY);
            styleVariables.remove(BRAND_COLOR_KEY);
        }
        if (StringUtils.isNotEmpty(secondaryColor)) {
            styleVariables.put(SECONDARY_COLOR_KEY, secondaryColor);
        } else {
            styleVariables.remove(SECONDARY_COLOR_KEY);
        }
        if (StringUtils.isNotEmpty(accentColor)) {
            styleVariables.put(ACCENT_COLOR_KEY, accentColor);
        } else {
            styleVariables.remove(ACCENT_COLOR_KEY);
        }
        setStyleConfigurations(styleVariables);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, String> getStyleConfigurations() {
        return (Current.getDomain() == null) ? Collections.emptyMap()
          : _configurationService.getDomain(DomainStyleConfig.key()).variables();
    }

    private void setStyleConfigurations(Map<String, String> vars) {
        DomainStyleConfig cfg = DomainStyleConfig.apply(vars);
        _configurationService.overwriteDomain(DomainStyleConfig.key(), cfg);
    }

    @Override
    public String getPrimaryColor() {
        return this.getStyleConfigurations().get(PRIMARY_COLOR_KEY);
    }

    @Override
    public String getSecondaryColor() {
        return this.getStyleConfigurations().get(SECONDARY_COLOR_KEY);
    }

    @Override
    public String getAccentColor() {
        return this.getStyleConfigurations().get(ACCENT_COLOR_KEY);
    }

    public String getStyleHash() {
        try {
            return DigestUtils.md5Hex(JacksonUtils.getMapper().writeValueAsString(new TreeMap<>(getStyleConfigurations()))).substring(0, 8);
        } catch (Exception ex) {
            throw new RuntimeException("Style hash error", ex);
        }
    }
}
