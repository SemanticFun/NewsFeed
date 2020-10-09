package main;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** Classe che si occupa della lettura del graph Facebook e del parsing dei post**/
public class ParseFacebook {

	private PostNews p;
	private ArrayList<PostNews> postList = new ArrayList<PostNews>();
	private ReplaceChar replace = new ReplaceChar();
	private String accessToken;

	/** Costuttore vuoto **/
	public ParseFacebook(String accessToken) {
		this.accessToken=accessToken;
	};

	public void parse(RssFeed r) {

		// Stringa contenente l'url al graph di facebook più al'access token
		String s = "https://graph.facebook.com/v2.4/"+ r.getLink()
				+ "?fields=posts%7Bmessage%2Cdescription%2Clink%2Ccreated_time%7D&access_token="+accessToken;

		InputStream in;
		try {
			
			// Lettura dei dati forniti dal graph
			in = new URL(s).openStream();
			BufferedReader streamReader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
			StringBuilder responseStrBuilder = new StringBuilder();

			String inputStr;
			while ((inputStr = streamReader.readLine()) != null)
				responseStrBuilder.append(inputStr);

			// Parsing del Json
			JSONObject jobj = new JSONObject(responseStrBuilder.toString());
			JSONObject posts = new JSONObject(jobj.get("posts").toString());
			JSONArray data = (JSONArray) posts.getJSONArray("data");

			// Lettura dei dati del post
			int i = 0;
			String temp;
			while (i < data.length()) {
				JSONObject post = new JSONObject(data.get(i).toString());
				p = new PostNews();
				
				if (post.toString().contains("message")) {
					p.setTitle(replace.change(post.get("message").toString()));
				}
				
				if (post.toString().contains("description")) {
					temp = post.get("description").toString();
					temp = replace.change(temp);
					p.setDescr(temp);
					temp = temp.replaceAll("<(\"[^\"]*\"|'[^']*'|[^'\">])*>",
							" ");
					temp = temp.replaceAll(" +", " ");
					p.setText(temp);
				}
				
				if (post.toString().contains("link")) {
					temp = post.get("link").toString();
					String expand = Extract.expandUrl(temp);
					if (expand == null) {
						p.setLink(temp);
					} else {
						temp=expand;
						expand = Extract.expandUrl(temp);
						if(expand == null){
							p.setLink(temp);
						}else{
							p.setLink(expand);
						}
					}
				}
				p.setPubDate(post.get("created_time").toString());
				p.setPubDate(p.getPubDate().replace("T", " "));
				p.setPubDate(p.getPubDate().replace("+0000", ""));

				i++;
				postList.add(p);
			}
		} catch (IOException | JSONException e) {
			new WriteConsole(e, "Errore classe parseFacebook");
		}

	}

	/** Metodo che "rovescia" l'Array. In questo modo i post più vecchi saranno i primi ad esser
	 * analizzati e non verranno persi dati.**/
	public ArrayList<PostNews> returnList() {
		Collections.reverse(postList);
		return postList;
	}
}
