package org.aksw.dependency.rule;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.Collection;

public class RuleModel {

	private Collection<Rule> rules;

	public RuleModel(Collection<Rule> rules) {
		this.rules = rules;
	}
	
	public Collection<Rule> getRules() {
		return rules;
	}
	
	public static RuleModel fromFile(File file){
		InputStream fis = null;
		try {
			fis = new FileInputStream(file);
			ObjectInputStream o = new ObjectInputStream(fis);
			Collection<Rule> rules = (Collection<Rule>) o.readObject();
			return new RuleModel(rules);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			System.err.println(e);
		} finally {
			try {
				fis.close();
			} catch (Exception e) {
			}
		}
		return null;
	}
}
