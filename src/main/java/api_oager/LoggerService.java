package api_oager;

import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class LoggerService {
    private final String logFile;

    public LoggerService() {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        this.logFile = "c:\\temp\\diferencias-" + date + ".log";
    }

    public void log(String message) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        try (FileWriter fw = new FileWriter(logFile, true)) {
            fw.write("[" + timestamp + "] " + message + System.lineSeparator());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

