package com.jonalabels.email.service;

import com.jonalabels.archivo.service.StorageService;
import com.jonalabels.pedido.domain.Cotizacion;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private StorageService storageService;

    @InjectMocks
    private EmailService emailService;

    private MimeMessage mimeMessage;

    @BeforeEach
    void setUp() throws Exception {
        var session = jakarta.mail.Session.getInstance(new Properties());
        mimeMessage = new MimeMessage(session);

        injectField("mailFrom", "no-reply@jonalabels.com");
        injectField("adminEmail", "admin@jonalabels.com");
    }

    private void injectField(String fieldName, String value) throws Exception {
        var field = EmailService.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(emailService, value);
    }

    private MimeMessage stubCreateMimeMessage() throws Exception {
        var msg = new MimeMessage(jakarta.mail.Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(msg);
        return msg;
    }

    // ── enviarConfirmacionCliente ──────────────────────────────────────

    @Test
    void enviarConfirmacionCliente_exito_enviaCorreo() throws Exception {
        stubCreateMimeMessage();

        emailService.enviarConfirmacionCliente(
                "cliente@test.com",
                "María López",
                "Etiquetas de Satén Premium",
                10000);

        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void enviarConfirmacionCliente_exito_configuraDestinatarioCorrecto() throws Exception {
        stubCreateMimeMessage();

        emailService.enviarConfirmacionCliente(
                "cliente@test.com",
                "María López",
                "Cartón Colgante",
                5000);

        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void enviarConfirmacionCliente_exito_asuntoContieneEmoji() throws Exception {
        var sent = stubCreateMimeMessage();

        emailService.enviarConfirmacionCliente(
                "cliente@test.com",
                "Ana García",
                "Etiquetas Económicas",
                15000);

        verify(mailSender).send(any(MimeMessage.class));
        assertThat(sent.getSubject()).contains("\uD83E\uDDF5");
    }

    @Test
    void enviarConfirmacionCliente_correoNulo_noEnviaCorreo() {
        emailService.enviarConfirmacionCliente(
                null,
                "María López",
                "Etiquetas de Satén Premium",
                10000);

        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void enviarConfirmacionCliente_correoEnBlanco_noEnviaCorreo() {
        emailService.enviarConfirmacionCliente(
                "  ",
                "María López",
                "Etiquetas de Satén Premium",
                10000);

        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void enviarConfirmacionCliente_excepcionMail_noPropagaExcepcion() throws Exception {
        stubCreateMimeMessage();

        doThrow(new MailSendException("SMTP server unavailable"))
                .when(mailSender).send(any(MimeMessage.class));

        emailService.enviarConfirmacionCliente(
                "cliente@test.com",
                "María López",
                "Etiquetas de Satén Premium",
                10000);

        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void enviarConfirmacionCliente_tipoProductoNulo_usaFallback() throws Exception {
        stubCreateMimeMessage();

        emailService.enviarConfirmacionCliente(
                "cliente@test.com",
                "Pedro Ruiz",
                null,
                7000);

        verify(mailSender).send(any(MimeMessage.class));
    }

    // ── enviarNotificacionNuevaCotizacion ──────────────────────────────

    @Test
    void enviarNotificacionNuevaCotizacion_exito_enviaCorreoAlAdmin() throws Exception {
        stubCreateMimeMessage();

        var cotizacion = Cotizacion.builder()
                .id(1L)
                .nombre("Test Marca")
                .whatsapp("5512345678")
                .email("test@ejemplo.com")
                .cantidad(10000)
                .tipoProducto("Etiquetas Premium")
                .medidas("5cm x 3cm")
                .build();

        emailService.enviarNotificacionNuevaCotizacion(cotizacion);

        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void enviarNotificacionNuevaCotizacion_excepcionMail_propagaExcepcion() throws Exception {
        stubCreateMimeMessage();

        var cotizacion = Cotizacion.builder()
                .id(2L)
                .nombre("Otra Marca")
                .whatsapp("5598765432")
                .email("otra@ejemplo.com")
                .cantidad(5000)
                .build();

        doThrow(new MailSendException("Connection refused"))
                .when(mailSender).send(any(MimeMessage.class));

        assertThatThrownBy(() -> emailService.enviarNotificacionNuevaCotizacion(cotizacion))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Error al enviar notificación por correo");
    }
}
