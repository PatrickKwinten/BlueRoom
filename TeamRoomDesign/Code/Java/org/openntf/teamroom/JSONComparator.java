package org.openntf.teamroom;

import java.util.Comparator;
import com.ibm.commons.util.io.json.JsonJavaObject;

/**
 * @author hintzen
 */
public class JSONComparator implements Comparator<JsonJavaObject> {

	String key;
	Boolean descending = false;
	
	public JSONComparator (String key) {
		this.key = key;
	}
	

	public JSONComparator (String key, Boolean sortDescending) {
		this.key = key;
		this.descending = sortDescending;
	}

	
	public int compare(JsonJavaObject jsonObject1, JsonJavaObject jsonObject2) {
		
		int ret = 0;
		
		//first some simple Tests 
		if (this.key == null || this.key.equals("")) return 0;
		if (jsonObject1.get(key) == null && jsonObject2.get(key) == null) return 0;
		if (jsonObject1.get(key) == null) return 1;
		if (jsonObject2.get(key) == null) return -1;

		
		//Now consider the type of value
		if (jsonObject1.get(key) instanceof String) {
		  String val1 = (String) jsonObject1.get(key);
		  String val2 = (String) jsonObject2.get(key);
					  
		  ret = val1.compareToIgnoreCase(val2);

		}else if (jsonObject1.get(key) instanceof Integer) {
		  Integer val1 = (Integer) jsonObject1.get(key);
		  Integer val2 = (Integer) jsonObject2.get(key);
					  
		  ret = val1.compareTo(val2);

		}else if (jsonObject1.get(key) instanceof Double) {
		  Double val1 = (Double) jsonObject1.get(key);
		  Double val2 = (Double) jsonObject2.get(key);
						  
		  ret = val1.compareTo(val2);

		} else {
			//TODO: implement more comparisons
			return 0;
		}
		

		if (descending) {
			ret = ret*-1;
		}
		
		return ret;

	}

}
