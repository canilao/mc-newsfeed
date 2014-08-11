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
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.TimeZone;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerAchievementAwardedEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.h2.jdbcx.JdbcConnectionPool;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.maylincraft.newsfeed.NewsFeedPlugin;

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

      // Schedule news finder related to logins.
      scheduleLoginNewsFinder();
   }

   private void scheduleLoginNewsFinder() {
      new BukkitRunnable() {
         public void run() {
            try {
               NewsFeedPlugin.getNewsFeedDatabase().runLoginNewsFinder();
            } catch (IOException e) {
               NewsFeedPlugin.logSevere("Failed to run news finder", e);
            } catch (SQLException e) {
               NewsFeedPlugin.logSevere("Failed to run news finder", e);
            }
         }
      }.runTaskLater(NewsFeedPlugin.getInstance(), 20);
   }

   private void scheduleDeathNewsFinder() {
      new BukkitRunnable() {
         public void run() {
            try {
               NewsFeedPlugin.getNewsFeedDatabase().runDeathNewsFinder();
            } catch (IOException e) {
               NewsFeedPlugin.logSevere("Failed to run news finder", e);
            } catch (SQLException e) {
               NewsFeedPlugin.logSevere("Failed to run news finder", e);
            }
         }
      }.runTaskLater(NewsFeedPlugin.getInstance(), 20);
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

      // Schedule news finder related to logins.
      scheduleLoginNewsFinder();
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

      query.append("RUNSCRIPT FROM 'classpath:/scripts/CreateTables.sql'");

      stmt = connection.prepareStatement(query.toString());

      stmt.executeUpdate();

      stmt.close();
      connection.close();
   }

   @SuppressWarnings("unchecked")
   public JSONArray getNewsFeed(int startIndex, int endIndex)
         throws SQLException, IOException {

      final String script = "/scripts/formatted/SelectNews.sql";

      Connection connection = connPool.getConnection();
      Statement stmt = null;
      InputStream stream = Database.class.getResourceAsStream(script);
      BufferedReader in = new BufferedReader(new InputStreamReader(stream));
      StringBuilder queryFormat = new StringBuilder();
      String line;

      while ((line = in.readLine()) != null) {
         queryFormat.append(line);
         queryFormat.append(System.getProperty("line.separator"));
      }

      in.close();

      String querySql = String.format(queryFormat.toString(), startIndex,
            endIndex);

      ResultSet rs;
      JSONObject jsonObj = new JSONObject();
      JSONArray jsonArray = new JSONArray();

      stmt = connection.createStatement();

      rs = stmt.executeQuery(querySql);

      String newsType;
      UUID newsUUID;
      Timestamp newsTimestamp;

      try {
         while (rs.next()) {
            newsType = rs.getString("type");
            newsUUID = UUID.fromString(rs.getString("event_uuid"));
            newsTimestamp = rs.getTimestamp("time");

            if (newsType.equals("login")) {
               jsonObj = selectLoginNews(newsUUID, newsTimestamp);
            } else if (newsType.equals("death")) {
               jsonObj = selectDeathNews(newsUUID, newsTimestamp);
            } else if (newsType.equals("mcmmo_levelup")) {
               jsonObj = selectMcmmoLevelUpNews(newsUUID, newsTimestamp);
            } else if (newsType.equals("diamond_break")) {
               jsonObj = selectDiamondBreakNews(newsUUID, newsTimestamp);
            } else if (newsType.equals("achievement")) {
               jsonObj = selectAchievementNews(newsUUID, newsTimestamp);
            }

            jsonArray.add(jsonObj);
         }

      } catch (SQLException e) {
         NewsFeedPlugin.logWarning(
               "Failed to generate news feed for web request", e);
      }

      stmt.close();
      connection.close();

      return jsonArray;
   }

   @SuppressWarnings("unchecked")
   private JSONObject selectAchievementNews(UUID newsUUID,
         Timestamp newsTimestamp) throws SQLException {
      JSONObject jsonObj = new JSONObject();

      Connection connection = connPool.getConnection();

      Statement stmt = null;
      StringBuilder query = new StringBuilder();

      query.append("SELECT * ");
      query.append("FROM achievement_events a ");
      query.append("JOIN players b ON a.player_id=b.id ");
      query.append("WHERE event_uuid='%s'");
      query.append(";");

      String querySql = String.format(query.toString(), newsUUID.toString());

      stmt = connection.createStatement();

      ResultSet rs = stmt.executeQuery(querySql);

      while (rs.next()) {
         jsonObj.put("news_type", "achievement");
         jsonObj.put("event_uuid", newsUUID.toString());
         jsonObj.put("player_id", rs.getInt("player_id"));
         jsonObj.put("name", rs.getString("name"));
         jsonObj.put("time", rs.getTimestamp("time") + "Z");
         jsonObj.put("achievement_type", rs.getString("achievement_type"));
      }

      stmt.close();
      connection.close();

      return jsonObj;
   }

   @SuppressWarnings("unchecked")
   private JSONObject selectDiamondBreakNews(UUID newsUUID,
         Timestamp newsTimestamp) throws SQLException {
      JSONObject jsonObj = new JSONObject();

      Connection connection = connPool.getConnection();

      Statement stmt = null;
      StringBuilder query = new StringBuilder();

      query.append("SELECT block_count, name, time, block_type ");
      query.append("FROM diamond_break_news ");
      query.append("WHERE event_uuid='%s'");
      query.append(";");

      String querySql = String.format(query.toString(), newsUUID.toString());

      stmt = connection.createStatement();

      ResultSet rs = stmt.executeQuery(querySql);

      while (rs.next()) {
         jsonObj.put("news_type", "diamond_break");
         jsonObj.put("block_count", rs.getInt("block_count"));
         jsonObj.put("name", rs.getString("name"));
         jsonObj.put("time", rs.getTimestamp("time") + "Z");
         jsonObj.put("block_type", rs.getString("block_type"));
      }

      stmt.close();
      connection.close();

      return jsonObj;
   }

   @SuppressWarnings("unchecked")
   private JSONObject selectMcmmoLevelUpNews(UUID newsUUID,
         Timestamp newsTimestamp) throws SQLException {
      JSONObject jsonObj = new JSONObject();

      Connection connection = connPool.getConnection();

      Statement stmt = null;
      StringBuilder query = new StringBuilder();

      query.append("SELECT a.player_id, b.name, a.time, a.skill_type, a.level ");
      query.append("FROM mcmmo_levelup_events a ");
      query.append("JOIN players b ON a.player_id=b.id ");
      query.append("WHERE event_uuid='%s'");
      query.append(";");

      String querySql = String.format(query.toString(), newsUUID.toString());

      stmt = connection.createStatement();

      ResultSet rs = stmt.executeQuery(querySql);

      while (rs.next()) {
         jsonObj.put("news_type", "mcmmo_levelup");
         jsonObj.put("player_id", rs.getInt("player_id"));
         jsonObj.put("name", rs.getString("name"));
         jsonObj.put("time", rs.getTimestamp("time") + "Z");
         jsonObj.put("skill_type", rs.getString("skill_type"));
         jsonObj.put("level", rs.getInt("level"));
      }

      stmt.close();
      connection.close();

      return jsonObj;
   }

   @SuppressWarnings("unchecked")
   private JSONObject selectDeathNews(UUID newsUUID, Timestamp newsTimestamp)
         throws SQLException {
      JSONObject jsonObj = new JSONObject();
      JSONObject jsonDeathObj;
      JSONArray jsonDeathArray = new JSONArray();
      String name = "";
      int playerId = -1, groupLabel = -1, deathCount;
      Timestamp time;

      Connection connection = connPool.getConnection();

      Statement stmt = null;
      StringBuilder query = new StringBuilder();

      query.append("SELECT a.event_uuid, a.player_id, a.name, a.time, a.group_label, a.death_count ");
      query.append("FROM player_death_news a ");
      query.append("WHERE event_uuid='%s' ");
      query.append("LIMIT 1 ");
      query.append(";");

      String querySql = String.format(query.toString(), newsUUID.toString());

      stmt = connection.createStatement();

      ResultSet rs = stmt.executeQuery(querySql);

      while (rs.next()) {
         playerId = rs.getInt("player_id");
         time = rs.getTimestamp("time");
         groupLabel = rs.getInt("group_label");
         deathCount = rs.getInt("death_count");
         name = rs.getString("name");

         jsonObj.put("news_type", "death");
         jsonObj.put("player_id", playerId);
         jsonObj.put("name", name);
         jsonObj.put("time", time + "Z");
         jsonObj.put("death_count", deathCount);
      }

      // In case that nothing comes back from the query.
      if (groupLabel == -1 || playerId == -1) {
         return jsonObj;
      }

      stmt.close();

      stmt = null;
      query = new StringBuilder();

      query.append("SELECT * ");
      query.append("FROM death_groups a ");
      query.append("JOIN player_death_events b ON a.death_event_uuid=b.event_uuid ");
      query.append("WHERE a.group_label='%d' AND a.player_id='%d' ");
      query.append("ORDER BY TIME ASC ");
      query.append(";");

      querySql = String.format(query.toString(), groupLabel, playerId);

      stmt = connection.createStatement();

      rs = stmt.executeQuery(querySql);

      while (rs.next()) {
         jsonDeathObj = new JSONObject();

         jsonDeathObj.put("event_uuid", rs.getString("event_uuid"));
         jsonDeathObj.put("time", rs.getTimestamp("time") + "Z");
         jsonDeathObj.put("killers_name", rs.getString("killers_name"));
         jsonDeathObj.put("cause_of_death", rs.getString("cause_of_death"));
         jsonDeathObj.put("death_message", rs.getString("death_message"));
         jsonDeathObj.put("weapon_used", rs.getString("weapon_used"));

         jsonDeathArray.add(jsonDeathObj);
      }

      jsonObj.put("deaths", jsonDeathArray);

      stmt.close();
      connection.close();

      return jsonObj;
   }

   @SuppressWarnings("unchecked")
   private JSONObject selectLoginNews(UUID newsUUID, Timestamp newsTimestamp)
         throws SQLException {
      JSONObject jsonObj = new JSONObject();

      Connection connection = connPool.getConnection();

      Statement stmt = null;
      StringBuilder query = new StringBuilder();

      query.append("SELECT player_id, name, group_label, login_time, logout_time, last_action, play_time_minutes ");
      query.append("FROM login_news ");
      query.append("WHERE event_uuid='%s'");
      query.append(";");

      String querySql = String.format(query.toString(), newsUUID.toString());

      stmt = connection.createStatement();

      ResultSet rs = stmt.executeQuery(querySql);

      while (rs.next()) {
         jsonObj.put("news_type", "login");
         jsonObj.put("player_id", rs.getInt("player_id"));
         jsonObj.put("name", rs.getString("name"));
         jsonObj.put("group_label", rs.getInt("group_label"));
         jsonObj.put("login_time", rs.getTimestamp("login_time") + "Z");
         jsonObj.put("logout_time", rs.getTimestamp("logout_time") + "Z");
         jsonObj.put("last_action", rs.getString("last_action"));
         jsonObj.put("play_time_minutes", rs.getString("play_time_minutes"));
         // TODO: Change player information to UUIDs.
         Player thePlayer = Bukkit.getPlayerExact(rs.getString("name"));
         boolean isOnline = thePlayer == null ? false : thePlayer.isOnline()
               && rs.getString("last_action").equals("login");
         jsonObj.put("is_online", isOnline);
      }

      stmt.close();
      connection.close();

      return jsonObj;
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

   public void insertAchievementEvent(PlayerAchievementAwardedEvent event) throws Exception {
      Connection connection = connPool.getConnection();

      Statement stmt = null;
      StringBuilder query = new StringBuilder();
      
      String playersName = null;
      String time = getIsoTime();
      
      // TODO Change to UUID
      if (event.getPlayer() != null) {
         playersName = event.getPlayer().getName();
      } else {
         throw new Exception("Failed to insert Achievement");
      }

      query.append("INSERT INTO achievement_events (");
      query.append("  event_uuid,");
      query.append("  player_Id,");
      query.append("  time,");
      query.append("  achievement_type");
      query.append(") ");
      query.append("VALUES (");
      query.append("  RANDOM_UUID(),");
      query.append("  SELECT id FROM PLAYERS WHERE name='%s',");
      query.append("  '%s',");
      query.append("  '%s'");
      query.append("); ");

      String querySql = String.format(query.toString(), playersName, time, event.getAchievement().name());

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

      // Queue up the death news collection.
      scheduleDeathNewsFinder();
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
      }

      String querySql = String.format(query.toString(), playersName, time,
            blockType);

      stmt = connection.createStatement();

      stmt.executeUpdate(querySql);

      stmt.close();
      connection.close();

      // Schedule diamond news finder.
      scheduleDiamondNewsFinder();
   }

   private BukkitRunnable diamondNewsFinderRunnable = null;

   private void scheduleDiamondNewsFinder() {
      // Several diamond breaks can happen in succession, run it after he/she is
      // done.
      if (diamondNewsFinderRunnable == null) {
         diamondNewsFinderRunnable = new BukkitRunnable() {
            public void run() {
               try {
                  NewsFeedPlugin.getNewsFeedDatabase().runDiamondNewsFinder();
               } catch (IOException e) {
                  NewsFeedPlugin.logSevere("Failed to run news finder", e);
               } catch (SQLException e) {
                  NewsFeedPlugin.logSevere("Failed to run news finder", e);
               }
               diamondNewsFinderRunnable = null;
            }
         };
         diamondNewsFinderRunnable.runTaskLater(NewsFeedPlugin.getInstance(),
               20 * 60);
      }
   }

   public void runLoginNewsFinder() throws IOException, SQLException {

      final String script = "/scripts/formatted/LoginNewsFinder.sql";

      Connection connection = connPool.getConnection();
      Statement stmt = null;
      InputStream stream = Database.class.getResourceAsStream(script);
      BufferedReader in = new BufferedReader(new InputStreamReader(stream));
      StringBuilder query = new StringBuilder();
      String line;
      ArrayList<Integer> playerIdArray = new ArrayList<Integer>();

      while ((line = in.readLine()) != null) {
         query.append(line);
         query.append(System.getProperty("line.separator"));
      }

      in.close();

      // Get the player ids that we need to use to run the new finder.
      StringBuilder queryIds = new StringBuilder();

      queryIds.append("SELECT DISTINCT(id) FROM players;");
      Statement stmtIds = connection.createStatement();
      ResultSet rs = stmtIds.executeQuery(queryIds.toString());

      while (rs.next()) {
         playerIdArray.add(rs.getInt("id"));
      }

      stmtIds.close();

      // Run the news for each player.
      for (Integer playerId : playerIdArray) {

         // 2 hours.
         int timeThreshold = 2;
         // Unit of time for the threshold.
         String unitOftime = "HOUR";

         String querySql = String.format(query.toString(), playerId,
               timeThreshold, unitOftime);

         stmt = connection.createStatement();

         stmt.execute(querySql);
      }

      stmt.close();
      connection.close();
   }

   public void runDiamondNewsFinder() throws IOException, SQLException {

      final String script = "/scripts/formatted/DiamondNewsFinder.sql";

      Connection connection = connPool.getConnection();
      Statement stmt = null;
      InputStream stream = Database.class.getResourceAsStream(script);
      BufferedReader in = new BufferedReader(new InputStreamReader(stream));
      StringBuilder query = new StringBuilder();
      String line;
      ArrayList<Integer> playerIdArray = new ArrayList<Integer>();

      while ((line = in.readLine()) != null) {
         query.append(line);
         query.append(System.getProperty("line.separator"));
      }

      in.close();

      // Get the player ids that we need to use to run the new finder.
      StringBuilder queryIds = new StringBuilder();

      queryIds.append("SELECT DISTINCT(id) FROM players;");
      Statement stmtIds = connection.createStatement();
      ResultSet rs = stmtIds.executeQuery(queryIds.toString());

      while (rs.next()) {
         playerIdArray.add(rs.getInt("id"));
      }

      stmtIds.close();

      // Run the news for each player.
      for (Integer playerId : playerIdArray) {

         // 2 hours.
         int timeThreshold = 3;
         // Unit of time for the threshold.
         String unitOftime = "MINUTE";

         String querySql = String.format(query.toString(), playerId,
               timeThreshold, unitOftime);

         stmt = connection.createStatement();

         stmt.execute(querySql);
      }

      stmt.close();
      connection.close();
   }

   public void runDeathNewsFinder() throws IOException, SQLException {

      final String script = "/scripts/formatted/DeathNewsFinder.sql";

      Connection connection = connPool.getConnection();
      Statement stmt = null;
      InputStream stream = Database.class.getResourceAsStream(script);
      BufferedReader in = new BufferedReader(new InputStreamReader(stream));
      StringBuilder query = new StringBuilder();
      String line;
      ArrayList<Integer> playerIdArray = new ArrayList<Integer>();

      while ((line = in.readLine()) != null) {
         query.append(line);
         query.append(System.getProperty("line.separator"));
      }

      in.close();

      // Get the player ids that we need to use to run the new finder.
      StringBuilder queryIds = new StringBuilder();

      queryIds.append("SELECT DISTINCT(id) FROM players;");
      Statement stmtIds = connection.createStatement();
      ResultSet rs = stmtIds.executeQuery(queryIds.toString());

      while (rs.next()) {
         playerIdArray.add(rs.getInt("id"));
      }

      stmtIds.close();

      // Run the news for each player.
      for (Integer playerId : playerIdArray) {

         // 2 hours.
         int timeThreshold = 2;
         // Unit of time for the threshold.
         String unitOftime = "HOUR";

         String querySql = String.format(query.toString(), playerId,
               timeThreshold, unitOftime);

         stmt = connection.createStatement();

         stmt.execute(querySql);
      }

      stmt.close();
      connection.close();
   }
}
