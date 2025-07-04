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

import axios, { AxiosError } from 'axios';
import cookies from 'browser-cookies';
import Course from '../bootstrap/course';
import { Tree, mapWithChildren } from '../instructorPages/customization/Tree';
import { Either, getValidation, left, right } from 'fp-ts/es6/Either';
import { NonEmptyArray, getSemigroup, of } from 'fp-ts/es6/NonEmptyArray';
import { pipe } from 'fp-ts/es6/pipeable';
import { mapWithIndex, sequence } from 'fp-ts/es6/Record';
import { Translate } from '../i18n/translationContext';
import { AssetTypeId } from '../utilities/assetTypes';
import { Omit } from '../types/omit';
import { Option, isPresent, fold as opFold } from '../types/option';

/**
 * ContentOverlayUpdate represents a set of changes
 * for one piece of content.
 *
 * All fields are optional, and possibly null.
 * If a key is undefined, that means "no change."
 * If a key is present but null, that means "delete what's there."
 * If a key is present and the value is not null, that means "update."
 */
export type ContentOverlayUpdate = {
  order?: string[] | null; // list of ids
  hide?: string[] | null; // list of ids

  title?: string | null;
  instructions?: string | null;
  duration?: number | null;
  isForCredit?: boolean | null;
  pointsPossible?: number | null;
  gateDate?: string | null;
  dueDate?: Date | null;
  dueDay?: Day | null;
  dueTime?: Time | null;
  metadata?: any | null; // wtf?!?
};

/**
 * A map where the keys are edgepaths to content,
 * and the values are updates to be applied to those keys
 */
export type Customizations = {
  [id: string]: ContentOverlayUpdate;
};

/**
 * Sends an XHR POST to update the customizations for the specified course.
 * @param courseId
 * @param customizations
 */
export function updateCustomizations(
  courseId: number,
  customizations: Customizations
): Promise<Customizations> {
  return axios
    .post(`/api/v2/lwc/${courseId}/customise/updates`, customizations, {
      headers: { 'X-CSRF': cookies.get('CSRF')! },
    })
    .then(({ data }) => data);
}

type ContentOverlayUpdateReturn = Omit<ContentOverlayUpdate, 'dueDate'> & {
  dueDate?: string | null;
};
export const customizeOneContent = (
  contentId: string,
  customization: ContentOverlayUpdate
): Promise<ContentOverlayUpdate> => {
  const contextId = Course.id;
  return axios
    .post<ContentOverlayUpdateReturn>(
      `/api/v2/lwc/${contextId}/customise/${contentId}`,
      customization
    )
    .catch((e: AxiosError) => {
      throw e.message;
    })
    .then(({ data }) => {
      return { ...data, dueDate: data.dueDate ? new Date(data.dueDate) : null };
    });
};

/**
 * The type of value we get from the backend
 */
type CustomisableContentReturn = {
  id: string;
  resourceType: string | null;
  gradable: boolean;
  typeId: AssetTypeId;
  title: string;
  titleCustomised: boolean;
  instructions: string | null;
  instructionsCustomised: boolean;
  gateDate: string | null;
  gateDateCustomised: boolean;
  dueDate: string | null;
  dueDateCustomised: boolean;
  pointsPossible: number | null;
  pointsPossibleCustomised: boolean;
  isForCredit: boolean;
  isForCreditCustomised: boolean;
  orderCustomised: boolean;
  hide: string[] | null;
  metadata: any;
};

/**
 * The type of value we map the server return value into
 */
export type CustomisableContent = Omit<CustomisableContentReturn, 'dueDate'> & {
  dueDay: Day | null;
  dueTime: Time | null;
  dueDate: Date | null;
  order: string[];
};

/**
 * Extracts Day & Time information from a Date
 * @param date
 */
export const deriveDayAndTime = (date: Date): { time: Time; day: Day } => {
  return {
    day: {
      year: date.getFullYear(),
      month: date.getMonth() + 1,
      date: date.getDate(),
    },
    time: {
      hours: date.getHours(),
      minutes: date.getMinutes(),
    },
  };
};

/**
 * Fetches the current customizations that are applied, mapping any due dates
 * to additionally include a day/time
 * @param courseId
 */
export function fetchCustomizations(courseId: number): Promise<Tree<CustomisableContent>> {
  type Resp = { data: Tree<CustomisableContentReturn> };
  return axios.get(`/api/v2/lwc/${courseId}/contents/customisable`).then(({ data }: Resp) => {
    // add parsed dueDay & dueTime
    return mapWithChildren(data, (content, children) => {
      const order = children.map(c => c.id);
      if (isPresent(content.dueDate)) {
        const dueDate = new Date(content.dueDate);
        const { time, day } = deriveDayAndTime(dueDate);
        return {
          ...content,
          dueDay: day,
          dueTime: time,
          dueDate,
          order,
        };
      } else {
        return {
          ...content,
          dueDay: null,
          dueTime: null,
          dueDate: null,
          order,
        };
      }
    });
  });
}

/**
 * Sends a POST request to undo all customizations for a course.
 * @param courseId
 */
export function resetCourseCustomizations(courseId: number): Promise<void> {
  return axios
    .post(`/api/v2/lwc/${courseId}/customise/reset`, null, {
      headers: { 'X-CSRF': cookies.get('CSRF')! },
    })
    .then(({ data }) => data);
}

/**
 * Applies a Day object to a Date, keeping the time in tact.
 * If no date is supplied, a new Date is created, using 12:00 AM as the time.
 * @param newDay
 */
export function applyDay(newDay: Day): (date: Option<Date>) => Date {
  return old => {
    const newDate = opFold(
      old,
      () => {
        // get date w/ time 12:00 AM
        const d = new Date();
        d.setHours(0, 0, 0, 0);
        return d;
      },
      o => new Date(o)
    );
    const { year, month, date } = newDay;
    newDate.setFullYear(year, month - 1, date);
    return newDate;
  };
}

/**
 * Applies a Time object to a Date, keeping the Day in tact
 * if no date is supplied, a new Date is created, using the current day
 * @param newTime
 */
export function applyTime(newTime: Time): (date: Option<Date>) => Date {
  return old => {
    const date = opFold(
      old,
      () => new Date(),
      o => new Date(o)
    );
    date.setHours(newTime.hours, newTime.minutes);
    return date;
  };
}

/**
 * A specific calendar Day.
 */
export type Day = {
  year: number;
  month: number;
  date: number;
};

/**
 * A time of day
 */
export type Time = {
  hours: number;
  minutes: number;
};

/**
 * A validation problem, with the id of the problem item and
 * a function to generate a message given the content item.
 */
export type Problem = {
  id: string;
  message: (c: CustomisableContent) => string;
};

/**
 * An Applicative instance for NonEmptyArray<string>
 * This will allow us to combine two validations whose left sides are NonEmptyArray<string>
 */
const validationApplicative = getValidation(getSemigroup<Problem>());

/**
 * Validate the supplied ContentOverlayUpdate, returning any problems if encountered
 * @param c
 */
function validateOverlay(
  translate: Translate
): (id: string, c: ContentOverlayUpdate) => Either<NonEmptyArray<Problem>, ContentOverlayUpdate> {
  return (id, c) => {
    if (isPresent(c.title) && c.title === '') {
      return left(
        of({
          id,
          message: (c: CustomisableContent) =>
            translate('CUSTOMIZATIONS_TITLE_NONBLANK', { name: c.title }),
        })
      );
    } else {
      return right(c);
    }
  };
}

/**
 * Validates all customizations, returning either a non-empty list of Problems, or the validated Customizations
 * @param translate
 * @param c
 */
export function validateCustomizations(
  translate: Translate,
  customizations: Customizations
): Either<NonEmptyArray<Problem>, Customizations> {
  const validatedCustomizations = pipe(customizations, mapWithIndex(validateOverlay(translate)));

  // sequence will turn that into: Map<string, Either<Problem[], ContentOverlayUpdate>>
  return sequence(validationApplicative)(validatedCustomizations);
}
