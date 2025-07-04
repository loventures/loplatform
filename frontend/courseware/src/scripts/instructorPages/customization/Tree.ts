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

import { zip } from 'fp-ts/es6/Array';
import { every } from 'lodash';
import { Option } from '../../types/option';

export type Tree<T> = {
  value: T;
  children: Tree<T>[];
};

/**
 * Build a tree with no children
 * @param node
 */
export function leaf<T>(node: T): Tree<T> {
  return { value: node, children: [] };
}

/**
 * Build a tree with chilidren
 * @param node
 * @param children
 */
export function tree<T>(node: T, ...children: Tree<T>[]): Tree<T> {
  return { value: node, children };
}

/**
 * Map over each value in the tree, returning a new tree
 * @param tree
 * @param f
 */
export function map<A, B>(tree: Tree<A>, f: (a: A) => B): Tree<B> {
  return {
    value: f(tree.value),
    children: tree.children.map(child => map(child, f)),
  };
}

/**
 * Map over each value and its children in the tree
 * @param tree
 * @param f
 */
export function mapWithChildren<A, B>(tree: Tree<A>, f: (a: A, children: A[]) => B): Tree<B> {
  return {
    value: f(
      tree.value,
      tree.children.map(n => n.value)
    ),
    children: tree.children.map(child => mapWithChildren(child, f)),
  };
}

/**
 * Maps depth first through the tree while passing a list of ancestors through to the mapper.
 */
export function tdhisto<A, B>(tree: Tree<A>, f: (a: A, ancestors: B[]) => B): Tree<B> {
  function inner(ancestors: B[], tree: Tree<A>): Tree<B> {
    const computedValue = f(tree.value, ancestors);
    return {
      value: computedValue,
      children: tree.children.map(c => inner([...ancestors, computedValue], c)),
    };
  }

  return inner([], tree);
}

/**
 * Maps depth first with previously-mapped ancestors and to-be-mapped children
 */
export function mapWithContext<A, B>(
  tree: Tree<A>,
  f: (a: A, ancestors: B[], children: A[]) => B
): Tree<B> {
  function inner(ancestors: B[], tree: Tree<A>): Tree<B> {
    const computedValue = f(
      tree.value,
      ancestors,
      tree.children.map(t => t.value)
    );
    return {
      value: computedValue,
      children: tree.children.map(c => inner([...ancestors, computedValue], c)),
    };
  }

  return inner([], tree);
}
/**
 * Visits each node depth first and sorts the children using the comparator
 */
export function sortChildren<A>(
  tree: Tree<A>,
  comparator: (a: Tree<A>, b: Tree<A>) => number
): Tree<A> {
  return {
    value: tree.value,
    children: tree.children.sort(comparator).map(c => sortChildren(c, comparator)),
  };
}

/**
 * Constructs a list from all the values in this tree
 * @param tree
 */
export function toList<A>(tree: Tree<A>): A[] {
  return [tree.value, ...tree.children.flatMap(toList)];
}

/**
 * Returns a new tree where the values are the original values,
 *   plus a list of their ancestors
 *
 * zipWithAncestors
 * @param tree
 */
export function zipWithAncestors<A>(tree: Tree<A>): Tree<[A[], A]> {
  function inner(ancestors: A[], t: Tree<A>): Tree<[A[], A]> {
    return {
      value: [ancestors, t.value],
      children: t.children.map(c => inner([...ancestors, t.value], c)),
    };
  }

  return inner([], tree);
}

/**
 * Reduces each value in tree to a sinigle value
 * @param tree
 */
export function fold<A>(tree: Tree<A>): <Z>(initial: Z, f: (z: Z, a: A) => Z) => Z {
  // bah i think toList can be in terms of fold...
  return (initial, f) => toList(tree).reduce(f, initial);
}

/**
 * Counts the number of nodes in a tree
 * @param tree
 */
export function count<A>(tree: Tree<A>): number {
  return fold(tree)(0, (sum, _) => sum + 1);
}

/**
 * Returns a list of each node with non-zero chilidren
 * @param tree
 */
export function nodesWithChildren<A>(tree: Tree<A>): A[] {
  return tree.children.length > 0
    ? [tree.value, ...tree.children.flatMap(t => nodesWithChildren(t))]
    : [];
}

/**
 * Counts the number of nodes with chilidren
 * @param tree
 */
export function countNodesWithChildren<A>(tree: Tree<A>): number {
  return nodesWithChildren(tree).length;
}

/**
 * Returns all nodes that match the supplied predicate
 * @param tree
 * @param f the predicate to
 */
export function findAll<A>(tree: Tree<A>, f: (a: A) => boolean): A[] {
  if (f(tree.value)) {
    return [tree.value, ...tree.children.flatMap(n => findAll(n, f))];
  } else {
    return tree.children.flatMap(n => findAll(n, f));
  }
}

/**
 * Finds a node that mathes a predicate
 * @param tree
 * @param f
 */
export function find<A>(tree: Tree<A>, f: (a: A) => boolean): Option<Tree<A>> {
  if (f(tree.value)) {
    return tree;
  } else if (tree.children.length === 0) {
    return null;
  } else {
    // if there are any children that are not null, that means we've found it
    return tree.children
      .map(t => find(t, f))
      .find(b => {
        return b !== null && typeof b !== 'undefined';
      });
  }
}

// TODO: can these be generalized

export function findParent<A>(tree: Tree<A>, f: (a: A) => boolean): Option<Tree<A>> {
  // if any of tree's children mtach, return it
  const matchedChild = tree.children.find(node => f(node.value));
  if (matchedChild) {
    return tree;
  } else if (tree.children.length === 0) {
    return null;
  } else {
    return tree.children
      .map(t => findParent(t, f))
      .find(b => {
        return b !== null && typeof b !== 'undefined';
      });
  }
}

export function findPath<A, Z>(tree: Tree<A>, f: (a: A) => boolean, g: (a: A) => Z): Option<Z[]> {
  function inner(tree: Tree<A>, path: Z[]): Option<Z[]> {
    if (f(tree.value)) {
      return [...path, g(tree.value)];
    } else if (tree.children.length === 0) {
      return null;
    } else {
      return tree.children
        .map(c => inner(c, [...path, g(tree.value)]))
        .find(num => {
          return num !== null && typeof num !== 'undefined';
        });
    }
  }

  return inner(tree, []);
}

/**
 * Finds the index of a node wrt it's siblings. Performs depth-first, and returns the first match.
 * @param tree
 * @param f
 */
export function findIndex<A>(tree: Tree<A>, f: (a: A) => boolean): Option<number> {
  function inner(tree: Tree<A>, index: number): Option<number> {
    if (f(tree.value)) {
      return index;
    } else if (tree.children.length === 0) {
      return null;
    } else {
      return tree.children
        .map((c, i) => inner(c, i))
        .find(num => {
          return num !== null && typeof num !== 'undefined';
        });
    }
  }

  return inner(tree, 0);
}

export function filter<A>(tree: Tree<A>, f: (a: A) => boolean): Tree<A> {
  if (tree.children.length === 0) {
    return tree;
  } else {
    const filteredChildren = tree.children.filter(c => f(c.value));
    return {
      value: tree.value,
      children: filteredChildren.map(c => filter(c, f)),
    };
  }
}

export function modifyAt<A>(
  tree: Tree<A>
): (f: (a: A) => boolean) => (map: (a: A) => A) => Tree<A> {
  return predicate => modifier => {
    return map(tree, a => {
      if (predicate(a)) {
        return modifier(a);
      } else {
        return a;
      }
    });
  };
}

export function modifyTreeAt<A>(
  tree: Tree<A>
): (f: (tree: Tree<A>) => boolean) => (map: (tree: Tree<A>) => Tree<A>) => Tree<A> {
  return predicate => modifier => {
    if (predicate(tree)) {
      return modifier(tree);
    } else {
      return {
        value: tree.value,
        children: tree.children.map(child => modifyTreeAt(child)(predicate)(modifier)),
      };
    }
  };
}

export function treesEqual<A>(left: Tree<A>[], right: Tree<A>[]): boolean {
  // eslint-disable-next-line @typescript-eslint/no-use-before-define
  return every(zip(left, right), ([l, r]) => treeEqual(l, r));
}

export function treeEqual<A>(left: Tree<A>, right: Tree<A>): boolean {
  return (
    left.value === right.value &&
    left.children.length === right.children.length &&
    treesEqual(left.children, right.children)
  );
}
