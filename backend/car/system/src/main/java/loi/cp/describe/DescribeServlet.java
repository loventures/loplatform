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

package loi.cp.describe;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.learningobjects.cpxp.BaseServiceMeta;
import com.learningobjects.cpxp.ServiceMeta;
import com.learningobjects.cpxp.component.ComponentArchive;
import com.learningobjects.cpxp.component.ComponentEnvironment;
import com.learningobjects.cpxp.component.annotation.Archive;
import com.learningobjects.cpxp.component.annotation.Component;
import com.learningobjects.cpxp.component.util.ComponentUtils;
import com.learningobjects.cpxp.component.web.AbstractComponentServlet;
import com.learningobjects.cpxp.component.web.ServletBinding;
import com.learningobjects.cpxp.filter.SendFileFilter;
import com.learningobjects.cpxp.util.HttpUtils;
import com.learningobjects.cpxp.util.MimeUtils;
import com.learningobjects.cpxp.util.StringUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import javax.inject.Inject;
import java.io.Writer;
import java.util.*;

@SuppressWarnings("unused") // component
@Component
@ServletBinding(
    path = "/sys/describe"
)
public class DescribeServlet extends AbstractComponentServlet {

    private static final ObjectMapper __mapper = new ObjectMapper();
    static {
        __mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
    }

    @Inject
    private ComponentEnvironment componentEnvironment;

    @Override
    public void get(HttpServletRequest request, HttpServletResponse response) throws Exception {
        HttpUtils.setExpired(response);

        Map<String, Object> status = new LinkedHashMap<>();
        Date date = new Date();
        status.put("asOf", date.toString());
        status.put("system", getSystemMap());

        Map<String, Object> platform = new LinkedHashMap<>();
        status.put("platform", platform);
        platform.put("name", "Difference Engine");
        final ServiceMeta serviceMeta = BaseServiceMeta.getServiceMeta();
        platform.put("version",serviceMeta.getVersion());
        platform.put("branch", serviceMeta.getBranch());
        platform.put("revision", serviceMeta.getRevision());
        platform.put("stashDetails", serviceMeta.getRevisionLink());
        platform.put("buildNumber", serviceMeta.getBuildNumber());
        platform.put("buildDate", serviceMeta.getBuildDate());

        List<Map<String, Object>> modules = new ArrayList<>();
        status.put("modules", modules);

        for (ComponentArchive archive : getAvailableArchives()) {
            Archive a = archive.getArchiveAnnotation();
            Map<String, Object> map = new LinkedHashMap<>();
            modules.add(map);
            map.put("identifier", archive.getIdentifier());
            map.put("name", a.name());
            map.put("version", a.version());
            map.put("branch", a.branch());
            map.put("revision", a.revision());
            map.put("buildNumber", a.buildNumber());
            map.put("buildDate", a.buildDate());
        }

        final String path = StringUtils.substringAfter(request.getPathInfo(), "/sys/describe/");
        final Object result = StringUtils.isEmpty(path) ? status
            : ComponentUtils.dereference(status, path.split("/"));

        response.setContentType(MimeUtils.MIME_TYPE_APPLICATION_JSON + MimeUtils.CHARSET_SUFFIX_UTF_8);
        try (Writer writer = response.getWriter()) {
            __mapper.writeValue(writer, result);
        }
    }

    private Iterable<ComponentArchive> getAvailableArchives() {
        return componentEnvironment.getAvailableArchives();
    }

    private Map<String, Object> getSystemMap() {
        final ServiceMeta serviceMeta = BaseServiceMeta.getServiceMeta();
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("cdn", (serviceMeta.getStaticHost() != null));
        map.put("sse", SendFileFilter.isNioAvailable());
        map.put("jvm", serviceMeta.getJvm());
        map.put("java", serviceMeta.getJava());
        map.put("scala", scala.util.Properties.versionNumberString());
        map.put("local", serviceMeta.isLocal());
        map.put("production", serviceMeta.isProduction());
        map.put("herd", serviceMeta.getPonyHerd());
        return map;
    }


}
