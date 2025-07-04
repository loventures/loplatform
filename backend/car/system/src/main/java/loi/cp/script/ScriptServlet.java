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

package loi.cp.script;

import com.learningobjects.cpxp.WebContext;
import com.learningobjects.cpxp.component.AbstractComponent;
import com.learningobjects.cpxp.component.ComponentEnvironment;
import com.learningobjects.cpxp.component.annotation.*;
import com.learningobjects.cpxp.component.util.ComponentUtils;
import com.learningobjects.cpxp.component.web.ErrorResponse;
import com.learningobjects.cpxp.component.web.FileResponse;
import com.learningobjects.cpxp.component.web.WebResponse;
import com.learningobjects.cpxp.operation.Operations;
import com.learningobjects.cpxp.operation.VoidOperation;
import com.learningobjects.cpxp.service.ServiceContext;
import com.learningobjects.cpxp.service.domain.DomainWebService;
import com.learningobjects.cpxp.service.query.QueryService;
import com.learningobjects.cpxp.shale.JsonMap;
import com.learningobjects.cpxp.util.HttpUtils;
import com.learningobjects.cpxp.util.ManagedUtils;
import com.learningobjects.cpxp.util.logging.LogCapture;
import com.learningobjects.cpxp.util.logging.MinimalFormatter;
import com.learningobjects.cpxp.util.logging.ThreadLogWriter;
import com.learningobjects.cpxp.util.task.Priority;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSessionBindingEvent;
import jakarta.servlet.http.HttpSessionBindingListener;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import javax.inject.Inject;
import javax.script.ScriptEngine;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Component
@EnforceOverlord
public class ScriptServlet extends AbstractComponent {
    private static final Logger logger = Logger.getLogger(ScriptServlet.class.getName());
    enum Language {
        Scala,
        SQL,
        Redshift
    }
    private static final String ENGINE_ATTR = "ug:engine";

    @PathInfo
    private String _lang;

    @Inject
    DomainWebService _domainWebService;

    @Inject
    QueryService _queryService;

    @Inject
    private ComponentEnvironment componentEnvironment;

    @Inject
    private WebContext webContext;

    public Language getLanguage() {
        if ("/sql".equals(_lang)) {
            return Language.SQL;
        } if("/redshift".equals(_lang)) {
            return Language.Redshift;
        } else {
            return Language.Scala;
        }
    }

    private static class BoxWriter extends PrintWriter {
        private final Box _box;
        private final StringBuffer _sb;

        BoxWriter(Box box) {
            this(box, new StringWriter());
        }

        private BoxWriter(Box box, StringWriter sw) {
            super(sw);
            _box = box;
            _sb = sw.getBuffer();
        }

        StringBuffer getStringBuffer() {
            return _sb;
        }

        @Override
        public void write(String s) {
            synchronized(_box) {
                if (!_box.complete) {
                    super.write(s);
                    if (s.endsWith("\n")) {
                        _box.notify();
                    }
                }
            }
        }
    }

    static class Box implements java.io.Serializable, HttpSessionBindingListener {
        public transient ScriptEngine engine;
        public boolean complete;
        public Throwable throwable;
        public BoxWriter writer;
        public StringBuffer sb;
        public BoxWriter log;
        public File file;
        public List<File> tmpFiles = new ArrayList<>();
        @Override
        public void valueBound(HttpSessionBindingEvent e) {
        }
        @Override
        public void valueUnbound(HttpSessionBindingEvent e) {
            finalize();
        }
        @Override
        protected void finalize() {
            for (File file : tmpFiles) {
                file.delete();
            }
        }
    }

    private boolean isScalacDebug() {
        return HttpUtils.getCookie(webContext.getRequest(), "scalac_debug") != null;
    }

    private ScriptEngine createScriptEngine(String language, PrintWriter writer) throws Exception {
        if (!language.equalsIgnoreCase("scala"))
            return null;

        var engine = DEScalaEngine.newEngine(writer, componentEnvironment.getClassLoader(), isScalacDebug());
        engine.put("BaseWebContext", webContext);
        engine.put("ComponentEnvironment", componentEnvironment);
        engine.put("ServiceContext", ServiceContext.getContext());
        engine.eval(INIT_SCALA);
        return engine;
    }

    private Box getEngine(HttpServletRequest request,String language) throws Exception {
        Box box = (Box) request.getSession().getAttribute(ENGINE_ATTR+'_'+language.toLowerCase());
        if (box == null) {
            box = new Box();
            BoxWriter bw = new BoxWriter(box);
            ScriptEngine engine = createScriptEngine(language, bw);
            if (engine != null) {
                box.engine = engine;
                engine.getContext().setWriter(bw);
            }
            box.writer = bw;
            box.sb = bw.getStringBuffer();
            box.log = new BoxWriter(box);
            request.getSession().setAttribute(ENGINE_ATTR+'_'+language.toLowerCase(), box);
        }
        return box;
    }

    @Rpc
    public JsonMap execute(@Parameter final String language, @Parameter final String script, @Parameter final Boolean extendedTimeout, @Parameter final Long domainId, HttpServletRequest request, HttpServletResponse response) throws Exception {
        logger.log(Level.WARNING, "Execute script:\n{0}", script);
        final Box box = getEngine(request,language);
        synchronized (box) {
            box.complete = false;
            box.throwable = null;
            box.file = null;
            box.sb.setLength(0);
        }
        if (domainId != null) {
            _domainWebService.setupContext(domainId);
        }
        Operations.deferTransact(new VoidOperation() {
                @Override
                public void execute() {
                    ThreadLogWriter threadLogs = new ThreadLogWriter(box.log, new MinimalFormatter(ScriptServlet.class, false), Thread.currentThread());
                    LogCapture.captureLogs(threadLogs, () -> {
                        try {
                            if("Redshift".equalsIgnoreCase(language) || (language == null)) {
                                RedshiftScriptRunner$.MODULE$.run(
                                  Optional.ofNullable(script).orElse("").trim(),
                                  box);
                            } else if ("SQL".equalsIgnoreCase(language) || (language == null)) {
                                SqlScriptRunner$.MODULE$.run(
                                  Optional.ofNullable(script).orElse("").trim(),
                                  BooleanUtils.isTrue(extendedTimeout),
                                  box);
                            } else {
                                Writer pw = box.engine.getContext().getWriter();
                                Object o = box.engine.eval(script);
                                ManagedUtils.commit();
                                if (o instanceof File) {
                                    File file = (File) o;
                                    if (!file.exists() || !file.isFile()) {
                                        throw new FileNotFoundException(file.getPath());
                                    }
                                    box.file = file;
                                } else if (o != null) {
                                    pw.write(language.equalsIgnoreCase("scala") ? replLike(o) : ComponentUtils.toJson(o) + "\n");
                                }
                            }
                            synchronized (box) {
                                box.complete = true;
                                box.notify();
                            }
                        } catch (Throwable th) {
                            ManagedUtils.rollback();
                            synchronized (box) {
                                box.complete = true;
                                box.throwable = th;
                                box.notify();
                            }
                            logger.log(Level.WARNING, "Script error", th);
                        }
                    });
                }
            }, Priority.Low, "Script");
        ManagedUtils.commit();
        return poll(language,request);
    }

    private String replLike(Object o){
       return "\""+o.toString()+"\": "+o.getClass().getName()+"\n";
    }

    @Rpc
    public JsonMap poll(@Parameter final String language, HttpServletRequest request) throws Exception {
        JsonMap map = new JsonMap();
        final Box box = getEngine(request,language);
        synchronized (box) {
            int n = box.sb.length();
            if (!box.complete && ((n == 0) || (box.sb.charAt(n - 1) != '\n'))) {
                box.wait(5000L);
                if (!box.complete) {
                    box.wait(100L); // handle a println just before completion better
                }
            }
            n = box.sb.length();
            if ((n > 0) && (box.sb.charAt(n - 1) == '\n') ) {
                map.put("stdout", box.sb.toString());
                box.sb.setLength(0);
            }
            if (box.throwable != null) {
                StringWriter sw = new StringWriter();
                ExceptionUtils.printRootCauseStackTrace(box.throwable, new PrintWriter(sw));
                map.put("stderr", sw.toString());
            }

            StringBuffer log = box.log.getStringBuffer();
            int m = log.length();
            if ((m > 0) && (log.charAt(m - 1) == '\n')) {
                map.put("stdlog", log.toString());
                log.setLength(0);
            }

            map.put("complete", box.complete);
            if (box.file != null) {
                map.put("filename", box.file.getName());
                map.put("filesize", box.file.length());
            }
        }
        return map;
    }

    @Rpc
    public JsonMap sqlHints() {
        var map = new JsonMap();
        var tableQuery = _queryService.createNativeQuery("SELECT table_name, column_name from information_schema.columns WHERE table_schema='public' ORDER BY table_name, column_name");
        List<Object[]> results = tableQuery.getResultList();
        map.putAll(results.stream().collect(
            Collectors.groupingBy(result -> result[0].toString(),
              Collectors.mapping(result -> result[1].toString(), Collectors.toList())
            )
          ));
        return map;
    }

    @Rpc
    public WebResponse download(@Parameter final String language, HttpServletRequest request, HttpServletResponse response) throws Exception {
        final var box = getEngine(request, language);
        if ((box.file == null) || !box.file.exists()) {
            return ErrorResponse.notFound();
        }
        return FileResponse.apply(box.file);
    }

    private static final String INIT_SCALA =
      "import com.learningobjects.cpxp.scala.cpxp.Component._\n" +
      "import com.learningobjects.cpxp.scala.cpxp.Facade._\n" +
      "import com.learningobjects.cpxp.scala.cpxp.Service._\n" +
      "import com.learningobjects.cpxp.scala.cpxp.Summon._\n";
}
