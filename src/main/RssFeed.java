package main;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Vector;

/** Classe per la gestione dei link rssfees */
public class RssFeed {

	private int id, id_author;
	private String channel, category, link;

	/** Costruttore vuoto **/
	public RssFeed() {}

	/** Costruttore **/
	public RssFeed(int id, int id_author, String category, String channel,
			String link) {
		this.id = id;
		this.id_author = id_author;
		this.channel = channel;
		this.category = category;
		this.link = link;
	}

	/*---Inzio Getters and Setters---*/

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public int getIdAuthor() {
		return id_author;
	}

	public void setIdAuthor(int id_author) {
		this.id_author = id_author;
	}

	public String getChannel() {
		return channel;
	}

	public void setChannel(String channel) {
		this.channel = channel;
	}

	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}

	public String getLink() {
		return link;
	}

	public void setLink(String link) {
		this.link = link;
	}

	/*---Fine Getters and Setters---*/

	/** Metodo per la creazione della lista rss presa dal database */
	public Vector<RssFeed> getList() {
		Vector<RssFeed> list = new Vector<RssFeed>();
		RssFeed r;
		
		ResultSet rs = Database.Query("SELECT * FROM rssfeed");
		
		//modificato per fare un test solo sui siti web
		//ResultSet rs = Database.Query("SELECT * FROM rssfeed where channel = 'facebook' and id_rss > 90 and id_rss < 100");
		
		
		
		try {
			while (rs.next()) {
				r = new RssFeed(rs.getInt("id_rss"), rs.getInt("id_author"),rs.getString("category"), 
						rs.getString("channel"),rs.getString("link_rss"));
				list.add(r);
			}
		} catch (SQLException ex) {
			new WriteConsole(ex, "SQLException: " + ex.getMessage()+ " SQLState: " + ex.getSQLState() + " VendorError: "
					+ ex.getErrorCode());
		}
		return list;
	}
}
