/*
 * Copyright 2014-2016 JKOOL, LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.jkool.tnt4j.streams.inputs;

import java.lang.IllegalStateException;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Map;

import javax.jms.*;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.commons.lang.StringUtils;

import com.jkool.tnt4j.streams.configure.StreamsConfig;
import com.jkool.tnt4j.streams.utils.StreamsResources;
import com.nastel.jkool.tnt4j.core.OpLevel;
import com.nastel.jkool.tnt4j.sink.DefaultEventSinkFactory;
import com.nastel.jkool.tnt4j.sink.EventSink;

/**
 * <p>
 * Implements a JMS message transported activity stream, where each JMS message
 * payload data carried data is assumed to represent a single activity or event
 * which should be recorded.
 *
 * </p>
 * <p>
 * This activity stream requires parsers that can support JMS {@code Message}
 * data.
 * </p>
 * This activity stream supports the following properties:
 * <ul>
 * <li>ServerURI - JMS server URL. (Required)</li>
 * <li>Queue - queue destination name. (Required - just one of 'Queue' or
 * 'Topic')</li>
 * <li>Topic - topic destination name. (Required - just one of 'Queue' or
 * 'Topic')</li>
 * <li>JNDIFactory - JNDI context factory name. (Required)</li>
 * <li>JMSConnFactory - JMS connection factory name. (Required)</li>
 * </ul>
 *
 * @version $Revision: 1$
 *
 * @see com.jkool.tnt4j.streams.parsers.ActivityParser#isDataClassSupported(Object)
 */
public class JMSStream extends AbstractBufferedStream<Message> {
	private static final EventSink LOGGER = DefaultEventSinkFactory.defaultEventSink(JMSStream.class);

	// Stream properties
	private String serverURL = null;
	private String queueName = null;
	private String topicName = null;
	private String jndiFactory = null;
	private String jmsConnFactory = null;

	private JMSDataReceiver jmsDataReceiver;

	/**
	 * Construct empty JMSStream. Requires configuration settings to set input
	 * stream source.
	 */
	public JMSStream() {
		super(LOGGER);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Object getProperty(String name) {
		if (StreamsConfig.PROP_SERVER_URI.equalsIgnoreCase(name)) {
			return serverURL;
		}
		if (StreamsConfig.PROP_QUEUE_NAME.equalsIgnoreCase(name)) {
			return queueName;
		}
		if (StreamsConfig.PROP_TOPIC_NAME.equalsIgnoreCase(name)) {
			return topicName;
		}
		if (StreamsConfig.PROP_JNDI_FACTORY.equalsIgnoreCase(name)) {
			return jndiFactory;
		}
		if (StreamsConfig.PROP_JMS_CONN_FACTORY.equalsIgnoreCase(name)) {
			return jmsConnFactory;
		}

		return super.getProperty(name);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setProperties(Collection<Map.Entry<String, String>> props) throws Throwable {
		if (props == null) {
			return;
		}

		super.setProperties(props);

		for (Map.Entry<String, String> prop : props) {
			String name = prop.getKey();
			String value = prop.getValue();
			if (StreamsConfig.PROP_SERVER_URI.equalsIgnoreCase(name)) {
				serverURL = value;
			} else if (StreamsConfig.PROP_QUEUE_NAME.equalsIgnoreCase(name)) {
				if (StringUtils.isNotEmpty(topicName)) {
					throw new IllegalStateException(
							StreamsResources.getStringFormatted("CharacterStream.cannot.set.both",
									StreamsConfig.PROP_QUEUE_NAME, StreamsConfig.PROP_TOPIC_NAME));
				}
				queueName = value;
			} else if (StreamsConfig.PROP_TOPIC_NAME.equalsIgnoreCase(name)) {
				if (StringUtils.isNotEmpty(queueName)) {
					throw new IllegalStateException(
							StreamsResources.getStringFormatted("CharacterStream.cannot.set.both",
									StreamsConfig.PROP_QUEUE_NAME, StreamsConfig.PROP_TOPIC_NAME));
				}
				topicName = value;
			} else if (StreamsConfig.PROP_JNDI_FACTORY.equalsIgnoreCase(name)) {
				jndiFactory = value;
			} else if (StreamsConfig.PROP_JMS_CONN_FACTORY.equalsIgnoreCase(name)) {
				jmsConnFactory = value;
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void initialize() throws Throwable {
		super.initialize();

		if (StringUtils.isEmpty(queueName) && StringUtils.isEmpty(queueName)) {
			throw new IllegalStateException(
					StreamsResources.getStringFormatted("TNTInputStream.property.undefined.one.of",
							StreamsConfig.PROP_QUEUE_NAME, StreamsConfig.PROP_TOPIC_NAME));
		}

		jmsDataReceiver = new JMSDataReceiver();
		Hashtable env = new Hashtable();
		env.put(Context.INITIAL_CONTEXT_FACTORY, jndiFactory);
		env.put(Context.PROVIDER_URL, serverURL);

		Context ic = new InitialContext(env);

		jmsDataReceiver.initialize(ic, StringUtils.isEmpty(queueName) ? topicName : queueName, jmsConnFactory);

		LOGGER.log(OpLevel.DEBUG, StreamsResources.getString("JMSStream.stream.ready"));

		jmsDataReceiver.start();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void cleanup() {
		jmsDataReceiver.shutdown();

		super.cleanup();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected boolean isInputEnded() {
		return jmsDataReceiver.isInputEnded();
	}

	private class JMSDataReceiver extends InputProcessor implements MessageListener {

		private ConnectionFactory jmsConFactory;
		private Connection jmsCon;
		private Session jmsSession;
		private MessageConsumer jmsReceiver;
		private Destination destination;

		private JMSDataReceiver() {

			super("JMSStream.JMSDataReceiver"); // NON-NLS
		}

		private void initialize(Context ctx, String destinationName, String jmsConnFactoryName)
				throws NamingException, JMSException {
			jmsConFactory = (ConnectionFactory) ctx.lookup(jmsConnFactoryName);
			jmsCon = jmsConFactory.createConnection();
			jmsSession = jmsCon.createSession(false, Session.AUTO_ACKNOWLEDGE);
			destination = (Destination) ctx.lookup(destinationName);
			jmsReceiver = jmsSession.createConsumer(destination);
			jmsReceiver.setMessageListener(this);
			jmsCon.start();
		}

		/**
		 * Adds received JMS message to input buffer destination.
		 *
		 * @param msg
		 *            received JMS message
		 *
		 * @see javax.jms.MessageListener#onMessage(Message)
		 */
		@Override
		public void onMessage(Message msg) {
			if (msg == null) {
				return;
			}

			addInputToBuffer(msg);
		}

		/**
		 * Closes JMS objects.
		 *
		 * @throws JMSException
		 *             if JMS fails to close objects due to internal error
		 */
		void close() throws Exception {
			jmsReceiver.close();
			jmsSession.close();
			jmsCon.close();

			super.close();
		}
	}
}