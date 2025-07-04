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

import { Content } from '../../../api/contentsApi';
import CollapseCard from '../../../components/CollapseCard';
import { ensureNotNull } from '../../../utils/utils';
import { includes } from 'lodash';
import React, { useContext } from 'react';
import { CardBody } from 'reactstrap';

import ActivityGateEditor from './ActivityGateEditor';
import GatedUserEditor from './GatedUserEditor';
import GatingEditorContext from './GatingEditorContext';
import OverridePolicyEditor from './OverridePolicyEditor';

const GatingEditorContentCard: React.FC<{ content: Content }> = ({ content }) => {
  const { activeStudent, overrides } = ensureNotNull(useContext(GatingEditorContext));
  return (
    <CollapseCard
      className="mb-2 gating-editor-content"
      initiallyOpen={false}
      renderHeader={() => <span>{content.name}</span>}
    >
      <CardBody>
        <OverridePolicyEditor contentId={content.id} />
        <ActivityGateEditor
          content={content}
          disabled={activeStudent !== null || includes(overrides.overall, content.id)}
        />
        {activeStudent === null && <GatedUserEditor contentId={content.id} />}
      </CardBody>
    </CollapseCard>
  );
};

export default GatingEditorContentCard;
