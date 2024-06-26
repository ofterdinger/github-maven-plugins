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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.settings.Proxy;
import org.apache.maven.settings.building.DefaultSettingsBuilderFactory;
import org.apache.maven.settings.building.DefaultSettingsBuildingRequest;
import org.apache.maven.settings.building.SettingsBuilder;
import org.apache.maven.settings.building.SettingsBuildingException;
import org.apache.maven.settings.building.SettingsBuildingResult;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.client.IGitHubConstants;
import org.junit.jupiter.api.Test;

/**
 * NonProxy tests for the various configuration
 *
 * @author Kiyofumi Kondoh
 */
class MatchNonProxyTest {

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
		public void execute() {
			// Intentionally left blank
		}
	}

	/**
	 * matchNonProxy tests with single nonProxyHosts
	 *
	 * @throws SettingsBuildingException
	 */
	@Test
	void matchNonProxyWithSingle_nonPorxyHosts() throws SettingsBuildingException {
		SettingsBuilder builder = new DefaultSettingsBuilderFactory().newInstance();
		assertNotNull(builder);

		DefaultSettingsBuildingRequest request = new DefaultSettingsBuildingRequest();
		request.setSystemProperties(System.getProperties());
		request.setUserSettingsFile(
				new File("src/test/resources/settings/proxy/nonproxy-github.xml").getAbsoluteFile());

		SettingsBuildingResult result = builder.build(request);
		assertNotNull(result);
		assertNotNull(result.getEffectiveSettings());

		TestMojo mojo = new TestMojo();
		assertNotNull(mojo);

		assertNotNull(result.getEffectiveSettings().getProxies());
		for (final Proxy proxy : result.getEffectiveSettings().getProxies()) {
			{
				final boolean isNonProxy = GitHubProjectMojo.matchNonProxy(proxy, IGitHubConstants.HOST_DEFAULT);
				assertTrue(isNonProxy);
			}
			{
				final boolean isNonProxy = GitHubProjectMojo.matchNonProxy(proxy, IGitHubConstants.HOST_API);
				assertFalse(isNonProxy);
			}
			{
				final boolean isNonProxy = GitHubProjectMojo.matchNonProxy(proxy,
						"hoge." + IGitHubConstants.HOST_DEFAULT);
				assertFalse(isNonProxy);
			}
			{
				final boolean isNonProxy = GitHubProjectMojo.matchNonProxy(proxy,
						"hoge" + IGitHubConstants.HOST_DEFAULT);
				assertFalse(isNonProxy);
			}
			{
				final boolean isNonProxy = GitHubProjectMojo.matchNonProxy(proxy, mojo.host.get());
				assertTrue(isNonProxy);
			}
		}
	}

	/**
	 * matchNonProxy tests with multiple nonProxyHosts
	 *
	 * @throws SettingsBuildingException
	 */
	@Test
	void matchNonProxyWithMultiple_nonPorxyHosts() throws SettingsBuildingException {
		SettingsBuilder builder = new DefaultSettingsBuilderFactory().newInstance();
		assertNotNull(builder);

		DefaultSettingsBuildingRequest request = new DefaultSettingsBuildingRequest();
		request.setSystemProperties(System.getProperties());
		request.setUserSettingsFile(
				new File("src/test/resources/settings/proxy/nonproxy-github_and_api.xml").getAbsoluteFile());

		SettingsBuildingResult result = builder.build(request);
		assertNotNull(result);
		assertNotNull(result.getEffectiveSettings());

		TestMojo mojo = new TestMojo();
		assertNotNull(mojo);

		assertNotNull(result.getEffectiveSettings().getProxies());
		for (final Proxy proxy : result.getEffectiveSettings().getProxies()) {
			{
				final boolean isNonProxy = GitHubProjectMojo.matchNonProxy(proxy, IGitHubConstants.HOST_DEFAULT);
				assertTrue(isNonProxy);
			}
			{
				final boolean isNonProxy = GitHubProjectMojo.matchNonProxy(proxy, IGitHubConstants.HOST_API);
				assertTrue(isNonProxy);
			}
			{
				final boolean isNonProxy = GitHubProjectMojo.matchNonProxy(proxy,
						"hoge." + IGitHubConstants.HOST_DEFAULT);
				assertFalse(isNonProxy);
			}
			{
				final boolean isNonProxy = GitHubProjectMojo.matchNonProxy(proxy,
						"hoge" + IGitHubConstants.HOST_DEFAULT);
				assertFalse(isNonProxy);
			}
			{
				final boolean isNonProxy = GitHubProjectMojo.matchNonProxy(proxy, mojo.host.get());
				assertTrue(isNonProxy);
			}
		}
	}

	/**
	 * matchNonProxy tests with wildcard nonProxyHosts
	 *
	 * @throws SettingsBuildingException
	 */
	@Test
	void matchNonProxyWithWildcard_nonPorxyHosts() throws SettingsBuildingException {
		SettingsBuilder builder = new DefaultSettingsBuilderFactory().newInstance();
		assertNotNull(builder);

		DefaultSettingsBuildingRequest request = new DefaultSettingsBuildingRequest();
		request.setSystemProperties(System.getProperties());
		request.setUserSettingsFile(
				new File("src/test/resources/settings/proxy/nonproxy-github_wildcard.xml").getAbsoluteFile());

		SettingsBuildingResult result = builder.build(request);
		assertNotNull(result);
		assertNotNull(result.getEffectiveSettings());

		TestMojo mojo = new TestMojo();
		assertNotNull(mojo);

		assertNotNull(result.getEffectiveSettings().getProxies());
		for (final Proxy proxy : result.getEffectiveSettings().getProxies()) {
			{
				final boolean isNonProxy = GitHubProjectMojo.matchNonProxy(proxy, IGitHubConstants.HOST_DEFAULT);
				assertTrue(isNonProxy);
			}
			{
				final boolean isNonProxy = GitHubProjectMojo.matchNonProxy(proxy, IGitHubConstants.HOST_API);
				assertTrue(isNonProxy);
			}
			{
				final boolean isNonProxy = GitHubProjectMojo.matchNonProxy(proxy,
						"hoge." + IGitHubConstants.HOST_DEFAULT);
				assertTrue(isNonProxy);
			}
			{
				final boolean isNonProxy = GitHubProjectMojo.matchNonProxy(proxy,
						"hoge" + IGitHubConstants.HOST_DEFAULT);
				assertTrue(isNonProxy);
			}
			{
				final boolean isNonProxy = GitHubProjectMojo.matchNonProxy(proxy, mojo.host.get());
				assertTrue(isNonProxy);
			}
		}
	}

	/**
	 * getProxy tests with single nonProxyHosts
	 */
	@Test
	void getProxyWithSingle_nonProxyHosts() throws Exception {
		SettingsBuilder builder = new DefaultSettingsBuilderFactory().newInstance();
		assertNotNull(builder);

		DefaultSettingsBuildingRequest request = new DefaultSettingsBuildingRequest();
		request.setSystemProperties(System.getProperties());
		request.setUserSettingsFile(
				new File("src/test/resources/settings/proxy/nonproxy-github.xml").getAbsoluteFile());

		SettingsBuildingResult result = builder.build(request);
		assertNotNull(result);
		assertNotNull(result.getEffectiveSettings());

		TestMojo mojo = new TestMojo();
		assertNotNull(mojo);

		Proxy proxy = GitHubProjectMojo.getProxy(result.getEffectiveSettings(), "intra_github-test-nonproxy",
				mojo.host.get());
		assertNull(proxy);

		proxy = GitHubProjectMojo.getProxy(result.getEffectiveSettings(), "intra_github-test-nonproxy",
				"intra-github.com");
		assertNotNull(proxy);
	}

	/**
	 * getProxy tests with nonProxyHosts, which have same id
	 *
	 * @throws SettingsBuildingException
	 */
	@Test
	void getProxyIntraWithSameId() throws SettingsBuildingException {
		SettingsBuilder builder = new DefaultSettingsBuilderFactory().newInstance();
		assertNotNull(builder);

		DefaultSettingsBuildingRequest request = new DefaultSettingsBuildingRequest();
		request.setSystemProperties(System.getProperties());
		request.setUserSettingsFile(
				new File("src/test/resources/settings/proxy/nonproxy-intra_github.xml").getAbsoluteFile());

		SettingsBuildingResult result = builder.build(request);
		assertNotNull(result);
		assertNotNull(result.getEffectiveSettings());

		TestMojo mojo = new TestMojo();
		assertNotNull(mojo);

		Proxy proxy = GitHubProjectMojo.getProxy(result.getEffectiveSettings(), "intra_github-test-nonproxy",
				mojo.host.get());
		assertNotNull(proxy);

		proxy = GitHubProjectMojo.getProxy(result.getEffectiveSettings(), "intra_github-test-nonproxy",
				"intra-github.com");
		assertNull(proxy);

		proxy = GitHubProjectMojo.getProxy(result.getEffectiveSettings(), "intra_github-test-nonproxy",
				"intra_github.com");
		assertNotNull(proxy);
	}

	/**
	 * getProxy tests with nonProxyHosts, which doesn't have same id
	 *
	 * @throws SettingsBuildingException
	 */
	@Test
	void getProxyIntraNoSameId() throws SettingsBuildingException {
		SettingsBuilder builder = new DefaultSettingsBuilderFactory().newInstance();
		assertNotNull(builder);

		DefaultSettingsBuildingRequest request = new DefaultSettingsBuildingRequest();
		request.setSystemProperties(System.getProperties());
		request.setUserSettingsFile(
				new File("src/test/resources/settings/proxy/nonproxy-intra_github-no_same_id.xml").getAbsoluteFile());

		SettingsBuildingResult result = builder.build(request);
		assertNotNull(result);
		assertNotNull(result.getEffectiveSettings());

		TestMojo mojo = new TestMojo();
		assertNotNull(mojo);

		Proxy proxy = GitHubProjectMojo.getProxy(result.getEffectiveSettings(), "intra_github-test-nonproxy",
				mojo.host.get());
		assertNotNull(proxy);

		proxy = GitHubProjectMojo.getProxy(result.getEffectiveSettings(), "intra_github-test-nonproxy",
				"intra-github.com");
		assertNull(proxy);

		proxy = GitHubProjectMojo.getProxy(result.getEffectiveSettings(), "intra_github-test-nonproxy",
				"intra_github.com");
		assertNotNull(proxy);
	}

}
