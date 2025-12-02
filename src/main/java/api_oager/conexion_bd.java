package api_oager;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class conexion_bd {

    public static Connection conexion() throws SQLException {
        // 1) Carga explícita del driver (por si la detección automática falla)
        try {
            Class.forName("oracle.jdbc.OracleDriver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("No se encontró el driver de Oracle en el classpath", e);
        }

        // 2) URL sin espacios extraños y en formato válido
        String jdbcUrl = 
            "fdsfsd";

        String usuario = "XXXXX";
	    String contrasena = "XXXXX";

        // 3) No envolver en try-with-resources: lo dejamos abierto para quien lo invoque
        return DriverManager.getConnection(jdbcUrl, usuario, contrasena);
    }
}

