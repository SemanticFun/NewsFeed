package main;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/** Classe per la gestione del post **/
public class PostNews {

	private String title, descr, text, pubDate, link, category;

	/** Costruttore vuoto **/
	public PostNews() {}

	/** Costruttore **/
	public PostNews(String title, String descr, String text, String pubDate,
			String link) {
		this.title = title;
		this.descr = descr;
		this.text = text;
		this.pubDate = pubDate;
		this.link = link;
	}

	/*----Inizio Getters and Setters----*/
	public String getCat() {
		return category;
	}

	public void setCat(String category) {
		this.category = category;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getDescr() {
		return descr;
	}

	public void setDescr(String descr) {
		this.descr = descr;
	}

	public String getPubDate() {
		return pubDate;
	}

	public void setPubDate(String pubDate) {
		this.pubDate = pubDate;
	}

	public String getLink() {
		return link;
	}

	public void setLink(String link) {
		this.link = link;
	}

	/*----Fine Getters and Setters----*/

	/** Metodo che si occupa di ritrovare l'ultima data inserita all'interno del DB in riferimento
	 * ad un id rss */
	public Date lastDate(int id_rss) {
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		ResultSet rs = null;
		Date d = new Date(0);

		try {
			rs = Database.Query("SELECT MAX(pubDate) AS ultimadata FROM post WHERE id_rss='"+ id_rss + "'");
			if (rs.next()) {
				if(rs.getString("ultimadata")==null || rs.getString("ultimadata").isEmpty()){
					return d;
				}else{
					d = formatter.parse(rs.getString("ultimadata"));
				}
			}
		} catch (SQLException | ParseException e) {
			new WriteConsole(e, "SQLException: " + e.getMessage());

		}
		return d;
	}

}