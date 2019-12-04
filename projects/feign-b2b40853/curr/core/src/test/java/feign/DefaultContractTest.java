/*
 * Copyright 2013 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package feign;

import com.google.gson.reflect.TypeToken;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.net.URI;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static feign.assertj.FeignAssertions.assertThat;
import static java.util.Arrays.asList;
import static org.assertj.core.data.MapEntry.entry;

/**
 * Tests interfaces defined per {@link Contract.Default} are interpreted into expected {@link feign
 * .RequestTemplate template} instances.
 */
public class DefaultContractTest {

  @Rule
  public final ExpectedException thrown = ExpectedException.none();

  Contract.Default contract = new Contract.Default();

  @Test
  public void httpMethods() throws Exception {
    assertThat(parseAndValidateMetadata(Methods.class, "post").template())
        .hasMethod("POST");

    assertThat(parseAndValidateMetadata(Methods.class, "put").template())
        .hasMethod("PUT");

    assertThat(parseAndValidateMetadata(Methods.class, "get").template())
        .hasMethod("GET");

    assertThat(parseAndValidateMetadata(Methods.class, "delete").template())
        .hasMethod("DELETE");
  }

  @Test
  public void bodyParamIsGeneric() throws Exception {
    MethodMetadata md = parseAndValidateMetadata(BodyParams.class, "post", List.class);

    assertThat(md.bodyIndex())
        .isEqualTo(0);
    assertThat(md.bodyType())
        .isEqualTo(new TypeToken<List<String>>() {
        }.getType());
  }

  @Test
  public void tooManyBodies() throws Exception {
    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("Method has too many Body");
    parseAndValidateMetadata(BodyParams.class, "tooMany", List.class, List.class);
  }

  @Test
  public void customMethodWithoutPath() throws Exception {
    assertThat(parseAndValidateMetadata(CustomMethod.class, "patch").template())
        .hasMethod("PATCH")
        .hasUrl("");
  }

  @Test
  public void queryParamsInPathExtract() throws Exception {
    assertThat(parseAndValidateMetadata(WithQueryParamsInPath.class, "none").template())
        .hasUrl("/")
        .hasQueries();

    assertThat(parseAndValidateMetadata(WithQueryParamsInPath.class, "one").template())
        .hasUrl("/")
        .hasQueries(
            entry("Action", asList("GetUser"))
        );

    assertThat(parseAndValidateMetadata(WithQueryParamsInPath.class, "two").template())
        .hasUrl("/")
        .hasQueries(
            entry("Action", asList("GetUser")),
            entry("Version", asList("2010-05-08"))
        );

    assertThat(parseAndValidateMetadata(WithQueryParamsInPath.class, "three").template())
        .hasUrl("/")
        .hasQueries(
            entry("Action", asList("GetUser")),
            entry("Version", asList("2010-05-08")),
            entry("limit", asList("1"))
        );

    assertThat(parseAndValidateMetadata(WithQueryParamsInPath.class, "twoAndOneEmpty").template())
        .hasUrl("/")
        .hasQueries(
            entry("flag", asList(new String[]{null})),
            entry("Action", asList("GetUser")),
            entry("Version", asList("2010-05-08"))
        );

    assertThat(parseAndValidateMetadata(WithQueryParamsInPath.class, "oneEmpty").template())
        .hasUrl("/")
        .hasQueries(
            entry("flag", asList(new String[]{null}))
        );

    assertThat(parseAndValidateMetadata(WithQueryParamsInPath.class, "twoEmpty").template())
        .hasUrl("/")
        .hasQueries(
            entry("flag", asList(new String[]{null})),
            entry("NoErrors", asList(new String[]{null}))
        ); 
  }

  @Test
  public void bodyWithoutParameters() throws Exception {
    MethodMetadata md = parseAndValidateMetadata(BodyWithoutParameters.class, "post");

    assertThat(md.template())
        .hasBody("<v01:getAccountsListOfUser/>");
  }

  @Test
  public void headersOnMethodAddsContentTypeHeader() throws Exception {
    MethodMetadata md = parseAndValidateMetadata(BodyWithoutParameters.class, "post");

    assertThat(md.template())
        .hasHeaders(
            entry("Content-Type", asList("application/xml")),
            entry("Content-Length", asList(String.valueOf(md.template().body().length)))
        );
  }

  @Test
  public void headersOnTypeAddsContentTypeHeader() throws Exception {
    MethodMetadata md = parseAndValidateMetadata(HeadersOnType.class, "post");

    assertThat(md.template())
        .hasHeaders(
            entry("Content-Type", asList("application/xml")),
            entry("Content-Length", asList(String.valueOf(md.template().body().length)))
        );
  }

  @Test
  public void withPathAndURIParam() throws Exception {
    MethodMetadata md = parseAndValidateMetadata(WithURIParam.class,
                                                 "uriParam", String.class, URI.class, String.class);

    assertThat(md.indexToName())
        .containsExactly(
            entry(0, asList("1")),
            // Skips 1 as it is a url index!
            entry(2, asList("2"))
        );

    assertThat(md.urlIndex()).isEqualTo(1);
  }

  @Test
  public void pathAndQueryParams() throws Exception {
    MethodMetadata md = parseAndValidateMetadata(WithPathAndQueryParams.class,
                                                 "recordsByNameAndType", int.class, String.class,
                                                 String.class);

    assertThat(md.template())
        .hasQueries(entry("name", asList("{name}")), entry("type", asList("{type}")));

    assertThat(md.indexToName()).containsExactly(
        entry(0, asList("domainId")),
        entry(1, asList("name")),
        entry(2, asList("type"))
    );
  }

  @Test
  public void bodyWithTemplate() throws Exception {
    MethodMetadata md = parseAndValidateMetadata(FormParams.class,
                                                 "login", String.class, String.class, String.class);

    assertThat(md.template())
        .hasBodyTemplate(
            "%7B\"customer_name\": \"{customer_name}\", \"user_name\": \"{user_name}\", \"password\": \"{password}\"%7D");
  }

  @Test
  public void formParamsParseIntoIndexToName() throws Exception {
    MethodMetadata md = parseAndValidateMetadata(FormParams.class,
                                                 "login", String.class, String.class, String.class);

    assertThat(md.formParams())
        .containsExactly("customer_name", "user_name", "password");

    assertThat(md.indexToName()).containsExactly(
        entry(0, asList("customer_name")),
        entry(1, asList("user_name")),
        entry(2, asList("password"))
    );
  }

  /**
   * Body type is only for the body param.
   */
  @Test
  public void formParamsDoesNotSetBodyType() throws Exception {
    MethodMetadata md = parseAndValidateMetadata(FormParams.class,
                                                 "login", String.class, String.class, String.class);

    assertThat(md.bodyType()).isNull();
  }

  @Test
  public void headerParamsParseIntoIndexToName() throws Exception {
    MethodMetadata md = parseAndValidateMetadata(HeaderParams.class, "logout", String.class);

    assertThat(md.template())
        .hasHeaders(entry("Auth-Token", asList("{authToken}", "Foo")));

    assertThat(md.indexToName())
        .containsExactly(entry(0, asList("authToken")));
  }

  @Test
  public void customExpander() throws Exception {
    MethodMetadata md = parseAndValidateMetadata(CustomExpander.class, "date", Date.class);

    assertThat(md.indexToExpanderClass())
        .containsExactly(entry(0, DateToMillis.class));
  }

  @Test
  public void slashAreEncodedWhenNeeded() throws Exception {
    MethodMetadata md = parseAndValidateMetadata(SlashNeedToBeEncoded.class,
                                                 "getQueues", String.class);

    assertThat(md.template().decodeSlash()).isFalse();

    md = parseAndValidateMetadata(SlashNeedToBeEncoded.class, "getZone", String.class);

    assertThat(md.template().decodeSlash()).isTrue();
  }

  interface Methods {

    @RequestLine("POST /")
    void post();

    @RequestLine("PUT /")
    void put();

    @RequestLine("GET /")
    void get();

    @RequestLine("DELETE /")
    void delete();
  }

  interface BodyParams {

    @RequestLine("POST")
    Response post(List<String> body);

    @RequestLine("POST")
    Response tooMany(List<String> body, List<String> body2);
  }

  interface CustomMethod {

    @RequestLine("PATCH")
    Response patch();
  }

  interface WithQueryParamsInPath {

    @RequestLine("GET /")
    Response none();

    @RequestLine("GET /?Action=GetUser")
    Response one();

    @RequestLine("GET /?Action=GetUser&Version=2010-05-08")
    Response two();

    @RequestLine("GET /?Action=GetUser&Version=2010-05-08&limit=1")
    Response three();

    @RequestLine("GET /?flag&Action=GetUser&Version=2010-05-08")
    Response twoAndOneEmpty();

    @RequestLine("GET /?flag")
    Response oneEmpty();

    @RequestLine("GET /?flag&NoErrors")
    Response twoEmpty();
  }

  interface BodyWithoutParameters {

    @RequestLine("POST /")
    @Headers("Content-Type: application/xml")
    @Body("<v01:getAccountsListOfUser/>")
    Response post();
  }

  @Headers("Content-Type: application/xml")
  interface HeadersOnType {

    @RequestLine("POST /")
    @Body("<v01:getAccountsListOfUser/>")
    Response post();
  }

  interface WithURIParam {

    @RequestLine("GET /{1}/{2}")
    Response uriParam(@Param("1") String one, URI endpoint, @Param("2") String two);
  }

  interface WithPathAndQueryParams {

    @RequestLine("GET /domains/{domainId}/records?name={name}&type={type}")
    Response recordsByNameAndType(@Param("domainId") int id, @Param("name") String nameFilter,
                                  @Param("type") String typeFilter);
  }

  interface FormParams {

    @RequestLine("POST /")
    @Body("%7B\"customer_name\": \"{customer_name}\", \"user_name\": \"{user_name}\", \"password\": \"{password}\"%7D")
    void login(
        @Param("customer_name") String customer,
        @Param("user_name") String user, @Param("password") String password);
  }

  interface HeaderParams {

    @RequestLine("POST /")
    @Headers({"Auth-Token: {authToken}", "Auth-Token: Foo"})
    void logout(@Param("authToken") String token);
  }

  interface CustomExpander {

    @RequestLine("POST /?date={date}")
    void date(@Param(value = "date", expander = DateToMillis.class) Date date);
  }

  class DateToMillis implements Param.Expander {

    @Override
    public String expand(Object value) {
      return String.valueOf(((Date) value).getTime());
    }
  }

  interface SlashNeedToBeEncoded {
    @RequestLine(value = "GET /api/queues/{vhost}", decodeSlash = false)
    String getQueues(@Param("vhost") String vhost);

    @RequestLine("GET /api/{zoneId}")
    String getZone(@Param("ZoneId") String vhost);
  }

  @Headers("Foo: Bar")
  interface SimpleParameterizedBaseApi<M> {

    @RequestLine("GET /api/{zoneId}")
    M get(@Param("key") String key);
  }

  interface SimpleParameterizedApi extends SimpleParameterizedBaseApi<String> {

  }

  @Test
  public void simpleParameterizedBaseApi() throws Exception {
    List<MethodMetadata> md = contract.parseAndValidatateMetadata(SimpleParameterizedApi.class);

    assertThat(md).hasSize(1);

    assertThat(md.get(0).configKey())
        .isEqualTo("SimpleParameterizedApi#get(String)");
    assertThat(md.get(0).returnType())
        .isEqualTo(String.class);
    assertThat(md.get(0).template())
        .hasHeaders(entry("Foo", asList("Bar")));
  }

  @Test
  public void parameterizedApiUnsupported() throws Exception {
    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("Parameterized types unsupported: SimpleParameterizedBaseApi");
    contract.parseAndValidatateMetadata(SimpleParameterizedBaseApi.class);
  }

  interface OverrideParameterizedApi extends SimpleParameterizedBaseApi<String> {

    @Override
    @RequestLine("GET /api/{zoneId}")
    String get(@Param("key") String key);
  }

  @Test
  public void overrideBaseApiUnsupported() throws Exception {
    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("Overrides unsupported: OverrideParameterizedApi#get(String)");
    contract.parseAndValidatateMetadata(OverrideParameterizedApi.class);
  }

  interface Child<T> extends SimpleParameterizedBaseApi<List<T>> {

  }

  interface GrandChild extends Child<String> {

  }

  @Test
  public void onlySingleLevelInheritanceSupported() throws Exception {
    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("Only single-level inheritance supported: GrandChild");
    contract.parseAndValidatateMetadata(GrandChild.class);
  }

  @Headers("Foo: Bar")
  interface ParameterizedBaseApi<K, M> {

    @RequestLine("GET /api/{key}")
    Entity<K, M> get(@Param("key") K key);

    @RequestLine("POST /api")
    Entities<K, M> getAll(Keys<K> keys);
  }

  static class Keys<K> {

    List<K> keys;
  }

  static class Entity<K, M> {

    K key;
    M model;
  }

  static class Entities<K, M> {

    private List<Entity<K, M>> entities;
  }

  @Headers("Version: 1")
  interface ParameterizedApi extends ParameterizedBaseApi<String, Long> {

  }

  @Test
  public void parameterizedBaseApi() throws Exception {
    List<MethodMetadata> md = contract.parseAndValidatateMetadata(ParameterizedApi.class);

    Map<String, MethodMetadata> byConfigKey = new LinkedHashMap<String, MethodMetadata>();
    for (MethodMetadata m : md) {
      byConfigKey.put(m.configKey(), m);
    }

    assertThat(byConfigKey)
        .containsOnlyKeys("ParameterizedApi#get(String)", "ParameterizedApi#getAll(Keys)");

    assertThat(byConfigKey.get("ParameterizedApi#get(String)").returnType())
        .isEqualTo(new TypeToken<Entity<String, Long>>() {
        }.getType());
    assertThat(byConfigKey.get("ParameterizedApi#get(String)").template()).hasHeaders(
        entry("Version", asList("1")),
        entry("Foo", asList("Bar"))
    );

    assertThat(byConfigKey.get("ParameterizedApi#getAll(Keys)").returnType())
        .isEqualTo(new TypeToken<Entities<String, Long>>() {
        }.getType());
    assertThat(byConfigKey.get("ParameterizedApi#getAll(Keys)").bodyType())
        .isEqualTo(new TypeToken<Keys<String>>() {
        }.getType());
    assertThat(byConfigKey.get("ParameterizedApi#getAll(Keys)").template()).hasHeaders(
        entry("Version", asList("1")),
        entry("Foo", asList("Bar"))
    );
  }

  private MethodMetadata parseAndValidateMetadata(Class<?> targetType, String method,
                                                  Class<?>... parameterTypes)
      throws NoSuchMethodException {
    return contract.parseAndValidateMetadata(targetType,
                                             targetType.getMethod(method, parameterTypes));
  }
}
