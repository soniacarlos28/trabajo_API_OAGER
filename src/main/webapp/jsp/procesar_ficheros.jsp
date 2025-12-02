<%@ page pageEncoding="UTF-8" contentType="text/html; charset=UTF-8" %>
<%@ page import="java.sql.*" %>
<%@ include file="menu.jspf" %>
<%@ include file="verificar_permisos.jspf" %>
<!DOCTYPE html>
<html lang="es">
<head>
  <meta charset="UTF-8">
  <title>Procesar Ficheros Pendientes</title>
  <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
  <script>
    function toggleAll(master) {
      document.querySelectorAll('.chk-fichero').forEach(chk => chk.checked = master.checked);
    }
  </script>
</head>
<body>
  <div class="main-content">
    <h2>Marcar Ficheros como Procesados en GEMA</h2>

    <form action="procesar_ficheros_accion.jsp" method="post">
      <table>
        <thead>
          <tr>
            <th><input type="checkbox" onclick="toggleAll(this)"></th>
            <th>ID</th>
            <th>Identificador</th>
             <th>Código</th>
            <th>Facturación</th>
            <th>Tipo Deuda</th>
          </tr>
        </thead>
        <tbody>
        <%
          PreparedStatement ps = null;
          ResultSet rs = null;
          try {
              // conn viene de conn_oracle.jsp
              Driver DriverSALDO = (Driver)Class.forName(MM_RRHH_DRIVER).newInstance();
              Connection conn = DriverManager.getConnection(MM_RRHH_STRING,MM_RRHH_USERNAME,MM_RRHH_PASSWORD);
              ps = conn.prepareStatement(            
                "sELECT a.id,identificador_fichero, a.codigo, z.descripcion AS tipo2, t.descripcion AS tipo,a.tipo_archivo " +
 				"FROM  atm_fichero_ingresos a, atm_tr_tipo_deuda t, atm_tr_tipo_fact_cont z " +
			    "WHERE a.tipo_deuda = t.id AND a.tipo_facturacion_contabilidad = z.id   and  a.procesado = 1 " +
        		"and a.tipo_archivo='excel' " +
 				"ORDER BY tipo2, tipo, TO_NUMBER(codigo)"        );
              rs = ps.executeQuery();
              while (rs.next()) {
                  int id = rs.getInt("id");
                  String ident = rs.getString("identificador_fichero");
        %>
          <tr>
            <td>
              <input
                type="checkbox"
                name="ids"
                value="<%= id %>"
                class="chk-fichero"
              >
            </td>
            <td><%= id %></td>
            <td><%= ident %></td>
              <td><%= rs.getString("codigo") %></td>
            <td><%= rs.getString("tipo2") %></td>
            <td><%= rs.getString("tipo") %></td>
          </tr>
        <%
              }
          } catch (SQLException e) {
        %>
          <tr>
            <td colspan="3">Error al recuperar ficheros: <%= e.getMessage() %></td>
          </tr>
        <%
          } finally {
              if (rs != null) try { rs.close(); } catch(Exception ignore){}
              if (ps != null) try { ps.close(); } catch(Exception ignore){}
          }
        %>
        </tbody>
      </table>

      <div style="margin-top:15px;">
        <button type="submit">Marcar seleccionados como procesados</button>
      </div>
    </form>
  </div>
</body>
</html>

