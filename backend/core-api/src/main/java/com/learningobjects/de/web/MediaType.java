/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.learningobjects.de.web;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.google.common.base.Predicate;
import com.google.common.collect.Ordering;
import com.learningobjects.cpxp.util.StringUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.collections4.map.CaseInsensitiveMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.util.*;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Represents an Internet Media Type, as defined in the HTTP specification.
 * <p/>
 * Copied from Spring Framework 4.0.1.
 * <p/>
 * <p>Consists of a {@linkplain #getType() type} and a {@linkplain #getSubtype() subtype}.
 * Also has functionality to parse media types from a string using {@link #parseMediaType(String)},
 * or multiple comma-separated media types using {@link #parseMediaTypes(String)}.
 *
 * @see <a href="http://tools.ietf.org/html/rfc2616#section-3.7">HTTP 1.1, section 3.7</a>
 * @since 3.0
 */
public class MediaType implements Comparable<MediaType> {

    /**
     * Public constant media type that includes all media ranges (i.e. {@code &#42;/&#42;}).
     */
    public static final MediaType ALL;

    /**
     * A String equivalent of {@link MediaType#ALL}.
     */
    public static final String ALL_VALUE = "*/*";

    /**
     * Public constant media type for {@code application/atom+xml}.
     */
    public static final MediaType APPLICATION_ATOM_XML;

    /**
     * A String equivalent of {@link MediaType#APPLICATION_ATOM_XML}.
     */
    public static final String APPLICATION_ATOM_XML_VALUE = "application/atom+xml";

    /**
     * Public constant media type for {@code application/x-www-form-urlencoded}.
     */
    public static final MediaType APPLICATION_FORM_URLENCODED;

    /**
     * A String equivalent of {@link MediaType#APPLICATION_FORM_URLENCODED}.
     */
    public static final String APPLICATION_FORM_URLENCODED_VALUE = "application/x-www-form-urlencoded";

    /**
     * Public constant media type for {@code application/json}.
     */
    public static final MediaType APPLICATION_JSON;

    /**
     * A String equivalent of {@link MediaType#APPLICATION_JSON}.
     */
    public static final String APPLICATION_JSON_VALUE = "application/json";

    /**
     * Public constant media type for {@code application/vnd.ims.lis.v2.lineitem+json}.
     */
    public static final MediaType APPLICATION_IMS_LINE_ITEM;

    /**
     * A String equivalent of {@link MediaType#APPLICATION_IMS_LINE_ITEM}.
     */
    public static final String APPLICATION_IMS_LINE_ITEM_VALUE = "application/vnd.ims.lis.v2.lineitem+json";

    /**
     * Public constant media type for {@code application/vnd.ims.lis.v2.result+json}.
     */
    public static final MediaType APPLICATION_IMS_RESULT;

    /**
     * A String equivalent of {@link MediaType#APPLICATION_IMS_RESULT}.
     */
    public static final String APPLICATION_IMS_RESULT_VALUE = "application/vnd.ims.lis.v2p1.result+json";

    /**
     * Public constant media type for {@code application/vnd.ims.lis.v2.result+json}.
     */
    public static final MediaType APPLICATION_IMS_SCORE_V1;

    /**
     * A String equivalent of {@link MediaType#APPLICATION_IMS_SCORE_V1}.
     */
    public static final String APPLICATION_IMS_SCORE_V1_VALUE = "application/vnd.ims.lis.v1.score+json";

    /**
     * Public constant media type for {@code application/json;charset=UTF-8}
     */
    public static final MediaType APPLICATION_JSON_UTF8;

    public static final String CHARSET_UTF_8 = ";charset=UTF-8";

    /**
     * A String equivalent of {@link MediaType#APPLICATION_JSON_UTF8}.
     */
    public static final String APPLICATION_JSON_UTF8_VALUE = APPLICATION_JSON_VALUE + CHARSET_UTF_8;

    /**
     * Public constant media type for {@code application/json-patch+json}.
     */
    public static final MediaType APPLICATION_PATCH_JSON;

    /**
     * A String equivalent of {@link MediaType#APPLICATION_PATCH_JSON}
     */
    public static final String APPLICATION_PATCH_JSON_VALUE = "application/json-patch+json";

    /**
     * Public constant media type for {@code application/schema+json}.
     */
    public static final MediaType APPLICATION_SCHEMA_JSON;

    /**
     * A String equivalent of {@link MediaType#APPLICATION_SCHEMA_JSON}.
     */
    public static final String APPLICATION_SCHEMA_JSON_VALUE = "application/schema+json";

    /**
     * Public constant media type for {@code application/octet-stream}.
     */
    public static final MediaType APPLICATION_OCTET_STREAM;

    /**
     * A String equivalent of {@link MediaType#APPLICATION_OCTET_STREAM}.
     */
    public static final String APPLICATION_OCTET_STREAM_VALUE = "application/octet-stream";

    /**
     * Public constant media type for {@code application/xhtml+xml}.
     */
    public static final MediaType APPLICATION_XHTML_XML;

    /**
     * A String equivalent of {@link MediaType#APPLICATION_XHTML_XML}.
     */
    public static final String APPLICATION_XHTML_XML_VALUE = "application/xhtml+xml";

    /**
     * Public constant media type for {@code application/xml}.
     */
    public static final MediaType APPLICATION_XML;

    /**
     * A String equivalent of {@link MediaType#APPLICATION_XML}.
     */
    public static final String APPLICATION_XML_VALUE = "application/xml";

    /**
     * Public constant media type for {@code application/zip}.
     */
    public static final MediaType APPLICATION_ZIP;

    /**
     * A String equivalent of {@link MediaType#APPLICATION_ZIP}.
     */
    public static final String APPLICATION_ZIP_VALUE = "application/zip";

    public static final MediaType APPLICATION_ZIP_COMPRESSED;

    public static final String APPLICATION_ZIP_COMPRESSED_VALUE = "application/x-zip-compressed";

    /**
     * Public constant media type for {@code application/javascript}.
     */
    public static final MediaType APPLICATION_JAVASCRIPT;

    /**
     * A String equivalent of {@link MediaType#APPLICATION_JAVASCRIPT}.
     */
    public static final String APPLICATION_JAVASCRIPT_VALUE = "application/javascript";

    /**
     * Public constant media type for {@code application/javascript}.
     */
    public static final MediaType TEXT_JAVASCRIPT;

    /**
     * A String equivalent of {@link MediaType#TEXT_JAVASCRIPT}.
     */
    public static final String TEXT_JAVASCRIPT_VALUE = "text/javascript";

    /*
     * A String equivalent of image/svg-xml
     */
    public static final String APPLICATION_SVG_VALUE = "image/svg+xml";
    public static final MediaType APPLICATION_SVG;

    /*
     * A String equivalent of application/x-font-ttf
     */
    public static final String APPLICATION_TTF_VALUE = "application/x-font-ttf";
    public static final MediaType APPLICATION_TTF;

    /*
     * A String equivalent of application/font-woff
     */
    public static final String APPLICATION_WOFF_VALUE = "application/font-woff";

    public static final MediaType APPLICATION_WOFF;

    /*
     * A String equivalent of application/x-font-opentype
     */
    public static final String APPLICATION_OTF_VALUE = "application/x-font-opentype";
    public static final MediaType APPLICATION_OTF;

    /*
     * A String equivalent of application/vnd.ms-fontobject
     */
    public static final String APPLICATION_EOT_VALUE = "application/vnd.ms-fontobject";
    public static final MediaType APPLICATION_EOT;

    /*
     * A String equivalent of application/font-woff2
     */
    public static final String APPLICATION_WOFF2_VALUE = "application/font-woff2";
    public static final MediaType APPLICATION_WOFF2;

    /**
     * Public constant media type for {@code image/gif}.
     */
    public static final MediaType IMAGE_GIF;

    /**
     * A String equivalent of {@link MediaType#IMAGE_GIF}.
     */
    public static final String IMAGE_GIF_VALUE = "image/gif";

    /**
     * Public constant media type for {@code image/jpeg}.
     */
    public static final MediaType IMAGE_JPEG;

    /**
     * A String equivalent of {@link MediaType#IMAGE_JPEG}.
     */
    public static final String IMAGE_JPEG_VALUE = "image/jpeg";

    /**
     * Public constant media type for {@code image/png}.
     */
    public static final MediaType IMAGE_PNG;

    /**
     * A String equivalent of {@link MediaType#IMAGE_PNG}.
     */
    public static final String IMAGE_PNG_VALUE = "image/png";

    /**
     * Public constant media type for {@code image/bmp}.
     */
    public static final MediaType IMAGE_BMP;

    /**
     * A String equivalent of {@link MediaType#IMAGE_BMP}.
     */
    public static final String IMAGE_BMP_VALUE = "image/x-ms-bmp";

    public static final MediaType IMAGE_WEBP;
    public static final String IMAGE_WEBP_VALUE = "image/webp";

    /**
     * Public constant media type for {@code multipart/form-data}.
     */
    public static final MediaType MULTIPART_FORM_DATA;

    /**
     * A String equivalent of {@link MediaType#MULTIPART_FORM_DATA}.
     */
    public static final String MULTIPART_FORM_DATA_VALUE = "multipart/form-data";

    /**
     * Public constant media type for {@code multipart/alternative}.
     */
    public static final MediaType MULTIPART_ALTERNATIVE;

    /**
     * A String equivalent of {@link MediaType#MULTIPART_ALTERNATIVE}.
     */
    public static final String MULTIPART_ALTERNATIVE_VALUE = "multipart/alternative";

    /**
     * Public constant media type for {@code multipart/*}.
     */
    public static final MediaType MULTIPART_ALL;

    /**
     * A String equivalent of {@link MediaType#MULTIPART_ALL}.
     */
    public static final String MULTIPART_ALL_VALUE = "multipart/*";

    /**
     * Public constant media type for {@code text/html}.
     */
    public static final MediaType TEXT_HTML;

    /**
     * A String equivalent of {@link MediaType#TEXT_HTML}.
     */
    public static final String TEXT_HTML_VALUE = "text/html";

    /**
     * Public constant media type for {@code text/plain}.
     */
    public static final MediaType TEXT_PLAIN;

    /**
     * A String equivalent of {@link MediaType#TEXT_PLAIN}.
     */
    public static final String TEXT_PLAIN_VALUE = "text/plain";

    /**
     * Public constant media type for {@code text/xml}.
     */
    public static final MediaType TEXT_XML;

    /**
     * A String equivalent of {@link MediaType#TEXT_XML}.
     */
    public static final String TEXT_XML_VALUE = "text/xml";

    /**
     * Public constant media type for {@code text/css}.
     */
    public static final MediaType TEXT_CSS;
    public static final MediaType TEXT_CSS_UTF_8;

    /**
     * A String equivalent of {@link MediaType#TEXT_CSS}.
     */
    public static final String TEXT_CSS_VALUE = "text/css";

    public static final String TEXT_VTT_VALUE = "text/vtt";
    public static final MediaType TEXT_VTT;

    public static final MediaType TEXT_PLAIN_UTF_8;
    public static final String TEXT_PLAIN_UTF_8_VALUE = TEXT_PLAIN_VALUE + CHARSET_UTF_8;
    public static final MediaType TEXT_HTML_UTF_8;
    public static final String TEXT_HTML_UTF_8_VALUE = TEXT_HTML_VALUE + CHARSET_UTF_8;
    public static final MediaType TEXT_XML_UTF_8;
    public static final String TEXT_XML_UTF_8_VALUE = TEXT_XML_VALUE + CHARSET_UTF_8;
    public static final MediaType TEXT_CSV;
    public static final String TEXT_CSV_VALUE = "text/csv";
    public static final MediaType TEXT_CSV_UTF_8;
    public static final String TEXT_CSV_UTF_8_VALUE = "text/csv" + CHARSET_UTF_8;
    public static final MediaType TEXT_EVENT_STREAM_UTF_8;
    public static final String TEXT_EVENT_STREAM_UTF_8_VALUE = "text/event-stream" + CHARSET_UTF_8;
    public static final MediaType APPLICATION_PDF;
    public static final String APPLICATION_PDF_VALUE = "application/pdf";
    public static final String APPLICATION_OOXML_DOC_VALUE = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
    public static final MediaType APPLICATION_OOXML_DOC;
    public static final String APPLICATION_OOXML_XLS_VALUE = "application/vnd.ms-excel"; // bad show old chap
    public static final MediaType APPLICATION_OOXML_XLS;
    public static final String APPLICATION_OOXML_PPT_VALUE = "application/vnd.ms-powerpoint"; // bad show old chap
    public static final MediaType APPLICATION_OOXML_PPT;
    public static final MediaType APPLICATION_UNKNOWN;
    public static final String APPLICATION_UNKNOWN_VALUE = "application/unknown";


    private static final BitSet TOKEN;

    private static final String WILDCARD_TYPE = "*";

    private static final String PARAM_QUALITY_FACTOR = "q";

    private static final String PARAM_CHARSET = "charset";


    private final String type;

    private final String subtype;

    private final Map<String, String> parameters;


    static {
        // variable names refer to RFC 2616, section 2.2
        BitSet ctl = new BitSet(128);
        for (int i = 0; i <= 31; i++) {
            ctl.set(i);
        }
        ctl.set(127);

        BitSet separators = new BitSet(128);
        separators.set('(');
        separators.set(')');
        separators.set('<');
        separators.set('>');
        separators.set('@');
        separators.set(',');
        separators.set(';');
        separators.set(':');
        separators.set('\\');
        separators.set('\"');
        separators.set('/');
        separators.set('[');
        separators.set(']');
        separators.set('?');
        separators.set('=');
        separators.set('{');
        separators.set('}');
        separators.set(' ');
        separators.set('\t');

        TOKEN = new BitSet(128);
        TOKEN.set(0, 128);
        TOKEN.andNot(ctl);
        TOKEN.andNot(separators);

        ALL = MediaType.valueOf(ALL_VALUE);
        APPLICATION_ATOM_XML = MediaType.valueOf(APPLICATION_ATOM_XML_VALUE);
        APPLICATION_FORM_URLENCODED = MediaType.valueOf(APPLICATION_FORM_URLENCODED_VALUE);
        APPLICATION_JSON = MediaType.valueOf(APPLICATION_JSON_VALUE);
        APPLICATION_IMS_LINE_ITEM = MediaType.valueOf(APPLICATION_IMS_LINE_ITEM_VALUE);
        APPLICATION_IMS_RESULT = MediaType.valueOf(APPLICATION_IMS_RESULT_VALUE);
        APPLICATION_IMS_SCORE_V1 = MediaType.valueOf(APPLICATION_IMS_SCORE_V1_VALUE);
        APPLICATION_JSON_UTF8 =  MediaType.valueOf(APPLICATION_JSON_UTF8_VALUE);
        APPLICATION_PATCH_JSON = MediaType.valueOf(APPLICATION_PATCH_JSON_VALUE);
        APPLICATION_SCHEMA_JSON = MediaType.valueOf(APPLICATION_SCHEMA_JSON_VALUE);
        APPLICATION_OCTET_STREAM = MediaType.valueOf(APPLICATION_OCTET_STREAM_VALUE);
        APPLICATION_XHTML_XML = MediaType.valueOf(APPLICATION_XHTML_XML_VALUE);
        APPLICATION_XML = MediaType.valueOf(APPLICATION_XML_VALUE);
        APPLICATION_ZIP = MediaType.valueOf(APPLICATION_ZIP_VALUE);
        APPLICATION_ZIP_COMPRESSED = MediaType.valueOf(APPLICATION_ZIP_COMPRESSED_VALUE);
        APPLICATION_JAVASCRIPT = MediaType.valueOf(APPLICATION_JAVASCRIPT_VALUE);
        TEXT_JAVASCRIPT = MediaType.valueOf(TEXT_JAVASCRIPT_VALUE);
        APPLICATION_WOFF = MediaType.valueOf(APPLICATION_WOFF_VALUE);
        APPLICATION_TTF = MediaType.valueOf(APPLICATION_TTF_VALUE);
        APPLICATION_EOT = MediaType.valueOf(APPLICATION_EOT_VALUE);
        APPLICATION_SVG = MediaType.valueOf(APPLICATION_SVG_VALUE);
        APPLICATION_OTF = MediaType.valueOf(APPLICATION_OTF_VALUE);
        APPLICATION_WOFF2 = MediaType.valueOf(APPLICATION_WOFF2_VALUE);
        IMAGE_GIF = MediaType.valueOf(IMAGE_GIF_VALUE);
        IMAGE_JPEG = MediaType.valueOf(IMAGE_JPEG_VALUE);
        IMAGE_PNG = MediaType.valueOf(IMAGE_PNG_VALUE);
        IMAGE_BMP = MediaType.valueOf(IMAGE_BMP_VALUE);
        IMAGE_WEBP = MediaType.valueOf(IMAGE_WEBP_VALUE);
        MULTIPART_FORM_DATA = MediaType.valueOf(MULTIPART_FORM_DATA_VALUE);
        MULTIPART_ALTERNATIVE = MediaType.valueOf(MULTIPART_ALTERNATIVE_VALUE);
        MULTIPART_ALL = MediaType.valueOf(MULTIPART_ALL_VALUE);
        TEXT_HTML = MediaType.valueOf(TEXT_HTML_VALUE);
        TEXT_PLAIN = MediaType.valueOf(TEXT_PLAIN_VALUE);
        TEXT_PLAIN_UTF_8 = MediaType.valueOf(TEXT_PLAIN_UTF_8_VALUE);
        TEXT_XML = MediaType.valueOf(TEXT_XML_VALUE);
        TEXT_CSS = MediaType.valueOf(TEXT_CSS_VALUE);
        TEXT_CSS_UTF_8 = MediaType.valueOf(TEXT_CSS_VALUE + CHARSET_UTF_8);

        TEXT_HTML_UTF_8 = MediaType.valueOf(TEXT_HTML_UTF_8_VALUE);
        TEXT_XML_UTF_8 = MediaType.valueOf(TEXT_XML_UTF_8_VALUE);
        TEXT_CSV = MediaType.valueOf(TEXT_CSV_VALUE);
        TEXT_CSV_UTF_8 = MediaType.valueOf(TEXT_CSV_UTF_8_VALUE);
        TEXT_EVENT_STREAM_UTF_8 = MediaType.valueOf(TEXT_EVENT_STREAM_UTF_8_VALUE);
        APPLICATION_PDF = MediaType.valueOf(APPLICATION_PDF_VALUE);
        APPLICATION_OOXML_DOC = MediaType.valueOf(APPLICATION_OOXML_DOC_VALUE);
        APPLICATION_OOXML_XLS = MediaType.valueOf(APPLICATION_OOXML_XLS_VALUE);
        APPLICATION_OOXML_PPT = MediaType.valueOf(APPLICATION_OOXML_PPT_VALUE);
        APPLICATION_UNKNOWN = MediaType.valueOf(APPLICATION_UNKNOWN_VALUE);
        TEXT_VTT = MediaType.valueOf(TEXT_VTT_VALUE);
    }


    /**
     * Create a new {@code MediaType} for the given primary type.
     * <p>The {@linkplain #getSubtype() subtype} is set to {@code &#42;}, parameters empty.
     *
     * @param type the primary type
     *
     * @throws IllegalArgumentException if any of the parameters contain illegal characters
     */
    public MediaType(String type) {
        this(type, WILDCARD_TYPE);
    }

    /**
     * Create a new {@code MediaType} for the given primary type and subtype.
     * <p>The parameters are empty.
     *
     * @param type    the primary type
     * @param subtype the subtype
     *
     * @throws IllegalArgumentException if any of the parameters contain illegal characters
     */
    public MediaType(String type, String subtype) {
        this(type, subtype, Collections.<String, String>emptyMap());
    }

    /**
     * Create a new {@code MediaType} for the given type, subtype, and character set.
     *
     * @param type    the primary type
     * @param subtype the subtype
     * @param charSet the character set
     *
     * @throws IllegalArgumentException if any of the parameters contain illegal characters
     */
    public MediaType(String type, String subtype, Charset charSet) {
        this(type, subtype, Collections.singletonMap(PARAM_CHARSET, charSet.name()));
    }

    /**
     * Create a new {@code MediaType} for the given type, subtype, and quality value.
     *
     * @param type         the primary type
     * @param subtype      the subtype
     * @param qualityValue the quality value
     *
     * @throws IllegalArgumentException if any of the parameters contain illegal characters
     */
    public MediaType(String type, String subtype, double qualityValue) {
        this(type, subtype, Collections.singletonMap(PARAM_QUALITY_FACTOR, Double.toString(qualityValue)));
    }

    /**
     * Copy-constructor that copies the type and subtype of the given {@code MediaType},
     * and allows for different parameter.
     *
     * @param other      the other media type
     * @param parameters the parameters, may be {@code null}
     *
     * @throws IllegalArgumentException if any of the parameters contain illegal characters
     */
    public MediaType(MediaType other, Map<String, String> parameters) {
        this(other.getType(), other.getSubtype(), parameters);
    }

    /**
     * Create a new {@code MediaType} for the given type, subtype, and parameters.
     *
     * @param type       the primary type
     * @param subtype    the subtype
     * @param parameters the parameters, may be {@code null}
     *
     * @throws IllegalArgumentException if any of the parameters contain illegal characters
     */
    public MediaType(String type, String subtype, Map<String, String> parameters) {
        checkArgument(StringUtils.isNotEmpty(type), "type must not be empty");
        checkArgument(StringUtils.isNotEmpty(subtype), "subtype must not be empty");
        checkToken(type);
        checkToken(subtype);
        this.type = type.toLowerCase(Locale.ENGLISH);
        this.subtype = subtype.toLowerCase(Locale.ENGLISH);
        if (!MapUtils.isEmpty(parameters)) {
            Map<String, String> m = new CaseInsensitiveMap<>(parameters.size());
            for (Map.Entry<String, String> entry : parameters.entrySet()) {
                String attribute = entry.getKey();
                String value = entry.getValue();
                checkParameters(attribute, value);
                m.put(attribute, value);
            }
            this.parameters = Collections.unmodifiableMap(m);
        } else {
            this.parameters = Collections.emptyMap();
        }
    }

    /**
     * Checks the given token string for illegal characters, as defined in RFC 2616, section 2.2.
     *
     * @throws IllegalArgumentException in case of illegal characters
     * @see <a href="http://tools.ietf.org/html/rfc2616#section-2.2">HTTP 1.1, section 2.2</a>
     */
    private void checkToken(String token) {
        for (int i = 0; i < token.length(); i++) {
            char ch = token.charAt(i);
            if (!TOKEN.get(ch)) {
                throw new IllegalArgumentException("Invalid token character '" + ch + "' in token \"" + token + "\"");
            }
        }
    }

    private void checkParameters(String attribute, String value) {
        checkArgument(StringUtils.isNotEmpty(attribute), "parameter attribute must not be empty");
        checkArgument(StringUtils.isNotEmpty(value), "parameter value must not be empty");
        checkToken(attribute);
        if (PARAM_QUALITY_FACTOR.equals(attribute)) {
            value = unquote(value);
            double d = Double.parseDouble(value);
            checkArgument(d >= 0D && d <= 1D, "Invalid quality value \"" + value + "\": should be between 0.0 and 1.0");
        } else if (PARAM_CHARSET.equals(attribute)) {
            value = unquote(value);
            Charset.forName(value);
        } else if (!isQuotedString(value)) {
            checkToken(value);
        }
    }

    private boolean isQuotedString(String s) {
        if (s.length() < 2) {
            return false;
        } else {
            return ((s.startsWith("\"") && s.endsWith("\"")) || (s.startsWith("'") && s.endsWith("'")));
        }
    }

    private String unquote(String s) {
        if (s == null) {
            return null;
        }
        return isQuotedString(s) ? s.substring(1, s.length() - 1) : s;
    }

    /**
     * Return the primary type.
     */
    public String getType() {
        return this.type;
    }

    /**
     * Indicates whether the {@linkplain #getType() type} is the wildcard character {@code &#42;} or not.
     */
    public boolean isWildcardType() {
        return WILDCARD_TYPE.equals(type);
    }

    /**
     * Return the subtype.
     */
    public String getSubtype() {
        return this.subtype;
    }

    /**
     * Indicates whether the {@linkplain #getSubtype() subtype} is the wildcard character {@code &#42;}
     * or the wildcard character followed by a sufiix (e.g. {@code &#42;+xml}), or not.
     *
     * @return whether the subtype is {@code &#42;}
     */
    public boolean isWildcardSubtype() {
        return WILDCARD_TYPE.equals(subtype) || subtype.startsWith("*+");
    }

    /**
     * Indicates whether this media type is concrete, i.e. whether neither the type or subtype is a wildcard
     * character {@code &#42;}.
     *
     * @return whether this media type is concrete
     */
    public boolean isConcrete() {
        return !isWildcardType() && !isWildcardSubtype();
    }

    /**
     * Return the character set, as indicated by a {@code charset} parameter, if any.
     *
     * @return the character set; or {@code null} if not available
     */
    public Charset getCharSet() {
        String charSet = getParameter(PARAM_CHARSET);
        return (charSet != null ? Charset.forName(unquote(charSet)) : null);
    }

    /**
     * Return the quality value, as indicated by a {@code q} parameter, if any.
     * Defaults to {@code 1.0}.
     *
     * @return the quality factory
     */
    public double getQualityValue() {
        String qualityFactory = getParameter(PARAM_QUALITY_FACTOR);
        return (qualityFactory != null ? Double.parseDouble(unquote(qualityFactory)) : 1D);
    }

    /**
     * Return a generic parameter value, given a parameter name.
     *
     * @param name the parameter name
     *
     * @return the parameter value; or {@code null} if not present
     */
    public String getParameter(String name) {
        return this.parameters.get(name);
    }

    /**
     * Return all generic parameter values.
     *
     * @return a read-only map, possibly empty, never {@code null}
     */
    public Map<String, String> getParameters() {
        return parameters;
    }

    /**
     * Indicate whether this {@code MediaType} includes the given media type.
     * <p>For instance, {@code text/*} includes {@code text/plain} and {@code text/html}, and {@code application/*+xml}
     * includes {@code application/soap+xml}, etc. This method is <b>not</b> symmetric.
     *
     * @param other the reference media type with which to compare
     *
     * @return {@code true} if this media type includes the given media type; {@code false} otherwise
     */
    public boolean includes(MediaType other) {
        if (other == null) {
            return false;
        }
        if (this.isWildcardType()) {
            // */* includes anything
            return true;
        } else if (this.type.equals(other.type)) {
            if (this.subtype.equals(other.subtype)) {
                return true;
            }
            if (this.isWildcardSubtype()) {
                // wildcard with suffix, e.g. application/*+xml
                int thisPlusIdx = this.subtype.indexOf('+');
                if (thisPlusIdx == -1) {
                    return true;
                } else {
                    // application/*+xml includes application/soap+xml
                    int otherPlusIdx = other.subtype.indexOf('+');
                    if (otherPlusIdx != -1) {
                        String thisSubtypeNoSuffix = this.subtype.substring(0, thisPlusIdx);
                        String thisSubtypeSuffix = this.subtype.substring(thisPlusIdx + 1);
                        String otherSubtypeSuffix = other.subtype.substring(otherPlusIdx + 1);
                        if (thisSubtypeSuffix.equals(otherSubtypeSuffix) && WILDCARD_TYPE.equals(thisSubtypeNoSuffix)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     * Indicate whether this {@code MediaType} includes the given media type vaulue.
     *
     * @param other the reference media type value with which to compare
     *
     * @return {@code true} if this media type includes the given media type; {@code false} otherwise
     */
    public boolean includes(String other) {
        return !StringUtils.isEmpty(other) && includes(MediaType.parseMediaType(other));
    }

    /**
     * Indicate whether this {@code MediaType} is compatible with the given media type.
     * <p>For instance, {@code text/*} is compatible with {@code text/plain}, {@code text/html}, and vice versa.
     * In effect, this method is similar to {@link #includes(MediaType)}, except that it <b>is</b> symmetric.
     *
     * @param other the reference media type with which to compare
     *
     * @return {@code true} if this media type is compatible with the given media type; {@code false} otherwise
     */
    public boolean isCompatibleWith(MediaType other) {
        if (other == null) {
            return false;
        }
        if (isWildcardType() || other.isWildcardType()) {
            return true;
        } else if (this.type.equals(other.type)) {
            if (this.subtype.equals(other.subtype)) {
                return true;
            }
            // wildcard with suffix? e.g. application/*+xml
            if (this.isWildcardSubtype() || other.isWildcardSubtype()) {

                int thisPlusIdx = this.subtype.indexOf('+');
                int otherPlusIdx = other.subtype.indexOf('+');

                if (thisPlusIdx == -1 && otherPlusIdx == -1) {
                    return true;
                } else if (thisPlusIdx != -1 && otherPlusIdx != -1) {
                    String thisSubtypeNoSuffix = this.subtype.substring(0, thisPlusIdx);
                    String otherSubtypeNoSuffix = other.subtype.substring(0, otherPlusIdx);

                    String thisSubtypeSuffix = this.subtype.substring(thisPlusIdx + 1);
                    String otherSubtypeSuffix = other.subtype.substring(otherPlusIdx + 1);

                    if (thisSubtypeSuffix.equals(otherSubtypeSuffix) && (WILDCARD_TYPE.equals(thisSubtypeNoSuffix) ||
                            WILDCARD_TYPE.equals(otherSubtypeNoSuffix))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Returns a {@link Predicate} that defines {@link MediaType}s that are compatible with the given {@link MediaType}.
     * @param other the {@link MediaType} over which the returned {@link Predicate} closes.
     * @return a {@link Predicate} that defines {@link MediaType}s that are compatible with the given {@link MediaType}.
     */
    public static Predicate<MediaType> isCompatibleWithPredicate(final MediaType other) {
        return new Predicate<MediaType>() {

            @Override
            public boolean apply(@Nullable final MediaType mediaType) {
                return mediaType != null && mediaType.isCompatibleWith(other);
            }
        };
    }

    /**
     * Return a replica of this instance with the quality value of the given MediaType.
     *
     * @return the same instance if the given MediaType doesn't have a quality value, or a new one otherwise
     */
    public MediaType copyQualityValue(MediaType mediaType) {
        if (!mediaType.parameters.containsKey(PARAM_QUALITY_FACTOR)) {
            return this;
        }
        Map<String, String> params = new LinkedHashMap<String, String>(this.parameters);
        params.put(PARAM_QUALITY_FACTOR, mediaType.parameters.get(PARAM_QUALITY_FACTOR));
        return new MediaType(this, params);
    }

    /**
     * Return a replica of this instance with the given quality value.
     *
     * @return a new media type instance with the given quality value.
     */
    public MediaType withQualityValue(double qualityValue) {
        Map<String, String> params = new LinkedHashMap<>(this.parameters);
        params.put(PARAM_QUALITY_FACTOR, Double.toString(qualityValue));
        return new MediaType(this, params);
    }

    /**
     * Return a replica of this instance with its quality value removed.
     *
     * @return the same instance if the media type doesn't contain a quality value, or a new one otherwise
     */
    public MediaType removeQualityValue() {
        if (!this.parameters.containsKey(PARAM_QUALITY_FACTOR)) {
            return this;
        }
        Map<String, String> params = new LinkedHashMap<String, String>(this.parameters);
        params.remove(PARAM_QUALITY_FACTOR);
        return new MediaType(this, params);
    }

    /**
     * Compares this {@code MediaType} to another alphabetically.
     *
     * @param other media type to compare to
     *
     * @see #sortBySpecificity(List)
     */
    @Override
    public int compareTo(@Nonnull MediaType other) {
        int comp = this.type.compareToIgnoreCase(other.type);
        if (comp != 0) {
            return comp;
        }
        comp = this.subtype.compareToIgnoreCase(other.subtype);
        if (comp != 0) {
            return comp;
        }
        comp = this.parameters.size() - other.parameters.size();
        if (comp != 0) {
            return comp;
        }
        TreeSet<String> thisAttributes = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
        thisAttributes.addAll(this.parameters.keySet());
        TreeSet<String> otherAttributes = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
        otherAttributes.addAll(other.parameters.keySet());
        Iterator<String> thisAttributesIterator = thisAttributes.iterator();
        Iterator<String> otherAttributesIterator = otherAttributes.iterator();
        while (thisAttributesIterator.hasNext()) {
            String thisAttribute = thisAttributesIterator.next();
            String otherAttribute = otherAttributesIterator.next();
            comp = thisAttribute.compareToIgnoreCase(otherAttribute);
            if (comp != 0) {
                return comp;
            }
            String thisValue = this.parameters.get(thisAttribute);
            String otherValue = other.parameters.get(otherAttribute);
            if (otherValue == null) {
                otherValue = "";
            }
            comp = thisValue.compareTo(otherValue);
            if (comp != 0) {
                return comp;
            }
        }
        return 0;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof MediaType)) {
            return false;
        }
        MediaType otherType = (MediaType) other;
        return (this.type.equalsIgnoreCase(otherType.type) && this.subtype.equalsIgnoreCase(otherType.subtype) &&
                this.parameters.equals(otherType.parameters));
    }

    @Override
    public int hashCode() {
        int result = this.type.hashCode();
        result = 31 * result + this.subtype.hashCode();
        result = 31 * result + this.parameters.hashCode();
        return result;
    }

    @Override
    @JsonValue
    public String toString() {
        StringBuilder builder = new StringBuilder();
        appendTo(builder);
        return builder.toString();
    }

    private void appendTo(StringBuilder builder) {
        builder.append(this.type);
        builder.append('/');
        builder.append(this.subtype);
        appendTo(this.parameters, builder);
    }

    private void appendTo(Map<String, String> map, StringBuilder builder) {
        for (Map.Entry<String, String> entry : map.entrySet()) {
            builder.append(';');
            builder.append(entry.getKey());
            builder.append('=');
            builder.append(entry.getValue());
        }
    }

    public com.google.common.net.MediaType toGuavaMediaType() {
        return com.google.common.net.MediaType.parse(toString());
    }

    public static MediaType fromGuavaMediaType(com.google.common.net.MediaType guavaMediaType) {
        return MediaType.parseMediaType(guavaMediaType.toString());
    }

    /**
     * Parse the given String value into a {@code MediaType} object,
     * with this method name following the 'valueOf' naming convention
     *
     * @see #parseMediaType(String)
     */
    public static MediaType valueOf(String value) {
        return parseMediaType(value);
    }

    /**
     * Parse the given String into a single {@code MediaType}.
     *
     * @param mediaType the string to parse
     *
     * @return the media type
     * @throws InvalidMediaTypeException if the string cannot be parsed
     */
    @JsonCreator
    public static MediaType parseMediaType(String mediaType) {
        checkArgument(StringUtils.isNotEmpty(mediaType), "'mediaType' must not be empty");
        String[] parts = StringUtils.tokenizeToStringArray(mediaType, ";");

        String fullType = parts[0].trim();
        // java.net.HttpURLConnection returns a *; q=.2 Accept header
        if (WILDCARD_TYPE.equals(fullType)) {
            fullType = "*/*";
        }
        int subIndex = fullType.indexOf('/');
        if (subIndex == -1) {
            throw new InvalidMediaTypeException(mediaType, "does not contain '/'");
        }
        if (subIndex == fullType.length() - 1) {
            throw new InvalidMediaTypeException(mediaType, "does not contain subtype after '/'");
        }
        String type = fullType.substring(0, subIndex);
        String subtype = fullType.substring(subIndex + 1, fullType.length());
        if (WILDCARD_TYPE.equals(type) && !WILDCARD_TYPE.equals(subtype)) {
            throw new InvalidMediaTypeException(mediaType, "wildcard type is legal only in '*/*' (all media types)");
        }

        Map<String, String> parameters = null;
        if (parts.length > 1) {
            parameters = new LinkedHashMap<String, String>(parts.length - 1);
            for (int i = 1; i < parts.length; i++) {
                String parameter = parts[i];
                int eqIndex = parameter.indexOf('=');
                if (eqIndex != -1) {
                    String attribute = parameter.substring(0, eqIndex);
                    String value = parameter.substring(eqIndex + 1, parameter.length());
                    parameters.put(attribute, value);
                }
            }
        }

        try {
            return new MediaType(type, subtype, parameters);
        } catch (UnsupportedCharsetException ex) {
            throw new InvalidMediaTypeException(mediaType, "unsupported charset '" + ex.getCharsetName() + "'");
        } catch (IllegalArgumentException ex) {
            throw new InvalidMediaTypeException(mediaType, ex.getMessage());
        }
    }


    /**
     * Parse the given, comma-separated string into a list of {@code MediaType} objects.
     * <p>This method can be used to parse an Accept or Content-Type header.
     *
     * @param mediaTypes the string to parse
     *
     * @return the list of media types
     * @throws IllegalArgumentException if the string cannot be parsed
     */
    public static List<MediaType> parseMediaTypes(String mediaTypes) {
        if (StringUtils.isEmpty(mediaTypes)) {
            return Collections.emptyList();
        }
        String[] tokens = mediaTypes.split(",\\s*");
        List<MediaType> result = new ArrayList<MediaType>(tokens.length);
        for (String token : tokens) {
            result.add(parseMediaType(token));
        }
        return result;
    }

    /**
     * Return a string representation of the given list of {@code MediaType} objects.
     * <p>This method can be used to for an {@code Accept} or {@code Content-Type} header.
     *
     * @param mediaTypes the string to parse
     *
     * @return the list of media types
     * @throws IllegalArgumentException if the String cannot be parsed
     */
    public static String toString(Collection<MediaType> mediaTypes) {
        StringBuilder builder = new StringBuilder();
        for (Iterator<MediaType> iterator = mediaTypes.iterator(); iterator.hasNext(); ) {
            MediaType mediaType = iterator.next();
            mediaType.appendTo(builder);
            if (iterator.hasNext()) {
                builder.append(", ");
            }
        }
        return builder.toString();
    }

    /**
     * Sorts the given list of {@code MediaType} objects by specificity.
     * <p>Given two media types:
     * <ol>
     * <li>if either media type has a {@linkplain #isWildcardType() wildcard type}, then the media type without the
     * wildcard is ordered before the other.</li>
     * <li>if the two media types have different {@linkplain #getType() types}, then they are considered equal and
     * remain their current order.</li>
     * <li>if either media type has a {@linkplain #isWildcardSubtype() wildcard subtype}, then the media type without
     * the wildcard is sorted before the other.</li>
     * <li>if the two media types have different {@linkplain #getSubtype() subtypes}, then they are considered equal
     * and remain their current order.</li>
     * <li>if the two media types have different {@linkplain #getQualityValue() quality value}, then the media type
     * with the highest quality value is ordered before the other.</li>
     * <li>if the two media types have a different amount of {@linkplain #getParameter(String) parameters}, then the
     * media type with the most parameters is ordered before the other.</li>
     * </ol>
     * <p>For example:
     * <blockquote>audio/basic &lt; audio/* &lt; *&#047;*</blockquote>
     * <blockquote>audio/* &lt; audio/*;q=0.7; audio/*;q=0.3</blockquote>
     * <blockquote>audio/basic;level=1 &lt; audio/basic</blockquote>
     * <blockquote>audio/basic == text/html</blockquote>
     * <blockquote>audio/basic == audio/wave</blockquote>
     *
     * @param mediaTypes the list of media types to be sorted
     *
     * @see <a href="http://tools.ietf.org/html/rfc2616#section-14.1">HTTP 1.1, section 14.1</a>
     */
    public static void sortBySpecificity(List<MediaType> mediaTypes) {
        checkArgument(mediaTypes != null, "'mediaTypes' must not be null");
        if (mediaTypes.size() > 1) {
            Collections.sort(mediaTypes, SPECIFICITY_COMPARATOR);
        }
    }

    /**
     * Sorts the given list of {@code MediaType} objects by quality value.
     * <p>Given two media types:
     * <ol>
     * <li>if the two media types have different {@linkplain #getQualityValue() quality value}, then the media type
     * with the highest quality value is ordered before the other.</li>
     * <li>if either media type has a {@linkplain #isWildcardType() wildcard type}, then the media type without the
     * wildcard is ordered before the other.</li>
     * <li>if the two media types have different {@linkplain #getType() types}, then they are considered equal and
     * remain their current order.</li>
     * <li>if either media type has a {@linkplain #isWildcardSubtype() wildcard subtype}, then the media type without
     * the wildcard is sorted before the other.</li>
     * <li>if the two media types have different {@linkplain #getSubtype() subtypes}, then they are considered equal
     * and remain their current order.</li>
     * <li>if the two media types have a different amount of {@linkplain #getParameter(String) parameters}, then the
     * media type with the most parameters is ordered before the other.</li>
     * </ol>
     *
     * @param mediaTypes the list of media types to be sorted
     * @return the list of media types, sorted by quality descending.
     *
     * @see #getQualityValue()
     */
    public static List<MediaType> sortByQualityValue(List<MediaType> mediaTypes) {
        checkArgument(mediaTypes != null, "'mediaTypes' must not be null");
        if (mediaTypes.size() > 1) {
            List<MediaType> sorted = new ArrayList<>(mediaTypes);
            Collections.sort(sorted, QUALITY_VALUE_COMPARATOR);
            return sorted;
        }
        return mediaTypes;
    }

    /**
     * Sorts the given list of {@code MediaType} objects by specificity as the
     * primary criteria and quality value the secondary.
     *
     * @see MediaType#sortBySpecificity(List)
     * @see MediaType#sortByQualityValue(List)
     */
    public static void sortBySpecificityAndQuality(List<MediaType> mediaTypes) {
        checkArgument(mediaTypes != null, "'mediaTypes' must not be null");
        if (mediaTypes.size() > 1) {
            Collections.sort(mediaTypes, Ordering.compound(List.of(MediaType.SPECIFICITY_COMPARATOR, MediaType.QUALITY_VALUE_COMPARATOR)));
        }
    }


    /**
     * Comparator used by {@link #sortBySpecificity(List)}.
     */
    public static final Comparator<MediaType> SPECIFICITY_COMPARATOR = new Comparator<MediaType>() {

        public int compare(MediaType mediaType1, MediaType mediaType2) {
            if (mediaType1.isWildcardType() && !mediaType2.isWildcardType()) { // */* < audio/*
                return 1;
            } else if (mediaType2.isWildcardType() && !mediaType1.isWildcardType()) { // audio/* > */*
                return -1;
            } else if (!mediaType1.getType().equals(mediaType2.getType())) { // audio/basic == text/html
                return 0;
            } else { // mediaType1.getType().equals(mediaType2.getType())
                if (mediaType1.isWildcardSubtype() && !mediaType2.isWildcardSubtype()) { // audio/* < audio/basic
                    return 1;
                } else if (mediaType2.isWildcardSubtype() && !mediaType1.isWildcardSubtype()) { // audio/basic > audio/*
                    return -1;
                } else if (!mediaType1.getSubtype().equals(mediaType2.getSubtype())) { // audio/basic == audio/wave
                    return 0;
                } else { // mediaType2.getSubtype().equals(mediaType2.getSubtype())
                    double quality1 = mediaType1.getQualityValue();
                    double quality2 = mediaType2.getQualityValue();
                    int qualityComparison = Double.compare(quality2, quality1);
                    if (qualityComparison != 0) {
                        return qualityComparison;  // audio/*;q=0.7 < audio/*;q=0.3
                    } else {
                        int paramsSize1 = mediaType1.parameters.size();
                        int paramsSize2 = mediaType2.parameters.size();
                        return (paramsSize2 < paramsSize1 ? -1 : (paramsSize2 == paramsSize1 ? 0 : 1)); //
                        // audio/basic;level=1 < audio/basic
                    }
                }
            }
        }
    };


    /**
     * Comparator used by {@link #sortByQualityValue(List)}.
     */
    public static final Comparator<MediaType> QUALITY_VALUE_COMPARATOR = new Comparator<MediaType>() {

        public int compare(MediaType mediaType1, MediaType mediaType2) {
            double quality1 = mediaType1.getQualityValue();
            double quality2 = mediaType2.getQualityValue();
            int qualityComparison = Double.compare(quality2, quality1);
            if (qualityComparison != 0) {
                return qualityComparison;  // audio/*;q=0.7 < audio/*;q=0.3
            } else if (mediaType1.isWildcardType() && !mediaType2.isWildcardType()) { // */* < audio/*
                return 1;
            } else if (mediaType2.isWildcardType() && !mediaType1.isWildcardType()) { // audio/* > */*
                return -1;
            } else if (!mediaType1.getType().equals(mediaType2.getType())) { // audio/basic == text/html
                return 0;
            } else { // mediaType1.getType().equals(mediaType2.getType())
                if (mediaType1.isWildcardSubtype() && !mediaType2.isWildcardSubtype()) { // audio/* < audio/basic
                    return 1;
                } else if (mediaType2.isWildcardSubtype() && !mediaType1.isWildcardSubtype()) { // audio/basic > audio/*
                    return -1;
                } else if (!mediaType1.getSubtype().equals(mediaType2.getSubtype())) { // audio/basic == audio/wave
                    return 0;
                } else {
                    int paramsSize1 = mediaType1.parameters.size();
                    int paramsSize2 = mediaType2.parameters.size();
                    return (paramsSize2 < paramsSize1 ? -1 : (paramsSize2 == paramsSize1 ? 0 : 1)); // audio/basic;
                    // level=1 < audio/basic
                }
            }
        }
    };

}
