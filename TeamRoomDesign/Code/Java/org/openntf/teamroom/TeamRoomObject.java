package org.openntf.teamroom;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import lotus.domino.NotesException;

import com.ibm.commons.util.io.json.JsonJavaObject;


/**
 * @author Patrick Kwinten
 * http://quintessens.wordpress.com
 */
public class TeamRoomObject implements Serializable {

	//The Properties
	private static final long serialVersionUID = 1L;
	
	//what is the type of the current object? comes from the DataSource Definition document e.g. member, team, document...
	private String type;
	
	//the data from the current object (comes from Notes view)
	public JsonJavaObject data;
	
	//relatedObjects are the type of objects the current object is related to (comes from Notes view)
	public ArrayList<JsonJavaObject> relatedObjects;
	
	private Boolean relatedObjectsLoaded = false;
	
	
	//private Boolean relationTypesLoaded = false;
	private Map<String, ArrayList<JsonJavaObject>> subRelations = new HashMap<String, ArrayList<JsonJavaObject>>();
	private String currRelatedObject;
	private String currRelation;
	
	
	//Constructor
	public TeamRoomObject(String objectType, String objectKey) {
		System.out.println("TeamRoomObject.java - Constructor started");	
		System.out.println("TeamRoomObject.java - objectType = " + objectType);	
		System.out.println("TeamRoomObject.java - objectKey = " + objectKey);	
		this.type = objectType;
		this.loadBackEndData(objectKey);		
		//this.loadRelationTypes();
	}
	
	//The Methods
	private void loadBackEndData(String key) {		
		System.out.println("TeamRoomObject.java - loadBackEndData(String Key). key = " + key);	
		try {
			JsonJavaObject backEndConfiguration = TeamRoom.getCurrentInstance().loadBackEndConfiguration(this.type);			
			if (!(backEndConfiguration == null)) {
				//Use TeamRoom
				this.data = TeamRoom.getCurrentInstance().loadJSONObject(backEndConfiguration, key);
			}			
		} catch (NotesException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.out.println("ERROR: TeamRoomObject.loadBackEndData");
		}		
	}
	
	

	private  void loadRelatedObjects() {
		System.out.println("TeamRoomObject.class - loadRelatedObjects()");
		try {
			//Use the TeamRoom class
			ArrayList<JsonJavaObject> relatedObjectsArr = TeamRoom.getCurrentInstance().loadRelatedObjects(this.type);
			if (!(relatedObjectsArr == null)) {
				this.relatedObjects = relatedObjectsArr;
				this.relatedObjectsLoaded = true;
			}			
		} catch (NotesException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
		//try to load initial a default related Object?
		for (JsonJavaObject relatedObject : this.relatedObjects) {
			if (relatedObject.getAsString("initialLoad") != null ) {
				this.loadRelations(relatedObject.getAsString("category"));
				break; //only the first
			}
		}	
	}
	
	public void loadRelations(String relatedObject) {
		Boolean loadAll = false; 
		if (relatedObject.endsWith("#loadAll")) {
			//button "alle" klicked
			loadAll = true;
			relatedObject = relatedObject.substring(0, (relatedObject.indexOf("#")));
		}
		this.currRelatedObject = relatedObject;
		this.currRelation = "all";
		//get the relationTypeConfig
		JsonJavaObject relatedObjectConfiguration = this.getRelatedObjectConfiguration(relatedObject);
		//get all Configs according to relationType
		ArrayList<JsonJavaObject> relatedObjectConfigurations = this.getRelatedObjectConfigurations(relatedObject);
		//remember the Configs
		ArrayList<JsonJavaObject> relationConfigsToLoad = new ArrayList<JsonJavaObject>(relatedObjectConfigurations);
		ArrayList<JsonJavaObject> relationConfigsToCheck = new ArrayList<JsonJavaObject>(relatedObjectConfigurations);
		if (!loadAll) {
			//maybe respect defaultRelationConfig --> so manipulate the ArrayLists
			if (relatedObjectConfiguration != null) {
				String DefaultRelation = relatedObjectConfiguration.getAsString("defaultRelation");
				if (DefaultRelation != null && (!DefaultRelation.equals(""))) {
					JsonJavaObject defaultRelationConfig = this.getRelationConfiguration(DefaultRelation);
					if (defaultRelationConfig != null) {
						//remove defaultRelationConfig
						relationConfigsToCheck.remove(defaultRelationConfig);
						
						//preload all Relations (but do nothing else), so we get the counter to determine, if we had to show the Sub-Relation Button
						this.handleRelationObjects(relationConfigsToCheck, null);
						
						//only keep defaultRelationConfig
						relationConfigsToLoad.clear();
						relationConfigsToLoad.add(defaultRelationConfig);
						this.currRelation = defaultRelationConfig.getAsString("name");
						
						System.out.println("TeamRoomObject.loadRelations: relcfgToLoad: " + relationConfigsToLoad.size() + ", refCfgToCheck: " + relationConfigsToCheck.size() );
					}
				}
			}		
		}
		
		
		//get Relations an store them in SSApp
		this.handleRelationObjects(relationConfigsToLoad, relatedObjectConfiguration);

	}
	
	private JsonJavaObject getRelatedObjectConfiguration(String key){		
		JsonJavaObject json = null;		
		for (JsonJavaObject relationDef : this.relatedObjects) {
			if (relationDef.getAsString("category").equals(key)) {
				json = relationDef;
				break;
			}
		}		
		return json;
	}
	
	private ArrayList<JsonJavaObject> getRelatedObjectConfigurations(String relationType) {
		
		//maybe already loaded?
		ArrayList<JsonJavaObject> relationConfigs = this.subRelations.get(relationType);
			
		if (relationConfigs == null) {
			// load all Configs according to categoryKey
			//System.out.println("SSO.getSubRelationConfigs: load relationConfigs...");
			
			try {
				relationConfigs = TeamRoom.getCurrentInstance().loadRelationConfigurations(relationType);
			} catch (NotesException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			//put the subRelationsConfigs in Map
			this.subRelations.put(relationType, relationConfigs);
		}
		
		return relationConfigs;
	}
	
	private JsonJavaObject getRelationConfiguration(String relationName) {
		JsonJavaObject relationConfig = null;		
		//maybe already loaded?
		 for (ArrayList<JsonJavaObject> subRelations : this.subRelations.values()) {			 
			 for (JsonJavaObject subRelation : subRelations) {
				 if (subRelation.getAsString("name").equals(relationName)) {
					 relationConfig = subRelation;
					 return subRelation;
				 }
			 }
		 }		 
		 if (relationConfig == null) {
			 try {
				relationConfig = TeamRoom.getCurrentInstance().loadRelationConfiguration(relationName);
			} catch (NotesException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		 }		 
		 return relationConfig;
	}
	
private void handleRelationObjects(ArrayList<JsonJavaObject> relationConfigurations, JsonJavaObject afterCareConfiguration) {
		
		ArrayList<JsonJavaObject> relationObjects = new ArrayList<JsonJavaObject>();
		
		//loop over all Configs
		for (JsonJavaObject relationConfiguration : relationConfigurations) {

			//build LookUp Key
			this.provideRelationKey(relationConfiguration);
				
			//get the relations
			ArrayList<JsonJavaObject> jsonObjects = new ArrayList<JsonJavaObject>();
			try {
				jsonObjects = TeamRoom.getCurrentInstance().loadJSONObjects(relationConfiguration);
			} catch (NotesException e) {
				System.out.println("ERROR SSObject.handleRelationObjects (SSApp.loadJsonObjects)");
				e.printStackTrace();
			}
			
			
			//add a Result-Counter and an Information about the loaded state
			relationConfiguration.put("count", jsonObjects.size());
			relationConfiguration.put("loaded", true);
			
			//maybe build additional Fields
			this.provideRelationFields(jsonObjects, relationConfiguration);

			//Add to Array
			relationObjects.addAll(jsonObjects);
		}
		
		//Add to currentObjectCollection and -Filterd 
		TeamRoom.getCurrentInstance().setCurrObjectCollection(relationObjects);
		//TeamRoom.getCurrentInstance().setCurrentObjectCollectionFiltered(relationObjects);
		
		//aftercare
		if (!(afterCareConfiguration == null )) {
			
			//ContentType configurated?
			this.handleCollectionType(afterCareConfiguration);
			
			//Filter configurated?
			//this.handleFilter(afterCareConfiguration);
			
			//DefaultSort provided?
			this.handleSort(afterCareConfiguration);
		}
		
		
		//TODO: ggf. spez. Config?
		//KPIManager kpiManager = new KPIManager(SSApp.getCurrentInstance().getCurrentObjectCollectionType());
		//TeamRoom.getCurrentInstance().setKPIManager(kpiManager);
	}

private void handleCollectionType(JsonJavaObject relatedObjectConfiguration) {
	//ContentType configurated
	String contentType = (String) relatedObjectConfiguration.get("type");
	
	if (!(contentType == null || contentType.equals(""))) {
		TeamRoom.getCurrentInstance().setCurrObjectCollectionType(contentType);
		TeamRoom.getCurrentInstance().loadCollectionCardType();
	}
}

private void handleSort(JsonJavaObject relatedObjectConfiguration) {
	String sortConfig = relatedObjectConfiguration.getAsString("collectionSort");
	
	if (sortConfig != null) {
		String[] srtCfgEle = sortConfig.split("#");
		TeamRoom.getCurrentInstance().sortCurrObjectCollection(srtCfgEle[0], (srtCfgEle[1] == "1"));
	}
}
	
@SuppressWarnings("unchecked")
private void provideRelationKey(JsonJavaObject relationConfig) {
	
	//maybe do nothing, if the key already exists
	Object keyObj = relationConfig.get("key");
	if (keyObj != null) {
		return;
	}
	
	// Build Key
	ArrayList<JsonJavaObject> keyConfigArr = (ArrayList<JsonJavaObject>) relationConfig.get("keyConfig");
	
	if (!(keyConfigArr == null)) {
		Object relationKey = this.buildRelationLookUpKey(keyConfigArr);
		
		//System.out.println("SSObject.provideRelationKey: relationKey: " + relationKey);
		
		if (relationKey != null && (!relationKey.equals(""))) {
			//add the builded key in relationConfig
			relationConfig.put("key", relationKey);		
		}
		
		//System.out.println("SSObject.provideRelationKey: relationConfig: " + relationConfig);
		
	}
}

@SuppressWarnings("unchecked")
private void provideRelationFields(ArrayList<JsonJavaObject> jsonObjects, JsonJavaObject relationConfig) {
	//manipulate the Array: e.g. add a Key-Value Pair specified by relationConfig
	ArrayList<JsonJavaObject> fieldConfig = (ArrayList<JsonJavaObject>) relationConfig.get("fieldConfig");

	if (!(fieldConfig == null)) {
		//System.out.println("SSO: loadRelations: found fieldConfig: " + fieldConfig);
		this.buildRelationFields(jsonObjects, fieldConfig);
	}
}

@SuppressWarnings("unchecked")
private ArrayList<JsonJavaObject> buildRelationFields(ArrayList<JsonJavaObject> relationObjects, ArrayList<JsonJavaObject> fieldConfigArr) {
	
	for (JsonJavaObject JSONObj : relationObjects) {
		
		for (JsonJavaObject fieldConfig : fieldConfigArr) {

			String cfg_fieldName = (String) fieldConfig.get("fieldName");
			String cfg_method = (String) fieldConfig.get("method");
	
			if (cfg_method.equals("remove")) {
				JSONObj.remove(cfg_fieldName);
			} else {
				//add or replace
				
				//Build new Value
				ArrayList<JsonJavaObject> valueConfig = (ArrayList<JsonJavaObject>) fieldConfig.get("fieldValue");

				if (!(valueConfig == null)) {
						
					//build the new value
					String newValue = "";
					
					for (JsonJavaObject valueObject : valueConfig) {
						for (Iterator<String> iterator = valueObject.keySet().iterator(); iterator.hasNext();) {
							String cfg_key = (String) iterator.next();
							String cfg_value = (String) valueObject.get(cfg_key);

							if (cfg_key.equals("String")) {
								newValue += cfg_value;
							} else if (cfg_key.equals("data.get")) {
								newValue += (String) this.data.get(cfg_value);
							} else if (cfg_key.equals("JSONObj.get")) {
								newValue += (String) JSONObj.get(cfg_value);
							}
						}
					}
			
					if (cfg_method.equals("add")) {
						Object oldValue = JSONObj.get(cfg_fieldName);
						
						if (oldValue != null) {
							if (oldValue instanceof ArrayList) {
								ArrayList<String> newValues = (ArrayList<String>) oldValue;
								newValues.add(newValue);
								JSONObj.put(cfg_fieldName, newValues);
							} else if (oldValue instanceof String) {
								newValue = oldValue + newValue;
								JSONObj.put(cfg_fieldName, newValue);
							}
						} else {
							JSONObj.put(cfg_fieldName, newValue);
						}
					} else {
						JSONObj.put(cfg_fieldName, newValue);
					}
					
				}
				
			}
		}
	}
	return relationObjects;
}
	


@SuppressWarnings("unchecked")
private ArrayList<String> buildRelationLookUpKeyComplex(ArrayList<Object> relationKeys, Integer maxEntries) {
	//first we build an Array of Arrays, witch has the the size of maxEntries
	ArrayList<ArrayList<String>> hlpArray = new ArrayList<ArrayList<String>>();
	
	for (Object entryObj: relationKeys) {
		if (entryObj instanceof String) {
			//create a new Array
			String entryString = (String) entryObj;
			ArrayList<String> entryArray = new ArrayList<String>();
			for(int i=0; i<maxEntries; i++) {
				entryArray.add(entryString);
			}
			hlpArray.add(entryArray);
		}else{
			//add the Array directly, but Cast it
			hlpArray.add((ArrayList<String>) entryObj);
		}
	}
	
	//now we have an Array of Arrays, witch has all the same size (maxEntries)
	ArrayList<String> resultArray = new ArrayList<String>(maxEntries); // Make a new list
	for (int i=0; i<maxEntries; i++) { // Loop through every array
		StringBuilder builder = new StringBuilder();
		for (ArrayList arr : hlpArray) {
		    builder.append((String) arr.get(i));
		}
		resultArray.add(builder.toString());
	}
	
	return resultArray;
}


@SuppressWarnings("unchecked")
private Object buildRelationLookUpKey(ArrayList<JsonJavaObject> keyConfig) {
	
	//String relationKey = "";
	ArrayList<Object> relationKeys = new ArrayList<Object>();
	
	Boolean hasArray = false;
	Boolean hasMultipleArray = false;
	Integer maxEntries = 1;
	
	//loop over all configs and collect the results in an ArrayList
	for (JsonJavaObject keyObject : keyConfig) {
		
		for (Iterator<String> iterator = keyObject.keySet().iterator(); iterator.hasNext();) {
			String cfg_key = (String) iterator.next();
			String cfg_value = (String) keyObject.get(cfg_key);

			if (cfg_key.equals("String")) {
				relationKeys.add(cfg_value);
			} else if (cfg_key.equals("data.get")) {
				Object valueObj = this.data.get(cfg_value);
				
				if ((valueObj instanceof ArrayList)) {
					relationKeys.add((ArrayList<String>) valueObj);
					//remember, that we have at least one array (in our Array)
					hasMultipleArray = (hasArray) ? true : hasMultipleArray; //if we already have an Array its going to be complicated
					hasArray = true;
					maxEntries = Math.max(maxEntries, ((ArrayList) valueObj).size());
				}else{
					relationKeys.add((String) valueObj);		
				}
			}
		}

	}
	
	//no we need to put all values together
	//System.out.println("SSObject.buildRelationLookUpKey: hA: " + hasArray + "; mE: " + maxEntries + ", rKs: " + relationKeys.size() + ", rK: " + relationKeys);

	if (hasArray) {
		
		if (hasMultipleArray) {
			//TODO: not handled so far!
			System.out.println("ERROR: SSObject.buildRelationLookUpKey: to many Arrays! I give up! Change your RelationConfig!");
			System.out.println("SSObject.buildRelationLookUpKey: hA: " + hasArray + "; mE: " + maxEntries + ", rKs: " + relationKeys.size() + ", rK: " + relationKeys);
			return null;
		}
		
		if (relationKeys.size()==1) {
			//if the array has only one Element: --> return that Element (in this case an Array)
			return relationKeys.iterator().next();
		}else{
			return this.buildRelationLookUpKeyComplex(relationKeys, maxEntries);
		}
		
	
	}else{
		//Build a single string
		StringBuilder builder = new StringBuilder();
		//Append all Objects as String to the StringBuilder.
		for (Object entry : relationKeys) {
		    builder.append((String) entry);
		}
		//System.out.println("SSObject.buildRelationLookUpKey: SBuilder: " + builder.toString());
		return builder.toString();
	}
	
}



//Getter Setter	
public String getType() {
	return type;
}

public Map<String, ArrayList<JsonJavaObject>> getSubRelations() {
	return subRelations;
}


public JsonJavaObject getData() {
	return data;
}

public ArrayList<JsonJavaObject> getRelatedObjects() {
	
	this.loadRelatedObjects();
//	if(!(this.relatedObjectsLoaded)) {
//		this.loadRelatedObjects();
//	}
	return relatedObjects;
}

public String getCurrRelatedObject() {
	return currRelatedObject;
}

public String getCurrRelation() {
	return currRelation;
}
	

	
}
