package fr.pilato.hibernate.plugins.elasticsearch;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

import javax.persistence.Id;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.Client;

import fr.pilato.hibernate.plugins.elasticsearch.annotations.ESIndexed;

/**
 * ElasticSearch Hibernate Listener implementation : <br>
 * Supported Annotations (and properties) :
 * <ul>
 * <li>ESIndexed(indexName=)
 * </ul>
 * 
 * @see ESIndexed
 * @author David Pilato
 */
public class ElasticSearchHelper {

	private static final Log log = LogFactory.getLog(ElasticSearchHelper.class);

	/**
	 * Push an entity to Elastic
	 * @param client Elastic Search Client
	 * @param entity Entity to remove
	 */
	public static void pushElastic(Client client, Object entity) {
		if (entity == null)
			throw new RuntimeException(
					"Trying to index a null entity ? What a strange idea !");
		if (client == null) {
			log.error("Trying to push index to a non existing client ? Bad idea !");
			return;
		}

		String indexName = getEntityIndexName(entity);
		String entityName = getEntityName(entity);
		String documentId = getDocumentId(entity);

		if (log.isDebugEnabled())
			log.debug("Trying to prepare EntityBuilder for " + indexName + "/"
					+ entityName + "/" + documentId);

		ObjectMapper mapper = new ObjectMapper();

		mapper.registerModule(new ElasticSearchJacksonHibernateModule());
		
		mapper.configure(SerializationConfig.Feature.WRITE_DATES_AS_TIMESTAMPS, true);
		mapper.configure(SerializationConfig.Feature.AUTO_DETECT_FIELDS, true);
		mapper.configure(SerializationConfig.Feature.AUTO_DETECT_GETTERS, false);
		mapper.configure(SerializationConfig.Feature.AUTO_DETECT_IS_GETTERS, false);
		mapper.configure(SerializationConfig.Feature.INDENT_OUTPUT, false);

		try {
			String result = mapper.writeValueAsString(entity);
			if (log.isDebugEnabled())
				log.debug(" - Entity will be indexed as "
						+ result);
			
			IndexResponse response = client
					.prepareIndex(indexName, entityName, documentId)
					.setSource(result).execute().actionGet();

			if (log.isDebugEnabled() && response != null)
				log.debug(" - Entity succesfully indexed...");
			if (response == null)
				log.warn("Unable to index entity " + entityName + " : " + entity);
		} catch (Exception e) {
			log.warn("Unable to push entity into Elastic Search...");
		}
	}

	/**
	 * Remove an entity from Elastic
	 * @param client Elastic Search Client
	 * @param entity Entity to remove
	 */
	public static void removeElastic(Client client, Object entity) {
		if (entity == null)
			throw new RuntimeException(
					"Trying to remove a null entity ? What a strange idea !");
		if (client == null) {
			log.error("Trying to remove an index to a non existing client ? Bad idea !");
			return;
		}

		String indexName = getEntityIndexName(entity);
		String entityName = getEntityName(entity);
		String documentId = getDocumentId(entity);

		if (log.isDebugEnabled())
			log.debug("Trying to remove entity " + indexName + "/" + entityName
					+ "/" + documentId);
		DeleteResponse response = client
				.prepareDelete(indexName, entityName, documentId).execute()
				.actionGet();

		// Just for debugging purpose
		if (log.isDebugEnabled() && response != null) {
			if (response.isNotFound())
				log.debug(" - Entity was not found...");
			else
				log.debug(" - Entity successfully removed...");
		}
		if (response == null)
			log.warn("Unable to remove entity " + entityName + " : " + entity);
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
	 * Shortcut to entity.getClass().getSimpleName()
	 * @param entity Entity
	 * @return simple Name for entity class
	 */
	private static String getEntityName(Object entity) {
		if (entity == null)
			return null;
		return entity.getClass().getSimpleName();
	}

	/**
	 * Get the index name as it was declared in ESIndexed annotation
	 * for a given entity
	 * @param entity Entity where to find index name
	 * @return The index name
	 */
	private static String getEntityIndexName(Object entity) {
		Class<?> clazz = entity.getClass();
		ESIndexed annotation = clazz.getAnnotation(ESIndexed.class);

		if (annotation != null) {
			return annotation.indexName().toLowerCase();
		} else {
			// Entity not annoted ! What the hell ????
			throw new RuntimeException("Entity is not annoted ! " + entity);
		}
	}

	/**
	 * Get the document id
	 * 
	 * @param entity
	 *            Entity
	 * @return
	 */
	private static String getDocumentId(Object entity) {
		Class<?> clazz = entity.getClass();

		String anno = findIdAnnotation(clazz);

		if (anno == null) {
			// Oh oh !!!! Big trouble !
			throw new RuntimeException(
					"Cannot find any Id or DocumentId annotation for "
							+ clazz.getName());
		}

		// Now, we need to get the value of this field
		Object value = getMemberValue(entity, anno);

		if (value == null)
			throw new RuntimeException(
					"Cannont index an entity without a correct Id");

		// We expect that toString() will return the Id
		return value.toString();
	}

	/**
	 * Get the document id
	 * 
	 * @param clazz
	 *            Annoted Class
	 * @return
	 */
	private static String findIdAnnotation(Class<?> clazz) {
		if (clazz == null)
			return null;
		Field[] fields = clazz.getDeclaredFields();
		for (int i = 0; i < fields.length; i++) {
			Annotation anno = (Annotation) fields[i].getAnnotation(Id.class);

			if (anno != null) {
				return fields[i].getName();
			}
		}

		// We will look in parent classes
		return findIdAnnotation(clazz.getSuperclass());
	}

//	/**
//	 * Get all the fields annoted by Field
//	 * 
//	 * @param clazz
//	 * @return
//	 */
//	private static Collection<Field> getAllFieldsAnnotation(Class<?> clazz) {
//		Collection<Field> returnFields = new ArrayList<Field>();
//		if (clazz == null)
//			return returnFields;
//		Field[] fields = clazz.getDeclaredFields();
//		for (int i = 0; i < fields.length; i++) {
//			Annotation anno = (Annotation) fields[i]
//					.getAnnotation(ESField.class);
//
//			if (anno != null) {
//				returnFields.add(fields[i]);
//			}
//		}
//
//		return returnFields;
//	}

//	/**
//	 * Get all the fields annoted by Field even in Parents !
//	 * 
//	 * @param clazz
//	 * @return
//	 */
//	private static Collection<Field> getAllFieldsAnnotationRecursive(
//			Class<?> clazz) {
//		Collection<Field> returnFields = new ArrayList<Field>();
//		returnFields.addAll(getAllFieldsAnnotation(clazz));
//		if (clazz != null)
//			returnFields.addAll(getAllFieldsAnnotationRecursive(clazz
//					.getSuperclass()));
//		return returnFields;
//	}

}
