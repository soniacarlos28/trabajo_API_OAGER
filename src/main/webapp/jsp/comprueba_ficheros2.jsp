<%@ page pageEncoding="UTF-8" contentType="text/html; charset=UTF-8" %>
<%@ page import="api_oager.comprueba_ficheros" %>
<%@ include file="menu.jspf" %>
<%@ include file="verificar_permisos.jspf" %>
<!DOCTYPE html>
<html lang="es">
<head>
  <meta charset="UTF-8">
  <title>Ejecutar Descarga</title>
  <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
</head>
<body>
  <div class="main-content">
    <h2>Ejecutar Descarga desde Principal</h2>

    <%
      // Si el parámetro action=descarga viene en la petición, llamamos al método Java
      String resultado = null;
      String usuario = (String) session.getAttribute("user");
      if ("descarga".equals(request.getParameter("action"))) {
          resultado=comprueba_ficheros.descarga();
      }
    %>

    <form method="post">
      <!-- Al pulsar este botón se reenviará 'action=descarga' -->
      <button type="submit" name="action" value="descarga">
        Comprobar fichero pendientes de descargar
      </button>
    </form>

    <% if (resultado != null) { %>
      <div class="actions">
        <p><strong>Resultado:</strong> <%= resultado %></p>
      </div>
    <% } %>

  </div>
</body>
</html>