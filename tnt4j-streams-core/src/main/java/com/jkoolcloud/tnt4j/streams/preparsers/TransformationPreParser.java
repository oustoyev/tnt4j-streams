/*
 * Copyright 2014-2018 JKOOL, LLC.
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

package com.jkoolcloud.tnt4j.streams.preparsers;

import java.io.InputStream;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.jkoolcloud.tnt4j.config.ConfigException;
import com.jkoolcloud.tnt4j.config.Configurable;
import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.sink.DefaultEventSinkFactory;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.transform.AbstractScriptTransformation;
import com.jkoolcloud.tnt4j.streams.transform.ValueTransformation;
import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;
import com.jkoolcloud.tnt4j.streams.utils.Utils;

/**
 * Pre-parser to convert parser parser input data using transformations compatible with
 * {@link com.jkoolcloud.tnt4j.streams.transform.ValueTransformation} and defined by script or transformation bean
 * reference.
 * <p>
 * This pre-parser supports the following configuration properties:
 * <ul>
 * <li>id - transformation identifier</li>
 * <li>lang - transformation script language</li>
 * <li>script - code of transformation script (can't be mixed with {@code beanRef})</li>
 * <li>beanRef - transformation bean reference (can't be mixed with {@code script})</li>
 * </ul>
 *
 * @version $Revision: 1 $
 */
public class TransformationPreParser extends AbstractPreParser<Object, Object> implements Configurable {
	private static final EventSink LOGGER = DefaultEventSinkFactory.defaultEventSink(TransformationPreParser.class);

	/**
	 * Constant for name of pre-parser configuration property {@value} defining transformation identifier.
	 */
	protected static final String PROP_ID = "id"; // NON-NLS
	/**
	 * Constant for name of pre-parser configuration property {@value} defining transformation used script language.
	 */
	protected static final String PROP_LANG = "lang"; // NON-NLS
	/**
	 * Constant for name of pre-parser configuration property {@value} defining transformation script code.
	 */
	protected static final String PROP_SCRIPT = "script"; // NON-NLS
	/**
	 * Constant for name of pre-parser configuration property {@value} defining transformation bean reference.
	 */
	protected static final String PROP_BEAN_REF = "beanRef"; // NON-NLS

	private Map<String, ?> configuration;
	private ValueTransformation<Object, Object> transformation;

	/**
	 * Instantiates a new Transformation pre parser.
	 */
	public TransformationPreParser() {

	}

	@Override
	public Map<String, ?> getConfiguration() {
		return configuration;
	}

	@Override
	@SuppressWarnings("unchecked")
	public void setConfiguration(Map<String, ?> settings) throws ConfigException {
		this.configuration = settings;

		Object transformBean = settings.get(PROP_BEAN_REF);
		String scriptCode = Utils.getString(PROP_SCRIPT, settings, "");

		if (transformBean != null) {
			if (!(transformBean instanceof ValueTransformation)) {
				throw new ConfigException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
						"TransformationPreParser.ref.must.implement", getName(), PROP_BEAN_REF,
						ValueTransformation.class.getName()), settings);
			}

			if (StringUtils.isNotEmpty(scriptCode)) {
				throw new ConfigException(
						StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
								"TransformationPreParser.config.has.both", getName(), PROP_BEAN_REF, PROP_SCRIPT),
						settings);
			}
		} else {
			if (StringUtils.isEmpty(scriptCode)) {
				throw new ConfigException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
						"TransformationPreParser.missing.transformation.definition", getName(), PROP_BEAN_REF,
						PROP_SCRIPT), settings);
			}
		}

		if (transformBean instanceof ValueTransformation) {
			transformation = (ValueTransformation<Object, Object>) transformBean;
			LOGGER.log(OpLevel.INFO,
					StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
							"TransformationPreParser.will.use.bean.transformation"),
					getName(), Utils.getName(transformation));
		} else {
			String id = Utils.getString(PROP_ID, settings, null);
			String lang = Utils.getString(PROP_LANG, settings, null);

			LOGGER.log(OpLevel.INFO, StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
					"TransformationPreParser.will.use.script.transformation"), getName(), scriptCode);

			transformation = AbstractScriptTransformation.createScriptTransformation(id, lang, scriptCode, null);
		}
	}

	@Override
	public Object preParse(Object data) throws Exception {
		return transformation.transform(data, null);
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * This pre-parser supports the following class types (and all classes extending/implementing any of these):
	 * <ul>
	 * <li>{@link java.lang.String}</li>
	 * <li>{@link java.io.InputStream}</li>
	 * <li>{@link java.io.Reader}</li>
	 * <li>{@code byte[]}</li>
	 * <li>{@link java.nio.ByteBuffer}</li>
	 * </ul>
	 */
	@Override
	public boolean isDataClassSupported(Object data) {
		return String.class.isInstance(data) || InputStream.class.isInstance(data) || Reader.class.isInstance(data)
				|| byte[].class.isInstance(data) || ByteBuffer.class.isInstance(data);
	}

	@Override
	public boolean isUsingParserForInput() {
		return true;
	}
}
