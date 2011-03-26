package fr.pilato.hibernate.plugins.elasticsearch.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicate the analyze for field or method (default : "default")<br>
 * E.g. : ESAnalyzer(analyzer="french")
 * <br>See <a href="http://www.elasticsearch.org/guide/reference/index-modules/analysis/lang-analyzer.html">Elastic Search Guide</a>
 * @deprecated by Annotations from OSEM Elastic Annotations
 * @author David Pilato
 */
@Retention( RetentionPolicy.RUNTIME )
@Target( {ElementType.FIELD, ElementType.METHOD} )
@Documented
@Deprecated
public @interface ESAnalyzer {
	/**
	 * @return the analyzer used by Elastic (Default : "default")
	 */
	String analyzer() default "default";
}
