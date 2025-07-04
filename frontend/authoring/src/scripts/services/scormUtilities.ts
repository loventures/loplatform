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

import JSZip from 'jszip';
import { map } from 'lodash';

function readFile(file) {
  return new Promise((resolve, reject) => {
    const deferred = { resolve, reject };

    const fileReader = new FileReader();

    fileReader.onload = fileLoadedEvent => {
      // @ts-ignore
      const { result } = fileLoadedEvent.target;
      JSZip.loadAsync(result)
        .then(contents => {
          const { files } = contents;

          const paths = Object.keys(files).filter(path => {
            return !path.endsWith('/');
          });

          //does a safe query so things don't crash if the manifest is missing a selector
          const queryTextContent = (dom, selector) => {
            const el = dom.querySelector(selector);
            return el ? el.textContent || '' : '';
          };

          //does a safe query so things don't crash if the manifest is missing a selector/attribute
          const queryAttr = (dom, selector, attr) => {
            const el = dom.querySelector(selector);
            return el ? el.getAttribute(attr) || '' : '';
          };

          return contents
            .file('imsmanifest.xml')
            .async('string')
            .then(function (text) {
              const manifest = new DOMParser().parseFromString(text, 'text/xml');

              // the title of the SCORM activity
              const title = queryTextContent(manifest, 'organizations > organization > title');

              // the href to the very first resource, the default landing page for the SCORM activity
              const resourcePath = queryAttr(manifest, 'resources > resource[href]', 'href');

              // all of the rest of the hrefs, and identifierrefs, which maybe navigation can use
              const allRefs = {};
              map(manifest.querySelectorAll('item[identifierref],resource[href]'), (o: Element) => {
                allRefs[o.getAttribute('identifier')] =
                  o.getAttribute('identifierref') || o.getAttribute('href');
              });

              // objectives of the SCORM activity, queried by some SCORMs, maybe
              // note: objectives can repeat, so we must unique them with a Set()
              const objIds = new Set();
              map(
                manifest.querySelectorAll('primaryObjective[objectiveID],objective[objectiveID]'),
                (o: Element) => {
                  objIds.add(o.getAttribute('objectiveID'));
                }
              );
              const objectiveIds = Array.from(objIds);

              // the minimum score to pass from the primary objective
              const passingScore = queryTextContent(
                manifest,
                'primaryObjective[objectiveID] > minNormalizedMeasure'
              );

              // IDs that can be used across SCORM activities. likely not a widely used feature, but yet a feature
              const dataIds = new Set();
              map(
                manifest.querySelectorAll('item > data > map[writeSharedData=true]'),
                (o: Element) => {
                  dataIds.add(o.getAttribute('targetID'));
                }
              );
              const sharedDataIds = Array.from(dataIds);

              deferred.resolve({
                paths,
                title,
                resourcePath,
                allRefs,
                passingScore,
                objectiveIds,
                sharedDataIds,
              });
            });
        })
        .catch(reject);
    };

    fileReader.onerror = function () {
      deferred.reject(fileReader.error);
    };

    fileReader.readAsArrayBuffer(file);
  });
}

export function generateScormMetadata(file) {
  return readFile(file);
}
