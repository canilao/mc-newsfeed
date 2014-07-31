package org.maylincraft.newsfeed.database;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.h2.jdbcx.JdbcConnectionPool;

import com.gmail.nossr50.events.experience.McMMOPlayerLevelUpEvent;

public class Database {

   public static final String driver = "org.h2.Driver";
   public static final String connectionStr = "jdbc:h2:./plugins/newsfeed/db/newsfeed.db";

   private JdbcConnectionPool connPool = null;

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

      connPool = JdbcConnectionPool.create(connectionStr, "sa", "password");
   }

   public void close() throws SQLException {
      connPool.dispose();
   }

   public void insertPlayerLogin(String name, String time) throws SQLException {
      Connection connection = connPool.getConnection();

      Statement stmt = null;
      StringBuilder query = new StringBuilder();

      query.append("INSERT INTO logins (player_Id, time, action)");
      query.append("    SELECT id, '%s',");
      query.append("        CASE WHEN (");
      query.append("                SELECT COUNT(*) FROM LOGINS WHERE player_id=(");
      query.append("                    SELECT id FROM PLAYERS WHERE name='%s')");
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
      connection.close();
   }

   public void insertPlayerQuit(String name, String time) throws SQLException {
      Connection connection = connPool.getConnection();

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
      connection.close();
   }

   public void insertMcmmoSkillEvent(McMMOPlayerLevelUpEvent event)
         throws Exception {
      Connection connection = connPool.getConnection();

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
      connection.close();
   }

   public int getPlayerId(String name) throws Exception {
      Connection connection = connPool.getConnection();

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
      connection.close();

      return playerId;
   }

   private void createTables() throws SQLException {
      Connection connection = connPool.getConnection();

      PreparedStatement stmt;
      StringBuilder query = new StringBuilder();

      query.append("RUNSCRIPT FROM 'classpath:/org/maylincraft/newsfeed/database/scripts/CreateTables.sql'");

      stmt = connection.prepareStatement(query.toString());

      stmt.executeUpdate();

      stmt.close();
      connection.close();
   }

   public void insertRecordNewPlayer(String name) throws SQLException {
      Connection connection = connPool.getConnection();

      Statement stmt = null;
      StringBuilder query = new StringBuilder();

      query.append("MERGE INTO players KEY(name) VALUES(SELECT id FROM players WHERE name='%s', '%s');");

      String querySql = String.format(query.toString(), name, name);

      stmt = connection.createStatement();

      stmt.executeUpdate(querySql);

      stmt.close();
      connection.close();
   }

   public void insertPlayerDeath(PlayerDeathEvent event) throws SQLException {
      Connection connection = connPool.getConnection();

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
      connection.close();
   }

   private String surroundQuotes(String text) {
      return "'" + text + "'";
   }

   public void insertDiamondOreBreak(BlockBreakEvent event) throws SQLException {
      Connection connection = connPool.getConnection();

      Statement stmt = null;
      StringBuilder query = new StringBuilder();

      query.append("INSERT INTO block_break_events (event_uuid, player_Id, time, blockType) ");
      query.append("VALUES (");
      query.append("  RANDOM_UUID(),");
      query.append("  SELECT id FROM PLAYERS WHERE name='%s', ");
      query.append("  '%s', ");
      query.append("  '%s' ");
      query.append(");");

      String playersName = null;
      String time = getIsoTime();
      String blockType = event.getBlock().getType().toString();

      if (event.getPlayer() != null) {
         playersName = event.getPlayer().getName();
         time = getIsoTime();
      }

      String querySql = String.format(query.toString(), playersName, time,
            blockType);

      stmt = connection.createStatement();

      stmt.executeUpdate(querySql);

      stmt.close();
      connection.close();
   }

   public void runNewsFinder() throws IOException, SQLException {

      final String script = "/org/maylincraft/newsfeed/database/scripts/formatted/LoginNewsFinder.sql";

      Connection connection = connPool.getConnection();
      Statement stmt = null;
      InputStream stream = Database.class.getResourceAsStream(script);
      BufferedReader in = new BufferedReader(new InputStreamReader(stream));
      StringBuilder query = new StringBuilder();
      String line;

      while ((line = in.readLine()) != null) {
         query.append(line);
         query.append(System.getProperty("line.separator"));
      }

      in.close();

      // Parameters for the script.
      int playerId = 3;
      // 2 hours.
      int timeThreshold = 2;
      // Unit of time for the threshold.
      String unitOftime = "HOUR";

      String querySql = String.format(query.toString(), playerId,
            timeThreshold, unitOftime);

      stmt = connection.createStatement();

      stmt.execute(querySql);

      stmt.close();
      connection.close();
   }
}
