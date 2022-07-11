package com.github.maven.plugins.core;

import static java.lang.Math.max;
import static java.lang.System.currentTimeMillis;

import java.io.IOException;
import java.net.HttpURLConnection;

import com.github.maven.plugins.core.egit.GitHubClientEgit;
import com.google.common.util.concurrent.RateLimiter;

public class RateLimitedGitHubClient extends GitHubClientEgit {

	private volatile RateLimiter rateLimiter; // NOSONAR

	public RateLimitedGitHubClient() {
		super();
	}

	public RateLimitedGitHubClient(String hostname) {
		super(hostname);
	}

	public RateLimitedGitHubClient(String hostname, int port, String scheme) {
		super(hostname, port, scheme);
	}

	@Override
	protected HttpURLConnection createPost(String uri) throws IOException {
		rateLimiter().acquire();
		return super.createPost(uri);
	}

	@Override
	protected HttpURLConnection createPut(String uri) throws IOException {
		rateLimiter().acquire();
		return super.createPut(uri);
	}

	private RateLimiter rateLimiter() {
		if (this.rateLimiter != null) {
			return this.rateLimiter;
		}

		return initializeRateLimiter();
	}

	private synchronized RateLimiter initializeRateLimiter() {
		if (this.rateLimiter != null) {
			return this.rateLimiter;
		}

		HttpURLConnection connection = null;

		try {
			// Query rate limit.
			connection = createGet("/rate_limit");

			int remaining = connection.getHeaderFieldInt("X-RateLimit-Remaining", -1);
			int reset = connection.getHeaderFieldInt("X-RateLimit-Reset", -1);
			int now = (int) (currentTimeMillis() / 1000);

			// Calculate the sustained request rate until the limits are reset.
			this.rateLimiter = RateLimiter.create((double) remaining / max(reset - now, 1));
			return this.rateLimiter;
		} catch (Exception e) {
			// Fall back to 20 requests per minute.
			//
			// As per https://github.com/octokit/octokit.net/issues/638#issuecomment-67795998,
			// it seems that GitHub only allow 20 API calls per 1-minute period
			this.rateLimiter = RateLimiter.create(20. / 60.);
			return this.rateLimiter;
		} finally {
			if (connection != null) {
				connection.disconnect();
			}
		}
	}
}
