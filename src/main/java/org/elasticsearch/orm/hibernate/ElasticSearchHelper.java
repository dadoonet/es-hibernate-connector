package org.elasticsearch.orm.hibernate;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.elasticsearch.common.jackson.annotate.JsonTypeInfo.Id;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.orm.hibernate.annotations.ESField;
import org.elasticsearch.orm.hibernate.annotations.ESIndexed;

/**
 * ElasticSearch Hibernate Listener implementation : <br>
 * Supported Annotations (and properties) :
 * <ul>
 * <li>ESIndexed(indexName=)
 * <li>ESField()
 * <li>Id()
 * </ul>
 * 
 * @see ESIndexed
 * @see ESField
 * @see Id
 * @author David Pilato
 */
public class ElasticSearchHelper {

	private static final Log log = LogFactory.getLog(ElasticSearchHelper.class);

	private static String printEntity(Object entity) {
		if (entity == null) return "null";
		return "(" + entity.getClass().getSimpleName() + ") " + entity;
	}
	
	public static XContentBuilder entityToJSon(XContentBuilder xcontent,
			Object entity) {
		log.debug("Working on " + printEntity(entity) + " : current JSon = " + ElasticSearchJSonHelper.printIndex(xcontent));
		
		if (entity == null)
			throw new RuntimeException(
					"Trying to index a null entity ? What a strange idea !");

		Collection<Field> fields = getAllFieldsAnnotationRecursive(entity
				.getClass());
		
		if (fields.isEmpty()) {
			try {
				log.debug(".value(" + printEntity(entity) + ") on " + xcontent);
				xcontent = xcontent.value(entity);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			xcontent = ElasticSearchJSonHelper.startIndex(xcontent);
			for (Iterator<Field> iterator = fields.iterator(); iterator.hasNext();) {
				Field field = iterator.next();
				String fieldName = field.getName();
				Object value = getMemberValue(entity, fieldName);

				if (value instanceof Collection<?>) {
					// Seems that we have a Collection here !
					Collection<?> col = (Collection<?>) value;

					if (!col.isEmpty()) {
						xcontent = ElasticSearchJSonHelper.startArray(xcontent,
								fieldName);
						for (Iterator<?> itCol = col.iterator(); itCol.hasNext();) {
							Object object = itCol.next();

							xcontent = entityToJSon(xcontent, object);
						}
						xcontent = ElasticSearchJSonHelper.stopArray(xcontent);
					}
				} else
					if (value instanceof Map<?, ?>) {
						// Seems that we have a Collection here !
						Map<?, ?> map = (Map<?, ?>) value;
						Collection<?> col = (Collection<?>) map.values();

						if (!col.isEmpty()) {
							xcontent = ElasticSearchJSonHelper.startArray(xcontent,
									fieldName);
							for (Iterator<?> itCol = col.iterator(); itCol.hasNext();) {
								Object object = itCol.next();

								xcontent = entityToJSon(xcontent, object);
							}
							xcontent = ElasticSearchJSonHelper.stopArray(xcontent);
						}
					}			
					else
				{
					xcontent = ElasticSearchJSonHelper.addIndex(xcontent, value,
							field);
				}
			}

			xcontent = ElasticSearchJSonHelper.stopIndex(xcontent);
			
		}
		

		return xcontent;
	}

	private static Object getMemberValue(Object bean, String field) {
		Object value;
		try {
			value = PropertyUtils.getNestedProperty(bean, field);
		} catch (Exception e) {
			throw new IllegalStateException("Could not get property value", e);
		}
		return value;
	}

	/**
	 * Get all the fields annoted by Field
	 * 
	 * @param clazz
	 * @return
	 */
	private static Collection<Field> getAllFieldsAnnotation(Class<?> clazz) {
		Collection<Field> returnFields = new ArrayList<Field>();
		if (clazz == null)
			return returnFields;
		Field[] fields = clazz.getDeclaredFields();
		for (int i = 0; i < fields.length; i++) {
			Annotation anno = (Annotation) fields[i]
					.getAnnotation(ESField.class);

			if (anno != null) {
				returnFields.add(fields[i]);
			}
		}

		return returnFields;
	}

	/**
	 * Get all the fields annoted by Field even in Parents !
	 * 
	 * @param clazz
	 * @return
	 */
	private static Collection<Field> getAllFieldsAnnotationRecursive(
			Class<?> clazz) {
		Collection<Field> returnFields = new ArrayList<Field>();
		returnFields.addAll(getAllFieldsAnnotation(clazz));
		if (clazz != null)
			returnFields.addAll(getAllFieldsAnnotationRecursive(clazz
					.getSuperclass()));
		return returnFields;
	}

}
