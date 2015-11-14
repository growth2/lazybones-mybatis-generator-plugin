package org.mybatis.generator.plugins;

import static org.mybatis.generator.internal.util.StringUtility.isTrue;

import java.sql.Connection;
import java.sql.ResultSet;
import java.text.MessageFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;

import org.mybatis.generator.api.CommentGenerator;
import org.mybatis.generator.api.IntrospectedColumn;
import org.mybatis.generator.api.IntrospectedTable;
import org.mybatis.generator.api.dom.java.Field;
import org.mybatis.generator.api.dom.java.InnerClass;
import org.mybatis.generator.api.dom.java.InnerEnum;
import org.mybatis.generator.api.dom.java.JavaElement;
import org.mybatis.generator.api.dom.java.Method;
import org.mybatis.generator.api.dom.xml.TextElement;
import org.mybatis.generator.api.dom.xml.XmlElement;
import org.mybatis.generator.config.Context;
import org.mybatis.generator.config.MergeConstants;
import org.mybatis.generator.config.PropertyRegistry;
import org.mybatis.generator.internal.DefaultCommentGenerator;
import org.mybatis.generator.internal.db.ConnectionFactory;
import org.mybatis.generator.logging.Log;
import org.mybatis.generator.logging.LogFactory;

/**
 * @author Jeff Butler
 * 
 */
public class LazybonesCommentGenerator extends DefaultCommentGenerator implements CommentGenerator {
	private Log log = LogFactory.getLog(getClass());
	private ResourceBundle messages;
	private boolean suppressDate;
	private boolean suppressAllComments;
	private Map<String,String> mapOfTableRemarks;


	public LazybonesCommentGenerator() {
		
		super();
		suppressDate = false;
		suppressAllComments = false;

		Locale l = Locale.getDefault();
		String language = System.getProperty("user.language", l.getLanguage());
		String country = System.getProperty("user.country", l.getCountry());
		String variant = System.getProperty("user.variant", l.getVariant());
		Locale newLocale = new Locale(language, country, variant);
		Locale.setDefault(newLocale);
		messages = ResourceBundle.getBundle("org.mybatis.generator.plugins.messages");

		loadTableRemarks();
	}

	@Override
	public void addComment(XmlElement xmlElement) {
		if(suppressAllComments) return;
		
		xmlElement.addElement(new TextElement("<!--")); 
		for(String line : getMessageLines("warn.comment", MergeConstants.NEW_ELEMENT_TAG)) {
			xmlElement.addElement(new TextElement("  " + line));
		}
		if(! suppressDate) {
			for(String line : getMessageLines("warn.comment.date", new Date())) {
				xmlElement.addElement(new TextElement("  " + line));
			}
		}
		xmlElement.addElement(new TextElement("-->")); 
	}

	@Override
	public void addConfigurationProperties(Properties properties) {
		super.addConfigurationProperties(properties);
		suppressDate = isTrue(properties.getProperty(PropertyRegistry.COMMENT_GENERATOR_SUPPRESS_DATE));
		suppressAllComments = isTrue(properties.getProperty(PropertyRegistry.COMMENT_GENERATOR_SUPPRESS_ALL_COMMENTS));
	}

	@Override
	protected void addJavadocTag(JavaElement javaElement, boolean markAsDoNotDelete) {
		javaElement.addJavaDocLine(" *"); 
		StringBuilder sb = new StringBuilder();
		sb.append(" * "); 
		sb.append(MergeConstants.NEW_ELEMENT_TAG);
		if(markAsDoNotDelete) {
			sb.append(" do_not_delete_during_merge"); 
		}
		if(! suppressDate) {
			sb.append(' ');
			sb.append(getMessage("date.format", new Date()));
		}
		javaElement.addJavaDocLine(sb.toString());
	}

	@Override
	public void addClassComment(InnerClass innerClass, IntrospectedTable introspectedTable) {
		if(suppressAllComments) return;
		
		innerClass.addJavaDocLine("/**");
		for(String line : getMessageLines("class.comment", args(introspectedTable))) {
			innerClass.addJavaDocLine(" * " + line);
		}
		addJavadocTag(innerClass, false);
		innerClass.addJavaDocLine(" */");
	}

	@Override
	public void addEnumComment(InnerEnum innerEnum, IntrospectedTable introspectedTable) {
		if(suppressAllComments) return;
		
		innerEnum.addJavaDocLine("/**");
		for(String line : getMessageLines("enum.comment", args(introspectedTable))) {
			innerEnum.addJavaDocLine(" * " + line);
		}
		addJavadocTag(innerEnum, false);
		innerEnum.addJavaDocLine(" */"); 
	}

	@Override
	public void addFieldComment(Field field, IntrospectedTable introspectedTable, IntrospectedColumn introspectedColumn) {
		if(suppressAllComments) return;
		
		field.addJavaDocLine("/**"); 
		for(String line : getMessageLines("field.comment.column", args(introspectedTable, introspectedColumn))) {
			field.addJavaDocLine(" * " + line);
		}
		addJavadocTag(field, false);
		field.addJavaDocLine(" */"); 
	}

	@Override
	public void addFieldComment(Field field, IntrospectedTable introspectedTable) {
		if(suppressAllComments) return;
		
		field.addJavaDocLine("/**"); 
		for(String line : getMessageLines("field.comment", args(introspectedTable))) {
			field.addJavaDocLine(" * " + line);
		}
		addJavadocTag(field, false);
		field.addJavaDocLine(" */"); 
	}

	@Override
	public void addGeneralMethodComment(Method method, IntrospectedTable introspectedTable) {
		if(suppressAllComments) return;
		
		method.addJavaDocLine("/**"); 
		for(String line : getMessageLines("field.comment", args(introspectedTable))) {
			method.addJavaDocLine(" * " + line);
		}
		addJavadocTag(method, false);
		method.addJavaDocLine(" */"); 
	}

	@Override
	public void addGetterComment(Method method, IntrospectedTable introspectedTable, IntrospectedColumn introspectedColumn) {
		if(suppressAllComments) return;
		
		method.addJavaDocLine("/**");
		for(String line : getMessageLines("getter.comment", args(introspectedTable, introspectedColumn))) {
			method.addJavaDocLine(" * " + line);
		}
		method.addJavaDocLine(" *"); 
		for(String line : getMessageLines("return", args(introspectedTable, introspectedColumn))) {
			method.addJavaDocLine(" * " + line);
		}
		addJavadocTag(method, false);
		method.addJavaDocLine(" */"); 
	}

	@Override
	public void addSetterComment(Method method, IntrospectedTable introspectedTable, IntrospectedColumn introspectedColumn) {
		if(suppressAllComments) return;
		
		method.addJavaDocLine("/**"); 
		
		Object[] args = args(introspectedTable, introspectedColumn);
		
		for(String line : getMessageLines("setter.comment", args)) {
			method.addJavaDocLine(" * " + line);
		}
		method.addJavaDocLine(" *"); 
		
		// Parameter parm = method.getParameters().get(0);

		for(String line : getMessageLines("param", args)) {
			method.addJavaDocLine(" * " + line);
		}
		addJavadocTag(method, false);
		method.addJavaDocLine(" */");
	}
	
	private String toJdbcTypeName(IntrospectedColumn c) {
		StringBuilder sb = new StringBuilder();
		sb.append(c.getJdbcTypeName());
//		switch(introspectedColumn.getJdbcType()) {
//		case java.sql.Types.DECIMAL
		
		if(c.getLength()>0) {
			sb.append('(').append(c.getLength());
			if(c.getScale()>0) {
				sb.append(',').append(c.getScale());
			}
			sb.append(')');
		}
		return sb.toString();
	}

	@Override
	public void addClassComment(InnerClass innerClass, IntrospectedTable introspectedTable, boolean markAsDoNotDelete) {
		if(suppressAllComments) return;

		innerClass.addJavaDocLine("/**"); 
		for(String line : getMessageLines("class.comment", args(introspectedTable))) {
			innerClass.addJavaDocLine(" * " + line);
		}
		addJavadocTag(innerClass, markAsDoNotDelete);
		innerClass.addJavaDocLine(" */"); 
	}

	private void loadTableRemarks() {
		if(mapOfTableRemarks != null) return;
		
		log.debug("Loading table remarks.");

		mapOfTableRemarks = new HashMap<String,String>();
		
		Context context = LazybonesPlugin.getPluginContext();
		Connection c = null;
		try {
			c = ConnectionFactory.getInstance().getConnection(context.getJdbcConnectionConfiguration());
			ResultSet rs = c.getMetaData().getTables(null, null, null, null);
			while(rs.next()) {
				String remarks = rs.getString("REMARKS");
				if(remarks != null) {
					mapOfTableRemarks.put(rs.getString("TABLE_NAME"), remarks);
				}
			}
			rs.close();
		}
		catch(Exception e) {
			throw new RuntimeException(e.getMessage(), e);
		}
		finally {
			if(c!=null) {
				try {
					c.close();
				}
				catch(Exception ignore) {
					//
				}
			}
		}
	}

	private String getTableRemarks(String tableName) {
		if(mapOfTableRemarks == null) throw new NullPointerException(LazybonesPlugin.class.getName() + " not initialized.");
		return this.mapOfTableRemarks.get(tableName);
	}

	private String getMessage(String key, Object...args) {
		try {
			String message = messages.getString(key);
			String result = MessageFormat.format(message, args);
			return result;
		}
		catch(Exception e) {
			throw new RuntimeException(String.format("failed to get a message '%s'", key), e);
		}
	}

	private String[] getMessageLines(String key, Object...args) {
		return getMessage(key, args).split("\\r?\\n");
	}

	
	private Object[] args(IntrospectedTable introspectedTable) {
		String name = introspectedTable.getFullyQualifiedTableNameAtRuntime().toString();
		String comment = getTableRemarks(introspectedTable.getFullyQualifiedTable().toString());
		return new Object[] { 
				name, // 0
				comment==null ? 0 : 1, comment // 1, 2 
			};
	}

	private Object[] args(IntrospectedColumn introspectedColumn) {
		String name = introspectedColumn.getActualColumnName();
		String comment = introspectedColumn.getRemarks();
		String insertDefault = introspectedColumn.getProperties().getProperty("insert");
		String updateDefault = introspectedColumn.getProperties().getProperty("update");
		String javaProperty = introspectedColumn.getJavaProperty();
		String jdbcTypeName = toJdbcTypeName(introspectedColumn);
		return new Object[] { 
				name, // 0 +3
				comment==null ? 0 : 1, comment, // 1, 2 +3 
				insertDefault==null ? 0 : 1, insertDefault, // 3, 4 +3
				updateDefault==null ? 0 : 1, updateDefault, // 5, 6 +3
				javaProperty, // 7 +3
				jdbcTypeName // 8 +3
			};
	}

	private Object[] args(IntrospectedTable introspectedTable, IntrospectedColumn introspectedColumn) {
		Object[] t = args(introspectedTable);
		Object[] c = args(introspectedColumn);
		Object[] join = new Object[t.length + c.length];
		System.arraycopy(t, 0, join, 0, t.length);
		System.arraycopy(c, 0, join, t.length, c.length);
		return join;
	}
}
