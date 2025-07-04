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

package com.learningobjects.cpxp.servlet;

import com.learningobjects.cpxp.BaseWebContext;
import com.learningobjects.cpxp.component.ComponentDescriptor;
import com.learningobjects.cpxp.component.ComponentEnvironment;
import com.learningobjects.cpxp.component.ComponentSupport;
import com.learningobjects.cpxp.component.util.ComponentUtils;
import com.learningobjects.cpxp.controller.domain.DomainAppearance;
import com.learningobjects.cpxp.filter.SendFileFilter;
import com.learningobjects.cpxp.service.Current;
import com.learningobjects.cpxp.service.mime.MimeWebService;
import com.learningobjects.cpxp.util.*;
import com.learningobjects.de.style.LibSassStyleCompiler;
import com.learningobjects.de.style.StyleCompiler;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.CharEncoding;

import javax.annotation.Nullable;
import javax.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.FileSystem;
import java.nio.file.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StaticServlet extends AbstractServlet {
    private static final Logger logger = Logger.getLogger(StaticServlet.class.getName());
    /** Serial version UID. */
    private static final long serialVersionUID = 1L;
    private static final Pattern MESSAGE_RE = Pattern.compile("\\$\\{(msg|constant)\\.(\\w*)\\}|\\$\\$(\\w*)(?:\\(['\"](\\w*)['\"]\\)|(/[^)\"]*))?");

    private Pattern _localizeRE;
    private String _base;
    private Boolean _disableCache;
    private FileCache _fileCache;
    private Map<String, StyleCompiler> styleCompilers;
    private List<String> styleExtensionsThatForceRecompile = Arrays.asList("sass", "scss");

    @Inject
    private MimeWebService _mimeWebService;

    public StaticServlet(String localize, String base, Boolean disableCache) {
        super();
        _localizeRE = Pattern.compile(localize);
        _base = base;
        _disableCache = disableCache;
    }

    @Override
    public void init() {
        _fileCache = FileCache.getInstance();
        //construct a map of possible style compilers
        styleCompilers = new HashMap<>();
        StyleCompiler libsassCompiler = new LibSassStyleCompiler();
        styleCompilers.put("sass", libsassCompiler);
        styleCompilers.put("scss", libsassCompiler);
        styleCompilers.put("css", new StyleLocalizer());
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws IOException {

        String pathInfo = request.getPathInfo(); // /version/blah/blah
        int idx = pathInfo.indexOf('/', 1);
        if (idx < 2) {
            logger.log(Level.FINE, "Invalid path" + " {0}", pathInfo);
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        String version = pathInfo.substring(1, idx);
        String resource = pathInfo.substring(1 + idx); // blah/blah

        FileInfo fileInfo = null;
        try {
            fileInfo = getFileInfo(request, response, version, resource);
        } catch (URISyntaxException e) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            logger.log(Level.FINE, "Invalid script uri", e);
        }

        if (fileInfo == null) {
            // Display a simple 404 for static content..
            // throw logThrowing(new FileNotFoundException(resource));
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        } else {
            if ((Boolean.TRUE == _disableCache) || !fileInfo.getDoCache()) {
                fileInfo.setExpires(0);
                fileInfo.setEternal(false);
                fileInfo.setDoCache(false);
            } else {
                // we only cache for a week if the content is varied by
                // localization since in that case the i18n version is not
                // in the url
                long days = fileInfo.getIsLocalized() ? 7L : 365L; // 1w or 1y
                fileInfo.setExpires(days * 24 * 60 * 60 * 1000);
                fileInfo.setEternal(!fileInfo.getIsLocalized());
            }

            logger.log(Level.FINE, "Sending file" + " {0}", fileInfo);
            request.setAttribute(SendFileFilter.REQUEST_ATTRIBUTE_SEND_FILE, fileInfo);
        }
    }

    private FileInfo getFileInfo(HttpServletRequest request, HttpServletResponse response, String version, String resource) throws IOException, URISyntaxException {
        if ((resource.indexOf('^') >= 0) || (resource.indexOf('|') >= 0)) {  // /foo/bar/alpha^alpha.css|beta^beta.css
            return getCompound(request, response, version, resource);
        } else if (Character.isLetter(version.charAt(0)) && (version.indexOf('.') > 0)) { // loi.cp.foo / 0.7/foo.gif
            return getScriptResource(request, response, version, resource);
        } else {
            return getStatic(request, response, version, resource);
        }
    }

    private FileInfo getCompound(HttpServletRequest request, HttpServletResponse response, String version, String resources) throws IOException, URISyntaxException {
        String localeStr = InternationalizationUtils.getLocaleSuffix(); // includes i18n version
        String extension = FilenameUtils.getExtension(resources);
        // Some broken clients are requesting... https://elb.evolv-009.els.campuspack.net/static/cdn/evolve/cdn.Cdn_4.8.78.427d14cbbac7-icarus-gz/fullcalendar%5E1.6.1%5Efullcalendar.css%7Cangular-strap%5E0.7.2%5Evendor%5Ebootstrap-datepicker.css%7Cangular-strap%5E0.7.2%5Evendor%5Ebootstrap-timepicker.css%7Cjqueryplugin%5Emask%5Ejquery.mask.min.cssjqueryplugin/mask/load.gif
        // TODO: support that monstrosity..
        if (!"js".equals(extension) && !"css".equals(extension)) {
            logger.log(Level.WARNING, "Invalid compound resource {0}", resources);
            return null;
        }
        String fileName = DigestUtils.md5Hex(resources) + "." + extension;
        String entryPath = FileUtils.cleanFilename(request.getServerName()) + "/compound/" + FileUtils.cleanFilename(localeStr) + "/" + fileName;
        FileHandle handle = _fileCache.getFile(entryPath);
        try {
            Streamer streamer = new CompoundStreamer();
        try {
            streamer.init(request, response, version);
            int index = resources.lastIndexOf('/');
            String prefix = (index < 0) ? "" : resources.substring(0, 1 + index);
            resources = resources.substring(1 + index);
            for (String resource : resources.split("\\|")) {
                streamer.add(prefix + resource.replace('^', '/'));
            }
            if (streamer.hasMissing()) {
                logger.log(Level.FINE, "File not found {0}", resources);
                return null;
            }
            boolean stale = streamer.getLastModified() > handle.getFile().lastModified();
            if (stale || !handle.exists()) {
                handle.recreate(); // may change handle File
                try {
                    try (FileOutputStream out = FileUtils.openOutputStream(handle.getFile())) {
                        streamer.stream(request, out);
                    }
                    handle.created();
                } catch (Exception ex) {
                    handle.failed();
                    throw new RuntimeException("Compound error", ex);
                }
            }
            handle.ref();
            FileInfo fileInfo = new LocalFileInfo(handle.getFile(), handle::deref);
            if (!handle.isTemporary()) {
                fileInfo.setDoCache(true);
                fileInfo.setCachePath(entryPath);
            }
            for (FileInfo info : streamer.getFiles()) {
                for (File dependency : info.getDependencies()) {
                    fileInfo.addDependency(dependency);
                }
            }
            fileInfo.setIsLocalized(streamer.isLocalized());
            fileInfo.setDisposition(HttpUtils.getDisposition(HttpUtils.DISPOSITION_INLINE, fileName)); // HACK: Because grizzly always sets a disposition otherwise
            fileInfo.setLastModified(new Date(streamer.getLastModified()));
            fileInfo.setContentType(getMimeType(fileName, streamer.isLocalized()));
            return fileInfo;
        } finally {
            streamer.deref();
        } } finally {
            handle.deref();
        }
    }

    private FileInfo getScriptResource(HttpServletRequest request, HttpServletResponse response, String version, String resource) throws IOException, URISyntaxException {
        // TODO: If the version does not precisely match this component (and theoritically if
        // the domain style config md5 desn't match) then return a non-caching result.
        int i0 = version.indexOf('_');
        if (i0 < 0) {
            logger.log(Level.FINE, "Invalid script resource" + " {0}", resource);
            return null;
        }
        String identifier = version.substring(0, i0);
        version = version.substring(1 + i0);
        ComponentEnvironment env = BaseWebContext.getContext().getComponentEnvironment();
        ComponentDescriptor component;
        try {
            component = env.getComponent(identifier);
        } catch (Exception ex) {
            logger.log(Level.FINE, "Unknown script resource component" + " {0}, {1}", new Object[] {identifier, resource});
            return null;
        }

        URL url = component.getResource(resource);

        if (url == null) {
            logger.log(Level.FINE, "Unknown script resource" + " {0}", resource);
            return null;
        }
        Path path = Paths.get(url.toURI());
        boolean localize = (resource.endsWith(".js") || resource.endsWith(".json")) && !"cdn.Cdn".equals(identifier); // HACK.. component scripts aren't normalized yet. _localizeRE.matcher(resource).matches(); // HACK.. Cdn non localized
        return getStaticFile(request, response, version, identifier + "/" + resource, path, localize, component);
    }

    private FileInfo getStatic(HttpServletRequest request, HttpServletResponse response, String version, String resource) throws IOException, URISyntaxException {
        boolean localize = _localizeRE.matcher(resource).matches();

        /* TODO: Make me FIX?
        if (!Boolean.TRUE.equals(request.getAttribute("debug"))) {
            String min = resource;
            if (localize && resource.endsWith(".js")) {
                min = resource.substring(0, resource.length() - 3) + "-min.js";
            } else if (resource.startsWith("styles/") && resource.endsWith(".css")) {
                min = resource.substring(0, resource.length() - 4) + "-min.css";
            }
            if (BaseWebContext.getDebug() && (min != resource)) {
                Path file = Paths.get(getServletContext().getRealPath(_base + "/" + resource));
                Path mf = Paths.get(getServletContext().getRealPath(_base + "/" + min));
                if (Files.getLastModifiedTime(file).toMillis() - Files.getLastModifiedTime(mf).toMillis() < 5000) {
                    // Use the minified version unless the plain version is
                    // at least 5 seconds newer. This skew is to allow for
                    // the resource unpacking process to be somewhat slow.
                    resource = min;
                }
            } else {
                resource = min;
            }
        }
        */

        String staticResource = _base + "/" + resource;
        Path staticPath = Paths.get(getServletContext().getRealPath(staticResource));
        if(!Files.exists(staticPath)) {
            final URL url = getClass().getClassLoader().getResource(staticResource);
            if (url != null) {
                final URI uri = url.toURI();
                try {
                    staticPath = Paths.get(uri);
                } catch (FileSystemNotFoundException ex) {
                    FileSystem fs = FileSystems.newFileSystem(uri, Collections.<String,String>emptyMap());
                    staticPath = fs.provider().getPath(uri);
                }
            }
        }
        return getStaticFile(request, response, version, resource, staticPath, localize, null);
    }

    private FileInfo getStaticFile(HttpServletRequest request, HttpServletResponse response, String version, String resource, Path path, boolean localize, ComponentDescriptor component) throws IOException {
        logger.log(Level.FINE, "Static file" + " {0}, {1}", new Object[] {resource, path});

        // Somewhat duplicated in AttachmentController

        if (!Files.exists(path) || !Files.isRegularFile(path) || !Files.isReadable(path)) {
            logger.log(Level.FINE, "File not found" + " {0}", path);
            return null;
        }

        // TODO: KILLME: Some component javascripts rely on localization.. But they shouldn't.
        // Until I kill them, use a LOJS first line to flag localization to happen. Or alternatively
        // rename them to .lo.js...
        if (localize && (component != null) && resource.endsWith(".js")) {
            try (InputStream in = Files.newInputStream(path)) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(in, CharEncoding.UTF_8));
                localize = StringUtils.contains(reader.readLine(), "LOJS");
            }
        }

        FileInfo fileInfo;
        boolean setLastModified = true;
        if (localize) {
            fileInfo = getLocalized(request, response, version, resource, path, component);
        } else if ((component != null) && styleCompilers.get(FilenameUtils.getExtension(resource)) != null) { // do we have a style compiler that matches this extension?
            fileInfo = getExpandedCss(request, version, resource, path, component, styleCompilers.get(FilenameUtils.getExtension(resource)));
            setLastModified = false; // let getExpandedCss handle this
        } else {
            final String cachePath;
            if (component != null && Current.getDomain() != null) {
                /* If this file comes from a virtual component then it must be cached with
                 * the domain, otherwise two versions of the same archive on two different
                 * domains will interfere with each other. */
                cachePath = _base + "/_domain/" + Current.getDomain() + "/" + resource;
            } else {
                cachePath = _base + "/" + resource;
            }

            fileInfo = new PathFileInfo(path);
            fileInfo.setDoCache(true);
            fileInfo.setCachePath(cachePath);
        }
        if(setLastModified) {
            fileInfo.setLastModified(new Date(Files.getLastModifiedTime(path).toMillis()));
        }
        fileInfo.setContentType(getMimeType(request.getRequestURI(), localize));

        return fileInfo;
    }

    private class StyleLocalizer implements StyleCompiler {
        @Override
        public void compileStyle(HttpServletRequest request, Path path, ComponentDescriptor component, OutputStream out, String resultFileName, DomainAppearance appearance) throws IOException {
            localize(request, path, component, out);
        }
    }

    private FileInfo getExpandedCss(HttpServletRequest request, String version, String resource, Path path, ComponentDescriptor component, StyleCompiler expander) {
        String entryPath = FileUtils.cleanFilename(request.getServerName()) + "/" + "css" + "/" + FileUtils.cleanFilename(resource);
        FileHandle handle = _fileCache.getFile(entryPath, Optional.empty(), Optional.ofNullable(version));
        String fileNameWithoutComponent = resource.substring(resource.indexOf("/"), resource.length());
        DomainAppearance appearance = ComponentSupport.lookupService(DomainAppearance.class);
        try {
            logger.log(Level.FINE, "Cs" + " {0}, {1}", new Object[] {entryPath, handle});
            if (!handle.exists()) { // never created, or evicted due to version mismatch
                handle.setVersion(version);
                handle.recreate();
                try {
                    try (FileOutputStream out = FileUtils.openOutputStream(handle.getFile())) {
                        expander.compileStyle(request, path, component, out, fileNameWithoutComponent, appearance);
                        //localize(request, file, component, out); // meh, hijack localization, works for now
                    }
                    handle.created();
                } catch (Exception ex) {
                    handle.failed();
                    throw new RuntimeException("Cssization error", ex);
                }
            }
            boolean cacheable = !styleExtensionsThatForceRecompile.contains(FilenameUtils.getExtension(resource))
                || version.contains(appearance.getStyleHash());
            handle.ref();
            FileInfo fileInfo = new LocalFileInfo(handle.getFile(), handle::deref);
            if (cacheable && !handle.isTemporary()) {
                fileInfo.setDoCache(true);
                fileInfo.setCachePath(entryPath);
            }
            fileInfo.setLastModified(new Date());
            fileInfo.addDependency(path);
            return fileInfo;
        } finally {
            handle.deref();
        }
    }

    private FileInfo getLocalized(HttpServletRequest request, HttpServletResponse response, String version, String resource, Path path, ComponentDescriptor component) throws IOException {
        String localeStr = InternationalizationUtils.getLocaleSuffix(); // includes i18n version
        String entryPath = FileUtils.cleanFilename(request.getServerName()) + "/" + FileUtils.cleanFilename(localeStr) + "/" + FileUtils.cleanFilename(resource);
        FileHandle handle = _fileCache.getFile(entryPath);
        try {
            logger.log(Level.FINE, "Localize" + " {0}, {1}", new Object[] {entryPath, handle});
            boolean stale = Files.getLastModifiedTime(path).toMillis() > handle.getFile().lastModified();
            if (stale || !handle.exists()) {
                handle.recreate();
                try {
                    try (FileOutputStream out = FileUtils.openOutputStream(handle.getFile())) {
                        localize(request, path, component, out);
                    }
                    handle.created();
                } catch (Exception ex) {
                    handle.failed();
                    throw new RuntimeException("Localization error", ex);
                }
            }
            handle.ref();
            FileInfo fileInfo = new LocalFileInfo(handle.getFile(), handle::deref);
            if (!handle.isTemporary()) {
                fileInfo.setDoCache(true);
                fileInfo.setCachePath(entryPath);
            }
            if (path.toUri().getScheme().equals("file")) {
                fileInfo.addDependency(path.toFile());
            }
            // If the version string includes the locale then don't flag
            // this as localized so we won't sent a Vary header in the
            // response. This because our URL is appropriately
            // uniquified. Reason for this is that IE6 through 9(!) won't cache
            // varying content. See JavascriptRenderer.
            fileInfo.setIsLocalized(!version.endsWith(localeStr));
            return fileInfo;
        } finally {
            handle.deref();
        }
    }

    private void localize(HttpServletRequest request, Path src, ComponentDescriptor component, OutputStream out) throws IOException {
        try (InputStream in = Files.newInputStream(src)) {
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out, CharEncoding.UTF_8));
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, CharEncoding.UTF_8));
            String line;
            StringBuffer sb = new StringBuffer();
            while ((line = reader.readLine()) != null) {
                Matcher m = MESSAGE_RE.matcher(line);
                while (m.find()) {
                    String replacement;
                    if (component == null) {
                        String bundle = m.group(1);
                        String key = m.group(2);
                        Map map = (Map) request.getAttribute(bundle);
                        if (map == null) {
                            throw new RuntimeException("Unknown message bundle: " + bundle);
                        }
                        replacement = (String) map.get(key);
                        // Horrible hack so that apastrophes in messages
                        // won't break javascript strings.
                        replacement = replacement.replace("\\", "\\\\");
                        replacement = replacement.replace("'", "\\'");
                        replacement = replacement.replace("\"", "\\\"");
                    } else {
                        String key = m.group(3);
                        String param = m.group(4), url = m.group(5), var = (param == null) ? url : param;
                        replacement = (var == null) ? ComponentUtils.getMessage(key, component)
                          : ComponentUtils.formatMessage(key, component, var);
                        replacement = JsonUtils.toJson(replacement);
                    }
                    m.appendReplacement(sb, Matcher.quoteReplacement(replacement));
                }
                m.appendTail(sb);
                sb.append('\n');
                writer.write(sb.toString());
                sb.setLength(0);
            }
            writer.flush();
        } catch (RuntimeException ex) {
            throw new RuntimeException("Exception while localizing file: " + src.toString(), ex);
        }
    }

    @Nullable
    private String getMimeType(String fileName, boolean localized) {
        String mimeType;
        if (fileName.endsWith(".js")) {
            mimeType = MimeUtils.MIME_TYPE_TEXT_JAVASCRIPT;
        } else if (fileName.endsWith(".css") || styleCompilers.containsKey(FilenameUtils.getExtension(fileName))) {
            mimeType = MimeUtils.MIME_TYPE_TEXT_CSS;
        } else if (Current.getDomain() == null) {
            mimeType = URLConnection.guessContentTypeFromName(fileName);
        } else {
            mimeType = _mimeWebService.getMimeType(fileName);
        }
        if(mimeType == null){
            // need to stop here, or we end up spitting out "null;charset=utf8"
            return null;
        }
        return mimeType + (localized ? MimeUtils.CHARSET_SUFFIX_UTF_8 : "");
    }

    private static final Pattern CSS_URL_RE = Pattern.compile("(url\\(['\"]?)([^'\")]*)(['\"]?\\))");

    private abstract class Streamer {
        protected final Map<String, FileInfo> _files = new LinkedHashMap<String, FileInfo>();
        private boolean _missing = false;
        private boolean _localized = false;
        private long _lastModified = Long.MIN_VALUE;

        private HttpServletRequest _request;
        private HttpServletResponse _response;
        private String _version;

        public void init(HttpServletRequest request, HttpServletResponse response, String version) {
            _request = request;
            _response = response;
            _version = version;
        }

        public void deref() {
            for (FileInfo info : _files.values()) {
                info.deref();
            }
        }

        public void add(String resource) throws IOException, URISyntaxException {
            FileInfo info = getFileInfo(_request, _response, _version, resource);
            if (info == null) {
                logger.log(Level.WARNING, "Missing resource {0}", resource);
                _missing = true;
            } else {
                _files.put(resource, info);
                _lastModified = Math.max(_lastModified, info.getLastModified().getTime());
                _localized = info.getIsLocalized();
            }
        }

        public long getLastModified() {
            return _lastModified;
        }

        public boolean hasMissing() {
            return _missing;
        }

        public boolean isLocalized() {
            return _localized;
        }

        public Collection<FileInfo> getFiles() {
            return _files.values();
        }

        public abstract void stream(HttpServletRequest request, OutputStream out) throws IOException;
    }

    private class CompoundStreamer extends Streamer {
        public void stream(HttpServletRequest request, OutputStream out) throws IOException {
            for (Map.Entry<String, FileInfo> entry : _files.entrySet()) {
                String resource = entry.getKey();
                try (InputStream in = entry.getValue().openInputStream()) {
                    if (resource.endsWith(".css")) {
                        rewriteCss(resource, in, out);
                    } else {
                        IOUtils.copy(in, out);
                    }
                }
            }
        }

        private void rewriteCss(String resource, InputStream in, OutputStream out) throws IOException {
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, CharEncoding.UTF_8));
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out, CharEncoding.UTF_8));
            String[] path = resource.split("/"); // [ alpha, beta, gamma.css ]
            String line;
            StringBuffer sb = new StringBuffer();
            while ((line = reader.readLine()) != null) {
                Matcher m = CSS_URL_RE.matcher(line);
                while (m.find()) {
                    String prefix = m.group(1);
                    String url = m.group(2);
                    String suffix = m.group(3);
                    int n = path.length - 1;
                    while (url.startsWith("../")) {
                        url = url.substring(3);
                        -- n;
                    }
                    String base = (n > 0) ? StringUtils.join(path, '/', 0, n) + "/" : "";
                    String replacement = prefix + (url.startsWith("data:") ? "" : base) + url + suffix;
                    m.appendReplacement(sb, Matcher.quoteReplacement(replacement));
                }
                m.appendTail(sb);
                sb.append('\n');
                writer.write(sb.toString());
                sb.setLength(0);
            }
            writer.flush();
        }
    }
}
