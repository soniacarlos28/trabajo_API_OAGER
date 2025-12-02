
<%@ page contentType="text/html;charset=UTF-8" import="java.sql.*"%>
<%@ include file="verificar_permisos.jspf" %>
<!DOCTYPE html>
<html>
<head>
    <title>Consulta de Descargas</title>
    <link rel="stylesheet" type="text/css" href="../css/style.css">
</head>
<body>

<%@ include file="menu.jspf" %>

<div class="content">
    <h1>Ficheros Descargados</h1>
    <table><tr><th>Nombre</th><th>Usuario</th><th>Fecha Descarga</th></tr>
    <%
    String sql = "SELECT decode(to_char(sysdate,'dd/mm/yyyy'),to_char(fecha_hora,'DD/mm/yyyy'),'Nuevo','') as reg,id,nombre_fichero as nombre_archivo, usuario_windows, fecha_hora as fecha_registro FROM atm_fichero_carga ORDER BY fecha_registro DESC";
    Driver DriverSALDO = (Driver)Class.forName(MM_RRHH_DRIVER).newInstance();
    Connection conn = DriverManager.getConnection(MM_RRHH_STRING,MM_RRHH_USERNAME,MM_RRHH_PASSWORD);
    PreparedStatement ps = conn.prepareStatement(sql);
    ResultSet rs = ps.executeQuery();
    while (rs.next()) {
        int id = rs.getInt("id");
        String nombre = rs.getString("nombre_archivo");
        String usuario = rs.getString("usuario_windows");
        Timestamp fecha = rs.getTimestamp("fecha_registro");
        String reg         = rs.getString("reg");               // <— tu columna reg
        boolean esNuevo    = "Nuevo".equalsIgnoreCase(reg);
    %>
     <tr class="<%= esNuevo ? "highlight-nuevo" : "" %>">
        <td><a href="detalle_fichero.jsp?id_descarga=<%=id%>"><%=nombre%></a></td>
        <td><%=usuario%></td>
        <td><%=fecha%></td>
    </tr>
    <%
    }
    rs.close(); ps.close(); conn.close();
    %>
    </table>
</div>

</body>
</html>
