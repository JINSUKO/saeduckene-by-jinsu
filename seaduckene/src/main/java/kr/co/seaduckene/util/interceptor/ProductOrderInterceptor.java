package kr.co.seaduckene.util.interceptor;

import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import kr.co.seaduckene.product.mapper.IProductMapper;
import kr.co.seaduckene.user.command.UserVO;
import lombok.extern.log4j.Log4j;

@Log4j
@Component
public class ProductOrderInterceptor implements HandlerInterceptor {

	@Autowired
	private IProductMapper mapper;
	

	@Override
	public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
			ModelAndView modelAndView) throws Exception {
		System.out.println("post 인터셉터 동작!!!!!!!!");

		HttpSession session = request.getSession(); 
		UserVO vo =(UserVO)session.getAttribute("login"); 
		int userNo = vo.getUserNo();
		

		// userEmail 등록여부 체크
		String email = mapper.checkEmail(userNo);
		System.out.println(email);

		if (email == null) {
			modelAndView.addObject("msg", "null");
		} else {
			modelAndView.addObject("msg", "exist");
		}
		


	}
	
	
	
	
	
	
	
}
