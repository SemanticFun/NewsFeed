package main;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;

import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.URLEntity;
import twitter4j.conf.ConfigurationBuilder;

/** Classe che si occupa dell'accesso alla API di twitter e della lettura del timeline
 * di ogni autore
 */
public class ParseTwitter {

	// Configuratore l'accesso alla api di twitter
	private ConfigurationBuilder cb = new ConfigurationBuilder();
	
	private PostNews p;
	private ArrayList<PostNews> postList = new ArrayList<PostNews>();
	private ReplaceChar replace = new ReplaceChar();

	/** Costruttore**/
	public ParseTwitter(String APIKey, String APISecret, String accessToken, String accessSecret) {
		// Impostazione dei dati di accesso
		cb.setDebugEnabled(true);
		cb.setOAuthConsumerKey(APIKey);
		cb.setOAuthConsumerSecret(APISecret);
		cb.setOAuthAccessToken(accessToken);
		cb.setOAuthAccessTokenSecret(accessSecret);
	}

	/** Classe che si occupa del parsing di Twitter**/
	public void parse(RssFeed r) {
		TwitterFactory twitterFactory = new TwitterFactory(cb.build());
		Twitter twitter = twitterFactory.getInstance();
		List<Status> listr;
		
		try {
			// Lettura dei post
			listr = twitter.getUserTimeline(r.getLink());
			Iterator<Status> it = listr.iterator();

			Status stato;
			String temp;
			SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
			while (it.hasNext()) {
				stato = it.next();
				p = new PostNews();

				p.setTitle("");

				temp = replace.change(stato.getText());
				p.setDescr(temp);
				temp = temp.replaceAll("<(\"[^\"]*\"|'[^']*'|[^'\">])*>", " ");
				temp = temp.replaceAll(" +", " ");
				p.setText(temp);

				// Lettura dell'url associato al post
				URLEntity[] url = stato.getURLEntities();
				for (int i = 0; i < url.length; i++) {
					temp = Extract.expandUrl(url[i].getExpandedURL());
					if (temp == null) {
						p.setLink(url[i].getExpandedURL());
					} else {
						p.setLink(temp);
					}
				}

				temp = formatter.format(stato.getCreatedAt());
				p.setPubDate(temp);

				postList.add(p);
			}
		} catch (TwitterException e) {
			// TODO Auto-generated catch block
			new WriteConsole(e,"Errore nella classe ParseTwitter all'rssfeed n° "+ r.getId());
		}
	}

	/** Metodo che "rovescia" l'Array. In questo modo i post più vecchi saranno i primi ad esser
	 * analizzati e non verranno persi dati.**/
	public ArrayList<PostNews> returnList() {
		Collections.reverse(postList);
		return postList;
	}

}
