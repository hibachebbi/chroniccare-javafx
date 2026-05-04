package com.chroniccare.services;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StripePaymentService {

    private static final Pattern PAYMENT_INTENT_ID_PATTERN = Pattern
            .compile("\\\"id\\\"\\s*:\\s*\\\"(pi_[^\\\"]+)\\\"");
    private static final Pattern STATUS_PATTERN = Pattern.compile("\\\"status\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"");
    private static final Pattern CHECKOUT_URL_PATTERN = Pattern.compile("\\\"url\\\"\\s*:\\s*\\\"(https:[^\\\"]+)\\\"");
    private static final Pattern CHECKOUT_SESSION_ID_PATTERN = Pattern
            .compile("\\\"id\\\"\\s*:\\s*\\\"(cs_[^\\\"]+)\\\"");
    private static final Pattern PAYMENT_STATUS_PATTERN = Pattern
            .compile("\\\"payment_status\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"");
    private static final Pattern PAYMENT_INTENT_FIELD_PATTERN = Pattern
            .compile("\\\"payment_intent\\\"\\s*:\\s*\\\"(pi_[^\\\"]+)\\\"");
    private static final Pattern PAYMENT_INTENT_OBJECT_ID_PATTERN = Pattern
            .compile("\\\"payment_intent\\\"\\s*:\\s*\\{[^}]*\\\"id\\\"\\s*:\\s*\\\"(pi_[^\\\"]+)\\\"");

    private static final String CHECKOUT_SUCCESS_URL_PREFIX = "https://chroniccare.local/stripe/success";
    private static final String CHECKOUT_CANCEL_URL_PREFIX = "https://chroniccare.local/stripe/cancel";

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public CheckoutSessionStartResult startCheckoutSession(double amount, String description) {
        try {
            String secretKey = System.getenv("STRIPE_SECRET_KEY");
            if (secretKey == null || secretKey.isBlank()) {
                return CheckoutSessionStartResult
                        .failed("Variable STRIPE_SECRET_KEY introuvable. Configurez votre cle secrete Stripe.");
            }

            long amountInCents = Math.round(amount * 100.0d);
            if (amountInCents <= 0) {
                return CheckoutSessionStartResult.failed("Montant invalide pour Stripe Checkout");
            }

            String currency = System.getenv("STRIPE_CURRENCY");
            if (currency == null || currency.isBlank()) {
                currency = "eur";
            }

            String safeDescription = description == null || description.isBlank()
                    ? "Commande ChronicCare"
                    : description.trim();

            String body = "mode=payment"
                    + "&success_url=" + encode(CHECKOUT_SUCCESS_URL_PREFIX + "?session_id={CHECKOUT_SESSION_ID}")
                    + "&cancel_url=" + encode(CHECKOUT_CANCEL_URL_PREFIX)
                    + "&line_items[0][quantity]=1"
                    + "&line_items[0][price_data][currency]=" + encode(currency.toLowerCase())
                    + "&line_items[0][price_data][unit_amount]=" + amountInCents
                    + "&line_items[0][price_data][product_data][name]=" + encode(safeDescription);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.stripe.com/v1/checkout/sessions"))
                    .timeout(Duration.ofSeconds(20))
                    .header("Authorization", "Bearer " + secretKey.trim())
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            String responseBody = response.body() == null ? "" : response.body();

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                String msg = extractErrorMessage(responseBody);
                return CheckoutSessionStartResult
                        .failed("Stripe Checkout error (" + response.statusCode() + "): " + msg);
            }

            String sessionId = extractValue(CHECKOUT_SESSION_ID_PATTERN, responseBody);
            String checkoutUrl = extractValue(CHECKOUT_URL_PATTERN, responseBody);
            if (sessionId == null || sessionId.isBlank() || checkoutUrl == null || checkoutUrl.isBlank()) {
                return CheckoutSessionStartResult.failed("Session Stripe invalide (url/session absente)");
            }

            return CheckoutSessionStartResult.success(sessionId, checkoutUrl, CHECKOUT_SUCCESS_URL_PREFIX,
                    CHECKOUT_CANCEL_URL_PREFIX);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return CheckoutSessionStartResult.failed("Creation session Stripe interrompue: " + e.getMessage());
        } catch (IOException e) {
            return CheckoutSessionStartResult.failed("Erreur creation session Stripe: " + e.getMessage());
        } catch (Exception e) {
            return CheckoutSessionStartResult.failed("Erreur Stripe Checkout: " + e.getMessage());
        }
    }

    public CheckoutSessionVerificationResult verifyCheckoutSession(String sessionId) {
        try {
            if (sessionId == null || sessionId.isBlank()) {
                return CheckoutSessionVerificationResult.failed("session_id Stripe manquant");
            }

            String secretKey = System.getenv("STRIPE_SECRET_KEY");
            if (secretKey == null || secretKey.isBlank()) {
                return CheckoutSessionVerificationResult.failed("Variable STRIPE_SECRET_KEY introuvable.");
            }

            String url = "https://api.stripe.com/v1/checkout/sessions/" + encodePathSegment(sessionId.trim())
                    + "?expand[]=payment_intent";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(20))
                    .header("Authorization", "Bearer " + secretKey.trim())
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            String responseBody = response.body() == null ? "" : response.body();

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                String msg = extractErrorMessage(responseBody);
                return CheckoutSessionVerificationResult
                        .failed("Verification Stripe impossible (" + response.statusCode() + "): " + msg);
            }

            String paymentStatus = extractValue(PAYMENT_STATUS_PATTERN, responseBody);
            String status = extractValue(STATUS_PATTERN, responseBody);
            String paymentIntentId = extractValue(PAYMENT_INTENT_OBJECT_ID_PATTERN, responseBody);
            if (paymentIntentId == null || paymentIntentId.isBlank()) {
                paymentIntentId = extractValue(PAYMENT_INTENT_FIELD_PATTERN, responseBody);
            }

            boolean paid = "paid".equalsIgnoreCase(paymentStatus)
                    || "complete".equalsIgnoreCase(status)
                    || "succeeded".equalsIgnoreCase(status);

            if (!paid) {
                return CheckoutSessionVerificationResult.failed("Paiement Stripe non confirme (status="
                        + (status == null ? "inconnu" : status)
                        + ", payment_status="
                        + (paymentStatus == null ? "inconnu" : paymentStatus)
                        + ")");
            }

            return CheckoutSessionVerificationResult.success(sessionId.trim(), paymentIntentId, status, paymentStatus);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return CheckoutSessionVerificationResult.failed("Verification Stripe interrompue: " + e.getMessage());
        } catch (IOException e) {
            return CheckoutSessionVerificationResult.failed("Erreur verification Stripe: " + e.getMessage());
        } catch (Exception e) {
            return CheckoutSessionVerificationResult.failed("Erreur Stripe: " + e.getMessage());
        }
    }

    public StripePaymentResult confirmTestPayment(double amount, String description) {
        try {
            String secretKey = System.getenv("STRIPE_SECRET_KEY");
            if (secretKey == null || secretKey.isBlank()) {
                return StripePaymentResult
                        .failed("Variable STRIPE_SECRET_KEY introuvable. Configurez votre cle secrete Stripe.");
            }

            long amountInCents = Math.round(amount * 100.0d);
            if (amountInCents <= 0) {
                return StripePaymentResult.failed("Montant invalide pour Stripe");
            }

            String currency = System.getenv("STRIPE_CURRENCY");
            if (currency == null || currency.isBlank()) {
                currency = "eur";
            }

            String safeDescription = description == null || description.isBlank()
                    ? "Commande ChronicCare"
                    : description.trim();

            String body = "amount=" + amountInCents
                    + "&currency=" + encode(currency.toLowerCase())
                    + "&confirm=true"
                    + "&payment_method=pm_card_visa"
                    + "&description=" + encode(safeDescription);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.stripe.com/v1/payment_intents"))
                    .timeout(Duration.ofSeconds(20))
                    .header("Authorization", "Bearer " + secretKey.trim())
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            String responseBody = response.body() == null ? "" : response.body();

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                String msg = extractErrorMessage(responseBody);
                return StripePaymentResult.failed("Stripe API error (" + response.statusCode() + "): " + msg);
            }

            String paymentIntentId = extractValue(PAYMENT_INTENT_ID_PATTERN, responseBody);
            String status = extractValue(STATUS_PATTERN, responseBody);

            if (paymentIntentId == null || paymentIntentId.isBlank()) {
                return StripePaymentResult.failed("Paiement Stripe cree sans identifiant exploitable.");
            }

            boolean paid = "succeeded".equalsIgnoreCase(status) || "requires_capture".equalsIgnoreCase(status);
            if (!paid) {
                return StripePaymentResult
                        .failed("Paiement Stripe non confirme (status=" + (status == null ? "inconnu" : status) + ")");
            }

            return StripePaymentResult.success(paymentIntentId, status);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return StripePaymentResult.failed("Appel Stripe interrompu: " + e.getMessage());
        } catch (IOException e) {
            return StripePaymentResult.failed("Erreur lors de l'appel Stripe: " + e.getMessage());
        } catch (Exception e) {
            return StripePaymentResult.failed("Erreur Stripe: " + e.getMessage());
        }
    }

    private String extractErrorMessage(String json) {
        if (json == null || json.isBlank()) {
            return "reponse vide";
        }
        Pattern messagePattern = Pattern.compile("\\\"message\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"");
        String message = extractValue(messagePattern, json);
        return message == null ? "inconnue" : message;
    }

    private String extractValue(Pattern pattern, String text) {
        if (text == null) {
            return null;
        }
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private String encodePathSegment(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    public static final class CheckoutSessionStartResult {
        private final boolean success;
        private final String sessionId;
        private final String checkoutUrl;
        private final String successUrlPrefix;
        private final String cancelUrlPrefix;
        private final String message;

        private CheckoutSessionStartResult(boolean success, String sessionId, String checkoutUrl,
                String successUrlPrefix, String cancelUrlPrefix, String message) {
            this.success = success;
            this.sessionId = sessionId;
            this.checkoutUrl = checkoutUrl;
            this.successUrlPrefix = successUrlPrefix;
            this.cancelUrlPrefix = cancelUrlPrefix;
            this.message = message;
        }

        public static CheckoutSessionStartResult success(String sessionId, String checkoutUrl,
                String successUrlPrefix, String cancelUrlPrefix) {
            return new CheckoutSessionStartResult(true, sessionId, checkoutUrl, successUrlPrefix, cancelUrlPrefix,
                    "OK");
        }

        public static CheckoutSessionStartResult failed(String message) {
            return new CheckoutSessionStartResult(false, null, null, null, null, message);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getSessionId() {
            return sessionId;
        }

        public String getCheckoutUrl() {
            return checkoutUrl;
        }

        public String getSuccessUrlPrefix() {
            return successUrlPrefix;
        }

        public String getCancelUrlPrefix() {
            return cancelUrlPrefix;
        }

        public String getMessage() {
            return message;
        }
    }

    public static final class CheckoutSessionVerificationResult {
        private final boolean success;
        private final String sessionId;
        private final String paymentIntentId;
        private final String status;
        private final String paymentStatus;
        private final String message;

        private CheckoutSessionVerificationResult(boolean success, String sessionId, String paymentIntentId,
                String status, String paymentStatus, String message) {
            this.success = success;
            this.sessionId = sessionId;
            this.paymentIntentId = paymentIntentId;
            this.status = status;
            this.paymentStatus = paymentStatus;
            this.message = message;
        }

        public static CheckoutSessionVerificationResult success(String sessionId, String paymentIntentId,
                String status, String paymentStatus) {
            return new CheckoutSessionVerificationResult(true, sessionId, paymentIntentId, status, paymentStatus, "OK");
        }

        public static CheckoutSessionVerificationResult failed(String message) {
            return new CheckoutSessionVerificationResult(false, null, null, null, null, message);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getSessionId() {
            return sessionId;
        }

        public String getPaymentIntentId() {
            return paymentIntentId;
        }

        public String getStatus() {
            return status;
        }

        public String getPaymentStatus() {
            return paymentStatus;
        }

        public String getMessage() {
            return message;
        }
    }

    public static final class StripePaymentResult {
        private final boolean success;
        private final String paymentIntentId;
        private final String status;
        private final String message;

        private StripePaymentResult(boolean success, String paymentIntentId, String status, String message) {
            this.success = success;
            this.paymentIntentId = paymentIntentId;
            this.status = status;
            this.message = message;
        }

        public static StripePaymentResult success(String paymentIntentId, String status) {
            return new StripePaymentResult(true, paymentIntentId, status, "OK");
        }

        public static StripePaymentResult failed(String message) {
            return new StripePaymentResult(false, null, null, message);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getPaymentIntentId() {
            return paymentIntentId;
        }

        public String getStatus() {
            return status;
        }

        public String getMessage() {
            return message;
        }
    }
}
