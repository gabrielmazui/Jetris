package config;

import java.io.*;
import java.util.Properties;

public class UserSession {

    public static Boolean logged = false;
    private static String token = null;
    private static String username = null;
    private static byte[] pfpBytes = null;
    private static final String FILE_PATH = "src/main/resources/cache_auth.properties";
    private static final String PFP_PATH = "src/main/resources/pfp.png";
    private static final String DEFAULT_PFP_PATH = "src/main/resources/default_pfp.png";

    public static void iniciarESalvarSessao(String t, String u) {
        token = t;
        username = u;

        Properties props = new Properties();
        props.setProperty("auth.token", token);
        props.setProperty("auth.username", username);

        try (FileWriter writer = new FileWriter(FILE_PATH)) {
            props.store(writer, "Cache de Autenticacao do App");
        } catch (IOException e) {
            System.out.println("Erro ao salvar o arquivo de cache: " + e.getMessage());
        }
    }

    public static void setPfp(byte[] bytes) {
        pfpBytes = bytes;
        try (FileOutputStream fos = new FileOutputStream(PFP_PATH)) {
            fos.write(bytes);
        } catch (IOException e) {
            System.out.println("Erro ao salvar pfp no cache: " + e.getMessage());
        }
    }

    public static byte[] getPfp() {
        if (pfpBytes != null) return pfpBytes;

        File file = new File(PFP_PATH);
        if (!file.exists()){
            file = new File(DEFAULT_PFP_PATH);
            if(!file.exists()){
                return null;
            }
        }

        try (FileInputStream fis = new FileInputStream(file)) {
            pfpBytes = fis.readAllBytes();
            return pfpBytes;
        } catch (IOException e) {
            System.out.println("Erro ao carregar pfp do cache: " + e.getMessage());
            return null;
        }
    }

    public static void carregarDoArquivo() {
        File arquivo = new File(FILE_PATH);
        if (!arquivo.exists()) return;

        Properties props = new Properties();
        try (FileReader reader = new FileReader(arquivo)) {
            props.load(reader);
            token = props.getProperty("auth.token");
            username = props.getProperty("auth.username");
        } catch (IOException e) {
            System.out.println("Erro ao ler o arquivo de cache: " + e.getMessage());
        }
    }

    public static void limparSessao() {
        token = null;
        username = null;
        logged = false;
        pfpBytes = null;

        new File(FILE_PATH).delete();
        new File(PFP_PATH).delete();
    }

    public static String getToken() { return token; }
    public static String getUsername() { return username; }
}