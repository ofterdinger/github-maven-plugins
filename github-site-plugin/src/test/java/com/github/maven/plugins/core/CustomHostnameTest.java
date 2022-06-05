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

import java.util.concurrent.atomic.AtomicReference;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.settings.Settings;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.junit.Test;

/**
 * Tests using client with custom hostname
 *
 * @author Kevin Sawicki (kevin@github.com)
 */
public class CustomHostnameTest {

	private class TestMojo extends GitHubProjectMojo {

		private final AtomicReference<String> host = new AtomicReference<>();

		@Override
		protected GitHubClient createClient() {
			this.host.set(null);
			return super.createClient();
		}

		@Override
		protected GitHubClient createClient(String hostname) throws MojoExecutionException {
			this.host.set(hostname);
			return super.createClient(hostname);
		}

		@Override
		public GitHubClient createClient(String hostname, String userName, String password, String oauth2Token,
				String serverId, Settings settings) throws MojoExecutionException {
			return super.createClient(hostname, userName, password, oauth2Token, serverId, settings);
		}

		@Override
		public void execute() throws MojoExecutionException, MojoFailureException {
			// Intentionally left blank
		}
	}

	/**
	 * Test custom hostname
	 *
	 * @throws MojoExecutionException
	 *
	 */
	@Test
	public void validHostname() throws MojoExecutionException {
		TestMojo mojo = new TestMojo();
		GitHubClient client = mojo.createClient("h", "a", "b", null, null, null);
		assertNotNull(client);
		assertEquals("h", mojo.host.get());
	}

	/**
	 * Test null custom hostname
	 *
	 * @throws MojoExecutionException
	 */
	@Test
	public void nullHostname() throws MojoExecutionException {
		TestMojo mojo = new TestMojo();
		GitHubClient client = mojo.createClient(null, "a", "b", null, null, null);
		assertNotNull(client);
		assertNull(mojo.host.get());
	}

	/**
	 * Test empty custom hostname
	 *
	 * @throws MojoExecutionException
	 */
	@Test
	public void emptyHost() throws MojoExecutionException {
		TestMojo mojo = new TestMojo();
		GitHubClient client = mojo.createClient("", "a", "b", null, null, null);
		assertNotNull(client);
		assertNull(mojo.host.get());
	}
}
