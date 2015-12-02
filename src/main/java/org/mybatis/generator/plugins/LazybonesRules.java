package org.mybatis.generator.plugins;

import java.util.Properties;

import org.mybatis.generator.internal.rules.Rules;
import org.mybatis.generator.internal.rules.RulesDelegate;

public class LazybonesRules extends RulesDelegate {
	
	private Properties properties;

	public LazybonesRules(Rules rules, Properties properties) {
		super(rules);
		this.properties = properties;
	}

	@Override
	public boolean generateBaseRecordClass() {
		Boolean generate = getRule("generateBaseRecordClass");
		return generate != null ? generate : super.generateBaseRecordClass();
	}

	@Override
	public boolean generateBaseResultMap() {
		Boolean generate = getRule("generateBaseResultMap");
		return generate != null ? generate : super.generateBaseResultMap();
	}

	@Override
	public boolean generateCountByExample() {
		if(isUseLazybonesRules()) return false;
		Boolean generate = getRule("generateCountByExample");
		return generate != null ? generate : super.generateCountByExample();
	}

	@Override
	public boolean generateDeleteByExample() {
		if(isUseLazybonesRules()) return false;
		Boolean generate = getRule("generateDeleteByExample");
		return generate != null ? generate : super.generateDeleteByExample();
	}

	@Override
	public boolean generateDeleteByPrimaryKey() {
		Boolean generate = getRule("generateDeleteByPrimaryKey");
		return generate != null ? generate : super.generateDeleteByPrimaryKey();
	}

	@Override
	public boolean generateExampleClass() {
		if(isUseLazybonesRules()) return false;
		Boolean generate = getRule("generateExampleClass");
		return generate != null ? generate : super.generateExampleClass();
	}

	@Override
	public boolean generateInsert() {
		Boolean generate = getRule("generateInsert");
		return generate != null ? generate : super.generateInsert();
	}

	@Override
	public boolean generateInsertSelective() {
		if(isUseLazybonesRules()) return false;
		Boolean generate = getRule("generateInsertSelective");
		return generate != null ? generate : super.generateInsertSelective();
	}

	@Override
	public boolean generatePrimaryKeyClass() {
		if(isUseLazybonesRules()) return false;
		Boolean generate = getRule("generatePrimaryKeyClass");
		return generate != null ? generate : super.generatePrimaryKeyClass();
	}

	@Override
	public boolean generateRecordWithBLOBsClass() {
		if(isUseLazybonesRules()) return false;
		Boolean generate = getRule("generateRecordWithBLOBsClass");
		return generate != null ? generate : super.generateRecordWithBLOBsClass();
	}

	@Override
	public boolean generateResultMapWithBLOBs() {
		Boolean generate = getRule("generateResultMapWithBLOBs");
		return generate != null ? generate : super.generateResultMapWithBLOBs();
	}

	@Override
	public boolean generateSelectByExampleWithBLOBs() {
		if(isUseLazybonesRules()) return false;
		Boolean generate = getRule("generateSelectByExampleWithBLOBs");
		return generate != null ? generate : super.generateSelectByExampleWithBLOBs();
	}

	@Override
	public boolean generateSelectByExampleWithoutBLOBs() {
		if(isUseLazybonesRules()) return false;
		Boolean generate = getRule("generateSelectByExampleWithoutBLOBs");
		return generate != null ? generate : super.generateSelectByExampleWithoutBLOBs();
	}

	@Override
	public boolean generateSelectByPrimaryKey() {
		Boolean generate = getRule("generateSelectByPrimaryKey");
		return generate != null ? generate : super.generateSelectByPrimaryKey();
	}

	@Override
	public boolean generateSQLExampleWhereClause() {
		if(isUseLazybonesRules()) return false;	
		Boolean generate = getRule("generateSQLExampleWhereClause");
		return generate != null ? generate : super.generateSQLExampleWhereClause();
	}

	@Override
	public boolean generateMyBatis3UpdateByExampleWhereClause() {
		if(isUseLazybonesRules()) return false;
		Boolean generate = getRule("generateMyBatis3UpdateByExampleWhereClause");
		return generate != null ? generate : super.generateMyBatis3UpdateByExampleWhereClause();
	}

	@Override
	public boolean generateUpdateByExampleSelective() {
		if(isUseLazybonesRules()) return false;
		Boolean generate = getRule("generateUpdateByExampleSelective");
		return generate != null ? generate : super.generateUpdateByExampleSelective();
	}

	@Override
	public boolean generateUpdateByExampleWithBLOBs() {
		if(isUseLazybonesRules()) return false;
		Boolean generate = getRule("generateUpdateByExampleWithBLOBs");
		return generate != null ? generate : super.generateUpdateByExampleWithBLOBs();
	}

	@Override
	public boolean generateUpdateByExampleWithoutBLOBs() {
		if(isUseLazybonesRules()) return false;
		Boolean generate = getRule("generateUpdateByExampleWithoutBLOBs");
		return generate != null ? generate : super.generateUpdateByExampleWithoutBLOBs();
	}

	@Override
	public boolean generateUpdateByPrimaryKeySelective() {
		Boolean generate = getRule("generateUpdateByPrimaryKeySelective");
		return generate != null ? generate : super.generateUpdateByPrimaryKeySelective();
	}

	@Override
	public boolean generateUpdateByPrimaryKeyWithBLOBs() {
		if(isUseLazybonesRules()) return false;
		Boolean generate = getRule("generateUpdateByPrimaryKeyWithBLOBs");
		return generate != null ? generate : super.generateUpdateByPrimaryKeyWithBLOBs();
	}

	@Override
	public boolean generateUpdateByPrimaryKeyWithoutBLOBs() {
		if(isUseLazybonesRules()) return false;
		Boolean generate = getRule("generateUpdateByPrimaryKeyWithoutBLOBs");
		return generate != null ? generate : super.generateUpdateByPrimaryKeyWithoutBLOBs();
	}

	@Override
	public boolean generateBaseColumnList() {
		Boolean generate = getRule("generateBaseColumnList");
		return generate != null ? generate : super.generateBaseColumnList();
	}

	@Override
	public boolean generateBlobColumnList() {
		Boolean generate = getRule("generateBlobColumnList");
		return generate != null ? generate : super.generateBlobColumnList();
	}

	@Override
	public boolean generateJavaClient() {
		Boolean generate = getRule("generateJavaClient");
		return generate != null ? generate : super.generateJavaClient();
	}
	
	private boolean isUseLazybonesRules() {
		return Boolean.valueOf(properties.getProperty("useLazybonesRules", "true"));
	}
	
	private Boolean getRule(String attributeName) {
		String value = properties.getProperty(attributeName);
		return value != null ? Boolean.valueOf(value) : null;
	}
}
