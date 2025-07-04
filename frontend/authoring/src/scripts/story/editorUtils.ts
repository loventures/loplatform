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

import { Dispatch, useCallback, useEffect, useRef } from 'react';
import { BlobRef, BlockPart, HtmlPart, NewAsset, NodeName } from '../types/asset';
import AuthoringApiService from '../services/AuthoringApiService.ts';
import { useDispatch } from 'react-redux';
import {
  addProjectGraphEdge,
  addProjectGraphNode,
  computeEditedTargets,
  getAllEditedOutEdges,
  getEditedAsset,
  putNodeBlobRef,
  useGraphEditSelector,
} from '../graphEdit';
import { NewEdge } from '../types/edge';
import { openToast, TOAST_TYPES } from '../toast/actions.ts';
import { serveUrl } from './editors/file/util.ts';
import { Thunk } from '../types/dcmState';
import { invert, mapValues } from 'lodash';
import { ProjectGraph } from '../structurePanel/projectGraphReducer.ts';
import { ProjectGraphEditState } from '../graphEdit/graphEditReducer.ts';
import { beautifyCode } from '../code/beautifyCode';

export const isHtmlPart = (part: HtmlPart | BlockPart | undefined): part is HtmlPart =>
  part?.partType === 'html';

// An empty document is often <p><br></p>
const NothingRE = /<\/?(?:p|span|div)>|<br\s*\/?>|\s+/g;

// Crude blank check. If the content is above 200 characters assumes non blank.
export const isBlankHtml = (html: string | undefined): boolean =>
  !html || (html.length < 200 && !html.replace(NothingRE, ''));

export const dropTrailingParagraphs = (html: string): string =>
  html.replace(/(\s*<p><br><\/p>)*$/, '');

// summernote needs that trailing blank paragraph.
export const normalizeBlankHtml = (html: string | undefined): string =>
  isBlankHtml(html) ? '<p><br></p>' : html!;

export const QuotedLoEdgeRE = /"loEdgeId:\/\/([0-9a-fA-F-]{36})"/g;

export const RenderedUrlRE =
  /\/api\/v2\/authoring\/(\d+)\/nodes\/([0-9a-fA-F-]{36})\/serve(?:\?[^"]*)?/;

export const QuotedRenderedUrlRE =
  /"\/api\/v2\/authoring\/(\d+)\/nodes\/([0-9a-fA-F-]{36})\/serve(?:\?[^"]*)?"/g;

// renders for viewing inside summernote or part editor
export const renderLoEdges = (
  value: string,
  branchId: number,
  targets: Record<string, string>,
  blobRefs?: Record<NodeName, BlobRef>
) =>
  value.replace(QuotedLoEdgeRE, (_, edgeId) =>
    serveUrl(branchId, targets[edgeId], blobRefs?.[targets[edgeId]])
  );

export const derenderLoEdges = (value: string, targets: Record<string, string>) => {
  const edgeIds = invert(targets);
  return value.replace(QuotedRenderedUrlRE, (_match, _project, name) => {
    return `loEdgeId://${edgeIds[name]}`;
  });
};

// This is horrid and so unused...
export const derenderAsset = (
  asset: NewAsset<any>,
  projectGraph: ProjectGraph,
  graphEdits: ProjectGraphEditState
) => {
  let edgeCache: Record<string, string> | undefined = undefined;

  const derenderValue = (value: any, key: string, object: any) =>
    key === 'html' && object.partType === 'html'
      ? value.replace(RenderedUrlRE, (_match, _project, name) => {
          let edgeIds = (edgeCache ??= Object.fromEntries(
            getAllEditedOutEdges(asset.name, projectGraph, graphEdits)
              .filter(edge => edge.group === 'resources')
              .map(edge => [edge.targetName, edge.edgeId])
          ));
          return `loEdgeId://${edgeIds[name]}`;
        })
      : derenderData(value);

  const derenderData = (data: any) =>
    Array.isArray(data)
      ? data.map(derenderData)
      : data && typeof data === 'object'
        ? mapValues(data, derenderValue)
        : data;

  return { ...asset, data: derenderData(asset.data) };
};

const doUpload =
  (file: File, setUploading: Dispatch<boolean>, editor: any, name: NodeName): Thunk =>
  (dispatch, getState) => {
    const { projectGraph, graphEdits } = getState();

    const images = computeEditedTargets(name, 'resources', 'image.1', projectGraph, graphEdits);
    setUploading(true);
    AuthoringApiService.uploadToBlobstore(
      {
        typeId: 'image.1',
        data: { title: file.name || 'Untitled' },
      },
      file
    )
      .then(blobData => {
        const image = images.find(
          rsc =>
            rsc.data.source?.provider === blobData.provider &&
            rsc.data.source?.name === blobData.name
        );
        if (image != null) {
          return serveUrl(projectGraph.branchId, image.name, blobData);
        } else {
          const addAsset: NewAsset<any> = {
            name: crypto.randomUUID(),
            typeId: 'image.1', // well.. for now
            data: {
              title: file.name || 'Untitled',
              source: blobData,
            },
          };
          dispatch(addProjectGraphNode(addAsset));
          const addEdge: NewEdge = {
            name: crypto.randomUUID(),
            edgeId: crypto.randomUUID(),
            sourceName: name,
            targetName: addAsset.name,
            group: 'resources',
            traverse: true,
            data: {},
            newPosition: 'end',
          };
          dispatch(addProjectGraphEdge(addEdge));
          dispatch(putNodeBlobRef(addAsset.name, blobData));
          return serveUrl(projectGraph.branchId, addAsset.name, blobData);
        }
      })
      .then(url => {
        editor.summernote('insertImage', url);
      })
      .catch(e => {
        console.warn(e);
        dispatch(openToast('An upload error occurred.', TOAST_TYPES.DANGER));
      })
      .finally(() => setUploading(false));
  };

export const useCmsUpload = (name: NodeName) => {
  const dispatch = useDispatch();

  return useCallback(
    (files: File[], setUploading: Dispatch<boolean>, editor: any) =>
      dispatch(doUpload(files[0], setUploading, editor, name)),
    [name]
  );
};

const AdobeStockAssetRE = /^.*stock\.adobe\.com.*\/(\d+)(?:\?.*)?$/;
const ApiV2RE = /\/api\/v2\//;

const fetchAdobeStock =
  (
    url: string,
    assetId: string,
    setUploading: Dispatch<boolean>,
    callback: (url: string, metadata: Record<string, string>) => void
  ): Thunk =>
  (dispatch, getState) => {
    const adobeStockApiKey = getState().configuration.adobeStockApiKey;
    if (!adobeStockApiKey) {
      callback(url, {});
      return;
    }
    setUploading(true);
    fetch(
      `https://stock.adobe.io/Rest/Media/1/Search/Files?locale=en_US&search_parameters%5Bmedia_id%5D=${assetId}&search_parameters%5Blimit%5D=1`,
      {
        headers: {
          'X-API-Key': adobeStockApiKey,
          'X-Product': 'CampusPack/4.0',
        },
        method: 'GET',
      }
    )
      .then(res => res.json())
      .then(res => {
        const count = res.files?.length ?? 0;
        if (count !== 1) {
          console.log(res);
          dispatch(openToast(`Adobe API returned ${count} results.`, TOAST_TYPES.DANGER));
          return;
        }
        const { id, creator_name, thumbnail_url, title } = res.files[0];
        callback(thumbnail_url, {
          'ip-pickup': 'New',
          'ip-asset-type': 'Image',
          'ip-asset-source': 'Adobe Stock',
          'ip-asset-source-id': `${id}`,
          'ip-description': title,
          'ip-credit-line': creator_name,
        });
      })
      .catch(e => {
        console.warn(e);
        dispatch(openToast('An Adobe API error occurred.', TOAST_TYPES.DANGER));
      })
      .finally(() => setUploading(false));
  };

const linkNativeAsset =
  (
    assetName: string,
    imageName: string,
    callback: (url: string, metadata: Record<string, string>) => void
  ): Thunk =>
  (dispatch, getState) => {
    const { projectGraph, graphEdits } = getState();
    const image = getEditedAsset(imageName, projectGraph, graphEdits);
    // console.log(assetName, imageName, image);
    if (image?.typeId !== 'image.1') {
      dispatch(openToast('Not a valid image URL.', TOAST_TYPES.DANGER));
      return;
    }
    const edges = getAllEditedOutEdges(assetName, projectGraph, graphEdits);
    const existing = edges.find(
      edge => edge.group === 'resources' && edge.targetName === imageName
    );
    if (!existing) {
      const addEdge: NewEdge = {
        name: crypto.randomUUID(),
        edgeId: crypto.randomUUID(),
        sourceName: assetName,
        targetName: imageName,
        group: 'resources',
        traverse: true,
        data: {},
        newPosition: 'end',
      };
      dispatch(addProjectGraphEdge(addEdge));
    }
    callback(serveUrl(projectGraph.branchId, imageName, graphEdits.blobRefs[imageName]), {});
  };

export const useCmsInsert = (name: NodeName) => {
  const dispatch = useDispatch();
  return useCallback(
    (
      url: string,
      setUploading: Dispatch<boolean>,
      callback: (url: string, metadata: Record<string, string>) => void
    ) => {
      const adobeStockMatch = url.match(AdobeStockAssetRE);
      const renderMatch = url.match(RenderedUrlRE);
      if (renderMatch) {
        dispatch(linkNativeAsset(name, renderMatch[2], callback));
      } else if (url.match(ApiV2RE)) {
        dispatch(openToast('This is not a valid URL', TOAST_TYPES.DANGER));
      } else if (adobeStockMatch) {
        dispatch(fetchAdobeStock(url, adobeStockMatch[1], setUploading, callback));
      } else if (url) {
        callback(url, {});
      }
    },
    []
  );
};

export const useExitEditingOnSave = (exitEditing: () => void) => {
  const generation = useGraphEditSelector(state => state.generation);
  const initialGeneration = useRef(generation);
  useEffect(() => {
    if (generation !== initialGeneration.current) exitEditing();
  }, [generation, exitEditing]);
  // useEffect(() => {
  //   const listener = (ev: MessageEvent) => void (ev.data.type === 'exitEdit' && exitEditing());
  //   window.addEventListener('message', listener, false);
  //   return () => window.removeEventListener('message', listener, false);
  // }, [exitEditing]);
};

export const edgeTargetMap = (edges: NewEdge[]) =>
  Object.fromEntries(
    edges.filter(edge => edge.group === 'resources').map(edge => [edge.edgeId, edge.targetName])
  );

// This relies on editHtml just dispatching more actions.
// I could add new edges for unlinked assets at this point, which would
// support people manually adding links but...
export const derenderHtmlAction =
  (name: NodeName, value: string, editHtml: (html: string) => void): Thunk =>
  (_, getState) => {
    const { projectGraph, graphEdits } = getState();

    const edges = getAllEditedOutEdges(name, projectGraph, graphEdits);
    const edgeTargets = edgeTargetMap(edges);

    editHtml(beautifyCode(derenderLoEdges(value, edgeTargets)));
  };
