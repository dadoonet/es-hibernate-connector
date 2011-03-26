package fr.pilato.hibernate.plugins.elasticsearch.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicate that the entity will be indexed by Elastic Search
 * @author David Pilato
 */
@Retention( RetentionPolicy.RUNTIME )
@Target( ElementType.TYPE )
@Documented
public @interface ESIndexed {
	/**
	 * @return the index Name used by Elastic (Default : "default")
	 */
	String indexName() default "default";
}
