package com.starto.service;

import com.starto.model.User;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    private static final String FROM_EMAIL = "sagarghate164@gmail.com";

    // COMMON METHOD
    private void sendEmail(String to, String subject, String body) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(to);
            helper.setFrom(FROM_EMAIL);
            helper.setSubject(subject);
            helper.setText(body, true);

            mailSender.send(message);
            log.info(" Email sent successfully to {}", to);

        } catch (Exception e) {
            log.error(" EMAIL FAILED to {} | ERROR: {}", to, e.getMessage(), e);
        }
    }

    //  EXPIRY REMINDER 
    public void sendExpiryReminder(User user, int daysLeft) {
        String subject = "Your Starto plan expires in " + daysLeft + " day(s)";
        sendEmail(user.getEmail(), subject, buildExpiryBody(user, daysLeft));
    }

    private String buildExpiryBody(User user, int daysLeft) {
        return """
            <div style="font-family: Arial; max-width:600px; margin:auto;">
                <h2 style="color:#f97316;">⚠️ Plan Expiring Soon</h2>
                <p>Hi <b>%s</b>,</p>
                <p>Your <b>%s</b> plan expires in <b>%d day(s)</b>.</p>
                <ul>
                    <li>Signals</li>
                    <li>WhatsApp Unlock</li>
                    <li>AI Features</li>
                </ul>
                <a href="https://starto.in/upgrade"
                   style="background:#f97316;color:white;padding:10px 20px;border-radius:6px;text-decoration:none;">
                   Upgrade Now
                </a>
            </div>
            """.formatted(user.getName(), user.getPlan().name(), daysLeft);
    }

    // WELCOME 
    public void sendWelcomePlanEmail(User user) {
        String subject = "Welcome to Starto " + user.getPlan().name() + " 🎉";
        sendEmail(user.getEmail(), subject, buildWelcomeBody(user));
    }

    private String buildWelcomeBody(User user) {
        return """
            <div style="font-family: Arial; max-width:600px; margin:auto;">
                <h2 style="color:#f97316;">Welcome %s 🚀</h2>
                <p>Hi <b>%s</b>,</p>
                <p>You activated <b>%s</b> plan.</p>
                %s
                <a href="https://starto.in/dashboard"
                   style="background:#f97316;color:white;padding:10px 20px;border-radius:6px;text-decoration:none;">
                   Dashboard
                </a>
            </div>
            """.formatted(
                user.getPlan().name(),
                user.getName(),
                user.getPlan().name(),
                getPlanFeatures(user.getPlan().name())
        );
    }

    //  PAYMENT
    public void sendPaymentSuccessEmail(User user, String plan, int amountPaise, String orderId) {

        double amount = amountPaise / 100.0;

        String subject = "Payment Successful — " + plan + " Activated 🎉";

        String body = """
            <div style="font-family: Arial; max-width:600px; margin:auto;">
                <h2 style="color:#22c55e;">Payment Successful ✅</h2>
                <p>Hi <b>%s</b>,</p>

                <table style="width:100%%;">
                    <tr><td>Plan</td><td><b>%s</b></td></tr>
                    <tr><td>Amount</td><td><b>₹%.2f</b></td></tr>
                    <tr><td>Order ID</td><td>%s</td></tr>
                    <tr><td>Date</td><td>%s</td></tr>
                </table>

                <a href="https://starto.in/dashboard"
                   style="background:#f97316;color:white;padding:10px 20px;border-radius:6px;text-decoration:none;">
                   Dashboard
                </a>
            </div>
            """.formatted(user.getName(), plan, amount, orderId, LocalDate.now());

        sendEmail(user.getEmail(), subject, body);
    }

    //  UPGRADE 
    public void sendPlanUpgradeEmail(User user, String oldPlan, String newPlan) {

        String subject = "Upgraded to " + newPlan + " 🚀";

        String body = """
            <div style="font-family: Arial; max-width:600px; margin:auto;">
                <h2 style="color:#f97316;">Upgrade Successful 🚀</h2>
                <p>Hi <b>%s</b>,</p>
                <p>%s → <b>%s</b></p>
                %s
            </div>
            """.formatted(
                user.getName(),
                oldPlan,
                newPlan,
                getPlanFeatures(newPlan)
        );

        sendEmail(user.getEmail(), subject, body);
    }

    //  EXPIRED 
    public void sendPlanExpiredEmail(User user) {

        String subject = "Your Plan Expired ❌";

        String body = """
            <div style="font-family: Arial; max-width:600px; margin:auto;">
                <h2 style="color:#ef4444;">Plan Expired ❌</h2>
                <p>Hi <b>%s</b>,</p>
                <p>Your <b>%s</b> plan expired.</p>
                <p>Now on <b>EXPLORER</b> plan.</p>
                <a href="https://starto.in/upgrade"
                   style="background:#ef4444;color:white;padding:10px 20px;border-radius:6px;text-decoration:none;">
                   Upgrade
                </a>
            </div>
            """.formatted(user.getName(), user.getPlan().name());

        sendEmail(user.getEmail(), subject, body);
    }

    //  FEATURES 
    private String getPlanFeatures(String plan) {
        return switch (plan) {
            case "SPRINT" -> "<ul><li>5 signals</li><li>20 offers</li><li>10 AI calls</li></ul>";
            case "BOOST" -> "<ul><li>8 signals</li><li>Unlimited offers</li><li>15 AI calls</li></ul>";
            case "PRO" -> "<ul><li>10 signals</li><li>Unlimited offers</li><li>20 AI calls</li></ul>";
            case "PRO_PLUS" -> "<ul><li>Unlimited everything</li></ul>";
            default -> "<ul><li>Basic features</li></ul>";
        };
    }


    public void sendWelcomeEmail(User user) {

    String subject = "Welcome to Starto 🚀";

    String body = """
        <div style="font-family: Arial; max-width:600px; margin:auto;">
            <h2 style="color:#f97316;">Welcome to Starto 🚀</h2>

            <p>Hi <b>%s</b>,</p>

            <p>We’re excited to have you onboard 🎉</p>

            <p>
                Starto helps you connect, explore signals, unlock offers,
                and grow faster.
            </p>

            <ul>
                <li>Explore signals</li>
                <li>Unlock WhatsApp contacts</li>
                <li>Use AI-powered features</li>
            </ul>

            <p>
                You are currently on <b>EXPLORER</b> plan.
                Upgrade anytime to unlock more features.
            </p>

            <a href="https://starto.in/dashboard"
               style="background:#f97316;color:white;padding:10px 20px;border-radius:6px;text-decoration:none;">
               Go to Dashboard
            </a>

            <p style="margin-top:20px;">— Team Starto ❤️</p>
        </div>
    """.formatted(user.getName());

    sendEmail(user.getEmail(), subject, body);
}
}