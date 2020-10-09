package main;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.sql.ResultSet;
import java.sql.SQLException;

/** Classe che si occupa dell'estrazione dei vari dati: link, immagini, hashtag */
public class Extract {

	private String[] appoggio;
	private ResultSet rs;

	/** Costruttore vuoto **/
	public Extract() {}

	/**Metodo per l'estrazione degli hashtag a partire dal testo della descrizione **/
	public void findHashtag(String s, int id_post) {
		
		// Effettuo una pulizia dai caratteri speciali e dai caratteri che disturbano l'hashtag
		ReplaceChar r = new ReplaceChar();
		s = r.change(s);
		s = s.replaceAll("[,.;:!?)(»]", " ");
		s = s.replaceAll("\"", " ");
		
		// Riduco più spazi vuoti vicini in un unico spazio vuoto
		s = s.replaceAll(" +", " ");
		
		// Splitto la stringa dopo ogni spazio
		appoggio = s.split(" ");
		for (int i = 0; i < appoggio.length; i++) {
			
			//A causa della pulizia dei caratteri potrebbe esserci uno spazio vuoto fra # e la parola
			//quindi se trovo il carattere # lo concateco con il successivo termine
			if (appoggio[i].equals("#") && (i+1)!=appoggio.length) {
				appoggio[i] = appoggio[i] + appoggio[i + 1];
			}
			
			if (appoggio[i].contains("#")) {
				String[] ap = appoggio[i].split("#");
				if (ap.length == 2) {
					// Controllo la già presenza della parola per quel post
					rs = Database.Query("SELECT * FROM hashtag WHERE name='#"+ ap[1] + "' AND id_post='" + id_post + "'");
					try {
						if (!rs.next()) {
							// Inserisco se non presente
							Database.Query("INSERT INTO hashtag(name,id_post) VALUES('#"+ ap[1] + "','" + id_post + "')");
						}
					} catch (SQLException e) {
						// Scrivo errore su file
						new WriteConsole(e, "Error from rssfedd n°"+ Capture.nrss + " errore estrazione hashtag");
					}
				}
			}
		}
	}

	/** Metodo per l'estrazione degli url contenuti nella descrizione del post **/
	public void findUrl(String s, int id_post) {
		
		rs=null;
		// Splitto la riga dopo ogni spazio vuoto
		appoggio = s.split(" ");
		
		for (int i = 0; i < appoggio.length; i++) {
			// Per esigente di codice sql modifico ' con \'
			appoggio[i] = appoggio[i].replace("'", "\'");
			
			// Cerco tutti i possibili link
			if (appoggio[i].contains("http://")	|| appoggio[i].contains("https://")) {
				String expandedURL;
				try {
					//Concateno la testa dell'url con il resto nel caso in cui siano separate a causa 
					//della pulizia dai vari caratteri speciali
					if (appoggio[i].equals("http://") || appoggio[i].equals("https://")) {
						if (i + 1 != appoggio.length) {
							appoggio[i] = appoggio[i] + appoggio[i + 1];
						}
					}
					
					/* Espando lo shorter url */
					expandedURL = expandUrl(appoggio[i]);
					if (expandedURL == null) {
						expandedURL = appoggio[i];
					}else{
						appoggio[i]=expandedURL;
						expandedURL=expandUrl(appoggio[i]);
						if(expandedURL==null){
							expandedURL = appoggio[i];
						}
					}
					
					//I link che si riferiscono a profili o gallerie di facebook non hanno la testa dell'url
					if (!expandedURL.contains("http")) {
						expandedURL = "https://www.facebook.com" + expandedURL;
					}
					
					//Controllo che non vi siano doppioni per l'url di uno stesso post
					rs = Database.Query("SELECT * FROM link WHERE url='"+ expandedURL + "' AND id_post='" + id_post + "'");
					if (rs==null || !rs.next()) {
						// Inserisco il dato
						Database.Query("INSERT INTO link(url,type,id_post) VALUES('"+ expandedURL + "','link','" 
						+ id_post + "')");
					}
				} catch (SQLException e) {
					// Scrivo eventuali errori su file
					new WriteConsole(e, "Error from rssfedd n°" + Capture.nrss+ " errore estrazione link");
				}
			}

		}
	}

	/**Metodo per estrarre i link delle immagini, ma anche per estrarre
	 * ulterioni link. Prende in ingresso il testo della descrizione e il numero
	 * del post che si sta annalizzando */
	public void findImg(String s, int id_post) throws IOException {
		
		// Splitto la stringa dopo ogni backlash
		appoggio = s.split(" ");
		for (int i = 0; i < appoggio.length; i++) {
			// Per problemi con sql devo modificare il carattere speciale ' 
			appoggio[i] = appoggio[i].replace("'", "\'");
			
			// Se la stringa continiene src, allora ci stiamo riferendo al link di un'immagine
			if (appoggio[i].contains("src")) {
				
				// Elimino ogni elemento superfluo
				appoggio[i] = appoggio[i].replace("src=", "");
				appoggio[i] = appoggio[i].replace("\"", "");
				
				// Effettuo una query per controllare che non vi siano doppioni sullo stesso link e stesso post
				rs = Database.Query("SELECT * FROM link WHERE url='"+ appoggio[i] + "' AND id_post='" + id_post + "'");
				try {
					if (!rs.next()) {
						// I link in riferimento alle gallerie di facebook omettenno la parte iniziale del url. Per renderlo
						// apribile tramite un qualsiasi broswer inserisco manualmente la testa dell'url
						if (!appoggio[i].contains("http")) {
							appoggio[i] = "https://www.facebook.com"+ appoggio[i];
						}
						
						// Inserimento dati in DB
						Database.Query("INSERT INTO link(url,type,id_post) VALUES('"+ appoggio[i] + "','img','" 
						+ id_post + "')");
					}
				} catch (SQLException e) {
					// Scrittura errori su file
					new WriteConsole(e, "Error from rssfedd n°" + Capture.nrss+ " errore estrazione img");
				}
			} else if (appoggio[i].contains("href")) {
				// Se la stringa contiene href ci stiamo riferendo ad un vero e proprio link. 
				//Effettuo una pulizia dei caratteri inutili prima di procedere
				appoggio[i] = appoggio[i].replace("'", "\'");
				appoggio[i] = appoggio[i].replace("href=", "");
				appoggio[i] = appoggio[i].replace("\"", "");
				appoggio[i] = appoggio[i].replaceAll("(?=</a>).*", "");
				appoggio[i] = appoggio[i].replaceAll("[@#>].*", "");
				
				// Alcuni link hanno un redirect tramite facebook. L'espansione di questi link avviene in 
				// modo diversodagli shorter link
				if (appoggio[i].contains("facebook")) {
					RedirectFb rf = new RedirectFb();
					if (rf.findRedirect(appoggio[i])) {
						appoggio[i] = rf.url;
					}
				}
				
				if (appoggio[i].contains("http")) {
					String expandedURL;
					// A causa della pulizia dei vari tag html possono crearsi dei backslah fra http:// 
					//e il resto del link.Se la stringa contiene sono http o https allora la unisco con 
					// la succesiva per ovviare al problema
					if (appoggio[i].equals("http://") || appoggio[i].equals("https://")) {
						appoggio[i] = appoggio[i] + appoggio[i + 1];
					}
					
					// Doppio controllo su shorter link: ho notato che alcuni shorter link rimandavano ad 
					// altri shorter link per ovviare al problema, controllo il link due volte. 
					// In questo modo per ogni post ho l'effettivo link di rimando
					expandedURL = expandUrl(appoggio[i]);
					if (expandedURL != null) {
						appoggio[i] = expandedURL;
					}
					expandedURL = expandUrl(appoggio[i]);
					if (expandedURL != null) {
						appoggio[i] = expandedURL;
					}
				}
				
				// Controllo di non inserire doppioni
				rs = Database.Query("SELECT * FROM link WHERE url='"+ appoggio[i] + "' AND id_post='" + id_post + "'");
				try {
					if (!rs.next()) {
						// Alcuni link fanno riferimento o a gallerie di facebook o a profili di facebook, ma non hanno la
						// testa del url. Inserisco manualmente la testa dell'url e controllo se si riferiscono a profili
						// (quindi sono link) o a gallerie (quindi sono img)
						if (!appoggio[i].contains("http")) {
							appoggio[i] = "https://www.facebook.com"+ appoggio[i];
							if (appoggio[i].contains("profile")) {
								// Inserimento dati in DB come link
								Database.Query("INSERT INTO link(url,type,id_post) VALUES('"+ appoggio[i]
										+ "','link','"+ id_post	+ "')");
							} else {
								// Inserimento dati in DB come img
								Database.Query("INSERT INTO link(url,type,id_post) VALUES('"+ appoggio[i]
										+ "','img','"+ id_post+ "')");
							}
						} else {
							// Inserimento dati in DB come link
							Database.Query("INSERT INTO link(url,type,id_post) VALUES('"+ appoggio[i]+ "','link','"
									+ id_post+ "')");
						}
					}
				} catch (SQLException e) {
					// Scrivo eventuali errori su file
					new WriteConsole(e, "Error from rssfedd n°" + Capture.nrss
							+ " errore estrazione link");
				}
			}
		}
	}

	/** Metodo per l'espesione del Short Link. Prende in ingresso il link e restituisce il campo html "location"**/
	public static String expandUrl(String shortenedUrl) {
		String expandedURL = null;
		URL url;
		if (shortenedUrl == null || shortenedUrl.isEmpty()) {
			return expandedURL;
		}
		
		// A causa della pulizia da parte dei caratteri speciali potrebbe esser presente dello spazio bianco
		// o altri caratteci che compromettono l'url. Con questo metodo si elimino tali caratteri
		int startht = shortenedUrl.indexOf("http");
		if (startht != 0 && startht>-1) {
			String replace = shortenedUrl.substring(0, startht);
			shortenedUrl = shortenedUrl.replace(replace, "");
		}

		try {
			url = new URL(shortenedUrl);
			// Apertura connessione reiderect
			HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection(Proxy.NO_PROXY);

			// Arresto del redirect
			httpURLConnection.setInstanceFollowRedirects(false);

			// Estrazione del campo location nel header contenente la destinazione attuale
			expandedURL = httpURLConnection.getHeaderField("Location");
			httpURLConnection.disconnect();

		} catch (IOException e) {
			// Ritorno null se ci sono stati problemi
			return null;
		}
		return expandedURL;
	}
}
