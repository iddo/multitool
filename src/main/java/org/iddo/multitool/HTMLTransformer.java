package org.iddo.multitool;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang.StringUtils;
import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.ContentNode;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.PrettyHtmlSerializer;
import org.htmlcleaner.TagNode;

public class HTMLTransformer implements Closeable {
	private String pagePrefix;
	private Properties properties;
	private OutputStream outputStream;
	private CleanerProperties cleanerProps;
	private TagNode node;

	private Map<String, Integer> propNameCounters;
	private String replacementFormat;

	public HTMLTransformer(String pagePrefix, InputStream input, Properties properties, OutputStream output, String replacementFormat) throws IOException {
		this.pagePrefix = pagePrefix;
		this.properties = properties;
		this.outputStream = output;
		this.propNameCounters = new HashMap<String, Integer>();
		this.replacementFormat = replacementFormat;

		// create an instance of HtmlCleaner
		HtmlCleaner cleaner = new HtmlCleaner();

		cleanerProps = cleaner.getProperties();
		// customize cleaner's behaviour with property setters
		cleanerProps.setOmitDoctypeDeclaration(false);

		node = cleaner.clean(input);

	}

	public void transform() {
		for (TagNode tagNode : node.getChildTags()) {
			transform(tagNode);
		}
	}

	private void transform(TagNode currentTag) {
		for (Object o : currentTag.getChildren()) {
			if (o instanceof TagNode) {
				transform((TagNode) o);
			} else if (o instanceof ContentNode) {
				ContentNode cn = (ContentNode) o;
				String content = StringUtils.trimToNull(cn.getContent().toString());
				if (content != null) {
					String key = addProp(currentTag.getName(), content);
					currentTag.replaceChild(cn, new ContentNode(MessageFormat.format(replacementFormat, key)));
				}
			}
		}
	}

	private String addProp(String optionalPropName, String content) {
		StringBuilder baseKey = new StringBuilder(pagePrefix);
		if (baseKey.length() > 0) {
			baseKey.append(".");
		}
		if (optionalPropName != null) {
			baseKey.append(optionalPropName);
			baseKey.append(".");
		}
		baseKey.append("text");
		Integer counter = propNameCounters.get(baseKey.toString());
		if (counter == null) {
			counter = 1;
		}
		String key = baseKey.toString() + counter;
		properties.setProperty(key, content);
		propNameCounters.put(baseKey.toString(), counter.intValue() + 1);
		return key;
	}

	public void close() throws IOException {
		// serialize a node to a file, output stream, DOM, JDom...
		new PrettyHtmlSerializer(cleanerProps).writeToStream(node, outputStream, "UTF-8", false);
	}
}
