package org.maylincraft.newsfeed;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

public class Database {

   public static final String driver = "org.sqlite.JDBC";
   public static final String connectionStr = "jdbc:sqlite:plugins/newsfeed/db/newsfeed.sqlite";

   private Connection connection = null;

   public Database() {
   }

   public void initialize() throws SQLException, InstantiationException,
         IllegalAccessException, ClassNotFoundException {
      setupDbConnection();
      createTables();
   }

   private void setupDbConnection() throws InstantiationException,
         IllegalAccessException, ClassNotFoundException, SQLException {
      Driver drvr = (Driver) Class.forName(driver).newInstance();
      DriverManager.registerDriver(drvr);
      connection = DriverManager.getConnection(connectionStr);
   }

   public void close() throws SQLException {
      connection.close();
   }

   public void insertPlayerLogin(String name, long time) throws SQLException {
      Statement stmt = null;
      StringBuilder query = new StringBuilder();

      query.append("INSERT INTO logins (playerId, time, action) ");
      query.append("select id, %d, 'login' ");
      query.append("from players where name ='%s'");
      query.append(";");

      String querySql = String.format(query.toString(), time, name);

      stmt = connection.createStatement();

      stmt.executeUpdate(querySql);
      
      stmt.close();
   }

   public void insertPlayerQuit(String name, long time) throws SQLException {
      Statement stmt = null;
      StringBuilder query = new StringBuilder();

      query.append("INSERT INTO logins (playerId, time, action) ");
      query.append("select id, %d, 'logout'");
      query.append("from players where name ='%s'");
      query.append(";");

      String querySql = String.format(query.toString(), time, name);

      stmt = connection.createStatement();

      stmt.executeUpdate(querySql);
      
      stmt.close();
   }

   private void createTables() throws SQLException {
      createPlayersTable();
      createLoginTable();
   }

   private void createPlayersTable() throws SQLException {
      PreparedStatement stmt;
      StringBuilder query = new StringBuilder();

      query.append("CREATE TABLE IF NOT EXISTS players (");
      query.append("id INTEGER PRIMARY KEY AUTOINCREMENT,");
      query.append("name TEXT UNIQUE");
      query.append(");");

      stmt = connection.prepareStatement(query.toString());

      stmt.executeUpdate();

      stmt.close();
   }

   private void createLoginTable() throws SQLException {
      PreparedStatement stmt;
      StringBuilder query = new StringBuilder();

      query.append("CREATE TABLE IF NOT EXISTS logins (");
      query.append("id INTEGER PRIMARY KEY AUTOINCREMENT,");
      query.append("playerId INTEGER,");
      query.append("action TEXT,");
      query.append("time INTEGER");     
      query.append(");");

      stmt = connection.prepareStatement(query.toString());

      stmt.executeUpdate();

      stmt.close();
   }

   public void insertRecordNewPlayer(String name) throws SQLException {
      Statement stmt = null;
      StringBuilder query = new StringBuilder();

      query.append("INSERT OR IGNORE INTO players (name) VALUES ('%s');");
      
      String querySql = String.format(query.toString(), name);

      stmt = connection.createStatement();

      stmt.executeUpdate(querySql);
      
      stmt.close();
   }
}
