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
import { customizeOneContent } from '../../../api/customizationApi';
import { GradebookColumn } from '../../../api/gradebookApi';
import CollapseCard from '../../../components/CollapseCard';
import DateEditor from '../../../components/dateTime/DateEditor';
import {
  fromInputToDate,
  fromISOToInput,
  isValidDateFromInput,
  isValidTimeFromInput,
} from '../../../components/dateTime/dateTimeInputUtils';
import DateTimeTrashButton from '../../../components/dateTime/DateTimeTrashButton';
import TimeEditor from '../../../components/dateTime/TimeEditor';
import ErrorMessage from '../../../directives/ErrorMessage';
import { TranslationContext } from '../../../i18n/translationContext';
import { CONTENT_TYPE_MODULE, CONTENT_TYPE_UNIT } from '../../../utilities/contentTypes';
import { CourseState } from '../../../loRedux';
import { filter, groupBy, isEmpty, isEqual, map, orderBy, sum } from 'lodash';
import { ContentThinnedWithLearningIndex } from '../../../utilities/contentResponse';
import React, { useContext, useMemo, useRef, useState } from 'react';
import { connect } from 'react-redux';
import { Button, Form, Label } from 'reactstrap';

const GradebookAssignmentsColumnForm: React.FC<{
  contentId: ContentId;
  dueDate: string | null;
}> = ({ contentId, dueDate }) => {
  const translate = useContext(TranslationContext);
  //yeah ideally this should propagate to the reducer but too much work for this
  const [dueDateState, setDueDate] = useState(dueDate);
  const inputValues = dueDateState ? fromISOToInput(dueDateState) : { date: null, time: null };

  const initialValues = useRef(inputValues);
  const [values, setValues] = useState(initialValues.current);
  const dirty = !isEqual(values, initialValues.current);
  const [saved, setSaved] = useState(false);
  const [error, setError] = useState(null);
  const [isSubmitting, setSubmitting] = useState(false);

  const errors = useMemo(
    () => [
      ...(values.date && !values.time ? [translate('DATE_PICKER_MISSING_TIME')] : []),
      ...(values.time && !values.date ? [translate('DATE_PICKER_MISSING_DATE')] : []),
      ...(values.date && !isValidDateFromInput(values.date)
        ? [translate('DATE_PICKER_INVALID_DATE')]
        : []),
      ...(values.time && !isValidTimeFromInput(values.time)
        ? [translate('TIME_PICKER_INVALID_TIME')]
        : []),
    ],
    [values]
  );

  return (
    <Form
      onSubmit={e => {
        e.preventDefault();
        setSubmitting(true);
        const payload = {
          dueDate: fromInputToDate(values.date, values.time),
        };
        customizeOneContent(contentId, payload).then(
          ({ dueDate }) => {
            initialValues.current = values;
            setDueDate(dueDate ? dueDate.toISOString() : null);
            setSaved(true);
            setError(null);
            setSubmitting(false);
          },
          errors => {
            setError(errors);
            setSubmitting(false);
          }
        );
      }}
    >
      <div className="flex-row-content align-items-end">
        <div>
          <Label htmlFor={`gb-assignment-date-${contentId}` as string}>
            {translate('GRADEBOOK_COLUMN_DUEDATE_DATE')}
          </Label>
          <DateEditor
            id={`gb-assignment-date-${contentId}`}
            value={values.date}
            onChange={e => setValues({ ...values, date: e.target.value })}
            disabled={isSubmitting}
          />
        </div>
        <div>
          <Label htmlFor={`gb-assignment-time-${contentId}` as string}>
            {translate('GRADEBOOK_COLUMN_DUEDATE_TIME')}
          </Label>
          <TimeEditor
            id={`gb-assignment-time-${contentId}`}
            value={values.time}
            onChange={e => setValues({ ...values, time: e.target.value })}
            disabled={isSubmitting}
          />
        </div>
        {values.date && (
          <DateTimeTrashButton
            className="m-2"
            type="button"
            onClick={() => setValues({ date: null, time: null })}
            disabled={isSubmitting}
          />
        )}
      </div>
      {!isEmpty(errors) && (
        <div
          className="mt-2 text-danger"
          role="alert"
        >
          {map(errors, (error, index) => (
            <p key={index}>{error}</p>
          ))}
        </div>
      )}
      <div className="flex-row-content mt-2">
        <Button
          type="submit"
          disabled={!dirty || isSubmitting || !!errors.length}
        >
          {translate('GRADEBOOK_SAVE')}
        </Button>
        {dirty && <span>{translate('GRADEBOOK_EDIT_DIRTY')}</span>}
        {isSubmitting && <span>{translate('GRADEBOOK_EDIT_SAVING')}</span>}
        {!isSubmitting && !dirty && saved && <span>{translate('GRADEBOOK_EDIT_SAVED')}</span>}
        {!isSubmitting && error && <ErrorMessage error={error.status} />}
      </div>
    </Form>
  );
};

const GradebookAssignmentsColumn: React.FC<{
  content: ContentThinnedWithLearningIndex;
  column: GradebookColumn;
}> = ({ content, column }) => {
  const translate = useContext(TranslationContext);
  return (
    <CollapseCard
      initiallyOpen={false}
      renderHeader={() => (
        <div className="flex-col-fluid flex-row-content">
          <span className="flex-col-fluid text-start">{column.name}</span>
          {column.credit === 'NoCredit' && <span>{translate('GRADEBOOK_NO_CREDIT')}</span>}
          {column.credit === 'Credit' && (
            <span>
              {translate('GRADEBOOK_COLUMN_WEIGHT', {
                weight: column.maximumPoints,
              })}
            </span>
          )}
        </div>
      )}
    >
      <div className="card-body">
        <GradebookAssignmentsColumnForm
          contentId={column.id}
          dueDate={content.dueDate}
        />
      </div>
    </CollapseCard>
  );
};

const GradebookAssignmentsCategory: React.FC<{
  category: GradebookColumn;
  columns: GradebookColumn[];
  contents: { [k: string]: ContentThinnedWithLearningIndex };
}> = ({ category, columns, contents }) => {
  const translate = useContext(TranslationContext);
  const forCreditPoints = sum(
    map(columns, c => {
      return c.credit === 'Credit' ? c.maximumPoints : 0;
    })
  );
  return (
    <CollapseCard
      initiallyOpen={true}
      renderHeader={() => (
        <div className="flex-col-fluid flex-row-content">
          <span className="flex-col-fluid text-start">{category.name}</span>
          {forCreditPoints === 0 && <span>{translate('GRADEBOOK_NO_CREDIT')}</span>}
          {forCreditPoints > 0 && (
            <span>
              {translate('GRADEBOOK_CATEGORY_WEIGHT', {
                totalWeight: forCreditPoints,
              })}
            </span>
          )}
        </div>
      )}
    >
      <div className="card-body">
        {map(columns, column => (
          <GradebookAssignmentsColumn
            key={column.id}
            column={column}
            content={contents[column.id]}
          />
        ))}
      </div>
    </CollapseCard>
  );
};

const isContainer = (typeId: string | undefined) =>
  typeId === CONTENT_TYPE_MODULE || typeId === CONTENT_TYPE_UNIT;

const GradebookAssignments: React.FC<{
  contents: { [k: string]: ContentThinnedWithLearningIndex };
  gradebookColumns: GradebookColumn[];
}> = ({ contents, gradebookColumns }) => {
  const orderedCategories = orderBy(
    filter(gradebookColumns, c => isContainer(contents[c.id]?.typeId)),
    'index'
  );
  const columnsByCategory = groupBy(
    filter(gradebookColumns, c => c?.id !== c.category_id),
    'category_id'
  );
  return (
    <div className="gradebook-editor">
      {map(orderedCategories, category => (
        <GradebookAssignmentsCategory
          key={category.id}
          category={category}
          columns={columnsByCategory[category.id]}
          contents={contents}
        />
      ))}
    </div>
  );
};

export default connect((state: CourseState) => {
  return {
    contents: state.api.contentItems,
    gradebookColumns: state.api.gradebookColumns as unknown as GradebookColumn[],
  };
})(GradebookAssignments);
