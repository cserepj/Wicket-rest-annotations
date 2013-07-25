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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

import org.apache.wicket.util.parse.metapattern.MetaPattern;

public class MultiParamSegment extends GeneralURLSegment {
	final private List<GeneralURLSegment> subSegments;
	final private MetaPattern metaPattern;
	
	MultiParamSegment(String text) {
		super(text);
		
		this.subSegments = loadVariables(text);
		this.metaPattern = loadMetaPattern(subSegments);
	}

	private List<GeneralURLSegment> loadVariables(String text) {
		Matcher matcher = SEGMENT_PARAMETER.matcher(text);
		List<GeneralURLSegment> subSegments = new ArrayList<GeneralURLSegment>();
		int fixedTextIndex = 0;

		while (matcher.find()) {
			String group = matcher.group();
			GeneralURLSegment segment = GeneralURLSegment.newSegment(group);
			String fixedText = text.substring(fixedTextIndex, matcher.start());

			fixedTextIndex = matcher.end();

			if (!fixedText.isEmpty()) {
				subSegments.add(GeneralURLSegment.newSegment(fixedText));
			}

			subSegments.add(segment);
		}
		
		if(fixedTextIndex < text.length()){
			String fixedText = text.substring(fixedTextIndex, text.length());
			subSegments.add(GeneralURLSegment.newSegment(fixedText));
		}
		
		return subSegments;
	}
	
	private MetaPattern loadMetaPattern(List<GeneralURLSegment> subSegments) {
		List<MetaPattern> patterns = new ArrayList<MetaPattern>();
		
		for (GeneralURLSegment segment : subSegments) {
			patterns.add(segment.getMetaPattern());
		}
		
		return new MetaPattern(patterns);
	}
	
	@Override
	protected int calculateScore(String actualSegment) {
		Matcher matcher = metaPattern.matcher(actualSegment);
		
		return matcher.matches() ? 1 : 0;
	}
	
	@Override
	public void populatePathVariables(Map<String, String> variables, String segment) {
		int startingIndex = 0;
		
		if(!metaPattern.matcher(segment).matches())
			return;
		
		for (GeneralURLSegment subSegment : subSegments) {
			MetaPattern pattern = subSegment.getMetaPattern();
			segment = segment.substring(startingIndex);			
			Matcher matcher = pattern.matcher(segment);
			
			if(matcher.find()){
				String group = matcher.group();
				
				subSegment.populatePathVariables(variables, group);
				
				startingIndex = matcher.end();
			}
		}
	}
	
	public List<GeneralURLSegment> getSubSegments() {
		return subSegments;
	}
	
	@Override
	public MetaPattern getMetaPattern() {
		return metaPattern;
	}
}
