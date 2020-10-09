package main;

import java.io.IOException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/** Classe che si occupa di leggere l'url attraverso un redirect effettuato con Facebook**/
public class RedirectFb {
	
	 String url;

	/** Costruttore vuoto**/
	public RedirectFb() {}

	/** Metodo che notifica se c'è un redirect**/
	public boolean findRedirect(String s) {
		Document doc;
		try {

			// Http protocol usato attraverso Mozilla
			doc = Jsoup.connect(s).userAgent("Mozilla").get();

			// Ricerca dei link
			Elements links = doc.select("a[href]");
			for (Element link : links) {

				if (link.text().contains("Segui link")) {
					url = link.attr("href");
					return true;
				}
			}

		} catch (IOException e) {
			new WriteConsole(e, "Error from rssfedd n°" + Capture.nrss
					+ " errore su redirect Facebook");
		}
		return false;
	}

	/** Metodo che ritorno il redirect**/
	public String returnRedirect(String s) {
		Document doc;
		try {

			doc = Jsoup.connect(s).userAgent("Mozilla").get();


			Elements links = doc.select("a[href]");
			for (Element link : links) {

				if (link.text().contains("Segui link")) {
					return link.attr("href");
				}
			}

		} catch (IOException e) {
			new WriteConsole(e, "Error from rssfedd n°" + Capture.nrss
					+ " errore su redirect Facebook");
		}
		return null;
	}

}
