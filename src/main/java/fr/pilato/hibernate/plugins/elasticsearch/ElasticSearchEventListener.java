package fr.pilato.hibernate.plugins.elasticsearch;

import static org.elasticsearch.node.NodeBuilder.nodeBuilder;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.persistence.Id;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.elasticsearch.client.Client;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.event.Destructible;
import org.hibernate.event.Initializable;
import org.hibernate.event.PostDeleteEvent;
import org.hibernate.event.PostDeleteEventListener;
import org.hibernate.event.PostInsertEvent;
import org.hibernate.event.PostInsertEventListener;
import org.hibernate.event.PostUpdateEvent;
import org.hibernate.event.PostUpdateEventListener;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.util.ReflectHelper;

import fr.pilato.hibernate.plugins.elasticsearch.annotations.ESIndexed;

/**
 * ElasticSearch Hibernate Listener implementation :
 * <br>Supported Annotations (and properties) :
 * <ul>
 * <li>Indexed(name=)
 * <li>Id()
 * <li>DocumentId()
 * </ul>
 * Set <i>es.client.only</i> system property to false if you want to start a real node (for tests)<br>
 * Use <i>es.config</i> system property to define your node configuration or add a config
 * file in the classpath
 * @see Indexed
 * @see DocumentId 
 * @see Id 
 * @author David Pilato
 */
@SuppressWarnings("serial")
public class ElasticSearchEventListener implements Initializable,
		PostDeleteEventListener,
		PostInsertEventListener, 
		PostUpdateEventListener, Destructible
		{

	private static final Log log = LogFactory.getLog(ElasticSearchEventListener.class);

	private Node node = null;

	private Map<String, Class<?>> entityMapper = new HashMap<String, Class<?>>();

	private boolean used = false;

	public void initialize(Configuration cfg) {
		// We need to configure ES to handle HSearch annotations
		if (log.isDebugEnabled()) log.debug( "Elastic Search Starting Configuration" );

		try {
			if (log.isDebugEnabled()) log.debug( "ES Indexed classes :" );
			for (Iterator<PersistentClass> itMappings = cfg.getClassMappings(); itMappings.hasNext();) {
				PersistentClass persistentClass = itMappings.next();
				
				// Looking if the Entity is Indexed
				Class<?> clazz = ReflectHelper.classForName( persistentClass.getEntityName() );
				boolean isIndexed = isEntityIndexed(clazz);
				
				if (isIndexed) {
					used = true;
					log.debug("  + " + clazz.getSimpleName());
					registerEntityHolder(clazz);
				}
			}
		} catch (ClassNotFoundException e) {
			// It should not be possible to get here !
			log.warn( "Can not find Entity Class : " + e.getMessage() );
		}

		if (log.isDebugEnabled()) log.debug( "Elastic Search Event Listener " + (used ? "activated" : "deactivated") );
	}

	/**
	 * Starting cluster connexion
	 */
	public ElasticSearchEventListener() {
		log.info( "Starting Elastic Search Plugin..." );
		boolean isClientOnly = true;

		// Let's find in system properties if we must start the client
		// as a Node also (for testing purpose)
		String strIsClientOnly = System.getProperty("es.client.only");
		if (strIsClientOnly != null && strIsClientOnly.toLowerCase().equals("false")) isClientOnly = false; 
		
		log.info( "Starting Node " + (isClientOnly ? "(Client Only)" : "(Full)") + " for Elastic Search..." );
		
		NodeBuilder nodeBuilder = nodeBuilder().client(isClientOnly);
		node = nodeBuilder.node();

		log.info( "Node [" + node.settings().get("name") + "] for [" + node.settings().get("cluster.name") + "] cluster started..." );
		log.info( "  - data : " + node.settings().get("path.data") );
		log.info( "  - logs : " + node.settings().get("path.logs") );
	}

	public boolean isUsed() {
		return used;
	}

	public void onPostDelete(PostDeleteEvent event) {
		final Object entity = event.getEntity();
		if (isEntityIndexed(entity)) {
			if (log.isDebugEnabled()) log.debug("Processing Delete event on " + getEntityName(entity));
			Client client = node.client();
			ElasticSearchHelper.removeElastic(client, entity);
			client.close();
		}
	}

	public void onPostInsert(PostInsertEvent event) {
		final Object entity = event.getEntity();
		if (isEntityIndexed(entity)) {
			if (log.isDebugEnabled()) log.debug("Processing Insert event on " + getEntityName(entity));
			Client client = node.client();
			ElasticSearchHelper.pushElastic(client, entity);
			client.close();
		}
	}

	public void onPostUpdate(PostUpdateEvent event) {
		final Object entity = event.getEntity();
		if (isEntityIndexed(entity)) {
			if (log.isDebugEnabled()) log.debug("Processing Insert event on " + getEntityName(entity));
			Client client = node.client();
			ElasticSearchHelper.pushElastic(client, entity);
			client.close();
		}
	}
	
	private void registerEntityHolder(Class<?> clazz) {
		entityMapper.put(clazz.getName(), clazz);
	}

	private boolean isEntityIndexed(final Object entity) {
		// Can we find the class for this entity in ou Map ?
		if (entity == null) return false;
		String className = entity.getClass().getName();
		
		return entityMapper.containsKey(className);
	}
	
	/**
	 * Find if the Entity Class is Indexed
	 * @param clazz
	 * @return true if true ( ;-) )
	 */
	private boolean isEntityIndexed(Class<?> clazz) {
		ESIndexed annotation = (ESIndexed) clazz.getAnnotation(ESIndexed.class);
		if (annotation != null) {
			return true;
		}
		return false;
	}

	private String getEntityName(Object entity) {
		if (entity == null) return null;
		return entity.getClass().getSimpleName();
	}

	public void cleanup() {
		log.info("Closing Elastic Search Plugin...");
		if (node != null) node.close();
	}

	/**
	 * Get the Elastic Search current node
	 * @return Current ES node
	 */
	public Node getESNode() {
		return node;
	}

	/**
	 * Get the Elastic Search current client
	 * @deprecated Don't use it anymore. Use {@link #getESNode()}.client() and 
	 * don't forget to close the client after use...
	 * @return an ES client
	 */
	@Deprecated
	public Client getESClient() {
		return node.client();
	}
}
