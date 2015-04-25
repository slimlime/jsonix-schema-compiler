package org.hisrc.jsonix.compilation.jsc;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.namespace.QName;

import org.apache.commons.lang3.Validate;
import org.hisrc.jsonix.definition.Mapping;
import org.hisrc.jsonix.jsonschema.JsonSchemaBuilder;
import org.hisrc.jsonix.naming.StandardNaming;
import org.jvnet.jaxb2_commons.xml.bind.model.MClassInfo;
import org.jvnet.jaxb2_commons.xml.bind.model.MClassTypeInfo;
import org.jvnet.jaxb2_commons.xml.bind.model.MPropertyInfo;

public class JsonSchemaClassInfoCompiler<T, C extends T> implements
		JsonSchemaTypeInfoCompiler<MClassInfo<T, C>, T, C> {

	// TODO move to constants
	public static final String DEFAULT_SCOPED_NAME_DELIMITER = ".";

	private final JsonSchemaMappingCompiler<T, C> mappingCompiler;
	private final Mapping<T, C> mapping;

	public JsonSchemaClassInfoCompiler(
			JsonSchemaMappingCompiler<T, C> mappingCompiler) {
		Validate.notNull(mappingCompiler);
		this.mappingCompiler = mappingCompiler;
		this.mapping = mappingCompiler.getMapping();
	}

	public JsonSchemaMappingCompiler<T, C> getMappingCompiler() {
		return mappingCompiler;
	}

	@Override
	public JsonSchemaBuilder compile(MClassInfo<T, C> classInfo) {
		final JsonSchemaBuilder classInfoSchema = new JsonSchemaBuilder();
		classInfoSchema.addType("object");
		final String localName = classInfo
				.getContainerLocalName(DEFAULT_SCOPED_NAME_DELIMITER);
		classInfoSchema.addTitle(localName);
		// TODO addId ?
		// ...
		final MClassTypeInfo<T, C> baseTypeInfo = classInfo.getBaseTypeInfo();
		final JsonSchemaBuilder typeInfoSchema;
		if (baseTypeInfo != null) {
			final JsonSchemaBuilder baseTypeInfoSchema = mappingCompiler
					.createTypeInfoSchemaRef(baseTypeInfo);
			typeInfoSchema = new JsonSchemaBuilder();
			typeInfoSchema.addAllOf(baseTypeInfoSchema);
			typeInfoSchema.addAllOf(classInfoSchema);
		} else {
			typeInfoSchema = classInfoSchema;
		}

		// TODO move to the builder
		final Map<String, JsonSchemaBuilder> propertyInfoSchemas = compilePropertyInfos(classInfo);
		for (Entry<String, JsonSchemaBuilder> entry : propertyInfoSchemas
				.entrySet()) {
			classInfoSchema.addProperty(entry.getKey(), entry.getValue());
		}
		classInfoSchema.add(JsonixJsonSchemaConstants.TYPE_TYPE_PROPERTY_NAME,
				StandardNaming.CLASS_INFO);
		final QName typeName = classInfo.getTypeName();
		if (typeName != null) {
			classInfoSchema
					.add(JsonixJsonSchemaConstants.TYPE_NAME_PROPERTY_NAME,
							new JsonSchemaBuilder()
									.add(JsonixJsonSchemaConstants.LOCAL_PART_PROPERTY_NAME,
											typeName.getLocalPart())
									.add(JsonixJsonSchemaConstants.NAMESPACE_URI_PROPERTY_NAME,
											typeName.getNamespaceURI()));
		}

		return typeInfoSchema;
	}

	private Map<String, JsonSchemaBuilder> compilePropertyInfos(
			MClassInfo<T, C> classInfo) {
		final Map<String, JsonSchemaBuilder> propertyInfoSchemas = new LinkedHashMap<String, JsonSchemaBuilder>(
				classInfo.getProperties().size());
		for (MPropertyInfo<T, C> propertyInfo : classInfo.getProperties()) {
			if (mapping.getPropertyInfos().contains(propertyInfo)) {
				propertyInfoSchemas
						.put(propertyInfo.getPrivateName(),
								propertyInfo
										.acceptPropertyInfoVisitor(new JsonSchemaPropertyInfoCompilerVisitor<T, C>(
												this)));
			}
		}
		return propertyInfoSchemas;
	}
}
