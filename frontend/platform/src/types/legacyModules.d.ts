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

/* The deprecated <marquee> element used by the announcement bar. */
declare namespace JSX {
  interface IntrinsicElements {
    marquee: React.DetailedHTMLProps<React.HTMLAttributes<HTMLElement>, HTMLElement>;
  }
}

/* Ambient declarations for third-party packages that ship no TypeScript types. */

declare module 'lodash';

declare module 'react-document-title' {
  import * as React from 'react';
  const DocumentTitle: React.ComponentType<{ title?: string; children?: React.ReactNode }>;
  export default DocumentTitle;
}

declare module 'debounce-promise' {
  function debounce<F extends (...args: any[]) => any>(
    fn: F,
    wait?: number,
    options?: { leading?: boolean }
  ): F;
  export default debounce;
}

declare module 'form-serialize' {
  function serialize(form: HTMLFormElement, options?: { hash?: boolean; empty?: boolean }): any;
  export default serialize;
}

declare module 'throttle-debounce' {
  export function debounce<F extends (...args: any[]) => any>(delay: number, callback: F): F;
  export function throttle<F extends (...args: any[]) => any>(delay: number, callback: F): F;
}

declare module 'react-codemirror' {
  import * as React from 'react';
  interface CodeMirrorProps {
    value?: string;
    className?: string;
    onChange?: (value: string) => void;
    options?: Record<string, any>;
    autoFocus?: boolean;
    style?: React.CSSProperties;
  }
  const CodeMirror: React.ComponentType<CodeMirrorProps>;
  export default CodeMirror;
}

declare module 'codemirror/mode/clike/clike';
declare module 'codemirror/mode/meta';
declare module 'codemirror/mode/javascript/javascript';

/* react-jsonschema-form ships no types; the config admin page imports its
 * internal components/fields/widgets by deep path. */
declare module 'react-jsonschema-form/lib/components/Form' {
  import * as React from 'react';
  const Form: React.ComponentType<any> & { default?: React.ComponentType<any> };
  export default Form;
}
declare module 'react-jsonschema-form/lib/components/fields/SchemaField' {
  import * as React from 'react';
  const SchemaField: React.ComponentType<any> & { default?: React.ComponentType<any> };
  export default SchemaField;
}
declare module 'react-jsonschema-form/lib/components/fields/ObjectField' {
  import * as React from 'react';
  const ObjectField: React.ComponentType<any> & { default?: React.ComponentType<any> };
  export default ObjectField;
}
declare module 'react-jsonschema-form/lib/components/fields/StringField' {
  import * as React from 'react';
  const StringField: React.ComponentType<any> & { default?: React.ComponentType<any> };
  export default StringField;
}
declare module 'react-jsonschema-form/lib/components/widgets/BaseInput' {
  import * as React from 'react';
  const BaseInput: React.ComponentType<any> & { default?: React.ComponentType<any> };
  export default BaseInput;
}
declare module 'react-jsonschema-form/lib/components/widgets/TextWidget' {
  import * as React from 'react';
  const TextWidget: React.ComponentType<any> & { default?: React.ComponentType<any> };
  export default TextWidget;
}
declare module 'react-jsonschema-form/lib/components/widgets/UpDownWidget' {
  import * as React from 'react';
  const UpDownWidget: React.ComponentType<any> & { default?: React.ComponentType<any> };
  export default UpDownWidget;
}
