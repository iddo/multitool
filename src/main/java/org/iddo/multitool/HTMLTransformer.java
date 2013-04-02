package org.iddo.multitool;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.validator.EmailValidator;
import org.apache.commons.validator.UrlValidator;
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
	private boolean removeDuplicates;
	private Map<String, String> duplicateMap;
	private boolean keepURLs;
	private boolean keepEmail;
	private Set<String> keptClasses;

	public HTMLTransformer(String pagePrefix, InputStream input, Properties properties, OutputStream output, String replacementFormat) throws IOException {
		this.pagePrefix = pagePrefix;
		this.properties = properties;
		this.outputStream = output;
		this.propNameCounters = new HashMap<String, Integer>();
		this.replacementFormat = replacementFormat;
		this.keepEmail = true;
		this.keepURLs = true;
		this.removeDuplicates = false;
		this.duplicateMap = new HashMap<String, String>();
		this.keptClasses = Collections.emptySet();

		// create an instance of HtmlCleaner
		HtmlCleaner cleaner = new HtmlCleaner();

		cleanerProps = cleaner.getProperties();
		// customize cleaner's behavior with property setters
		cleanerProps.setOmitDoctypeDeclaration(false);

		node = cleaner.clean(input);
	}

	/**
	 * @param removeDuplicates
	 *            Re-use keys if content is the same (default: false)
	 */
	public void setRemoveDuplicates(boolean removeDuplicates) {
		this.removeDuplicates = removeDuplicates;
	}

	/**
	 * 
	 * @param keepURLs
	 *            if set to true URL's will not be externelized (default: true)
	 */
	public void setKeepURLs(boolean keepURLs) {
		this.keepURLs = keepURLs;
	}

	/**
	 * 
	 * @param keepEmails
	 *            if set to true Emails will not be externelized (default: true)
	 */
	public void setKeepEmails(boolean keepEmails) {
		this.keepEmail = keepEmails;
	}

	/**
	 * 
	 * @param classes
	 *            html classes to not externelize
	 */
	public void setKeptClasses(Set<String> classes) {
		this.keptClasses = classes;
	}

	public void transform() {
		for (TagNode tagNode : node.getChildTags()) {
			transform(tagNode);
		}
	}

	public static void main(String[] args) throws IOException {
		if (args.length != 3) {
			System.out.println("Syntax: HTMLTransformer <input file> <key prefix> <replacement format>");
			System.out.println("Example: HTMLTransformer input.html mypage '<spring:message code=\"{0}\" />'");
			return;
		}
		transform(new File(args[0]), args[1], args[2]);
	}

	public static void transform(File inputFile, String name, String keyFormat) throws IOException {
		Properties props = new Properties();

		String filename = inputFile.getName();
		String baseName = FilenameUtils.getBaseName(filename);
		String ext = FilenameUtils.getExtension(filename);

		OutputStream transformedFileOS = null;
		OutputStream propertiesFileOS = null;
		InputStream is = null;
		HTMLTransformer htmlTransformer = null;
		try {
			transformedFileOS = new FileOutputStream(baseName + ".multi." + ext);
			is = new FileInputStream(inputFile);
			htmlTransformer = new HTMLTransformer(name, is, props, transformedFileOS, keyFormat);
			htmlTransformer.transform();

			propertiesFileOS = new FileOutputStream(baseName + ".properties");
			props.store(propertiesFileOS, name);
		} finally {
			IOUtils.closeQuietly(propertiesFileOS);
			IOUtils.closeQuietly(htmlTransformer);
			IOUtils.closeQuietly(is);
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
					if (keepEmail && isEmail(content)) {
						// Skip email
					} else if (keepURLs && isUrl(content)) {
						// Skip url
					} else {
						String tagClass = currentTag.getAttributeByName("class");
						if (tagClass != null && keptClasses.contains(tagClass)) {
							// Skip tag
						} else {
							String key;
							if (removeDuplicates && duplicateMap.containsKey(content)) {
								key = duplicateMap.get(content);
							} else {
								key = addProp(currentTag.getName(), content);
								if (removeDuplicates) {
									duplicateMap.put(content, key);
								}
							}
							currentTag.replaceChild(cn, new ContentNode(MessageFormat.format(replacementFormat, key)));
						}
					}
				}
			}
		}
	}

	private boolean isEmail(String content) {
		return EmailValidator.getInstance().isValid(content);
	}

	private boolean isUrl(String content) {
		UrlValidator urlValidator = new UrlValidator();
		return urlValidator.isValid(content);
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
