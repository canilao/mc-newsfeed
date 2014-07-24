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

import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;

import com.gmail.nossr50.events.experience.McMMOPlayerLevelUpEvent;

public class Database {

   public static final String driver = "org.h2.Driver";
   public static final String connectionStr = "jdbc:h2:./plugins/newsfeed/db/newsfeed.db;USER=sa;PASSWORD=password";

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

      query.append("INSERT INTO logins (player_Id, time, action)");
      query.append("    SELECT id, '%s',");
      query.append("        CASE WHEN (");
      query.append("                SELECT COUNT(*) FROM LOGINS WHERE player_id=(");
      query.append("                    SELECT * FROM PLAYERS WHERE name='%s')");
      query.append("                ) > 0  THEN 'login'");
      query.append("        ELSE 'initial'");
      query.append("        END");
      query.append("    FROM players");
      query.append("    WHERE name ='%s'");
      query.append(";");

      String querySql = String.format(query.toString(), time, name, name);

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

      query.append("INSERT INTO mcmmo_levelup_events (event_uuid, player_Id, time, skill_type, level) ");
      query.append("VALUES ( RANDOM_UUID(), %d, '%s', '%s', %d);");

      String querySql = String.format(query.toString(), getPlayerId(event
            .getPlayer().getName()), getIsoTime(), event.getSkill().getName(),
            event.getSkillLevel());

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
      createPlayerDeathTable();
   }

   private void createMcmmoLevelUpEventTable() throws SQLException {
      PreparedStatement stmt;
      StringBuilder query = new StringBuilder();

      query.append("CREATE TABLE IF NOT EXISTS mcmmo_levelup_events (");
      query.append("id INTEGER IDENTITY,");
      query.append("event_uuid UUID,");
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

   private void createPlayerDeathTable() throws SQLException {
      PreparedStatement stmt;
      StringBuilder query = new StringBuilder();

      query.append("CREATE TABLE IF NOT EXISTS player_death_events (");
      query.append("  id INTEGER IDENTITY,");
      query.append("  event_uuid UUID,");
      query.append("  player_Id INT,");
      query.append("  time TIMESTAMP,");
      query.append("  killers_name VARCHAR(20),");
      query.append("  cause_of_death VARCHAR(30),");
      query.append("  death_message VARCHAR(40),");
      query.append("  weapon_used VARCHAR(40),");
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

   public void insertPlayerDeath(PlayerDeathEvent event) throws SQLException {
      Statement stmt = null;
      StringBuilder query = new StringBuilder();

      query.append("INSERT INTO player_death_events (");
      query.append("  event_uuid,");
      query.append("  player_Id,");
      query.append("  time,");
      query.append("  killers_name,");
      query.append("  cause_of_death,");
      query.append("  death_message,");
      query.append("  weapon_used");
      query.append(") ");
      query.append("VALUES (");
      query.append("  RANDOM_UUID(),");
      query.append("  SELECT id FROM PLAYERS WHERE name=%s,");
      query.append("  '%s',");
      query.append("  %s,");
      query.append("  %s,");
      query.append("  %s,");
      query.append("  %s");
      query.append("); ");

      String deadPlayersName = null;
      String causeOfDeath = null;
      String deathMessage = null;
      String weaponUsed = null;
      String killersName = null;

      if (event.getDeathMessage() != null) {
         deathMessage = event.getDeathMessage();
      }
      if (event.getEntity() != null) {
         if (event.getEntity().getName() != null) {
            deadPlayersName = event.getEntity().getName();
         }
         if (event.getEntity().getLastDamageCause() != null
               && event.getEntity().getLastDamageCause().getCause() != null) {
            causeOfDeath = event.getEntity().getLastDamageCause().getCause()
                  .toString();
         }
         if (event.getEntity().getKiller() != null) {
            if (event.getEntity().getKiller() instanceof Player) {
               Player p = (Player) event.getEntity();
               p.getItemInHand().getType().toString();
               weaponUsed = p.getKiller().getItemInHand().getType().toString();
               killersName = p.getKiller().getName();
            }
         }
      }

      String querySql = String.format(query.toString(),
            deadPlayersName == null ? "null" : surroundQuotes(deadPlayersName),
            getIsoTime(), killersName == null ? "null"
                  : surroundQuotes(killersName), causeOfDeath == null ? "null"
                  : surroundQuotes(causeOfDeath), deathMessage == null ? "null"
                  : surroundQuotes(deathMessage), weaponUsed == null ? "null"
                  : surroundQuotes(weaponUsed));

      stmt = connection.createStatement();

      stmt.executeUpdate(querySql);

      stmt.close();
   }

   private String surroundQuotes(String text) {
      return "'" + text + "'";
   }
}
