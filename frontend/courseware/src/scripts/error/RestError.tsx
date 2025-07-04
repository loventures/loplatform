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

import { withTranslation } from '../i18n/translationContext.tsx';
import React from 'react';

import { ErrorResponseBody } from './restErrorsTypes.ts';

type RestErrorProps = {
  error: ErrorResponseBody;
};

function isError(e: any): e is Error {
  return e instanceof Error;
}

function formatMessage(error: any) {
  if (import.meta.env.DEV) {
    if (isError(error)) {
      return (
        <div style={{ marginTop: '1rem' }}>
          <pre
            style={{
              backgroundColor: '#fff',
              border: '1 px solid #ddd',
              padding: '1rem',
              borderRadius: '2px',
              color: '#666',
            }}
          >
            {error.stack}
          </pre>
        </div>
      );
    } else {
      return <pre>{JSON.stringify(error, null, 2)}</pre>;
    }
  } else {
    return null;
  }
}

class RestErrorInner extends React.Component<
  RestErrorProps & { translate: (s: string, ...args: any[]) => string }
> {
  render() {
    return (
      <div className="alert alert-danger mt-5">
        <div>{this.props.translate('ERROR_PAGE_TITLE')}</div>
        <div>{this.props.translate('ERROR_PAGE_TITLE_CONTACT')}</div>
        {formatMessage(this.props.error)}
        {this.props.error.guid && (
          <div>{this.props.translate('ERROR_PAGE_GUID', this.props.error.guid)}</div>
        )}
      </div>
    );
  }
}

export const RestError: React.ComponentType<RestErrorProps> = withTranslation(RestErrorInner);
