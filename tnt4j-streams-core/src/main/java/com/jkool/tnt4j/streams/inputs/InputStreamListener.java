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

package com.jkool.tnt4j.streams.inputs;

import com.nastel.jkool.tnt4j.core.OpLevel;

/**
 * <p>
 * A streaming progress/status notifications listener interface. This interface
 * can be implemented by classes that are interested in streaming process
 * progress and status changes.
 * </p>
 *
 * @see TNTInputStream#addStreamListener(InputStreamListener)
 *
 * @version $Revision: 1 $
 *
 */
public interface InputStreamListener<T> {
	/**
	 * This method gets called when activity items streaming process progress
	 * has updated.
	 * 
	 * @param stream
	 *            stream sending notification
	 * @param current
	 *            index of currently streamed activity item
	 * @param total
	 *            total number of activity items to stream
	 */
	void onProgressUpdate(TNTInputStream stream, int current, int total);

	/**
	 * This method gets called when activity items streaming process has
	 * completed successfully.
	 *
	 * @param stream
	 *            stream sending notification
	 * @param result
	 *            result got after streaming completion
	 */
	void onSuccess(TNTInputStream stream, T result);

	/**
	 * This method gets called when activity items streaming process has failed.
	 * 
	 * @param stream
	 *            stream sending notification
	 * @param msg
	 *            failure message
	 * @param exc
	 *            failure exception
	 * @param code
	 *            failure code
	 */
	void onFailure(TNTInputStream stream, String msg, Throwable exc, String code);

	/**
	 * This method gets called when activity items streaming process status has
	 * changed.
	 *
	 * @param stream
	 *            stream sending notification
	 * @param status
	 *            new stream status value
	 */
	void onStatusChange(TNTInputStream stream, StreamStatus status);

	/**
	 * This method gets called when activity items streaming process has
	 * finished independent of completion state.
	 *
	 * @param stream
	 *            stream sending notification
	 * @param stats
	 *            stream statistics
	 *
	 */
	void onFinish(TNTInputStream stream, TNTInputStream.StreamStats stats);

	/**
	 * This method gets called when activity items streaming process detects
	 * some notable event.
	 *
	 * @param stream
	 *            stream sending notification
	 * @param level
	 *            event severity level
	 * @param message
	 *            event related message
	 * @param source
	 *            event source
	 */
	void onStreamEvent(TNTInputStream stream, OpLevel level, String message, Object source);
}
