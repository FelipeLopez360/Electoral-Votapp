package co.com.votapp.ws;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Configura el socket de Docker para Testcontainers antes de que el contexto de Spring arranque.
 *
 * <p>Docker Desktop >= 4.13 en macOS expone DOS sockets:
 * <ul>
 *   <li>{@code docker.sock} — proxy stub: responde HTTP 400 en {@code /info} con payload vacío.
 *       Testcontainers interpreta esto como "Docker no disponible" aunque Docker sí esté corriendo.</li>
 *   <li>{@code docker.raw.sock} — daemon real: responde HTTP 200 en {@code /info} con metadata real.</li>
 * </ul>
 * La solución es forzar Testcontainers a usar {@code docker.raw.sock} vía
 * {@code DOCKER_HOST} (system property o env var).
 */
public final class TestcontainersDockerConfig {

    private TestcontainersDockerConfig() {
    }

    public static void configure() {
        // Testcontainers respeta DOCKER_HOST como system property cuando se setea ANTES de que
        // DockerClientFactory inicialice su estrategia.
        if (System.getProperty("DOCKER_HOST") == null) {
            String socket = resolveDockerSocket();
            if (socket != null) {
                System.setProperty("DOCKER_HOST", socket);
                // Testcontainers también necesita saber la ruta del socket para montarlo en ryuk
                String socketPath = socket.replace("unix://", "");
                System.setProperty("TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE", socketPath);
            }
        }
        // docker.raw.sock requiere API v1.44+. docker-java lee "api.version" de System.getProperties().
        // Forzar 1.44 evita el fallback a 1.32 que docker.raw.sock rechaza.
        if (System.getProperty("api.version") == null) {
            System.setProperty("api.version", "1.44");
        }
        // Ryuk (resource reaper) intenta montar el socket de Docker dentro de un contenedor.
        // El socket docker.raw.sock no es accesible desde contenedores internos en Docker Desktop macOS.
        // Deshabilitar Ryuk es seguro en desarrollo: los contenedores de test se limpian al finalizar.
        if (System.getProperty("TESTCONTAINERS_RYUK_DISABLED") == null) {
            System.setProperty("TESTCONTAINERS_RYUK_DISABLED", "true");
        }
    }

    private static String resolveDockerSocket() {
        // 1. Respetar variable de entorno explícita (CI/CD con socket correcto ya configurado)
        String envDockerHost = System.getenv("DOCKER_HOST");
        if (envDockerHost != null && !envDockerHost.isBlank()) {
            return envDockerHost;
        }
        // 2. Docker Desktop en macOS: docker.raw.sock es el daemon real (devuelve HTTP 200 en /info).
        //    docker.sock es un proxy stub que devuelve HTTP 400 — Testcontainers lo rechaza.
        Path rawSock = Path.of(System.getProperty("user.home"),
                "Library/Containers/com.docker.docker/Data/docker.raw.sock");
        if (Files.exists(rawSock)) {
            return "unix://" + rawSock;
        }
        // 3. Fallback: ~/.docker/run/docker.sock (puede funcionar en versiones anteriores de Docker Desktop)
        Path runSock = Path.of(System.getProperty("user.home"), ".docker/run/docker.sock");
        if (Files.exists(runSock)) {
            return "unix://" + runSock;
        }
        // 4. Socket estándar Linux / CI
        if (Files.exists(Path.of("/var/run/docker.sock"))) {
            return "unix:///var/run/docker.sock";
        }
        return null;
    }
}
