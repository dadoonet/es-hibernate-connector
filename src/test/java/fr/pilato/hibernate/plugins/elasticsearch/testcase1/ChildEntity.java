package fr.pilato.hibernate.plugins.elasticsearch.testcase1;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.codehaus.jackson.map.annotate.JsonSerialize;

@Entity
public class ChildEntity {
	@Id
	@GeneratedValue
	private Long id;

	@JsonSerialize
	private String value;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}
	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}
}
