package org.openntf.teamroom;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Vector;

import javax.faces.context.FacesContext;

import lotus.domino.Database;
import lotus.domino.NotesException;
import lotus.domino.Session;
import lotus.domino.View;
import lotus.domino.ViewEntry;
import lotus.domino.ViewEntryCollection;

import org.openntf.teamroom.DaoBean;
import org.openntf.teamroom.JSONComparator;
import org.openntf.teamroom.TeamRoomObject;
import org.openntf.xsp.debugtoolbar.beans.DebugToolbarBean;

import com.ibm.commons.util.io.json.JsonException;
import com.ibm.commons.util.io.json.JsonJavaFactory;
import com.ibm.commons.util.io.json.JsonJavaObject;
import com.ibm.commons.util.io.json.JsonParser;
import com.ibm.domino.xsp.module.nsf.NotesContext;



/**
 * @author Patrick Kwinten
 * http://quintessens.wordpress.com
 */
public class TeamRoom implements Serializable {
	
	//The Properties
	private static final long serialVersionUID = 1L;
	
	public TeamRoomObject currObject;
	private ArrayList<JsonJavaObject> currObjects = new ArrayList<JsonJavaObject>();
	private ArrayList<JsonJavaObject> currObjectsFiltered = new ArrayList<JsonJavaObject>();
	
	private String currObjectsType = "";
	private String currObjectsCard = "cardDefault";
	private String currObjectsDisplayType = "default";
	private String currObjectsTarget = "_self";
	
	private String currObjectsTargetType = "_self";
	
	//The DaoBean contains information where the data database is located e.g. on premise or on bluemix
	static DaoBean dao = new DaoBean();
	
	//True means logging to OpenLog plus debugtoolbar (for admins)
	public static Boolean debugMode = true;

	// The Constructor
	public TeamRoom(){
		System.out.println("TeamRoom.java - Constructor started");	
		//DebugToolbarBean.get().warn( "Hello OpenLog" );
		TeamRoom.debugMode = true;		
	}

	//The methods
	public void reset() {
		this.currObject = null;
		this.currObjects.clear();
		this.currObjectsType = "";
		this.currObjectsCard = "cardDefault";
		this.currObjectsDisplayType = "default";
		this.currObjectsTarget = "_self";
		
		
	}
	
	public TeamRoomObject getCurrObject() {
		return currObject;
	}
	
	public void setCurrObjectCollectionType(String currObjectCollectionType) {
		this.currObjectsType = currObjectCollectionType;
	}
	
	public void setCurrObjectCollection(ArrayList<JsonJavaObject> currObjectCollection) {
		this.currObjects = currObjectCollection;
	}
	
	public static TeamRoom getCurrentInstance() {
		// This is a neat way to get a handle on the instance of this bean in the application scope from other Java code...
		FacesContext context = FacesContext.getCurrentInstance();
		TeamRoom bean = (TeamRoom) context.getApplication().getVariableResolver().resolveVariable(context, "TeamController");
		return bean;
	}
	
	public JsonJavaObject loadBackEndConfiguration(String key) throws NotesException {		
		System.out.println("TeamRoom.class - loadBackEndConfiguration(String Key). key = " + key);
		JsonJavaObject json = null;
		NotesContext nct = NotesContext.getCurrent();
		Session session = nct.getCurrentSession();	
		String sName = dao.getServerName();
		//String dbName = session.getCurrentDatabase().getFilePath();
		String dbName =  dao.getLocalDataSourceFilePath();
		String vwName = "(sDataSource)";
		try{
			json = loadJSONObject(sName, dbName, vwName, key, 1);
		}
		catch (NotesException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return json;
	}
	
	public ArrayList<JsonJavaObject> loadJSONObjects(JsonJavaObject luConfig) throws NotesException {

		ArrayList<JsonJavaObject> JSONObjects = new ArrayList<JsonJavaObject>();
		
		//Build Parameters
		String serverName = (String) luConfig.get("serverName");
		String databaseName = (String) luConfig.get("dbName");
		String viewName = (String) luConfig.get("viewName");
		String colIdx = (String) luConfig.get("columnIndex");
		
		Object keyObj = luConfig.get("key");
		
		if (keyObj instanceof String) {
			String key = (String) keyObj;
			JSONObjects = this.loadJSONObjects(serverName, databaseName, viewName, key, Integer.parseInt(colIdx));
		} else if (keyObj instanceof ArrayList) {
			@SuppressWarnings("unchecked")
			ArrayList<String> key = (ArrayList<String>) keyObj;
			JSONObjects = this.loadJSONObjects(serverName, databaseName, viewName, key, Integer.parseInt(colIdx));
		}
		
		return JSONObjects;
	}
	
public ArrayList<JsonJavaObject> loadJSONObjects(String ServerName, String DatabaseName, String ViewName, ArrayList<String> Keys, Integer ColIdx) throws NotesException {
		
		ArrayList<JsonJavaObject> JSONObjects = new ArrayList<JsonJavaObject>();
		
		NotesContext nct = NotesContext.getCurrent();
		Session session = nct.getCurrentSession();
		Database dB = session.getDatabase(ServerName, DatabaseName);
		
		if (!(dB==null)) {
			
			View luView = dB.getView(ViewName); 
			
			if (!(luView == null)) {
	
				JsonJavaFactory factory = JsonJavaFactory.instanceEx;
				
				for (String key: Keys) {
	
					ViewEntryCollection vec = luView.getAllEntriesByKey(key, true);
					
					ViewEntry entry = vec.getFirstEntry();
					while (entry != null) {
						
						Vector<?> columnValues = entry.getColumnValues();
						String colJson = String.valueOf(columnValues.get(ColIdx));
						
						JsonJavaObject json = null;
						
						try {
							json = (JsonJavaObject) JsonParser.fromJson(factory, colJson);
							
							if (json != null) {
								//füge das PersonJSON-Object in meine ArrayList
								JSONObjects.add(json);
							}
						} catch (JsonException e) {
							//e.printStackTrace();
							System.out.println("ERROR: SSApp.loadJsonObjects 2: colJson: " + colJson);
						}
							
					   ViewEntry tempEntry = entry;
					   entry = vec.getNextEntry();
					   tempEntry.recycle();
					}
				} 	
				luView.recycle();
			}
			dB.recycle();
		}
		return JSONObjects;
	}
	
	
	public JsonJavaObject loadJSONObject(String serverName, String dbName, String viewName, String key, Integer colIdx) throws NotesException {
		System.out.println("TeamRoom.class - loadJSONObject(String serverName, String dbName, String viewName, String key, Integer colIdx)");
		System.out.println("... serverName = " + serverName);
		System.out.println("... dbName = " + dbName);
		System.out.println("... viewName = " + viewName);
		System.out.println("... key = " + key);
		System.out.println("... colIdx = " + colIdx);
		
			JsonJavaObject json = null;			
			NotesContext nct = NotesContext.getCurrent();
			Session session = nct.getCurrentSession();
			if (serverName.equals("")){
				serverName = dao.getServerName();
			}
			if (dbName.equals("")){
				dbName = dao.getLocalDataSourceFilePath();
			}
			Database DB = session.getDatabase(serverName, dbName);			
			if (!(DB==null)) {				
				if (!(DB.isOpen())) {
					System.out.println("TeamRoom: loadJSONObject: found DB: but DB is NOT open: " +DB.isOpen() );
					DB.open();
				}				
				View luView = DB.getView(viewName); 
				if (!(luView == null)) {					
					ViewEntry entry = luView.getEntryByKey(key);
					if (!(entry == null)) {						
						Vector<?> columnValues = entry.getColumnValues();
						String colJson = String.valueOf(columnValues.get(colIdx));						
						try {
							JsonJavaFactory factory = JsonJavaFactory.instanceEx;
							json = (JsonJavaObject) JsonParser.fromJson(factory, colJson);
						} catch (JsonException e) {
							System.out.println("ERROR: TeamRoom.loadJSONObject 1: colJson: " + colJson);
						}						
						entry.recycle();
					}
					luView.recycle();
				}
				DB.recycle();
			}
			System.out.println("json: " + json.toString());
			return json;
		}
	
	public JsonJavaObject loadJSONObject(JsonJavaObject backEndConfig, String key) throws NotesException {	
		System.out.println("TeamRoom.class - loadJSONObject(JsonJavaObject backEndConfig, String key)");
		JsonJavaObject json = null;		
		//read JSON-Object
		String sName = (String) backEndConfig.get("serverName");
		String dbName = (String) backEndConfig.get("dbName");
		String vwName = (String) backEndConfig.get("viewName");
		String colIdx = (String) backEndConfig.get("columnIndex");			
		//use the other Method
		json = loadJSONObject(sName, dbName, vwName, key, Integer.parseInt(colIdx));		
		return json;
	}
	
	public ArrayList<JsonJavaObject> loadJSONObjects(String serverName, String dbName, String viewName, String key, Integer colIdx) throws NotesException {
		System.out.println("TeamRoom.class loadJSONObjects");
		ArrayList<JsonJavaObject> JSONObjects = new ArrayList<JsonJavaObject>();
		
		NotesContext nct = NotesContext.getCurrent();
		Session session = nct.getCurrentSession();
		Database DB = session.getDatabase(serverName, dbName);
		
		if (!(DB==null)) {
			View luView = DB.getView(viewName); 
			
			if (!(luView == null)) {
				JsonJavaFactory factory = JsonJavaFactory.instanceEx;
				
				ViewEntryCollection vec = luView.getAllEntriesByKey(key, true);

				ViewEntry entry = vec.getFirstEntry();
				while (entry != null) {
					
					/*View: 
					 * 1. Column: Key (sorted)
					 * 2. Column: JSON String: (z.b. {"docUNID": "FAE110220E57ECE7C12578A700375101","name": "Petar Angelchev","job": "Bauingenieur","pictureURL": "https://notes.voessing.de/Kontaktdatenbank.nsf/0/FAE110220E57ECE7C12578A700375101/$FILE/PortalPicture.jpg"})
					*/
					
					Vector<?> columnValues = entry.getColumnValues();
					String colJson = String.valueOf(columnValues.get(colIdx));
					JsonJavaObject json = null;
					
					try {
						json = (JsonJavaObject) JsonParser.fromJson(factory, colJson);
						if (json != null) {
							JSONObjects.add(json);
						}
							
					} catch (JsonException e) {
						System.out.println("ERROR: SSApp.loadJsonObjects 1: colJson: " + colJson);
					}
						
				   ViewEntry tempEntry = entry;
				   entry = vec.getNextEntry();
				   tempEntry.recycle();
				} 	
				luView.recycle();
			}
			DB.recycle();
		}
		return JSONObjects;
	}

	public void loadObject(String objectType, String key) {
		System.out.println("TeamRoom.class - loadObject(String objectType, String key). objectType = " + objectType + " , key = " + key);
		this.currObject = new TeamRoomObject(objectType, key);
	}
	
	public ArrayList<JsonJavaObject> loadRelatedObjects(String Key) throws NotesException {	
		System.out.println("TeamRoom.class - loadRelatedObjects(String Key). Key = " + Key);
		ArrayList<JsonJavaObject> relatedObjects = new ArrayList<JsonJavaObject>();		
		//get Info about Environment
		NotesContext nct = NotesContext.getCurrent();
		Session session = nct.getCurrentSession();
		String serverName = dao.getServerName();
		String dbName =  dao.getLocalDataSourceFilePath();
		String viewName = "(sDataRelatedObjects)";
		
		try {
			relatedObjects = loadJSONObjects(serverName, dbName, viewName, Key, 2);
		} catch (NotesException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return relatedObjects;
	}
	
public void sortCurrObjectCollection(String key, Boolean sortdescending) {
		
		System.out.println("Debug: TeamRoom.sortCurrentObjectCollection key: " + key);
		System.out.println("Debug: TeamRoom.sortCurrentObjectCollection sortdescending: " + sortdescending);
		JSONComparator jsonComparator = new JSONComparator(key, sortdescending);
		Collections.sort(this.currObjectsFiltered, jsonComparator);
	}

public void loadCollectionCardType() {
	
	String result = "cardSimple"; //Default
	String resultDsp = "default"; //Default
	String resultTarget = "_self"; //Default
	
	try {
		JsonJavaObject json = this.loadInteractionConfig(this.currObjectsType + "_CollectionCardType");
		
		if (!(json == null)) {
			JsonJavaObject configJson = (JsonJavaObject) json.get("Config");
			
			if (!(configJson == null)) {
		
				result = configJson.getAsString("cardType");
				resultDsp = configJson.getAsString("displayType");
				resultTarget = configJson.getAsString("targetType");
				
			}
		}
	} catch (NotesException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	//System.out.println("DEBUG SSApp.loadCollectionCardType: currentObjectCollectionCardType " + result);
	//System.out.println("DEBUG SSApp.loadCollectionCardType: currentObjectCollectionDisplayType " + resultDsp);
	this.currObjectsCard = result;
	this.currObjectsDisplayType = resultDsp;
	this.currObjectsTargetType = resultTarget;
}

public JsonJavaObject loadInteractionConfig(String Key) throws NotesException {
	
	JsonJavaObject json = null;
	
	//get Info about Environment
	NotesContext nct = NotesContext.getCurrent();
	Session session = nct.getCurrentSession();

	String ServerName = "dev1";
	String DatabaseName = session.getCurrentDatabase().getFilePath();
	String ViewName = "(LookUpInteractionConfig)";
	
	try {
		json = loadJSONObject(ServerName, DatabaseName, ViewName, Key, 1);
	} catch (NotesException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}

	return json;
}

public JsonJavaObject loadRelationConfiguration(String key) throws NotesException {
	
	JsonJavaObject json = null;
	
	//get Info about Environment
	NotesContext nct = NotesContext.getCurrent();
	Session session = nct.getCurrentSession();

	String serverName = "dev1";
	String databaseName = session.getCurrentDatabase().getFilePath();
	String viewName = "(LookUpRelationConfigByName)";
	
	try {
		json = loadJSONObject(serverName, databaseName, viewName, key, 2);
	} catch (NotesException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	
	return json;
}

public ArrayList<JsonJavaObject> loadRelationConfigurations(String Key) throws NotesException {
	
	ArrayList<JsonJavaObject> relationConfigs = new ArrayList<JsonJavaObject>();
	
	//get Info about Environment
	NotesContext nct = NotesContext.getCurrent();
	Session session = nct.getCurrentSession();

	String ServerName = "dev1";
	String DatabaseName = session.getCurrentDatabase().getFilePath();
	String ViewName = "(LookUpRelationConfig)";

	try {
		relationConfigs = loadJSONObjects(ServerName, DatabaseName, ViewName, Key, 2);
	} catch (NotesException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	
	return relationConfigs;
}
	

}
