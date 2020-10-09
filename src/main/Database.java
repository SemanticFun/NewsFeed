package main;

import java.io.FileReader;
import java.io.IOException;
import java.sql.*;

import org.json.JSONException;
import org.json.JSONObject;

/** Classe che si occupa della connessione e disconnessione al db, include
 * funzioni per il recupero delle informazioni*/

public class Database {

	/** Variabili della connessione **/
	private static Connection conn = null;
	private static String host = "";
	private static String port = "";
	private static String user = "";
	private static String pass = "";
	private static String dbname = "";

	/** Costruttore **/
	public Database() {
		
		// Recupero di dati di configurazione al DB attraverso l'apposito file Json
		String filePath = "/home/tomcat/somer/config.json";
		//String filePath = "./config.json";
		FileReader reader;
		
		//Lettura file
		try {
			reader = new FileReader(filePath);
			String fileContents = "";

			int j;

			while ((j = reader.read()) != -1) {
				char ch = (char) j;

				fileContents = fileContents + ch;
			}

			//Parsing del file Json
			JSONObject jsonObject = new JSONObject(fileContents);
			JSONObject feed = new JSONObject(jsonObject.get("connection").toString());
			
			host = feed.get("host").toString();
			port = feed.get("port").toString();
			user = feed.get("user").toString();
			pass = feed.getString("pass").toString();
			dbname = feed.getString("dbname").toString();

		} catch (IOException | JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		//Connessione al database
		Connect();

	}

	/** Funzione che si connette al database, dati i dati di connessione **/
	public static Connection Connect() {
		boolean answer = false;
		try {
			Class.forName("com.mysql.jdbc.Driver");
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			// PARAMETRI DI CONNESSIONE
			conn = DriverManager.getConnection("jdbc:mysql://" + host + ":"	+ port + "/" + dbname
					+ "?useUnicode=true&characterEncoding=utf-8", user, pass);
			answer = true;

		} catch (SQLException ex) {
			// Scrivo errore su file
			new WriteConsole(ex, "SQLException: " + ex.getMessage()
					+ " SQLState: " + ex.getSQLState() + " VendorError: "
					+ ex.getErrorCode());
		}
		if (answer == false) {
			System.out.println("Errore connessione database");
		}

		return conn;
	}

	/** Metodo che ritorna la connessione alle classi esterne **/
	public static Connection getConn() {
		return conn;
	}

	/** Metodo per l'inoltro delle query al database, richiede stringa della query**/
	public static ResultSet Query(String query) {

		Statement stmt = null;
		ResultSet rs = null;

		try {
			stmt = conn.createStatement();
			if (stmt.execute(query))
				rs = stmt.getResultSet(); // eseguo la query

		} catch (SQLException ex) {
			// gestisco gli errori con l'error handler: errori di entry doppia non vengono segnalati
			if (ex.getErrorCode() != 1062 && ex.getErrorCode() != 1040 && ex.getErrorCode() != 1064) {
				// Scrittura errore su file
				new WriteConsole(ex, "Error from rssfedd n°" + Capture.nrss
						+ " SQLException: " + ex.getMessage() + " SQLState: "
						+ ex.getSQLState() + " VendorError: "
						+ ex.getErrorCode());
			}
		}

		return rs;
	}

	/** Metodo per la chiusura della connessione al database **/
	public void Close() {
		try {
			conn.close();
		} catch (SQLException ex) {
			// Scrittura errore su file
			new WriteConsole(ex, "SQLException: " + ex.getMessage()
					+ " SQLState: " + ex.getSQLState() + " VendorError: "
					+ ex.getErrorCode());
		}
	}

	/** Metodo per il ritorno di count, contatore delle righe della tabella **/
	public static int returnCount() {
		int c = 0;
		ResultSet rs = Database.Query("SELECT COUNT(*) AS count FROM post");
		try {
			if (rs.next()) {
				c = rs.getInt("count");
			}
		} catch (SQLException e) {
			// Scrittura errore su file
			new WriteConsole(e, "Errore nel conteggio dei post");
		}
		return c;
	}

}
