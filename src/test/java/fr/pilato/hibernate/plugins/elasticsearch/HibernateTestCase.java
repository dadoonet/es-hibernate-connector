package fr.pilato.hibernate.plugins.elasticsearch;

import static org.elasticsearch.index.query.xcontent.QueryBuilders.termQuery;
import static org.elasticsearch.node.NodeBuilder.nodeBuilder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.net.URL;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.node.Node;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import fr.pilato.hibernate.plugins.elasticsearch.testcase1.ChildEntity;
import fr.pilato.hibernate.plugins.elasticsearch.testcase1.EntityMaker;
import fr.pilato.hibernate.plugins.elasticsearch.testcase1.SimpleEntity;

public class HibernateTestCase {

	protected static Configuration cfg;
	protected static Session session;

	private static final String clusterName = "myTestCluster";
	
	static private boolean deleteDirectory(File path) {
	    if( path.exists() ) {
	      File[] files = path.listFiles();
	      for(int i=0; i<files.length; i++) {
	         if(files[i].isDirectory()) {
	           deleteDirectory(files[i]);
	         }
	         else {
	           files[i].delete();
	         }
	      }
	    }
	    return( path.delete() );
	  }

	@Before
	public void setUp() throws Exception {
		// Setting things for Elastic Search Standalone
//		System.setProperty("es.index.store.type", "memory");
		System.setProperty("es.cluster.name", clusterName);
		System.setProperty("es.client.only", "false");
		
		// Let's find target/data dir for tests
		ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
		// get a URL reference to something we now is part of the classpath (us)
		URL myUrl = contextClassLoader.getResource( HibernateTestCase.class.getName().replace( '.', '/' ) + ".class" );
		
		
		File myPath = new File( myUrl.getFile() );
		// navigate back to '/target'
		File targetDir = myPath
				.getParentFile()  // target/test-classes/fr/pilato/hibernate/plugins/elasticsearch
				.getParentFile()  // target/test-classes/fr/pilato/hibernate/plugins
				.getParentFile()  // target/test-classes/fr/pilato/hibernate
				.getParentFile()  // target/test-classes/fr/pilato
				.getParentFile()  // target/test-classes/fr
				.getParentFile()  // target/test-classes/
				.getParentFile(); // target

		// Removing old test files
		HibernateTestCase.deleteDirectory( new File( targetDir + "/esdata" ) );

		File indexDir = new File( targetDir, "esdata" );
		System.setProperty("es.path.data", indexDir.getAbsolutePath());

		// Setting things for Hibernate
		if (cfg == null) {
			cfg = new Configuration();
			cfg.setProperty(org.hibernate.cfg.Environment.SHOW_SQL, "true");
			cfg.setProperty(org.hibernate.cfg.Environment.HBM2DDL_AUTO, "create");
			cfg.setProperty(org.hibernate.cfg.Environment.DIALECT, "org.hibernate.dialect.HSQLDialect");
			cfg.setProperty(org.hibernate.cfg.Environment.DRIVER, "org.hsqldb.jdbcDriver");
			cfg.setProperty(org.hibernate.cfg.Environment.URL, "jdbc:hsqldb:mem:aname");
			cfg.setProperty(org.hibernate.cfg.Environment.USER, "sa");
			cfg.setProperty(org.hibernate.cfg.Environment.PASS, "");
			cfg.addAnnotatedClass(SimpleEntity.class);
			cfg.addAnnotatedClass(ChildEntity.class);

			// Activate automatic indexing with ES
			cfg.setProperty("hibernate.search.autoregister_listeners", "false");
			cfg.setListener("post-delete", "fr.pilato.hibernate.plugins.elasticsearch.ElasticSearchEventListenerFactory");
			cfg.setListener("post-update", "fr.pilato.hibernate.plugins.elasticsearch.ElasticSearchEventListenerFactory");
			cfg.setListener("post-insert", "fr.pilato.hibernate.plugins.elasticsearch.ElasticSearchEventListenerFactory");
			
			SessionFactory sf = cfg.buildSessionFactory();
			session = sf.openSession();
			SchemaExport export = new SchemaExport( cfg );
			export.create( true, true );
		}
	}

	@After
	public void tearDown() throws Exception {
		if (session != null) {
		     session.close();
		}
	}
	
	@Test
	public void doMyTest() {
		SimpleEntity entity = EntityMaker.getEntity4_2();

		Transaction tx = null;
		 try {
		     tx = session.beginTransaction();
		     
		     session.persist(entity);
		     
		     tx.commit();
		 }
		 catch (Exception e) {
		     if (tx != null) tx.rollback();
		     fail("Cannot persist entity before running the real Elastic Test : " + e.getMessage());
		 }
		 
		 // So now we have an entity
		 // We can search for it
		Node node = null;
		Client client = null;

		node = nodeBuilder().clusterName(clusterName).node();
		client = node.client();
	
		SearchResponse response = client.prepareSearch("default")
	        .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
	        .setQuery(termQuery("value", "djsfhdhfhifhize"))
	        .setFrom(0).setSize(60).setExplain(true)
	        .execute()
	        .actionGet();

		assertEquals("We should find nothing with this criteria", 0, response.getHits().getTotalHits());
		
		response = client.prepareSearch("default")
	        .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
	        .setQuery(termQuery("value", "child"))
	        .setFrom(0).setSize(60).setExplain(true)
	        .execute()
	        .actionGet();

		assertEquals("We should find one document with this criteria", 1, response.getHits().getTotalHits());
	}
}
