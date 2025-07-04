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

import { capitalize } from 'lodash';

import { Polyglot } from '../types/polyglot';
import { ConfirmModalData, ConfirmationTypes } from './ConfirmModal';

/**
 * Q: Why does this file exist?
 *
 * A: These functions produce a typed ConfirmModalData for use with the ConfirmModal. Unfortunately
 *    bad things happen to our linters when you just change .js to .ts and start adding types (_especially_
 *    for angular code). So these helpers exist temporarily while the call sites get typed/rewritten.
 **/

export function getReuseConfirmConfig(polyglot: Polyglot, confirmCallback) {
  const confirmModalConfig: ConfirmModalData = {
    confirmationType: ConfirmationTypes.ConfirmUnderstandReuse,
    words: {
      header: polyglot.t('CONFIRM_REUSE_SAVE_CHANGES_HEADER'),
      body: polyglot.t('CONFIRM_REUSE_SAVE_CHANGES_BODY'),
      confirm: polyglot.t('SAVE'),
    },
    confirmCallback,
  };
  return confirmModalConfig;
}

export function getRemoveCompetencyConfirmConfig(
  polyglot: Polyglot,
  competencyTitle: string,
  numberOfChildren: number,
  confirmCallback
) {
  const comp = polyglot.t('CONFIRM_REMOVE_MODAL.COMPETENCY_BODY', {
    competencyTitle,
  });

  const start = '<br/> <br/>';
  const children = numberOfChildren
    ? polyglot.t('CONFIRM_REMOVE_MODAL.CHILD_COMPETENCIES', { smart_count: numberOfChildren })
    : '';
  const end0 = polyglot.t('CONFIRM_REMOVE_MODAL.END');
  const extras = numberOfChildren ? start + capitalize(children + end0) : '';

  const confirmConfig: ConfirmModalData = {
    confirmationType: ConfirmationTypes.RemoveCompetency,
    color: 'danger',
    words: {
      header: polyglot.t('CONFIRM_REMOVE_MODAL.HEADER'),
      htmlBody: comp + extras,
      confirm: polyglot.t('REMOVE'),
    },
    confirmCallback,
  };
  return confirmConfig;
}
