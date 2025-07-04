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

package com.learningobjects.de.style;

import com.learningobjects.cpxp.BaseServiceMeta;
import com.learningobjects.cpxp.component.ComponentDescriptor;
import com.learningobjects.cpxp.controller.domain.DomainAppearance;
import com.learningobjects.cpxp.util.FileUtils;
import com.learningobjects.cpxp.util.StringUtils;
import de.larsgrefer.sass.embedded.SassCompilationFailedException;
import de.larsgrefer.sass.embedded.SassCompilerFactory;
import de.larsgrefer.sass.embedded.connection.BundledPackageProvider;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.io.file.PathUtils;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.sass_lang.embedded_protocol.OutputStyle.COMPRESSED;
import static com.sass_lang.embedded_protocol.OutputStyle.EXPANDED;

public class LibSassStyleCompiler extends AbstractSassCompiler implements StyleCompiler {

    private final static Logger logger = Logger.getLogger(LibSassStyleCompiler.class.getName());

    public LibSassStyleCompiler() {
        try {
            // This library is "clever" and doesn't unpack itself if its temp directory still exists.
            // But "clever" operating systems delete its files without deleting its directories so it
            // gets into a permanent dead state. So purge that directory every startup.
            var provider = new BundledPackageProvider();
            var method = provider.getClass().getSuperclass().getDeclaredMethod("getTargetPath");
            method.setAccessible(true);
            var path = (Path) method.invoke(provider);
            logger.log(Level.INFO, "Cleaning larsgrefer path: " + path);
            PathUtils.deleteDirectory(path);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error cleaning larsgrefer", e);
        }
    }

    @Override
    public void compileStyle(final HttpServletRequest request, final Path src, final ComponentDescriptor component, final OutputStream out, final String resultFileName, final DomainAppearance appearance) throws StyleCompileException {
        withTempDirs(tempDirs -> {
            try {
                final Path sourceDirectory;
                try {
                    Path sourcePath = Paths.get(component.getResource("/").toURI());
                    if (sourcePath.getFileSystem() == FileSystems.getDefault()) {
                    /* it's a directory on the fs (probably in an exploded archive) */
                        sourceDirectory = sourcePath;
                    } else {
                    /* it's a zipped component archive or jar */
                        sourceDirectory = tempDirs.get();
                        logger.info("Walking component root " + sourcePath.toUri() + " to " + sourceDirectory.toUri());
                        Files.walkFileTree(sourcePath, new UnzippingVisitor(sourceDirectory));
                    }
                } catch (IOException | URISyntaxException exn) {
                    throw new StyleCompileException("Resource path " + component.getResource("/") + " cannot be converted to a valid URI", exn);
                }

                logger.info("Creating SassContext from: " + src.toUri());



                File styleOutputFolder = tempDirs.get().toFile();
                Path serverImportsPath = Paths.get(styleOutputFolder.getAbsolutePath() + File.separator + AbstractSassCompiler.RELATIVE_SERVER_IMPORTS_PATH);
                Files.createDirectory(serverImportsPath);

                logger.info("Writing variables file in: " + serverImportsPath);

                writeVariableFile(getVariables(appearance), serverImportsPath.toString());

                List<File> includePaths = new ArrayList<>();
                includePaths.add(sourceDirectory.resolve(src.getParent().toString().substring(1)).toFile());
                includePaths.add(serverImportsPath.toFile());
                Path compileSource;
                try {
                    includePaths.add(src.getParent().toFile());
                    compileSource = src;
                } catch (UnsupportedOperationException uoe) { // i should just check the src filesystem, no?
                    //assume src is in a jar.
                    final Path target = tempDirs.get();
                    final Path source = src.getParent();
                    logger.info("Walking source path from " + source.toUri() + " to " + target.toUri());
                    Files.walkFileTree(source, new UnzippingVisitor(target));
                    includePaths.add(target.toFile());
                    compileSource = target.resolve(src.toString().substring(1));
                }

                try (var compiler = SassCompilerFactory.bundled()) {
                    /* don't minify locally, for debugging help */
                    compiler.setOutputStyle(BaseServiceMeta.getServiceMeta().isLocal() ? EXPANDED : COMPRESSED);
                    compiler.setLoadPaths(includePaths);
                    var compiled = compiler.compile(compileSource.toUri().toURL());
                    String css = compiled.getCss();
                    out.write(css.getBytes(StandardCharsets.UTF_8));
                }
            } catch (SassCompilationFailedException | IOException e) {
                logger.info(e.getMessage());
                throw new StyleCompileException("Style compilation failed. ", e);
            }
        });
    }

    private Map<String, String> getVariables(DomainAppearance appearance) {
        // add defaults so our sass compilation doesn't fill the logs with errors on unstyled domains
        Map<String, String> variables = new HashMap<>();
        variables.put("color-primary", "#333333");
        variables.put("color-secondary", "#555555");
        variables.put("color-accent", "#FF0000");
        variables.put("brand-primary", "#333333");
        variables.putAll(appearance.getStyleConfigurations());
        return variables;
    }

    /** Perform an operation that requires temporary directories, then cleanup. */
    static void withTempDirs(Consumer<Supplier<Path>> f) throws StyleCompileException {
        try (TempDirs dirs = new TempDirs()) {
            f.accept(dirs);
        }
    }

    /** A supplier of temporary directories that cleans up afterwards. */
    private static final class TempDirs implements Supplier<Path>, AutoCloseable {
        private final List<File> _dirs = new ArrayList<>();

        @Override
        public Path get() {
            try {
                Path it = Files.createTempDirectory(LibSassStyleCompiler.class.getSimpleName());
                it.toFile().deleteOnExit();
                _dirs.add(it.toFile());
                return it;
            } catch (IOException ex) {
                throw new RuntimeException("Temp directory error", ex);
            }
        }

        @Override
        public void close() {
            _dirs.forEach(FileUtils::deleteQuietly);
        }
    }

    private static final class UnzippingVisitor extends SimpleFileVisitor<Path> {
        private final Path target;

        UnzippingVisitor(Path target) {
            this.target = target;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            Path targetFile = target.resolve(file.toString().substring(1));
            String extension = StringUtils.substringAfterLast(file.getFileName().toString(), ".");
            if (STYLE_EXTENSIONS.contains(extension)) {
                Files.copy(file, targetFile);
            }
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
            Path newDir = target.resolve(dir.toString().substring(1));
            if (!Files.exists(newDir)) {
                Files.createDirectories(newDir);
            }
            return FileVisitResult.CONTINUE;
        }
    }
}
