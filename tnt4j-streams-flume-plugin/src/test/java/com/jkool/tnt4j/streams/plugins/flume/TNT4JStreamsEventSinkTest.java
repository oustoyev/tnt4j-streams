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

package com.jkool.tnt4j.streams.plugins.flume;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.net.ServerSocket;

import org.apache.flume.*;
import org.junit.Test;

import com.jkool.tnt4j.streams.utils.FlumeConstants;
import com.jkool.tnt4j.streams.utils.StreamsResources;

/**
 * @author akausinis
 * @version 1.0
 */
public class TNT4JStreamsEventSinkTest {

	private static final int PORT = 8565;

	@Test
	public void testConfigure() {
		TNT4JStreamsEventSink flumeSink = new TNT4JStreamsEventSink();
		Context context = new Context() {
			{
				put(TNT4JStreamsEventSink.PROP_HOST, "localhost");
				put(TNT4JStreamsEventSink.PROP_PORT, "8528");
				put(TNT4JStreamsEventSink.PROP_STREAM_CONFIG, "");
			}
		};
		flumeSink.configure(context);
	}

	@Test
	public void testConfigureNoPort() {
		TNT4JStreamsEventSink flumeSink = new TNT4JStreamsEventSink();
		Context context = new Context() {
			{
				put(TNT4JStreamsEventSink.PROP_HOST, "localhost");
				put(TNT4JStreamsEventSink.PROP_STREAM_CONFIG, "");
			}
		};
		flumeSink.configure(context);
	}

	@Test
	public void testConfigureNoHost() {
		TNT4JStreamsEventSink flumeSink = new TNT4JStreamsEventSink();
		Context context = new Context() {
			{
				put(TNT4JStreamsEventSink.PROP_PORT, "8528");
				put(TNT4JStreamsEventSink.PROP_STREAM_CONFIG, "");
			}
		};
		flumeSink.configure(context);
	}

	@Test
	public void testConfigureExpException() {
		TNT4JStreamsEventSink flumeSink = new TNT4JStreamsEventSink();
		Context context = new Context() {
			{
				put(TNT4JStreamsEventSink.PROP_HOST, "localhost");
				put(TNT4JStreamsEventSink.PROP_PORT, "TEST");
				put(TNT4JStreamsEventSink.PROP_STREAM_CONFIG, "");
			}
		};
		flumeSink.configure(context);

	}

	@Test
	public void testProcess() throws EventDeliveryException, IOException {
		TNT4JStreamsEventSink flumeSink = new TNT4JStreamsEventSink();

		Context context = new Context() {
			{
				put(TNT4JStreamsEventSink.PROP_HOST, "localhost");
				put(TNT4JStreamsEventSink.PROP_PORT, String.valueOf(PORT));
				put(TNT4JStreamsEventSink.PROP_STREAM_CONFIG, "config.xml");
			}
		};
		flumeSink.configure(context);

		Thread serverThread = new Thread(new Runnable() {
			@Override
			public void run() {
				ServerSocket server;
				try {
					server = new ServerSocket(PORT);
					server.accept();
				} catch (IOException e) {
				}
			}
		});
		serverThread.start();

		final Channel channelMock = mock(Channel.class);
		final Event eventMock = mock(Event.class);

		flumeSink.setChannel(channelMock);
		when(channelMock.take()).thenReturn(eventMock);
		when(channelMock.getTransaction()).thenReturn(mock(Transaction.class));

		flumeSink.process();

		verify(channelMock).take();
	}

	@Test
	public void testRB() {
		String keyModule = "TNT4JStreamsEventSink.streams.starting";
		String keyCore = "ActivityField.field.type.name.empty";

		String rbs1 = StreamsResources.getString(FlumeConstants.RESOURCE_BUNDLE_FLUME, keyModule);
		assertNotEquals("Flume resource bundle entry not found", rbs1, keyModule);
		rbs1 = StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_CORE, keyModule);
		assertEquals("Flume resource bundle entry found in core", rbs1, keyModule);
		rbs1 = StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_CORE, keyCore);
		assertNotEquals("Core resource bundle entry not found", rbs1, keyCore);
		rbs1 = StreamsResources.getString(FlumeConstants.RESOURCE_BUNDLE_FLUME, keyCore);
		assertEquals("Core resource bundle entry found in flume", rbs1, keyCore);
	}

}
