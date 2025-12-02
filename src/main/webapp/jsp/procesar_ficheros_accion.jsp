<%@ page pageEncoding="UTF-8" contentType="text/html; charset=UTF-8" %>
<%@ page import="java.sql.*, java.net.URLEncoder" %>
<%@ include file="menu.jspf" %>
<%@ include file="verificar_permisos.jspf" %>
<%
request.setCharacterEncoding("UTF-8");

String identificador = request.getParameter("identificador");
String nuevoParam = request.getParameter("nuevo");
String returnUrl = request.getParameter("returnUrl");
if (returnUrl == null) returnUrl = "";

Integer nuevo = null;
try { nuevo = Integer.valueOf(nuevoParam); } catch(Exception ignore){}

if (identificador == null || identificador.trim().isEmpty() || nuevo == null) {
    response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Parámetros inválidos");
    return;
}

Connection conn = null;
PreparedStatement psSel = null, psUpd = null;
ResultSet rs = null;

try {
    conn = DriverManager.getConnection(MM_RRHH_STRING, MM_RRHH_USERNAME, MM_RRHH_PASSWORD);
    conn.setAutoCommit(false);

    psSel = conn.prepareStatement("SELECT descargado FROM atm_fichero_descarga WHERE identificador_fichero = ? FOR UPDATE");
    psSel.setString(1, identificador);
    rs = psSel.executeQuery();

    if (!rs.next()) {
        conn.rollback();
        response.sendError(HttpServletResponse.SC_NOT_FOUND, "Fichero no encontrado");
        return;
    }
    int actual = rs.getInt(1);

    boolean permitido = false;
    if (actual == 1 && (nuevo == 2 || nuevo == 3)) permitido = true;
    if (actual == 2 && nuevo == 3) permitido = true;
    if (!permitido) {
        conn.rollback();
        response.sendError(HttpServletResponse.SC_CONFLICT, "Transición no permitida");
        return;
    }

    psUpd = conn.prepareStatement("UPDATE atm_fichero_descarga SET descargado = ? WHERE identificador_fichero = ?");
    psUpd.setInt(1, nuevo);
    psUpd.setString(2, identificador);
    psUpd.executeUpdate();

    conn.commit();

    String destino = "listado_ficheros.jsp" + (returnUrl.isEmpty() ? "" : ("?" + returnUrl));
    response.sendRedirect(destino);

} catch (Exception ex) {
    if (conn != null) try { conn.rollback(); } catch(Exception ignore){}
    throw new ServletException("Error al actualizar estado", ex);
} finally {
    if (rs != null) try { rs.close(); } catch(Exception ignore){}
    if (psSel != null) try { psSel.close(); } catch(Exception ignore){}
    if (psUpd != null) try { psUpd.close(); } catch(Exception ignore){}
    if (conn != null) try { conn.close(); } catch(Exception ignore){}
}
%>
