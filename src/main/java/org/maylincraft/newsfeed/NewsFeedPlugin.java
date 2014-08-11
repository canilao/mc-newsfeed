package org.maylincraft.newsfeed;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.security.CodeSource;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.maylincraft.newsfeed.data.McmmoFullStats;
import org.maylincraft.newsfeed.data.NewsFeed;
import org.maylincraft.newsfeed.database.Database;
import org.maylincraft.newsfeed.listeners.BlockBreakListener;
import org.maylincraft.newsfeed.listeners.LoginListener;
import org.maylincraft.newsfeed.listeners.McmmoXpGainListener;
import org.maylincraft.newsfeed.listeners.PlayerDeathListener;

public class NewsFeedPlugin extends JavaPlugin {

   private static Database db;
   private static NewsFeedPlugin thePlugin;

   public static Database getNewsFeedDatabase() {
      return db;
   }

   public static NewsFeedPlugin getInstance() {
      return thePlugin;
   }

   public static void logSevere(String msg, Exception e) {
      StringWriter sw = new StringWriter();
      PrintWriter pw = new PrintWriter(sw);
      e.printStackTrace(pw);
      thePlugin.getLogger().severe(msg + " - " + sw.toString());
   }

   public static void logWarning(String msg, Exception e) {
      StringWriter sw = new StringWriter();
      PrintWriter pw = new PrintWriter(sw);
      e.printStackTrace(pw);
      thePlugin.getLogger().severe(msg + " - " + sw.toString());
   }

   @Override
   public void onEnable() {
      thePlugin = this;
      // Create the directory structure.
      createDirectoryStructure();
      // Initialize the database.
      try {
         initializeDatabase();
      } catch (InstantiationException e) {
         logSevere("Failed to initialize database: ", e);
      } catch (IllegalAccessException e) {
         logSevere("Failed to initialize database: ", e);
      } catch (ClassNotFoundException e) {
         logSevere("Failed to initialize database: ", e);
      } catch (SQLException e) {
         logSevere("Failed to initialize database: ", e);
      }

      // Register our event listeners.
      registerListeners();

      // Start the web service.
      try {
         startWebServer();
      } catch (Exception e) {
         logSevere("Failed to initialize webserver: ", e);
      }

      // Start scheduled tasks.
      startSchedulers();
   }

   private void startSchedulers() {
      final long fifteenMinutes = 20L * 60L * 15L;
      BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
      scheduler.scheduleSyncRepeatingTask(this, new Runnable() {
         public void run() {
            try {
               getNewsFeedDatabase().runLoginNewsFinder();
               getNewsFeedDatabase().runDiamondNewsFinder();
               getNewsFeedDatabase().runDeathNewsFinder();
            } catch (IOException e) {
               logWarning("Failed to initialize run News Finder: ", e);
               e.printStackTrace();
            } catch (SQLException e) {
               logWarning("Failed to initialize run News Finder: ", e);
            }
         }
      }, 0L, fifteenMinutes);
   }

   private void createDirectoryStructure() {
      boolean success = true;

      File rsrcDir = new File("plugins/newsfeed/");
      if (!rsrcDir.exists()) {
         success &= rsrcDir.mkdir();
      }
      File dbDir = new File("plugins/newsfeed/db/");
      if (!dbDir.exists()) {
         success &= dbDir.mkdir();
      }
      File webDir = new File("plugins/newsfeed/web/");
      if (!webDir.exists()) {
         success &= webDir.mkdir();
      }
      webDir = new File("plugins/newsfeed/web/css/");
      if (!webDir.exists()) {
         success &= webDir.mkdir();
      }
      webDir = new File("plugins/newsfeed/web/js/");
      if (!webDir.exists()) {
         success &= webDir.mkdir();
      }
      webDir = new File("plugins/newsfeed/web/fonts/");
      if (!webDir.exists()) {
         success &= webDir.mkdir();
      }
      webDir = new File("plugins/newsfeed/web/images/");
      if (!webDir.exists()) {
         success &= webDir.mkdir();
      }

      try {
         if (success) {
            final String pluginDirectory = "plugins/newsfeed/";

            // Copy the files to the structure.
            ArrayList<String> files = getListOfFilesInJarLocation("web/");

            for (String file : files) {
               copyFileFromJar("/" + file, pluginDirectory + file);
            }
         } else {
            throw new IOException();
         }
      } catch (IOException e) {
         logSevere("Failed to create directory structure", e);
      }
   }

   private ArrayList<String> getListOfFilesInJarLocation(String resourcePath)
         throws IOException {
      ArrayList<String> files = new ArrayList<String>();
      CodeSource src = NewsFeedPlugin.class.getProtectionDomain()
            .getCodeSource();
      if (src != null) {
         URL jar = src.getLocation();
         ZipInputStream zip = new ZipInputStream(jar.openStream());
         while (true) {
            ZipEntry e = zip.getNextEntry();
            if (e == null)
               break;
            String name = e.getName();
            if (name.startsWith(resourcePath) && !e.isDirectory()) {
               files.add(name);
            }
         }
      } else {
      }
      return files;
   }

   private void copyFileFromJar(String resourcePath, String destinationPath)
         throws IOException {
      InputStream stream = NewsFeedPlugin.class
            .getResourceAsStream(resourcePath);
      if (stream == null) {
         getLogger().severe("Failed to create input stream");
      }
      OutputStream resStreamOut = null;
      int readBytes;
      byte[] buffer = new byte[4096];
      try {
         resStreamOut = new FileOutputStream(new File(destinationPath));
         while ((readBytes = stream.read(buffer)) > 0) {
            resStreamOut.write(buffer, 0, readBytes);
         }
      } catch (IOException e) {
         logSevere("Failed to write files", e);
      } finally {
         stream.close();
         if (resStreamOut != null) {
            resStreamOut.close();
         }
      }
   }

   @Override
   public void onDisable() {
      try {
         db.close();
      } catch (SQLException e) {
         logSevere("Failed to close the database: ", e);
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

   private void initializeDatabase() throws InstantiationException,
         IllegalAccessException, ClassNotFoundException, SQLException {
      db = new Database();
      db.initialize();
   }

   private void registerListeners() {
      getServer().getPluginManager().registerEvents(new LoginListener(this),
            this);
      getServer().getPluginManager().registerEvents(
            new McmmoXpGainListener(this), this);
      getServer().getPluginManager().registerEvents(
            new PlayerDeathListener(this), this);
      getServer().getPluginManager().registerEvents(
            new BlockBreakListener(this), this);
   }

   private void startWebServer() throws Exception {
      Server server = new Server(1975);

      ServletContextHandler context = new ServletContextHandler(
            ServletContextHandler.SESSIONS);

      ResourceHandler resource_handler = new ResourceHandler();
      resource_handler.setDirectoriesListed(true);
      resource_handler.setWelcomeFiles(new String[] { "index.html" });
      resource_handler.setResourceBase("plugins/newsfeed/web");

      context.setContextPath("/newsfeed");
      context.setResourceBase("plugins/newsfeed/web");
      context.setHandler(resource_handler);
      context.setClassLoader(Thread.currentThread().getContextClassLoader());

      ServletContextHandler helloContextHandler = new ServletContextHandler(
            ServletContextHandler.SESSIONS);
      helloContextHandler.setContextPath("/data");
      helloContextHandler.addServlet(new ServletHolder(new McmmoFullStats()),
            "/fullstats");
      helloContextHandler.addServlet(new ServletHolder(new NewsFeed()),
            "/newsfeed");

      ContextHandlerCollection contexts = new ContextHandlerCollection();
      contexts.setHandlers(new Handler[] { context, helloContextHandler });

      server.setHandler(contexts);

      server.start();
   }
}
