package org.iddo.multitool;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Properties;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

public class HTMLTransformerTest {

	@Test
	public void test() throws IOException {
		Properties props = new Properties();
		OutputStream os = System.out;
		HTMLTransformer htmlTransformer = null;
		try {
			htmlTransformer = new HTMLTransformer("testpage", this.getClass().getClassLoader().getResourceAsStream("test.html"), props, os,
					"<spring:message code=\"{0}\" />");
			htmlTransformer.transform();

			props.store(os, "Test props");
		} finally {
			IOUtils.closeQuietly(htmlTransformer);
		}
	}

}
