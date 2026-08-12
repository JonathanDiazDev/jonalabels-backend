package com.jonalabels.email.service;

import com.jonalabels.archivo.service.StorageService;
import com.jonalabels.pedido.domain.Cotizacion;
import jakarta.mail.MessagingException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final StorageService storageService;

    @Value("${app.admin.email}")
    private String adminEmail;

    @Value("${app.mail.from}")
    private String mailFrom;

    @Async
    public void enviarNotificacionNuevaCotizacion(Cotizacion cotizacion) {
        try {
            var message = mailSender.createMimeMessage();
            var helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(mailFrom, "JonaLabels Estudio");
            helper.setTo(adminEmail);
            helper.setSubject("Nueva cotización - " + cotizacion.getNombre());

            String urlDiseno = cotizacion.getUrlDiseno();
            String linkDiseno = (urlDiseno != null && !urlDiseno.isBlank())
                    ? "\nDiseño: " + urlDiseno : "";

            helper.setText(String.format("""
                    Se ha recibido una nueva solicitud de cotización:

                    Nombre / Marca: %s
                    WhatsApp: %s
                    Email: %s
                    Cantidad solicitada: %d
                    Medidas: %s%s
                    """,
                    cotizacion.getNombre(),
                    cotizacion.getWhatsapp(),
                    cotizacion.getEmail() != null ? cotizacion.getEmail() : "No proporcionado",
                    cotizacion.getCantidad(),
                    cotizacion.getMedidas() != null ? cotizacion.getMedidas() : "No especificado",
                    linkDiseno));

            if (urlDiseno != null && !urlDiseno.isBlank()) {
                adjuntarDiseno(helper, urlDiseno);
            }

            mailSender.send(message);
        } catch (MailException | MessagingException | UnsupportedEncodingException e) {
            throw new RuntimeException("Error al enviar notificación por correo", e);
        }
    }

    private void adjuntarDiseno(MimeMessageHelper helper, String urlDiseno) {
        String nombre = extraerNombreAdjunto(urlDiseno);

        try {
            if (urlDiseno.startsWith("http://") || urlDiseno.startsWith("https://")) {
                byte[] bytes = descargarDesdeUrl(urlDiseno);
                helper.addAttachment(nombre, new ByteArrayResource(bytes));
            } else {
                helper.addAttachment(nombre, storageService.cargarComoRecurso(urlDiseno));
            }
        } catch (IOException | MessagingException | RuntimeException e) {
            log.warn("No se pudo adjuntar el diseño {}: {}", urlDiseno, e.getMessage());
        }
    }

    private byte[] descargarDesdeUrl(String urlDiseno) throws IOException {
        try (InputStream in = new URL(urlDiseno).openStream();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            in.transferTo(out);
            return out.toByteArray();
        }
    }

    @Async
    public void enviarConfirmacionCliente(String correoCliente, String nombreCliente, String tipoProducto, int cantidad) {
        if (correoCliente == null || correoCliente.isBlank()) return;

        try {
            var message = mailSender.createMimeMessage();
            var helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(mailFrom, "JonaLabels Estudio");
            helper.setTo(correoCliente);
            helper.setSubject("Hemos recibido tu solicitud de cotización \uD83E\uDDF5");

            String producto = (tipoProducto != null && !tipoProducto.isBlank()) ? tipoProducto : "etiquetas personalizadas";
            String body = String.format("""
                    <p>Hola <strong>%s</strong>,</p>

                    <p>Gracias por contactarnos. Hemos recibido tu solicitud de cotización para <strong>%s</strong> con una cantidad de <strong>%s piezas</strong>.</p>

                    <p>Nuestro equipo ya está revisando los detalles y te contactaremos en las próximas 24 horas para brindarte una propuesta a tu medida.</p>

                    <p>Si tienes alguna pregunta adicional, no dudes en responder a este correo o escribirnos por WhatsApp.</p>

                    <br/>
                    <p>Saludos,<br/>
                    <strong>JonaLabels Estudio</strong><br/>
                    <em>La calidad de una prenda comienza por sus detalles</em></p>
                    """,
                    nombreCliente,
                    producto,
                    String.format("%,d", cantidad));

            helper.setText(body, true);
            mailSender.send(message);
        } catch (MailException | MessagingException | UnsupportedEncodingException e) {
            log.error("Error al enviar confirmación al cliente {}: {}", correoCliente, e.getMessage());
        }
    }

    private String extraerNombreAdjunto(String urlArchivo) {
        int idx = urlArchivo.lastIndexOf('.');
        return idx > 0 ? "diseno" + urlArchivo.substring(idx) : "diseno";
    }
}
