package org.mybatis.generator.plugins;

import java.sql.Types;

import org.mybatis.generator.api.IntrospectedColumn;
import org.mybatis.generator.api.dom.java.FullyQualifiedJavaType;
import org.mybatis.generator.internal.types.JavaTypeResolverDefaultImpl;

public class LazybonesJavaTypeResolver extends JavaTypeResolverDefaultImpl {

	@Override
	public FullyQualifiedJavaType calculateJavaType(IntrospectedColumn introspectedColumn) {
		FullyQualifiedJavaType answer;

		if(introspectedColumn.getJdbcType() == Types.INTEGER) {
			if(introspectedColumn.getLength() > 9) {
				answer = new FullyQualifiedJavaType(Long.class.getName());
			}
			else if (introspectedColumn.getLength() > 4) {
				answer = new FullyQualifiedJavaType(Integer.class.getName());
			}
			else {
				answer = new FullyQualifiedJavaType(Short.class.getName());
			}
		}
		else {
			answer = super.calculateJavaType(introspectedColumn);
		}
		return answer;
	}
}
