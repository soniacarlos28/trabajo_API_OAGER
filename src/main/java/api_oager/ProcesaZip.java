package api_oager;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ProcesaZip { 
	 static LoggerService logger=new LoggerService();
	 
	public static void procesarZip(byte[] contenidoZip, int idDescargaZip, Connection conn) {
		
		
	    try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(contenidoZip))) {
	        ZipEntry entry;
	        Integer i_descarga=1;
	        logger.log("Procesa ZIP");
	        while ((entry = zis.getNextEntry()) != null) {
	            String nombreArchivo = entry.getName();
	            String tipo = nombreArchivo.endsWith(".pdf") ? "pdf" :
	                          (nombreArchivo.endsWith(".xls") || nombreArchivo.endsWith(".xlsx")) ? "excel" : "otro";

	            ByteArrayOutputStream baos = new ByteArrayOutputStream();
	            byte[] buffer = new byte[8192];
	            int len;
	            while ((len = zis.read(buffer)) != -1) {
	                baos.write(buffer, 0, len);
	            }
	            byte[] contenido = baos.toByteArray();
	            
	            int idFichero = guardarEnIngresos(idDescargaZip, nombreArchivo, tipo, contenido, conn);
	            i_descarga++;
	          //  logger.log("Antes EXCEL");
	           if ("excel".equals(tipo)) {
	        	   logger.log("Dentro EXCEL");
	                procesarExcel(new ByteArrayInputStream(contenido), idFichero, conn);
	             } else if ("pdf".equals(tipo)) {
                    logger.log("Dentro PDF");
                    procesarPdf(new ByteArrayInputStream(contenido), idFichero, conn);
                }

	            zis.closeEntry();
	        }
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	}

    /*private static int guardarEnIngresos(int idDescarga, String nombreArchivo, String tipoArchivo, byte[] contenido,Connection conn) throws SQLException {
        int idFichero = -1;
        try (
             PreparedStatement ps = conn.prepareStatement(
                 "INSERT INTO ATM_FICHERO_INGRESOS (id_descarga, nombre_archivo, tipo_archivo, contenido_blob, fecha_registro) VALUES (?, ?, ?, ?, SYSTIMESTAMP)",
                 new String[]{"id"})) {
        	  
            ps.setInt(1, idDescarga);
            ps.setString(2, nombreArchivo);
            ps.setString(3, tipoArchivo);
            ps.setBytes(4, contenido);
            ps.executeUpdate();

            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                idFichero = rs.getInt(1);
            } 
        } 
        return idFichero;
    }*/
	
	private static int guardarEnIngresos(int idDescarga, String nombreArchivo, String tipoArchivo, byte[] contenido, Connection conn) throws SQLException {
	    int idFichero = -1;

	    // --- Cálculo de campos derivados ---
	    String identificadorFichero = null;
	    String ejercicio = null;
	    int entidad = 3;
	    String codigo = null;
	    int tipoDeuda = -1;
	    int tipoFact = 0;

	    try {
	        // Identificador = hasta primer "_"
	        int posUnderscore = nombreArchivo.indexOf("_");
	        if (posUnderscore > 0) {
	            identificadorFichero = nombreArchivo.substring(0, posUnderscore);
	        }

	        // Ejercicio = primeros 4 caracteres
	        if (nombreArchivo.length() >= 4) {
	            ejercicio = nombreArchivo.substring(0, 4);
	        }

	        // Código tras "Nº"
	        Pattern codigoPattern = Pattern.compile("N[ºº] ?(\\d+)");
	        Matcher matcher = codigoPattern.matcher(nombreArchivo);
	        if (matcher.find()) {
	            codigo = matcher.group(1);
	        }

	        // TipoDeuda
	        String[] clavesTipoDeuda = {"LIQUID", "AUTOLI", "RECIBO", "DEVOLU", "MULTA", "INGRES"};
	        String upper = nombreArchivo.toUpperCase();
	        for (int i = 0; i < clavesTipoDeuda.length; i++) {
	            if (upper.contains(clavesTipoDeuda[i])) {
	                tipoDeuda = i;
	                break;
	            }
	        }

	        // TipoFacturaciónContabilidad
	        if (upper.contains("CARGO")) {
	            tipoFact = 1;
	        } else if (upper.matches(".*LIQUID.*V.*")) {
	            tipoFact = 2;
	        } else if (upper.matches(".*LIQUID.*E.*")) {
	            tipoFact = 3;
	        } else if (upper.matches(".*AUTOLI.*V.*")) {
	            tipoFact = 2;
	        } else if (upper.matches(".*AUTOLI.*E.*")) {
	            tipoFact = 3;
	        } else if (upper.matches(".*IMPROCE.*V.*")) {
	            tipoFact = 4;
	        } else if (upper.matches(".*IMPROCE.*E.*")) {
	            tipoFact = 5;
	        } else if (upper.contains("FALLIDO")) {
	            tipoFact = 6;
	        }
	    } catch (Exception ex) {
	        // Opcional: loggear o lanzar excepción personalizada
	        System.err.println("Error al calcular campos derivados de nombreArchivo: " + ex.getMessage());
	    }

	    // --- Inserción en la base de datos ---
	  /*  String sql = "INSERT INTO ATM_FICHERO_INGRESOS " +
	            "(id_descarga, nombre_archivo, tipo_archivo, contenido_blob, fecha_registro, " +
	            "identificador_fichero, ejercicio, entidad, codigo, tipo_deuda, tipo_facturacion_contabilidad) " +
	            "VALUES (?, ?, ?, ?, SYSTIMESTAMP, ?, ?, ?, ?, ?, ?)";

	    try (PreparedStatement ps = conn.prepareStatement(sql, new String[]{"id"})) {
	        ps.setInt(1, idDescarga);
	        ps.setString(2, nombreArchivo);
	        ps.setString(3, tipoArchivo);
	        ps.setBytes(4, contenido);
	        ps.setString(5, identificadorFichero);
	        ps.setString(6, ejercicio);
	        ps.setInt(7, entidad);
	        ps.setString(8, codigo);
	        ps.setInt(9, tipoDeuda);
	        ps.setInt(10, tipoFact);

	        ps.executeUpdate();

	        ResultSet rs = ps.getGeneratedKeys();
	        if (rs.next()) {
	            idFichero = rs.getInt(1);
	        }
	    }*/
	    
	    String call = "{ call CARGA_ATM_FICHERO_INGRESOS(?,?,?,?,?,?,?,?,?,?,?) }";
	    try (CallableStatement cs = conn.prepareCall(call)) {
	        cs.setInt    (1, idDescarga);
	        cs.setString (2, nombreArchivo);
	        cs.setString (3, tipoArchivo);
	        cs.setBytes  (4, contenido);
	        cs.setString (5, identificadorFichero);
	        cs.setString (6, ejercicio);
	        cs.setInt    (7, entidad);
	        cs.setString (8, codigo);
	        cs.setInt    (9, tipoDeuda);
	        cs.setInt    (10, tipoFact);
	        cs.registerOutParameter(11, java.sql.Types.INTEGER);

	        cs.execute();

	        // Recuperamos el ID de salida
	        idFichero = cs.getInt(11);
	    }
	    return idFichero;

	    
	}


    private static void procesarExcel(InputStream excelStream, int idFichero,Connection conn) {
        
    	
    	
    	try (Workbook workbook = new XSSFWorkbook(excelStream);
        			 
        		
        		) {

            Sheet sheet = workbook.getSheetAt(0);
            for (Row row : sheet) {
                if (row.getRowNum() == 0) continue;

                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO ATM_FICHERO_ING_EXCEL (id_fichero, ejercicio, fecha, fecha_contabil, codigo_documento, tipo_documento, descripcion, ejercicio_concepto, " +
                                "concepto_ingresos, concepto_no_presup, concepto_roe, nif_deudor, importe, importe_iva, tipo_iva, importe_total, ordinal_tesoreria, " +
                                "referencia_padre, referencia, exaccion, referencia_interna, ente_roe) " +
                                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {

                    ps.setInt(1, idFichero);
                    for (int i = 0; i < 21; i++) {
                        Cell cell = row.getCell(i);
                        if (cell == null) {
                            ps.setNull(i + 2, Types.VARCHAR);
                        } else if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
                            ps.setDate(i + 2, new java.sql.Date(cell.getDateCellValue().getTime()));
                        } else if (cell.getCellType() == CellType.NUMERIC) {
                            ps.setDouble(i + 2, cell.getNumericCellValue());
                        } else {
                            ps.setString(i + 2, cell.toString());
                        }
                    }
                    ps.executeUpdate();
                }
            } 
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private static void procesarPdf(InputStream pdfStream, int idFichero, Connection conn) {
        try (PDDocument document = PDDocument.load(pdfStream)) {

            PDFTextStripper stripper = new PDFTextStripper();
            String texto = stripper.getText(document);

//            Pattern patron = Pattern.compile("(\\d{4}) (\\d{2}/\\d{2}/\\d{4}) +(\\d{4}) +(\\d+) +(\\w+) +([\\d,.]+) +(\\d{20,30})");
            Pattern patron = Pattern.compile(
            	    "(\\d{4})\\s+(\\d{2}/\\d{2}/\\d{4})\\s+(\\d{4})\\s+(?:(\\d{5})\\s+)?(\\w+)\\s+([\\d.,]+)\\s+(\\d{20,30})"
            	);

            Matcher matcher = patron.matcher(texto);
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");

            while (matcher.find()) {
                int ejercicio = Integer.parseInt(matcher.group(1));
                java.sql.Date fecha = new java.sql.Date(sdf.parse(matcher.group(2)).getTime());
                int ejercicioConcepto = Integer.parseInt(matcher.group(3));
                String concepto = matcher.group(4);
                String nif = matcher.group(5);
                double importe = Double.parseDouble(matcher.group(6).replace(".", "").replace(",", "."));
                String referencia = matcher.group(7);

                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO ATM_FICHERO_ING_PDF (ID_FICHERO, EJERCICIO, FECHA, EJERCICIO_CONCEPTO, CONCEPTO, NIF_DEUDOR, IMPORTE_TOTAL, REFERENCIA) " +
                                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {
                    ps.setInt(1, idFichero);
                    ps.setInt(2, ejercicio);
                    ps.setDate(3, fecha);
                    ps.setInt(4, ejercicioConcepto);
                    ps.setString(5, concepto);
                    ps.setString(6, nif);
                    ps.setDouble(7, importe);
                    ps.setString(8, referencia);
                    ps.executeUpdate();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
}