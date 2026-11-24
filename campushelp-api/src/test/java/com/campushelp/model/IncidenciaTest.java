package com.campushelp.model;

import com.campushelp.exception.ReglaNegocioException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Pruebas unitarias del flujo de una incidencia (sin base de datos). */
class IncidenciaTest {

    private Usuario estudiante;
    private Usuario admin;
    private Usuario mantenimiento;
    private Usuario otroMantenimiento;
    private Incidencia inc;

    private static Usuario usuario(String id, String nombre, Rol rol) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setNombre(nombre);
        u.setRol(rol);
        return u;
    }

    @BeforeEach
    void preparar() {
        estudiante = usuario("e1", "Valentina", Rol.ESTUDIANTE);
        admin = usuario("a1", "Patricia", Rol.ADMINISTRADOR);
        mantenimiento = usuario("m1", "Julián", Rol.MANTENIMIENTO);
        otroMantenimiento = usuario("m2", "Marta", Rol.MANTENIMIENTO);
        inc = new Incidencia();
        inc.setPrioridad(Prioridad.MEDIA);
        inc.registrar("CH-000001", estudiante);
    }

    @Test
    void flujoCompletoHastaSolucionada() {
        inc.iniciarRevision(admin);
        assertThat(inc.getEstado()).isEqualTo(EstadoIncidencia.EN_REVISION);
        assertThat(inc.colorMapa()).isEqualTo(ColorMapa.AMARILLO);

        inc.validarYAsignar(admin, mantenimiento, Prioridad.URGENTE);
        assertThat(inc.getEstado()).isEqualTo(EstadoIncidencia.ASIGNADA);
        assertThat(inc.getPrioridad()).isEqualTo(Prioridad.URGENTE);
        assertThat(inc.colorMapa()).isEqualTo(ColorMapa.ROJO);

        inc.iniciarReparacion(mantenimiento, null);
        inc.registrarProgreso(mantenimiento, "Se cambió el cable");
        inc.solucionar(mantenimiento, "Reparado");
        assertThat(inc.getEstado()).isEqualTo(EstadoIncidencia.SOLUCIONADA);
        assertThat(inc.colorMapa()).isEqualTo(ColorMapa.VERDE);
        assertThat(inc.horasHastaSolucion()).isNotNull();
        // el responsable no puede reabrirla; el administrador si
        assertThatThrownBy(() -> inc.iniciarReparacion(mantenimiento, null)).isInstanceOf(ReglaNegocioException.class);
        inc.cambiarEstadoManual(admin, EstadoIncidencia.EN_REPARACION, "La lámpara volvió a fallar");
        assertThat(inc.getEstado()).isEqualTo(EstadoIncidencia.EN_REPARACION);
        assertThat(inc.getSolucion()).isNull();
        assertThat(inc.getSeguimiento()).extracting(Actualizacion::getTipo).contains(
                TipoActualizacion.REGISTRO, TipoActualizacion.REVISION, TipoActualizacion.VALIDACION,
                TipoActualizacion.ASIGNACION, TipoActualizacion.INICIO_REPARACION, TipoActualizacion.PROGRESO,
                TipoActualizacion.SOLUCION);
    }

    @Test
    void reporteNoValidoSeCierraConMotivo() {
        inc.iniciarRevision(admin);
        assertThatThrownBy(() -> inc.rechazar(admin, " ")).isInstanceOf(ReglaNegocioException.class);
        inc.rechazar(admin, "Duplicado");
        assertThat(inc.getEstado()).isEqualTo(EstadoIncidencia.CERRADA);
        assertThat(inc.getMotivoCierre()).isEqualTo("Duplicado");
        assertThatThrownBy(() -> inc.agregarInformacion(estudiante, "más datos")).isInstanceOf(ReglaNegocioException.class);
    }

    @Test
    void soloElResponsableAsignadoPuedeRepararYSolucionar() {
        inc.validarYAsignar(admin, mantenimiento, null);
        assertThatThrownBy(() -> inc.iniciarReparacion(otroMantenimiento, null))
                .isInstanceOf(ReglaNegocioException.class).hasMessageContaining("no está asignada a Marta");
        inc.iniciarReparacion(mantenimiento, null);
        assertThatThrownBy(() -> inc.solucionar(mantenimiento, "")).isInstanceOf(ReglaNegocioException.class);
    }

    @Test
    void noSePuedeAsignarAQuienNoEsMantenimiento() {
        assertThatThrownBy(() -> inc.validarYAsignar(admin, estudiante, null))
                .isInstanceOf(ReglaNegocioException.class).hasMessageContaining("no es personal de mantenimiento");
    }

    @Test
    void transicionNoPermitidaEsRechazada() {
        assertThatThrownBy(() -> inc.solucionar(mantenimiento, "x")).isInstanceOf(ReglaNegocioException.class);
        assertThatThrownBy(() -> inc.cambiarEstadoManual(admin, EstadoIncidencia.EN_REPARACION, null))
                .isInstanceOf(ReglaNegocioException.class).hasMessageContaining("No se puede pasar de REPORTADA a EN_REPARACION");
    }
}
