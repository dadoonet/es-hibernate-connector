package fr.pilato.hibernate.plugins.elasticsearch;

import static org.elasticsearch.index.query.xcontent.QueryBuilders.termQuery;
import static org.elasticsearch.node.NodeBuilder.nodeBuilder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.net.URL;

import org.elasticsearch.action.count.CountResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.node.Node;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.pilato.hibernate.plugins.elasticsearch.testcase1.ChildEntity;
import fr.pilato.hibernate.plugins.elasticsearch.testcase1.EntityMaker;
import fr.pilato.hibernate.plugins.elasticsearch.testcase1.SimpleEntity;

public class HibernateTestCase {

	protected static Configuration cfg;
	protected static Session session;
	protected static Node node;
	
	private static boolean deleteDirectory(File path) {
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

	@BeforeClass
	public static void setUp() throws Exception {
		// Setting things for Elastic Search Standalone
		System.setProperty("es.client.only", "false");
		
		// Let's find target/data dir for tests
		ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();

		URL myConfigUrl = contextClassLoader.getResource( "myconfig/myelasticsearch.yml" );
		System.setProperty("es.config", myConfigUrl.getPath());

		// We can start a node
		node = nodeBuilder().client(true).build();
		
//		ImmutableMap<String, String> settings = node.settings().getAsMap();
//		
//		for (String string : settings.keySet()) {
//			System.out.println(string + " : " + settings.get(string));
//		}
		
		// Removing old test files
		// We ask to the node where are datas
		String dataDir = node.settings().get("path.data");
		HibernateTestCase.deleteDirectory( new File(dataDir) );

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

		 node = node.start();
		 
		 // Just wait a while for synchronizing the two nodes (HibernateNode and JUnitTestNode)
		 Thread.sleep(1000);
	}

	@AfterClass
	public static void tearDown() throws Exception {
		if (node != null) {
			node.close();
		}

		if (session != null) {
			session.getSessionFactory().close();
		    session.close();
		}
	}

	@Test
	public void countEntity() {
		Client client = node.client();
	
		CountResponse response = client.prepareCount("default")
	        .setQuery(termQuery("value", "child"))
	        .execute()
	        .actionGet();

		client.close();
		
		assertEquals("We should find one document with this criteria", 1, response.count());
	}

	@Test
	public void findEntity() {
		Client client = node.client();
	
		SearchResponse response = client.prepareSearch("default")
	        .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
	        .setQuery(termQuery("value", "child"))
	        .setFrom(0).setSize(60).setExplain(true)
	        .execute()
	        .actionGet();

		client.close();
		
		assertEquals("We should find one document with this criteria", 1, response.getHits().getTotalHits());
	}
	
	@Test
	public void doNotFindEntity() {
		Client client = node.client();
		
		SearchResponse response = client.prepareSearch("default")
	        .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
	        .setQuery(termQuery("value", "djsfhdhfhifhize"))
	        .setFrom(0).setSize(60).setExplain(true)
	        .execute()
	        .actionGet();

		client.close();
		
		assertEquals("We should find nothing with this criteria", 0, response.getHits().getTotalHits());
	}
}
