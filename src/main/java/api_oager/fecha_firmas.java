package api_oager;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.sql.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class fecha_firmas {

    // ==== CONFIGURA AQUÍ LA CONEXIÓN ====
    // Opción A (recomendada): JDBC Thin de Oracle.
      
    private static final String JDBC_URL = "dafadf";
    private static final String DB_USER = "XXXX";
    private static final String DB_PASS = "XXXX";
    static LoggerService logger=new LoggerService();
    // Opción B (si usas un puente ODBC→JDBC de terceros con DSN):
    // private static final String JDBC_URL = "jdbc:odbc:DSN_MI_ORACLE"; // Requiere bridge de terceros.

    // Consulta de lectura (ajusta el WHERE si quieres limitar por estado)
    private static final String SQL_SELECT =
            "SELECT identificador_fichero, tipo_archivo, contenido_blob " +
            "FROM atm_fichero_ingresos " +
            "WHERE contenido_blob IS NOT NULL and tipo_archivo<>'otro' order by identificador_fichero,tipo_archivo ";

    // Update de destino
    private static final String SQL_UPDATE =
            "UPDATE atm_fichero_ingresos SET firma_fichero = ? WHERE identificador_fichero = ? and tipo_archivo=?";

    public static void main(String[] args) {
        int procesados = 0, actualizados = 0, sinFecha = 0, errores = 0;
       
        
        
        try (Connection cn = DriverManager.getConnection(JDBC_URL, DB_USER, DB_PASS)) {
            cn.setAutoCommit(false);

            try (PreparedStatement psSel = cn.prepareStatement(SQL_SELECT);
                 PreparedStatement psUpd = cn.prepareStatement(SQL_UPDATE)) {

                try (ResultSet rs = psSel.executeQuery()) {
                    while (rs.next()) {
                        procesados++;
                        String id = rs.getString("identificador_fichero");
                        String tipo = rs.getString("tipo_archivo"); // 'excel' o 'pdf'
                        Blob blob = rs.getBlob("contenido_blob");

                        LocalDate fechaFirma = null;
                        try (InputStream in = blob.getBinaryStream()) {
                            if ("excel".equalsIgnoreCase(tipo)) {
                                fechaFirma = extraerFechaDeExcel(in);
                                System.err.printf("ID %s: Fecha_firma excel : %s%n", id, fechaFirma.toString());
                            } else if ("pdf".equalsIgnoreCase(tipo)) {
                                fechaFirma = extraerFechaDePdf(in);
                                System.err.printf("ID %s: Fecha_firma pdf: %s%n", id, fechaFirma.toString());
                            } else {
                                System.err.printf("ID %s: tipo_archivo desconocido: %s%n", id, tipo);
                                logger.log(" tipo_archivo desconocido: ");
                            }
                        } catch (Exception e) {
                            errores++;
                            System.err.printf("ID %s: error leyendo blob: %s%n", id, e.getMessage());
                            logger.log(" error leyendo blob");
                            continue;
                        }

                        if (fechaFirma != null) {
                            psUpd.setDate(1, java.sql.Date.valueOf(fechaFirma));
                            psUpd.setString(2, id);
                            psUpd.setString(3, tipo);
                            try {
                                int n = psUpd.executeUpdate();
                                if (n > 0) actualizados++;
                            } catch (SQLException e) {
                                errores++;
                                System.err.printf("ID %s: error actualizando fecha: %s%n", id, e.getMessage());
                                logger.log("error actualizando fecha: %s%n");
                            }
                        } else {
                            sinFecha++;
                            System.out.printf("ID %s: no se pudo determinar fecha de firma%n", id);
                            logger.log("no se pudo determinar fecha de firma");
                        }
                    }
                }

                cn.commit();
            } catch (Exception e) {
                cn.rollback();
                throw e;
            }

        } catch (SQLException e) {
            System.err.println("Error de conexión/SQL: " + e.getMessage());
            System.exit(1);
        } catch (Exception e) {
            System.err.println("Error general: " + e.getMessage());
            System.exit(1);
        }

        System.out.printf("Procesados: %d, Actualizados: %d, Sin fecha: %d, Errores: %d%n",
                procesados, actualizados, sinFecha, errores);
    }

    // ================== EXCEL ==================
    // Lee B2 (fila 1, columna 1 en índice 0) intentando parsear como fecha.
    private static LocalDate extraerFechaDeExcel(InputStream in) throws IOException {
        Workbook wb = null;
        try {
            // Detecta y abre xls/xlsx automáticamente
            wb = WorkbookFactory.create(in);
            
            boolean use1904 = false;
            if (wb instanceof XSSFWorkbook) {
                use1904 = ((XSSFWorkbook) wb).isDate1904();
            }
            Sheet sheet = wb.getNumberOfSheets() > 0 ? wb.getSheetAt(0) : null;
            if (sheet == null) return null;

            Row row = sheet.getRow(1); // B2 -> fila 2 (índice 1)
            if (row == null) return null;

            Cell cell = row.getCell(1); // B2 -> columna 2 (índice 1)
            if (cell == null) return null;

            // Evalúa fórmula si la hay
            FormulaEvaluator evaluator = wb.getCreationHelper().createFormulaEvaluator();
            if (cell.getCellType() == CellType.FORMULA) {
                CellValue cv = evaluator.evaluate(cell);
                if (cv != null) {
                    switch (cv.getCellType()) {
                        case NUMERIC:
                            // ¿Es fecha?
                            if (isCellDateFormattedSafe(cell)) {
                                java.util.Date d = cell.getDateCellValue();
                                return d.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                            } else {
                                // Si no está marcado como fecha, aun así puede ser el serial de Excel
                                double serial = cv.getNumberValue();
                                java.util.Date d = DateUtil.getJavaDate(serial, use1904);
                                return d.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                            }
                        case STRING:
                            String s = cv.getStringValue();
                            return parseDateFromTextOrSerial(s, use1904);
                        default:
                            return null;
                    }
                }
            }

            // No es fórmula: trata tipos directos
            if (cell.getCellType() == CellType.NUMERIC) {
                if (DateUtil.isCellDateFormatted(cell)) {
                    java.util.Date d = cell.getDateCellValue();
                    return d.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                } else {
                    // Puede ser serial de Excel sin formato de fecha aplicado
                    double serial = cell.getNumericCellValue();
                    java.util.Date d = DateUtil.getJavaDate(serial, use1904);
                    return d.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                }
            } else if (cell.getCellType() == CellType.STRING) {
                String text = cell.getStringCellValue();
                if (text == null || text.trim().isEmpty()) return null;
                return parseDateFromTextOrSerial(text, use1904);
            } else {
                // Como último intento, usa DataFormatter (respeta formato visible en Excel)
                DataFormatter df = new DataFormatter(Locale.forLanguageTag("es-ES"));
                String display = df.formatCellValue(cell, evaluator);
                if (display != null && !display.trim().isEmpty()) {
                    return parseDateFromTextOrSerial(display, use1904);
                }
            }

            return null;
        } catch (Exception e) {
            // deja que el caller registre/decida
            throw new IOException("Error leyendo fecha B2: " + e.getMessage(), e);
        } finally {
            if (wb != null) {
                wb.close();
            }
        }
    }
    
    private static LocalDate parseDateFromTextOrSerial(String raw, boolean use1904) {
        if (raw == null) return null;
        String s = normalizeSpaces(raw);

        // 1) ¿Es un número serial de Excel?
        try {
            double serial = Double.parseDouble(s.replace(',', '.')); // soporta coma decimal
            java.util.Date d = DateUtil.getJavaDate(serial, use1904);
            return d.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        } catch (NumberFormatException ignore) {
            // no era número → seguimos probando patrones
        }

        // 2) Patrones habituales
        List<DateTimeFormatter> patrones = Arrays.asList(
            DateTimeFormatter.ofPattern("dd/MM/yyyy", new Locale("es", "ES")),
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", new Locale("es", "ES")),
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss", new Locale("es", "ES")),
            DateTimeFormatter.ofPattern("dd-MM-yyyy", new Locale("es", "ES")),
            DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm", new Locale("es", "ES")),
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
            DateTimeFormatter.ofPattern("dd.MM.yyyy"),
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
        );

        // 2a) Intentar parsear como LocalDate
        for (DateTimeFormatter f : patrones) {
            try { return LocalDate.parse(s, f); } 
            catch (Exception ignore) {}
        }

        // 2b) Intentar parsear como LocalDateTime y devolver solo fecha
        for (DateTimeFormatter f : patrones) {
            try { 
                LocalDateTime ldt = LocalDateTime.parse(s, f); 
                return ldt.toLocalDate();
            } catch (Exception ignore) {}
        }

        // 3) Intentar como ISO_INSTANT (ej: 2024-10-01T14:30:00Z)
        try {
            return Instant.parse(s).atZone(ZoneId.systemDefault()).toLocalDate();
        } catch (Exception ignore) {}

        return null;
    }

    /** Limpia espacios raros (NBSP, etc.) */
    private static String normalizeSpaces(String s) {
        return s.replace('\u00A0', ' ')
                .replace('\u2007', ' ')
                .replace('\u202F', ' ')
                .trim();
    }
    
    private static String getCellAsString(Cell cell) {
        DataFormatter fmt = new DataFormatter();
        return fmt.formatCellValue(cell);
    }
    
    private static boolean isCellDateFormattedSafe(Cell cell) {
        try {
            return cell != null && DateUtil.isCellDateFormatted(cell);
        } catch (Exception e) {
            return false;
        }
    }

    // ================== PDF ==================
    // Intenta leer la fecha de firma de los diccionarios de firma.
    private static LocalDate extraerFechaDePdf(InputStream in) throws IOException {
        try (PDDocument doc = PDDocument.load(in)) {
            List<PDSignature> firmas = doc.getSignatureDictionaries();
            for (PDSignature sig : firmas) {
                if (sig.getSignDate() != null) {
                    return sig.getSignDate().toInstant()
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate();
                }
            }
            // Fallback: usa Metadata/Info si no hay firmas (NO es firma electrónica, pero puede servir)
            if (doc.getDocumentInformation() != null) {
            	PDDocumentInformation info = doc.getDocumentInformation();
                String[] candidates = { info.getCustomMetadataValue("SigningDate"),
                                        info.getCreationDate() != null ? info.getCreationDate().getTime().toString() : null,
                                        info.getModificationDate() != null ? info.getModificationDate().getTime().toString() : null };
                for (String c : candidates) {
                    LocalDate parsed = tryParseDateLoose(c);
                    if (parsed != null) return parsed;
                }
            }
        }
        return null;
    }

    // Parser “flexible” para algunos formatos sueltos
    private static LocalDate tryParseDateLoose(String s) {
        if (s == null) return null;
        s = s.trim();
        List<DateTimeFormatter> formatos = Arrays.asList(
        	    DateTimeFormatter.ofPattern("dd/MM/yyyy"),
        	    DateTimeFormatter.ofPattern("yyyy-MM-dd"),
        	    DateTimeFormatter.ofPattern("dd-MM-yyyy"),
        	    DateTimeFormatter.ofPattern("dd.MM.yyyy")
        	);
        for (DateTimeFormatter f : formatos) {
            try { return LocalDate.parse(s, f); } catch (Exception ignore) {}
        }
        // Timestamps largos -> solo fecha
        try {
            return Instant.parse(s).atZone(ZoneId.systemDefault()).toLocalDate();
        } catch (Exception ignore) { }
        return null;
    }

    private static InputStream ensureMarkSupported(InputStream in) {
        return in.markSupported() ? in : new BufferedInputStream(in);
    }
}
