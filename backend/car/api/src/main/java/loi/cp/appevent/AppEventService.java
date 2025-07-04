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

package loi.cp.appevent;

import com.learningobjects.cpxp.Id;
import com.learningobjects.cpxp.component.annotation.Service;

import java.util.Date;

/**
 * App event service API.
 */
@Service
public interface AppEventService {
    void registerListener(Long listener, Class<? extends AppEvent> event, Long target);

    void deregisterListener(Long listener, Class<? extends AppEvent> event, Long target);

    void fireEvent(Id source, AppEvent event, Id... rels);

    void scheduleEvent(Date when, Id source, Id target, AppEvent event, Id... rels);

    void deleteEvents(Id source, Id target, Class<? extends AppEvent> eventType);

    Date getNextEventTime(Id source, Id target, Class<? extends AppEvent> eventType);

    /**
     * A structure to hold statistics about how quickly the app events are being processed.
     */
    public static class QueueStats {
        /** The number of appevents that should have been executed by now but instead are waiting */
        public long numInQueue;

        /**
         * The wait time of the event that has been waiting the longest - meaning how far beyond
         * the deadline is the event that is most overdue.
         * If numInQueue is 0, this will be 0.
         * This is in milliseconds. */
        public long currentWaitTimeMs;
    }

    /**
     * @return information about how quickly the app events are processed.  These stats are for
     * all the app events in the system, not just those that have affinity to this server.
     */
    QueueStats getQueueStats();
}
