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

import { each } from 'lodash';

const ContentPartTemplateUtils = {
  createHtml: function (tagName, attributes, innerHtml) {
    var el = document.createElement(tagName);
    each(attributes, function (value, name) {
      el.setAttribute(name, value);
    });
    el.innerHTML = innerHtml;
    return el.outerHTML;
  },

  imageWithCaption: function (asset) {
    var img = this.createHtml('img', {
      src: `/api/v2/authoring/nodes/${asset.id}/serve`,
      title: asset.data.title,
      alt: asset.data.altText,
    });
    var caption = this.createHtml('figcaption', { class: 'figure-caption' }, asset.data.caption);

    var innerHtml = img + caption;

    return this.createHtml('figure', {}, innerHtml);
  },

  file: function (asset) {
    return this.createHtml(
      'a',
      {
        href: `/api/v2/authoring/nodes/${asset.id}/serve`,
        title: asset.data.title,
        target: '_blank',
      },
      asset.data.title
    );
  },

  image: function (asset) {
    if (asset.data.caption) {
      return this.imageWithCaption(asset);
    }

    return this.createHtml('img', {
      src: `/api/v2/authoring/nodes/${asset.id}/serve`,
      title: asset.data.title,
      alt: asset.data.altText,
    });
  },
};

export default ContentPartTemplateUtils;
