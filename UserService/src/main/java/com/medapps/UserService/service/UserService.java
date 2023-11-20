package com.medapps.UserService.service;

import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.medapps.UserService.dao.UserRepository;
import com.medapps.UserService.entity.User;
import com.medapps.UserService.exception.UserLoginException;
import com.medapps.UserService.exception.UserRegistrationException;

@Service
public class UserService {
	
	private static final Logger logger=LoggerFactory.getLogger(UserService.class);
	
	@Autowired
	private UserRepository userRepository;

	@Autowired
	private JavaMailSender javaMailSender;

	public User registerUser(User user) {
		try {
		String otp = generateOtp();
		user.setOtp(otp);
		user.setActive(false);// set the account as inactive until the otp verification
		
		//Hash the password before saving
		BCryptPasswordEncoder passwordEncoder=new BCryptPasswordEncoder();
		String hashPassowrd=passwordEncoder.encode(user.getPassword());
		user.setPassword(hashPassowrd);
		return userRepository.save(user);
		}catch (Exception e) {
			logger.error("User Registration Failed "+e.getMessage());
			//performing nessesary action to notify the Admin
			sendAdminNotificationAboutRegistrationFailure(user,e);
			throw new UserRegistrationException("User Registration Failed "+e.getMessage());
		}

	}

	private void sendAdminNotificationAboutRegistrationFailure(User user, Exception e) {
		
		try {
		SimpleMailMessage message =new SimpleMailMessage();
		message.setTo("admin@example.com");
		message.setSubject("User Registration Failure");
		message.setText("User Registration fail for "+user.getUsername() +" \nError "+e.getMessage());
		javaMailSender.send(message);
		}catch (MailException mailException) {
			logger.error("Failed to send admin notification: "+mailException.getMessage());
		}
		
	}

	public String generateOtp() {
		Random random = new Random();
		int otpNumber = 100_000 + random.nextInt(90_000);
		return String.valueOf(otpNumber);

	}

	public void sendOtpByEmail(User user) {
		SimpleMailMessage simpleMailMessage = new SimpleMailMessage();
		simpleMailMessage.setTo(user.getEmail());
		simpleMailMessage.setSubject("Your OTP for account verification");
		simpleMailMessage.setText("Your OTP is " + user.getOtp());
		javaMailSender.send(simpleMailMessage);

	}

	// method to verify otp
	public boolean verifyOTP(Long userId, String otp) {

		User user = userRepository.findById(userId).orElse(null);
		if (user != null && user.getOtp().equals(otp)) {
			user.setActive(true);
			userRepository.save(user);
			return true;
		}
		return false;

	}

	// method for login
	public User userLogin(String username, String password) {

		User user = userRepository.findByUsername(username);
		if (user != null) {
			BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();
			if (bCryptPasswordEncoder.matches(password, user.getPassword()) && user.isActive()) {
				return user;
			}

		}
		throw new UserLoginException("Invalid credentials !!!!");

	}

}
