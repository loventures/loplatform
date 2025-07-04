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

import { ISCEService } from 'angular';
import { angular2react } from 'angular2react';
import { Option } from '../types/option.ts';
import React from 'react';

interface EmbeddedContentProps {
  url: string;
  title?: string;
  rawHtml?: string;
  expandable?: boolean;
  contentWidth?: Option<number>;
  contentHeight?: Option<number>;
  onLoaded?: () => void;
  printView?: boolean;
}

function embeddedController($sce: ISCEService) {
  // eslint-disable-next-line @typescript-eslint/ban-ts-comment
  // @ts-ignore
  this.$onChanges = ({ url }: { url: any }) => {
    if (url) {
      // eslint-disable-next-line @typescript-eslint/ban-ts-comment
      // @ts-ignore
      this.trustedUrl = $sce.trustAsResourceUrl(url.currentValue);
    }
  };
}

export const component = {
  template: `
        <embedded-content
            url="$ctrl.trustedUrl"
            title="$ctrl.title"
            raw-html="$ctrl.rawHtml"
            expandable="$ctrl.expandable"
            content-width="$ctrl.contentWidth"
            content-height="$ctrl.contentHeight"
            on-loaded="$ctrl.onLoaded"
            print-view="$ctrl.printView"
        ></embedded-content>
    `,
  bindings: {
    url: '<',
    title: '<?',
    rawHtml: '<',
    expandable: '<',
    contentWidth: '<',
    contentHeight: '<',
    onLoaded: '<',
    printView: '<?',
  },

  controller: ['$sce', embeddedController],
};

export let EmbeddedContent: React.ComponentClass<EmbeddedContentProps>;

export const bindComponent = ($injector: any) => {
  EmbeddedContent = angular2react<EmbeddedContentProps>(
    'embeddedContentForReact',
    component,
    $injector
  );
};
