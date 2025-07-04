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

import { CustomisableContent, applyDay, applyTime } from '../../api/customizationApi';
import { isForCredit } from '../../utilities/creditTypes';
import { Option, getOrElse, isPresent, map, fold as opFold } from '../../types/option';

import { ContentEdit, match } from './contentEdits';
import { Tree, find } from './Tree';

/**
 * Represents an the change made to a value, both with the old value and new value,
 * one of which can be undefined
 *
 * Similar to scalaz's \&/
 */
export type Diff<T> = { old?: T | null; new?: T | null };

/**
 * A complete diff of a single piece of content
 */
export type ContentDiff = {
  content: CustomisableContent;
  position: Option<Diff<number>>;
  hidden: Option<Diff<boolean>>;
  title: Option<Diff<string>>;
  instructions: Option<Diff<string>>;
  isForCredit: Option<Diff<boolean>>;
  pointsPossible: Option<Diff<number>>;
  dueDate: Option<Diff<Date>>;
};

/**
 * A course's worth of Diffs, where each key is the id of the content of the Diff
 */
export type CourseDiff = {
  [id: string]: ContentDiff;
};

/**
 * Take two optional values and construct a Diff object. Returns undefined if both items
 * are equivalent
 */
export function toDiff<T>(old: Option<T>, nue: Option<T>): Option<Diff<T>> {
  if (old === nue) {
    return undefined;
  } else if (!isPresent(old) && !isPresent(nue)) {
    return undefined;
  } else if (typeof nue === 'undefined') {
    // todo: i think this is wrong
    return undefined;
  } else {
    return { old, new: nue };
  }
}

/**
 * Computes a diff based on the original tree (with no edits applied)
 * and the customizations that will be applied
 *
 * @param tree
 * @param edits
 */
export function computeDiff(course: Tree<CustomisableContent>, edits: ContentEdit[]): CourseDiff {
  /**
   * Helper method that provides a mapping method for a given content id.
   */
  function updateDiff(
    diff: CourseDiff,
    id: string,
    newContentDiff: (content: CustomisableContent) => Partial<ContentDiff>
  ): CourseDiff {
    const contentDiff = diff[id];
    const content = getOrElse(
      map(contentDiff, d => d.content),

      find(course, c => c.id === id)!.value
    );

    return {
      ...diff,
      [id]: {
        ...contentDiff,
        content,
        ...newContentDiff(content),
      },
    };
  }

  /**
   * Applies a single ContentEdit to an entire CourseDiff
   */
  function applyEdit(diff: CourseDiff, edit: ContentEdit): CourseDiff {
    return match(edit, {
      rename: r => updateDiff(diff, r.id, c => ({ title: toDiff(c.title, r.name) })),
      reinstruct: r =>
        updateDiff(diff, r.id, c => ({ instructions: toDiff(c.instructions, r.instructions) })),
      setForCredit: s =>
        updateDiff(diff, s.id, c => ({
          isForCredit: toDiff(c.isForCredit, isForCredit(s.type)),
        })),
      hideChild: h => {
        const oldHidden = find(course, n => n.id === h.id)!.value.hide || [];
        return updateDiff(diff, h.childId, () => ({
          hidden: toDiff(oldHidden.includes(h.childId), h.hidden),
        }));
      },
      move: m => {
        const parent = find(course, n => n.id === m.id)!.value;
        const oldPosition = parent.order.indexOf(m.childId);
        return updateDiff(diff, m.childId, () => ({
          position: toDiff(oldPosition, m.newPosition),
        }));
      },
      changeDueDay: cdd => {
        return updateDiff(diff, cdd.id, c => {
          const contentDiff = diff[cdd.id];
          const useContentDate = () => c.dueDate;

          const contentDiffDate = opFold(
            contentDiff?.dueDate,
            useContentDate,
            dueDate => dueDate.new
          );
          const newDate = applyDay(cdd)(contentDiffDate);
          return { dueDate: toDiff(c.dueDate, newDate) };
        });
      },
      changeDueTime: cdt => {
        return updateDiff(diff, cdt.id, c => {
          const contentDiff = diff[cdt.id];
          const useContentDate = () => c.dueDate;

          const contentDiffDate = opFold(
            contentDiff?.dueDate,
            useContentDate,
            dueDate => dueDate.new
          );
          const newDate = applyTime(cdt)(contentDiffDate);
          return { dueDate: toDiff(c.dueDate, newDate) };
        });
      },
      changePointsPossible: r =>
        updateDiff(diff, r.id, c => ({
          pointsPossible: toDiff(c.pointsPossible, r.newPointsPossible),
        })),
      removeDueDate: r => updateDiff(diff, r.id, c => ({ dueDate: toDiff(c.dueDate, null) })),
    });
  }

  return edits.reduce(applyEdit, {});
}
