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

import { UserInfo } from '../../../../loPlatform';
import StudentPickerModal from '../../../components/StudentPickerModal';
import { TranslationContext } from '../../../i18n/translationContext';
import React, { useContext, useState } from 'react';
import Button from 'reactstrap/lib/Button';
import ButtonGroup from 'reactstrap/lib/ButtonGroup';
import FormGroup from 'reactstrap/lib/FormGroup';

type GatingEditorSelectUserProps = {
  i18nKey: string;
  onSetStudent: (student: UserInfo) => void;
  onClearStudent: () => void;
};

const GatingEditorSelectUser: React.FC<GatingEditorSelectUserProps> = ({
  i18nKey,
  onSetStudent,
  onClearStudent,
}) => {
  const translate = useContext(TranslationContext);
  const [student, setStudent] = useState<string | null>(null);
  const [modalOpen, setModalOpen] = useState(false);

  return (
    <FormGroup className="mt-3">
      <span className="students-label me-2">{translate(i18nKey)}</span>
      <ButtonGroup>
        <Button
          color="primary"
          className="picker-button"
          title={translate('GATING_EDITOR_SELECT_STUDENT')}
          onClick={() => {
            setModalOpen(!modalOpen);
          }}
        >
          <span className="student-name">
            {student ? student : translate('GATING_EDITOR_ALL_STUDENT')}
          </span>
        </Button>
        {student && (
          <Button
            color="primary"
            className="reset-picker-button dropdown-toggle-split"
            onClick={() => {
              onClearStudent();
              setStudent(null);
            }}
          >
            <i className="icon icon-cross" />
          </Button>
        )}
      </ButtonGroup>
      <StudentPickerModal
        isOpen={modalOpen}
        onToggle={() => setModalOpen(!modalOpen)}
        onSetStudent={student => {
          onSetStudent(student);
          setStudent(student.fullName);
          setModalOpen(false);
        }}
      />
    </FormGroup>
  );
};

export default GatingEditorSelectUser;
