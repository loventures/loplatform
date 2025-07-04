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

/**
 * Conditional types for extracting parameter names from url templates.
 *
 * The original idea and concept comes from https://ja.nsommer.dk/articles/type-checked-url-router.html
 *
 * */

type ExtractMatrix<T extends string> = string extends T
  ? T
  : // extract the matrix param from the const={var} syntax
    T extends `${infer Start}={${infer Matrix}}`
    ? Matrix
    : never;

export type MatrixParams<T extends string> = string extends T
  ? T
  : // no need to narrow a full template because matrix params are so unique. They never match without the =
    T extends `${infer Start};${infer Rest}`
    ? MatrixParams<Start> | MatrixParams<Rest>
    : // match start , param , rest
      T extends `${infer Start};${infer MatrixSegment};${infer Rest}`
      ? MatrixParams<Start> | ExtractMatrix<MatrixSegment> | MatrixParams<Rest>
      : // match start ; param
        T extends `${infer Start};${infer MatrixSegment}`
        ? MatrixParams<Start> | ExtractMatrix<MatrixSegment>
        : // discard query params
          T extends `${infer Start}?${infer Rest}`
          ? MatrixParams<Start>
          : // match the param
            T extends `${infer MatrixSegment}`
            ? ExtractMatrix<MatrixSegment>
            : // none found
              never;

export type QueryParams<T extends string> = string extends T
  ? T
  : // narrow a full template to just query params
    T extends `${infer Start}?${infer Rest}`
    ? QueryParams<Rest>
    : // match start , param , rest
      T extends `${infer Start},${infer QueryParam},${infer Rest}`
      ? QueryParams<Start> | QueryParam | QueryParams<Rest>
      : // match start , param
        T extends `${infer Start},${infer QueryParam}`
        ? QueryParams<Start> | QueryParam
        : // match , param , rest
          T extends `,${infer QueryParam},${infer Rest}`
          ? QueryParam | QueryParams<Rest>
          : // match the param
            T extends `${infer QueryParam}`
            ? QueryParam
            : // none found
              never;

export type PathParams<T extends string> = string extends T
  ? T
  : // Match sandwiched variable
    T extends `${infer Start}/{${infer Variable}}/${infer Rest}`
    ? PathParams<Start> | Variable | PathParams<Rest>
    : // Match variable just before query params
      T extends `${infer Start}/{${infer Variable}}?${infer Rest}`
      ? PathParams<Start> | Variable
      : // Match ending variable
        T extends `${infer Start}/{${infer Variable}}`
        ? PathParams<Start> | Variable
        : // Match variable beginning the string
          T extends `{${infer Variable}}/${infer Rest}`
          ? Variable | PathParams<Rest>
          : // Match [variable]
            T extends `{${infer Variable}}`
            ? Variable
            : // Exclude mixed values /
              T extends `${infer Start}{${infer Rest}}`
              ? [`Invalid template, can't mix types: ${Start}{${Rest}}`]
              : // No variables found
                never;

export type TemplateParams<T extends string> = string extends T
  ? T
  : // Match empty strings
    T extends ''
    ? ['Please provide a template']
    : // Match whitespace
      T extends `${infer _Start} ${infer _Rest}`
      ? [`Whitespace disallowed in url's`]
      : // Split into recursive path and query params [path/variables]?[query variables]
        T extends `${infer Start}?${infer Rest}`
        ? TemplateParams<Start> | QueryParams<Rest>
        : // Split into recursive path and matrix params [path/variables]?[matrix variables]
          T extends `${infer Start};${infer Rest}`
          ? PathParams<Start> | MatrixParams<T> // note how matrix scans the whole url.
          : // Assume we only have path variables.
            PathParams<T>;

/**
 * Params objects to be passed to a template. There are several options depending on how permissive
 * one wants to be.
 * */
export type LooseParamsObject<T extends string = string> = {
  [key in TemplateParams<T>]?: string | number;
};

export type PathParamsObject<T extends string = string> = {
  [key in PathParams<T>]: string | number;
};

export type QueryParamsObject<T extends string = string> = {
  [key in QueryParams<T>]?: string | number;
};

export type MatrixParamsObject<T extends string = string> = {
  [key in MatrixParams<T>]?: string | number;
};

export type StrictParamsObject<T extends string = string> = PathParamsObject<T> &
  QueryParamsObject<T> &
  MatrixParamsObject<T>;

/******************* Examples *******************/
type Extracted1 = TemplateParams<'posts/{postId}/comments/{commentId}?user,id'>;
type Extracted2 = PathParams<'posts/{postId}/comments/{commentId}?user,id'>;
type Extracted3 = QueryParams<'posts/{postId}/comments/{commentId}?user,id'>;
type Extracted4 =
  MatrixParams<'/api/v2/{assessment}/attemptOverview;context={contextId};paths={paths};userId={userId}?viewingAs,summarize'>;

type ExtractedLooseObject =
  LooseParamsObject<'/api/v2/{assessment}/attemptOverview/{id};context={contextId};paths={paths}?viewingAs,summarize'>;
type ExtractedPathObject = PathParamsObject<'posts/{postId}/comments/{commentId}?user,id'>;
type ExtractedQueryObject = QueryParamsObject<'posts/{postId}/comments/{commentId}?user,id'>;
type ExtractedStrictObject =
  StrictParamsObject<'/api/v2/{assessment}/attemptOverview;context={contextId};paths={paths};userId={userId}?viewingAs,summarize'>;
