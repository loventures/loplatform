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

import { Translate, withTranslation } from '../../i18n/translationContext';
import React from 'react';
import { Card, CardBody, CardHeader, CardTitle } from 'reactstrap';

import LoadedExemptLearnerForm from './ExemptLearnerForm';

const DueDateAccomodations = ({ translate }: { translate: Translate }) => (
  <Card className="my-2">
    <CardHeader>{translate('DUE_DATE_ACCOMMODATIONS')}</CardHeader>
    <CardBody>
      <CardTitle>{translate('DUE_DATE_EXEMPT_LEARNERS')}</CardTitle>
      <LoadedExemptLearnerForm />
    </CardBody>
  </Card>
);

export default withTranslation(DueDateAccomodations);
