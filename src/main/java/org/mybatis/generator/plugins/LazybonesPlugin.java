package org.mybatis.generator.plugins;

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
	
	private final Pattern PATTERN_SQLMAPS_PARAMETER = Pattern.compile("#\\{(?:[\\w]+\\.)?([\\w]+)[^\\}]*\\}\\s*,?");
	private final Pattern PATTERN_IF_TEST_ATTRIBUTE = Pattern.compile("(?:[\\w]+\\.)?([\\w]+)");
	private final Pattern PATTERN_PART_OF_COLUMN_NAMES = Pattern.compile("insert\\s.+\\(", Pattern.CASE_INSENSITIVE);
	private final Pattern PATTTERN_COLUMNS = Pattern.compile("([A-Za-z_가-힣][\\w가-힣]+),?");
	private final String leadingSpace = "      ";

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
	
	private void replaceParameterWithDefault(XmlElement element, IntrospectedTable introspectedTable, String insertOrUpdate) {
		boolean isPartOfColumnNames = false;
		boolean commentOutMode = Boolean.valueOf(getProperties().getProperty("commentOut", "false"));

		List<Element> result = new ArrayList<Element>();
		for(Element child : element.getElements()) {
			if(child instanceof TextElement) {
				String content = ((TextElement)child).getContent();
				StringBuffer sb = new StringBuffer();
				boolean isProcessed = false;
				
				if(!isPartOfColumnNames) {
					Matcher m = PATTERN_PART_OF_COLUMN_NAMES.matcher(content);
					if(m.find()) {
						isPartOfColumnNames = true;
						// insert into table (
						sb.append(content.substring(0, m.end()));
						// list of column names
						String s = content.substring(m.end());
						sb.append(replaceInsert(introspectedTable, s, insertOrUpdate));
					}
				}
				else {
					String n = replaceInsert(introspectedTable, content, insertOrUpdate);
					sb.append(n);
				}
				if(isPartOfColumnNames) {
					if(content.trim().endsWith(")")) {
						isPartOfColumnNames = false;
					}
				}
				else {

					Matcher m = PATTERN_SQLMAPS_PARAMETER.matcher(content);
					
					while(m.find()) {
						String javaProperty = m.group(1);
						
						IntrospectedColumn introspectedColumn = findColumn(introspectedTable, javaProperty);
						if(introspectedColumn != null) {
							String columnDefault = getColumnDefault(introspectedColumn);
							String configDefault = getConfigDefault(introspectedColumn, insertOrUpdate);
							
							if(columnDefault != null && configDefault == null) {
								StringBuilder r = new StringBuilder();
								r.append('\n').append(leadingSpace).append("<if test=\"").append(javaProperty).append(" != null\"" ).append(">");
								r.append('\n').append(leadingSpace).append("    <!-- DEFAULT ").append(columnDefault).append(" -->");
								r.append('\n').append(leadingSpace).append("    ").append(m.group(0));
								r.append('\n').append(leadingSpace).append("</if>");
								m.appendReplacement(sb, r.toString());
							}
							else if(configDefault != null) {
								StringBuilder r = new StringBuilder();
								r.append('\n').append(leadingSpace).append("<if test=\"").append(javaProperty).append(" != null\"" ).append(">");
								r.append('\n').append(leadingSpace).append("    ").append(m.group(0));
								r.append('\n').append(leadingSpace).append("</if>");
								r.append('\n').append(leadingSpace).append("<if test=\"").append(javaProperty).append(" == null\"" ).append(">");
								r.append('\n').append(leadingSpace).append("    ").append(configDefault);
								if(m.group(0).trim().endsWith(",")) {
									r.append(',');
								}
								r.append('\n').append(leadingSpace).append("</if>");
								m.appendReplacement(sb, r.toString());
							}
							else if("update".equals(insertOrUpdate)) {
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
					m.appendTail(sb);
				}
				
				if(!isProcessed) {
					
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
	
	private String replaceInsert(IntrospectedTable introspectedTable, String text, String insertOrUpdate) {
		StringBuffer sb = new StringBuffer();
		// 정규식으로 변수를 찾는다.
		Matcher m = PATTTERN_COLUMNS.matcher(text);
		while(m.find()) {
			String var = m.group(1);
			
			IntrospectedColumn introspectedColumn = findColumnBy(introspectedTable, var);
			if(introspectedColumn != null) {
				String javaProperty = introspectedColumn.getJavaProperty();
				String columnDefault = getColumnDefault(introspectedColumn);
				String configDefault = getConfigDefault(introspectedColumn, insertOrUpdate);
				if(columnDefault != null && configDefault == null) {
					StringBuilder r = new StringBuilder();
					r.append('\n').append(leadingSpace).append("<if test=\"").append(javaProperty).append(" != null\"" ).append(">");
					r.append('\n').append(leadingSpace).append("    <!-- DEFAULT ").append(columnDefault).append(" -->");
					r.append('\n').append(leadingSpace).append("    ").append(m.group(0));
					r.append('\n').append(leadingSpace).append("</if>");
					m.appendReplacement(sb, r.toString());
				}
				else {
					m.appendReplacement(sb, m.group(0));				
				}
			}
			else {
				m.appendReplacement(sb, m.group(0));
			}
		}
		m.appendTail(sb);
		return sb.toString();
	}

	private IntrospectedColumn findColumn(IntrospectedTable introspectedTable, String javaProperty) {
		for(IntrospectedColumn introspectedColumn : introspectedTable.getAllColumns()) {
			if(introspectedColumn.getJavaProperty().equals(javaProperty)) return introspectedColumn;
		}
		return null;
	}

	private IntrospectedColumn findColumnBy(IntrospectedTable introspectedTable, String column) {
		for(IntrospectedColumn introspectedColumn : introspectedTable.getAllColumns()) {
			if(introspectedColumn.getActualColumnName().equals(column)) return introspectedColumn;
		}
		return null;
	}
	
	private String getColumnDefault(IntrospectedColumn introspectedColumn) {
		String defaultValue = introspectedColumn.getDefaultValue();
		if("NULL".equalsIgnoreCase(defaultValue)) {
			return null;
		}
		return defaultValue;
	}
	private String getConfigDefault(IntrospectedColumn introspectedColumn, String insertOrUpdate) {
		String defaultValue = introspectedColumn.getProperties().getProperty(insertOrUpdate);
		if("NULL".equalsIgnoreCase(defaultValue)) {
			return null;
		}
		return defaultValue;
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
			String a = getAttributeValue(document.getRootElement(), "namespace");
			namespace = replaceNamespace(a);
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
		String impls = getProperty(introspectedTable, "implements");
		if(impls != null) {
			for(String i : impls.split(",")) {
				String name = i.trim(); 
				topLevelClass.addImportedType(name);
				topLevelClass.addSuperInterface(new FullyQualifiedJavaType(name));
			}
		}
		String superClass = getProperty(introspectedTable, "extends");
		if(superClass != null) {
			topLevelClass.addImportedType(superClass);
			topLevelClass.setSuperClass(new FullyQualifiedJavaType(superClass));
		}

		String annotation = getProperty(introspectedTable, "annotation");
		if(annotation != null) {
			topLevelClass.addAnnotation(annotation);
		}
		return true;
	}
	
	private String getProperty(IntrospectedTable introspectedTable, String propertyName) {
		String value = introspectedTable.getTableConfiguration().getProperty(propertyName);
		if(value == null) {
			value = getProperties().getProperty(propertyName);
		}
		return value;
	}
	
	@Override
	public void initialized(IntrospectedTable introspectedTable) {
		boolean useLowerActualColumnNames = Boolean.valueOf(getProperties().getProperty("useLowerActualColumnNames", "false"));
		if(useLowerActualColumnNames) {
			for(IntrospectedColumn c : introspectedTable.getAllColumns()) {
				c.setJavaProperty(c.getActualColumnName().toLowerCase());
			}
		}
		introspectedTable.setRules(new LazybonesRules(introspectedTable.getRules(), getProperties()));
	}

	@Override
	public boolean clientGenerated(Interface interfaze, TopLevelClass topLevelClass, IntrospectedTable introspectedTable) {
		boolean useRepositoryAnnotation = Boolean.valueOf(getProperties().getProperty("useRepositoryAnnotation", "false"));
		if(useRepositoryAnnotation) {
			interfaze.addImportedType(new FullyQualifiedJavaType("org.springframework.stereotype.Repository"));
			String repository = interfaze.getType().getFullyQualifiedName();
			interfaze.addAnnotation(String.format("@Repository(\"%s\")", repository));
		}
		return true;
	}
	
	private String replaceNamespace(String namespace) {
		String namespaceSearchString = context.getSqlMapGeneratorConfiguration().getProperty("namespaceSearchString");
		if(namespaceSearchString != null) {
			String namespaceReplaceString = context.getSqlMapGeneratorConfiguration().getProperty("namespaceReplaceString");
			Matcher m = Pattern.compile(namespaceSearchString).matcher(namespace);
			if(m.find()) {
				return m.replaceAll(namespaceReplaceString);
			}
		}
		return namespace;
	}

//	private void log(String format, Object... args) {
//		System.out.printf(format + "\n", args);
//	}
}
