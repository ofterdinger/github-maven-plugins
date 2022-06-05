/*
 * Copyright (c) 2012 GitHub Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.github.maven.plugins.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainerException;
import org.codehaus.plexus.context.Context;
import org.codehaus.plexus.context.ContextException;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Credential tests for the various configuration types
 *
 * @author Kevin Sawicki (kevin@github.com)
 */
public class ClientCredentialsTest {

	private class TestMojo extends GitHubProjectMojo {

		private final AtomicReference<String> user = new AtomicReference<>();

		private final AtomicReference<String> password = new AtomicReference<>();

		private final AtomicReference<String> token = new AtomicReference<>();

		@Override
		protected GitHubClient createClient() {
			try {
				DefaultPlexusContainer container = new DefaultPlexusContainer();
				Context context = container.getContext();
				context.put(PlexusConstants.PLEXUS_KEY, container);
				super.contextualize(context);
			} catch (PlexusContainerException pce) {
				pce.printStackTrace(System.err);
			} catch (ContextException ce) {
				ce.printStackTrace(System.err);
			}

			return new GitHubClient() {
				@Override
				public GitHubClient setCredentials(String user, String password) {
					TestMojo.this.user.set(user);
					TestMojo.this.password.set(password);
					return super.setCredentials(user, password);
				}

				@Override
				public GitHubClient setOAuth2Token(String token) {
					TestMojo.this.token.set(token);
					return super.setOAuth2Token(token);
				}

			};
		}

		@Override
		public void execute() {
			// Intentionally left blank
		}
	}

	/**
	 * Test configured client with direct user name and password
	 *
	 * @throws MojoExecutionException
	 */
	@Test
	public void validUserNameAndPassword() throws MojoExecutionException {
		TestMojo mojo = new TestMojo();
		GitHubClient client = mojo.createClient(null, "a", "b", null, null, null);
		assertNotNull(client);
		assertEquals("a", mojo.user.get());
		assertEquals("b", mojo.password.get());
		assertNull(mojo.token.get());
	}

	/**
	 * Test configured client with no user name
	 *
	 * @throws MojoExecutionException
	 */
	@Test(expected = MojoExecutionException.class)
	public void noUserName() throws MojoExecutionException {
		TestMojo mojo = new TestMojo();
		mojo.createClient(null, "a", null, null, null, null);
	}

	/**
	 * Test configured client with no password
	 *
	 * @throws MojoExecutionException
	 */
	@Test(expected = MojoExecutionException.class)
	public void noPassword() throws MojoExecutionException {
		TestMojo mojo = new TestMojo();
		mojo.createClient(null, null, "b", null, null, null);
	}

	/**
	 * Test configured client with token
	 *
	 * @throws MojoExecutionException
	 */
	@Test
	public void validOAuth2Token() throws MojoExecutionException {
		TestMojo mojo = new TestMojo();
		GitHubClient client = mojo.createClient(null, null, null, "token", null, null);
		assertNotNull(client);
		assertNull(mojo.user.get());
		assertNull(mojo.password.get());
		assertEquals("token", mojo.token.get());
	}

	/**
	 * Test configured client with token
	 *
	 * @throws MojoExecutionException
	 */
	@Test
	public void validOAuth2TokenWithUsername() throws MojoExecutionException {
		TestMojo mojo = new TestMojo();
		GitHubClient client = mojo.createClient(null, "a", null, "token", null, null);
		assertNotNull(client);
		assertNull(mojo.user.get());
		assertNull(mojo.password.get());
		assertEquals("token", mojo.token.get());
	}

	/**
	 * Test configured client with server with username & password
	 *
	 * @throws MojoExecutionException
	 */
	@Test
	@Ignore
	public void validServerUsernameAndPassword() throws MojoExecutionException {
		TestMojo mojo = new TestMojo();
		Settings settings = new Settings();
		Server server = new Server();
		server.setId("server");
		server.setUsername("a");
		server.setPassword("b");
		settings.addServer(server);
		GitHubClient client = mojo.createClient(null, null, null, null, "server", settings);
		assertNotNull(client);
		assertEquals("a", mojo.user.get());
		assertEquals("b", mojo.password.get());
		assertNull(mojo.token.get());
	}

	/**
	 * Test configured client with server and no username & password
	 *
	 * @throws MojoExecutionException
	 */
	@Test(expected = MojoExecutionException.class)
	public void noServerUsernameAndPassword() throws MojoExecutionException {
		TestMojo mojo = new TestMojo();
		Settings settings = new Settings();
		Server server = new Server();
		server.setId("server");
		server.setUsername("");
		server.setPassword("");
		settings.addServer(server);
		mojo.createClient(null, null, null, null, "server", settings);
	}

	/**
	 * Test configured client with server with OAuth 2 token
	 *
	 * @throws MojoExecutionException
	 */
	@Test
	@Ignore
	public void validServerToken() throws MojoExecutionException {
		TestMojo mojo = new TestMojo();
		Settings settings = new Settings();
		Server server = new Server();
		server.setId("server");
		server.setPassword("b");
		settings.addServer(server);
		GitHubClient client = mojo.createClient(null, null, null, null, "server", settings);
		assertNotNull(client);
		assertNull(mojo.user.get());
		assertNull(mojo.password.get());
		assertEquals("b", mojo.token.get());
	}

	/**
	 * Test configured client with missing server
	 *
	 * @throws MojoExecutionException
	 */
	@Test(expected = MojoExecutionException.class)
	public void missingServerNoSettings() throws MojoExecutionException {
		TestMojo mojo = new TestMojo();
		mojo.createClient(null, null, null, null, "server", null);
	}

	/**
	 * Test configured client with missing server
	 *
	 * @throws MojoExecutionException
	 */
	@Test(expected = MojoExecutionException.class)
	public void missingServerNullServers() throws MojoExecutionException {
		TestMojo mojo = new TestMojo();
		Settings settings = new Settings();
		mojo.createClient(null, null, null, null, "server", settings);
	}

	/**
	 * Test configured client with missing server
	 *
	 * @throws MojoExecutionException
	 */
	@Test(expected = MojoExecutionException.class)
	public void missingServerEmptyServers() throws MojoExecutionException {
		TestMojo mojo = new TestMojo();
		Settings settings = new Settings();
		settings.setServers(Collections.<Server>emptyList());
		mojo.createClient(null, null, null, null, "server", settings);
	}

	/**
	 * Test configured client with missing server
	 *
	 * @throws MojoExecutionException
	 */
	@Test(expected = MojoExecutionException.class)
	public void missingServerNoMatching() throws MojoExecutionException {
		TestMojo mojo = new TestMojo();
		Settings settings = new Settings();
		Server server = new Server();
		server.setId("server2");
		server.setPassword("b");
		settings.addServer(server);
		mojo.createClient(null, null, null, null, "server", settings);
	}

	/**
	 * Test configured client with no configuration
	 *
	 * @throws MojoExecutionException
	 */
	@Test(expected = MojoExecutionException.class)
	public void noConfiguration() throws MojoExecutionException {
		TestMojo mojo = new TestMojo();
		mojo.createClient(null, null, null, null, null, null);
	}

	/**
	 * Test configured client with no configuration
	 *
	 * @throws MojoExecutionException
	 */
	@Test(expected = MojoExecutionException.class)
	public void noConfigurationWithSettings() throws MojoExecutionException {
		TestMojo mojo = new TestMojo();
		Settings settings = new Settings();
		mojo.createClient(null, null, null, null, null, settings);
	}
}
