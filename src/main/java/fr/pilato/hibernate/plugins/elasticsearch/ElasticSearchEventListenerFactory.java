package fr.pilato.hibernate.plugins.elasticsearch;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.event.Destructible;
import org.hibernate.event.Initializable;
import org.hibernate.event.PostDeleteEvent;
import org.hibernate.event.PostDeleteEventListener;
import org.hibernate.event.PostInsertEvent;
import org.hibernate.event.PostInsertEventListener;
import org.hibernate.event.PostUpdateEvent;
import org.hibernate.event.PostUpdateEventListener;

/**
 * <b>ElasticSearch Hibernate Listener implementation</b><br>
 * You can configure the cluster name to connect on by specifying <i>es.cluster.name</i> system property.<br>
 * e.g. -Des.cluster.name=MYCLUSTER<br>
 * If the property is undefined, ELASTICSEARCH will be used as default cluster name.
 * @author David Pilato
 */
@SuppressWarnings("serial")
public class ElasticSearchEventListenerFactory implements 
		PostDeleteEventListener,
		PostInsertEventListener, 
		PostUpdateEventListener,
		Initializable, 
		Destructible {

	private static final Log log = LogFactory.getLog(ElasticSearchEventListenerFactory.class);

	private static ElasticSearchEventListener listener;
	
	public static ElasticSearchEventListener getInstance() {
		if (listener == null) {
			listener = new ElasticSearchEventListener(); 
		}
		
		return listener;
	}
	
	public void initialize(Configuration cfg) {
		// The first time we get here, listener is null !
		if (listener == null) {
			getInstance().initialize(cfg);
		}
	}

	public void onPostDelete(PostDeleteEvent event) {
		getInstance();
		if ( listener.isUsed() ) listener.onPostDelete(event);
	}

	public void onPostInsert(PostInsertEvent event) {
		getInstance();
		if ( listener.isUsed() ) listener.onPostInsert(event);
	}

	public void onPostUpdate(PostUpdateEvent event) {
		getInstance();
		if ( listener.isUsed() ) listener.onPostUpdate(event);
	}

	public void cleanup() {
		if (listener != null) {
			if (log.isDebugEnabled()) log.debug("Stopping Elastic Search Event Listener");
			listener.cleanup();
			listener = null;
		}
	}
}
