package api_oager;

import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public class CredentialLoader {

    private static final String LOCAL_PATH = "src/main/resources/credentials.properties";
    private static final String CLASSPATH_RESOURCE = "credentials.properties";

    public static Properties load() {
        Properties props = new Properties();

        // First try: load from classpath (packaged resource)
        try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(CLASSPATH_RESOURCE)) {
            if (in != null) {
                props.load(in);
                return props;
            }
        } catch (Exception e) {
            System.out.println("Aviso: error cargando credenciales desde classpath: " + e.getMessage());
        }

        // Second try: load from project resources on filesystem (useful during desarrollo)
        Path p = Paths.get(LOCAL_PATH);
        if (Files.exists(p)) {
            try (InputStream fis = new FileInputStream(p.toFile())) {
                props.load(fis);
                return props;
            } catch (Exception e) {
                System.out.println("Aviso: error cargando credenciales desde '" + LOCAL_PATH + "': " + e.getMessage());
            }
        }

        // If we reach here, no credentials file found
        System.out.println("WARNING: No se encontró 'credentials.properties'. Copia 'credentials-example.properties' a 'src/main/resources/credentials.properties' y añade tus credenciales. Este archivo está en .gitignore para evitar subir secretos.");
        return props;
    }

    public static void main(String[] args) {
        Properties p = load();
        System.out.println("Credentials loaded: " + (!p.isEmpty()));
        if (!p.isEmpty()) {
            // Print only keys, never values (avoid leaking secrets in logs)
            System.out.println("Keys: " + p.keySet());
        }
    }
}
