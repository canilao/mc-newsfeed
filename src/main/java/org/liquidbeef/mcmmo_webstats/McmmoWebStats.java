package org.liquidbeef.mcmmo_webstats;

import java.sql.*;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.eclipse.jetty.server.Server;

public class McmmoWebStats extends JavaPlugin {

   private static Database db;
   
   public static Database getSqliteDatabase() {
      return db;
   }

   @Override
   public void onEnable() {
      // Initialize the database.
      try {
         initializeDatabase();
      } catch (InstantiationException e) {
         logSevere("Failed to initialize database: "
               + e.getMessage());
      } catch (IllegalAccessException e) {
         logSevere("Failed to initialize database: "
               + e.getMessage());
      } catch (ClassNotFoundException e) {
         logSevere("Failed to initialize database: "
               + e.getMessage());
      } catch (SQLException e) {
         logSevere("Failed to initialize database: "
               + e.getMessage());
      }

      // Register our event listeners.
      registerListeners();

      // Start the web service.
      startWebServer();
   }


   @Override
   public void onDisable() {
      try {
         db.close();
      } catch (SQLException e) {
         logSevere("Failed to close the database: " + e.getMessage());
      }
   }

   @Override
   public boolean onCommand(CommandSender sender, Command cmd, String label,
         String[] args) {
      if (cmd.getName().equalsIgnoreCase("basic")) {
         // If the player typed
         // /basic then do the
         // following...
         // do something...
         return true;
      } else if (cmd.getName().equalsIgnoreCase("basic2")) {
         if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be run by a player.");
         } else {
            Player player = (Player) sender;
            player.sendMessage("asdfasdfasdf");
            // do something
         }
         return true;
      }
      return false;
   }

   private void logSevere(String msg) {
      this.getLogger().severe(msg);
   }

   private void initializeDatabase() throws InstantiationException,
         IllegalAccessException, ClassNotFoundException, SQLException {
      db = new Database();
      db.initialize();
   }

   private void registerListeners() {
      getServer().getPluginManager().registerEvents(new LoginListener(this), this);
   }

   private void startWebServer() {
      Server server = new Server(1975);
      server.setHandler(new HelloWorld());
      try {
         server.start();
      } catch (Exception e) {
         e.printStackTrace();
      }
   }
}
