package config;

import java.io.*;
import java.util.Properties;

public class UserSession {

    private static String token;
    private static String username;
    private static final String FILE_PATH = "cache_auth.properties";

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

    public static void carregarDoArquivo() {
        File arquivo = new File(FILE_PATH);
        
        if (!arquivo.exists()) {
            return;
        }

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

        File arquivo = new File(FILE_PATH);
        if (arquivo.exists()) {
            arquivo.delete();
        }
    }

    public static String getToken() { return token; }
    public static String getUsername() { return username; }
}