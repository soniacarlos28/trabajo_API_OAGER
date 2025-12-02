<%@ page import="java.sql.*, java.io.*" %>
<%@ include file="conn_oracle.jsp" %>
<%@ include file="verificar_permisos.jspf" %>
<%
    String idFichero = request.getParameter("id_fichero");
    String sql = "SELECT nombre_archivo, contenido_blob FROM atm_fichero_ingresos WHERE id = ?";
    Driver DriverSALDO = (Driver)Class.forName(MM_RRHH_DRIVER).newInstance();
    Connection conn = DriverManager.getConnection(MM_RRHH_STRING,MM_RRHH_USERNAME,MM_RRHH_PASSWORD);
    PreparedStatement ps = conn.prepareStatement(sql);
    ps.setInt(1, Integer.parseInt(idFichero));
    ResultSet rs = ps.executeQuery();

    if (rs.next()) {
        String fileName = rs.getString("nombre_archivo");
        Blob blob = rs.getBlob("contenido_blob");
        InputStream input = blob.getBinaryStream();

        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition", "attachment;filename=\"" + fileName + "\"");

        OutputStream outStream = response.getOutputStream();
        byte[] buffer = new byte[4096];
        int bytesRead = -1;

        while ((bytesRead = input.read(buffer)) != -1) {
            outStream.write(buffer, 0, bytesRead);
        }

        input.close();
        outStream.flush();
        outStream.close();
    }

    rs.close();
    ps.close();
    conn.close();
%>
