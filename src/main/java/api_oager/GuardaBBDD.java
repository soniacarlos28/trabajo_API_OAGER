package api_oager;

import java.io.*;
import java.sql.*;
import java.time.LocalDateTime;
import api_oager.*;

public class GuardaBBDD{
	 static LoggerService logger=new LoggerService();
    public static int guardarEnBaseDatos(String nombreFichero, String usuario, byte[] contenido,Connection conn,int tipoFacturacion,int tipoDeuda ) throws SQLException {
    	  int idFichero = -1;
    	  if  (conn.isClosed()) System.out.println("Conexion nula");
    		
    	  String sql = "{ call CARGA_ATM_FICHERO(?, ?, ?, ?, ?, ?) }";

    	  try (CallableStatement cs = conn.prepareCall(sql)) {

    	      cs.setString(1, nombreFichero);
    	      cs.setString(2, usuario);
    	      cs.setBytes(3, contenido); // Asegúrate que ficheroZip es un InputStream o Blob
    	      cs.setInt(4, tipoFacturacion);
    	      cs.setInt(5, tipoDeuda);
    	      cs.registerOutParameter(6, Types.NUMERIC); // Suponiendo que 'id' es un número

    	      cs.execute();

    	      idFichero = cs.getInt(6);
    	    

    	  } catch (SQLException e) {
    	      e.printStackTrace();
    	      // Manejo personalizado si es necesario
    	  }
    	  
    	              
        return idFichero;
            
    	}  
        
    

    public static boolean existeFichero(String nombreFichero,Connection conn) throws SQLException {
    	 if  (conn == null && conn.isClosed())  logger.log("Conexion nula");
        try (   	
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT COUNT(*) FROM ATM_FICHERO_CARGA WHERE nombre_fichero = ?")) {
        	logger.log("Consulta fichero base de datos");
            ps.setString(1, nombreFichero);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            logger.log("Error Consulta fichero" + e.getMessage());
        }
        return false;
    }
}










