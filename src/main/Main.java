package main;

public class Main {
	public static void main(String[] args){
		new WriteConsole("\nControllo del file config. Attendere. ");
		Database db = new Database();
		InsertRssfeed insertRss = new InsertRssfeed();
		// Leggo e/o aggiorno gli indirizzi rssfeed
		try {
			insertRss.read();
		} catch (Exception e) {
			new WriteConsole(e, "Errore classe Repeat");
		}
		new Capture().capturePost();
		db.Close();
	}
}
