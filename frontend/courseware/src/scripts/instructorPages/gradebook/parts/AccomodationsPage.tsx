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

import axios from 'axios';
import { UserInfo } from '../../../../loPlatform';
import { EdgePath } from '../../../api/commonTypes';
import Course from '../../../bootstrap/course';
import GatingEditorSelectUser from '../../../instructorPages/gatingEditorPage/parts/GatingEditorSelectUser';
import {
  ContentWithAncestors,
  useFlatLearningPathResource,
} from '../../../resources/LearningPathResource';
import { LoCheckbox } from '../../../directives/LoCheckbox';
import { useTranslation } from '../../../i18n/translationContext';
import React, { useCallback, useEffect, useState } from 'react';
import { Button, Card, CardBody, CardHeader, FormGroup, Input, Label, Spinner } from 'reactstrap';

type Accommodations = Record<EdgePath, number>;

export const loadAccommodations = (user: number): Promise<Accommodations> =>
  axios
    .get<Accommodations>(`/api/v2/lwc/${Course.id}/accommodations/${user}`)
    .then(res => res.data);

export const putAccommodation = (
  user: number,
  path: EdgePath,
  maxMinutes: number | null
): Promise<void> =>
  axios.put(`/api/v2/lwc/${Course.id}/accommodations/${user}/${path}`, { maxMinutes }).then();

export const AccomodationsPage: React.FC = () => {
  const translate = useTranslation();
  const learningPath = useFlatLearningPathResource();
  const timeLimited = learningPath.filter(c => c.maxMinutes);

  const [activeStudent, setActiveStudent] = useState<UserInfo | null>(null);
  const [accommodations, setAccommodations] = useState<Accommodations>();

  useEffect(() => {
    setAccommodations(undefined);
    if (activeStudent) {
      loadAccommodations(activeStudent.id).then(setAccommodations);
    }
  }, [activeStudent]);

  const updateAccommodation = useCallback(
    (edgePath: EdgePath, accommodation: number | undefined) => {
      setAccommodations(a => {
        if (accommodation == null) {
          const { [edgePath]: _, ...rest } = a!;
          return rest;
        } else {
          return { ...a!, [edgePath]: accommodation };
        }
      });
    },
    [activeStudent]
  );

  return !timeLimited.length ? (
    <div className="my-5 text-center text-muted">{translate('ACCOMMODATIONS_EMPTY')}</div>
  ) : (
    <div className="accommodations-editor">
      <GatingEditorSelectUser
        i18nKey="ACCOMMODATIONS_FOR"
        onClearStudent={() => setActiveStudent(null)}
        onSetStudent={student => {
          setActiveStudent(student);
        }}
      />
      {activeStudent && !accommodations ? (
        <div className="my-5 text-center">
          <Spinner color="muted" />
        </div>
      ) : (
        timeLimited.map(content => {
          return (
            <Card
              className="mb-2 accommodation-card"
              key={`${content.id}-${activeStudent?.id}`}
            >
              <CardHeader>
                {content.module ? content.module.name + ' / ' : ''}
                {content.lesson ? content.lesson.name + ' / ' : ''}
                {content.name}
              </CardHeader>
              <CardBody className="mb-last-0">
                <div className="mb-3">
                  {translate('ACCOMMODATIONS_TIME_LIMIT')}
                  <strong className="ms-2">
                    {translate('ACCOMMODATIONS_MAX_MINUTES', { max: content.maxMinutes })}
                  </strong>
                </div>
                {activeStudent && (
                  <AccommodationEditor
                    student={activeStudent}
                    content={content}
                    accommodation={accommodations?.[content.id]}
                    setAccommodation={updateAccommodation}
                  />
                )}
              </CardBody>
            </Card>
          );
        })
      )}
    </div>
  );
};

const AccommodationEditor: React.FC<{
  student: UserInfo;
  content: ContentWithAncestors;
  accommodation: number | undefined;
  setAccommodation: (edgePath: EdgePath, accommodation: number | undefined) => void;
}> = ({ student, content, accommodation, setAccommodation }) => {
  const translate = useTranslation();
  const [original, setOriginal] = useState(accommodation);

  return (
    <div className="d-flex align-items-center gap-3 accommodation-editor">
      <LoCheckbox
        checkboxFor={`accommodation-toggle-${student.id}-${content.id}`}
        checkboxLabel="HAS_ACCOMMODATION"
        onToggle={() =>
          setAccommodation(content.id, accommodation == null ? content.maxMinutes! : undefined)
        }
        state={accommodation != null}
      />
      <FormGroup
        check
        className="flex-shrink-0"
      >
        <Label check>
          <Input
            id={`accommodation-no-limit-${student.id}-${content.id}`}
            className=" no-time-limit"
            type="checkbox"
            disabled={accommodation == null}
            checked={accommodation === 0}
            onChange={() =>
              setAccommodation(content.id, accommodation === 0 ? content.maxMinutes! : 0)
            }
          />
          {translate('ACCOMMODATION_NO_TIME_LIMIT')}
        </Label>
      </FormGroup>
      <Label className="d-flex align-items-center mb-0 gap-1">
        <Input
          key={accommodation ? 'none' : 'some'}
          className="input-time-limit"
          type="number"
          style={{ width: '5rem' }}
          defaultValue={accommodation || content.maxMinutes!}
          disabled={!accommodation}
          min={1}
          onChange={e => {
            if (e.target.valueAsNumber) setAccommodation(content.id, e.target.valueAsNumber);
          }}
        />
        {translate('ACCOMMODATION_MINUTE_LIMIT')}
      </Label>
      <Button
        className="ms-auto save-accommodation"
        disabled={accommodation === original}
        onClick={() => {
          putAccommodation(
            student.id,
            content.id,
            accommodation === content.maxMinutes! ? null : (accommodation ?? null)
          ).then(() => setOriginal(accommodation));
        }}
      >
        {translate('SAVE')}
      </Button>
    </div>
  );
};
