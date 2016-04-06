package org.openntf.teamroom;

import java.util.ArrayList;
import java.util.Map;
import java.util.List;
import java.util.Vector;

import javax.faces.context.FacesContext;
import javax.faces.component.UIComponent;

import lotus.domino.Database;
import lotus.domino.Name;
import lotus.domino.NotesException;
import lotus.domino.Session;
import lotus.domino.View;
import lotus.domino.ViewEntry;
import lotus.domino.ViewEntryCollection;

import com.ibm.commons.util.io.json.JsonException;
import com.ibm.commons.util.io.json.JsonJavaFactory;
import com.ibm.commons.util.io.json.JsonJavaObject;
import com.ibm.commons.util.io.json.JsonParser;
import com.ibm.xsp.binding.PropertyMap;

public class JSFUtil {
	
	//The DaoBean contains information where the data database is located e.g. on premise or on bluemix
	static DaoBean dao = new DaoBean();
	
	private String userRoles;
	
	public static Boolean debugMode = true;
	
	public JSFUtil() throws NotesException{
		System.out.println("JSFUtil.java - Constructor started");	
		//debugMode = true;
	}

	public static Map getApplicationScope() {
		return (Map) resolveVariable("applicationScope");
	}

	public Database getCurrentDatabase() {
		return (Database) resolveVariable("database");
	}

	public static Map getRequestScope() {
		return (Map) resolveVariable("requestScope");
	}

	public static Session getSession() {
		return (Session) resolveVariable("session");
	}

	public Session getSessionAsSigner() {
		return (Session) resolveVariable("sessionAsSigner");
	}

	public static Map getSessionScope() {
		return (Map) resolveVariable("sessionScope");
	}

	public static PropertyMap getViewScope() {
		return (PropertyMap) resolveVariable("viewScope");
	}

	public static Object resolveVariable(String variable) {
		return FacesContext.getCurrentInstance().getApplication().getVariableResolver().resolveVariable(FacesContext.getCurrentInstance(), variable);
	}

    public static UIComponent findComponent(UIComponent topComponent, String compId) {
        if (compId==null){
        	throw new NullPointerException("Component identifier cannot be null");
        }
        if (compId.equals(topComponent.getId())){
        	return topComponent;
        }
        if (topComponent.getChildCount()>0) {
        	List<UIComponent> childComponents=topComponent.getChildren();
        	for (UIComponent currChildComponent : childComponents) {
        		UIComponent foundComponent=findComponent(currChildComponent, compId);
        		if (foundComponent!=null){
        			return foundComponent;
        		}
        	}
        }
        return null;
    } 	

    public static Name getCurrentUser() {
		Session session = getSession();
		try {
			return session.createName(session.getEffectiveUserName());
		} catch (NotesException e) {
			e.printStackTrace();
			return null;
		}			
	}
    
	public static Session getCurrentSession() {
		FacesContext context = FacesContext.getCurrentInstance();
		return (Session) context.getApplication().getVariableResolver()
				.resolveVariable(context, "session");
	}
	
	public String getUserRoles() {
		return userRoles;
	}
	
	public boolean hasRole(String roleName) {
		return (this.userRoles.indexOf(roleName) != -1);
	}	
	
	public static ArrayList<JsonJavaObject> loadJSONObjects(String ServerName, String DatabaseName, String ViewName, String Key, Integer ColIdx) throws NotesException {
		System.out.println("kut" );
		if (JSFUtil.debugMode == true){
			System.out.println("JSFUtil.java loadJSONObjects(..., ..., ..., ..., ...)" );
		}
		
		ArrayList<JsonJavaObject> JSONObjects = new ArrayList<JsonJavaObject>();
		Database dataDB = dao.getDatabase();		
		if (!(dataDB==null)) {
			View luView = dataDB.getView(ViewName); 
			if (!(luView == null)) {
				JsonJavaFactory factory = JsonJavaFactory.instanceEx;	
				ViewEntryCollection vec = null;
				if (Key == ""){
					vec = luView.getAllEntries();		
				}
				else{
					vec = luView.getAllEntriesByKey(Key, true);		
				}
				ViewEntry entry = vec.getFirstEntry();
				if (JSFUtil.debugMode == true){
					System.out.println("JSFUtil.java loadJSONObjects(...). vec.count = " + vec.getCount());
				}								
				while (entry != null) {
					Vector<?> columnValues = entry.getColumnValues();
					String colJson = String.valueOf(columnValues.get(ColIdx));
					JsonJavaObject json = null;
					try {
						json = (JsonJavaObject) JsonParser.fromJson(factory, colJson);
						
						//json.put("comments", 10);
						if (JSFUtil.debugMode == true){
							System.out.println("JSFUtil.java loadJSONObjects(...). JSON: " + json);	
						}											
						if (json != null) {
							JSONObjects.add(json);
						}
					} catch (JsonException e) {
						// TODO Auto-generated catch block
						//e.printStackTrace();			
						if (JSFUtil.debugMode == true){
							System.out.println("ERROR: JSFUtil.java loadJSONObjects(...). colJson: " + colJson);	
						}						
					}					
				   ViewEntry tempEntry = entry;
				   entry = vec.getNextEntry();
				   tempEntry.recycle();
				} 	
				luView.recycle();
			}
			dataDB.recycle();
		}	
		return JSONObjects;
	}
	
	
	public static JsonJavaObject loadJSONObject(String ServerName, String DatabaseName, String ViewName, String Key, Integer ColIdx) throws NotesException {
		System.out.println("lul" );
		JsonJavaObject json = null;
		
		Database dataDB = dao.getDatabase();	
		
		if (!(dataDB==null)) {
			
			if (!(dataDB.isOpen())) {
				System.out.println("JSFUtil.java: loadJSONObject: found DB: but DB is NOT open: " + dataDB.isOpen() );
				dataDB.open();
			}
			
			View luView = dataDB.getView(ViewName); 
			if (!(luView == null)) {
				luView.setAutoUpdate(false);
				ViewEntry entry = luView.getEntryByKey(Key);
				if (!(entry == null)) {
					
					Vector<?> columnValues = entry.getColumnValues();
					String colJson = String.valueOf(columnValues.get(ColIdx));
					
					try {
						JsonJavaFactory factory = JsonJavaFactory.instanceEx;
						json = (JsonJavaObject) JsonParser.fromJson(factory, colJson);
					} catch (JsonException e) {
						System.out.println("ERROR: JSFUtil.java loadJSONObject 1: colJson: " + colJson);
					}
					
					entry.recycle();
				}
				luView.recycle();
			}
			dataDB.recycle();
		}
		return json;
	}
	

}

