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

import React, { useEffect, useRef } from 'react';

import { HtmlWithMathJax } from '../../components/HtmlWithMathjax';
import { useTranslation } from '../../i18n/translationContext.tsx';
import settings from '../../utilities/settingsService';

const MinimalTheme = [
  { name: 'basicstyle', items: ['Bold', 'Italic'] },
  { name: 'links', items: ['Link', 'Unlink'] },
];

export interface RichTextEditorProps {
  content?: string;
  onChange?: (html: string) => void;
  isDisabled?: boolean;
  isMinimal?: boolean | string;
  placeholder?: string;
  label?: string;
  minHeight?: number;
  focusOnRender?: boolean;
  fixedHeight?: number;
  toolbar?: any;
  // Consumers also pass extras the old (untyped) angular2react export accepted and
  // ignored — `className`, `disabled` (≠ isDisabled), `required`, … — keep doing so.
  [key: string]: any;
}

// Last line of defense against pasted `compile=`, plus undoing server de-xss escaping
// and trailing newlines — kept verbatim from the AngularJS controller.
const clean = (html: string): string =>
  (html || '')
    .replace(/compile="/g, 'c0mp1le="')
    .replace(/&quot;/g, '"')
    .replace(/&#39;/g, "'")
    .replace(/[\n]+$/g, '');

/**
 * React port of the `richTextEditor` directive (a CKEditor 4 wrapper). The editor
 * is created imperatively in an effect (`CKEDITOR.replace` on an uncontrolled
 * textarea) so React never reconciles CKEditor's generated DOM; content/disabled
 * sync and the change events are bridged through refs to avoid stale closures.
 * Previously an Angular component exposed to React via angular2react — now a native
 * React component, bridged *back* to Angular via react2angular for the remaining
 * Angular consumers (essay, discussion, messaging, …). CKEditor's config is
 * unchanged, so its iframe/toolbar DOM (and the Selenide selectors) are identical.
 */
export const RichTextEditor: React.FC<RichTextEditorProps> = ({
  content,
  onChange,
  isDisabled,
  isMinimal,
  placeholder,
  label,
  minHeight,
  focusOnRender,
  fixedHeight,
  toolbar,
}) => {
  const translate = useTranslation();
  const textareaRef = useRef<HTMLTextAreaElement>(null);
  const editorRef = useRef<any>(null);

  // Latest props for the imperative CKEditor callbacks (mount-once effect).
  const contentRef = useRef(content);
  const onChangeRef = useRef(onChange);
  const isDisabledRef = useRef(isDisabled);
  contentRef.current = content;
  onChangeRef.current = onChange;
  isDisabledRef.current = isDisabled;

  const updateToEditor = (editor: any) => {
    const fromCtrl = clean(contentRef.current ?? '');
    const fromEditor = clean(editor.getData());
    if (fromCtrl !== fromEditor) editor.setData(fromCtrl);
  };

  // Create / destroy the CKEditor instance once.
  useEffect(() => {
    const CKEDITOR = (window as any).CKEDITOR;
    if (!CKEDITOR || !textareaRef.current) return;

    const ckeditorDisallowedContent = settings.getSettings('CkeditorDisallowedContent');

    const editor = CKEDITOR.replace(textareaRef.current, {
      mathJaxLib: (window as any).lo_platform.cdn_url + 'assets/mathjax/tex-mms-chtml.js',
      removePlugins: 'oembed,contextmenu,liststyle,tabletools,tableresize',
      height: minHeight || 200,
      autoGrow_minHeight: fixedHeight ? fixedHeight : minHeight || 200,
      autoGrow_maxHeight: fixedHeight ? fixedHeight : undefined,
      autoGrow_onStartup: true,
      disallowedContent: ckeditorDisallowedContent,
      disableNativeSpellChecker: false,
      toolbar: toolbar ?? (isMinimal ? MinimalTheme : null),
      startupFocus: focusOnRender,
      className: 'd-print-none',
    });
    editorRef.current = editor;

    const updateFromEditor = () => {
      const fromCtrl = clean(contentRef.current ?? '');
      const fromEditor = clean(editor.getData());
      if (fromCtrl !== fromEditor) onChangeRef.current?.(fromEditor);
    };

    if (isDisabledRef.current) {
      editor.on('instanceReady', () => {
        if (contentRef.current) updateToEditor(editor);
        editor.setReadOnly(true);
      });
    }

    editor.on('change', updateFromEditor);
    editor.on('contentDom', () => {
      const editable = editor.editable();
      editable.attachListener(editable, 'input', updateFromEditor);
    });
    editor.on('contentDomUnload', () => editor.editable()?.removeAllListeners());

    const resizer = new ResizeObserver(([entry]) => {
      // 2px for border
      editor.container?.setStyle('width', entry.contentRect.width - 2 + 'px');
    });
    const observed = textareaRef.current.parentElement?.parentElement;
    if (observed) resizer.observe(observed);

    return () => {
      resizer.disconnect();
      editor.destroy();
      editorRef.current = null;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // Sync the content prop into the editor when it changes *externally*. While the
  // user is typing, the change round-trips through onChange and lands back here a
  // tick later; pushing that (often stale) value back with setData would clobber
  // the live editor, so skip the sync while the editor is focused / not yet ready.
  useEffect(() => {
    const editor = editorRef.current;
    if (!editor || editor.status !== 'ready') return;
    if (editor.focusManager?.hasFocus) return;
    updateToEditor(editor);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [content]);

  // Sync the disabled prop.
  useEffect(() => {
    const editor = editorRef.current;
    if (editor && editor.status === 'ready') editor.setReadOnly(!!isDisabled);
  }, [isDisabled]);

  return (
    <div className="lo-rich-text-editor">
      <textarea
        ref={textareaRef}
        aria-label={label ? translate(label) : undefined}
        placeholder={placeholder ?? translate('RICH_TEXT_EDITOR_PLACEHOLDER')}
        defaultValue={content}
        disabled={isDisabled}
      />
      <div className="print-only">
        <HtmlWithMathJax html={content} />
      </div>
    </div>
  );
};

