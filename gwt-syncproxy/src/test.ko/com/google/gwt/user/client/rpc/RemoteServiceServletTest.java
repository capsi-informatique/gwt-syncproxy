/*
 * Copyright 2008 Google Inc.
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

import com.gdevelop.gwt.syncrpc.SyncProxy;
import com.google.gwt.core.client.GWT;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;

/**
 * This test case is used to check that the RemoteServiceServlet walks the class
 * hierarchy looking for the service interface. Prior to this test the servlet
 * would only look into the concrete class but not in any of its super classes.
 *
 * See <a href=
 * "http://code.google.com/p/google-web-toolkit/issues/detail?id=50&can=3&q="
 * >Bug 50</a> for more details.
 * <p>
 * This test works in conjunction with
 * {@link com.google.gwt.user.server.rpc.RemoteServiceServletTestServiceImpl}.
 * </p>
 *
 * Modified by P.Prith in 0.5 to utilize Local App Engine server for service
 * through SyncProxy against Test in GWT 2.7.0
 */
public class RemoteServiceServletTest extends RpcTestBase {
	private static class MyRpcRequestBuilder extends RpcRequestBuilder {
		private boolean doCreate;
		private boolean doFinish;
		private boolean doSetCallback;
		private boolean doSetContentType;
		private boolean doSetRequestData;
		private boolean doSetRequestId;

		public void check() {
			assertTrue("doCreate", this.doCreate);
			assertTrue("doFinish", this.doFinish);
			assertTrue("doSetCallback", this.doSetCallback);
			assertTrue("doSetContentType", this.doSetContentType);
			assertTrue("doSetRequestData", this.doSetRequestData);
			assertTrue("doSetRequestId", this.doSetRequestId);
		}

		@Override
		protected RequestBuilder doCreate(String serviceEntryPoint) {
			this.doCreate = true;
			return super.doCreate(serviceEntryPoint);
		}

		@Override
		protected void doFinish(RequestBuilder rb) {
			this.doFinish = true;
			super.doFinish(rb);
		}

		@Override
		protected void doSetCallback(RequestBuilder rb, RequestCallback callback) {
			this.doSetCallback = true;
			super.doSetCallback(rb, callback);
		}

		@Override
		protected void doSetContentType(RequestBuilder rb, String contentType) {
			this.doSetContentType = true;
			super.doSetContentType(rb, contentType);
		}

		@Override
		protected void doSetRequestData(RequestBuilder rb, String data) {
			this.doSetRequestData = true;
			super.doSetRequestData(rb, data);
		}

		@Override
		protected void doSetRequestId(RequestBuilder rb, int id) {
			this.doSetRequestId = true;
			super.doSetRequestId(rb, id);
		}
	}

	private Request req;

	protected RemoteServiceServletTestServiceAsync getAsyncService() {
		RemoteServiceServletTestServiceAsync service = SyncProxy
				.create(RemoteServiceServletTestService.class);
		((ServiceDefTarget) service).setServiceEntryPoint(getModuleBaseURL()
				+ "servlettest");
		return service;
	}

	/**
	 * @see com.google.gwt.user.client.rpc.RpcTestBase#setUp()
	 */
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		SyncProxy.suppressRelativePathWarning(true);
	}

	/**
	 * Modified by P.Prith to handle the {@link GWT#getModuleBaseURL()}
	 * dependency
	 */
	public void testAlternateStatusCode() {
		RemoteServiceServletTestServiceAsync service = getAsyncService();
		((ServiceDefTarget) service).setServiceEntryPoint(getModuleBaseURL()
				+ "servlettest/404");

		delayTestFinishForRpc();

		service.test(new AsyncCallback<Void>() {

			@Override
			public void onFailure(Throwable caught) {
				if (caught instanceof StatusCodeException) {
					assertEquals(Response.SC_NOT_FOUND,
							((StatusCodeException) caught).getStatusCode());
					assertEquals("Not Found",
							((StatusCodeException) caught).getStatusText());
					finishTest();
				} else {
					TestSetValidator.rethrowException(caught);
				}
			}

			@Override
			public void onSuccess(Void result) {
				fail("Should not have succeeded");
			}
		});
	}

	/**
	 * Verify behavior when the RPC method throws a RuntimeException declared on
	 * the RemoteService interface.
	 */
	public void testDeclaredRuntimeException() {
		RemoteServiceServletTestServiceAsync service = getAsyncService();

		delayTestFinishForRpc();

		service.throwDeclaredRuntimeException(new AsyncCallback<Void>() {

			@Override
			public void onFailure(Throwable caught) {
				assertTrue(caught instanceof NullPointerException);
				assertEquals("expected", caught.getMessage());
				finishTest();
			}

			@Override
			public void onSuccess(Void result) {
				fail();
			}
		});
	}

	public void testManualSend() throws RequestException {
		RemoteServiceServletTestServiceAsync service = getAsyncService();

		delayTestFinishForRpc();

		RequestBuilder builder = service
				.testExpectCustomHeader(new AsyncCallback<Void>() {

					@Override
					public void onFailure(Throwable caught) {
						TestSetValidator.rethrowException(caught);
					}

					@Override
					public void onSuccess(Void result) {
						assertTrue(!RemoteServiceServletTest.this.req
								.isPending());
						finishTest();
					}
				});

		builder.setHeader("X-Custom-Header", "true");
		this.req = builder.send();
		assertTrue(this.req.isPending());
	}

	/**
	 * Modified by P.prith to remove dependence on GWT
	 */
	public void testPermutationStrongName() {
		RemoteServiceServletTestServiceAsync service = getAsyncService();

		delayTestFinishForRpc();

		// assertNotNull(GWT.getPermutationStrongName());
		assertNotNull(((ServiceDefTarget) service).getSerializationPolicyName());
		// service.testExpectPermutationStrongName(GWT.getPermutationStrongName(),
		service.testExpectPermutationStrongName(
				((ServiceDefTarget) service).getSerializationPolicyName(),
				new AsyncCallback<Void>() {

					@Override
					public void onFailure(Throwable caught) {
						TestSetValidator.rethrowException(caught);
					}

					@Override
					public void onSuccess(Void result) {
						finishTest();
					}
				});
	}

	/**
	 * Test that the policy strong name is available from browser-side Java
	 * code.
	 */
	public void testPolicyStrongName() {
		String policy = ((ServiceDefTarget) getAsyncService())
				.getSerializationPolicyName();
		assertNotNull(policy);
		assertTrue(policy.length() != 0);
	}

	/**
	 * Send request without the permutation strong name and expect a
	 * SecurityException. This tests
	 * RemoteServiceServlet#checkPermutationStrongName.
	 */
	public void testRequestWithoutStrongNameHeader() {
		RemoteServiceServletTestServiceAsync service = getAsyncService();
		((ServiceDefTarget) service)
		.setRpcRequestBuilder(new RpcRequestBuilder() {
			/**
			 * Copied from base class.
			 */
			@Override
			protected void doFinish(RequestBuilder rb) {
				// Don't set permutation strong name
				rb.setHeader(MODULE_BASE_HEADER, GWT.getModuleBaseURL());
			}

		});

		delayTestFinishForRpc();
		service.test(new AsyncCallback<Void>() {
			@Override
			public void onFailure(Throwable caught) {
				assertTrue(caught instanceof StatusCodeException);
				assertEquals(500,
						((StatusCodeException) caught).getStatusCode());
				finishTest();
			}

			@Override
			public void onSuccess(Void result) {
				fail();
			}
		});
	}

	/**
	 * Ensure that each doFoo method is called.
	 */
	public void testRpcRequestBuilder() {
		final MyRpcRequestBuilder builder = new MyRpcRequestBuilder();
		RemoteServiceServletTestServiceAsync service = getAsyncService();
		((ServiceDefTarget) service).setRpcRequestBuilder(builder);

		delayTestFinishForRpc();
		service.test(new AsyncCallback<Void>() {
			@Override
			public void onFailure(Throwable caught) {
				TestSetValidator.rethrowException(caught);
			}

			@Override
			public void onSuccess(Void result) {
				builder.check();
				finishTest();
			}
		});
	}

	public void testServiceInterfaceLocation() {
		RemoteServiceServletTestServiceAsync service = getAsyncService();

		delayTestFinishForRpc();

		this.req = service.test(new AsyncCallback<Void>() {

			@Override
			public void onFailure(Throwable caught) {
				TestSetValidator.rethrowException(caught);
			}

			@Override
			public void onSuccess(Void result) {
				assertTrue(!RemoteServiceServletTest.this.req.isPending());
				finishTest();
			}
		});
		assertTrue(this.req.isPending());
	}

	/**
	 * Verify behavior when the RPC method throws an unknown RuntimeException
	 * (possibly one unknown to the client).
	 */
	public void testUnknownRuntimeException() {
		RemoteServiceServletTestServiceAsync service = getAsyncService();

		delayTestFinishForRpc();

		service.throwUnknownRuntimeException(new AsyncCallback<Void>() {

			@Override
			public void onFailure(Throwable caught) {
				assertTrue(caught instanceof InvocationException);
				finishTest();
			}

			@Override
			public void onSuccess(Void result) {
				fail();
			}
		});
	}
}
