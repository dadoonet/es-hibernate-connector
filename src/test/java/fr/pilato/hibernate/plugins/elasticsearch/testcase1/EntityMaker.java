package fr.pilato.hibernate.plugins.elasticsearch.testcase1;

public class EntityMaker {
	
	/**
	 * @return An empty entity
	 */
	public static SimpleEntity getEntity1() {
		SimpleEntity entity = new SimpleEntity();
		return entity;
	}

	/**
	 * @return {@link #getEntity1()} + field set to "my field 1"
	 */
	public static SimpleEntity getEntity2() {
		SimpleEntity entity = getEntity1();
		entity.setField("my field 1");
		return entity;
	}

	/**
	 * @return {@link #getEntity2()} + one empty child entity
	 */
	public static SimpleEntity getEntity3_1() {
		SimpleEntity entity = getEntity2();
		ChildEntity centity = new ChildEntity();
		entity.addToSentities(centity);
		
		return entity;
	}

	/**
	 * @return {@link #getEntity2()} + one child entity with field "my child 1 field"
	 */
	public static SimpleEntity getEntity3_2() {
		SimpleEntity entity = getEntity2();
		ChildEntity centity = new ChildEntity();
		centity.setValue("my child 1 field");
		entity.addToSentities(centity);
		
		return entity;
	}
	
	/**
	 * @return {@link #getEntity3_1()} + one empty child entity
	 */
	public static SimpleEntity getEntity4_1() {
		SimpleEntity entity = getEntity3_1();
		ChildEntity centity = new ChildEntity();
		entity.addToSentities(centity);
		
		return entity;
	}
	
	/**
	 * @return {@link #getEntity3_2()} + one child entity with field "my child 2 field"
	 */
	public static SimpleEntity getEntity4_2() {
		SimpleEntity entity = getEntity3_2();
		ChildEntity centity = new ChildEntity();
		centity.setValue("my child 2 field");
		entity.addToSentities(centity);
		
		return entity;
	}
	
}
