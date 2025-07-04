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

package loi.cp.status;

import loi.apm.Apm;
import org.apache.pekko.actor.ActorSystem;
import com.learningobjects.cpxp.BaseServiceMeta;
import com.learningobjects.cpxp.ServiceMeta;
import com.learningobjects.cpxp.component.ComponentSupport;
import com.learningobjects.cpxp.component.annotation.Component;
import com.learningobjects.cpxp.component.web.AbstractComponentServlet;
import com.learningobjects.cpxp.component.web.Method;
import com.learningobjects.cpxp.component.web.ServletBinding;
import com.learningobjects.cpxp.filter.AccessFilter;
import com.learningobjects.cpxp.filter.SendFileFilter;
import com.learningobjects.cpxp.operation.Executor;
import com.learningobjects.cpxp.scala.actor.CpxpActorSystem;
import com.learningobjects.cpxp.schedule.Scheduled;
import com.learningobjects.cpxp.service.session.SessionService;
import com.learningobjects.cpxp.util.*;
import com.sun.management.UnixOperatingSystemMXBean;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.io.output.NullWriter;

import javax.inject.Inject;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.io.Writer;
import java.lang.management.*;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;

@Component(
  name = "$$name=Machine-Readable Status Servlet",
  description = "$$description=This servlet reports system status in a machine readable format.",
  version = "0.7"
)
@ServletBinding(
  path = "/control/mstatus",
  system = true,
  transact = false
)
public class MStatusServlet extends AbstractComponentServlet {
    private static final Logger logger = Logger.getLogger(MStatusServlet.class.getName());

    private static final MBeanServer _mbs = ManagementFactory.getPlatformMBeanServer();
    private static final ClassLoadingMXBean _classLoadingBean = ManagementFactory.getClassLoadingMXBean();
    private static final List<GarbageCollectorMXBean> _gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
    private static final List<MemoryPoolMXBean> _memoryPoolBeans = ManagementFactory.getMemoryPoolMXBeans();
    private static final UnixOperatingSystemMXBean _osBean = (UnixOperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
    private static final ThreadMXBean _threadBean = ManagementFactory.getThreadMXBean();
    private static final MemoryMXBean _memoryBean = ManagementFactory.getMemoryMXBean();

    @Inject
    private SessionService _sessionService;

    @Inject
    private ActorSystem actorSystem;

    @Override
    public void get(HttpServletRequest request, HttpServletResponse response) throws Exception {
        HttpUtils.setExpired(response);

        Map<String, Object> status = new TreeMap<String, Object>(); // treemap sorts in alphabetical order for display

        AccessFilter.getStatus(status);

        final ServiceMeta serviceMeta = BaseServiceMeta.getServiceMeta();
        status.put("cp.Node.Name", serviceMeta.getNode());
        status.put("cp.Node.Host", serviceMeta.getLocalHost());
        status.put("cp.Node.Build", serviceMeta.getBuild());
        status.put("cp.Node.Version", serviceMeta.getVersion());
        status.put("cp.Node.Cluster", serviceMeta.getCluster());
        status.put("cp.Nio.Enabled", SendFileFilter.isNioAvailable());

        status.put("java.Version", serviceMeta.getJvm());

        status.put("jmx.ClassLoading.LoadedClassCount", _classLoadingBean.getLoadedClassCount());
        status.put("jmx.ClassLoading.UnloadedClassCount", _classLoadingBean.getUnloadedClassCount());

        for (GarbageCollectorMXBean gc : _gcBeans) {
            String gcName = gc.getName().replaceAll(" ", "");
            status.put("jmx.GarbageCollector." + gcName + ".CollectionCount", gc.getCollectionCount());
            status.put("jmx.GarbageCollector." + gcName + ".CollectionTime", gc.getCollectionTime());
        }

        MemoryUsage heapUsage = _memoryBean.getHeapMemoryUsage();
        MemoryUsage nonHeapUsage = _memoryBean.getNonHeapMemoryUsage();

        status.put("jvm.memory.Heap.Used", heapUsage.getUsed());
        status.put("jvm.memory.Heap.Committed", heapUsage.getCommitted());
        status.put("jvm.memory.Heap.Max", heapUsage.getMax());

        status.put("jvm.memory.NonHeap.Used", nonHeapUsage.getUsed());
        status.put("jvm.memory.NonHeap.Committed", nonHeapUsage.getCommitted());
        status.put("jvm.memory.NonHeap.Max", nonHeapUsage.getMax());

        for (MemoryPoolMXBean memoryPool : _memoryPoolBeans) {
            String memoryPoolName = memoryPool.getName().replaceAll(" ", "");
            MemoryUsage memoryUsage = memoryPool.getUsage();
            status.put("jmx.MemoryPool." + memoryPoolName + ".Used", memoryUsage.getUsed());
            status.put("jmx.MemoryPool." + memoryPoolName + ".Committed", memoryUsage.getCommitted());
            status.put("jmx.MemoryPool." + memoryPoolName + ".Max", memoryUsage.getMax());
        }

        status.put("jmx.OperatingSystem.OpenFileDescriptorCount", _osBean.getOpenFileDescriptorCount());
        status.put("jmx.OperatingSystem.MaxFileDescriptorCount", _osBean.getMaxFileDescriptorCount());
        status.put("jmx.OperatingSystem.SystemLoadAverage", _osBean.getSystemLoadAverage());

        status.put("jmx.Threading.DaemonThreadCount", _threadBean.getDaemonThreadCount());
        status.put("jmx.Threading.ThreadCount", _threadBean.getThreadCount());
        status.put("jmx.Threading.PeakThreadCount", _threadBean.getPeakThreadCount());


        ObjectName jdbcPool = ObjectName.getInstance("com.zaxxer.hikari:type=Pool (underground)");
        status.put("jdbc.ConnectionPool.Active", _mbs.getAttribute(jdbcPool, "ActiveConnections"));
        status.put("jdbc.ConnectionPool.Idle", _mbs.getAttribute(jdbcPool, "IdleConnections"));
        status.put("jdbc.ConnectionPool.Size", _mbs.getAttribute(jdbcPool, "TotalConnections"));
        status.put("jdbc.ConnectionPool.WaitCount", _mbs.getAttribute(jdbcPool, "ThreadsAwaitingConnection"));


        ObjectName threadPools = new ObjectName("*:type=ThreadPool,name=*");
        for (ObjectName threadPool : _mbs.queryNames(threadPools, null)) {
            String prefix = "catalina." + threadPool.getKeyProperty("name").replaceAll("\"", "");
            status.put(prefix + ".CurrentThreadCount", _mbs.getAttribute(threadPool, "currentThreadCount"));
            status.put(prefix + ".CurrentThreadsBusy", _mbs.getAttribute(threadPool, "currentThreadsBusy"));
            status.put(prefix + ".MaxThreads", _mbs.getAttribute(threadPool, "maxThreads"));
        }

        ObjectName catalinaMgr = new ObjectName("*:type=Manager,context=/,host=localhost");
        for (ObjectName catalina : _mbs.queryNames(catalinaMgr, null)) {
            status.put("catalina.Session.Active", _mbs.getAttribute(catalina, "activeSessions"));
            status.put("catalina.Session.Count", _mbs.getAttribute(catalina, "sessionCounter"));
            status.put("cp.Session.Count", _sessionService.getActiveSessionCount()); // TODO: per node?
        }
        for (MStatusComponent component : ComponentSupport.lookupAll(MStatusComponent.class)) {
            MStatusBinding binding = ComponentSupport.getBinding(component, MStatusComponent.class);
            for (Map.Entry<String, Object> entry : component.getMStatus().entrySet()) {
                status.put(String.format("%1$s.%2$s", binding.value(), entry.getKey()), entry.getValue());
            }
        }

        MonitoredHttpClient.getStatus(status);

        CpxpActorSystem.updateStatus(status, actorSystem);

        S3Statistics.updateStatus(status);

        /*
        Set<ObjectInstance> instances = _mbs.queryMBeans(null, null);
        Iterator<ObjectInstance> iterator = instances.iterator();
        while (iterator.hasNext()) {
            ObjectInstance instance = iterator.next();
            MBeanInfo info = _mbs.getMBeanInfo(instance.getObjectName());
            for (MBeanAttributeInfo attInfo : info.getAttributes()) {
                String ikey = instance.getObjectName() + "//" + attInfo.getName();
                try {
                    status.put(ikey, _mbs.getAttribute(instance.getObjectName(), attInfo.getName()));
                } catch (Throwable th) {
                    status.put(ikey, th.toString());
                }
            }
        }
        */

        Executor.getStatus(status);

        boolean json = "json".equals(request.getParameter("format"));
        String mimeType = json ? MimeUtils.MIME_TYPE_APPLICATION_JSON : MimeUtils.MIME_TYPE_TEXT_PLAIN;
        response.setContentType(mimeType + MimeUtils.CHARSET_SUFFIX_UTF_8);
        Writer writer = Method.HEAD.name().equals(request.getMethod())
          ? new NullWriter() : response.getWriter();

        if (json) {
            writer.write(JsonUtils.toJson(status));
        } else {
            for (Map.Entry<String, Object> entry : status.entrySet()) {
                writer.write(entry.getKey() + "=" + entry.getValue() + "\n");
            }
        }

        writer.close();
    }

    @SuppressWarnings("unused")
    @Scheduled("1 minute")
    public void recordFileDescriptors() {
        final long fds = _osBean.getOpenFileDescriptorCount();
        logger.fine("Open file descriptors: " + fds);
        Apm.recordMetric("Custom/fileDescriptors", fds);
    }
}
