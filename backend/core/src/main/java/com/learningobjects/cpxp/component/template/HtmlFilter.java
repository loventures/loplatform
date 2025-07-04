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

/*
 * HtmlFilter
 *
 * Expand ${variables}, implement if="..." conditional elements and
 * foreach="var:collection" looping elements.
 */

package com.learningobjects.cpxp.component.template;

import com.learningobjects.cpxp.BaseServiceMeta;
import com.learningobjects.cpxp.BaseWebContext;
import com.learningobjects.cpxp.Url;
import com.learningobjects.cpxp.component.ComponentSupport;
import com.learningobjects.cpxp.component.RpcInstance;
import com.learningobjects.cpxp.component.TagInstance;
import com.learningobjects.cpxp.component.element.CustomTag;
import com.learningobjects.cpxp.component.util.*;
import com.learningobjects.cpxp.service.Current;
import com.learningobjects.cpxp.shale.JsonMap;
import com.learningobjects.cpxp.util.DeXssSupport;
import com.learningobjects.cpxp.util.GuidUtil;
import com.learningobjects.cpxp.util.NumberUtils;
import com.learningobjects.cpxp.util.ObjectUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.apache.commons.collections4.map.SingletonMap;
import org.apache.commons.lang3.StringUtils;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.NamespaceSupport;

import java.io.IOException;
import java.io.Writer;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class HtmlFilter implements ContentHandler, LexicalHandler {

    public static class HtmlFilterException extends Exception {

        public HtmlFilterException(String message) {
            super(message);
        }

        public HtmlFilterException(String message, Throwable cause) {
            super(message, cause);
        }

    }

    public static final String UGNS_NAME = "urn:ug:1.0";

    static final Logger logger = Logger.getLogger(HtmlFilter.class.getName());

    // Variable bindings, etc.
    private RenderContext   _context;
    // Absolute value is structural depth inside elements that are being
    // skipped; count is positive (and increases with depth) when the
    // containing element is also to be suppressed (false if,
    // 0-iteration loop), and negative (and decreases with depth) when
    // the containing element should still be displayed (body=""
    // replacement).
    private int             _suppressing = 0;
    // Structural depth inside a loop.
    private int             _looping = -1;
    // End of writing chain, for writing material that won't be filtered or
    // escaped.
    private Writer          _rawWriter;
    // Map from element QName to set of attributes which are rewritable
    // resource URLs for that element.
    private Map<String,Set<String>> _resourceAttributes =
            new HashMap<String,Set<String>>();
    // Repeater used to record loop bodies and closures.
    private Repeater        _repeater = null;
    // Recipient of all onbound events.
    private ContentHandler  _contentHandler;
    // Tag to invoke as callback.
    private TagInstance     _callbackTag = null;
    private CustomTag       _customTag = null;
    private HtmlTemplate    _callbackTemplate = null;
    private int             _callbackStart = -1;
    private int             _atParamStart = -1;
    private NamespaceSupport _ns;

    // Just a struct to look after loop data.
    private class LoopContext {
        int            _pos;        // Start index of loop in _repeater.
        String         _varName;    // Index variable name.
        ScriptIterator _iterator;   // Iterator.
        AttributesImpl _atts;       // Copy of atts for iteration.
        int            _oldLooping; // Copy of container's _looping.
        int            _depth;      // Recursion depth.
        String         _namespaceURI; // Replacement for recursion.
        String         _localName;  // Replacement for recursion.
        String         _qName;      // Replacement for recursion.
        int            _exit;       // Return point on exit for recursion.
    }

    private Stack<LoopContext> _loops = new Stack<LoopContext>();

    private Stack<Boolean> _tests = new Stack<Boolean>(); // Stacked if/elif/else test status
    private boolean _test; // Whether a previous if/elif/else condition has evaluated to true

    private Stack<Integer> _lets = new Stack<Integer>();

    private static Set<String> oneSet(String member) {
        Set<String> rv = new TreeSet<String>();
        rv.add(member);
        return rv;
    }

    HtmlFilter(RenderContext context, ContentHandler contentHandler,
               Writer rawWriter, NamespaceSupport ns) {
        _context = context;
        _contentHandler = contentHandler;
        _rawWriter = rawWriter;
        _resourceAttributes.put("a", oneSet("href"));
        _resourceAttributes.put("link", oneSet("href"));
        _resourceAttributes.put("img", oneSet("src"));
        _resourceAttributes.put("script", oneSet("src"));
        _ns = ns;
    }

    public void setRepeater(Repeater repeater) {
        _repeater = repeater;
    }

    // Matches 'string-constant', function.name(args) and x.y.z with optional negation.  Four groups:
    //   1) string-constant value
    //   2) optional !
    //   3) function.name of function.name(args) or x.y.z
    //   4) args of x.y.z(args)
    //   5) numeric constant
    //   6) optional l|L
    ///////////////// THESE TWO *MUST* BE MAINTAINED IN LOCK STEP /////////////
    private static final Pattern PRIMARY =
            Pattern.compile("\\s*(?:'((?:[^'}\\\\]|\\\\['}\\\\])*)'|(!)?([a-zA-Z_\\$][a-zA-Z0-9_\\.\\$:]*)(?:\\(([^)}]*)\\))?|(-?[0-9]+)([lL])?)\\s*");
    ///////////////// THESE TWO *MUST* BE MAINTAINED IN LOCK STEP /////////////
    // Non-grouping version of PRIMARY.
    private static final Pattern NG_PRIMARY =
            Pattern.compile("\\s*(?:'(?:[^'}\\\\]|\\\\['}\\\\])*'|!?[a-zA-Z_\\$][a-zA-Z0-9_\\.\\$:]*(?:\\([^)}]*\\))?|-?[0-9]+[lL]?)\\s*");
    ///////////////// THESE TWO *MUST* BE MAINTAINED IN LOCK STEP /////////////

    private static final Pattern RELOP = Pattern.compile("(?:<|<=|>|>=|&&|\\|\\||==|!=|\\?|:|-|\\+)");

    // Matches a rudimentary expression. Two groups:
    //   1) the left hand primary
    //   2) the operator and right hand expression
    ///////////////// THESE TWO *MUST* BE MAINTAINED IN LOCK STEP /////////////
    private static final Pattern PRIMARY_EXP = Pattern.compile("(" +
            NG_PRIMARY + ")((?:" + RELOP + NG_PRIMARY + ")*)");
    ///////////////// THESE TWO *MUST* BE MAINTAINED IN LOCK STEP /////////////
    private static final Pattern NG_PRIMARY_EXP = Pattern.compile(
            NG_PRIMARY + "(?:" + RELOP + NG_PRIMARY + ")*");
    ///////////////// THESE TWO *MUST* BE MAINTAINED IN LOCK STEP /////////////

    private static final Pattern FIRST_PRIMARY = Pattern.compile("(" + NG_PRIMARY + ")");
    private static final Pattern NEXT_PRIMARY = Pattern.compile("(" + RELOP + ")(" + NG_PRIMARY + ")");

    // To match parameter lists: first, next and all.

    ///////////////// THESE TWO *MUST* BE MAINTAINED IN LOCK STEP /////////////
    private static final Pattern FIRST_PARAM = Pattern.compile("(" +
            NG_PRIMARY_EXP + ")");
    ///////////////// THESE TWO *MUST* BE MAINTAINED IN LOCK STEP /////////////
    private static final Pattern NG_FIRST_PARAM = NG_PRIMARY_EXP;
    ///////////////// THESE TWO *MUST* BE MAINTAINED IN LOCK STEP /////////////

    ///////////////// THESE TWO *MUST* BE MAINTAINED IN LOCK STEP /////////////
    private static final Pattern NEXT_PARAM = Pattern.compile(",(" +
            NG_PRIMARY_EXP + ")");
    ///////////////// THESE TWO *MUST* BE MAINTAINED IN LOCK STEP /////////////
    private static final Pattern NG_NEXT_PARAM = Pattern.compile("," +
            NG_PRIMARY_EXP);
    ///////////////// THESE TWO *MUST* BE MAINTAINED IN LOCK STEP /////////////

    private static final Pattern NG_PARAMS = Pattern.compile(NG_FIRST_PARAM +
            "(?:" + NG_NEXT_PARAM + ")*");

    // To match ${...}, ${... op ...}, $$call and
    // $$call(params).  Three groups:
    //   1) primary exp
    //   2) call of $$call or $$call(params)
    //   3) params of $$call(params)
    ///////////////// THESE TWO *MUST* BE MAINTAINED IN LOCK STEP /////////////
    static final Pattern EXPRESSION = Pattern.compile("\\$\\{(" +
            NG_PRIMARY_EXP + ")}|\\$\\$([^(\\s,'\"]+)(?:\\((" + NG_PARAMS + ")\\))?");
    ///////////////// THESE TWO *MUST* BE MAINTAINED IN LOCK STEP /////////////
    private static final Pattern NG_EXPRESSION = Pattern.compile("\\$\\{" +
            NG_PRIMARY_EXP + "}|\\$\\$[^(\\s,'\"]+(?:\\(" + NG_PARAMS + "\\))?");
    ///////////////// THESE TWO *MUST* BE MAINTAINED IN LOCK STEP /////////////

    Object evaluateMatchedExpression(Matcher matcher) throws
            HtmlFilterException {
        String call = matcher.group(2);
        if (call == null) { // Single value or ternary operator
            return evaluatePrimaryExpression(matcher.group(1));
        } else { // $$call or $$call(params)
            String paramStr = matcher.group(3);
            if (paramStr == null) { // $$call
                return ComponentUtils.getMessage(call, _context.getScope());
            } else { // $$call(params)
                Matcher pm = FIRST_PARAM.matcher(paramStr);
                List<Object> params = new ArrayList<Object>();
                while (pm.find()) {
                    Object value = evaluatePrimaryExpression(pm.group(1));
                    if (params.isEmpty()) {
                        pm.usePattern(NEXT_PARAM);
                    }
                    params.add(value);
                }
                if ("api".equals(call)) {
                    String url = (String) params.get(0);
                    try {
                        return ComponentSupport.getFn("srs", "api").invoke(StringUtils.join(params, ""));
                    } catch (Exception ex) {
                        logger.log(Level.WARNING, "Api error: " + url, ex);
                        return JsonMap.of("error", "Api error: " + url);
                    }
                } else if ("get".equals(call)) {
                    String url = (String) params.get(0);
                    // TODO: PATH INFO SUPPORT AND ALL THAT JAZZ.. See CurrentFilter#executeRpc
                    try {
                        RpcInstance rpc = ComponentSupport.lookupRpc(url);
                        if (rpc == null) {
                            throw new Exception("Unknown Rpc: " + url);
                        }
                        return rpc.invoke(BaseWebContext.getContext().getRequest()); // TODO: new FakeHttpServletRequest());
                    } catch (Exception ex) {
                        logger.log(Level.WARNING, "Rpc error: " + url, ex);
                        return JsonMap.of("error", "Rpc error: " + url);
                    }
                } else {
                    if ("rpc".equals(call) && (params.size() == 1) && (_context.getContext() instanceof Url)) {
                        params.add(0, _context.getContext());
                    }
                    return ComponentUtils.formatMessage(call, _context.getScope(), params.toArray());
                }
            }
        }
    }

    private Object evaluatePrimaryExpression(String expression) throws HtmlFilterException {
        try {
            Matcher matcher = FIRST_PRIMARY.matcher(expression);
            if (!matcher.find()) {
                throw new HtmlFilterException("Missing primary: " + expression);
            }
            Object value = lookupDeref(matcher.group(1));
            matcher.usePattern(NEXT_PRIMARY);
            while (matcher.find()) {
                String op = matcher.group(1);
                String expr = matcher.group(2);
                if ("?".equals(op)) {
                    if (ComponentUtils.test(value)) {
                        value = lookupDeref(expr);
                        while (matcher.find() && !":".equals(matcher.group(1))) {
                            value = eval(value, matcher.group(1), matcher.group(2));
                        }
                        break;
                    } else {
                        do {
                        } while (matcher.find() && !":".equals(matcher.group(1)));
                        value = lookupDeref(matcher.group(2));
                    }
                } else {
                    value = eval(value, op, expr);
                }
            }
            return value;
        } catch (HtmlFilterException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new HtmlFilterException("Invalid expression: " + expression, ex);
        }
    }

    private Object eval(Object value, String op, String expr) throws HtmlFilterException {
        Object result;
        if ("||".equals(op)) {
            result = ComponentUtils.test(value) ? value : lookupDeref(expr);
        } else if ("&&".equals(op)) {
            result = ComponentUtils.test(value) ? lookupDeref(expr) : value;
        } else if ("==".equals(op)) {
            result = ObjectUtils.equals2(value, lookupDeref(expr));
        } else if ("!=".equals(op)) {
            result = !ObjectUtils.equals2(value, lookupDeref(expr));
        } else if ("<".equals(op)) {
            result = ObjectUtils.compare(value, lookupDeref(expr)) < 0;
        } else if ("<=".equals(op)) {
            result = ObjectUtils.compare(value, lookupDeref(expr)) <= 0;
        } else if (">".equals(op)) {
            result = ObjectUtils.compare(value, lookupDeref(expr)) > 0;
        } else if (">=".equals(op)) {
            result = ObjectUtils.compare(value, lookupDeref(expr)) >= 0;
        } else if ("-".equals(op)) {
            Number n0 = (Number) value, n1 = (Number) lookupDeref(expr);
            if ((n0 instanceof Double) || (n1 instanceof Double)) {
                return NumberUtils.doubleValue(n0) - NumberUtils.doubleValue(n1);
            } else {
                return NumberUtils.longValue(n0) - NumberUtils.longValue(n1);
            }
        } else if ("+".equals(op)) {
            String s0 = (String) value, s1 = (String) lookupDeref(expr);
            return StringUtils.defaultString(s0) + StringUtils.defaultString(s1);
        } else {
            throw new RuntimeException("Unknown operator: " + op);
        }
        return result;
    }

    // Expression value with associated is-singular boolean: if an expression
    // turns out to be a single EXPRESSION with no other material, then
    // isSingular() will return true and the direct value of the evaluated
    // EXPRESSION (list, object, null, etc.) may be obtained using getValue().
    // getString() will return "" for nulls in this instance, and
    // value.toString() for non-null values.
    private static class ExpressionValue {
        boolean _singular;
        Object  _value;
        public ExpressionValue(boolean singular, Object value) { _singular = singular; _value = value; }
        public boolean isSingular() { return _singular; }
        public Object getValue() { return _value; }
        public String getString() { return _value == null ? "" : _value.toString(); }
    }

    private static final Pattern I18N = Pattern.compile("(?s)\\s*\\$\\$(.*)\\$\\$\\s*");
    private static final Pattern I18N_PARAM = Pattern.compile("\\{(\\d+)\\}|\\\\\\\\|\\\\\\{");
    private static final Pattern I18N_KEY = Pattern.compile("([a-zA-Z_]+)\\s*=\\s*(.*)");

    /**
     * $$Foo ${ i0 } \bar\ ${ i1 }.$$
     * Gets turned into the i18n key "Foo {0} \\bar\\ {1}."
     * That's looked up in 18n and expanded out.
     */
    private ExpressionValue expandI18nExpression(CharSequence i18n) throws
      HtmlFilterException {
        // A lot of regex.. precompiling templates would be nice
        Matcher matcher = I18N.matcher(i18n);
        if (! matcher.matches()) {
            return expandExpression(i18n);
        }
        // TODO: How to allow trailing/loading whitespace??
        String expr = matcher.group(1).trim().replaceAll("\\s\\s+", " "); // normalize whitespace
        matcher = EXPRESSION.matcher(expr);
        List<String> parameters = new ArrayList<String>();
        StringBuilder rv = new StringBuilder();
        int index = 0;
        while (matcher.find()) { // Expand parameters and form the i18n key
            rv.append(escapeI18nKey(expr.substring(index, matcher.start())));
            index = matcher.end();
            rv.append('{').append(parameters.size()).append('}');
            Object value = evaluateMatchedExpression(matcher);
            parameters.add(value == null ? "" : value.toString());
        }
        rv.append(escapeI18nKey(expr.substring(index)));
        // TODO: Think about the hierarchy of message keys. Should it be
        // archiveid/componentid/templatename/MSG ???
        String msg = rv.toString(), message;
        Matcher m2 = I18N_KEY.matcher(msg);
        if (m2.matches()) {
            message = ComponentUtils.getMessage(m2.group(1), m2.group(2), _context.getScope());
        } else {
            message = ComponentUtils.i18n(msg, _context.getScope());
        }
        // Replace all {0} parameters with the actual values and unescape \ and {
        StringBuffer sb = new StringBuffer();
        matcher = I18N_PARAM.matcher(message);
        while (matcher.find()) { // Finds {0} or \{ or \\
            String idxs = matcher.group(1), param;
            if (idxs == null) {
                param = matcher.group(0).substring(1); // \{ or \       \
            } else {
                int idx = Integer.parseInt(idxs);
                param = (idx < parameters.size()) ? parameters.get(idx) : "";
            }
            matcher.appendReplacement(sb, Matcher.quoteReplacement(param));
        }
        matcher.appendTail(sb);
        return new ExpressionValue(false, sb.toString());
    }

    private String escapeI18nKey(String part) {
        return part.replaceAll("[\\\\{]", "\\\\$0");
    }

    public String expandString(String expr) throws
      HtmlFilterException {
        ExpressionValue exp = expandExpression(expr);
        if (exp == null) {
            return expr;
        }
        return exp.getString();
    }

    // Expand all ${...} in expr.  Returns null if none found, signifying that
    // expr is unchanged.
    private ExpressionValue expandExpression(CharSequence expr) throws
      HtmlFilterException {
        Matcher matcher = EXPRESSION.matcher(expr);
        if (! matcher.find()) {
            return null;
        }
        if(matcher.start() == 0 && matcher.end() == expr.length()) {
            return new ExpressionValue(true,
                    evaluateMatchedExpression(matcher));
        }
        StringBuffer rv = new StringBuffer();
        do {
            Object value = evaluateMatchedExpression(matcher);
            matcher.appendReplacement(rv, value == null ? "" :
                    Matcher.quoteReplacement(value.toString()));
        } while (matcher.find());
        matcher.appendTail(rv);
        return new ExpressionValue(false, rv.toString());
    }

    private void inlineThrowable(HtmlFilterException e) throws SAXException {
        String guid = GuidUtil.errorGuid();
        comment("(" + guid + ")\n" + e.getMessage());
        logger.log(Level.WARNING, "HtmlFilterException: " + guid, e);
    }

    // Get the value of a primary term ('constant', foo.bar.baz or
    // function.call(args)).
    private Object lookupDeref(String expr) throws HtmlFilterException {
        Matcher matcher = PRIMARY.matcher(expr);
        if (!matcher.matches()) {
            throw new HtmlFilterException("Bad expression: ${" + expr + "}");
        }
        String literal = matcher.group(1), tokens = matcher.group(3);
        if (literal != null) {
            // Undo \', \} and \\ quoting.
            return literal.replaceAll("\\\\(['}\\\\])", "$1");
        } else if (tokens != null) {
            boolean negative = matcher.group(2) != null;
            String params = matcher.group(4);
            Object result;
            if (params == null) {
                if (tokens.startsWith("$$")) {
                    result = ComponentUtils.getMessage(tokens.substring(2), _context.getScope());
                } else {
                    try {
                        result = derefTokens(tokens);
                    } catch (Exception e) {
                        System.out.println(_context.getScope());
                        System.out.println(tokens);
                        e.printStackTrace();
                        throw e;
                    }
                }
            } else {
                Object[] args = parseArgs(params);
                if (tokens.startsWith("$$")) {
                    result = ComponentUtils.formatMessage(tokens.substring(2), _context.getScope(), args);
                } else {
                    result = derefFunction(tokens, args);
                }
            }
            return negative ? !ComponentUtils.test(result) : result;
        } else {
            String numeric = matcher.group(5);
            if (matcher.group(6) != null) { // l|L
                return Long.valueOf(numeric);
            } else {
                return Integer.valueOf(numeric);
            }
        }
    }

    private static final Pattern PRIMITIVE = Pattern.compile("null|true|false|uncallable|Param|Request|Response|Attribute|Session|Current|ProxyUtils|ServiceMeta|WebContext|([0-9]+)");

    private Object derefTokens(String tokens) throws HtmlFilterException {
        String[] parts = tokens.trim().split("\\.");
        int nparts = parts.length;
        if (nparts <= 0) {
            throw new HtmlFilterException("Bad expression: ${" + tokens + "}");
        }
        String first = parts[0];
        Matcher matcher = PRIMITIVE.matcher(first);
        Object o;
        if (matcher.matches()) { // really crude primitive support.
            if ("null".equals(first)) {
                o = null;
            } else if ("true".equals(first)) {
                o = true;
            } else if ("false".equals(first)) {
                o = false;
            } else if ("uncallable".equals(first)) {
                o = new Callable<Void>() {
                    @Override
                    public Void call() {
                        return null;
                    }
                };
            } else if ("ServiceMeta".equals(first)) {
                o = BaseServiceMeta.getServiceMeta();
            } else if ("Current".equals(first)) {
                o = Current.getInstance();
            } else if ("Request".equals(first)) {
                o = BaseWebContext.getContext().getRequest();
            } else if ("Response".equals(first)) {
                o = BaseWebContext.getContext().getResponse();
            } else if ("Param".equals(first)) {
                o = new SingletonMap() {
                        private final HttpServletRequest _request = BaseWebContext.getContext().getRequest();
                        @Override
                        public Object get(Object key) {
                            return _request.getParameter((String) key);
                        }
                    };
            } else if ("Attribute".equals(first)) {
                o = new SingletonMap() {
                        private final HttpServletRequest _request = BaseWebContext.getContext().getRequest();
                        @Override
                        public Object get(Object key) {
                            return _request.getAttribute((String) key);
                        }
                    };
            } else if ("Session".equals(first)) {
                o = new SingletonMap() {
                        private final HttpSession _session = BaseWebContext.getContext().getRequest().getSession(false);
                        @Override
                        public Object get(Object key) {
                            return (_session == null) ? null : _session.getAttribute((String) key);
                        }
                    };
            } else if ("WebContext".equals(first)) {
                o = BaseWebContext.getContext();
            } else {
                o = Long.parseLong(first);
            }
        } else {
            try {
                o = _context.lookup(first);
            } catch (PropertyAccessorException e) {
                /* tagsoup makes [ls]et bindings all lowercase. hacky second try. */
                o = _context.lookupBinding(first.toLowerCase());
                if (o == null) {
                    throw new HtmlFilterException(
                      "PropertyAccessorException dereferencing " + first + ": " + e.getMessage(), e);
                }
            }
        }
        //System.err.println("Deref " + parts[0] + ": " + o);
        for (int i = 1; i < nparts; ++i) {
            try {
                o = ComponentUtils.dereference0(o, parts[i]);
                //System.err.println("  -> " + parts[i] + ": " + o);
            } catch (PropertyAccessorException e) {
                throw new HtmlFilterException(
                        "PropertyAccessorException dereferencing " +
                        StringUtils.join(parts, '.', 0, 1 + i) + ": " +
                        e.getMessage(), e);
            }
        }
        return o;
    }

    private Object[] parseArgs(String params) throws HtmlFilterException {
        // this uses string splitting rather than regex, like EXPRESSION, because
        // PRIMARY can't depend on itself recursively for parameters.
        String[] exprs = StringUtils.split(params.trim(), ',');
        int nparams = exprs.length;
        Object[] args = new Object[nparams];
        for (int i = 0; i < nparams; ++ i) {
            args[i] = lookupDeref(exprs[i].trim());
        }
        return args;
    }

    private Object derefFunction(String tokens, Object[] args) throws
            HtmlFilterException {
        int index = tokens.indexOf(':');
        if (index >= 0) {
            try {
                String prefix = tokens.substring(0, index), suffix = tokens.substring(1 + index);
                String namespace = _ns.getURI(prefix);
                if (namespace == null) {
                    throw new Exception("Unbound namespace prefix");
                }
                return ComponentSupport.getFn(namespace, suffix).invoke(args);
            } catch (Exception e) {
                throw new HtmlFilterException("Bad function call " + tokens + "(" + StringUtils.join(args) + "): " + e.getMessage(), e);
            }
        } else {
            return derefNativeFunction(tokens, args);
        }
    }

    private Object derefNativeFunction(String tokens, Object[] args) throws
            HtmlFilterException {
        // TODO: Cache the lookup?
        int index = tokens.lastIndexOf('.');
        String name = tokens.substring(1 + index);
        Object context = (index < 0) ? _context.getContext() :
                derefTokens(tokens.substring(0, index));
        if (context == null) {
            return null; // anything on null is null...
        }
        for (java.lang.reflect.Method method :
                context.getClass().getMethods()) { // TODO: cache this somehow
            if (name.equals(method.getName()) &&
                    (args.length == method.getParameterTypes().length)) {
                try {
                    return method.invoke(context, args);
                } catch (Exception e) {
                    throw new HtmlFilterException("Bad function call " +
                            tokens + "(" + StringUtils.join(args) + "): " + e.getMessage(), e);
                }
            }
        }
        throw new HtmlFilterException("No method: " + tokens + "(#" + args.length +
                ")");
    }

    private StringBuilder _cb = new StringBuilder();

    // Substitute ${} for whole character sequence, if appropriate.
    // Also skips if in a false conditional.
    @Override
    public void characters(char[] ch, int start, int length) {
        if (_customTag != null) {
            return;
        }
        _cb.append(ch, start, length);
    }

    private void flushCharacters() throws SAXException {
        if (_cb.length() == 0) {
            return;
        }
        if (_suppressing == 0) {
            ExpressionValue expanded = null;
            try {
                expanded = expandI18nExpression(_cb);
            } catch (HtmlFilterException e) {
                inlineThrowable(e);
            }
            if (expanded != null) {
                try {
                    final Object singular = expanded.isSingular() ? expanded.getValue() : null;
                    if ((singular instanceof Callable) || (singular instanceof Html)) {
                        _context.getWriter().write(singular);
                    } else {
                        characters(expanded.getString());
                    }
                } catch (Exception ex) {
                    throw new SAXException("Singular error", ex);
                }
            } else {
                characters(_cb.toString());
            }
        }
        _cb.setLength(0);
    }

    private enum EntryClass {
        FIRST, ODD, EVEN, LAST;
        public String toString() {
            return super.toString().toLowerCase();
        }
    }

    private static final Pattern CLASS_TOKEN_RE = Pattern.compile("([^ {]|\\{[^}]*\\})+");

    // Extra variables created while looping.
    private static final String ITERATOR_SUFFIX = "_i";
    private static final String DEPTH_SUFFIX = "_depth";
    private static final String CSS_SUFFIX = "_css";

    // Special attributes.
    private static final String IF_ATTR = "if";
    private static final String FOREACH_ATTR = "foreach";
    private static final String RECURSE_ATTR = "recurse";
    private static final String TEST_ATTR = "test";
    private static final String VALUE_ATTR = "value";
    private static final String TEMPLATE_ATTR = "template";

    // Special elements.
    private static final String IF_EL = "if";
    private static final String ELSIF_EL = "elif";
    private static final String ELSE_EL = "else";
    private static final String FOREACH_EL = "foreach";
    private static final String OUT_EL = "out";
    private static final String LET_EL = "let";
    private static final String SET_EL = "set";
    private static final String CONS_EL = "cons";

    // Elements not to nerder
    private static final Set<String> _ignoredElements = new HashSet<String>();
    static {
        _ignoredElements.add(OUT_EL);
        _ignoredElements.add(CONS_EL);
        _ignoredElements.add(LET_EL);
        _ignoredElements.add(SET_EL);
        _ignoredElements.add(FOREACH_EL);
    }

    // Add first/odd/even/last to class attribute, as appropriate.  Adds
    // new class attribute if necessary, and clears any preexisting
    // first/odd/even/last values.
    static private void adjustClass(AttributesImpl atts, ScriptIterator it) {
        int idx = atts.getIndex("class");
        Set<String> classes = new TreeSet<String>();
        if (idx >= 0) {
            Matcher matcher = CLASS_TOKEN_RE.matcher(atts.getValue(idx));
            while (matcher.find()) {
                classes.add(matcher.group());
            }
        }
        for (EntryClass e: EntryClass.values()) {
            classes.remove(e.toString());
        }
        if (it.isFirst()) {
            classes.add(EntryClass.FIRST.toString());
        }
        classes.add((it.isOdd() ? EntryClass.ODD : EntryClass.EVEN).toString());
        if (it.isLast()) {
            classes.add(EntryClass.LAST.toString());
        }
        String newClass = StringUtils.join(classes.toArray(), ' ');
        if (idx >= 0) {
            atts.setValue(idx, newClass);
        } else {
            atts.addAttribute("", "class", "class", "CDATA", newClass);
        }
    }

    static private String getCssClass(ScriptIterator it) {
        Set<String> classes = new TreeSet<String>();
        if (it.isFirst()) {
            classes.add(EntryClass.FIRST.toString());
        }
        classes.add((it.isOdd() ? EntryClass.ODD : EntryClass.EVEN).toString());
        if (it.isLast()) {
            classes.add(EntryClass.LAST.toString());
        }
        return StringUtils.join(classes.toArray(), ' ');
    }

    // To pick var and collection out of foreach="var:${collection}".
    private static final Pattern FOREACH =
            Pattern.compile("^\\s*([^\\s:]+)\\s*([:\\[])\\s*(" + NG_EXPRESSION + ")\\s*$");
    // RFC 3986: scheme = ALPHA *( ALPHA / DIGIT / "+" / "-" / "." )
    private static final Pattern DONT_REWRITE =
            Pattern.compile("(?:#|/|\\p{Alpha}[\\p{Alnum}+.-]*:).*");

    // Namespaces we don't turn into callables.
    private static final Set<String> _ignoredNamespaces;
    static {
        _ignoredNamespaces = new HashSet<String>();
        _ignoredNamespaces.add(null);
        _ignoredNamespaces.add("");
        _ignoredNamespaces.add("http://www.w3.org/1999/xhtml");
        _ignoredNamespaces.add(UGNS_NAME);
    }

    // startElement and endElement implement loops and conditionals, as
    // well as expanding attribute values of the form attr="${...}".
    // Note that currently if the value is not exactly and only a ${}
    // then expansion will not occur.  Loops are implemented by presence
    // of a foreach="var:collection" attribute, and conditionals by an
    // if="${varToTest}" attribute.  Both may be used together, but you
    // can't have more than one if or foreach attribute.
    @Override
    public void startElement(String namespaceURI, String localName,
            String qName, Attributes atts) throws SAXException {
        flushCharacters();
        // For ug:body/ug:html
        ExpressionValue bodyValue = null;
        boolean raw = false;

        try {
            // Copy of atts for modification etc; copied on write only.
            AttributesImpl newAtts = null;

            if (_suppressing > 0) { // We're in a false conditional/0-loop
                if ((_suppressing == 1) && qName.startsWith("@")) {
                    _atParamStart = _repeater.getPos() + 1;
                }
                ++_suppressing;
                return;
            } else if (_suppressing < 0) { // We're replacing a body
                --_suppressing;
                return;
            } else if (IF_EL.equals(qName) || ELSIF_EL.equals(qName) || ELSE_EL.equals(qName)) {
                _tests.push(false); // push false so unwinding works if the test throws
                // This does not effectively capture malformedness
                int testIndex = atts.getIndex(TEST_ATTR);
                if ((testIndex < 0) != ELSE_EL.equals(qName)) {
                    throw new HtmlFilterException("Invalid " + qName + " element test attribute");
                }
                boolean test = true;
                if (testIndex >= 0) {
                    ExpressionValue value =
                        expandExpression(atts.getValue(testIndex));
                    // Anything other than a singular expression will be a string,
                    // which evaluates to true
                    test = (value == null) || !value.isSingular() ||
                        ComponentUtils.test(value.getValue());
                }
                boolean state = test || (!IF_EL.equals(qName) && _test);
                _tests.set(_tests.size() - 1, state);
                if (!test || (!IF_EL.equals(qName) && _test)) {
                    _suppressing = 1;
                }
                return;
            } else if (LET_EL.equals(qName) || SET_EL.equals(qName)) {
                boolean let = LET_EL.equals(qName);
                int lets = 0;
                for (int i = 0; i < atts.getLength(); ++ i) {
                    String name = atts.getLocalName(i);
                    Object value = expandObject(atts.getValue(i));
                    if (let) {
                        _context.pushBinding(name, value);
                        ++ lets;
                    } else {
                        _context.setBinding(name, value);
                    }
                }
                if (let) {
                    _lets.push(lets);
                    atts = newAtts = new AttributesImpl();
                } else {
                    _suppressing = 1;
                    return;
                }
            }

            // Implement if="..." conditional.
            // The (uri,localName) version of getIndex() doesn't seem to work.
            int ifIndex = atts.getIndex(IF_ATTR);
            if (ifIndex >= 0) {
                ExpressionValue value =
                        expandExpression(atts.getValue(ifIndex));
                // Anything other than a singular expression will be a string,
                // which evaluates to true; only suppress on false.
                if (value != null && value.isSingular() &&
                        ! ComponentUtils.test(value.getValue())) {
                    _suppressing = 1;
                    return;
                }
                // Predicate is true; remove if attribute before continuing.
                atts = newAtts = new AttributesImpl(atts);
                newAtts.removeAttribute(ifIndex);
            }

            // Implement foreach="var:collection" loop.
            // The (uri,localName) version of getIndex() doesn't seem to work.
            int foreachIndex = atts.getIndex(FOREACH_ATTR);
            // Need this here for conflict-with-recurse test.
            int recurseIndex = atts.getIndex(RECURSE_ATTR);
            // Need this here for conflict-with-callable test.
            boolean isCallable = ! _ignoredNamespaces.contains(namespaceURI) &&
                ComponentSupport.hasComponent(namespaceURI);
            if (_looping == 0) { // Just beginning a new iteration.
                LoopContext lc = _loops.peek();
                // Get a new copy of loop attributes to expand.
                atts = newAtts = new AttributesImpl(lc._atts);
                // adjustClass(newAtts, lc._iterator);
                if (null != lc._qName) {
                    // Recursion: switch to replacement element name.
                    namespaceURI = lc._namespaceURI;
                    localName = lc._localName;
                    qName = lc._qName;
                }
                ++_looping;
            } else if ((foreachIndex >= 0) || FOREACH_EL.equals(qName)) {
                if (recurseIndex >= 0) {
                    throw new HtmlFilterException("Can't have foreach= and recurse= attributes on the same element");
                }
                ExpressionValue value;
                boolean nonMap;
                String varName;
                if (FOREACH_EL.equals(qName)) {
                    if (atts.getLength() != 1) {
                        throw new HtmlFilterException("Bad foreach element attributes");
                    }
                    foreachIndex = 0;
                    varName = atts.getLocalName(0);
                    value = expandExpression(atts.getValue(0));
                    if ((value == null) || !value.isSingular()) {
                        throw new HtmlFilterException("Bad foreach element value: " + atts.getValue(0));
                    }
                    nonMap = false;
                } else {
                    String foreachValue = atts.getValue(foreachIndex);
                    String[] parts = foreachValue.split(":");
                    Matcher matcher = FOREACH.matcher(foreachValue);
                    if (! matcher.find() || matcher.groupCount() != 3) {
                        throw new HtmlFilterException("Bad foreach specifier: \"" +
                                                      foreachValue + "\"");
                    }
                    // Guaranteed to be non-null and singular since group(3) is
                    // already an NG_EXPRESSION.
                    value = expandExpression(matcher.group(3));
                    nonMap = matcher.group(2).equals("[");
                    varName = matcher.group(1);
                }
                Object collection = value.getValue();

                ScriptIterator it = nonMap ?
                        ScriptIterator.getNonMapInstance(collection) :
                        ScriptIterator.getInstance(collection);
                if (it.hasNext()) {
                    // Remove foreach attribute before continuing.
                    if (atts != newAtts) { // Haven't made a writable copy yet.
                        atts = newAtts = new AttributesImpl(atts);
                    }
                    newAtts.removeAttribute(foreachIndex);
                    _context.pushBinding(varName);
                    _context.setBinding(varName, it.getNext());
                    _context.pushBinding(varName + ITERATOR_SUFFIX, it);
                    int depth = 0;
                    _context.pushBinding(varName + DEPTH_SUFFIX, depth);
                    _context.pushBinding(varName + CSS_SUFFIX, getCssClass(it));
                    // adjustClass(newAtts, it);
                    LoopContext lc = new LoopContext();
                    lc._pos = _repeater.getPos();
                    lc._varName = varName;
                    lc._iterator = it;
                    // Copy since we're yet to do value expansion.
                    lc._atts = new AttributesImpl(newAtts);
                    lc._oldLooping = _looping;
                    lc._depth = depth;
                    lc._namespaceURI = null;
                    lc._localName = null;
                    lc._qName = null;
                    lc._exit = -1;
                    _loops.push(lc);
                    _looping = 1;
                } else { // Empty collection: same as if (false)...
                    _suppressing = 1;
                    return;
                }
            } else if (_looping >= 0) { // We're already in a loop.
                ++_looping;
            }

            // Implement recurse="var:collection" loop recursion.
            if (recurseIndex >= 0) {
                if (isCallable) {
                    throw new HtmlFilterException("May not use recurse attribute on a callable element (" + qName + ")");
                }
                String recurseValue = atts.getValue(recurseIndex);
                String[] parts = recurseValue.split(":");
                Matcher matcher = FOREACH.matcher(recurseValue);
                if (! matcher.find() || matcher.groupCount() != 3) {
                    throw new HtmlFilterException("Bad recurse specifier: \"" +
                            recurseValue + "\"");
                }
                // Guaranteed to be non-null and singular since group(3) is
                // already an NG_EXPRESSION.
                ExpressionValue value = expandExpression(matcher.group(3));
                Object collection = value.getValue();
                boolean nonMap = matcher.group(2).equals("[");
                String varName = matcher.group(1);
                LoopContext container = null;
                for (LoopContext cur: _loops) {
                    if (cur._varName.equals(varName)) {
                        container = cur;
                    }
                }
                if (container == null) {
                    throw new HtmlFilterException("Bad recurse specifier \"" +
                            recurseValue + "\": no containing loop over \"" +
                            varName + "\"");
                }

                ScriptIterator it = collection == null ? null : nonMap ?
                        ScriptIterator.getNonMapInstance(collection) :
                        ScriptIterator.getInstance(collection);
                if (it != null && it.hasNext()) {
                    _context.pushBinding(varName);
                    _context.setBinding(varName, it.getNext());
                    _context.pushBinding(varName + ITERATOR_SUFFIX, it);
                    int depth = container._depth + 1;
                    _context.pushBinding(varName + DEPTH_SUFFIX, depth);
                    _context.pushBinding(varName + CSS_SUFFIX, getCssClass(it));
                    // Remove recurse attribute before continuing.
                    if (atts != newAtts) { // Haven't made a writable copy yet.
                        atts = newAtts = new AttributesImpl(atts);
                    }
                    newAtts.removeAttribute(recurseIndex);
                    // adjustClass(newAtts, it);
                    LoopContext lc = new LoopContext();
                    lc._pos = container._pos;
                    lc._varName = varName;
                    lc._iterator = it;
                    // Copy since we're yet to do value expansion.
                    lc._atts = new AttributesImpl(newAtts);
                    lc._oldLooping = _looping;
                    lc._depth = depth;
                    lc._namespaceURI = namespaceURI;
                    lc._localName = localName;
                    lc._qName = qName;
                    lc._exit = 1 + _repeater.getPos();
                    _loops.push(lc);
                    _looping = 1;
                    _repeater.setPos(1 + lc._pos);
                } else { // Empty collection: same as if (false)...
                    _suppressing = 1;
                    return;
                }
            }

            String templateFile = null;
            // Implement template="..." attribute.
            int tplIndex = atts.getIndex(TEMPLATE_ATTR);
            if (tplIndex >= 0) {
                String attValue = atts.getValue(tplIndex);
                ExpressionValue value = expandExpression(attValue);
                templateFile = (value == null) ? attValue : value.getString();
                if (atts != newAtts) {
                    atts = newAtts = new AttributesImpl(atts);
                }
                newAtts.removeAttribute(tplIndex);
            }

            // For all ug:xxx attributes, replace xxx with same if present.
            for (int i = atts.getLength() - 1; i >= 0; --i) {
                if (UGNS_NAME.equals(atts.getURI(i))) {
                    int j = atts.getIndex(atts.getLocalName(i));
                    if (j >= 0) {
                        if (newAtts != atts) {
                            atts = newAtts = new AttributesImpl(atts);
                        }
                        newAtts.setValue(j, atts.getValue(i));
                        newAtts.removeAttribute(i);
                    }
                }
            }

            if (qName.endsWith(".html") || (templateFile != null)) {
                // TODO: callable body...
                String template = (templateFile == null) ? qName : templateFile;
                HtmlTemplate html = HtmlTemplate.apply(_context.getContext(), template);
                for (int i = atts.getLength() - 1; i >= 0; --i) {
                    html = html.bind(atts.getLocalName(i), expandObject(atts.getValue(i)));
                }
                _callbackTemplate = html;
                _suppressing = 1;
                return;
            } else if (isCallable) { // Implement body-as-callable.
                int bodyIndex = atts.getIndex(UGNS_NAME, "body");
                if (bodyIndex < 0) { // Also check we've raw replacement content.
                    bodyIndex = atts.getIndex(UGNS_NAME, "html");
                }
                if (bodyIndex >= 0) {
                    throw new HtmlFilterException("May not use body/html attribute on a callable element (" + qName + ")");
                }
                _suppressing = 1;
                try {
                    _callbackTag = ComponentSupport.getTag(namespaceURI, localName);
                    if (_callbackTag == null) {
                        throw new HtmlFilterException("Unknown tag " + namespaceURI + ":" + localName);
                    }
                } catch (PropertyAccessorException e) {
                    throw new HtmlFilterException(
                            "PropertyAccessorException getting tag " +
                            namespaceURI + ":" + localName + ": " +
                            e.getMessage(), e);
                }
                int na = atts.getLength();
                for (int i = 0; i < na; ++i) {
                    ExpressionValue expanded = expandI18nExpression(atts.getValue(i));
                    Object value = (expanded == null) ? atts.getValue(i)
                        : expanded.getValue();
                    _callbackTag.setParameter(atts.getQName(i), value);
                }
                _callbackStart = _repeater.getPos() +
                        (recurseIndex >= 0 ? 0 : 1);
                return;
            } else if (qName.startsWith("@")) { // at this point we're evaluating the custom tag body
                _suppressing = 1;
                return;
            }

            // ug:body and ug:html replace content with their value --- raw, no
            // escaping, in the case of ug:html. out is raw always.
            int bodyIndex;
            if (OUT_EL.equals(qName)) {
                // Because expansion occurs already and null attributes are deleted I can't require
                // this attribute to be present.
                bodyIndex = atts.getIndex(VALUE_ATTR);
                raw = true;
                _suppressing = -1; // I have to set this in case bodyIndex < 0
            } else {
                bodyIndex = atts.getIndex(UGNS_NAME, "body");
                if (bodyIndex < 0) { // Also check we've raw replacement content.
                    bodyIndex = atts.getIndex(UGNS_NAME, "html");
                    if (bodyIndex >= 0) {
                        raw = true;
                    }
                }
            }

            if (bodyIndex >= 0) {
                bodyValue= expandI18nExpression(atts.getValue(bodyIndex));
                if (newAtts != atts) {
                    atts = newAtts = new AttributesImpl(atts);
                }
                newAtts.removeAttribute(bodyIndex);
                _suppressing = -1;
            }

            // Expand any ${} expressions in atts.  Since Attributes doesn't
            // support setValue, do a copy-on-write into an AttributesImpl.
            for (int i = atts.getLength() - 1; i >= 0; --i) {
                ExpressionValue expanded = expandI18nExpression(atts.getValue(i));
                if (expanded != null) {
                    if (newAtts != atts) {
                        atts = newAtts = new AttributesImpl(atts);
                    }
                    if (expanded.isSingular() && expanded.getValue() == null) {
                        newAtts.removeAttribute(i);
                    } else {
                        newAtts.setValue(i, expanded.getString());
                    }
                }
            }

            // Rewrite resource URLs.
            Set<String> resourceAttributes = _resourceAttributes.get(qName);
            if ((resourceAttributes != null) && (_context.getUrl() != null))  {
                for (String attr: resourceAttributes) {
                    int attrIndex = atts.getIndex(attr);
                    if (attrIndex >= 0) {
                        String attrValue = atts.getValue(attrIndex);
                        if (! DONT_REWRITE.matcher(attrValue).matches()) {
                            if (newAtts != atts) {
                                atts = newAtts = new AttributesImpl(atts);
                            }
                            newAtts.setValue(attrIndex, _context.getUrl() +
                                    attrValue);
                        }
                    }
                }
            }
        } catch (HtmlFilterException e) {
            // In case of exception we inline the error message and unexpanded
            // (at least beyond the point of error) startElement, and suppress
            // content.
            _suppressing = -1;
            inlineThrowable(e);
        }

        if (doRender(namespaceURI, qName)) {
            if (_customTag != null) {
                _customTag.startElement(namespaceURI, localName, atts); // crappy
            } else {
                _contentHandler.startElement(namespaceURI, localName, qName, atts);
            }
        }

        if (bodyValue != null) {
            if (!raw) {
                characters(bodyValue.getString());
            } else {
                try {
                    final Object singular = bodyValue.isSingular() ? bodyValue.getValue() : null;
                    if ((singular instanceof Callable) || (singular instanceof Html)) {
                        _context.getWriter().write(singular);
                    } else {
                        raw(DeXssSupport.wellFormed(bodyValue.getString()));
                    }
                } catch (Exception e) {
                    inlineThrowable(new HtmlFilterException(
                        "Callback failed: " + e.getMessage(), e));
                }
            }
        }
    }

    private Object expandObject(String value) throws HtmlFilterException {
        ExpressionValue expanded = expandExpression(value);
        if (expanded == null) {
            return value;
        } else if (expanded.isSingular()) {
            return expanded.getValue();
        } else {
            return expanded.getString();
        }
    }

    private void comment(String s) throws SAXException {
        boolean debug = BaseWebContext.getDebug();
        raw(debug ? "<pre style=\"color: red\">" : "<!-- ");
        characters(s);
        raw(debug ? "</pre>" : "\n-->");
    }

    private void raw(String s) throws SAXException {
        try {
            _rawWriter.write(s);
        } catch (IOException e) {
            throw new SAXException("Raw write failed: " + e.getMessage(), e);
        }
    }

    private void characters(String s) throws SAXException {
        if (_context.getWriter().getJsonEncoding()) { // hack for javascript support
            raw(s);
        } else {
            char[] chars = s.toCharArray();
            _contentHandler.characters(chars, 0, chars.length);
        }
    }

    // Attribute containing if="${false}" for suppressing recurse elements'
    // bodies after the recursion finishes.
    private static final AttributesImpl ifFalseAttribute;
    static {
        ifFalseAttribute = new AttributesImpl();
        ifFalseAttribute.addAttribute("", "if", "if", "CDATA", "${false}");
    }

    // Decrements if-false depth, handles tail end of loops: iterate,
    // rewind and unread a fake <openingTag>.
    @Override
    public void endElement(String namespaceURI, String localName,
            final String qName) throws SAXException {
        flushCharacters();
        boolean hadCallback = false;

        if (_suppressing > 0) { // We're in a false conditional/0-loop
            --_suppressing;
            if ((0 == _suppressing) && (null != _callbackTag)) {
                final TagInstance callbackTag = _callbackTag;
                final int start = _callbackStart;
                final int end = _repeater.getPos();
                _callbackTag = null;
                try {
                    boolean isCustomTag =
                      CustomTag.class.isAssignableFrom(callbackTag.getFunction().getMethod().getReturnType());
                    if (end > start) {
                        callbackTag.setParameter("body",
                                new Callable<Void>() {
                            public Void call() throws Exception {
                                _repeater.repeat(start, end);
                                flushCharacters();
                                return null;
                            }
                        });
                    }
                    /* XXX: either Html or CustomTag... */
                    Object result = callbackTag.invoke();
                    if (result instanceof CustomTag) {
                        _customTag = (CustomTag) result;
                        _context.getWriter().write(_customTag.render());
                    } else {
                        _context.getWriter().write((Html) result);
                    }
                } catch (Exception e) {
                    inlineThrowable(new HtmlFilterException(
                            "Callback failed: " + e.getMessage(), e));
                }
                _customTag = null;
                hadCallback = true;
            } else if ((0 == _suppressing) && (null != _callbackTemplate)) {
                final HtmlTemplate template = _callbackTemplate;
                _callbackTemplate = null;
                try {
                    _context.getWriter().write(template);
                } catch (Exception e) {
                    inlineThrowable(new HtmlFilterException(
                      "Callback failed: " + e.getMessage(), e));
                }
                hadCallback = true;
            } else if ((1 == _suppressing) && qName.startsWith("@")) {
                final int start = _atParamStart;
                final int end = _repeater.getPos();
                final String name = qName.substring(1);

                _callbackTag.setParameter(name,
                  new Callable<Void>() {
                      public Void call() throws Exception {
                          _repeater.repeat(start, end);
                          flushCharacters();
                          return null;
                      }
                  });
                return;
            } else {
                if ((0 == _suppressing) && (IF_EL.equals(qName) || ELSIF_EL.equals(qName) || ELSE_EL.equals(qName))) {
                    // Control flow element
                    _test = _tests.pop();
                }
                return;
            }
        } else if (_suppressing < 0 && ++_suppressing < 0) {
            // We're replacing a body
            return;
        } else if (IF_EL.equals(qName) || ELSIF_EL.equals(qName) || ELSE_EL.equals(qName)) {
            // Control flow element
            _test = _tests.pop();
            return;
        } else if (LET_EL.equals(qName)) {
            boolean let = LET_EL.equals(qName);
            int lets = _lets.pop();
            while (lets -- > 0) {
                _context.popBinding(null);
            }
        }

        if (_looping > 0 && --_looping == 0) { // Close loop
            LoopContext lc = _loops.peek();
            if (! hadCallback) {
                if (lc._qName == null) {
                    if (doRender(namespaceURI, qName)) {
                        _contentHandler.endElement(namespaceURI, localName, qName);
                    }
                } else { // On a recursion
                    if (doRender(lc._namespaceURI, lc._qName)) {
                        _contentHandler.endElement(lc._namespaceURI, lc._localName, lc._qName);
                    }
                }
            }
            ScriptIterator it = lc._iterator;
            if (it.hasNext()) {
                _context.setBinding(lc._varName, it.getNext());
                _context.setBinding(lc._varName + CSS_SUFFIX, getCssClass(it));
                _looping = 0; // Depth 0 => new iteration.
                _repeater.setPos(lc._pos);
            } else { // Done last iteration
                _context.popBinding(lc._varName + CSS_SUFFIX);
                _context.popBinding(lc._varName + DEPTH_SUFFIX);
                _context.popBinding(lc._varName + ITERATOR_SUFFIX);
                _context.popBinding(lc._varName);
                _loops.pop();
                _looping = lc._oldLooping;
                if (lc._qName != null) { // Just finished a recursion
                    --_looping;
                    _repeater.setPos(lc._exit);
                    // We're just about to run through the original content of
                    // the <x recurse="..."> element, so set up to skip it by
                    // inserting a fake open element with if-false...
                    startElement(lc._namespaceURI, lc._localName, lc._qName,
                            ifFalseAttribute);
                }
            }
        } else if (! hadCallback) {
            if (doRender(namespaceURI, qName)) {
                if (_customTag != null) {
                    // _customTag.endElement(namespaceURI, localName);
                } else {
                    _contentHandler.endElement(namespaceURI, localName, qName);
                }
            }
        }
    }

    private boolean doRender(String namespaceURI, String qName) {
        return !UGNS_NAME.equals(namespaceURI) && !_ignoredElements.contains(qName);
    }

    // Only relevant thing here is to skip if in a false conditional.
    @Override
    public void ignorableWhitespace(char[] ch, int start, int length) throws
            SAXException {
        if (_customTag != null) {
            return;
        }
        flushCharacters();
        if (_suppressing != 0) { // False conditional/0-loop/replace-body
            return;
        }

        _contentHandler.ignorableWhitespace(ch, start, length);
    }

    @Override
    public void startDocument() throws SAXException {
        flushCharacters();
        _contentHandler.startDocument();
    }

    @Override
    public void endDocument() throws SAXException {
        flushCharacters();
        _contentHandler.endDocument();
    }

    @Override
    public void startPrefixMapping(String prefix, String uri) throws
            SAXException {
        flushCharacters();
        _contentHandler.startPrefixMapping(prefix, uri);
    }

    @Override
    public void endPrefixMapping(String prefix) throws SAXException {
        flushCharacters();
        _contentHandler.endPrefixMapping(prefix);
    }

    @Override
    public void processingInstruction(String target, String data) throws
            SAXException {
        flushCharacters();
        _contentHandler.processingInstruction(target, data);
    }

    @Override
    public void setDocumentLocator(Locator locator) {
        if (_cb.length() > 0) {
            throw new RuntimeException("Shouldn't have any chars collected before setDocumentLocator() is called");
        }
        _contentHandler.setDocumentLocator(locator);
    }

    @Override
    public void skippedEntity(String name) throws SAXException {
        flushCharacters();
        _contentHandler.skippedEntity(name);
    }

    private static final Pattern IE_CONDITIONAL_MATCHER = Pattern.compile("\\[(?:if|endif)");

    @Override
    public void comment(char[] ch, int start, int length) {
        String str = new String(ch, start, length);
        if (IE_CONDITIONAL_MATCHER.matcher(str).find()) { // Allows through MSIE conditionals.. <!--[if ... ]--><!--[endif]-->
            try {
               raw("<!--" + expandObject(str) + "-->");
            } catch (Exception ex) {
                throw new RuntimeException("Expanding comment", ex);
            }
        }
    }

    @Override
    public void startCDATA() {
    }

    @Override
    public void endCDATA() {
    }

    @Override
    public void startDTD(String name, String publicId, String systemId) throws SAXException {
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE ").append(name);
        if (publicId != null) {
            sb.append(" PUBLIC \"").append(publicId).append('"');
        }
        if (systemId != null) {
            sb.append(" \"").append(systemId).append('"');
        }
        sb.append(">\n");
        raw(sb.toString());
    }

    @Override
    public void endDTD() {
    }

    @Override
    public void startEntity(String name) {
    }

    @Override
    public void endEntity(String name) {
    }
}
