<%@ page pageEncoding="UTF-8" contentType="text/html; charset=UTF-8" %>
<%@ page import="java.sql.*" %>
<%@ page import="api_oager.comprueba_ficheros" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ include file="menu.jspf" %>
<%@ include file="verificar_permisos.jspf" %>
<!DOCTYPE html>
<html lang="es">
<head>
  <meta charset="UTF-8">
  <title>comprueba ficheros</title>
  <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
  <style>
    table { border-collapse: collapse; width: 100%; margin-top: 18px; }
    th, td { padding: 8px 10px; border-bottom: 1px solid #e0e0e0; }
    thead th { background: #f6f6f6; text-align: left; }
    .actions form { display:inline; }
    .btn { padding: 6px 10px; border: 1px solid #ccc; background:#fafafa; border-radius:4px; cursor:pointer; }
    .btn:hover { background:#eee; }
    .info { margin: 12px 0; padding: 8px 12px; background:#f7fafd; border:1px solid #e1eefb; border-radius:6px; }
  </style>
</head>
<body>
  <div class="main-content">
    <h2>Ejecutar Descarga desde Principal</h2>

    <%
      // 1) Ejecutar descarga si se solicita
      String resultado = null;
      if ("descarga".equals(request.getParameter("action"))) {
          // Llama a tu método que comprueba en el API e inserta N nuevos en ATM_FICHERO_DESCARGA
          resultado = comprueba_ficheros.descarga();
      }
    %>

    <!-- URL de esta misma página para postear action=descarga -->
   <!-- Botón para lanzar la comprobación/descarga -->
<form method="post" action="${pageContext.request.requestURI}">
  <button type="submit" name="action" value="descarga" class="btn">
    Comprobar ficheros pendientes de descargar (API)
  </button>
</form>

    <% if (resultado != null) { %>
      <div class="info">
        <strong>Resultado:</strong> <%= resultado %>
      </div>
    <% } %>

    <h3>Pendientes de descarga en ATM_FICHERO_DESCARGA</h3>

    <table>
      <thead>
        <tr>
          <th>Identificador</th>
          <th>Código</th>
          <th>Ejercicio</th>
          <th>Facturación</th>
          <th>Tipo de deuda</th>
          <th class="actions">Acciones</th>
        </tr>
      </thead>
      <tbody>
      <%
        // 2) Listar PENDIENTES (descargado = 0)
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = DriverManager.getConnection(MM_RRHH_STRING, MM_RRHH_USERNAME, MM_RRHH_PASSWORD);

            String sql =
              "SELECT d.identificador_fichero, " +
              "       d.codigo, " +
              "       d.ejercicio, " +
              "       d.tipo_facturacion_contab AS facturacion, " +
              "       d.tipo_deuda, " +
              "       z.descripcion AS desc_facturacion, " +
              "       t.descripcion AS desc_deuda " +
              "  FROM atm_fichero_descarga d " +
              "  LEFT JOIN atm_tr_tipo_deuda t ON d.tipo_deuda = t.id " +
              "  LEFT JOIN atm_tr_tipo_fact_cont z ON d.tipo_facturacion_contab = z.id " +
              " WHERE d.descargado = 0 " +
              " ORDER BY z.descripcion, t.descripcion, TO_NUMBER(d.codigo)";

            ps = conn.prepareStatement(sql);
            rs = ps.executeQuery();

            boolean hayFilas = false;
            while (rs.next()) {
                hayFilas = true;
                String identificador = rs.getString("identificador_fichero");
                String codigo        = rs.getString("codigo");
                String ejercicio     = rs.getString("ejercicio");
                String facturacion   = rs.getString("facturacion");
                String tipoDeuda     = rs.getString("tipo_deuda");
                String descFact      = rs.getString("desc_facturacion");
                String descDeuda     = rs.getString("desc_deuda");
      %>
        <tr>
          <td><%= identificador %></td>
          <td><%= codigo %></td>
          <td><%= ejercicio %></td>
          <td><%= (descFact != null ? descFact : facturacion) %></td>
          <td><%= (descDeuda != null ? descDeuda : tipoDeuda) %></td>
          <td class="actions">
            <!-- 3) Botón que llama al servlet repetir_descarga con los parámetros requeridos -->
            <c:url var="repetirUrl" value="/repetir_descarga" />
            <form method="post" action="${repetirUrl}">
              <input type="hidden" name="year"         value="<%= ejercicio %>">
              <input type="hidden" name="codigo"       value="<%= codigo %>">
              <input type="hidden" name="facturacion"  value="<%= facturacion %>">
              <input type="hidden" name="tipo_deuda"   value="<%= tipoDeuda %>">
              <button type="submit" class="btn"
                      onclick="this.disabled=true; this.innerText='Descargando…'; this.form.submit();">
                Descargar
              </button>
            </form>
          </td>
        </tr>
      <%
            } // while
            if (!hayFilas) {
      %>
        <tr><td colspan="6" style="color:#666;">No hay ficheros pendientes (descargado = 0).</td></tr>
      <%
            }
        } catch (SQLException e) {
      %>
        <tr><td colspan="6" style="color:#b71c1c;">Error al obtener datos: <%= e.getMessage() %></td></tr>
      <%
        } finally {
            if (rs != null) try { rs.close(); } catch (Exception ignore) {}
            if (ps != null) try { ps.close(); } catch (Exception ignore) {}
            if (conn != null) try { conn.close(); } catch (Exception ignore) {}
        }
      %>
      </tbody>
    </table>

  </div>
</body>
</html>