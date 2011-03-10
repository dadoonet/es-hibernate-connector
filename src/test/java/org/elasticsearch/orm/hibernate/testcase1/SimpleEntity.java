package org.elasticsearch.orm.hibernate.testcase1;

import java.util.ArrayList;
import java.util.Collection;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Transient;

import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.elasticsearch.orm.hibernate.annotations.ESIndexed;
import org.hibernate.annotations.Cascade;

@Entity
@ESIndexed
public class SimpleEntity {
	@Id
	@GeneratedValue
	private Long id;

	@JsonSerialize
	private String field;

	// Won't be serialize so not annotated with JsonSerialize
	@Transient
	private Collection<String> stringsfield = new ArrayList<String>(); 
	
	@JsonSerialize
	@OneToMany( cascade = {CascadeType.PERSIST, CascadeType.MERGE} )
    @Cascade({org.hibernate.annotations.CascadeType.SAVE_UPDATE,
        org.hibernate.annotations.CascadeType.DELETE})
	private Collection<ChildEntity> sentities = new ArrayList<ChildEntity>(); 
	
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Collection<ChildEntity> getSentities() {
		return sentities;
	}

	public void setSentities(Collection<ChildEntity> sentities) {
		this.sentities = sentities;
	}

	public void addToSentities(ChildEntity entity) {
		this.sentities.add(entity);
	}

	public String getField() {
		return field;
	}

	public void setField(String field) {
		this.field = field;
	}

	public Collection<String> getStringsfield() {
		return stringsfield;
	}

	public void setStringsfield(Collection<String> stringsfield) {
		this.stringsfield = stringsfield;
	}

	public void addToStringsfield(String string) {
		this.stringsfield.add(string);
	}
}
