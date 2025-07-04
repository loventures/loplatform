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

package com.learningobjects.cpxp.component.function;

import com.learningobjects.cpxp.component.annotation.Rpc;
import com.learningobjects.cpxp.component.internal.FunctionDescriptor;
import com.learningobjects.cpxp.component.web.Method;
import com.learningobjects.cpxp.component.web.RpcMethod;
import com.learningobjects.cpxp.util.StringUtils;

import java.lang.annotation.Annotation;
import java.util.*;

public class RpcFunctionRegistry implements FunctionRegistry {

    //the registry is now a tree.
    private Node _functionRoot = new Node();

    @Override
    public void register(FunctionDescriptor function) {
        Annotation a = function.getAnnotation();
        Method method = getRpcMethod(a);
        String name = (a instanceof Rpc) ? ((Rpc) a).name() : "";

        if (StringUtils.isEmpty(name)) {
            name = function.getMethod().getName();
            if (method.isTerminal() && method.name().toLowerCase().equals(StringUtils.camelCasePrefix(name))) { // postFoo => foo
                name = StringUtils.toLowerCaseFirst(name.substring(method.name().length()));
            }
        }
        String mappedName = mapName(method, name);
        Node functionNode = new Node();
        functionNode.rpcFunction = function;
        functionNode.path = mappedName;
        _functionRoot.children.add(functionNode);
    }

    @Override
    public FunctionDescriptor lookup(Object... keys) {

        FunctionDescriptor function;

        if (keys.length == 1 || keys.length == 2 && keys[1].equals("GET")) { // singular lookup means find the get method
            //TODO not sure what is calling this but something is.
            function = findFunction(Method.GET, keys[0].toString(), null);

        } else if (keys.length == 2 || keys.length == 3) {
            //2 keys is an RPC lookup, 3 keys is either RPC or jax-rs. HTTP  method is first, then rel path, then full path (if applicable)

            String m = (String) keys[0];

            String fullPath = null;
            if(keys.length == 3) {
                fullPath = keys[2].toString();
            }

            function = findFunction(Method.valueOf(m), keys[1].toString(), fullPath);

            if (function != null) { // verify that the method is supported
                Method method = getRpcMethod(function.getAnnotation());
                if (!method.matches(m)) {
                    function = null;
                }
            }

        } else {
            return null;
        }

        return function;
    }

    /**
     * Finds the function that either matches the firstPart i.e. "foo" or matches the fullPath i.e. "foo/123".  If both match, fullPath wins.
     * We do this because traditional RPCs only have the first part of the path and do not include placeholders for path parameters, like the jax-rs ones do.
     *
     * @param method the http method
     * @param firstPart the first part of the path
     * @param fullPath the full path - this can be null to be backwards compatible
     * @return  a function descriptor
     */
    private FunctionDescriptor findFunction(Method method, String firstPart, String fullPath) {
        FunctionDescriptor function = null;

        //first check the rpcs which are always right off the function root
        String rpcPath = mapName(method, firstPart);
        //find by POST:foo
        Node match = _functionRoot.findMatch(rpcPath);
        if (match == null) {
            //try finding by just foo
            match = _functionRoot.findMatch(firstPart);
        }

        if(match != null) {
            if(match.rpcFunction != null) {
                function = match.rpcFunction;
            } else if (match.functions.get(method) != null) {
                //this means it was a jax-rs function
                function = match.functions.get(method);
            }
        }


       return function;
    }


    private String mapName(Method method, String path) {
        return (method.isTerminal() && !Method.GET.equals(method)) ? method + ":" + path : path;
    }


    private static Method getRpcMethod(Annotation a) {
        return (a instanceof Rpc) ? ((Rpc) a).method() :  a.annotationType().getAnnotation(RpcMethod.class).value();
    }

    @Override
    public Collection<FunctionDescriptor> lookupAll() {
        Set<FunctionDescriptor> allFunctions = new HashSet<FunctionDescriptor>();

        //traverse the entire tree and pull out all the function descriptions
        addNodeFunctions(allFunctions, _functionRoot);
        return allFunctions;
    }


    private void addNodeFunctions(Collection<FunctionDescriptor> functions, Node node) {

        if(node.rpcFunction != null) {
            functions.add(node.rpcFunction);
        }

        functions.addAll(node.functions.values());

        //now do the same for all my children
        for(Node n : node.children) {
            addNodeFunctions(functions, n);
        }
    }


    private class Node {
        //we store the registry as a tree because of the wildcards that may exist within the paths. So for each part of
        // the path, it is possible that there could be multiple matches. The actual match could be a leaf or not a leaf - it is just the node that we are at when we run out of path.
        String path = "";
        List<Node> children = new ArrayList<Node>();
        Map<Method, FunctionDescriptor> functions = new HashMap<Method, FunctionDescriptor>();

        //only for rpc calls
        FunctionDescriptor rpcFunction;


        /**
         * Finds a match on a nodes children. If useWildcard is allowed and no match is found, but a wildcard node exists, the wildcard will be returned. otherwise null.
         *
         * @param path
         * @return
         */
        Node findMatch(String path) {

            for (Node c : children) {
                if (c.path.toUpperCase().equals(path.toUpperCase())) {
                    return c;
                }
            }

            return null;
        }

    }
}
