package org.maylincraft.newsfeed.data;

import java.io.IOException;
import java.sql.SQLException;
import java.util.UUID;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.simple.JSONArray;
import org.maylincraft.newsfeed.NewsFeedPlugin;

public class OlderNewsFeedGroup extends HttpServlet {

   private static final long serialVersionUID = 7900161820232537542L;

   public void doGet(HttpServletRequest request, HttpServletResponse response)
         throws IOException, ServletException {
      try {
         String startUUIDStr = request.getParameter("startUUID");
         String countStr = request.getParameter("count"); 

         if (startUUIDStr == null || countStr == null) {
            throw new Exception();
         }

         UUID startUUID = UUID.fromString(startUUIDStr);
         int count = Integer.parseInt(countStr);

         JSONArray jsonArray = NewsFeedPlugin.getNewsFeedDatabase()
               .getOlderNewsByUUID(startUUID, count);

         response.setContentType("text/html;charset=utf-8");
         response.setStatus(HttpServletResponse.SC_OK);

         response.getWriter().println(jsonArray.toJSONString());
         response.addHeader("Access-Control-Allow-Origin", "*");

      } catch (SQLException e) {
         NewsFeedPlugin.logWarning(
               "Fail to generate news feed for web request", e);

         response.setContentType("text/html;charset=utf-8");
         response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

         response.addHeader("Access-Control-Allow-Origin", "*");
      } catch (Exception e) {
         NewsFeedPlugin.logWarning("Bad newsfeed query", e);

         response.setContentType("text/html;charset=utf-8");
         response.setStatus(HttpServletResponse.SC_BAD_REQUEST);

         response.addHeader("Access-Control-Allow-Origin", "*");
      }
   }
}
