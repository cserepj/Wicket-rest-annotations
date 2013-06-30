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
package org.wicketstuff.rest.resource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;

import org.apache.wicket.ajax.json.JSONObject;
import org.apache.wicket.protocol.http.servlet.ServletWebRequest;
import org.apache.wicket.protocol.http.servlet.ServletWebResponse;
import org.apache.wicket.request.Request;
import org.apache.wicket.request.Response;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.AbstractResource;
import org.apache.wicket.request.resource.IResource;
import org.apache.wicket.util.string.StringValue;
import org.apache.wicket.util.string.StringValueConversionException;
import org.wicketstuff.rest.annotations.HttpMethod;
import org.wicketstuff.rest.annotations.JsonBody;
import org.wicketstuff.rest.annotations.MethodMapping;


import com.google.gson.Gson;

/**
 * Base class to build a resource that serves REST requests with JSON as exchange format.
 * 
 * @author andrea del bene
 *
 */
public class JsonRestResource implements IResource {
	private Map<String, UrlMappingInfo> mappedMethods = new HashMap<String, UrlMappingInfo>();
	private Gson gson = new Gson();
	
	public JsonRestResource() {
		loadAnnotatedMethods();
	}
	
	/**
	 * Convenience method to configure the JSON serializer/deserializer object.
	 * 
	 * @param gson
	 * 			the gson object used for serialize/deserialize JSON 
	 */
	protected void configureGson(final Gson gson){}

	/***
	 * Handles a REST request invoking one of the methods annotated with {@link MethodMapping}. If the method returns a value, 
	 * it is automatically serialized as a JSON string and written in the response.
	 */
	@Override
	public void respond(Attributes attributes) {
		PageParameters pageParameters = attributes.getParameters();
		ServletWebResponse response = (ServletWebResponse) attributes.getResponse();
		HttpMethod httpMethod = getHttpMethod((ServletWebRequest) RequestCycle.get().getRequest());
		int indexedParamCount = pageParameters.getIndexedCount();
		
		UrlMappingInfo mappedMethod = mappedMethods.get(indexedParamCount + "_" +
										httpMethod.getMethod());
		
		if(mappedMethod != null){
			Object result = invokeMappedMethod(mappedMethod, pageParameters);
			
			if(result != null){
				response.setContentType("application/json");
				try {
					response.write(gson.toJson(result));
				} catch (Exception e) {
					throw new RuntimeException("Error dserializing object to response", e);
				}
			}
		}
	}
	
	/***
	 * Internal method to load class methods annotated with {@link MethodMapping}
	 */
	private void loadAnnotatedMethods() {
		Method[] methods = getClass().getDeclaredMethods();		
		
		for (int i = 0; i < methods.length; i++) {
			Method method = methods[i];
			MethodMapping methodMapped = method.getAnnotation(MethodMapping.class);
			
			if(methodMapped != null){
				String urlPath = methodMapped.value();
				HttpMethod httpMethod = methodMapped.httpMethod();
				UrlMappingInfo urlMappingInfo = new UrlMappingInfo(urlPath, httpMethod, method);
				
				mappedMethods.put(urlMappingInfo.getSegmentsCount() + "_" + 
									httpMethod.getMethod(), urlMappingInfo);
			}
		}
	}
	
	/**
	 * Utility method to extract the request method
	 * @param clazz
	 * @param value
	 * @return the HTTP method used for this request
	 * @see HttpMethod
	 */
	public static HttpMethod getHttpMethod(ServletWebRequest request){
		HttpServletRequest httpRequest = request.getContainerRequest();
		return HttpMethod.toHttpMethod((httpRequest.getMethod()));
	}
	
	/***
	 * This method invokes one of the resource's method annotated with {@link MethodMapping}
	 * 
	 * @param mappedMethod
	 * 			mapping info of the method
	 * @param pageParameters
	 * 			PageParametrs object of the current request
	 * @return
	 * 			the value returned by the invoked method
	 */
	private Object invokeMappedMethod(UrlMappingInfo mappedMethod,
			PageParameters pageParameters) {
		
		Method targetMethod = mappedMethod.getMethod();
		Class<?>[] argsClasses = targetMethod.getParameterTypes();
		List parametersValues = new ArrayList();		
		Iterator<StringValue> segmentsIterator = mappedMethod.getSegments().iterator();
		
		for (int i = 0; i < argsClasses.length; i++) {
			Class<?> argClass = argsClasses[i];
			
			if(parameterIsJsonBody(i, targetMethod.getParameterAnnotations()))
				parametersValues.add(extractObjectFromBody(argClass));
			else
				parametersValues.add(extractParameterFromUrl(mappedMethod, pageParameters,
														 segmentsIterator, argClass)); 
		}
		
		try {
			return targetMethod.invoke(this, parametersValues.toArray());
		} catch (Exception e) {
			throw new RuntimeException("Error invoking method '" + targetMethod.getName() + "'", e);
		} 
	}

	/**
	 * Internal method that tries to extract an instance of the given class from the request body.
	 * @param argClass
	 * 			the type we want to extract from request body
	 * @return
	 * 			the extracted object
	 */
	private Object extractObjectFromBody(Class<?> argClass) {
		ServletWebRequest servletRequest = (ServletWebRequest)RequestCycle.get().getRequest();
		HttpServletRequest httpRequest = servletRequest.getContainerRequest();
		try {
			BufferedReader bufReader = httpRequest.getReader();
			StringBuilder builder = new StringBuilder();
			String jsonString;
			
			while((jsonString = bufReader.readLine()) != null)
				builder.append(jsonString);
			
			return gson.fromJson(builder.toString(), argClass);				
		} catch (IOException e) {
			throw new RuntimeException("Error deserializing object from request", e);
		}
	}

	/**
	 * Check if a parameter is annotated with {@link JsonBody}
	 * @param i 
	 * 			function parameter index 
	 * @param parametersAnnotations
	 * 			bidimensional array containing the annotations for function parameters
	 * @return true if the function parameter is annotated with JsonBody, false otherwise
	 * @see JsonBody
	 */
	private boolean parameterIsJsonBody(int i,
			Annotation[][] parametersAnnotations) {
		if(parametersAnnotations.length == 0)
			return false;
		
		Annotation[] parameterAnnotation = parametersAnnotations[i];
		
		for (int j = 0; j < parameterAnnotation.length; j++) {
			Annotation annotation = parameterAnnotation[j];
			if(annotation instanceof JsonBody)
				return true;
		}
		return false;
	}

	/***
	 * Extract parameters values from the rest URL 
	 * 
	 * @param mappedMethod
	 * 			mapping info of the method
	 * @param pageParameters
	 * 			PageParametrs object of the current request
	 * @param segmentsIterator
	 * 			iterator over the mapped segments
	 * @param argClass
	 * 			type of the parameter we want to extract
	 * @return
	 * 			the parameter's value
	 */
	private Object extractParameterFromUrl(UrlMappingInfo mappedMethod, PageParameters pageParameters, 
											Iterator<StringValue> segmentsIterator, Class<?> argClass) {
		try {
			StringValue segmentValue = null;
			
			while(segmentsIterator.hasNext()){
				StringValue currentSegment = segmentsIterator.next();
				
				if(currentSegment instanceof VariableSegment){
					segmentValue = currentSegment;
					break;
				}
			}
			
			if(segmentValue != null){
				int indexOf = mappedMethod.getSegments().indexOf(segmentValue);
				StringValue actualValue = pageParameters.get(indexOf);
				
				return toObject(argClass, actualValue.toString());
			}
			return null;
		} catch (Exception e) {
			throw new RuntimeException("Error retrieving a constructor with a string parameter.", e);
		}
	}
	
	/**
	 * Utility method to convert primitive data types to the corresponding wrapper objects
	 * @param clazz
	 * 			the primitive class we want to convert
	 * @param value
	 * 			the string value we want to convert into the wrapper class
	 * @return
	 * 			the wrapper class for the given primitive type
	 */
	public static Object toObject( Class clazz, String value ) {
	    if( boolean.class == clazz ) return Boolean.parseBoolean( value );
	    if( byte.class == clazz ) return Byte.parseByte( value );
	    if( short.class == clazz ) return Short.parseShort( value );
	    if( int.class == clazz ) return Integer.parseInt( value );
	    if( long.class == clazz ) return Long.parseLong( value );
	    if( float.class == clazz ) return Float.parseFloat( value );
	    if( double.class == clazz ) return Double.parseDouble( value );
	    return value;
	}
}

/**
 * 
 * @author andrea del bene
 *
 */
class UrlMappingInfo{
	private HttpMethod httpMethod;
	private List<StringValue> segments = new ArrayList<StringValue>();
	private Method method;
	
	public UrlMappingInfo(String urlPath, HttpMethod httpMethod, Method method){
		this.httpMethod = httpMethod;
		this.method = method;
		
		String[] segArray = urlPath.split("/");
		
		for (int i = 0; i < segArray.length; i++) {
			String segment = segArray[i];
			StringValue segmentValue;
			
			if(segment.isEmpty())continue;
			
			if(isParameterSegment(segment))
				segmentValue = new VariableSegment(segment);
			else
				segmentValue = StringValue.valueOf(segment);
			
			segments.add(segmentValue);
		}
	}
	/**
	 * Utility method to check if a segment contains a parameter (i.e. '/{parameterName}/').
	 * 
	 * @param segment
	 * @return true if the segment contains a parameter, false otherwise.
	 */
	private boolean isParameterSegment(String segment) {
		return segment.length() >= 4 && segment.startsWith("{") 
				&& segment.endsWith("}");
	}
	
	//getters and setters
	
	public List<StringValue> getSegments() {
		return segments;
	}
	
	public int getSegmentsCount(){
		return segments.size();
	}

	public HttpMethod getHttpMethod() {
		return httpMethod;
	}

	public Method getMethod() {
		return method;
	}
}
/**
 * {@link StringValue} subtype that contains a mounted segment with a parameter's value (for example '/{id}/'). 
 * 
 * @author andrea del bene 
 *
 */
class VariableSegment extends StringValue{
	protected VariableSegment(String text) {
		super(text);
	}
}