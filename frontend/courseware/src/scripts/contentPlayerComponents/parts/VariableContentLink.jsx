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

import LTINewWindowLauncher from '../../lti/components/LTINewWindowLauncher.js';
import { getLTIConfigUrl } from '../../lti/configUrl.js';
import ExternalLink from './ExternalLink.js';
import ContentLink from './ContentLink.js';
import { connect } from 'react-redux';
import { reportProgressActionCreator } from '../../courseActivityModule/actions/activityActions.js';

import { CONTENT_TYPE_LTI, CONTENT_TYPE_RESOURCE } from '../../utilities/contentTypes.js';

import { LTI_TYPE_NEW_WINDOW } from '../../utilities/contentSubTypes.js';

import { EXTERNAL_LINK } from '../../utilities/resource1Types.js';

const VariableContentLink = ({ content, children, setProgress, ...props }) => {
  if (content.typeId === CONTENT_TYPE_LTI && content.subType === LTI_TYPE_NEW_WINDOW) {
    return (
      <LTINewWindowLauncher
        launchPath={getLTIConfigUrl(content)}
        onClick={() => setProgress(content, 1)}
        children={children}
        {...props}
      />
    );
  } else if (content.typeId === CONTENT_TYPE_RESOURCE && content.subType === EXTERNAL_LINK) {
    return (
      <ExternalLink
        content={content}
        children={children}
        {...props}
      />
    );
  } else {
    return (
      <ContentLink
        content={content}
        children={children}
        {...props}
      />
    );
  }
};

export default connect(null, {
  setProgress: reportProgressActionCreator,
})(VariableContentLink);
