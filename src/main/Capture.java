package main;

import java.io.*;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;

import javax.xml.parsers.*;

import org.json.JSONException;
import org.json.JSONObject;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.DefaultHandler;

/** Classe per la "cattura" delle notizie **/
public class Capture {

	private SimpleDateFormat sdf = new SimpleDateFormat();
	
	// Chiavi di accesso per i social network
	private String accessTokenFB, APIKeyTW, APISecretTW, accessTokenTW, accessTokenTWS; 
	
	// Tengo conto del conteggio delle news inserite fino ad ora
	public static int count = Database.returnCount();
	
	// Tengo contro degli errori che si verificano
	public static int nrss, errCount = 0; 
	
	/** Costruttore vuoto **/
	public Capture() {
		// Recupero di dati di accesso dei social network attraverso l'apposito file Json
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
			JSONObject feed = new JSONObject(jsonObject.get("social").toString());

			accessTokenFB = feed.get("accessTokenFB").toString();
			APIKeyTW = feed.get("APIKeyTW").toString();
			APISecretTW = feed.get("APISecretTW").toString();
			accessTokenTW = feed.getString("accessTokenTW").toString();
			accessTokenTWS = feed.getString("accessTokenTWS").toString();

		} catch (IOException | JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**Metodo avvia il processo di cattura. Stampa a video quando ha inziato a
	 * catturare e quando ha finito con un conteggio delle notizie inserite **/
	public void capturePost() {
		// Formato della data e dell'ora
		sdf.applyPattern("dd/MM/yy HH:mm");

		// Data odierna
		String dataStr = sdf.format(new Date());

		RssFeed r = new RssFeed();

		// Recupero la lista dei link rss
		Vector<RssFeed> list = new Vector<RssFeed>();
		list = r.getList();
		Iterator<RssFeed> it = list.iterator();

		// Messaggio di attesa
		new WriteConsole("Inizio analisi: " + dataStr + " ");

		// Fintanto che ho link rss eseguo il parsing
		while (it.hasNext()) {
			r = it.next();

			// Registo l'id dell'rssfeed che sto controllando
			nrss = r.getId();

			// In base al canale del link effettuo un parsing diverso e lancio la classe
			if (r.getChannel().equals("facebook")) {
				if (!r.getLink().isEmpty()) {
					ParseFacebook parsef = new ParseFacebook(accessTokenFB);
					parsef.parse(r);
					// Inserisco post trovati in database
					InsertDB.insertPostDb(r.getChannel(), r.getId(),parsef.returnList());
				}
			} else if (r.getChannel().equals("twitter")) {
				if (!r.getLink().isEmpty()) {
					ParseTwitter parset = new ParseTwitter(APIKeyTW,APISecretTW,accessTokenTW,accessTokenTWS);
					parset.parse(r);
					// Inserisco post trovati in database
					InsertDB.insertPostDb(r.getChannel(), r.getId(),parset.returnList());
				}
			} else {
				try {
					
					// Istanza per la cattura dei dati rss/xml
					SAXParser parser = SAXParserFactory.newInstance().newSAXParser();

					DefaultHandler handler = new ParseXml();

					//Apertura del link rss
					URL URL = new URL(r.getLink());
					InputStream ism = URL.openStream();

					//Problemi di codifica caratteri con rssfeed 284 
					if (r.getId() == 284) {
						// Lettura delle notizie
						parser.parse(ism, handler);
					} else {
						// Codifica dei caratteri
						Reader reader = new InputStreamReader(ism, "UTF-8");
						InputSource is = new InputSource(reader);
						is.setEncoding("UTF-8");

						//Lettura delle notizie
						parser.parse(is, handler);
					}

					// Scrittura post in Database
					InsertDB.insertPostDb(r.getChannel(), r.getId(),((ParseXml) handler).returnList());

				} catch (Exception e) {
					
					// Scrittura di eventuali errori in un file di testo
					new WriteConsole(e, "Errore rssfeed n° " + nrss);
				}
			}
		}

		// Stampo messaggio su file di completamento operazione
		dataStr = sdf.format(new Date());
		new WriteConsole("- Fine: " + dataStr + ". ");

		// Calcolo dei post inseriti e scrittura messaggio
		int diff = (Database.returnCount()) - count;
		if (diff == 0) {
			new WriteConsole("Nessuna nuova notizia inserita. ");
		} else {
			new WriteConsole("Inserite '" + diff + "' nuove notizie. ");
			count = Database.returnCount();
		}
		
		new WriteConsole(errCount + " errori rilevati");
		errCount = 0;
	}
}
