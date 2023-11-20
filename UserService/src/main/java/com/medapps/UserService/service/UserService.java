package com.medapps.UserService.service;

import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.medapps.UserService.dao.UserRepository;
import com.medapps.UserService.entity.User;

@Service
public class UserService {
	@Autowired
	private UserRepository userRepository;

	@Autowired
	private JavaMailSender javaMailSender;

	public User registerUser(User user) {
		String otp = generateOtp();
		user.setOtp(otp);
		user.setActive(false);// set the account as inactive until the otp verification
		
		//Hash the password before saving
		BCryptPasswordEncoder passwordEncoder=new BCryptPasswordEncoder();
		String hashPassowrd=passwordEncoder.encode(user.getPassword());
		user.setPassword(hashPassowrd);
		
		
		return userRepository.save(user);

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
		return null;

	}

}
