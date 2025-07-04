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

// Summernote and our plugins
import 'summernote/dist/summernote-lite';

import './summernote-cleaner.js';
import './summernote-image-title.js';
import './summernote-text-findnreplace.js';
import './customTagsPlugin';
import './customUploadPlugin';
import './customLinkPlugin';

import $ from 'jquery';

import classNames from 'classnames';
import React, { Dispatch, MutableRefObject, useCallback, useEffect, useRef, useState } from 'react';
import { useDispatch } from 'react-redux';

export interface RichTextEditorProps {
  id: string;
  value: string;
  mode: 'htmlmixed' | 'javascript' | 'css';
  onChange: (s: string) => void;
  onBlur?: () => void; // Typically handled by click to edit.
  onFocus?: () => void;
  size?: 'fullPage' | 'inline';
  placeholder?: string;
  lineWrapping?: boolean; // from config.
  initializeWithCodeView?: boolean;
  readOnly?: boolean; // if the asset is archived.
  toolbar?: 'simple' | 'none';
  noCustomTags?: boolean;
  noCustomFiles?: boolean;
  noRareStyles?: boolean;
  codeLast?: boolean;
  findReplacePlugin?: boolean;
  cleanerPlugin?: boolean;
  fillInTheBlank?: boolean;
  contentLink?: string; // if specified, the name of this asset, i.e. from whence edges are created
  eBookLink?: boolean;
  contentClass?: string;
  focus?: boolean;
  tabDisable?: boolean;
  resizable?: boolean;
  doneEditing?: () => void; // if specified a done editing button is added or, if mini toolbar, cmd enter
  onImageLink?: (url: string, setUploading: Dispatch<boolean>, editor: any) => void;
  onImageUpload?: (files: File[], setUploading: Dispatch<boolean>, editor: any) => void;
}

/**
 * Wrapper for Summernote editor
 *
 */
const CodeEditor: React.FC<RichTextEditorProps> = ({
  id: uid,
  value,
  mode,
  onChange,
  onBlur = Function.prototype,
  onFocus = Function.prototype,
  size = 'fullPage',
  placeholder,
  lineWrapping,
  initializeWithCodeView,
  readOnly,
  toolbar,
  noCustomTags,
  noCustomFiles,
  noRareStyles,
  codeLast,
  findReplacePlugin,
  cleanerPlugin,
  fillInTheBlank,
  contentLink,
  eBookLink,
  contentClass = 'rich-content',
  focus = true,
  tabDisable = false,
  resizable = false,
  doneEditing,
  onImageLink,
  onImageUpload,
}) => {
  const dispatch = useDispatch();
  const [initialValue] = useState(value);
  const [uploading, setUploading] = useState(false);

  const editor: MutableRefObject<any> = useRef({});
  const currentValue: MutableRefObject<string> = useRef(initialValue);

  // NOTE: the commented mechanisms are designed to allow us to "grab" the contents of the editor
  //        when we want to save an html asset. We get the data and create a file to upload.
  //        I'm leaving this as a reminder of that requirement but we may just use onChange to
  //        keep a copy of changed files for saving if this makes it to those editors.
  // const getValue = () => editor.current.summernote('code');
  // useEffect(() => {
  //   if (initialValue !== currentValue.current && editor.current) {
  //     currentValue.current = initialValue;
  //     updateFromEditor(getValue);
  //     editor.current.summernote('code', initialValue);
  //   }
  // }, [initialValue, updateFromEditor]);

  const onChangeRef = useRef(onChange);
  useEffect(() => {
    onChangeRef.current = onChange;
  }, [onChange]);

  const internalOnChange = useCallback(
    changes => {
      if (changes !== currentValue.current) {
        currentValue.current = changes;
        onChangeRef.current(changes);
      }
    },
    [uid]
  );

  const onBlurRef = useRef(onBlur);
  useEffect(() => {
    onBlurRef.current = onBlur;
  }, [onBlur]);

  const internalOnBlur = useCallback(
    evt => {
      // ::hand waving:: to be sure we are blurring the whole editor.
      const p = evt.target.parentNode.parentNode;
      if (!(evt.relatedTarget && $.contains(p, evt.relatedTarget))) {
        onBlurRef.current?.();
      }
    },
    [uid]
  );

  const onFocusRef = useRef(onFocus);
  useEffect(() => {
    onFocusRef.current = onFocus;
  }, [onFocus]);

  const internalOnFocus = useCallback(() => {
    onFocusRef.current?.();
  }, [uid]);

  const internalOnImageUpload = useCallback(
    (files: File[]) => onImageUpload(files, setUploading, editor.current),
    [onImageUpload, editor, setUploading]
  );

  const internalOnImageUrl = useCallback(
    (url: string, callback: (url: string, metadata: Record<string, string>) => void) =>
      onImageLink(url, setUploading, callback),
    [onImageLink, setUploading]
  );

  const internalOnImageLinkInsert = useCallback(
    (url: string) => {
      internalOnImageUrl(url, (url, metadata) => {
        editor.current.summernote('insertImage', url, function ($image) {
          for (const [k, v] of Object.entries(metadata)) {
            $image.attr(`data-${k}`, v);
          }
        });
      });
    },
    [internalOnImageUrl, editor]
  );

  const doneEditingRef = useRef(doneEditing);
  useEffect(() => {
    doneEditingRef.current = doneEditing;
  }, [doneEditing]);

  const internalDoneEditing = useCallback(() => {
    doneEditingRef.current?.();
  }, [uid]);

  const postInitialize = () => {
    editor.current.summernote(readOnly ? 'disable' : 'enable');
    if (initializeWithCodeView) {
      editor.current.summernote('codeview.toggle');
    }
    // slightly risky change to the default empty state. If we don't do this we can trigger
    // onChange during dismount because '' is falsey.
    $.summernote.dom.emptyPara = '';
    editor.current.on('summernote.change.codeview', (ignore, changes) => internalOnChange(changes));
  };

  const ro = useRef(readOnly);
  useEffect(() => {
    if (editor.current.summernote && readOnly !== ro.current) {
      ro.current = readOnly;
      editor.current.summernote(readOnly ? 'disable' : 'enable');
    }
  }, [readOnly]);

  // This in fact just resets the toolbar???
  // useEffect(() => {
  //   if (editor.current.summernote) {
  //     editor.current.summernote('code', '<p><br></p>');
  //   }
  // }, [uid]);

  const noToolbar = toolbar === 'none';
  const simpleToolbar = toolbar === 'simple';

  useEffect(() => {
    const cleaner = cleanerPlugin
      ? {
          notTime: 2400, // Time to display Notifications.
          action: 'paste', // both|button|paste 'button' only cleans via toolbar button, 'paste' only clean when pasting content, both does both options.
          newline: '', // Summernote's default is to use '<p><br></p>'
          notStyle: 'position:absolute;top:0;left:0;right:0', // Position of Notification
          keepHtml: true, // Remove all Html formats
          keepOnlyTags: [
            '<p>',
            '<pre>',
            '<br>',
            '<ul>',
            '<ol>',
            '<li>',
            '<b>',
            '<strong>',
            '<i>',
            '<a>',
            '<em>',
            '<h1>',
            '<h2>',
            '<h3>',
            '<h4>',
            '<h5>',
            '<h6>',
          ], // If keepHtml is true, remove all tags except these
          keepClasses: false, // Remove Classes
          badTags: ['style', 'script', 'applet', 'embed', 'noframes', 'noscript'], // Remove full tags with contents
          badAttributes: ['style', 'start', 'class', 'id', 'dir'], // Remove attributes from remaining tags
        }
      : {
          action: 'none',
        };
    const toolbar = simpleToolbar
      ? [
          ['style', ['bold', 'italic', 'underline', 'strikethrough', 'clear']],
          ['para', ['style', 'ul', 'ol', 'paragraph']],
          ['color', ['color']],
          ...(doneEditing ? [['done', ['done']]] : []),
        ]
      : [
          ['style', ['bold', 'italic', 'underline', 'clear']],
          ...(noRareStyles ? [] : [['font', ['strikethrough', 'superscript', 'subscript']]]),
          ['para', ['style', 'ul', 'ol', 'paragraph']],
          ...(noRareStyles ? [] : [['color', ['color']]]),
          ...(noCustomFiles ? [] : [['uploads', ['image', 'file']]]),
          [
            'insert',
            noCustomFiles
              ? [
                  'picture',
                  'video',
                  'link',
                  ...(contentLink ? ['content'] : []),
                  ...(eBookLink ? ['ebook'] : []),
                  'table',
                  'hr',
                  'newline',
                  ...(fillInTheBlank ? ['fitb'] : []),
                ]
              : ['video', 'link', 'table', 'hr'],
          ],
          ...(noCustomTags ? [] : [['customtags', ['article', 'aside']]]),
          ...(findReplacePlugin ? [['custom', ['findnreplace']]] : []),
          ['misc', ['undo', 'redo', 'help']],
          ...(codeLast ? [['code', ['beautify']]] : []),
          ...(doneEditing ? [['done', ['done']]] : []),
          ...(cleanerPlugin ? [['cleaner', ['cleaner']]] : []), // configured to not show a button, just run on paste
        ];

    const cmdEnter = doneEditing && (simpleToolbar || noToolbar);
    const keyMap = {
      pc: {
        ...$.summernote.options.keyMap.pc,
        ...(tabDisable
          ? {
              TAB: 'indent',
              'SHIFT+TAB': 'outdent',
            }
          : {}),
        ...(cmdEnter
          ? {
              'CTRL+ENTER': 'customUpload.done',
            }
          : {}),
      },
      mac: {
        ...$.summernote.options.keyMap.mac,
        ...(tabDisable
          ? {
              TAB: 'indent',
              'SHIFT+TAB': 'outdent',
            }
          : {}),
        ...(cmdEnter
          ? {
              'CMD+ENTER': 'customUpload.done',
            }
          : {}),
      },
    };

    const options = {
      toolbar: noToolbar ? [] : toolbar,
      callbacks: {
        onInit: postInitialize,
        onChange: internalOnChange,
        onBlur: internalOnBlur,
        onFocus: internalOnFocus,
        onImageUpload: onImageUpload ? internalOnImageUpload : undefined,
        onImageLinkInsert: onImageLink ? internalOnImageLinkInsert : undefined,
        onImageUrl: onImageLink ? internalOnImageUrl : undefined, // custom callback
      },
      maximumImageFileSize: onImageUpload ? undefined : 4096, // absurdly small size for base 64 behaviour
      height: size === 'inline' ? '' : '40rem',
      placeholder: placeholder,
      focus: focus,
      dialogsInBody: false,
      disableResizeEditor: !resizable,
      inheritPlaceholder: true,
      popatmouse: false,
      tabDisable: tabDisable,
      findnreplace: {
        lang: 'en-US',
      },
      cleaner,
      popover: {
        image: [
          ['imageResize', ['resizeFull', 'resizeHalf', 'resizeQuarter', 'resizeNone']],
          ['float', ['floatLeft', 'floatRight', 'floatNone']],
          ['custom', ['imageTitle']],
          ['remove', ['removeMedia']],
        ],
      },
      keyMap,
      styleTags: ['p', 'h2', 'h3', 'h4', 'h5', 'blockquote', 'pre'],
      lo: { dispatch, name: contentLink, doneEditing: internalDoneEditing },
    };

    editor.current = $(`#${uid}`);
    editor.current.inited = true;
    editor.current.summernote(options);
    return () => {
      if (editor.current.summernote) {
        editor.current.summernote('destroy');
      }
    };
  }, [lineWrapping, mode, uid]);

  return (
    <div className={classNames('summernote-wrapper', contentClass, { uploading })}>
      <div
        id={uid}
        className="naked-html"
        dangerouslySetInnerHTML={{ __html: initialValue }}
      />
    </div>
  );
};

export default CodeEditor;
