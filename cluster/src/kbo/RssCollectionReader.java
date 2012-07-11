package kbo;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Random;

import dragon.onlinedb.Article;
import dragon.onlinedb.ArticleParser;
import dragon.onlinedb.BasicArticle;
import dragon.onlinedb.CollectionReader;
/** Collection Reader class reads rss articles
 * stored in database
 * 
 * @author KBO
 *
 */
public class RssCollectionReader implements CollectionReader {
	Connection conn;
	Statement stat;
	Integer index;
	protected short[] shuffle;
	protected int shuffleCount;
	public RssCollectionReader(){
		try {
			Class.forName("org.sqlite.JDBC");
			conn = DriverManager.getConnection("jdbc:sqlite:test.db");
			stat = conn.createStatement();
			ResultSet rs = stat.executeQuery("SELECT MAX(_id) as last FROM feeds;");
			while (rs.next()) {
				index = rs.getInt("last");
			}
			rs.close();
			shuffle = new short[index+1];
			Random rgen = new Random();
			for (short i =0;i<shuffle.length;i++){
				shuffle[i] = i;
			}
			for (int i=0; i<shuffle.length; i++) {
				int randomPosition = rgen.nextInt(shuffle.length);
				short temp = shuffle[i];
				shuffle[i] = shuffle[randomPosition];
				shuffle[randomPosition] = temp;
			}
			shuffleCount =0;
		}
		catch (SQLException e) {
			e.printStackTrace();} 
		catch (ClassNotFoundException e) {
			e.printStackTrace();
		}

	}
	public Article getNextArticle(){
		Article cur = new BasicArticle();

		//cur.setKey(0);
		//cur.setTitle
		//cur.setBody
		//SELECT id, title,content FROM feeds
		return cur;
	}
	@Override
	public Article getArticleByKey(String key) {
		Article cur = new BasicArticle();
		try {
			ResultSet rs = stat.executeQuery("SELECT _id, title,content FROM feeds WHERE _id = " + key);
			while (rs.next()) {
				String rssTitle = rs.getString("title");
				String rssContent = rs.getString("content");
				Integer rssIndex = rs.getInt("_id");
				cur.setKey(rssIndex.toString());
				cur.setTitle(rssTitle);
				cur.setBody(rssContent);
			}
			rs.close();
		}
		catch (SQLException e) {
			e.printStackTrace();
		}
		return cur;
	}
	public Article getNextRandom(){
		Article cur = new BasicArticle();
		if(shuffleCount >= shuffle.length)
			return null;
		int id = shuffle[shuffleCount];
		shuffleCount++;
		//small fix, shouldn't really be here
		if(id==0){
			id = shuffle[shuffleCount];
			shuffleCount++;
		}
		try {
			ResultSet rs = stat.executeQuery("SELECT _id, title,content FROM feeds WHERE _id = " + id);
			while (rs.next()) {
				String rssTitle = rs.getString("title");
				String rssContent = rs.getString("content");
				Integer rssIndex = rs.getInt("_id");
				cur.setKey(rssIndex.toString());
				cur.setTitle(rssTitle);
				cur.setBody(rssContent);
			}
			rs.close();
		}
		catch (SQLException e) {
			e.printStackTrace();
		}
		return cur;
	}
	public void close(){
		try {
			stat.close();
			conn.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		finally{
			try {
				if(conn!=null)
					conn.close();
			} catch(SQLException se) {
				se.printStackTrace();
			}
		}
	}
	@Override
	public ArticleParser getArticleParser() {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public void setArticleParser(ArticleParser parser) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean supportArticleKeyRetrieval() {
		// TODO Auto-generated method stub
		return false;
	}
	@Override
	public void restart() {
		// TODO Auto-generated method stub
		
	}
	@Override
	public int size() {
		// TODO Auto-generated method stub
		return 0;
	}
}
