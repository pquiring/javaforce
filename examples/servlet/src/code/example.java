/*
 * example.java
 *
 * Created : May 14, 2023
 *
 * Author : Peter Quiring
 *
 */

package code;

import java.io.*;

import javax.servlet.*;
import javax.servlet.http.*;


public class example extends HttpServlet {

  private static final boolean listing = false;

  /** Processes requests for both HTTP <code>GET</code> and <code>POST</code> methods.
   * @param request servlet request
   * @param response servlet response
   */
  protected void processRequest(HttpServletRequest request, HttpServletResponse response)
  throws ServletException, IOException {
    doRequest(request, response);
  }

  // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
  /** Handles the HTTP <code>GET</code> method.
   * @param request servlet request
   * @param response servlet response
   */
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
  throws ServletException, IOException {
    processRequest(request, response);
  }

  /** Handles the HTTP <code>POST</code> method.
   * @param request servlet request
   * @param response servlet response
   */
  protected void doPost(HttpServletRequest request, HttpServletResponse response)
  throws ServletException, IOException {
    processRequest(request, response);
  }

  /** Returns a short description of the servlet.
   */
  public String getServletInfo() {
    return "Short description";
  }
  // </editor-fold>

  public void init() {
  }

  public void destroy() {
  }

  public void doRequest(HttpServletRequest request, HttpServletResponse response) {
    try {
      PrintWriter out = response.getWriter();
      StringBuilder sb = new StringBuilder();
      sb.append("Example Servlet");
      out.print(sb.toString());
    } catch (Exception e) {
      try {
        response.setContentType("text/html;charset=UTF-8");
        OutputStream out = response.getOutputStream();
        out.write(("Exception:" + e).getBytes());
      } catch (Exception e2) {}
    }
  }

  public String getParameter(HttpServletRequest request, String name) {
    String value = request.getParameter(name);
    if (value == null) return "";
    return value;
  }

  public String getParameter(HttpServletRequest request, String name, String defvalue) {
    String value = request.getParameter(name);
    if (value == null) return defvalue;
    return value;
  }
}
