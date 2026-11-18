package ec.udla.consulta.exception;

public final class ApiExceptions {

    private ApiExceptions() {}

    /** Dato solicitado no existe (404). */
    public static class NoEncontradoException extends RuntimeException {
        public NoEncontradoException(String msg) { super(msg); }
    }

    /** Regla de negocio incumplida, p. ej. no es persona natural (422). */
    public static class ReglaNegocioException extends RuntimeException {
        public ReglaNegocioException(String msg) { super(msg); }
    }

    /** Servicio externo caido y sin cache de respaldo (503). */
    public static class ServicioExternoException extends RuntimeException {
        public ServicioExternoException(String msg, Throwable cause) { super(msg, cause); }
        public ServicioExternoException(String msg) { super(msg); }
    }
}
