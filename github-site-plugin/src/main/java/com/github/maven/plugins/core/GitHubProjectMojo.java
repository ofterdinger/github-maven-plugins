/*
 * Copyright (c) 2011 GitHub Inc.
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

import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Proxy;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.crypto.DefaultSettingsDecryptionRequest;
import org.apache.maven.settings.crypto.SettingsDecrypter;
import org.apache.maven.settings.crypto.SettingsDecryptionResult;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.context.Context;
import org.codehaus.plexus.context.ContextException;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Contextualizable;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.client.IGitHubConstants;

import com.github.maven.plugins.core.egit.GitHubClientEgit;
import com.google.common.annotations.VisibleForTesting;

/**
 * Base GitHub Mojo class to be extended.
 *
 * @author Kevin Sawicki (kevin@github.com)
 */
public abstract class GitHubProjectMojo extends AbstractMojo implements Contextualizable {

	@Requirement
	private PlexusContainer container;

	/**
	 * {@inheritDoc}
	 */
	public void contextualize(Context context) throws ContextException {
		this.container = (PlexusContainer) context.get(PlexusConstants.PLEXUS_KEY);
	}

	/**
	 * Is debug logging enabled?
	 *
	 * @return true if enabled, false otherwise
	 */
	protected boolean isDebug() {
		return getLog().isDebugEnabled();
	}

	/**
	 * Is info logging enabled?
	 *
	 * @return true if enabled, false otherwise
	 */
	protected boolean isInfo() {
		return getLog().isInfoEnabled();
	}

	/**
	 * Log given message at debug level
	 *
	 * @param message
	 */
	protected void debug(String message) {
		getLog().debug(message);
	}

	/**
	 * Log given message and throwable at debug level
	 *
	 * @param message
	 * @param throwable
	 */
	protected void debug(String message, Throwable throwable) {
		getLog().debug(message, throwable);
	}

	/**
	 * Log given message at info level
	 *
	 * @param message
	 */
	protected void info(String message) {
		getLog().info(message);
	}

	/**
	 * Log given message and throwable at info level
	 *
	 * @param message
	 * @param throwable
	 */
	protected void info(String message, Throwable throwable) {
		getLog().info(message, throwable);
	}

	/**
	 * Create client
	 *
	 * @param host
	 * @param userName
	 * @param password
	 * @param oauth2Token
	 * @param serverId
	 * @param settings
	 * @return client
	 * @throws MojoExecutionException
	 */
	protected GitHubClient createClient(String host, String userName, String password, String oauth2Token,
			String serverId, Settings settings) throws MojoExecutionException {
		GitHubClient client;
		if (!StringUtils.isEmpty(host)) {
			if (isDebug()) {
				debug("Using custom host: " + host);
			}
			client = createClient(host);
		} else {
			client = createClient();
		}

		Proxy proxy = getProxy(settings, serverId, host);
		if (null != proxy) {
			try {
				SettingsDecrypter settingsDecrypter = container.lookup(SettingsDecrypter.class);
				SettingsDecryptionResult result = settingsDecrypter
						.decrypt(new DefaultSettingsDecryptionRequest(proxy));
				proxy = result.getProxy();
			} catch (ComponentLookupException cle) {
				throw new MojoExecutionException("Unable to lookup SettingsDecrypter: " + cle.getMessage(), cle);
			}
		}

		if (null != proxy) {
			java.net.Proxy javaProxy = new java.net.Proxy(java.net.Proxy.Type.HTTP,
					new InetSocketAddress(proxy.getHost(), proxy.getPort()));
			if (isDebug()) {
				debug(MessageFormat.format("Found Proxy {0}:{1}", proxy.getHost(), proxy.getPort()));
			}
			if (client instanceof GitHubClientEgit) {
				GitHubClientEgit clientEgit = (GitHubClientEgit) client;
				if (isDebug())
					debug(MessageFormat.format("Use Proxy for Egit {0}", javaProxy));
				clientEgit.setProxy(javaProxy);
			}
		}

		if (configureUsernamePassword(client, userName, password) || configureOAuth2Token(client, oauth2Token)
				|| configureServerCredentials(client, serverId, settings)) {
			return client;
		} else {
			throw new MojoExecutionException("No authentication credentials configured");
		}
	}

	/**
	 * Create client
	 * <p>
	 * Subclasses can override to do any custom client configuration
	 *
	 * @param hostname
	 * @return non-null client
	 * @throws MojoExecutionException
	 */
	GitHubClient createClient(String hostname) throws MojoExecutionException {
		if (!hostname.contains("://"))
			return new RateLimitedGitHubClient(hostname);
		try {
			URL hostUrl = new URL(hostname);
			return new RateLimitedGitHubClient(hostUrl.getHost(), hostUrl.getPort(), hostUrl.getProtocol());
		} catch (MalformedURLException e) {
			throw new MojoExecutionException("Could not parse host URL " + hostname, e);
		}
	}

	/**
	 * Create client
	 * <p>
	 * Subclasses can override to do any custom client configuration
	 *
	 * @return non-null client
	 */
	GitHubClient createClient() {
		return new RateLimitedGitHubClient();
	}

	/**
	 * Configure credentials from configured username/password combination
	 *
	 * @param client
	 * @param userName
	 * @param password
	 * @return true if configured, false otherwise
	 */
	private boolean configureUsernamePassword(GitHubClient client, String userName, String password) {
		if (StringUtils.isEmpty(userName, password)) {
			return false;
		}
		if (isDebug()) {
			debug("Using basic authentication with username: " + userName);
		}
		client.setCredentials(userName, password);
		return true;
	}

	/**
	 * Configure credentials from configured OAuth2 token
	 *
	 * @param client
	 * @param oauth2Token
	 * @return true if configured, false otherwise
	 */
	private boolean configureOAuth2Token(GitHubClient client, String oauth2Token) {
		if (StringUtils.isEmpty(oauth2Token)) {
			return false;
		}
		if (isDebug()) {
			debug("Using OAuth2 access token authentication");
		}
		client.setOAuth2Token(oauth2Token);
		return true;
	}

	/**
	 * Configure client with credentials from given server id
	 *
	 * @param client
	 * @param serverId
	 * @param settings
	 * @return true if configured, false otherwise
	 * @throws MojoExecutionException
	 */
	private boolean configureServerCredentials(GitHubClient client, String serverId, Settings settings)
			throws MojoExecutionException {
		if (StringUtils.isEmpty(serverId)) {
			return false;
		}

		String serverUsername = null;
		String serverPassword = null;

		Server server = getServer(settings, serverId);
		if (server == null) {
			throw new MojoExecutionException(MessageFormat.format("Server ''{0}'' not found in settings", serverId));
		}
		if (isDebug()) {
			debug(MessageFormat.format("Using ''{0}'' server credentials", serverId));
		}
		try {
			SettingsDecrypter settingsDecrypter = container.lookup(SettingsDecrypter.class);
			SettingsDecryptionResult result = settingsDecrypter.decrypt(new DefaultSettingsDecryptionRequest(server));
			server = result.getServer();
		} catch (ComponentLookupException cle) {
			throw new MojoExecutionException("Unable to lookup SettingsDecrypter: " + cle.getMessage(), cle);
		}

		serverUsername = server.getUsername();
		serverPassword = server.getPassword();

		if (!StringUtils.isEmpty(serverUsername, serverPassword)) {
			if (isDebug()) {
				debug("Using basic authentication with username: " + serverUsername);
			}
			client.setCredentials(serverUsername, serverPassword);
			return true;
		}

		// A server password without a username is assumed to be an OAuth2 token
		if (!StringUtils.isEmpty(serverPassword)) {
			if (isDebug()) {
				debug("Using OAuth2 access token authentication");
			}
			client.setOAuth2Token(serverPassword);
			return true;
		}

		if (isDebug()) {
			debug(MessageFormat.format("Server ''{0}'' is missing username/password credentials", serverId));
		}
		return false;
	}

	/**
	 * Get repository and throw a {@link MojoExecutionException} on failures
	 *
	 * @param project
	 * @param owner
	 * @param name
	 * @return non-null repository id
	 * @throws MojoExecutionException
	 */
	protected RepositoryId getRepository(MavenProject project, String owner, String name)
			throws MojoExecutionException {
		RepositoryId repository = RepositoryUtils.getRepository(project, owner, name);
		if (repository == null)
			throw new MojoExecutionException("No GitHub repository (owner and name) configured");
		if (isDebug())
			debug(MessageFormat.format("Using GitHub repository {0}", repository.generateId()));
		return repository;
	}

	/**
	 * Get server with given id
	 *
	 * @param settings
	 * @param serverId must be non-null and non-empty
	 * @return server or null if none matching
	 */
	private Server getServer(Settings settings, String serverId) {
		if (settings == null)
			return null;
		List<Server> servers = settings.getServers();
		if (servers == null || servers.isEmpty()) {
			return null;
		}
		for (Server server : servers) {
			if (serverId.equals(server.getId())) {
				return server;
			}
		}
		return null;
	}

	/**
	 * Check hostname that matched nonProxy setting
	 *
	 * @param proxy    Maven Proxy. Must not null
	 * @param hostname
	 * @return matching result. true: match nonProxy
	 */
	boolean matchNonProxy(Proxy proxy, String hostname) {
		String host = hostname;

		if (null == hostname) {
			host = IGitHubConstants.HOST_DEFAULT;
		}

		// code from org.apache.maven.plugins.site.AbstractDeployMojo#getProxyInfo
		String nonProxyHosts = proxy.getNonProxyHosts();
		if (null != nonProxyHosts) {
			String[] nonProxies = nonProxyHosts.split("(,)|(;)|(\\|)");
			if (null != nonProxies) {
				for (String nonProxyHost : nonProxies) {
					if (null != nonProxyHost && nonProxyHost.contains("*")) {
						// Handle wildcard at the end, beginning or middle of the nonProxyHost
						int pos = nonProxyHost.indexOf('*');
						String nonProxyHostPrefix = nonProxyHost.substring(0, pos);
						String nonProxyHostSuffix = nonProxyHost.substring(pos + 1);
						// prefix*
						if (!StringUtils.isEmpty(nonProxyHostPrefix) && host.startsWith(nonProxyHostPrefix)
								&& StringUtils.isEmpty(nonProxyHostSuffix)) {
							return true;
						}
						// *suffix
						if (StringUtils.isEmpty(nonProxyHostPrefix) && !StringUtils.isEmpty(nonProxyHostSuffix)
								&& host.endsWith(nonProxyHostSuffix)) {
							return true;
						}
						// prefix*suffix
						if (!StringUtils.isEmpty(nonProxyHostPrefix) && host.startsWith(nonProxyHostPrefix)
								&& !StringUtils.isEmpty(nonProxyHostSuffix) && host.endsWith(nonProxyHostSuffix)) {
							return true;
						}
					} else if (host.equals(nonProxyHost)) {
						return true;
					}
				}
			}
		}

		return false;
	}

	/**
	 * Get proxy from settings
	 *
	 * @param settings
	 * @param serverId must be non-null and non-empty
	 * @param host     hostname
	 * @return proxy or null if none matching
	 */
	@VisibleForTesting
	Proxy getProxy(Settings settings, String serverId, String host) {
		if (settings == null) {
			return null;
		}
		List<Proxy> proxies = settings.getProxies();
		if (proxies == null || proxies.isEmpty()) {
			return null;
		}
		// search id match first
		if (serverId != null && !serverId.isEmpty()) {
			for (Proxy proxy : proxies) {
				if (proxy.isActive()) {
					String proxyId = proxy.getId();
					if (proxyId != null && !proxyId.isEmpty() && proxyId.equalsIgnoreCase(serverId)
							&& ("http".equalsIgnoreCase(proxy.getProtocol())
									|| "https".equalsIgnoreCase(proxy.getProtocol()))) {
						if (matchNonProxy(proxy, host)) {
							return null;
						} else {
							return proxy;
						}
					}
				}
			}
		}

		// search active proxy
		for (Proxy proxy : proxies) {
			if (proxy.isActive() && ("http".equalsIgnoreCase(proxy.getProtocol())
					|| "https".equalsIgnoreCase(proxy.getProtocol()))) {
				if (matchNonProxy(proxy, host))
					return null;
				else
					return proxy;
			}
		}
		return null;
	}
}
