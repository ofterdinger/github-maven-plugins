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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Unit tests of {@link StringUtils}
 *
 * @author Kevin Sawicki (kevin@github.com)
 */
class StringUtilsTest {

	/**
	 * Tests of {@link StringUtils#isEmpty(String...)}
	 */
	@Test
	void isEmpty() {
		assertTrue(StringUtils.isEmpty((String[]) null));
		assertTrue(StringUtils.isEmpty(new String[0]));
		assertTrue(StringUtils.isEmpty((String) null));
		assertTrue(StringUtils.isEmpty(""));
		assertTrue(StringUtils.isEmpty("content", null));
		assertTrue(StringUtils.isEmpty("content", ""));
		assertFalse(StringUtils.isEmpty("content"));
	}

	/**
	 * Tests of {@link StringUtils#removeEmpties(String...)}
	 */
	@Test
	void removeEmpties() {
		assertArrayEquals(new String[0], StringUtils.removeEmpties((String[]) null));
		assertArrayEquals(new String[0], StringUtils.removeEmpties((String) null));
		assertArrayEquals(new String[0], StringUtils.removeEmpties(""));
		assertArrayEquals(new String[] { "content" }, StringUtils.removeEmpties("", "content"));
		assertArrayEquals(new String[] { "content" }, StringUtils.removeEmpties(null, "content"));
	}

}
