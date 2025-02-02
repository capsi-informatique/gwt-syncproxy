/*
 * Copyright 2011 Google Inc.
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

package com.google.gwt.user.client.rpc;

import java.util.logging.Level;
import java.util.logging.LogRecord;

import com.gdevelop.gwt.syncrpc.SyncProxy;

/**
 * Test cases for Generic Collections in GWT RPC.
 *
 * Modified by P.Prith in 0.5 to utilize Local App Engine server for service
 * through SyncProxy against Test in GWT 2.7.0. Alerted
 * {@link #METHOD_START_LINE} and {@link #METHOD_EXCEPTION_LINE} to reflect
 * modified code position. Also modified test to base from modified RpcTestBase
 * for async methods
 */
public class LoggingRPCTest extends RpcTestBase {

	private static LogRecord createLogRealRecord(Throwable thrown) {
		LogRecord result = new LogRecord(Level.INFO, "Test Log Record");

		// Only set serialized fields.
		result.setLoggerName("Test Logger Name");
		result.setMillis(1234567);

		result.setThrown(thrown);

		return result;
	}

	private static LogRecord createLogRecord() {
		LogRecord result = new LogRecord(Level.INFO, "Test Log Record");

		// Only set serialized fields.

		result.setLoggerName("Test Logger Name");
		result.setMillis(1234567);

		Throwable thrown = new Throwable("Test LogRecord Throwable 1");
		thrown.initCause(new Throwable("Test LogRecord Throwable cause"));
		result.setThrown(thrown);

		return result;
	}

	public static boolean isValid(LogRecord value) {
		if (!expectedLogRecord.getLevel().toString()
				.equals(value.getLevel().toString())) {
			return false;
		}

		if (!expectedLogRecord.getMessage().equals(value.getMessage())) {
			return false;
		}

		if (expectedLogRecord.getMillis() != value.getMillis()) {
			return false;
		}

		if (!expectedLogRecord.getLoggerName().equals(value.getLoggerName())) {
			return false;
		}

		Throwable expectedCause = expectedLogRecord.getThrown();
		Throwable valueCause = value.getThrown();
		while (expectedCause != null) {
			if (valueCause == null) {
				return false;
			}

			if (!expectedCause.getMessage().equals(valueCause.getMessage())) {
				return false;
			}

			// Do not compare trace as it is not stable across RPC.

			expectedCause = expectedCause.getCause();
			valueCause = valueCause.getCause();
		}
		if (valueCause != null) {
			return false;
		}

		return true;
	}

	/**
	 * WARNING! WARNING! If you edit this method or insert any lines of code
	 * above this method,you must re-edit the line number constants below;
	 */
	private static final int METHOD_START_LINE = 221;

	private static final int METHOD_EXCEPTION_LINE = 223;

	private static final LogRecord expectedLogRecord = createLogRecord();

	private LoggingRPCTestServiceAsync loggingRPCTestService;

	@Override
	public String getModuleName() {
		return "com.google.gwt.user.LoggingRPCSuite";
	}

	private LoggingRPCTestServiceAsync getServiceAsync() {
		if (this.loggingRPCTestService == null) {
			this.loggingRPCTestService = SyncProxy
					.create(LoggingRPCTestService.class);
		}
		return this.loggingRPCTestService;
	}

	public void testLogRecord() {
		LoggingRPCTestServiceAsync service = getServiceAsync();
		delayTestFinishForRpc();
		service.echoLogRecord(expectedLogRecord,
				new AsyncCallback<LogRecord>() {
			@Override
			public void onFailure(Throwable caught) {
				TestSetValidator.rethrowException(caught);
			}

			@Override
			public void onSuccess(LogRecord result) {
				assertNotNull(result);
				assertTrue(isValid(result));
				finishTest();
			}
		});
	}

	// Disabled as it does not appear that this test will directly apply to POJ
	// env. This may change

	// public void testStackMapDeobfuscation() {
	// final LoggingRPCTestServiceAsync service = getServiceAsync();
	// delayTestFinishForRpc();
	// // GWT.runAsync(LoggingRPCTest.class,
	// // new com.google.gwt.core.client.RunAsyncCallback() {
	// //
	// // @Override
	// // public void onFailure(Throwable reason) {
	// // }
	// //
	// // @Override
	// // public void onSuccess() {
	// try {
	// throwException("Runtime Exception");
	// } catch (Exception e) {
	// service.deobfuscateLogRecord(createLogRealRecord(e),
	// new AsyncCallback<LogRecord>() {
	//
	// @Override
	// public void onFailure(Throwable caught) {
	// TestSetValidator.rethrowException(caught);
	// }
	//
	// @Override
	// public void onSuccess(LogRecord record) {
	// Throwable thrown = record.getThrown();
	// boolean found = false;
	// for (StackTraceElement e : thrown.getStackTrace()) {
	// if (e.getFileName().contains("LoggingRPCTest")) {
	// // if DevMode or SourceMaps
	// // enabled and Chrome is the
	// // browser, check for exact
	// // line
	// if (SourceMapProperty
	// .isDetailedDeobfuscatedStackTraceSupported()) {
	// assertEquals(METHOD_EXCEPTION_LINE,
	// e.getLineNumber());
	// } else {
	// // else fallback to line
	// // number of method
	// // itself
	// assertEquals(METHOD_START_LINE,
	// e.getLineNumber());
	// }
	// String methodName = "throwException";
	// assertTrue(
	// "Method name mismatch, expected = "
	// + methodName
	// + " vs. actual = "
	// + e.getMethodName(),
	// e.getMethodName().contains(
	// methodName));
	// String className = "com.google.gwt.user.client.rpc.LoggingRPCTest";
	// assertTrue(
	// "Class name mismatch, expected = "
	// + className + " actual = "
	// + e.getClassName(),
	// className.contains(e.getClassName()));
	// found = true;
	// break;
	// }
	// }
	// assertTrue(found);
	// finishTest();
	// }
	// });
	// }
	// // }
	// // });
	// }

	private void throwException(String arg) {
		// prevent inlining by double referencing arg
		throw new RuntimeException(arg.charAt(0) + arg.substring(1));
	}

}
