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

import { CustomisableContent, Day, Time } from '../../../api/customizationApi';
import { debounce, isEmpty, thru } from 'lodash';
import { Translate, WithTranslate } from '../../../i18n/translationContext';
import { Option, map, sequence } from '../../../types/option';
import React from 'react';
import { Input, InputGroup, InputGroupText } from 'reactstrap';
import { Dispatch } from 'redux';

import { ContentEdit, changeDueDay, changeDueTime, removeDueDate } from '../contentEdits';
import { addEdit, updateContentBeingEdited } from '../courseCustomizerReducer';
import { MdCalendarToday, MdClose, MdSchedule } from 'react-icons/md';

type DueDateProps = {
  dispatch: Dispatch;
  content: CustomisableContent;
};

type DueDateState = {
  time?: Option<string>;
  day?: Option<string>;
};

const pad = (n: number) => n.toString().padStart(2, '0');
const formatDate = (d: Day) => `${d.year}-${pad(d.month)}-${pad(d.date)}`;
const formatTime = (t: Time) => `${pad(t.hours)}:${pad(t.minutes)}`;

const numberIsValid = (numberStr: string): Option<number> =>
  thru(Number.parseInt(numberStr, 10), n => (Number.isNaN(n) ? null : n));

function dayIsValid(dateStr: string): Option<Day> {
  const parsed = dateStr.split('-');
  return parsed.length === 3
    ? map(
        sequence(numberIsValid(parsed[0]), numberIsValid(parsed[1]), numberIsValid(parsed[2])),
        ([year, month, date]) => ({ year, month, date })
      )
    : null;
}

function timeIsValid(dateStr: string): Option<Time> {
  const parsed = dateStr.split(':');
  return parsed.length === 2
    ? map(sequence(numberIsValid(parsed[0]), numberIsValid(parsed[1])), ([hours, minutes]) => ({
        hours,
        minutes,
      }))
    : null;
}

export class DueDateEditor extends React.Component<DueDateProps, DueDateState> {
  inputRef: React.RefObject<HTMLInputElement>;
  buttonRef: React.RefObject<HTMLButtonElement>;
  constructor(props: DueDateProps) {
    super(props);
    this.inputRef = React.createRef();
    this.buttonRef = React.createRef();
    this.dispatchEdit = debounce(this.dispatchEdit.bind(this), 500);
    this.updateDueDateDay = this.updateDueDateDay.bind(this);
    this.updateDueDateTime = this.updateDueDateTime.bind(this);
    this.updateEditorState = this.updateEditorState.bind(this);

    this.state = {
      day: map(props.content.dueDay, formatDate),
      time: map(props.content.dueTime, formatTime),
    };
  }
  dispatchEdit(edit: ContentEdit) {
    this.props.dispatch(addEdit(edit));
  }
  updateDueDateDay(day?: string) {
    this.setState({
      day,
    });
    if (day) {
      map(dayIsValid(day), d => {
        this.dispatchEdit(changeDueDay({ id: this.props.content.id, ...d }));
      });
    } else {
      this.props.dispatch(addEdit(removeDueDate({ id: this.props.content.id })));
    }
  }
  updateDueDateTime(time: string) {
    this.setState({
      time,
    });
    map(timeIsValid(time), t => {
      this.dispatchEdit(changeDueTime({ id: this.props.content.id, ...t }));
    });
  }
  updateEditorState(isEditing: boolean) {
    this.props.dispatch(updateContentBeingEdited(isEditing ? this.props.content.id : void 0));
  }
  componentDidUpdate(prevProps: DueDateProps) {
    const statePropsToUpdate: any = {};
    if (prevProps.content.dueDay != this.props.content.dueDay) {
      statePropsToUpdate.day = this.props.content.dueDay
        ? formatDate(this.props.content.dueDay)
        : undefined;
    }
    if (prevProps.content.dueTime != this.props.content.dueTime) {
      statePropsToUpdate.time = this.props.content.dueTime
        ? formatTime(this.props.content.dueTime)
        : undefined;
    }
    if (!isEmpty(statePropsToUpdate)) {
      this.setState(statePropsToUpdate);
    }
  }
  render() {
    const { content } = this.props;
    return (
      <WithTranslate>
        {translate => (
          <>
            <span className={'due-date-editor right-aligned open'}>
              <DueDate
                resetDueDate={() => this.updateDueDateDay()}
                time={this.state.time}
                day={this.state.day}
                inputRef={this.inputRef}
                content={content}
                translate={translate}
                updateTime={this.updateDueDateTime}
                updateDay={this.updateDueDateDay}
                updateEditorState={this.updateEditorState}
              />
            </span>
          </>
        )}
      </WithTranslate>
    );
  }
}

const DueDate: React.FC<{
  content: CustomisableContent;
  translate: Translate;
  inputRef: React.RefObject<HTMLInputElement>;
  time: Option<string>;
  day: Option<string>;
  updateTime: (time: string) => void;
  updateDay: (time: string) => void;
  resetDueDate: () => void;
  updateEditorState: (alwaysActive: boolean) => void;
}> = React.memo(
  ({
    content,
    translate,
    inputRef,
    resetDueDate,
    updateDay,
    updateTime,
    updateEditorState,
    time,
    day,
  }) => (
    <>
      {translate('SET_DUE_DATE', { name: content.title })}
      <div className="due-date-inputs">
        <InputGroup className="day">
          <Input
            className="day-editor"
            innerRef={inputRef}
            type="date"
            value={day || ''}
            onChange={e => {
              updateDay(e.target.value);
            }}
            onFocus={() => {
              updateEditorState(true);
            }}
            onBlur={() => {
              updateEditorState(false);
            }}
          />
          <InputGroupText>
            <MdCalendarToday />
          </InputGroupText>
        </InputGroup>
        <InputGroup className="time">
          <Input
            className="time-editor"
            type="time"
            value={time || ''}
            onChange={e => updateTime(e.target.value)}
          />
          <InputGroupText>
            <MdSchedule />
          </InputGroupText>
        </InputGroup>
        {time && day && (
          <button
            className="ms-1 delete icon-btn"
            onClick={resetDueDate}
            title={translate('REMOVE_DUE_DATE', { name: content.title })}
          >
            <MdClose />
            <span className="sr-only">{translate('REMOVE_DUE_DATE', { name: content.title })}</span>
          </button>
        )}
      </div>
    </>
  )
);
