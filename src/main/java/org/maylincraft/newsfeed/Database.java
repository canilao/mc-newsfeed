package org.maylincraft.newsfeed;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import com.gmail.nossr50.events.experience.McMMOPlayerLevelUpEvent;

public class Database {

   public static final String driver = "org.h2.Driver";
   public static final String connectionStr = "jdbc:h2:plugins/newsfeed/db/newsfeed.db;USER=sa;PASSWORD=sa";

   private Connection connection = null;

   static public String getIsoTime() {
      TimeZone tz = TimeZone.getTimeZone("UTC");
      DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
      df.setTimeZone(tz);
      String nowAsISO = df.format(new Date());
      return nowAsISO;
   }

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

   public void insertPlayerLogin(String name, String time) throws SQLException {
      Statement stmt = null;
      StringBuilder query = new StringBuilder();

      query.append("INSERT INTO logins (player_Id, time, action) ");
      query.append("SELECT id, '%s', 'login' ");
      query.append("FROM players WHERE name ='%s'");
      query.append(";");

      String querySql = String.format(query.toString(), time, name);

      stmt = connection.createStatement();

      stmt.executeUpdate(querySql);

      stmt.close();
   }

   public void insertPlayerQuit(String name, String time) throws SQLException {
      Statement stmt = null;
      StringBuilder query = new StringBuilder();

      query.append("INSERT INTO logins (player_Id, time, action) ");
      query.append("SELECT id, '%s', 'logout'");
      query.append("FROM players WHERE name ='%s'");
      query.append(";");

      String querySql = String.format(query.toString(), time, name);

      stmt = connection.createStatement();

      stmt.executeUpdate(querySql);

      stmt.close();
   }

   public void insertMcmmoSkillEvent(McMMOPlayerLevelUpEvent event)
         throws Exception {
      Statement stmt = null;
      StringBuilder query = new StringBuilder();

      query.append("INSERT INTO mcmmo_levelup_events (player_Id, time, skill_type, level) ");
      query.append("VALUES (%d, '%s', '%s', %d);");

      String querySql = String.format(query.toString(), getPlayerId(event
            .getPlayer().getName()), getIsoTime(), event.getSkill().getName(), event.getSkillLevel());

      stmt = connection.createStatement();

      stmt.executeUpdate(querySql);

      stmt.close();
   }

   public int getPlayerId(String name) throws Exception {
      Statement stmt = null;
      StringBuilder query = new StringBuilder();

      query.append("SELECT id FROM players where name='%s';");

      String querySql = String.format(query.toString(), name);

      stmt = connection.createStatement();

      ResultSet rs = stmt.executeQuery(querySql);

      Integer playerId = null;

      while (rs.next()) {
         playerId = rs.getInt("id");
         if (playerId != null) {
            break;
         }
      }

      if (playerId == null) {
         throw new Exception();
      }

      stmt.close();

      return playerId;
   }

   private void createTables() throws SQLException {
      createPlayersTable();
      createLoginTable();
      createMcmmoLevelUpEventTable();
   }

   private void createMcmmoLevelUpEventTable() throws SQLException {
      PreparedStatement stmt;
      StringBuilder query = new StringBuilder();

      query.append("CREATE TABLE IF NOT EXISTS mcmmo_levelup_events (");
      query.append("id INTEGER IDENTITY,");
      query.append("player_Id INT,");
      query.append("time TIMESTAMP,");
      query.append("skill_type VARCHAR(20),");
      query.append("level INT");
      query.append(");");

      stmt = connection.prepareStatement(query.toString());

      stmt.executeUpdate();

      stmt.close();
   }

   private void createPlayersTable() throws SQLException {
      PreparedStatement stmt;
      StringBuilder query = new StringBuilder();

      query.append("CREATE TABLE IF NOT EXISTS players (");
      query.append("id INTEGER IDENTITY,");
      query.append("name VARCHAR(16) UNIQUE");
      query.append(");");

      stmt = connection.prepareStatement(query.toString());

      stmt.executeUpdate();

      stmt.close();
   }

   private void createLoginTable() throws SQLException {
      PreparedStatement stmt;
      StringBuilder query = new StringBuilder();

      query.append("CREATE TABLE IF NOT EXISTS logins (");
      query.append("id BIGINT IDENTITY,");
      query.append("player_Id INT,");
      query.append("action VARCHAR(10),");
      query.append("time TIMESTAMP");
      query.append(");");

      stmt = connection.prepareStatement(query.toString());

      stmt.executeUpdate();

      stmt.close();
   }

   public void insertRecordNewPlayer(String name) throws SQLException {
      Statement stmt = null;
      StringBuilder query = new StringBuilder();

      query.append("MERGE INTO players KEY(name) VALUES(SELECT id FROM players WHERE name='%s', '%s');");

      String querySql = String.format(query.toString(), name, name);

      stmt = connection.createStatement();

      stmt.executeUpdate(querySql);

      stmt.close();
   }
}
