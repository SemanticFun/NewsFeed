package main;

import java.io.FileReader;
import java.sql.ResultSet;

import org.json.JSONArray;
import org.json.JSONObject;

/** Classe che si occupa di leggere il file con l'elenco degli indirizzi ed
 * inserirli nel DB. Ritorna una variabile booleana che notifica la fine dell'inserimento dei dati in DB.**/
public class InsertRssfeed {

	/** Costruttore vuoto **/
	public InsertRssfeed() {	}

	/** Metodo per la lettura del file json e l'esportazione del link rss in Db **/
	public void read() throws Exception {

		String filePath = "/home/tomcat/somer/config.json";
		//String filePath = "./config.json";
		String author, link_rss, category, channel;
		int id_author = 0;

		try {
			ResultSet rs;
			int count = 0, countold = 0;/*Variabili che tengono in memoria il n° di link inseriti*/
			rs = Database.Query("SELECT COUNT(*) AS count FROM rssfeed");
			if (rs.next()) {
				countold = rs.getInt("count");
			}

			// Lettura del file
			@SuppressWarnings("resource")
			FileReader reader = new FileReader(filePath);
			String fileContents = "";

			int j;

			while ((j = reader.read()) != -1) {
				char ch = (char) j;

				fileContents = fileContents + ch;
			}

			// Parsing del json
			JSONObject jsonObject = new JSONObject(fileContents);
			JSONObject feed = new JSONObject(jsonObject.get("feed").toString());
			JSONArray data = (JSONArray) feed.getJSONArray("data");

			// Lettura dei dati
			for (int i = 0; i < data.length(); i++) {
				JSONObject post = new JSONObject(data.get(i).toString());
				if (post.toString().contains("author")
						&& post.toString().contains("link_rss")
						&& post.toString().contains("channel")
						&& post.toString().contains("category")
						&& post.toString().contains("type")) {

					author = post.getString("author").toLowerCase();
					if (author.isEmpty()) {
						new WriteConsole("\nAttenzione il feed n° " + (i + 1)+ " ha il campo author vuoto.");
					}

					link_rss = post.getString("link_rss");
					if (link_rss.isEmpty()) {
						new WriteConsole("\nAttenzione il feed n° " + (i + 1)+ " dell'autore " + author
								+ " ha il campo link_rss vuoto.");
					}

					channel = post.getString("channel").toLowerCase();
					if (channel.isEmpty()) {
						new WriteConsole("\nAttenzione il feed n° " + (i + 1)+ " dell'autore " + author
								+ " ha il campo channel vuoto.");
					}

					category = post.getString("category".toLowerCase());
					if (category.isEmpty()) {
						new WriteConsole("\nAttenzione il feed n° " + (i + 1)+ " dell'autore " + author
								+ " ha il campo category vuoto.");
					}

					if (post.getString("type").isEmpty()) {
						new WriteConsole("\nAttenzione il feed n° " + (i + 1)+ " dell'autore " + author
								+ " ha il campo type vuoto.");
					}

					// In base all'azione richiesta si effettua un diverso tipo di operazione
					
					// Inserimento
					if (post.getString("type").equals("insert")) {
						// Controllo che il nome dell'autore non sia giù presente in DB
						rs = Database.Query("SELECT * FROM author WHERE name='"	+ author + "'");
						
						if (!rs.next()) {
							// Inserisco dato in database
							Database.Query("INSERT INTO author(name) VALUES('"+ author + "')");
							rs = Database.Query("SELECT * FROM author WHERE name='"	+ author + "'");
							rs.next();
						}
						// Registro id autore
						id_author = rs.getInt("id_author");
						
						//Controllo che non vi sia già un rss con i dati inseriti
						rs = Database.Query("SELECT * FROM rssfeed WHERE link_rss='"+ link_rss + "' AND id_author='"
										+ id_author + "'");
						
						if (!rs.next()) {
							// Inserisco dato in database
							Database.Query("INSERT INTO rssfeed "+ "(id_author,category,channel,link_rss) VALUES('"
									+ id_author + "','" + category + "','"+ channel + "','" + link_rss + "'" + ")");
						}
						
					// Update	
					} else if (post.getString("type").equals("update")) {
						rs = Database.Query("SELECT * FROM author WHERE name='"	+ author + "'");
						if (rs.next()) {
							ResultSet rsu;
							id_author = rs.getInt("id_author");
							rsu = Database.Query("SELECT id_rss FROM rssfeed WHERE id_author='"	+ id_author
											+ "' AND channel='"	+ channel+ "' AND category='"+ category + "'");
							
							//Aggiorno i dati in database con i nuovi dati inseriti
							if (rsu.next()) {
								Database.Query("UPDATE rssfeed SET link_rss='"+ link_rss + "' WHERE id_rss='"
							+ rsu.getInt("id_rss") + "'");
							}
						} else {
							// Scrivo su file eventuali errori
							new WriteConsole("\nAttenzione: il feed n°"	+ (i + 1)+ " dell'autore "+ author
											+ " del canale "+ channel+ " non è stato "
											+ "aggiornato perchè non presente in database.");
						}
					
					// Delete
					} else if (post.getString("type").equals("delete")) {
						rs = Database.Query("SELECT id_author FROM author WHERE name='"+ author + "'");
						if (rs.next()) {
							Database.Query("DELETE FROM rssfeed WHERE id_author='"+ rs.getInt("id_author")
									+ " AND link_rss='" + link_rss + "'");
						} else {
							// Scrivo su file eventuali errori
							new WriteConsole("\nAttenzione: il feed n°"+ (i + 1)+ " dell'autore "
											+ author+ " del canale "+ channel+ " non è stato "
											+ "cancellato perchè non presente in database.");
						}
					}

				} else {
					// Scrivo su file eventuali errori
					new WriteConsole("\nAttenzione il feed n° " + (i + 1)+ " non contiene tutti i dati necessari.");
				}

			}

			rs = Database.Query("SELECT COUNT(*) AS count FROM rssfeed");
			if (rs.next()) {
				count = rs.getInt("count");
			}

			// Se non è stato aggiunto nessun nuovo link stampo un messaggio diverso
			if (count < countold || count == countold) {
				new WriteConsole("Nessun nuovo link inserito.\n");
			} else {
				new WriteConsole("Inseriti " + (count - countold)+ " nuovi link.");
			}

		} catch (Exception e) {
			new WriteConsole(e, "Errore classe InsertRssfeed");
		}
	}

}
