<%@ page language="java" contentType="text/html; charset=UTF-8" import="api_oager.*" %>
<%
    // Si ya está autenticado, redirigir
    if (session.getAttribute("user") != null) {
        response.sendRedirect("index.jsp");
        return;
    }

    String error = null;
    if ("POST".equalsIgnoreCase(request.getMethod())) {
        String user = request.getParameter("username");
        String pass = request.getParameter("password");
        if (user == null || user.isEmpty() || pass == null || pass.isEmpty()) {
            error = "Debe indicar usuario y contraseña.";
        } else {
            LdapLogin.AuthResult result = LdapLogin.authenticate(user, pass);
            switch (result) {
                case SUCCESS:
                    session.setAttribute("user", user);
                    response.sendRedirect("index.jsp");
                    return;
                case INVALID_CREDENTIALS:
                    error = "Usuario o contraseña incorrectos.";
                    break;
                case NO_GROUP:
                    error = "No tiene permisos para el grupo GA_R_COMERCIOS.";
                    break;
                case ERROR_CONNECTING:
                    error = "Error al conectar con el directorio. Intente más tarde.";
                    break;
            }
        }
    }
%>
<!DOCTYPE html>
<html lang="es">
<head>
  <meta charset="UTF-8">
  <title>Login</title>
  <style>
    form { max-width: 300px; margin: 150px auto; }
    .error { color: red; margin-bottom: 10px; }
  </style>
</head>
<body>
  <form method="post" action="login.jsp">
    <h2>API descarga de ficheros ingresos v2</h2>
    <% if (error != null) { %>
      <div class="error"><%= error %></div>
    <% } %>
    <div>
      <label for="username">Usuario:</label><br>
      <input type="text" id="username" name="username" autofocus>
    </div>
    <div style="margin-top:8px;">
      <label for="password">Contraseña:</label><br>
      <input type="password" id="password" name="password">
    </div>
    <div style="margin-top:12px;">
      <button type="submit">Entrar</button>
    </div>
  </form>
</body>
</html>