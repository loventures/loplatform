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

import MessageFormat from 'messageformat';

import { getI18n, type I18n } from '../../api/i18nApi.ts';
import { formatDayjs } from '../../filters/pure/formatDayjs.ts';

/**
 * Default AngularJS `$interpolate` interpolation (double-brace `{{expr}}`) — the courseware
 * `$translateProvider` default. Translations evaluate full Angular expressions (vars, member access,
 * ternaries, comparisons) and a `| filter:arg` chain; the only filter used is `formatDayjs` (37 sites).
 */
const FILTERS: Record<string, (v: any, ...args: any[]) => any> = { formatDayjs };

/**
 * Evaluate an Angular-style expression against the params. Uses `new Function` (the app already permits it —
 * messageformat compiles with it), with each param as a named arg. Translation strings are server-controlled,
 * the same trust model as AngularJS `$parse`, which also evaluated these `{{…}}` expressions.
 */
const evalExpr = (expr: string, params: any): any => {
  // A Proxy whose `has` trap claims every identifier, so `with(scope)` resolves unknown vars to `undefined`
  // (AngularJS `$parse` semantics: `{{val || 'none'}}` with no `val` → `'none'`) instead of throwing a
  // ReferenceError. The `new Function` body is non-strict, so `with` is permitted (the body is a runtime
  // string, invisible to the bundler).
  const scope = new Proxy(params ?? {}, {
    has: () => true,
    get: (t: any, k: any) => (k in t ? t[k] : undefined),
  });
  // eslint-disable-next-line no-new-func
  const fn = new Function('$scope', `with ($scope) { return (${expr}); }`);
  return fn(scope);
};

const evalArg = (arg: string, params: any): any => {
  try {
    return evalExpr(arg, params);
  } catch {
    return arg; // a bareword filter arg (e.g. `formatDayjs:s`) → the literal string
  }
};

const interpolateDouble = (msg: string, params: any): string =>
  msg.replace(/\{\{\s*([\s\S]+?)\s*\}\}/g, (_m, inner) => {
    try {
      // Split `expr | filter:arg…` on a single pipe (NOT the `||` logical-or operator).
      const parts = String(inner).split(/(?<!\|)\|(?!\|)/);
      let value = evalExpr(parts[0].trim(), params);
      for (let i = 1; i < parts.length; i++) {
        const [fname, ...rawArgs] = parts[i].trim().split(':');
        const fn = FILTERS[fname.trim()];
        if (fn) value = fn(value, ...rawArgs.map(a => evalArg(a.trim(), params)));
      }
      return value == null ? '' : String(value);
    } catch {
      // Unresolvable expression (e.g. a var absent from params) → empty, like `$interpolate` with no scope.
      return '';
    }
  });

/**
 * Pure-TS i18n singleton — replaces the `lojector.get('$translate')` reach-ins (the AngularJS
 * `$translate` service configured in `bootstrap/i18n.jsx`). This module reproduces the exact runtime
 * behaviour of `$translate.instant(...)` under the courseware configuration:
 *
 *  - LOADER: `getI18n(locale, component)` (the same pure axios call angular-translate's loader used),
 *    where `locale = window.lo_platform.i18n.locale` and `component = window.lo_platform.identifier`.
 *  - INTERPOLATION: `$translateMessageFormatInterpolation` — i.e. the `messageformat` package. We
 *    instantiate `new MessageFormat(locale)` once and `mf.compile(msg)` on demand, caching the compiled
 *    fn per message string (matching angular-translate's `'mf:' + string` cache). Before compiling we
 *    coerce integer-string params to numbers, exactly as the interpolator does (so plural/select rules
 *    fire on the numeric type).
 *  - SANITIZE: HTML-escape string params recursively to depth < 3, preserving dates / non-plain objects
 *    (the `useSanitizeValueStrategy('params')` strategy from `bootstrap/i18n.jsx`). Only params are
 *    sanitized; the formatted text is returned verbatim (the courseware strategy is a no-op for 'text').
 *  - MISSING-KEY FALLBACK: `params?._ ?? key` (the `loFallbackTranslator`), also returned when called
 *    before the table has loaded — same as `$translate.instant` before its loader resolves.
 *
 * This module is the sole i18n layer now: `bootstrap/i18n.jsx` + angular-translate were removed in K3c-4 PR-C.
 */

const lo_platform = window.lo_platform;
const locale = lo_platform?.i18n?.locale ?? 'en';
const component = lo_platform?.identifier;

let table: I18n | undefined;
let mf: any | undefined;
const compiledCache = new Map<string, (params: any) => string>();
let loadPromise: Promise<void> | undefined;

/**
 * Load the translation table + create the per-locale MessageFormat instance. Idempotent: repeat calls
 * return the in-flight (or settled) promise. Kicked off eagerly at module load (see `ready` below) so the
 * table is ready by the time React text renders.
 */
export const loadI18n = (): Promise<void> => {
  if (loadPromise) return loadPromise;
  loadPromise = getI18n(locale, component).then(data => {
    table = data;
    mf = new MessageFormat(locale);
  });
  return loadPromise;
};

/**
 * Recursively HTML-escape string params (depth < 3), preserving dates and other non-plain objects.
 * Mirrors `sanitizeParameters` in `bootstrap/i18n.jsx`. `escapeHtml` matches the AngularJS
 * `element.text(value).html()` behaviour — escape `&`, `<`, `>` only (quotes are left untouched, the same
 * as a DOM textContent → innerHTML round-trip). String-based so it works in the (DOM-less) node test env.
 */
const escapeHtml = (value: string): string =>
  value.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');

const isPlainObject = (value: any): boolean =>
  value != null && typeof value === 'object' && value.constructor === Object;

const sanitizeParameters = (value: any, depth: number): any => {
  // test for plain object to avoid destroying dayjs/Date/etc.
  if (isPlainObject(value)) {
    const result: Record<string, any> = {};
    if (depth < 3) {
      // to reduce cost, just drop nested objects beyond depth 3
      for (const key in value) {
        if (Object.prototype.hasOwnProperty.call(value, key)) {
          result[key] = sanitizeParameters(value[key], depth + 1);
        }
      }
    }
    return result;
  } else if (typeof value === 'string') {
    return escapeHtml(value);
  } else {
    return value;
  }
};

/**
 * Synchronous translate — the pure equivalent of `$translate.instant(key, params)`.
 *
 * The optional `format` arg accepts `'messageformat'` for call-site parity with the angular-translate
 * `instant(key, params, 'messageformat')` overload; the AngularJS $interpolate double-brace interpolator is the default; messageformat is used only for that arg.
 *
 * Behaviour:
 *  - `key` null/undefined → returned as-is (matches `$translate.instant`).
 *  - empty string → returned as-is.
 *  - table not loaded yet OR key missing → fallback `params?._ ?? key`.
 *  - else → sanitize params, messageformat-compile `table[key]` (cached), return the interpolated string.
 */
export const instant = (key: string, params?: any, format?: string): string => {
  if (key == null) return key as any;
  if (typeof key === 'string' && key.length < 1) return key;

  const trimmed = typeof key === 'string' ? key.trim() : key;

  const msg = table?.[trimmed];
  if (msg === undefined || mf === undefined) {
    // not loaded yet, or missing key → loFallbackTranslator
    return params?._ ?? trimmed;
  }

  const sanitized = sanitizeParameters(params ?? {}, 0);

  // The DEFAULT interpolator is AngularJS `$interpolate` (double-brace `{{expr}}`). messageformat
  // (single-brace `{n, plural, …}`) is the *added* interpolator (`$translateProvider.addInterpolation`),
  // reached ONLY when the call passes `format === 'messageformat'`. Running every string through
  // messageformat throws on the common `{{…}}` strings ("Expected … but '{' found").
  if (format !== 'messageformat') {
    return interpolateDouble(msg, sanitized);
  }

  // Coerce integer-string params to numbers EVERY call (not just on first compile) so messageformat
  // plural/select rules fire on the numeric type — the compiled fn is cached per message, but the params
  // arrive per call, so a later `{count:'1'}` would otherwise miss the `one{…}` branch ("You have .").
  for (const k in sanitized) {
    if (Object.prototype.hasOwnProperty.call(sanitized, k)) {
      const n = parseInt(sanitized[k], 10);
      if (!isNaN(n) && '' + n === sanitized[k]) {
        sanitized[k] = n;
      }
    }
  }

  let compiled = compiledCache.get(msg);
  if (!compiled) {
    try {
      compiled = mf.compile(msg);
    } catch {
      // A malformed messageformat string must never crash the render — fall back to default interpolation.
      return interpolateDouble(msg, sanitized);
    }
    compiledCache.set(msg, compiled!);
  }

  return compiled!(sanitized);
};

/**
 * Resolves once the translation table is loaded. Awaiting this before reading React text reproduces the
 * `$translate.onReady(...)` delay-render-until-ready behaviour the `TranslationProvider` relied on.
 */
export const ready: Promise<void> = loadI18n();
