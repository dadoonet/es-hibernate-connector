package org.elasticsearch.orm.hibernate;

import java.util.ArrayList;
import java.util.Collection;

import org.elasticsearch.orm.hibernate.annotations.ESField;
import org.elasticsearch.orm.hibernate.annotations.ESIndexed;

@ESIndexed
public class SimpleEntity {
	@ESField
	private String field;

	@ESField
	private Collection<String> stringsfield = new ArrayList<String>(); 
	
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
