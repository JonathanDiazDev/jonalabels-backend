package com.jonalabels.email.service;

import com.jonalabels.archivo.service.StorageService;
import com.jonalabels.pedido.domain.Cotizacion;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final StorageService storageService;

    @Value("${app.admin.email}")
    private String adminEmail;

    @Async
    public void enviarNotificacionNuevaCotizacion(Cotizacion cotizacion) {
        try {
            var message = mailSender.createMimeMessage();
            var helper = new MimeMessageHelper(message, true);

            helper.setTo(adminEmail);
            helper.setSubject("Nueva cotización - " + cotizacion.getNombre());
            helper.setText(String.format("""
                    Se ha recibido una nueva solicitud de cotización:

                    Nombre / Marca: %s
                    WhatsApp: %s
                    Email: %s
                    Cantidad solicitada: %d
                    Medidas: %s
                    """,
                    cotizacion.getNombre(),
                    cotizacion.getWhatsapp(),
                    cotizacion.getEmail() != null ? cotizacion.getEmail() : "No proporcionado",
                    cotizacion.getCantidad(),
                    cotizacion.getMedidas() != null ? cotizacion.getMedidas() : "No especificado"));

            if (cotizacion.getUrlArchivo() != null && !cotizacion.getUrlArchivo().isBlank()) {
                var recurso = storageService.cargarComoRecurso(cotizacion.getUrlArchivo());
                var nombre = extraerNombreAdjunto(cotizacion.getUrlArchivo());
                helper.addAttachment(nombre, recurso);
            }

            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Error al enviar notificación por correo", e);
        }
    }

    private String extraerNombreAdjunto(String urlArchivo) {
        int idx = urlArchivo.lastIndexOf('.');
        return idx > 0 ? "diseno" + urlArchivo.substring(idx) : "diseno";
    }
}
