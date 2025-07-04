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

import LoadingSpinner from './loadingSpinner/index.js';
import ErrorMessage from './ErrorMessage.jsx';
import SuccessMessage from './SuccessMessage.jsx';

const LoadingMessages = ({ loadingState, loading, success }) => {
  if (loading && loadingState.loading) {
    return (
      <div className="container">
        <div className="alert alert-info my-4 text-center">
          <LoadingSpinner message={loading} />
        </div>
      </div>
    );
  }

  if (loadingState.error) {
    return <ErrorMessage error={this.props.loadingState.error} />;
  }

  if (success && loadingState.loaded) {
    return <SuccessMessage message={success} />;
  }

  return <div></div>;
};

export default LoadingMessages;
