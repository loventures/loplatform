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

package loi.cp.notification;

import scala.Option;
import scala.Some;

/**
 * Subscription interest level. These look like bitmaps but are not currently used as such. They are
 * so-valued to allow database comparison (highest value wins) and potential future rework as a bitmap.
 */
public enum Interest {
    Notify(16), // just add to activity feed
    Alert(32), // send an alert about this
    Mute(128); // mute this item

    private final int value;

    Interest(int value) {
        this.value = value;
    }

    /** Ordinal stored in the database to enable comparisons. */
    public int getValue() {
        return value;
    }

    public static Option<Interest> apply(int value) {
        switch (value) {
            case 16: return Some.apply(Notify);
            case 32: return Some.apply(Alert);
            case 128: return Some.apply(Mute);
            default: return Option.empty();
        }
    }
}
