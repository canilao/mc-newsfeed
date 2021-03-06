package org.maylincraft.newsfeed.data;

import java.io.IOException;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.simple.JSONArray;
import org.maylincraft.newsfeed.NewsFeedPlugin;

public class NewsFeedInitialGroup extends HttpServlet {

   private static final long serialVersionUID = 1860794553944872859L;

   public void doGet(HttpServletRequest request, HttpServletResponse response)
         throws IOException, ServletException {
      try {
         String initialCountStr = request.getParameter("initialcount");

         if (initialCountStr == null) {
            throw new Exception();
         }

         int initialCount = Integer.parseInt(initialCountStr);

         if (initialCount < 1) {
            throw new Exception();
         }

         JSONArray jsonArray = NewsFeedPlugin.getNewsFeedDatabase()
               .getNews(0, initialCount + 1);

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
      } catch (NumberFormatException e) {
         NewsFeedPlugin.logWarning("Bad web request parameter", e);

         response.setContentType("text/html;charset=utf-8");
         response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      } catch (Exception e) {
         NewsFeedPlugin.logWarning("Bad newsfeed query", e);

         response.setContentType("text/html;charset=utf-8");
         response.setStatus(HttpServletResponse.SC_BAD_REQUEST);

         response.addHeader("Access-Control-Allow-Origin", "*");
      }
   }
}
