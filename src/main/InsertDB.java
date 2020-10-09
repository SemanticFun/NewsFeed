package main;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;

public class InsertDB {

	/** Metodo per la scrittura in database dei dati di ogni post **/
	@SuppressWarnings("deprecation")
	public static void insertPostDb(String channel, int id_rss, ArrayList<PostNews> postList) {
		
		boolean insert = false;// Variabile che tiene traccia dell'inserimento della notizia
		SimpleDateFormat formatter;
		
		if (channel.equals("facebook") || channel.equals("twitter")) {
			formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
		} else {
			formatter = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss",Locale.US);
		}

		Iterator<PostNews> it = postList.iterator();
		
		// Fintanto che ho dati nella lista, li scrivo sul Db
		while (it.hasNext()) {
			ResultSet rs;
			int lastid = 0; // Tengo traccia dell'ultimo id inserito
			rs = Database.Query("SELECT MAX(id_post) AS max FROM post");
			try {
				if (rs.next()) {
					lastid = rs.getInt("max");
				}
			} catch (SQLException e1) {
				// Scrivo errore su file
				new WriteConsole(e1, "Errore rssfeed " + Capture.nrss);
			}
			
			// Oggetto post
			PostNews p = new PostNews();
			p = it.next();
			if (p.getCat() == null) {
				p.setCat("");
			}
			
			try {
				//Tengo traccia dell'ultima data presente in database per l'rss che stiamo analizzando
				//in modo da inserire solo post pubblicati dopo l'ultima cattura
				Date d = p.lastDate(id_rss), datep;
				String tempdescr = "", temptitle="";
				try {
					
					// Controllo il formato delle date
					if (!channel.equals("facebook")	&& !channel.equals("twitter")) {						
						formatter = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);

						//alcune date possono essere in italiano, questo controllo le porta in inglese
						String data_sub3 = p.getPubDate().substring(0, 3);
						if (data_sub3.contains("Lun") || data_sub3.contains("Mar") || data_sub3.contains("Mer") || data_sub3.contains("Gio") || data_sub3.contains("Ven") || data_sub3.contains("Sab") || data_sub3.contains("Dom")){
							p.setPubDate(p.getPubDate().replace("Lun", "Mon"));
							//occorre fare particolare attenzione a Mar perchè può stare per Martedì, Marzo e March
							if (data_sub3.equals("Mar"))
								p.setPubDate("Tue" + p.getPubDate().substring(3));
							p.setPubDate(p.getPubDate().replace("Mer", "Wed"));
							p.setPubDate(p.getPubDate().replace("Gio", "Thu"));
							p.setPubDate(p.getPubDate().replace("Ven", "Fri"));
							p.setPubDate(p.getPubDate().replace("Sab", "Sat"));
							p.setPubDate(p.getPubDate().replace("Dom", "Sun"));
							
							p.setPubDate(p.getPubDate().replace("Gen", "Jan"));
							p.setPubDate(p.getPubDate().replace("Mag", "May"));
							p.setPubDate(p.getPubDate().replace("Giu", "Jun"));
							p.setPubDate(p.getPubDate().replace("Lug", "Jul"));
							p.setPubDate(p.getPubDate().replace("Ago", "Aug"));
							p.setPubDate(p.getPubDate().replace("Set", "Sep"));
							p.setPubDate(p.getPubDate().replace("Ott", "Oct"));
							p.setPubDate(p.getPubDate().replace("Dic", "Dec"));
						}

						if (p.getPubDate().matches("([A-z]{3}) ([A-z]{3}) ([0-9]{2}) [0-9]{2}:[0-9]{2}:[0-9]{2} ([A-z]{3}) ([0-9]{4})")){
							formatter = new SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy",Locale.US);
						}else if (p.getPubDate().matches("([0-9]{2}) ([A-z]{3}) ([0-9]{4}) ([0-9]{2}):([0-9]{2}):([0-9]{2}) ([+,0-9]{5})")){
							formatter = new SimpleDateFormat("dd MMM yyyy HH:mm:ss z",Locale.US);
						} else {
							if(!p.getPubDate().contains(",")){
								formatter=new SimpleDateFormat("EEE dd MMM yyyy HH:mm:ss z",Locale.US);
							}
						}
						datep = formatter.parse(p.getPubDate());

					} else {
						// Le date di facebook sono fornite con due ore in meno. Modifico tali date aggiungendo le due ore
						formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",	Locale.US);
						datep = formatter.parse(p.getPubDate());
						if (channel.equals("facebook")) {
							datep.setHours(datep.getHours() + 2);
						}
					}
					
					
					// Comparo la data dell'ultimo post inserito di un determinato rssfeed. Se la data è più recente allora
					// registro il post altrimenti lo scarto
					if (d == null || datep.compareTo(d) > 0) {
						
						//Normalizzo il formato delle date
						formatter.applyPattern("yyyy-MM-dd HH:mm:ss");
						
						String dats = formatter.format(datep), title = "", text_descr ="", descr;
						
						if (p.getTitle() == null || p.getTitle().isEmpty() || p.getTitle().equals("null")) {
							title = "";
						} else {
							temptitle = p.getTitle();
							String[] split=temptitle.split(" ");
							for(int i=0;i<split.length;i++){
								if(split[i].contains("http") || split[i].contains(".it") || split[i].contains("tinyurl") || split[i].contains("fb.me")
										|| split[i].contains("pic.twitter") || split[i].contains("bit.ly") || split[i].contains("goo.gl") || split[i].contains("ilfat.to")
										|| split[i].contains(".com")){
									continue;
								}else{
									title=title+" "+split[i];
								}
							}
						}
						
						if (p.getDescr() == null || p.getDescr().isEmpty()	|| p.getDescr().equals("null")) {
							descr = "";
							text_descr = "";
						} else {
							descr = p.getDescr();
							tempdescr = p.getText();
							String[] split=tempdescr.split(" ");
							for(int i=0;i<split.length;i++){
								if(split[i].contains("http") || split[i].contains(".it") || split[i].contains("tinyurl") || split[i].contains("fb.me")
										|| split[i].contains("pic.twitter") || split[i].contains("bit.ly") || split[i].contains("goo.gl") || split[i].contains("ilfat.to")
										|| split[i].contains(".com")){
									continue;
								}else{
									text_descr=text_descr+" "+split[i];
								}
							}
						}
						
						// Controllo che non vi siano doppioni
						rs = Database.Query("SELECT * FROM post WHERE (title='"	+ title + "' AND text_descr='" + text_descr
								+ " ' AND link='" + p.getLink()	+ "' AND id_rss='" + id_rss + "')");
						if (rs == null) {
							continue;
						}
						if (!rs.next()) {
							// Inserimento dati su database
							Database.Query("INSERT INTO post(title,descr,text_descr,category,link,pubDate,id_rss) VALUES ('"
									+ title	+ "','"	+ descr	+ "','"	+ text_descr+ "','"+ p.getCat()	+ "','"+ p.getLink()
									+ "','"	+ dats+ "','"+ id_rss + "')");
							insert = true;
						}
					}
					// Resetto il formato della data
					formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",Locale.US);
				} catch (ParseException e) {
					// Scrivo errore su file
					new WriteConsole(e, "Errore rssfeed " + Capture.nrss);
				}
				
				// Se la notizia è inserita allora estraggo i link e gli hashtag, nonchè il testo della descrizione
				if (insert == true) {
					rs = Database.Query("SELECT MAX(id_post) as max FROM post");
					Extract e = new Extract();
					if (rs.next()) {
						int max = rs.getInt("max");
						if (max > lastid) {
							e.findHashtag(tempdescr + " " + temptitle, max);
							e.findUrl(tempdescr + " " + temptitle, max);
							e.findImg(p.getDescr() + " " + temptitle, max);
							lastid = max;
						}
					}
				}
			} catch (SQLException | IOException e) {
				// Scrivo eventuali errori su file
				new WriteConsole(e, "Errore rssfeed " + Capture.nrss);
			}
			
			// Resetto variabile
			insert = false;
		}
	}

}
