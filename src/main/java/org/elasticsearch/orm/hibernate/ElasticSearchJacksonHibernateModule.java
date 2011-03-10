package org.elasticsearch.orm.hibernate;

import org.codehaus.jackson.Version;
import org.codehaus.jackson.map.Module;

//TODO for future use only
public class ElasticSearchJacksonHibernateModule extends Module {
	private final String NAME = "ESJacksonHibernateModule";

	private final static Version VERSION = new Version(0, 1, 0, null);

	@Override
	public String getModuleName() {
		return NAME;
	}

	@Override
	public Version version() {
		return VERSION;
	}

	@Override
	public void setupModule(SetupContext context) {
		context.insertAnnotationIntrospector(new ElasticSearchHibernateAnnotationIntrospector());
		// context.addSerializers(...);
	}
}