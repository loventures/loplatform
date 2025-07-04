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

import { ContentId } from '../../../api/contentsApi';
import CollapseCard from '../../../components/CollapseCard';
import { TranslationContext } from '../../../i18n/translationContext';
import { ensureNotNull } from '../../../utils/utils';
import { filter, includes, isEmpty, keys, map, pickBy } from 'lodash';
import React, { useContext } from 'react';

import GatingEditorContext from './GatingEditorContext';

const GatedUserEditor: React.FC<{ contentId: ContentId }> = ({ contentId }) => {
  const translate = useContext(TranslationContext);
  const { overrides, usersById } = ensureNotNull(useContext(GatingEditorContext));

  const gatedUsers = filter(
    map(
      keys(pickBy(overrides.perUser, contentIds => includes(contentIds, contentId))),
      userId => usersById[userId]
    ),
    u => !!u
  );

  return (
    <>
      {!isEmpty(gatedUsers) && (
        <CollapseCard
          initiallyOpen={false}
          className="mb-2 user-policy-editor"
          headerClassName="bg-primary p-1"
          renderHeader={() => (
            <span className="policy-editor-title">
              {translate('GATING_POLICY_INDIVIDUALLY_DISABLED_USERS', {
                numberOfUsers: gatedUsers.length,
              })}
            </span>
          )}
        >
          <ul className="policy-editor-users m-2">
            {map(gatedUsers, u => (
              <li key={u.id}>{u.fullName}</li>
            ))}
          </ul>
        </CollapseCard>
      )}
    </>
  );
};

export default GatedUserEditor;
