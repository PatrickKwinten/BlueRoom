package org.openntf.teamroom;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Properties;
import java.util.Vector;

import javax.faces.context.FacesContext;

import lotus.domino.Database;
import lotus.domino.Name;
import lotus.domino.NotesException;
import lotus.domino.Session;

import com.ibm.xsp.bluemix.util.context.BluemixContext;
import com.ibm.xsp.bluemix.util.context.BluemixContextManager;
import com.ibm.xsp.bluemix.util.context.DataService;
import com.ibm.xsp.extlib.util.ExtLibUtil;

public class DaoBean implements Serializable {
	private static final long serialVersionUID = 1L;
	private String server;
	private String servername;
	private String filepath;
	private String dbpath;
	private static final String STATIC_NSF_NAME = "tododata.nsf";

	public DaoBean() {
		getDatabase();
	}

	private Properties getDataSourceProperties() throws IOException {
		InputStream input = FacesContext.getCurrentInstance().getExternalContext().getResourceAsStream("datasource.properties");
		Properties props = new Properties();
		props.load(input);
		return props;
	}

	/**
	 * for local development you can define a separate datasource db
	 * @return
	 */
	public String getLocalDataSourceFilePath() {
		String fp = null;
		try {
			Properties props = getDataSourceProperties();
			fp = props.getProperty("DB_FILEPATH");
			return fp;
		} catch (Exception e) {
			return null;
		}
	}
	
	public String getServerName() {
		String fp = null;
		try {
			Properties props = getDataSourceProperties();
			fp = props.getProperty("DB_SERVERNAME");
			return fp;
		} catch (Exception e) {
			return null;
		}
	}

	private String getBluemixDatabaseFilename() {
		String fp = null;
		try {
			Properties props = getDataSourceProperties();
			fp = props.getProperty("DB_BLUEMIX_FILEPATH");
			return fp;
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * return the server name of the datasource db
	 * @return
	 */
	public String getServer() {
		return this.server;
	}

	private void setServer(String s) {
		this.server = s;
	}

	/**
	 * return the filepath to the datasource db
	 * @return
	 */
	public String getFilepath() {
		return this.filepath;
	}

	private void setFilepath(String s) {
		this.filepath = s;
	}

	public String getDbpath() {
		return this.dbpath;
	}

	private void setDbpath(String s) {
		this.dbpath = s;
	}

	/**
	 * return the database object itself (for i.e. fulltext searches)
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public Database getDatabase() {
		BluemixContext bc = getBluemixContext();
		Session session = ExtLibUtil.getCurrentSession();
		Database database = null;
		Name serverName;
		try {
			if (bc.isRunningOnBluemix()) {
				DataService ds = bc.getDataService();
				Vector v = ds.atDbName();
				serverName = session.createName(v.get(0).toString());
				setServer(serverName.getAbbreviated());
				String bluemixFileName = getBluemixDatabaseFilename();
				String defaultFilePath = v.get(1).toString();
				setFilepath(bluemixFileName != null ? defaultFilePath.replace(STATIC_NSF_NAME, bluemixFileName) : v.get(1).toString());
			} else {
				String fp = getLocalDataSourceFilePath();
				serverName = session.createName(session.getCurrentDatabase().getServer());
				setServer(serverName.getAbbreviated());
				setFilepath(fp != null ? fp : session.getCurrentDatabase().getFilePath());
			}
			setDbpath(getServer() + "!!" + getFilepath());
			database = session.getDatabase(getServer(), getFilepath());
		} catch (NotesException e) {
			e.printStackTrace();
		}
		return database;
	}

	public BluemixContext getBluemixContext() {
		return BluemixContextManager.getInstance();
	}
}