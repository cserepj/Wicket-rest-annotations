/**
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.wicketstuff.rest.testJsonRequest;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.apache.wicket.authroles.authorization.strategies.role.IRoleCheckingStrategy;
import org.apache.wicket.util.lang.Args;
import org.wicketstuff.rest.Person;
import org.wicketstuff.rest.annotations.AuthorizeInvocation;
import org.wicketstuff.rest.annotations.MethodMapping;
import org.wicketstuff.rest.annotations.parameters.CookieParam;
import org.wicketstuff.rest.annotations.parameters.HeaderParam;
import org.wicketstuff.rest.annotations.parameters.MatrixParam;
import org.wicketstuff.rest.annotations.parameters.RequestBody;
import org.wicketstuff.rest.annotations.parameters.RequestParam;
import org.wicketstuff.rest.resource.AbstractRestResource;
import org.wicketstuff.rest.utils.HttpMethod;

public class RestResourceFullAnnotated extends AbstractRestResource<TestJsonDesSer> {
	public RestResourceFullAnnotated(TestJsonDesSer jsonSerialDeserial,
			IRoleCheckingStrategy roleCheckingStrategy) {
		super(jsonSerialDeserial, roleCheckingStrategy);
	}

	public RestResourceFullAnnotated(TestJsonDesSer jsonSerialDeserial) {
		super(jsonSerialDeserial);
	}

	/**
	 * Method for GET requests and URLs like '<resource path>/5'. The id
	 * parameter is automatically extracted from URL.
	 */
	@MethodMapping("/{id}")
	public int testMethodInt(int id) {
		return id;
	}

	/**
	 * Method invoked for GET requests and URLs like '<resource path>/5' The id
	 * parameter is automatically extracted from URL The person parameter is
	 * automatically deserialized from request body (which is JSON) The returned
	 * object is automatically serialized to JSON and written in the response
	 */
	@MethodMapping(value = "/{id}", httpMethod = HttpMethod.POST)
	public Person testMethodPostComplex(int id, @RequestBody Person person) {
		return person;
	}

	@MethodMapping(value = "/boolean/{boolean}", httpMethod = HttpMethod.GET)
	public String testMethodPostBoolean(boolean value) {
		return "testMethodPostBoolean:" + value;
	}

	@MethodMapping(value = "/monoseg", httpMethod = HttpMethod.POST)
	public String testMethodPostSegFixed() {
		return "testMethodPostSegFixed";
	}

	@MethodMapping("/")
	public String testMethodNoArgs() {
		return "testMethodNoArgs";
	}

	@MethodMapping(value = "/", httpMethod = HttpMethod.POST)
	public Person testMethodPost() {
		Person person = createTestPerson();
		return person;
	}

	@MethodMapping(value = "/admin", httpMethod = HttpMethod.GET)
	@AuthorizeInvocation("ROLE_ADMIN")
	public void testMethodAdminAuth() {

	}

	@MethodMapping(value = "/products/{id}")
	public String testMethodGetParameter(int productId, @RequestParam("price") float prodPrice) {
		Args.notNull(productId, "productId");
		Args.notNull(prodPrice, "price");

		return "testMethodGetParameter";
	}

	@MethodMapping(value = "/book/{id}")
	public String testMethodHeaderParameter(int productId, @HeaderParam("price") float prodPrice) {
		Args.notNull(productId, "productId");
		Args.notNull(prodPrice, "price");

		return "testMethodHeaderParameter";
	}

	@MethodMapping(value = "/person/{id}", httpMethod = HttpMethod.POST)
	public String testMethodCookieParameter(@CookieParam("name") String name, int id,
			@MatrixParam(segmentIndex = 1, variableName = "height") float height) {
		Args.notNull(id, "id");
		Args.notNull(name, "name");
		Args.notNull(height, "name");

		return "testMethodCookieParameter:" + id + name;
	}

	@MethodMapping(value = "/book/{id}", httpMethod = HttpMethod.POST)
	public String testPostRequestParameter(int productId, @RequestParam("title") String title) {
		Args.notNull(productId, "productId");
		Args.notNull(title, "title");

		return "testPostRequestParameter";
	}

	@MethodMapping(value = "/param/{id}/annotated/{name}", httpMethod = HttpMethod.POST)
	public String testAnnotatedParameters(int id, @TestAnnotation String name,
			@TestAnnotation @RequestParam("title") String title) {
		Args.notNull(id, "id");
		Args.notNull(name, "name");
		Args.notNull(title, "title");

		return "testAnnotatedParameters";
	}

	@MethodMapping(value = "/test/with/headerparams", httpMethod = HttpMethod.POST)
	public String testHeaderParams(@HeaderParam("credential") String credential) {
		Args.notNull(credential, "credential");
		
		return "testHeaderParams";
	}
	
	public static Person createTestPerson() {
		return new Person("Mary", "Smith", "m.smith@gmail.com");
	}

	@Override
	protected String serializeObjToString(Object result, TestJsonDesSer jsonSerialDeserial) {
		if (Person.class.isInstance(result))
			return TestJsonDesSer.getJSON();
		return result.toString();
	}

	@Override
	protected Object deserializeObjFromString(Class argClass, String json,
			TestJsonDesSer jsonSerialDeserial) {
		return TestJsonDesSer.getObject();
	}

}

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
@interface TestAnnotation {
}