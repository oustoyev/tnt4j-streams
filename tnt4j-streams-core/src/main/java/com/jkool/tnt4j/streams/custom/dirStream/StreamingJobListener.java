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

package com.jkool.tnt4j.streams.custom.dirStream;

import com.jkool.tnt4j.streams.inputs.StreamingStatus;
import com.jkool.tnt4j.streams.inputs.TNTInputStream;
import com.nastel.jkool.tnt4j.core.OpLevel;

/**
 * @author akausinis
 * @version 1.0 TODO
 */
public interface StreamingJobListener<T> {
	void onProgressUpdate(StreamingJob job, int current, int total);

	void onSuccess(StreamingJob job, T result);

	void onFailure(StreamingJob job, String msg, Throwable exc, String code);

	void onStatusChange(StreamingJob job, StreamingStatus status);

	void onFinish(StreamingJob job, TNTInputStream.StreamStats stats);

	void onStreamEvent(StreamingJob job, OpLevel level, String message, Object source);
}
