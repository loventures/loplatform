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

import { angular2react } from 'angular2react';
import Settings from '../../utilities/settings.jsx';

import template from './richTextEditor.html';

const MinimalTheme = [
  {
    name: 'basicstyle',
    items: ['Bold', 'Italic'],
  },
  {
    name: 'links',
    items: ['Link', 'Unlink'],
  },
];

const component = {
  template,

  bindings: {
    content: '<',
    onChange: '<',
    isDisabled: '<',
    isMinimal: '<',
    placeholder: '<?',
    label: '<?',
    minHeight: '<?',
    focusOnRender: '<?',
    fixedHeight: '<?',
    toolbar: '<?',
  },

  controller: [
    '$element',
    '$timeout',
    'Settings',
    function ($element, $timeout, Settings) {
      const ckeditorDisallowedContent = Settings.getSettings('CkeditorDisallowedContent');

      const resizer = new ResizeObserver(([entry]) => {
        // 2px for border
        this.editorInstance?.container?.setStyle('width', entry.contentRect.width - 2 + 'px');
      });

      this.$onDestroy = () => {
        resizer.disconnect();
      };

      this.$onInit = () => {
        if (!window.CKEDITOR) {
          return;
        }

        const editorContainer = $element.find('textarea')[0];

        this.editorInstance = window.CKEDITOR.replace(editorContainer, {
          mathJaxLib: window.lo_platform.cdn_url + 'assets/mathjax/tex-mms-chtml.js',
          removePlugins: 'oembed,contextmenu,liststyle,tabletools,tableresize',
          height: this.minHeight || 200,
          autoGrow_minHeight: this.fixedHeight ? this.fixedHeight : this.minHeight || 200,
          autoGrow_maxHeight: this.fixedHeight ? this.fixedHeight : undefined,
          autoGrow_onStartup: true,
          disallowedContent: ckeditorDisallowedContent,
          disableNativeSpellChecker: false,
          toolbar: this.toolbar ?? (this.isMinimal ? MinimalTheme : null),
          startupFocus: this.focusOnRender,
          className: 'd-print-none',
        });

        if (this.isDisabled) {
          this.editorInstance.on('instanceReady', () => {
            if (this.content) {
              this.updateToEditorContent();
            }
            this.editorInstance.setReadOnly(this.isDisabled);
          });
        }
        resizer.observe($element.parent()[0]);

        this.editorInstance.on('change', () => {
          $timeout(() => this.updateFromEditorContent());
        });

        this.editorInstance.on('contentDom', () => {
          const editable = this.editorInstance.editable();

          editable.attachListener(editable, 'input', () => {
            $timeout(() => this.updateFromEditorContent());
          });
        });

        this.editorInstance.on('contentDomUnload', () => {
          const editable = this.editorInstance.editable();
          editable.removeAllListeners();
        });
      };

      this.$onChanges = ({ isDisabled, content }) => {
        if (isDisabled && !isDisabled.isFirstChange()) {
          this.editorInstance.setReadOnly(!!isDisabled.currentValue);
        }

        if (content && !content.isFirstChange()) {
          this.updateToEditorContent();
        }
      };

      this.updateToEditorContent = () => {
        const fromCtrl = this.clean(this.content);
        const fromEditor = this.clean(this.editorInstance.getData());
        if (fromCtrl !== fromEditor) {
          this.editorInstance.setData(fromCtrl);
        }
      };

      this.updateFromEditorContent = () => {
        const fromCtrl = this.clean(this.content);
        const fromEditor = this.clean(this.editorInstance.getData());
        if (fromCtrl !== fromEditor) {
          this.onChange(fromEditor);
        }
      };

      this.clean = html => {
        return (
          html
            //last line of defense against copy paste
            .replace(/compile="/g, 'c0mp1le="')
            //battling server changing stuff for de-xss
            .replace(/&quot;/g, '"')
            .replace(/&#39;/g, "'")
            .replace(/[\n]+$/g, '')
        );
      };
    },
  ],
};

export let RichTextEditor = props => <div {...props}>'RichTextEditor: ng module not included'</div>;
export default angular
  .module('lo.contentEditor.richTextEditor', [Settings.name])
  .component('richTextEditor', component)
  .run([
    '$injector',
    function ($injector) {
      RichTextEditor = angular2react('richTextEditor', component, $injector);
    },
  ]);
