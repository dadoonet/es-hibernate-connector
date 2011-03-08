package org.elasticsearch.orm.hibernate;

import static org.elasticsearch.common.xcontent.XContentFactory.safeJsonBuilder;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.junit.Test;

/**
 * @author David Pilato
 *
 */
public class JSonBuilderTest {
	private static final Log log = LogFactory.getLog(JSonBuilderTest.class);
	
	@Test
	public void testSimpleEntityThatFails() throws IOException {
		SimpleEntity entity = new SimpleEntity();
		entity.setField("Value for field");

		entity.addToStringsfield("Value for stringsfield");

		XContentBuilder xcontent = ElasticSearchHelper.entityToJSon(null, entity);
		
		log.debug("End of test : " + ElasticSearchJSonHelper.printIndex(xcontent));
	}
	
	@Test
	public void testSimpleEntityThatSuccess() throws IOException {
		XContentBuilder xcontent = null;
		xcontent = safeJsonBuilder();
		xcontent = xcontent.startObject();
		xcontent = xcontent.field("field", "Value for field");
		xcontent = xcontent.startArray("stringsfield");
		xcontent = xcontent.value("Value for stringsfield");
		xcontent = xcontent.endArray();
		xcontent = xcontent.endObject();
		
		log.debug("End of test : " + ElasticSearchJSonHelper.printIndex(xcontent));
	}
	
}
