package egovframework.kevinlab.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
//import org.springframework.security.authentication.encoding.PasswordEncoder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/auth")
public class SignController {
	private static final Logger logger = LoggerFactory.getLogger(SignController.class);

	//@Autowired
	//private PasswordEncoder passwordEncoder;
	@RequestMapping(value="/signin", method = RequestMethod.GET)
	public String signin(@RequestParam(value="error", required=false) String error, Model model) {
//    	 logger.info("SIGN page move!!");

         // Sha 암호값을 보기 위한 테스트용.
    	 //String guest_password = passwordEncoder.encodePassword("guest", null);
    	 //String admin_password = passwordEncoder.encodePassword("admin", null);
    	 //logger.info(guest_password + "//" + admin_password);

    	 model.addAttribute("error", error);

    	 return "signin";
	}

	@RequestMapping(value="/signout", method = RequestMethod.GET)
	public String signout(@RequestParam(value="error", required=false) String error, Model model) {
		logger.info("SIGN OUT");
		return "signin";
	}

	@PreAuthorize("authenticated")
	@RequestMapping(value="/mypage", method = RequestMethod.GET)
	public String mypage(Model model) {
		logger.info("mypage 이동");

		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		model.addAttribute("user_name", auth.getName());

		return "mypage";
	}

	@RequestMapping(value="/login_duplicate", method = RequestMethod.GET)
	public String login_duplicate() {
		logger.info("중복로그인");
		return "login_duplicate";
	}

	@RequestMapping(value="/denied", method = RequestMethod.GET)
	public String denied() {
		logger.info("접속거부");
		return "denied";
	}
}