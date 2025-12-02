<%@ page contentType="text/html;charset=UTF-8"  import="java.sql.*"%>
<%@ include file="menu.jspf" %>
<!DOCTYPE html>
<html>
<head>
    <title>Detalle del Fichero</title>
    <link rel="stylesheet" type="text/css" href="../css/style.css">
    
</head>
<body>
<div class="content">
    <%
    String idDescarga = request.getParameter("id_descarga");
    String nombreFichero = "";
    
    String sqlNombre = "SELECT nombre_fichero FROM atm_fichero_carga WHERE id = ?";
    Driver DriverSALDO = (Driver)Class.forName(MM_RRHH_DRIVER).newInstance();
    Connection conn = DriverManager.getConnection(MM_RRHH_STRING,MM_RRHH_USERNAME,MM_RRHH_PASSWORD);
    PreparedStatement psNombre = conn.prepareStatement(sqlNombre);
    psNombre.setInt(1, Integer.parseInt(idDescarga));
    ResultSet rsNombre = psNombre.executeQuery();
    if (rsNombre.next()) {
        nombreFichero = rsNombre.getString("nombre_fichero").replace("%20", " ");
    }
    rsNombre.close();
    psNombre.close();
    %>

    <h1>Detalle del fichero: <%=nombreFichero%></h1>

    <%
    String sql = "SELECT id, nombre_archivo FROM atm_fichero_ingresos WHERE id_descarga=? AND UPPER(tipo_archivo)=?";
    PreparedStatement ps = conn.prepareStatement(sql);
    ResultSet rs;

    ps.setInt(1, Integer.parseInt(idDescarga));
    ps.setString(2, "EXCEL");
    rs = ps.executeQuery();
    %>
    <h2>Ficheros Excel</h2><ul>
    <% while (rs.next()) {
        int idFichero = rs.getInt("id");
        String nombreArchivo = rs.getString("nombre_archivo");
    %>
        <li>
            <%=nombreArchivo%>
            <a class="icon" href="contenido_excel.jsp?id_fichero=<%=idFichero%>" title="Ver contenido">📄</a>
            <a class="icon" href="descargar_blob.jsp?id_fichero=<%=idFichero%>" title="Descargar archivo">⬇️</a>
        </li>
    <% } rs.close(); %>
    </ul>

    <%
    ps.setInt(1, Integer.parseInt(idDescarga));
    ps.setString(2, "PDF");
    rs = ps.executeQuery();
    %>
    <h2>Ficheros PDF</h2><ul>
    <% while (rs.next()) {
        int idFichero = rs.getInt("id");
        String nombreArchivo = rs.getString("nombre_archivo");
    %>
        <li>
            <%=nombreArchivo%>
            <a class="icon" href="contenido_pdf.jsp?id_fichero=<%=idFichero%>" title="Ver contenido">📄</a>
            <a class="icon" href="descargar_blob.jsp?id_fichero=<%=idFichero%>" title="Descargar archivo">⬇️</a>
        </li>
    <% } rs.close(); ps.close(); conn.close(); %>
    </ul>
</div>
</body>
</html>

