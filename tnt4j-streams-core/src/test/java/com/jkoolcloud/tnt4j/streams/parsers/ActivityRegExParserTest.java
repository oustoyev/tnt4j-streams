/*
 * Copyright 2014-2016 JKOOL, LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jkoolcloud.tnt4j.streams.parsers;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.io.Reader;
import java.text.ParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Before;
import org.junit.Test;

import com.jkoolcloud.tnt4j.streams.configure.ParserProperties;
import com.jkoolcloud.tnt4j.streams.fields.ActivityField;
import com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocator;
import com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocatorType;
import com.jkoolcloud.tnt4j.streams.inputs.TNTInputStream;

/**
 * @author akausinis
 * @version 1.0
 */
public class ActivityRegExParserTest extends ActivityParserTestBase {

	private static final String TEST_STRING = "TEST_STRING";
	private static final String TEST_PATTERN = "(\\S+)";
	private TNTInputStream<?, ?> stream = mock(TNTInputStream.class);

	@Override
	@Before
	public void prepare() {
		parser = new ActivityRegExParser();
	}

	@Override
	@Test
	public void setPropertiesTest() throws Exception {
		setProperty(parser, ParserProperties.PROP_PATTERN, TEST_PATTERN);
	}

	@Test
	@Override
	public void isDataClassSupportedTest() {
		assertTrue(parser.isDataClassSupported("TEST"));
		assertTrue(parser.isDataClassSupported("TEST".getBytes()));
		assertTrue(parser.isDataClassSupported(mock(Reader.class)));
		assertTrue(parser.isDataClassSupported(mock(InputStream.class)));
		assertFalse(parser.isDataClassSupported(this.getClass()));
	}

	@Test
	public void addField() {
		final ActivityField field = new ActivityField("Test");
		final ActivityFieldLocator locator = mock(ActivityFieldLocator.class);
		when(locator.getType()).thenReturn(ActivityFieldLocatorType.REMatchNum.name());
		field.addLocator(locator);
		parser.addField(field);
	}

	@Test
	public void addGroupField() {
		final ActivityField field = new ActivityField("TestGroup");
		final ActivityFieldLocator locator = mock(ActivityFieldLocator.class);
		when(locator.getType()).thenReturn(ActivityFieldLocatorType.REGroupNum.name());
		field.addLocator(locator);
		parser.addField(field);
	}

	@Test
	public void testParse() throws Exception {
		setPropertiesTest();
		addField();
		addGroupField();
		TNTInputStream<?, ?> stream = mock(TNTInputStream.class);
		parser.addField(mock(ActivityField.class));
		assertNotNull(parser.parse(stream, TEST_STRING));
	}

	@Test
	public void maches() {
		Pattern pattern = Pattern.compile(TEST_PATTERN);
		Matcher matcher = pattern.matcher(TEST_STRING);
		assertTrue(matcher.matches());
	}

	@Test
	public void setPropertiesNotEqualsPropNameTest() throws Exception {
		setProperty(parser, ParserProperties.PROP_SIG_DELIM, "test");
	}

	@Test
	public void setPropertiesWhenPropValueIsEmptyTest() throws Exception {
		setProperty(parser, ParserProperties.PROP_PATTERN, "");
	}

	@Test
	public void addFieldWhenDataIsNullTest() {
		ActivityField af = new ActivityField("Test");
		parser.addField(af);
	}

	@Test(expected = IllegalArgumentException.class)
	public void addFieldExceptionTest() {
		ActivityFieldLocator locator = new ActivityFieldLocator(ActivityFieldLocatorType.REMatchNum, "REMatchNum");
		ActivityFieldLocator locator2 = new ActivityFieldLocator(ActivityFieldLocatorType.Label, "REMatchNum");
		ActivityField af = new ActivityField("test");
		af.addLocator(locator);
		parser.addField(af);
		af.addLocator(locator2);
		parser.addField(af);
	}

	@Test(expected = IllegalArgumentException.class)
	public void addFieldExceptionOtherTest() {
		ActivityFieldLocator locator = new ActivityFieldLocator(ActivityFieldLocatorType.REMatchNum, "REMatchNum");
		ActivityFieldLocator locator2 = new ActivityFieldLocator(ActivityFieldLocatorType.Label, "REMatchNum");
		ActivityField af = new ActivityField("test");
		af.addLocator(locator2);
		parser.addField(af);
		af.addLocator(locator);
		parser.addField(af);
	}

	@Test(expected = IllegalStateException.class)
	public void parseWhenPatternNullTest() throws Exception {
		parser.parse(stream, "");
	}

	@Test
	public void parseWhenWhenDataIsNullTest() throws Exception {
		setProperty(parser, ParserProperties.PROP_PATTERN, "test");
		assertNull(parser.parse(stream, null));
	}

	@Test
	public void parseWhenWhenDataIsEmptyTest() throws Exception {
		setProperty(parser, ParserProperties.PROP_PATTERN, "test");
		assertNull(parser.parse(stream, ""));
	}

	@Test
	public void parseWhenNoMatchesTest() throws Exception {
		setProperty(parser, ParserProperties.PROP_PATTERN, "(\\d+)");
		assertNull(parser.parse(stream, "test"));
	}

	@Test(expected = ParseException.class)
	public void parseWhenMatchMapExceptionTest() throws Exception {
		ActivityFieldLocator locator = new ActivityFieldLocator(ActivityFieldLocatorType.REMatchNum, "REMatchNum");
		ActivityField af = new ActivityField("test");
		af.addLocator(locator);
		parser.addField(af);
		setProperty(parser, ParserProperties.PROP_PATTERN, "(\\d+)");
		parser.parse(stream, "1111");
	}

	@Test
	public void parseWhenMatchMapIsEmptyTest() throws Exception {
		setProperty(parser, ParserProperties.PROP_PATTERN, "(\\d+)");
		parser.parse(stream, "1111");
	}

	@Test
	public void parseMatchMapOneEntryTest() throws Exception {
		ActivityFieldLocator locator = new ActivityFieldLocator(ActivityFieldLocatorType.REMatchNum, null);
		ActivityField af = new ActivityField("test");
		af.addLocator(locator);
		parser.addField(af);
		setProperty(parser, ParserProperties.PROP_PATTERN, "\\d+");
		parser.parse(stream, "1111555999");
	}

	@Test
	public void parseMatchMapTwoEntriesTest() throws Exception {
		ActivityFieldLocator locator = new ActivityFieldLocator(ActivityFieldLocatorType.REMatchNum, "0");
		ActivityFieldLocator locator1 = new ActivityFieldLocator(ActivityFieldLocatorType.REMatchNum, "2");
		ActivityField af = new ActivityField("test");
		af.addLocator(locator);
		af.addLocator(locator1);
		parser.addField(af);
		setProperty(parser, ParserProperties.PROP_PATTERN, "\\d+");
		parser.parse(stream, "1111555999");
	}

	@Test
	public void parseGroupMapOneEntryTest() throws Exception {
		ActivityFieldLocator locator = new ActivityFieldLocator(ActivityFieldLocatorType.REMatchNum, null);
		ActivityField af = new ActivityField("test");
		af.addLocator(locator);
		parser.addField(af);
		setProperty(parser, ParserProperties.PROP_PATTERN, "\\d+");
		parser.parse(stream, "1111555999");
	}

	@Test
	public void parseGroupMapTwoEntriesTest() throws Exception {
		ActivityFieldLocator locator = new ActivityFieldLocator(ActivityFieldLocatorType.REGroupNum, "0");
		ActivityFieldLocator locator1 = new ActivityFieldLocator(ActivityFieldLocatorType.StreamProp, "2");
		ActivityField af = new ActivityField("test");
		af.addLocator(locator);
		af.addLocator(locator1);
		parser.addField(af);
		setProperty(parser, ParserProperties.PROP_PATTERN, "\\d+");
		parser.parse(stream, "1111555999");
	}
}