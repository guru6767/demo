package com.starto.service;

import com.starto.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    public void sendExpiryReminder(User user, int daysLeft) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setTo(user.getEmail());
            helper.setFrom("starto@gmail.com");
            helper.setSubject("Your Starto plan expires in " + daysLeft + " day(s)");
            helper.setText(buildEmailBody(user, daysLeft), true); // true = HTML

            mailSender.send(message);
            log.info("Expiry reminder sent to {}", user.getEmail());

        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", user.getEmail(), e.getMessage());
        }
    }

    private String buildEmailBody(User user, int daysLeft) {
        return """
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: auto;">
                    <h2 style="color: #f97316;">⚠️ Your plan is expiring soon!</h2>
                    <p>Hi <strong>%s</strong>,</p>
                    <p>Your <strong>%s</strong> plan on Starto expires in <strong>%d day(s)</strong>.</p>
                    <p>Upgrade now to keep access to all your features:</p>
                    <ul>
                        <li>Post signals</li>
                        <li>Unlock WhatsApp contacts</li>
                        <li>AI-powered features</li>
                    </ul>
                    <a href="https://starto.in/upgrade"
                       style="background:#f97316;color:white;padding:12px 24px;
                              border-radius:8px;text-decoration:none;display:inline-block;margin-top:16px;">
                        Upgrade Now
                    </a>
                    <p style="color:#888;margin-top:24px;font-size:12px;">
                        You're receiving this because you have an active plan on Starto.
                    </p>
                </div>
                """.formatted(user.getName(), user.getPlan().name(), daysLeft);
    }

    public void sendWelcomePlanEmail(User user) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setTo(user.getEmail());
            helper.setFrom("starto@gmail.com");
            helper.setSubject("Welcome to Starto " + user.getPlan().name() + " Plan! 🎉");
            helper.setText(buildWelcomeBody(user), true);

            mailSender.send(message);
            log.info("Welcome email sent to {}", user.getEmail());

        } catch (Exception e) {
            log.error("Failed to send welcome email: {}", e.getMessage());
        }
    }

    private String buildWelcomeBody(User user) {
        return """
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: auto;">
                    <h2 style="color: #f97316;">Welcome to %s Plan! 🎉</h2>
                    <p>Hi <strong>%s</strong>,</p>
                    <p>You have successfully subscribed to the <strong>%s</strong> plan on Starto.</p>
                    <p>You now have access to:</p>
                    %s
                    <a href="https://starto.in/dashboard"
                       style="background:#f97316;color:white;padding:12px 24px;
                              border-radius:8px;text-decoration:none;display:inline-block;margin-top:16px;">
                        Go to Dashboard
                    </a>
                    <p style="color:#888;margin-top:24px;font-size:12px;">
                        Thank you for choosing Starto!
                    </p>
                </div>
                """.formatted(
                user.getPlan().name(),
                user.getName(),
                user.getPlan().name(),
                getPlanFeatures(user.getPlan().name()));
    }

    private String getPlanFeatures(String plan) {
        return switch (plan) {
            case "SPRINT" ->
                "<ul><li>5 signals</li><li>20 offers</li><li>10 AI calls</li><li>WhatsApp unlock</li></ul>";
            case "BOOST" ->
                "<ul><li>8 signals</li><li>Unlimited offers</li><li>15 AI calls</li><li>WhatsApp unlock</li></ul>";
            case "PRO" ->
                "<ul><li>10 signals</li><li>Unlimited offers</li><li>20 AI calls</li><li>WhatsApp unlock</li></ul>";
            case "PRO_PLUS" ->
                "<ul><li>Unlimited signals</li><li>Unlimited offers</li><li>30 AI calls</li><li>WhatsApp unlock</li></ul>";
            case "GROWTH", "ANNUAL", "CAPTAIN_PRO" ->
                "<ul><li>Unlimited signals</li><li>Unlimited offers</li><li>Unlimited AI calls</li><li>WhatsApp unlock</li></ul>";
            case "CAPTAIN" ->
                "<ul><li>10 signals</li><li>Unlimited offers</li><li>20 AI calls</li><li>WhatsApp unlock</li></ul>";
            default -> "<ul><li>2 signals</li><li>3 offers</li><li>No AI calls</li></ul>";
        };
    }

    public void sendPaymentSuccessEmail(User user, String plan, int amountPaise, String orderId) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setTo(user.getEmail());
            helper.setFrom("starto@gmail.com");
            helper.setSubject("Payment Successful — Starto " + plan + " Plan Activated! 🎉");
            helper.setText(buildPaymentSuccessBody(user, plan, amountPaise, orderId), true);

            mailSender.send(message);
            log.info("Payment success email sent to {}", user.getEmail());

        } catch (Exception e) {
            log.error("EMAIL SENDING FAILED", e);
        }
    }

    private String buildPaymentSuccessBody(User user, String plan, int amountPaise, String orderId) {

        double amount = amountPaise / 100.0;

        return """
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: auto;">
                    <h2 style="color: #22c55e;">Payment Successful! ✅</h2>

                    <p>Hi <strong>%s</strong>,</p>
                    <p>Your payment has been received and your <strong>%s</strong> plan is now active.</p>

                    <div style="background:#f9f9f9;padding:16px;border-radius:8px;margin:16px 0;">
                        <h3 style="margin:0 0 12px 0;">Payment Receipt</h3>

                        <table style="width:100%%;border-collapse:collapse;">
                            <tr>
                                <td style="padding:8px 0;color:#666;">Plan</td>
                                <td style="padding:8px 0;font-weight:bold;">%s</td>
                            </tr>

                            <tr>
                                <td style="padding:8px 0;color:#666;">Amount Paid</td>
                                <td style="padding:8px 0;font-weight:bold;">₹%.2f</td>
                            </tr>

                            <tr>
                                <td style="padding:8px 0;color:#666;">Order ID</td>
                                <td style="padding:8px 0;font-size:12px;color:#888;">%s</td>
                            </tr>

                            <tr>
                                <td style="padding:8px 0;color:#666;">Date</td>
                                <td style="padding:8px 0;">%s</td>
                            </tr>
                        </table>
                    </div>

                    <a href="https://starto.in/dashboard"
                       style="background:#f97316;color:white;padding:12px 24px;
                              border-radius:8px;text-decoration:none;display:inline-block;margin-top:16px;">
                        Go to Dashboard
                    </a>

                    <p style="color:#888;margin-top:24px;font-size:12px;">
                        Keep this email as your payment receipt.
                        For support, contact us at support@starto.in
                    </p>
                </div>
                """.formatted(
                user.getName(),
                plan,
                plan,
                amount,
                orderId,
                java.time.LocalDate.now());
    }

    public void sendPlanUpgradeEmail(User user, String oldPlan, String newPlan) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setTo(user.getEmail());
            helper.setFrom("starto@gmail.com");
            helper.setSubject("Plan Upgraded to " + newPlan + "! 🚀");
            helper.setText(buildUpgradeBody(user, oldPlan, newPlan), true);

            mailSender.send(message);
            log.info("Upgrade email sent to {}", user.getEmail());

        } catch (Exception e) {
            log.error("Failed to send upgrade email: {}", e.getMessage());
        }
    }

    private String buildUpgradeBody(User user, String oldPlan, String newPlan) {
        return """
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: auto;">
                    <h2 style="color: #f97316;">Plan Upgraded! 🚀</h2>
                    <p>Hi <strong>%s</strong>,</p>
                    <p>You have successfully upgraded from <strong>%s</strong> to <strong>%s</strong> plan.</p>
                    <p>You now have access to all %s features!</p>
                    %s
                    <a href="https://starto.in/dashboard"
                       style="background:#f97316;color:white;padding:12px 24px;
                              border-radius:8px;text-decoration:none;display:inline-block;margin-top:16px;">
                        Explore New Features
                    </a>
                    <p style="color:#888;margin-top:24px;font-size:12px;">
                        Thank you for choosing Starto!
                    </p>
                </div>
                """.formatted(
                user.getName(),
                oldPlan,
                newPlan,
                newPlan,
                getPlanFeatures(newPlan));
    }

    public void sendPlanExpiredEmail(User user) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setTo(user.getEmail());
            helper.setFrom("starto@gmail.com");
            helper.setSubject("Your Starto Plan Has Expired ❌");
            helper.setText(buildExpiredBody(user), true); // HTML

            mailSender.send(message);
            log.info("Plan expired email sent to {}", user.getEmail());

        } catch (Exception e) {
            log.error("Failed to send expired email: {}", e.getMessage());
        }
    }

    private String buildExpiredBody(User user) {
        return """
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: auto;">
                    <h2 style="color: #ef4444;">Your Plan Has Expired ❌</h2>

                    <p>Hi <strong>%s</strong>,</p>

                    <p>Your <strong>%s</strong> plan has expired.</p>

                    <p>You have now been moved to the <strong>EXPLORER</strong> plan.</p>

                    <p>Upgrade now to continue enjoying premium features:</p>

                    <ul>
                        <li>More signals</li>
                        <li>Unlimited offers</li>
                        <li>AI-powered tools</li>
                        <li>WhatsApp unlock</li>
                    </ul>

                    <a href="https://starto.in/upgrade"
                       style="background:#ef4444;color:white;padding:12px 24px;
                              border-radius:8px;text-decoration:none;display:inline-block;margin-top:16px;">
                        Upgrade Now
                    </a>

                    <p style="color:#888;margin-top:24px;font-size:12px;">
                        We’d love to have you back on premium 🚀
                    </p>
                </div>
                """.formatted(user.getName(), user.getPlan().name());
    }

    public void sendVerificationEmail(User user) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setTo(user.getEmail());
            helper.setFrom("starto@gmail.com");
            helper.setSubject("Verify your email — Starto 🚀");
            helper.setText(buildVerificationBody(user), true);

            mailSender.send(message);
            log.info("Verification email sent to {}", user.getEmail());

        } catch (Exception e) {
            log.error("Failed to send verification email: {}", e.getMessage());
        }
    }

    private String buildVerificationBody(User user) {
        return """
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: auto; padding: 20px; border: 1px solid #eee; border-radius: 10px;">
                    <div style="text-align: center; margin-bottom: 20px;">
                        <h1 style="color: #f97316; margin: 0;">Starto</h1>
                    </div>
                    <h2 style="color: #333;">Welcome to the community! 🚀</h2>
                    <p>Hi <strong>%s</strong>,</p>
                    <p>Thank you for joining Starto. We're excited to have you on board!</p>
                    <p>To get started, please verify your email address by clicking the button below:</p>
                    
                    <div style="text-align: center; margin: 30px 0;">
                        <a href="https://starto.in/verify?email=%s" 
                           style="background: #f97316; color: white; padding: 14px 28px; 
                                  border-radius: 8px; text-decoration: none; font-weight: bold; display: inline-block;">
                            Verify Email Address
                        </a>
                    </div>
                    
                    <p style="color: #666; font-size: 14px;">If you didn't create an account, you can safely ignore this email.</p>
                    <hr style="border: none; border-top: 1px solid #eee; margin: 20px 0;">
                    <p style="color: #888; font-size: 12px; text-align: center;">
                        © 2026 Starto. All rights reserved.<br>
                        Fueling the Next Gen Entrepreneurs.
                    </p>
                </div>
                """.formatted(user.getName(), user.getEmail());
    }
}