package com.saas.billing.infrastructure;

/** Preserva o comportamento original, que montava o e-mail sem enviá-lo. */
public final class PreviewEmailSender implements EmailSender {
    @Override
    public void send(String recipient, String subject, String body) {
        // O envio real depende de um provedor de e-mail fornecido pela aplicação.
    }
}
