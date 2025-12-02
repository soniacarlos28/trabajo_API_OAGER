
<%@ page contentType="text/html;charset=UTF-8" import="java.sql.*"%>

<%@ include file="menu.jspf" %>
<!DOCTYPE html>
<html>
<head>
    <title>Contenido PDF</title>
    <link rel="stylesheet" type="text/css" href="../css/style.css">
</head>
<body>
<div class="content">
    <h1>Contenido del fichero PDF</h1>
    <%
    String idFichero = request.getParameter("id_fichero");
    String sql = "SELECT concepto, importe_total FROM atm_fichero_ing_pdf WHERE id_fichero=?";
    Driver DriverSALDO = (Driver)Class.forName(MM_RRHH_DRIVER).newInstance();
    Connection conn = DriverManager.getConnection(MM_RRHH_STRING,MM_RRHH_USERNAME,MM_RRHH_PASSWORD);
    PreparedStatement ps = conn.prepareStatement(sql);
    ps.setInt(1, Integer.parseInt(idFichero));
    ResultSet rs = ps.executeQuery();
    double total = 0;
    %>
    <table><tr><th>Concepto</th><th>Importe Total</th></tr>
    <% while (rs.next()) {
        String concepto = rs.getString("concepto");
        double importe = rs.getDouble("importe_total");
        total += importe;
    %>
    <tr><td><%=concepto%></td><td><%=importe%></td></tr>
    <% } %>
    <tr><th>Total</th><th><%=total%></th></tr>
    </table>
    <% rs.close(); ps.close(); conn.close(); %>
</div>
</body>
</html>
