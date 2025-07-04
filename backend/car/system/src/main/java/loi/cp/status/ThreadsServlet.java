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

import com.learningobjects.cpxp.component.annotation.Component;
import com.learningobjects.cpxp.component.web.*;
import com.learningobjects.cpxp.util.NaturalOrder;
import com.learningobjects.cpxp.util.ThreadLog;
import com.learningobjects.cpxp.util.ThreadLog.ThreadInfo;
import com.learningobjects.cpxp.util.ThreadTerminator;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import loi.cp.overlord.EnforceOverlordAuth;

import java.util.*;

@SuppressWarnings("unused") // component
@Component
@EnforceOverlordAuth
@ServletBinding(
    path = "/sys/threads",
    system = true,
    transact = false
)
public class ThreadsServlet extends AbstractComponentServlet {

    @Override
    public WebResponse service(HttpServletRequest req, HttpServletResponse rsp) {
        switch(req.getMethod().toUpperCase()) {
            case "GET":
                return HtmlResponse.apply(this, "threads.html");
            case "POST":
                // TODO: The kill should be based on a *transaction id* managed by ThreadLog.
                // That way you are killing a transaction rather, than laying a mine for a
                // thread to stumble upon at some point in the future.
                Long id = Long.parseLong(req.getParameter("id"));
                ThreadTerminator.kill(id);
                findThreadById(id).ifPresent(Thread::interrupt);
                return NoContentResponse.instance();
            default:
                return ErrorResponse.methodNotAllowed();
        }
    }

    private static final Comparator<String> __comparator = NaturalOrder.getNaturalComparatorIgnoreCaseAscii();

    @SuppressWarnings("unused") /* lohtml */
    public List<ThreadInfo> getThreadInfo() {
        List<ThreadInfo> ti = ThreadLog.threads();
        Collections.sort(ti, (ThreadInfo t0, ThreadInfo t1) ->
            __comparator.compare(t0.getThread().getName(), t1.getThread().getName()));
        return ti;
    }

    @SuppressWarnings("unused") /* lohtml */
    public List<Map.Entry<Thread, StackTraceElement[]>> getStackTraces() {
        List<Map.Entry<Thread, StackTraceElement[]>> st = new ArrayList<>(Thread.getAllStackTraces().entrySet());
        Collections.sort(st, (Map.Entry<Thread, StackTraceElement[]> t0, Map.Entry<Thread, StackTraceElement[]> t1) ->
            __comparator.compare(t0.getKey().getName(), t1.getKey().getName()));
        return st;
    }

    // TODO: Switch to ThreadUtils.findThreadById once commons lang 3.5 ships
    private static Optional<Thread> findThreadById(Long id) {
        ThreadGroup group = Thread.currentThread().getThreadGroup();
        while (group.getParent() != null) {
            group = group.getParent();
        }
        int count = group.activeCount();
        Thread[] threads;
        do {
            threads = new Thread[count + (count / 2) + 1];
            count = group.enumerate(threads, true);
        } while (count >= threads.length);
        for (int i = 0; i < count; ++i) {
            if (threads[i].getId() == id) {
                return Optional.of(threads[i]);
            }
        }
        return Optional.empty();
    }
}
