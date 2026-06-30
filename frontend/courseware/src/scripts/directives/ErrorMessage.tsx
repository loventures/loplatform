/*
 * LO Platform copyright (C) 2007–2026 LO Ventures LLC.
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

import { errorMessage } from '../filters/pure/errorMessage.ts';
import { useTranslation } from '../i18n/translationContext.tsx';

interface ErrorMessageProps {
  error: any;
}

const ErrorMessage = ({ error }: ErrorMessageProps) => {
  const translate = useTranslation();
  return (
    <div className="alert alert-danger">
      <span>Error: </span>
      <span>{errorMessage(error, translate)}</span>
    </div>
  );
};

export default ErrorMessage;
