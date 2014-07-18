package org.maylincraft.newsfeed;

import java.sql.SQLException;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class LoginListener implements Listener {

   private McmmoWebStats plugIn;

   public LoginListener(McmmoWebStats thePlugin) {
      plugIn = thePlugin;
   }

   @EventHandler
   public void onPlayerLogin(PlayerLoginEvent event) {
      Database db = McmmoWebStats.getSqliteDatabase();
      try {
         db.insertRecordNewPlayer(event.getPlayer().getName()); 
         db.insertPlayerLogin(event.getPlayer().getName(), System.currentTimeMillis());
      } catch (SQLException e) {
         plugIn.getLogger().warning(e.getMessage());
      }
   }
   
   @EventHandler
   public void onPlayerQuit(PlayerQuitEvent event) {
      Database db = McmmoWebStats.getSqliteDatabase();
      try {
         db.insertPlayerQuit(event.getPlayer().getName(), System.currentTimeMillis());
      } catch (SQLException e) {
         plugIn.getLogger().warning(e.getMessage());
      }
   }

   /*
   private String getIsoTime() {
      TimeZone tz = TimeZone.getTimeZone("UTC");
      DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
      df.setTimeZone(tz);
      String nowAsISO = df.format(new Date());

      return nowAsISO;
   }
   */
}
