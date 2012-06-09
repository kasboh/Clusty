package kbo;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Database Utility is used to access database
 * 
 * @author KBO
 * 
 */
public class DatabaseUtility {
	public DatabaseUtility() {
	}

	public static Connection getDBConnection() {
		try {
			Class.forName("org.sqlite.JDBC");
			Connection connection = null;
			// create database connection
			connection = DriverManager.getConnection("jdbc:sqlite:test.db");
			return connection;
		} catch (Exception e) {
			return null;
		}
	}

	public static void executeQuery(Connection con, String sql) throws Exception {
		Statement st;
		try {
			st = con.createStatement();
			st.executeUpdate(sql);
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
	}

	public static void closeConnection(Connection con) {
		try {
			con.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static ResultSet getResultSet(Connection con, String sql) {
		try {
			Statement st=con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
			//Statement st=con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);The field
			// ResultSet.TYPE_SCROLL_SENSITIVE creates a ResultSet object whose
			// cursor can move both forward and backward relative to the current
			// position and to an absolute position. The field
			// ResultSet.CONCUR_UPDATABLE creates a ResultSet object that can be
			// updated.
			// Statement st = con.createStatement();
			ResultSet rs = st.executeQuery(sql);
			return rs;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public static int getRecordCount(ResultSet rs) {
		if (rs == null)
			return 0;

		try {
			if (rs.getType() == ResultSet.TYPE_FORWARD_ONLY)
				return 0;
			else {
				int pos = rs.getRow();
				rs.last();
				int count = rs.getRow();
				if (pos == 0)
					rs.first();
				else
					rs.absolute(pos);

				return count;
			}
		} catch (SQLException e) {
			return 0;
		}
	}
}
