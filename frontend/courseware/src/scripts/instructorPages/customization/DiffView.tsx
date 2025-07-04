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

import { CustomisableContent } from '../../api/customizationApi';
import dayjs from 'dayjs';
import localized from 'dayjs/plugin/localizedFormat';
import { includes, isNumber } from 'lodash';
import { WithTranslate } from '../../i18n/translationContext';
import { Option } from '../../types/option';
import React from 'react';
import { withState } from 'recompose';

import { ContentDiff, CourseDiff, Diff } from './ContentDiff';
import { Tree, findPath } from './Tree';
import { MdArrowRightAlt, MdKeyboardArrowDown, MdKeyboardArrowRight } from 'react-icons/md';

dayjs.extend(localized);

type DiffViewProps = {
  diffs: ContentDiff[];
  module: CustomisableContent;
  open: boolean;
  setOpen: (b: boolean) => boolean;
};

/**
 * Returns true if the specified content is underneath the module
 * @param course
 * @param moduleId
 * @param cId
 */
const isIn = (course: Tree<CustomisableContent>, moduleId: string, contentId: string): boolean => {
  const contentPath = findPath(
    course,
    n => n.id === contentId,
    c => c.id
  );
  return contentPath ? includes(contentPath, moduleId) : false;
};

const DiffViewInner = ({ diffs, module, open, setOpen }: DiffViewProps) => (
  <WithTranslate>
    {translate => (
      <>
        <div className="module-name">
          {open ? (
            <button
              className="icon-btn"
              onClick={() => setOpen(false)}
            >
              <MdKeyboardArrowDown />
            </button>
          ) : (
            <button
              className="icon-btn"
              onClick={() => setOpen(true)}
            >
              <MdKeyboardArrowRight />
            </button>
          )}
          <h5>{module.title}</h5>
          <span className="details">
            {translate('UPDATE_COUNT', { count: diffs.length }, 'messageformat')}
          </span>
        </div>
        {open &&
          diffs.map(diff => (
            <div
              className="content-node"
              key={diff.content.id}
            >
              <span className="name">{diff.content.title}</span>
              <Change diff={diff.title}>{t => t}</Change>
              <Change diff={diff.instructions}>
                {t => <div dangerouslySetInnerHTML={{ __html: t ?? '' }} />}
              </Change>
              <Change diff={diff.isForCredit}>{t => (t ? 'For Credit' : 'Not For Credit')}</Change>
              <Change diff={diff.dueDate}>
                {date => (date ? dayjs(date).format('LLL') : 'No Due Date')}
              </Change>
              <Change diff={diff.hidden}>{hidden => (hidden ? 'Hidden' : 'Visible')}</Change>
              <Change diff={diff.pointsPossible}>{points => `${points} Points`}</Change>
              <Change diff={diff.position}>{p => `Position ${isNumber(p) ? p + 1 : 0}`}</Change>
            </div>
          ))}
      </>
    )}
  </WithTranslate>
);

const DiffView = withState('open', 'setOpen', true)(DiffViewInner);

/**
 * Renders a computed diff for the user.
 */
export const CourseDiffView: React.FC<{
  modules: {
    module: CustomisableContent;
    diffs: ContentDiff[];
  }[];
}> = ({ modules }) => (
  <WithTranslate>
    {translate => (
      <div className="customizations-review">
        <span className="mb-2">
          {translate('MODULE_CHANGE_COUNT', { count: modules.length }, 'messageformat')}
        </span>
        {modules.map(change => (
          <DiffView
            {...change}
            key={change.module.id}
          />
        ))}
      </div>
    )}
  </WithTranslate>
);

const Change: <T>(props: {
  diff?: Option<Diff<T>>;
  children: (t: T | null | undefined) => React.ReactNode;
}) => any = ({ diff, children }) =>
  diff ? (
    <span className="change">
      <span className="old">{children(diff.old)}</span>
      <MdArrowRightAlt />
      <span className="new">{children(diff.new)}</span>
    </span>
  ) : null;

function contentDiffIsEmpty(c: ContentDiff): boolean {
  return (Object.keys(c) as (keyof ContentDiff)[])
    .filter(k => k !== 'content')
    .every(k => typeof c[k] === 'undefined');
}

export function courseDiffIsEmpty(c: CourseDiff): boolean {
  return Object.keys(c).every(k => contentDiffIsEmpty(c[k]));
}

/**
 * Groups a set of ContentDiffs by their module
 * @param courseDiff
 * @param course
 */
export const splitByModule = (courseDiff: CourseDiff, course: Tree<CustomisableContent>) => {
  // group all changes under their module
  const contentDiffs = Object.keys(courseDiff)
    .filter(cdKey => !contentDiffIsEmpty(courseDiff[cdKey]))
    .map(cdKey => courseDiff[cdKey]);

  const foo = course.children
    .map(m => m.value)
    .flatMap(module => {
      const diffs = contentDiffs.filter(diff => isIn(course, module.id, diff.content.id));
      if (diffs.length == 0) {
        return [];
      } else {
        return [{ module, diffs }];
      }
    });

  return foo;
};
