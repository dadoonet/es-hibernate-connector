package fr.pilato.hibernate.plugins.elasticsearch;

import static org.elasticsearch.common.xcontent.XContentFactory.safeJsonBuilder;

import java.io.IOException;
import java.lang.reflect.Field;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.elasticsearch.common.xcontent.XContentBuilder;

public class ElasticSearchJSonHelper {
	private static final Log log = LogFactory.getLog(ElasticSearchJSonHelper.class);

	private static String printEntity(Object entity) {
		if (entity == null) return "null";
		return "(" + entity.getClass().getSimpleName() + ") " + entity;
	}
	
	public static XContentBuilder startIndex(XContentBuilder xcontent) {
		try {
			if (xcontent == null) {
				log.debug("safeJsonBuilder()");
				xcontent = safeJsonBuilder();
			}
			log.debug(".startObject() on " + xcontent);
			return xcontent.startObject();
		} catch (IOException e) {
			throw new RuntimeException("Unable to start JSonBuilder");
		}
	}

	public static XContentBuilder stopIndex(XContentBuilder xcontent) {
		try {
			log.debug(".endObject() on " + xcontent);
			return xcontent.endObject();
		} catch (IOException e) {
			throw new RuntimeException("Unable to stop JSonBuilder");
		}
	}
	
	public static XContentBuilder startArray(XContentBuilder xcontent, String name) {
		try {
			if (xcontent == null) {
				log.debug("safeJsonBuilder()");
				xcontent = safeJsonBuilder();
			}
			log.debug(".startArray(" + name + ") on " + xcontent);
			return xcontent.startArray(name);
		} catch (IOException e) {
			throw new RuntimeException("Unable to start array " + name);
		}
	}

	public static XContentBuilder stopArray(XContentBuilder xcontent) {
		try {
			log.debug(".endArray() on " + xcontent);
			return xcontent.endArray();
		} catch (IOException e) {
			throw new RuntimeException("Unable to stop array");
		}
	}

	public static XContentBuilder addIndex(XContentBuilder xcontent, Object entity, Field field) {
		// If nothing, do nothing !
		if (entity == null || field == null) {
			log.debug(" do nothing()");
			return xcontent;
		}
		try {
			log.debug(".field(" + field.getName() + ", " + printEntity(entity) + ") on " + xcontent);
			return xcontent.field(field.getName(), entity);
		} catch (IOException e) {
			throw new RuntimeException("Unable to add " + field.getName(), e);
		}
	}

	public static String printIndex(XContentBuilder xcontent) {
		if (xcontent == null) return "";
		try {
			return xcontent.string();
		} catch (IOException e) {
			return "";
		}
	}
	
}
