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

package loi.cp.bootstrap;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.learningobjects.cpxp.BaseWebContext;
import com.learningobjects.cpxp.Id;
import com.learningobjects.cpxp.component.ComponentEnvironment;
import com.learningobjects.cpxp.component.ComponentInstance;
import com.learningobjects.cpxp.component.ComponentSupport;
import com.learningobjects.cpxp.component.eval.AbstractEvaluator;
import com.learningobjects.cpxp.component.function.AbstractFunctionInstance;
import com.learningobjects.cpxp.component.function.FunctionBinding;
import com.learningobjects.cpxp.component.internal.FunctionDescriptor;
import com.learningobjects.cpxp.component.web.Method;
import com.learningobjects.cpxp.component.web.WebRequest;
import com.learningobjects.cpxp.component.web.WebRequestFactory;
import com.learningobjects.cpxp.controller.upload.UploadInfo;
import com.learningobjects.cpxp.util.lang.OptionLike;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import scala.Tuple2;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@FunctionBinding(
  registry = BootstrapFunctionRegistry.class,
  annotations = Bootstrap.class
)
public class BootstrapInstance extends AbstractFunctionInstance {
    private static final Logger logger = Logger.getLogger(BootstrapInstance.class.getName());

    private ScopeEvaluator _scopeEvaluator;
    private Type _param;
    private int _nargs;
    private JavaType _type;
    private ObjectMapper _mapper;

    @Override
    public void init(ComponentInstance instance, FunctionDescriptor function) {
        super.init(instance, function);

        _mapper = ComponentSupport.getObjectMapper().copy();
        _mapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
        _mapper.registerModule(new UploadModule());

        Type[] types = function.getMethod().getGenericParameterTypes();
        _nargs = types.length;
        _param = (_nargs == 0) ? null : types[_nargs - 1];
        if ((_param != null) && (_param != JsonNode.class)) {
            _type = _mapper.getTypeFactory().constructType(_param);
        }
        if (_nargs >= 2) {
            _scopeEvaluator = new ScopeEvaluator();
            _scopeEvaluator.init(null, null, types[0], function.getMethod().getParameterAnnotations()[0]);
        }
    }

    static class ScopeEvaluator extends AbstractEvaluator {
        public Object getScope(Long scope) {
            return getItem(scope, null);
        }

        @Override
        public boolean isStateless() {
            return false;
        }
    }

    public boolean hasCollectionParameter() {
        return (_param != null) && ((_type == null) || _type.isArrayType() || _type.isCollectionLikeType());
    }

    public Long invoke(Long scope, JsonNode config) throws Exception {
        List<Object> argl = new ArrayList<>();
        if (_scopeEvaluator != null) {
            argl.add(_scopeEvaluator.getScope(scope));
        }
        if (_nargs >= 1) {
            // I could coerce non-array to array or list but...
            argl.add((_type == null) ? config : fromJson(config, _type));
        }
        Object[] args = argl.toArray(new Object[argl.size()]);
        Object object = super.invoke(getObject(), args);
        if ((object != null) && OptionLike.isOptionLike(object.getClass())) {
            object = OptionLike.getOrNull(object);
        }
        if (object == null) {
            return null;
        } else if (object instanceof Id) {
            return ((Id) object).getId();
        } else {
            return (Long) object;
        }
    }

    private static final Pattern SRS_BOOTSTRAP = Pattern.compile("^([A-Z]+) (/.*)$");

    public static BootstrapInstance lookup(String name) {
        Matcher matcher = SRS_BOOTSTRAP.matcher(name);
        if (matcher.matches()) {
            WebRequest webRequest = ComponentSupport.lookupService(WebRequestFactory.class)
              .create(Method.valueOf(matcher.group(1)), matcher.group(2));
            ComponentEnvironment env = BaseWebContext.getContext().getComponentEnvironment();
            FunctionDescriptor function = env.getRegistry().lookupFunction(BootstrapInstance.class, "srs");
            ComponentInstance instance = function.getDelegate().getComponent().getInstance(null, null, webRequest);
            return instance.getFunctionInstance(BootstrapInstance.class, function);
        } else {
            ComponentEnvironment env = BaseWebContext.getContext().getComponentEnvironment();
            FunctionDescriptor function = env.getRegistry().lookupFunction(BootstrapInstance.class, name);
            if (function == null) {
                return null;
            }
            ComponentInstance instance = function.getDelegate().getComponent().getInstance(null, null);
            return instance.getFunctionInstance(BootstrapInstance.class, function);
        }
    }

    public <T> T fromJson(JsonNode node, Class<T> type) throws IOException {
        return _mapper.readValue(_mapper.treeAsTokens(node), type);
    }

    public <T> T fromJson(JsonNode node, JavaType type) throws IOException {
        return _mapper.readValue(_mapper.treeAsTokens(node), type);
    }

    private static ThreadLocal<Tuple2<String, String>> __s3 = new ThreadLocal<>();

    public static void initS3(String identity, String credential) {
        __s3.set(Tuple2.apply(identity, credential));
    }

    public static void clearS3() {
        __s3.remove();
    }

    class UploadModule extends SimpleModule {
        public UploadModule() {
            super("UploadModule", new Version(1, 0, 0, null, null, null));
            addDeserializer(UploadInfo.class, new UploadDeserializer());
        }
    }

    class UploadDeserializer extends JsonDeserializer<UploadInfo> {
        @Override
        public UploadInfo deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
            JsonNode json = _mapper.readTree(jp);
            return download(json);
        }
    }

    public static UploadInfo download(String url) throws IOException {
        return download(ComponentSupport.getObjectMapper().getNodeFactory().textNode(url));
    }

    public static UploadInfo download(JsonNode json) throws IOException {
        String href = json.isTextual() ? json.textValue() : json.path("url").textValue();
        logger.log(Level.INFO, "Downloading file, {0}", href);
        URI uri;
        try {
            uri = new URI(href);
        } catch (Exception ex) {
            throw new IOException("URI error", ex);
        }
        File file = File.createTempFile("bootstrap", "tmp");
        file.deleteOnExit();
        if (uri.getScheme().equals("s3")) {
            JsonNode idNode = json.path("identity");
            Tuple2<String, String> s3Credentials = __s3.get();
            String identity = idNode.isMissingNode() ? s3Credentials._1() : idNode.textValue();
            JsonNode credNode = json.path("credential");
            String credential = credNode.isMissingNode() ? s3Credentials._2() : credNode.textValue();
            var credentials = AwsBasicCredentials.create(identity, credential);
            try (
              var s3 =
                S3Client.builder()
                  .credentialsProvider(StaticCredentialsProvider.create(credentials))
                  .region(Region.US_EAST_1)
                  .build()
            ) {
                var getObject =
                  GetObjectRequest.builder()
                    .bucket(uri.getHost())
                    .key(uri.getPath().substring(1))
                    .build();
                int tries = 0;
                boolean done = false;
                do {
                    try {
                        file.delete(); // S3 cannot abide
                        var result = s3.getObject(getObject, file.toPath());
                        done = true;
                        if (result == null) {
                            throw new IOException("Error (null) retrieving S3 blob: " + uri.getHost() + " / " + uri.getPath() + "#" + uri.getFragment());
                        }
                    } catch (Exception ex) {
                        if (done || (++tries >= 4)) {
                            throw ex;
                        }
                        logger.log(Level.INFO, "Download error ({1}), retrying... {0}", new Object[]{ex.getMessage(), uri.toString()});
                        try {
                            Thread.sleep(tries * tries * 1000L); // 1s 4s 9s
                        } catch (Exception ignored) {
                        }
                    }
                } while (!done);
            }
        } else {
            URLConnection conn = uri.toURL().openConnection();
            if (json.has("username") && json.has("password")) {
                String username = json.path("username").textValue();
                String password = json.path("password").textValue();
                String auth = username + ":" + password;
                String encoded = Base64.encodeBase64String(auth.getBytes("UTF-8"));
                conn.setRequestProperty("Authorization", "Basic " + encoded);
            }
            try (InputStream in = conn.getInputStream()) {
                FileUtils.copyInputStreamToFile(in, file);
            }
        }
        String name = FilenameUtils.getName(uri.getPath());
        return new UploadInfo(name, "application/unknown", file, true);
    }
}
