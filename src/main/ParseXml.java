package main;

import java.util.ArrayList;
import java.util.Collections;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**Classe per la lettura dei file Rss/Xml **/
public class ParseXml extends DefaultHandler {

	private StringBuffer buffer = new StringBuffer();

	private PostNews postNews;
	private String temp = ""; //Variabile di appoggio per la registrazione del contenuto dei tag
	public ArrayList<PostNews> postList = new ArrayList<PostNews>();
	private boolean item = false; // Variabile che notifica quando un item è presente
	private ReplaceChar r = new ReplaceChar();// Oggetto per la sostituzione dei caratteri speciali

	/** Metodo che legge i tag di apertura '<'**/
	public void startElement(String uri, String localName, String qName,Attributes attributes) throws SAXException {
		// Azzero il buffer e annullo il temp
		buffer.setLength(0);
		temp = "";

		if (qName.equalsIgnoreCase("item")) {
			// Se il tag inizia con item allora creo un nuovo oggetto Post e passo alla variabile "item" true
			// per inidicare il nuovo inizio di un post
			postNews = new PostNews();
			item = true;
		}

	}

	/** Metodo per il reperimento delle informazioni infra tag */
	public void characters(char ch[], int start, int length) throws SAXException {
		// Registro la stringa di informazioni
		buffer.append(new String(ch, start, length));
	}

	/** Metodo che legge i tag di chiusura '<'**/
	public void endElement(String uri, String localName, String qName) throws SAXException {
		// Se il tag di chiusura è un item allora notifico la fine del post e lo aggiungo nell'ArrayList
		if (qName.equals("item")) {
			postList.add(postNews);
			item = false;
		} else {
			// Se item è ancora aperto allora registro il dato
			if (item) {
				temp = buffer.toString();
				// Tolgo il carattere ' perchè se presente può dar fastidio all'inserimento dei dati nella query Sql
				temp = temp.replace("'", " ");

				if (qName.equals("title")) {
					temp = r.change(temp);
					postNews.setTitle(temp);
				} else if (qName.equals("description")) {
					temp = r.change(temp);
					postNews.setDescr(temp);
					temp = temp.replaceAll("<(\"[^\"]*\"|'[^']*'|[^'\">])*>"," ");
					// Se vi sono più di uno spazio vuoto li sostituisco con un unico backlash
					temp = temp.replaceAll(" +", " ");
					postNews.setText(temp);
				} else if (qName.equals("link")) {
					postNews.setLink(temp);
				} else if (qName.equals("pubDate")) {
					postNews.setPubDate(temp);
				} else if (qName.equals("category")) {
					temp = r.change(temp);
					postNews.setCat(temp);
				}
			}
		}

	}

	/** Metodo che "rovescia" l'Array. In questo modo i post più vecchi saranno i primi ad esser
	 * analizzati e non verranno persi dati.**/
	public ArrayList<PostNews> returnList() {
		Collections.reverse(postList);
		return postList;
	}

}
