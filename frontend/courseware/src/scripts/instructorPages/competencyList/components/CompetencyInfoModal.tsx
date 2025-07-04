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

import { CompetencyWithRelations } from '../../../resources/CompetencyResource';
import { filter, map } from 'lodash';
import ContentLine from '../../../contentPlayerComponents/contentViews/ContentLine';
import { useTranslation } from '../../../i18n/translationContext';
import { UserWithRoleInfo } from '../../../utilities/rootSelectors';
import React from 'react';
import { Modal, ModalBody, ModalHeader } from 'reactstrap';

type CompetencyInfoModalFilter = 'SHOW_ALL' | 'ACTIVITIES_ONLY' | 'ASSIGNMENTS_ONLY';

const CompetencyInfoModal: React.FC<{
  competency: CompetencyWithRelations;
  viewingAs: UserWithRoleInfo;
  initialFilter: CompetencyInfoModalFilter;
  toggleModal: () => void;
}> = ({ competency, viewingAs, initialFilter, toggleModal }) => {
  const translate = useTranslation();

  const visibleRelations = filter(competency.relations, rel => {
    switch (initialFilter) {
      case 'SHOW_ALL':
        return true;
      case 'ACTIVITIES_ONLY':
        return !rel.isForCredit;
      case 'ASSIGNMENTS_ONLY':
        return rel.isForCredit;
    }
  });

  return (
    <Modal
      isOpen={true}
      toggle={toggleModal}
      size="lg"
      contentClassName="max-height"
    >
      <ModalHeader
        toggle={toggleModal}
        tag="div"
      >
        {competency.ancestors.map(c => (
          <div
            key={c.id}
            className="text-muted mb-2 competency-modal-crumb"
            style={{ fontSize: '1rem', lineHeight: '1.1' }}
          >
            {c.title}
          </div>
        ))}
        <h5
          className="h4"
          style={{ marginBottom: 0 }}
        >
          {competency.title}
        </h5>
      </ModalHeader>
      <ModalBody className="overflow-auto">
        {map(visibleRelations, rel => (
          <ContentLine
            key={`${rel.id}-${competency.id}`}
            content={rel}
            viewingAs={viewingAs}
          ></ContentLine>
        ))}
        {!visibleRelations.length && (
          <div>
            {translate(
              initialFilter === 'ASSIGNMENTS_ONLY'
                ? 'NO_COMPETENCY_ASSIGNMENTS'
                : 'NO_COMPETENCY_ACTIVITIES'
            )}
          </div>
        )}
      </ModalBody>
    </Modal>
  );
};

export default CompetencyInfoModal;
