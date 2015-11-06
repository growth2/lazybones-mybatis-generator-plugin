package org.mybatis.generator.plugins;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.mybatis.generator.api.IntrospectedColumn;
import org.mybatis.generator.api.IntrospectedTable;
import org.mybatis.generator.api.PluginAdapter;
import org.mybatis.generator.api.dom.xml.Attribute;
import org.mybatis.generator.api.dom.xml.Element;
import org.mybatis.generator.api.dom.xml.TextElement;
import org.mybatis.generator.api.dom.xml.XmlElement;
import org.mybatis.generator.config.Context;

public class LazybonesPlugin extends PluginAdapter {
	
	private final Pattern PATTERN_SQLMAPS_PARAMETER = Pattern.compile("#\\{(?:[\\w]+\\.)?([\\w]+)[^\\}]*\\}");
	private final Pattern PATTERN_IF_TEST_ATTRIBUTE = Pattern.compile("(?:[\\w]+\\.)?([\\w]+)");

	public LazybonesPlugin() {
		super();
	}
	
	private static Context context;

	public boolean validate(List<String> warnings) {
		return true;
	}

	@Override
	public void setContext(Context context) {
		super.setContext(context);
		LazybonesPlugin.context = context;
	}

	@Override
	public boolean sqlMapInsertElementGenerated(XmlElement element, IntrospectedTable introspectedTable) {
		replaceParameterWithDefault(element, introspectedTable, "insert");
		return true;
	}
	
	private void replaceParameterWithDefault(XmlElement element, IntrospectedTable introspectedTable, String propertyForReplace) {
		boolean commentOutMode = Boolean.valueOf(getProperties().getProperty("commentOut", "false"));

		List<Element> result = new ArrayList<Element>();
		for(Element child : element.getElements()) {
			if(child instanceof TextElement) {
				String content = ((TextElement)child).getContent();
				boolean isDeleted = false;

				Matcher m = PATTERN_SQLMAPS_PARAMETER.matcher(content);
				
				StringBuffer sb = new StringBuffer();
				while(m.find()) {
					String javaProperty = m.group(1);
					
					IntrospectedColumn introspectedColumn = findColumn(introspectedTable, javaProperty);
					if(introspectedColumn != null) {
						String defaultValue = introspectedColumn.getProperties().getProperty(propertyForReplace);
						if(defaultValue != null) {
							m.appendReplacement(sb, defaultValue);
						}
						else if("update".equals(propertyForReplace)) {
							String insertDefault = introspectedColumn.getProperties().getProperty("insert");
							if(insertDefault != null) {
								isDeleted = true;
								break;
							}
							else {
								m.appendReplacement(sb, m.group(0));
							}
						}
					}
					else {
						m.appendReplacement(sb, m.group(0));
					}
				}
				
				if(isDeleted) {
					if(commentOutMode) {
						TextElement newTextElement = new TextElement("<!-- " + content + " -->");
						result.add(newTextElement);
					}
					else {
						//
					}
				}
				else {
					m.appendTail(sb);
					content = sb.toString();
					TextElement newTextElement = new TextElement(content);
					result.add(newTextElement);
				}
			}
			else {
				result.add(child);
			}
		}
		element.getElements().clear();
		element.getElements().addAll(result);
	}

	private IntrospectedColumn findColumn(IntrospectedTable introspectedTable, String javaProperty) {
		for(IntrospectedColumn introspectedColumn : introspectedTable.getAllColumns()) {
			if(introspectedColumn.getJavaProperty().equals(javaProperty)) return introspectedColumn;
		}
		return null;
	}

	@Override
	public boolean sqlMapUpdateByExampleSelectiveElementGenerated(XmlElement element, IntrospectedTable introspectedTable) {
		stripIfTagAndReplaceParameterWithDefault(element, introspectedTable, "update");
		return true;
	}

	@Override
	public boolean sqlMapUpdateByExampleWithBLOBsElementGenerated(XmlElement element, IntrospectedTable introspectedTable) {
		replaceParameterWithDefault(element, introspectedTable, "update");
		return true;
	}

	@Override
	public boolean sqlMapUpdateByExampleWithoutBLOBsElementGenerated(XmlElement element, IntrospectedTable introspectedTable) {
		replaceParameterWithDefault(element, introspectedTable, "update");
		return true;
	}

	@Override
	public boolean sqlMapUpdateByPrimaryKeySelectiveElementGenerated(XmlElement element, IntrospectedTable introspectedTable) {
		stripIfTagAndReplaceParameterWithDefault(element, introspectedTable, "update");
		return true;
	}

	@Override
	public boolean sqlMapUpdateByPrimaryKeyWithBLOBsElementGenerated(XmlElement element, IntrospectedTable introspectedTable) {
		replaceParameterWithDefault(element, introspectedTable, "update");
		return true;
	}

	@Override
	public boolean sqlMapUpdateByPrimaryKeyWithoutBLOBsElementGenerated(XmlElement element, IntrospectedTable introspectedTable) {
		replaceParameterWithDefault(element, introspectedTable, "update");
		return true;
	}

	/**
	 * 원래의 <if test="property != null"/>은 그대로 두고,
	 * 아래쪽에 <if test="property == null"/> 조건으로 기본값을 삽입하도록 할것.
	 */
	@Override
	public boolean sqlMapInsertSelectiveElementGenerated(XmlElement element, IntrospectedTable introspectedTable) {
		stripIfTagAndReplaceParameterWithDefault(element, introspectedTable, "insert");
		return true;
	}

	private void stripIfTagAndReplaceParameterWithDefault(XmlElement element, IntrospectedTable introspectedTable, String propertyForReplace) {
		List<Element> result = new ArrayList<Element>();
		
		for(Element child : element.getElements()) {
			result.add(child);
			
			if(child instanceof XmlElement) {
				XmlElement xmlChild = (XmlElement)child;
				if("if".equals(xmlChild.getName())) {
					String test = getAttributeValue(xmlChild, "test");
					Matcher m = PATTERN_IF_TEST_ATTRIBUTE.matcher(test);
					if(m.find()) {
						String javaProperty = m.group(1);
						IntrospectedColumn introspectedColumn = findColumn(introspectedTable, javaProperty);
						if(introspectedColumn != null) {
							String defaultValue = introspectedColumn.getProperties().getProperty(propertyForReplace);
							if(defaultValue != null) {
								XmlElement ifElement = new XmlElement("if");
								ifElement.addAttribute(new Attribute("test", test.replace("!=", "==")));
								String content = String.format("%s = %s,", introspectedColumn.getActualColumnName(), defaultValue);
								ifElement.getElements().add(new TextElement(content));
								result.add(ifElement);
							}
						}
					}
				}
				else {
					stripIfTagAndReplaceParameterWithDefault(xmlChild, introspectedTable, propertyForReplace);
				}
			}
		}
		element.getElements().clear();
		element.getElements().addAll(result);
	}	

	private String getAttributeValue(XmlElement element, String attributeName) {
		for(Attribute a : element.getAttributes()) {
			if(attributeName.equals(a.getName())) return a.getValue(); 
		}
		return null;
	}

	public static Context getPluginContext() {
		if(context == null) throw new IllegalStateException("context not initialized.");
		return context;
	}
}
