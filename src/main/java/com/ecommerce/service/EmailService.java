package com.ecommerce.service;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;


@Service
public class EmailService {
	
	@Autowired
    private JavaMailSender javaMailSender;
	final String setFromEmail = "manapolimeras@gmail.com";
	
	private void sendEmail(String toEmail, String subject, String body) {
        MimeMessage message = javaMailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message);

            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(body,false);

            javaMailSender.send(message);
            System.out.println("Mail sent to : " + toEmail);
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send email", e);
        }
    }
	
	private void sendSimpleEmail(String toEmail, String subject, String body) {
        MimeMessage message = javaMailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, false);

            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(body);

            javaMailSender.send(message);
            System.out.println("Mail sent to : " + toEmail);
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send email", e);
        }
    }
	
	public void newRegister(String firstname, long id, String toEmail) {
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message,true);

            helper.setFrom("manapolimeras@gmail.com");
//            helper.setFrom(users.getEmail());
            helper.setTo(toEmail);
            helper.setSubject("Mana Polimeraas welcomes you Mr./Ms. " + firstname);

            helper.setText(
                    "Hii " + firstname + " this is yours id : " + id

            );
//            helper.addAttachment("polimeras Logo.png", new File("C:\\Users\\vadla vineeth kumar\\Downloads\\polimeras Logo.png"));
            javaMailSender.send(message);
            System.out.println("Mail sent to : " + toEmail);

        } catch (Exception e) {
            System.out.println("Mail Not sent");
            System.out.println(e.getMessage());
        }
    }

//	public void successOrder(String orderNumber, BigDecimal totalAmount, String email, String shippingAddress) {
//		
//		String subject = "Successfully Order Placed";
//		String body = "Your Order Number is : " + orderNumber + "/n" +
//				"Total Amount is : " + totalAmount + "/n" +
//				"Shipping Address : " + shippingAddress + "/n/n/n/n" + 
//				"From," +"/n"+
//				"Mana Polimeras.";
//		sendEmail(email, subject, body);
//		System.out.println("Mail Sent to "+email);
//	}

	public void sendSuccessEmail(String orderNumber , BigDecimal totalAmount , String email,String add) {
        MimeMessage message = javaMailSender.createMimeMessage();

        try {
            MimeMessageHelper helper = new MimeMessageHelper(message);
            String subject = "Successfully Order Placed";
            String body = "Your Order Number is : " + orderNumber + "\n" +
                    "Total Amount is : " + totalAmount + "\n" +
                    "Shipping Address : " + add + "\n\n\n\n" +
                    "From," +"\n"+
                    "Mana Polimeras.";
            helper.setSubject(subject);
            helper.setText(body);
            helper.setFrom(setFromEmail);
            helper.setTo(email);
            javaMailSender.send(message);
            System.out.println("Mail sent to : " + email );
        }catch (MessagingException e) {
            System.out.println(e.getMessage());
            throw new RuntimeException(e);
        }

    }
}
