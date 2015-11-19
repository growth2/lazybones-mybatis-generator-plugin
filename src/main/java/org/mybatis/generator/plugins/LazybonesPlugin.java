package org.mybatis.generator.plugins;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.mybatis.generator.api.IntrospectedColumn;
import org.mybatis.generator.api.IntrospectedTable;
import org.mybatis.generator.api.PluginAdapter;
import org.mybatis.generator.api.dom.java.FullyQualifiedJavaType;
import org.mybatis.generator.api.dom.java.Interface;
import org.mybatis.generator.api.dom.java.Method;
import org.mybatis.generator.api.dom.java.TopLevelClass;
import org.mybatis.generator.api.dom.xml.Attribute;
import org.mybatis.generator.api.dom.xml.Document;
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
				boolean isProcessed = false;

				Matcher m = PATTERN_SQLMAPS_PARAMETER.matcher(content);
				
				StringBuffer sb = new StringBuffer();
				while(m.find()) {
					String javaProperty = m.group(1);
					
					IntrospectedColumn introspectedColumn = findColumn(introspectedTable, javaProperty);
					if(introspectedColumn != null) {
						String defaultValue = introspectedColumn.getProperties().getProperty(propertyForReplace);
						if(defaultValue != null) {
							final String leadingSpace = "      ";
							StringBuilder replacement = new StringBuilder();
							replacement.append('\n').append(leadingSpace).append("<if test=\"").append(javaProperty).append(" != null\"" ).append(">");
							replacement.append('\n').append(leadingSpace).append("    ").append(m.group(0));
							replacement.append('\n').append(leadingSpace).append("</if>");
							replacement.append('\n').append(leadingSpace).append("<if test=\"").append(javaProperty).append(" == null\"" ).append(">");
							replacement.append('\n').append(leadingSpace).append("    ").append(defaultValue);
							replacement.append('\n').append(leadingSpace).append("</if>");
							m.appendReplacement(sb, replacement.toString());
						}
						else if("update".equals(propertyForReplace)) {
							String insertDefault = introspectedColumn.getProperties().getProperty("insert");
							if(insertDefault != null) {
								// 업데이트 기본값이 지정되어 있지만, 등록시 기본값이 있다면, 기본값을 할당 하지 않도록 한다.
								// 뭐 강제로 값이 할당되어 있으면, 할당하도록 해야지...
								isProcessed = true;
								
								if(commentOutMode) {
									TextElement newElement = new TextElement("<!-- " + content + " -->");
									result.add(newElement);
								}
								else {
									XmlElement newElement = new XmlElement("if");
									newElement.addAttribute(new Attribute("test", javaProperty +" != null"));
									newElement.addElement(new TextElement(content));
									result.add(newElement);
								}
								break;
							}
							else {
								// 업데이트시에 기본값이 할당되어 있어서, 이 값으로 대체 한다.
								m.appendReplacement(sb, m.group(0));
							}
						}
					}
					else {
						m.appendReplacement(sb, m.group(0));
					}
				}
				
				if(!isProcessed) {
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

	@Override
	public boolean sqlMapDocumentGenerated(Document document, IntrospectedTable introspectedTable) {
		String namespace = introspectedTable.getTableConfiguration().getProperty("namespace");
		if(namespace == null) {
			String namespaceSearchString = context.getSqlMapGeneratorConfiguration().getProperty("namespaceSearchString");
			if(namespaceSearchString != null) {
				String namespaceReplaceString = context.getSqlMapGeneratorConfiguration().getProperty("namespaceReplaceString");
				String a = getAttributeValue(document.getRootElement(), "namespace");
				Matcher m = Pattern.compile(namespaceSearchString).matcher(a);
				if(m.find()) {
					namespace = m.replaceAll(namespaceReplaceString);
				}
			}
		}
		if(namespace != null) {
			 XmlElement oldRoot = document.getRootElement();
			 XmlElement newRoot = new XmlElement("mapper");
			 newRoot.addAttribute(new Attribute("namespace", namespace));
			 for(Element element : oldRoot.getElements()) {
				 newRoot.addElement(element);
			 };
			 document.setRootElement(newRoot);
		};
		return true;
	}

	@Override
	public boolean modelGetterMethodGenerated(Method method, TopLevelClass topLevelClass,
			IntrospectedColumn introspectedColumn, IntrospectedTable introspectedTable, ModelClassType modelClassType) {
		boolean setFinal = Boolean.valueOf(getProperties().getProperty("setFinal", "false"));
		if(setFinal) method.setFinal(true);
		return true;
	}

	@Override
	public boolean modelSetterMethodGenerated(Method method, TopLevelClass topLevelClass,
			IntrospectedColumn introspectedColumn, IntrospectedTable introspectedTable, ModelClassType modelClassType) {
		boolean setFinal = Boolean.valueOf(getProperties().getProperty("setFinal", "false"));
		if(setFinal) method.setFinal(true);
		return true;
	}

	@Override
	public boolean modelBaseRecordClassGenerated(TopLevelClass topLevelClass, IntrospectedTable introspectedTable) {
		String impls = introspectedTable.getTableConfiguration().getProperty("implements");
		if(impls != null) {
			for(String i : impls.split(",")) {
				String name = i.trim(); 
				topLevelClass.addImportedType(name);
				topLevelClass.addSuperInterface(new FullyQualifiedJavaType(name));
				if(Serializable.class.getName().equals(name)) {
					topLevelClass.addAnnotation("@SuppressWarnings(\"serial\")");
				}
			}
		}
		return true;
	}

	@Override
	public void initialized(IntrospectedTable introspectedTable) {
		boolean useLowerActualColumnNames = Boolean.valueOf(getProperties().getProperty("useLowerActualColumnNames", "false"));
		if(useLowerActualColumnNames) {
			for(IntrospectedColumn c : introspectedTable.getAllColumns()) {
				c.setJavaProperty(c.getActualColumnName().toLowerCase());
			}
		}
	}

	@Override
	public boolean clientGenerated(Interface interfaze, TopLevelClass topLevelClass, IntrospectedTable introspectedTable) {
		boolean useRepositoryAnnotation = Boolean.valueOf(getProperties().getProperty("useRepositoryAnnotation", "false"));
		if(useRepositoryAnnotation) {
			interfaze.addImportedType(new FullyQualifiedJavaType("org.springframework.stereotype.Repository"));
			interfaze.addAnnotation(String.format("@Repository(\"%s\")", interfaze.getType().getFullyQualifiedName()));
		}
		return true;
	}
}
