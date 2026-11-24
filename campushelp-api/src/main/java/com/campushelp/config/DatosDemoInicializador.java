package com.campushelp.config;

import com.campushelp.model.Actualizacion;
import com.campushelp.model.Evidencia;
import com.campushelp.model.Incidencia;
import com.campushelp.model.Lugar;
import com.campushelp.model.Prioridad;
import com.campushelp.model.Rol;
import com.campushelp.model.TipoProblema;
import com.campushelp.model.Usuario;
import com.campushelp.repository.IncidenciaRepository;
import com.campushelp.repository.LugarRepository;
import com.campushelp.repository.UsuarioRepository;
import com.campushelp.service.EvidenciaService;
import com.campushelp.service.GeneradorCodigoService;
import com.campushelp.service.NotificacionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Datos de demostracion: lugares del campus con su posicion en el mapa, usuarios de los tres roles e
 * incidencias en todos los estados. Solo se cargan si la base esta vacia (app.datos-demo=false los desactiva).
 * Las incidencias se crean con las mismas reglas de la entidad y luego se ubican en los ultimos dias.
 */
@Component
@ConditionalOnProperty(name = "app.datos-demo", havingValue = "true", matchIfMissing = true)
public class DatosDemoInicializador implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DatosDemoInicializador.class);

    private final UsuarioRepository usuarios;
    private final LugarRepository lugares;
    private final IncidenciaRepository incidencias;
    private final GeneradorCodigoService codigos;
    private final EvidenciaService evidencias;
    private final NotificacionService notificaciones;

    public DatosDemoInicializador(UsuarioRepository usuarios, LugarRepository lugares, IncidenciaRepository incidencias,
                                  GeneradorCodigoService codigos, EvidenciaService evidencias,
                                  NotificacionService notificaciones) {
        this.usuarios = usuarios;
        this.lugares = lugares;
        this.incidencias = incidencias;
        this.codigos = codigos;
        this.evidencias = evidencias;
        this.notificaciones = notificaciones;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (usuarios.count() > 0 || lugares.count() > 0 || incidencias.count() > 0) return;

        Usuario admin = usuario("Patricia Morales", "patricia.morales@campus.edu.co", "ADM-001", Rol.ADMINISTRADOR, null);
        Usuario valentina = usuario("Valentina Ruiz", "valentina.ruiz@campus.edu.co", "2023101", Rol.ESTUDIANTE, null);
        Usuario andres = usuario("Andrés Gómez", "andres.gomez@campus.edu.co", "2022145", Rol.ESTUDIANTE, null);
        Usuario camila = usuario("Camila Torres", "camila.torres@campus.edu.co", "2024033", Rol.ESTUDIANTE, null);
        Usuario julian = usuario("Julián Ortega", "julian.ortega@campus.edu.co", "MNT-01", Rol.MANTENIMIENTO, TipoProblema.ELECTRICO);
        Usuario marta = usuario("Marta Silva", "marta.silva@campus.edu.co", "MNT-02", Rol.MANTENIMIENTO, TipoProblema.MOBILIARIO);
        Usuario luis = usuario("Luis Peña", "luis.pena@campus.edu.co", "MNT-03", Rol.MANTENIMIENTO, TipoProblema.TECNOLOGIA);
        Usuario rosa = usuario("Rosa Díaz", "rosa.diaz@campus.edu.co", "MNT-04", Rol.MANTENIMIENTO, TipoProblema.SANITARIO);

        Lugar a101 = lugar("A-101", "Aula 101", "Bloque A", 1, 18, 30);
        Lugar a205 = lugar("A-205", "Aula 205", "Bloque A", 2, 22, 22);
        Lugar banoA = lugar("A-BANOS-1", "Baños Bloque A, piso 1", "Bloque A", 1, 27, 38);
        Lugar labSis = lugar("B-LAB-SIS", "Laboratorio de Sistemas", "Bloque B", 2, 47, 27);
        Lugar b301 = lugar("B-301", "Aula 301", "Bloque B", 3, 53, 20);
        Lugar biblio = lugar("BIB-SALA", "Biblioteca, sala de estudio", "Biblioteca", 1, 76, 28);
        Lugar cafe = lugar("CAF", "Cafetería central", "Cafetería", 1, 60, 55);
        Lugar audi = lugar("AUD", "Auditorio principal", "Auditorio", 1, 34, 63);
        Lugar cancha = lugar("CANCHA", "Cancha múltiple", "Zona deportiva", 0, 83, 74);
        Lugar parq = lugar("PARQ", "Parqueadero", "Parqueadero", 0, 14, 82);
        lugar("ADM-BANOS", "Baños Edificio Administrativo", "Administrativo", 1, 89, 45);

        Incidencia lampara = caso(valentina, a101, TipoProblema.ELECTRICO, Prioridad.MEDIA,
                "La lámpara del fondo del aula está apagada y parpadea al encenderla.", 9, true)
                .revisar(admin, 60).asignar(admin, julian, 90).reparar(julian, "Se revisa el balasto", 600)
                .solucionar(julian, "Se cambió el tubo LED y el balasto.", 1500).guardar("Solucionado: se cambió el tubo LED y el balasto.");
        caso(andres, a101, TipoProblema.ELECTRICO, Prioridad.MEDIA, "Lámpara dañada en el aula 101.", 8, false)
                .revisar(admin, 30).rechazar(admin, "Duplicado de " + lampara.getCodigo() + ", que ya está en atención.", 45)
                .guardar("Tu reporte se cerró: es un duplicado.");
        caso(camila, labSis, TipoProblema.TECNOLOGIA, Prioridad.ALTA,
                "El proyector no enciende; la luz del equipo queda en rojo.", 3, true)
                .revisar(admin, 20).asignar(admin, luis, 40).reparar(luis, "Se pidió una lámpara de repuesto", 300)
                .guardar("Está en reparación.");
        caso(valentina, banoA, TipoProblema.SANITARIO, Prioridad.URGENTE,
                "Hay una fuga de agua en el lavamanos y el piso está inundado.", 1, true)
                .revisar(admin, 10).asignar(admin, rosa, 15).reparar(rosa, "Se cerró la llave de paso", 40)
                .guardar("Está en reparación.");
        caso(andres, banoA, TipoProblema.SANITARIO, Prioridad.ALTA, "El inodoro del segundo cubículo está tapado.", 0, false)
                .guardar("Recibimos tu reporte. Está pendiente de revisión.");
        caso(camila, a205, TipoProblema.MOBILIARIO, Prioridad.BAJA, "Silla con el espaldar suelto en la fila 3.", 2, false)
                .revisar(admin, 120).asignar(admin, marta, 150).guardar("Tu reporte fue asignado a Marta Silva.");
        caso(andres, cancha, TipoProblema.ELECTRICO, Prioridad.URGENTE,
                "Hay un cable eléctrico expuesto junto a la tribuna.", 0, false)
                .revisar(admin, 5).asignar(admin, julian, 10).guardar("Tu reporte fue asignado a Julián Ortega.");
        caso(valentina, b301, TipoProblema.TECNOLOGIA, Prioridad.MEDIA, "El proyector se ve borroso y no se puede leer.", 12, false)
                .revisar(admin, 60).asignar(admin, luis, 80).reparar(luis, null, 500)
                .solucionar(luis, "Se limpió el lente y se ajustó el enfoque.", 700).guardar("Solucionado: se limpió el lente.");
        caso(camila, biblio, TipoProblema.TECNOLOGIA, Prioridad.MEDIA, "El wifi de la sala de estudio es muy lento.", 1, false)
                .revisar(admin, 90).guardar("El administrador está revisando tu reporte.");
        caso(andres, cafe, TipoProblema.MOBILIARIO, Prioridad.BAJA, "Una mesa está coja y se mueve mucho.", 15, false)
                .revisar(admin, 200).asignar(admin, marta, 240).reparar(marta, null, 1400)
                .solucionar(marta, "Se niveló la pata de la mesa.", 1500).guardar("Solucionado: se niveló la mesa.");
        caso(valentina, audi, TipoProblema.INFRAESTRUCTURA, Prioridad.ALTA, "Hay una gotera sobre la fila 5 cuando llueve.", 4, false)
                .revisar(admin, 100).asignar(admin, marta, 130).guardar("Tu reporte fue asignado a Marta Silva.");
        caso(camila, parq, TipoProblema.LIMPIEZA, Prioridad.MEDIA, "Hay basura acumulada en la entrada del parqueadero.", 6, false)
                .revisar(admin, 60).asignar(admin, rosa, 70).reparar(rosa, null, 200)
                .solucionar(rosa, "Se recogió la basura y se instaló una caneca.", 300).guardar("Solucionado.");
        caso(andres, biblio, TipoProblema.ELECTRICO, Prioridad.BAJA, "Una luz del pasillo de la biblioteca parpadea.", 20, false)
                .revisar(admin, 300).asignar(admin, julian, 360).reparar(julian, null, 2000)
                .solucionar(julian, "Se cambió la bombilla.", 2100).guardar("Solucionado: se cambió la bombilla.");

        log.info("Datos de demostración de CampusHelp: {} usuarios, {} lugares y {} incidencias",
                usuarios.count(), lugares.count(), incidencias.count());
    }

    private Usuario usuario(String nombre, String email, String codigo, Rol rol, TipoProblema especialidad) {
        Usuario u = new Usuario();
        u.setNombre(nombre);
        u.setEmail(email);
        u.setCodigo(codigo);
        u.setRol(rol);
        u.setEspecialidad(especialidad);
        return usuarios.save(u);
    }

    private Lugar lugar(String codigo, String nombre, String edificio, int piso, double x, double y) {
        Lugar l = new Lugar();
        l.setCodigo(codigo);
        l.setNombre(nombre);
        l.setEdificio(edificio);
        l.setPiso(piso);
        l.setX(x);
        l.setY(y);
        return lugares.save(l);
    }

    private Caso caso(Usuario estudiante, Lugar lugar, TipoProblema tipo, Prioridad prioridad, String descripcion,
                      int haceDias, boolean conFoto) {
        Instant inicio = Instant.now().minus(Duration.ofDays(haceDias)).minus(Duration.ofHours(2)).truncatedTo(ChronoUnit.MINUTES);
        return new Caso(estudiante, lugar, tipo, prioridad, descripcion, inicio, conFoto);
    }

    /** Recrea la vida de una incidencia con fechas simuladas (minutos desde el reporte). */
    private final class Caso {
        private final Incidencia inc = new Incidencia();
        private final Instant inicio;

        Caso(Usuario estudiante, Lugar lugar, TipoProblema tipo, Prioridad prioridad, String descripcion,
             Instant inicio, boolean conFoto) {
            this.inicio = inicio;
            inc.setLugarId(lugar.getId());
            inc.setTipo(tipo);
            inc.setPrioridad(prioridad);
            inc.setDescripcion(descripcion);
            inc.registrar(codigos.siguienteCodigo(), estudiante);
            inc.setFechaReporte(inicio);
            if (conFoto) {
                Evidencia e = fotoDeEjemplo(lugar.getNombre(), descripcion, estudiante.getId());
                if (e != null) inc.agregarEvidencia(e, estudiante);
            }
            fechar(0, 1);
        }

        Caso revisar(Usuario admin, long min) {
            int n = inc.getSeguimiento().size();
            inc.iniciarRevision(admin);
            inc.setFechaRevision(t(min));
            fechar(n, min);
            return this;
        }

        Caso asignar(Usuario admin, Usuario responsable, long min) {
            int n = inc.getSeguimiento().size();
            inc.validarYAsignar(admin, responsable, null);
            inc.setFechaAsignacion(t(min));
            fechar(n, min);
            return this;
        }

        Caso rechazar(Usuario admin, String motivo, long min) {
            int n = inc.getSeguimiento().size();
            inc.rechazar(admin, motivo);
            inc.setFechaCierre(t(min));
            fechar(n, min);
            return this;
        }

        Caso reparar(Usuario m, String nota, long min) {
            int n = inc.getSeguimiento().size();
            inc.iniciarReparacion(m, nota);
            inc.setFechaInicioReparacion(t(min));
            fechar(n, min);
            return this;
        }

        Caso solucionar(Usuario m, String solucion, long min) {
            int n = inc.getSeguimiento().size();
            inc.solucionar(m, solucion);
            inc.setFechaSolucion(t(min));
            fechar(n, min);
            return this;
        }

        Incidencia guardar(String ultimoAviso) {
            Incidencia g = incidencias.save(inc);
            notificaciones.notificarReportante(g, ultimoAviso);
            return g;
        }

        private Instant t(long min) {
            Instant f = inicio.plus(Duration.ofMinutes(min));
            return f.isAfter(Instant.now()) ? Instant.now() : f;
        }

        private void fechar(int desde, long min) {
            List<Actualizacion> nuevos = inc.getSeguimiento().subList(desde, inc.getSeguimiento().size());
            for (Actualizacion a : nuevos) a.setFecha(t(min));
        }
    }

    /** Genera una imagen PNG sencilla que hace de "foto" de demostracion. */
    private Evidencia fotoDeEjemplo(String lugar, String descripcion, String autorId) {
        try {
            BufferedImage img = new BufferedImage(640, 400, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = img.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(new Color(0xE9EEF2));
            g.fillRect(0, 0, 640, 400);
            g.setColor(new Color(0x1F3A5F));
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 30));
            g.drawString("Foto de evidencia", 30, 70);
            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 22));
            g.drawString(lugar, 30, 120);
            g.setColor(new Color(0x444444));
            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 18));
            g.drawString(descripcion.length() > 60 ? descripcion.substring(0, 60) + "..." : descripcion, 30, 170);
            g.dispose();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(img, "png", out);
            byte[] bytes = out.toByteArray();
            return evidencias.guardar(new ByteArrayInputStream(bytes), "evidencia.png", "image/png", bytes.length, autorId);
        } catch (IOException | RuntimeException e) {
            log.warn("No se pudo generar la foto de demostración: {}", e.getMessage());
            return null;
        }
    }
}
