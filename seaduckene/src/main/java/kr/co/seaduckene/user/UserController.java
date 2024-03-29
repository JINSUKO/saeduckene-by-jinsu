package kr.co.seaduckene.user;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.imageio.ImageIO;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.FlashMap;
import org.springframework.web.servlet.FlashMapManager;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.support.RequestContextUtils;
import org.springframework.web.util.WebUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import kr.co.seaduckene.admin.command.AskListVO;
import kr.co.seaduckene.admin.service.IAdminService;
import kr.co.seaduckene.board.service.IBoardService;
import kr.co.seaduckene.common.AddressVO;
import kr.co.seaduckene.common.CategoryVO;
import kr.co.seaduckene.product.command.ProductBasketVO;
import kr.co.seaduckene.product.command.ProductOrderVO;
import kr.co.seaduckene.product.command.ProductVO;
import kr.co.seaduckene.product.service.IProductService;
import kr.co.seaduckene.user.command.UserVO;
import kr.co.seaduckene.user.service.IUserService;
import kr.co.seaduckene.user.service.KakaoLoginService;
import kr.co.seaduckene.user.service.NaverLoginService;
import kr.co.seaduckene.util.AskCategoryBoardVO;
import kr.co.seaduckene.util.CertificationMailService;
import lombok.extern.log4j.Log4j;

@Controller
@RequestMapping("/user")
@Log4j
public class UserController {
	
	@Autowired
	private IBoardService boardService;
	
	@Autowired
	private IUserService userService;
	
	@Autowired
	private CertificationMailService mailService;
	
	@Autowired
	private IProductService productService;
	
	@Autowired
	private IAdminService adminService;
	
	@Autowired
	private KakaoLoginService kktLoginService;
	
	@Autowired
	private NaverLoginService nvLoginService;
	

	@GetMapping("/userLogin")
	public void userLogin(Model model, HttpSession session) {
		log.info("login get 요청");
		
		Map<String, String> kakaoTokenMap = kktLoginService.getKakaoAuthUrl();
		model.addAttribute("KktUrl", kakaoTokenMap.get("url"));
		
		Map<String, String> naverTokenMap = nvLoginService.getNaverAuthUrl();
		model.addAttribute("nvUrl", naverTokenMap.get("url"));
		
		session.setAttribute("kakaoState", kakaoTokenMap.get("state"));
		session.setAttribute("naverState", naverTokenMap.get("state"));
		
	}
	
	
	@GetMapping("/userKakaoLogin")
	public ModelAndView userKaKaoLogin(String code, String state, String error, String error_description
									, ModelAndView modelAndView, HttpServletRequest request, HttpServletResponse response) {
		log.info("kakaoLogin redirect uri 요청");
		log.info("error: " + error);
		log.info("error_description: " + error_description);
		
		// url로 직접 접근 막기.
		if (code == null && error == null) {
			modelAndView.setViewName("redirect:/user/userLoginAuth");
			
			return modelAndView;
		}
		
		// 카카오 로그인 최초 시도 - 카카오계정 정보를 서비스DB에 저장함.
		String kakaoAccessToken = null;
		if (error == null && state.equals(request.getSession().getAttribute("kakaoState"))) {
			
			kakaoAccessToken = kktLoginService.getKakaoAuthToken(code);
			
			Map<String, String> kakaoInfosMap = kktLoginService.getKakaoUserInfos(kakaoAccessToken);
			
			String KKLId = kakaoInfosMap.get("KKLId");
			
			UserVO userVo = new UserVO();
			userVo.setUserId(kakaoInfosMap.get("userId"));
			userVo.setUserPw(kakaoInfosMap.get("userPw"));
			userVo.setUserNickname(kakaoInfosMap.get("userNickname"));
			userVo.setUserName(kakaoInfosMap.get("userName"));
			userVo.setUserTel(kakaoInfosMap.get("userTel"));
			
			// false == 계정이 없음 -> 계정, 카카오계정 등록 후 userVo 받음
			if (!userService.checkKKL(KKLId)) {
				userService.registUser(userVo);
				
				// userNo 있는 useVo 받음.
				userVo = userService.getUserVo(userVo);
				userVo.setUserKakaoId(KKLId);
				userVo.setUserKakaoAccessToken(kakaoAccessToken);
				
				userService.registKKLAcc(userVo);
				
				String[] majorTitle = {"미디어"};
				String[] minorTitle = {"드라마"};
				
				userService.addUserFavorites(majorTitle, minorTitle, userVo.getUserNo());
				
				
				// 카카오 url 이미지에서 파일 받아오고 로컬에 저장.
				// 이미지 저장 데이터 준비
				String KKLProfilePath = kakaoInfosMap.get("userProfilePath");
				String KKLProfileFolder = kakaoInfosMap.get("userProfileFolder");
				String KKLProfileImageName = kakaoInfosMap.get("userProfileFileName");
				URL kakaoImageurl = null;
				try {
					kakaoImageurl = new URL(KKLProfilePath + KKLProfileFolder + "/" + KKLProfileImageName);
					log.info("kakaoImageurl: " + kakaoImageurl);
				} catch (MalformedURLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				SimpleDateFormat simple = new SimpleDateFormat("yyyyMMdd");
				String today = simple.format(new Date());
				
				BufferedImage kakaoImage = null;
				try {
					kakaoImage = ImageIO.read(kakaoImageurl);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				KKLProfilePath = "c:/imgduck/user/";
				KKLProfileFolder = today;
				
				String formatName = KKLProfileImageName.substring(KKLProfileImageName.lastIndexOf(".") + 1);
				
				String uploadFolder = KKLProfilePath + today;
				
				File saveFile = new File(uploadFolder + "/" + KKLProfileImageName);
				
				File folder = new File(uploadFolder);
				if(!folder.exists()) {
					folder.mkdirs();
				}
				
				try {
					log.info("kakaoImage: " + kakaoImage);
					log.info("formatName: " + formatName);
					log.info("saveFile: " + saveFile);
					ImageIO.write(kakaoImage, formatName, saveFile);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				
				userVo.setUserProfilePath(KKLProfilePath);
				userVo.setUserProfileFolder(KKLProfileFolder);
				userVo.setUserProfileFileName(KKLProfileImageName);
				
				userService.updateUserInfo(userVo);
				
			// true == 계정이 있음 -> 카카오계정으로 userVo 받음
			} else {
				userVo = userService.getUserVoWithKKLId(KKLId);
				userVo.setUserKakaoAccessToken(kakaoAccessToken);
				
				userService.updateKKLAccToken(userVo);
			}
			
			
			log.info("Kakao Login userVo: " + userVo);
			
			modelAndView.addObject("userId", userVo.getUserId());
			modelAndView.addObject("userPw", userVo.getUserPw());
			
			return modelAndView;
		}
		
		FlashMap fm = new FlashMap();
		fm.put("msg", "kakaoFail");
		FlashMapManager fmm = RequestContextUtils.getFlashMapManager(request);
		fmm.saveOutputFlashMap(fm, request, response);
		modelAndView.setViewName("redirect:/user/userLogin");
		
		return modelAndView;
	}
	
	@GetMapping("/userNaverLogin")
	public ModelAndView userNaverLogin(String code, String state, String error, String error_description
									, ModelAndView modelAndView, HttpServletRequest request, HttpServletResponse response) {
		log.info("naverLogin redirect uri 요청");
		log.info("error: " + error);
		log.info("error_description: " + error_description);
		
		// url로 직접 접근 막기.
		if (code == null && error == null) {
			modelAndView.setViewName("redirect:/user/userLoginAuth");
			
			return modelAndView;
		}
		
		String naverAccessToken = null;
		
		// 네이버 로그인 시도 성공 시 회원 정보 얻는 코드 실행
		if (code != null && state.equals(request.getSession().getAttribute("naverState"))) {
			Map<String, Object> naverTokens = nvLoginService.getNaverAuthToken(code, state);
			
			naverAccessToken = (String) naverTokens.get("access_token");
			
			Map<String, String> naverInfosMap = nvLoginService.getNaverUserInfos(naverAccessToken, (String) naverTokens.get("token_type"));
			
			UserVO userVo = new UserVO();
			String NVLId = naverInfosMap.get("NVLId");
			
			// false == 계정이 없음 -> 계정, 네이버계정 등록 후 userVo 받음
			if (!userService.checkNL(NVLId)) {
				userVo.setUserId(naverInfosMap.get("userId"));
				userVo.setUserPw(naverInfosMap.get("userPw"));
				userVo.setUserNickname(naverInfosMap.get("userNickname"));
				userVo.setUserName(naverInfosMap.get("userName"));
				userVo.setUserTel(naverInfosMap.get("userTel"));
				userVo.setUserEmail(naverInfosMap.get("userEmail"));
				
				userService.registUser(userVo);
				
				// userNo 있는 useVo 받음.
				userVo = userService.getUserVo(userVo);
				userVo.setUserNaverId(NVLId);
				userVo.setUserNaverAccessToken(naverAccessToken);

				userService.registNVLAcc(userVo);
				
				String[] majorTitle = {"미디어"};
				String[] minorTitle = {"드라마"};
				
				userService.addUserFavorites(majorTitle, minorTitle, userVo.getUserNo());
				
				
				// 네이버 url 이미지에서 파일 받아오고 로컬에 저장.
				// 이미지 저장 데이터 준비
				String NLProfilePath = naverInfosMap.get("userProfilePath");
				String NLProfileFolder = naverInfosMap.get("userProfileFolder");
				String NLProfileImageName = naverInfosMap.get("userProfileFileName");
				URL naverImageurl = null;
				try {
					naverImageurl = new URL(NLProfilePath + NLProfileFolder + "/" + NLProfileImageName);
					log.info("naverImageurl: " + naverImageurl);
				} catch (MalformedURLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				SimpleDateFormat simple = new SimpleDateFormat("yyyyMMdd");
				String today = simple.format(new Date());
				
				BufferedImage naverImage = null;
				try {
					naverImage = ImageIO.read(naverImageurl);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				NLProfilePath = "c:/imgduck/user/";
				NLProfileFolder = today;
				
				String formatName = NLProfileImageName.substring(NLProfileImageName.lastIndexOf(".") + 1);
				
				String uploadFolder = NLProfilePath + today;
				
				File saveFile = new File(uploadFolder + "/" + NLProfileImageName);
				
				File folder = new File(uploadFolder);
				if(!folder.exists()) {
					folder.mkdirs();
				}
				
				try {
					log.info("naverImage: " + naverImage);
					log.info("formatName: " + formatName);
					log.info("saveFile: " + saveFile);
					ImageIO.write(naverImage, formatName, saveFile);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				
				userVo.setUserProfilePath(NLProfilePath);
				userVo.setUserProfileFolder(NLProfileFolder);
				userVo.setUserProfileFileName(NLProfileImageName);
				
				userService.updateUserInfo(userVo);
				
			// true == 계정이 있음 -> 네이버계정으로 userVo 받음
			} else {
				userVo = userService.getUserVoWithNVLId(NVLId);
				userVo.setUserNaverAccessToken(naverAccessToken);
				
				userService.updateNVLAccToken(userVo);
			}
			

			log.info("Naver Login userVo: " + userVo);
			
			modelAndView.addObject("userId", userVo.getUserId());
			modelAndView.addObject("userPw", userVo.getUserPw());
			
			return modelAndView;
		}
		
		// 네이버 로그인 실패 시 진행
		FlashMap fm = new FlashMap();
		fm.put("msg", "naverFail");
		FlashMapManager fmm = RequestContextUtils.getFlashMapManager(request);
		fmm.saveOutputFlashMap(fm, request, response);
		modelAndView.setViewName("redirect:/user/userLogin");
		
		return modelAndView;
	}
	
	@GetMapping("/userLoginAuth")
	public void userLoginAuth() {}
	
	@PostMapping("/userLoginAuth")
	public ModelAndView userLogin(UserVO userVO, ModelAndView modelAndView, int autoLoginCheck
							, String kakaoLogin, String naverLogin) {
		log.info(userVO);
		
		// 비밀번호 암호화는 나중에 구현할 것.
		
		modelAndView.addObject("userVo", userService.getUserVo(userVO));
		modelAndView.addObject("autoLoginCheck", autoLoginCheck);
		modelAndView.addObject("kakaoLogin", kakaoLogin);
		modelAndView.addObject("naverLogin", naverLogin);
		
		return modelAndView;
	}
	
	@GetMapping("/userJoin")
	public void userJoin(HttpServletRequest request) {
		log.info(userService.getCategories());
		
		request.setAttribute("categoryList", userService.getCategories());
		request.setAttribute("majorLength", userService.getCategories().size() - 1);
		log.info(userService.getCategories().size() - 1);
	}
	
	@GetMapping("/userLogout")
	public ModelAndView userLogin(ModelAndView modelAndView, HttpServletRequest request, HttpServletResponse response) {
		
		HttpSession session = request.getSession();
		
		Cookie autoLoginCookie = WebUtils.getCookie(request, "autoLoginCookie");
		if (autoLoginCookie != null) {
			UserVO userVo = userService.getUserBySessionId(autoLoginCookie.getValue());
			log.info("autoLogin userVo: " + userVo);
			
			if (userVo != null) {
				// 쿠키 삭제는 받아온 쿠키 객체를 직접 지운다
				autoLoginCookie.setPath(request.getContextPath() + "/");
				autoLoginCookie.setMaxAge(0);
				response.addCookie(autoLoginCookie);
				userService.undoAutoLogin(userVo.getUserNo());
			}
			
		}
		
		if (session.getAttribute("login") != null) {
			
			int userNo = ((UserVO) session.getAttribute("login")).getUserNo();
			
			nvLoginService.doNaverLogout(userService.getUserVoWithNo(userNo));
			
			session.removeAttribute("kakao");
			session.removeAttribute("naver");
			session.removeAttribute("login");
		}
		modelAndView.setViewName("redirect:/user/userLogin");
		
		return modelAndView;
	}
	
	// 파라미터로 여러 name에서 List로 받아오려면
	// @RequestParam를 사용하여 요청파라미터 키를 명시해주어야 List로 받을 수 있다. 
	@PostMapping("/userJoin")
	public ModelAndView userjoin(UserVO userVO, AddressVO addressVO
			, @RequestParam("categoryMajorTitles") List<String> categoryMajorTitles, @RequestParam("categoryMinorTitles") List<String> categoryMinorTitles
			, ModelAndView modelAndView, MultipartFile profilePic) {
		log.info(userVO);
		log.info(addressVO);
		log.info(categoryMajorTitles);
		log.info(categoryMinorTitles);
		log.info(profilePic);
		
		if (profilePic.getSize() != 0) {
			SimpleDateFormat simple = new SimpleDateFormat("yyyyMMdd");
			String today = simple.format(new Date());
			
			String fileRealName = profilePic.getOriginalFilename(); // 파일 원본명
			String profilePath = "c:/imgduck/user/";
			
			String fileExtension = fileRealName.substring(fileRealName.lastIndexOf("."),fileRealName.length());
			
			UUID uuid = UUID.randomUUID();
			String uu = uuid.toString().replace("-","");
			
			
			userVO.setUserProfileFileRealName(fileRealName);
			userVO.setUserProfilePath(profilePath);
			userVO.setUserProfileFolder(today);
			userVO.setUserProfileFileName(uu + fileExtension); 
			
			String uploadFolder = profilePath + today;
			File folder = new File(uploadFolder);
			if(!folder.exists()) {
				folder.mkdirs();
			}
			File saveFile = new File(uploadFolder+"/"+uu+fileExtension);
			try {
				profilePic.transferTo(saveFile);
			} catch (IllegalStateException | IOException e) {
				e.printStackTrace();
			}
		}
		
		//user table 등록
		userService.registUser(userVO);
		
		// 다른 곳에서 user정보가 필요할 시, 로그인 중인 세션에서 uservo 갖고 올 예정.
		// - 안됨. userno에서 uservo를 가져와야 최신 유저 정보를 쓸 수 있다.
		// 계정 생성중에는 세션 정보가 없다.
		UserVO registeredUserVO = userService.getUserVo(userVO);
		int registerdUserNo = registeredUserVO.getUserNo();
		addressVO.setAddressUserNo(registerdUserNo);
		
		String[] majorTitleArray = categoryMajorTitles.toArray(new String[categoryMajorTitles.size()]);
		String[] minorTitleArray = categoryMinorTitles.toArray(new String[categoryMinorTitles.size()]);
		
		//favorite table 등록
		userService.addUserFavorites(majorTitleArray, minorTitleArray, registerdUserNo);
		
		if (!addressVO.getAddressBasic().equals("")) {
			// address table 등록
			userService.registAddr(addressVO);
		}
		
		modelAndView.setViewName("redirect:/user/userJoinSuccess");
		
		return modelAndView;
	}
	
	@GetMapping("/userJoinSuccess")
	public void userJoinSuccess() {};
	
	// email인증
	@ResponseBody
	@PostMapping("/userConfEmail")
	public String userConfEmail(@RequestBody String email) {
		log.info("email인증요청 들어옴" + email);
		return mailService.joinEmail(email);
	}

	@GetMapping("/userMyPage/{head}")
	public ModelAndView userMyPage(ModelAndView modelAndView, @PathVariable int head, HttpSession session) {
		modelAndView.addObject("toggle", head);
		
		modelAndView.setViewName("/user/userMyPage");
		int userNo = ((UserVO)session.getAttribute("login")).getUserNo();
		UserVO userVo = userService.getUserVoWithNo(userNo);
		List<ProductBasketVO> bvo = userService.getBasket(userNo);
		List<ProductOrderVO> ovo = productService.getOrder(userNo);
		List<String> name = new ArrayList<String>();
		System.out.println(ovo);
		
		if(ovo != null) {
			for(ProductOrderVO order : ovo) {
				ProductVO vo2 = productService.getContent(order.getOrderProductNo());
				
				name.add(vo2.getProductName());
			}
		}
		
		
		int total = 0;
		for(ProductBasketVO b : bvo) {
			total += b.getBasketQuantity() * b.getBasketPrice();
		}
		
		modelAndView.addObject("name", name);
		modelAndView.addObject("order", ovo);
		modelAndView.addObject("basket", bvo);
		modelAndView.addObject("btotal", total);
		modelAndView.addObject("user", userVo);
		
		log.info(userService.getCategories());
		
		modelAndView.addObject("categoryList", userService.getCategories());
		modelAndView.addObject("majorLength", userService.getCategories().size() - 1);
		
		List<CategoryVO> categoryVOs = userService.getUserCategories(userNo);
		
		
		modelAndView.addObject("userCategoryList", categoryVOs);
		log.info(categoryVOs);
		
		log.info(userService.getUserCategories(userNo).toString());
		
		List<AddressVO> userAddrList = userService.getUserAddr(userNo);
		
		modelAndView.addObject("userAddrList", userAddrList);
		
		
		List<AskCategoryBoardVO> askCategoryBoardList = userService.getUserAskCategoryBoardList(userNo);
		
		modelAndView.addObject("askCategoryBoardList", askCategoryBoardList);

		List<AskListVO> askList = adminService.getAskLisk(userNo);
		
		modelAndView.addObject("askList", askList);
		
		modelAndView.addObject("total", boardService.getMyBoardTotal(userNo));
		
		return modelAndView;
	}
	
	@GetMapping("/userBasket")
	public ModelAndView basket(ModelAndView modelAndView) {
		System.out.println("/userBasket GET");
		modelAndView.setViewName("redirect:/user/userMyPage/3");
		return modelAndView;
	}
	

	@ResponseBody
	@PostMapping("/checkId")
	public String checkId(@RequestBody String userId) {
		log.info(userId);
		
		if (userService.checkId(userId) == 0) {

			return "accepted";
		} else {

			return "duplicated";
		}
		
		
	}
	
	@ResponseBody
	@PostMapping("/checkNickname")
	public String checkNickname(@RequestBody String userNickname) {
		log.info(userNickname);
		
		if (userService.checkNickname(userNickname) == 0) {
			
			return "accepted";
		} else {
			
			return "duplicated";
		}
		
		
	}
	
	@ResponseBody
	@PostMapping("/pwModify")
	public String pwModify(@RequestBody List<String> passwords, HttpServletRequest request) {
		log.info(passwords);
		String userPw = passwords.get(0);
		String modiPw = passwords.get(1);
		String checkPw = passwords.get(2);
		log.info(userPw);
		log.info(modiPw);
		log.info(checkPw);
		
		HttpSession session = request.getSession();
		int userNo = ((UserVO) session.getAttribute("login")).getUserNo();
		
		Map<String, String> pwMap = new HashMap<String, String>();
		pwMap.put("userNo", Integer.toString(userNo));
		pwMap.put("userPw", userPw);
		pwMap.put("modiPw", modiPw);
		pwMap.put("checkPw", checkPw);
		
		if (userService.checkCurrPw(pwMap) == 1) {
			
			if (modiPw.equals(checkPw)) {
				userService.changePw(pwMap);
			}
			
			return "PwChanged";
		} else {
			
			return "wrongPw";
		}
		
	}
	
	@ResponseBody
	@PostMapping("/userPwConfirm")
	public String userUpdateConfirm(@RequestBody List<String> passwords, HttpServletRequest request) {
		String userPw = passwords.get(0);
		String checkPw = passwords.get(1);
		
		HttpSession session = request.getSession();
		int userNo = ((UserVO) session.getAttribute("login")).getUserNo();
		
		Map<String, String> pwMap = new HashMap<String, String>();
		pwMap.put("userNo", Integer.toString(userNo));
		pwMap.put("userPw", userPw);
		pwMap.put("checkPw", checkPw);
		
		return Integer.toString(userService.checkCurrPw(pwMap));
	}
	
	@PostMapping("/userUpdate") // 동일한 name으로 여러 태그에서 받아올때, String은 문자열 붙여서 들어오는데, List로 받아서 들어올 수도 있다.
	public ModelAndView userUpdate(UserVO userVO, @RequestParam(value = "addressBasics", required = false) List<String> addressBasics
	, @RequestParam(value = "addressDetails", required = false) List<String> addressDetails, @RequestParam(value = "addressZipNums", required = false) List<String> addressZipNums
	, @RequestParam("categoryMajorTitles") List<String> categoryMajorTitles, @RequestParam("categoryMinorTitles") List<String> categoryMinorTitles
								, ModelAndView modelAndView, MultipartFile profilePic, String categoryIndex, String addressCount) {
		log.info("/userUpdate");
		log.info(userVO); 
		log.info(addressBasics); // 수정된 부분 확인 후 db 수정
		log.info(addressDetails); // 수정된 부분 확인 후 db 수정
		log.info(addressZipNums); // 수정된 부분 확인 후 db 수정
		log.info(categoryMajorTitles); // 삭제된 부분 조회 후 삭제 처리 먼저, 추가된 부분 확인 후 db favorite 추가. 
		log.info(categoryMinorTitles);  
		log.info(profilePic);
		log.info(categoryIndex);
		log.info(addressCount);
		
		int userNo = userVO.getUserNo();
		
		ObjectMapper categoryConverter = new ObjectMapper();
		ObjectMapper addressConverter = new ObjectMapper();
		List<String> categoryIndexList = new ArrayList<>();
		List<String> addressCountList = new ArrayList<>();
		try {
			categoryIndexList = categoryConverter.readValue(categoryIndex, new TypeReference<List<String>>(){});
			addressCountList = addressConverter.readValue(addressCount, new TypeReference<List<String>>(){});
		} catch (JsonProcessingException e1) {
			e1.printStackTrace();
		}
		
		// 저장된 카테고리 삭제 코드
		List<Integer> deletedFavoriteIndex = new ArrayList<>();
		Map<String, Object> deletedFavoriteIndexMap = new HashMap<>();
		for (int i = 0; i < userService.getUserFavorites(userNo).size(); i++) {
			if (categoryIndexList.contains(Integer.toString(i))) {
				continue;
			} else {
				deletedFavoriteIndex.add(i + 1);
			}
		}
		
		deletedFavoriteIndexMap.put("deleted_favorite_index", deletedFavoriteIndex);
//		deletedFavoriteIndexMap.put("size", deletedFavoriteIndex.size());
		deletedFavoriteIndexMap.put("userNo", userNo);
		if (deletedFavoriteIndex.size() > 0) {
			userService.deleteUserFavorites(deletedFavoriteIndexMap);
		}
		
		// 저장된 카테고리 수정 코드
		int currUserFavortiesCount = categoryIndexList.size();
		
		if (currUserFavortiesCount > 0) {
			String[] currMajorArray = new String[currUserFavortiesCount];
			String[] currMinorArray = new String[currUserFavortiesCount];
			
			for (int i = 0; i < currUserFavortiesCount; i++) {
				currMajorArray[i] = categoryMajorTitles.get(i);
				currMinorArray[i] = categoryMinorTitles.get(i);
			}
			
			log.info(currMajorArray);
			log.info(currMinorArray);
			userService.updateUserFavorites(currMajorArray, currMinorArray, userNo);
		}
		
		// 추가된 카테고리 insert 코드
		log.info(categoryMajorTitles);
		log.info(categoryMinorTitles);
		
		int allUserCategoriesCount = categoryMajorTitles.size();
		if (allUserCategoriesCount == 0) {
			allUserCategoriesCount = 1;
		}
//		currUserFavortiesCount = categoryIndexList.size();
		
		currUserFavortiesCount = userService.getCountUserAddress(userNo);
		
		if (currUserFavortiesCount < allUserCategoriesCount) {
			String[] newMajorArray = new String[allUserCategoriesCount - currUserFavortiesCount];
			String[] newMinorArray = new String[allUserCategoriesCount - currUserFavortiesCount];
			
			for (int i = currUserFavortiesCount; i < allUserCategoriesCount; i++) {
				newMajorArray[i - currUserFavortiesCount] = categoryMajorTitles.get(i);
				newMinorArray[i - currUserFavortiesCount] = categoryMinorTitles.get(i);
			}
			
			log.info(newMajorArray);
			log.info(newMinorArray);
			userService.addUserFavorites(newMajorArray, newMinorArray, userNo);
		}
		
		
		
		log.info(addressCountList);
		
		List<AddressVO> beforeDeleteAddressList = userService.getUserAddr(userNo);

		if (addressBasics != null) {
			log.info(addressBasics);
			log.info(addressDetails);
			log.info(addressZipNums);
			
			int allAddressCount = addressBasics.size();
			int currAddressCount = addressCountList.size();
			
			// 저장된 주소 삭제 코드
			List<Integer> deletedAddressCount = new ArrayList<>();
			Map<String, Object> deletedAddressCountMap = new HashMap<>();
			for (int i = 0; i < beforeDeleteAddressList.size(); i++) {
				if (addressCountList.contains(Integer.toString(i + 1))) {
					continue;
				} else {
					deletedAddressCount.add(i + 1);
				}
			}
			
			deletedAddressCountMap.put("deleted_adderss_count", deletedAddressCount);
//		deletedAddressCountMap.put("size", deletedAddressCount.size());
			deletedAddressCountMap.put("userNo", userNo);
			
			log.info("deletedAddressCountMap: " + deletedAddressCountMap);
			
			if (deletedAddressCount.size() > 0) {
				userService.deleteUserAddress(deletedAddressCountMap);
			}
			
			// 저장된 주소 변경 코드
			if (currAddressCount > 0) {
				String[] modiAddressBasicArray = new String[currAddressCount];
				String[] modiAddressDetailArray = new String[currAddressCount];
				String[] modiAddressZipNumArray = new String[currAddressCount];
				
				for (int i = 0; i < currAddressCount; i++) {
					modiAddressBasicArray[i] = addressBasics.get(i);
					modiAddressDetailArray[i] = addressDetails.get(i);
					modiAddressZipNumArray[i] = addressZipNums.get(i);
				}
				
				userService.updateUserAddress(modiAddressBasicArray, modiAddressDetailArray, modiAddressZipNumArray, userNo);
			}
			
			// 추가된 주소 insert 코드
			if (currAddressCount < allAddressCount) {
				
				String[] newAddressBasicArray = new String[allAddressCount - currAddressCount];
				String[] newAddressDetailArray = new String[allAddressCount - currAddressCount];
				String[] newAddressZipNumArray = new String[allAddressCount - currAddressCount];
				
				for (int i = currAddressCount; i < allAddressCount; i++) {
					newAddressBasicArray[i - currAddressCount] = addressBasics.get(i);
					newAddressDetailArray[i - currAddressCount] = addressDetails.get(i);
					newAddressZipNumArray[i - currAddressCount] = addressZipNums.get(i);
				}	
				
				log.info(newAddressBasicArray);
				log.info(newAddressDetailArray);
				log.info(newAddressZipNumArray);
				userService.addNewAddress(newAddressBasicArray, newAddressDetailArray, newAddressZipNumArray, userNo);
			}
			
			log.info("allAddressCount: " + allAddressCount);
			log.info("currAddressCount: " + currAddressCount);
			
		}
		
		
		if (profilePic.getSize() != 0) {
			SimpleDateFormat simple = new SimpleDateFormat("yyyyMMdd");
			String today = simple.format(new Date());
			
			String fileRealName = profilePic.getOriginalFilename(); // 파일 원본명
			String profilePath = "c:/imgduck/user/";
			
			String fileExtension = fileRealName.substring(fileRealName.lastIndexOf("."), fileRealName.length());
			
			UUID uuid = UUID.randomUUID();
			String uu = uuid.toString().replace("-", "");
			
			
			userVO.setUserProfileFileRealName(fileRealName);
			userVO.setUserProfilePath(profilePath);
			userVO.setUserProfileFolder(today);
			userVO.setUserProfileFileName(uu + fileExtension); 
			
			String uploadFolder = profilePath + today;
			File folder = new File(uploadFolder);
			if(!folder.exists()) {
				folder.mkdirs();
			}
			File saveFile = new File(uploadFolder+"/"+uu+fileExtension);
			try {
				profilePic.transferTo(saveFile);
			} catch (IllegalStateException | IOException e) {
				e.printStackTrace();
			}
			
			UserVO userBeforeUpdate = userService.getUserVoWithNo(userVO.getUserNo());
			File previousFile = new File(userBeforeUpdate.getUserProfilePath() + userBeforeUpdate.getUserProfileFolder()
										+ "/" + userBeforeUpdate.getUserProfileFileName());
			
			previousFile.delete();
			
		}
		
		// user 정보 업데이트
		userService.updateUserInfo(userVO);
		
		modelAndView.setViewName("redirect:/user/userMyPage/1");
		
		return modelAndView;
	}
	
	@PostMapping("/userDelete")
	public ModelAndView userDelete(int userNo, ModelAndView modelAndView, HttpServletRequest request, HttpServletResponse response) {
		
		userService.deleteUserAllInfo(userNo, request, response);
		modelAndView.setViewName("redirect:/user/userLogin");
		
		return modelAndView;
	}
	
	@GetMapping("/userFindAccount")
	public void userFindAccount() {}
	
	@PostMapping("/userFindAccount")
	public String userFindAccount (String userName, String userEmail, Model model) {
		List<String> userIds = userService.findAccount(userName,userEmail);
		model.addAttribute("userIds", userIds);
		return "/user/userFindAccountRes";
	}
	
	@GetMapping("/userFindAccountRes")
	public void successFindAccount() {}

	@GetMapping("/userFindPw")
	public void userFindPw() {}
	
	@PostMapping("/userFindPw")
	public String userFindPw(String userId, String userEmail, Model model) {
		String tmpPw = null;
		// mailService에서 임시비밀번호 보내기
		if(userService.checkUser(userId,userEmail)== 1) {
			tmpPw = mailService.sendTmpPw(userEmail);
			
			// mapper에서 임시비밀번호로 비밀번호 수정하기
			userService.updatePw(userId, tmpPw);
			return "/user/userLogin";
		} else {
			model.addAttribute("msg","noUser");
			return "/user/userFindPw";
		}
		
	}
	
	@ResponseBody
	@GetMapping("/getProfile")
	public ResponseEntity<byte[]> getProfile(HttpSession session) {
		
		int userNo = ((UserVO) session.getAttribute("login")).getUserNo();
		UserVO loginUser = userService.getUserVoWithNo(userNo);
		
		File file = new File(loginUser.getUserProfilePath() + loginUser.getUserProfileFolder() + '/' +loginUser.getUserProfileFileName());
		ResponseEntity<byte[]> result = null;
		HttpHeaders headers = new HttpHeaders();
		try {
			headers.add("Content-Type", Files.probeContentType(file.toPath()));
			result = new ResponseEntity<byte[]>(FileCopyUtils.copyToByteArray(file), headers, HttpStatus.OK);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		log.info("getProfile: " + result);
		return result;
	}
	
	@ResponseBody
	@PostMapping("/changeMainAddress")
	public String changeMainAddress(@RequestBody String addressCount, HttpSession session) {
		log.info(addressCount);
		
		int userNo = ((UserVO) session.getAttribute("login")).getUserNo();
		int addressRn = Integer.parseInt(addressCount);
		
		AddressVO prevRprsttvAddress = userService.getUserAddressWithRn(1, userNo);
		AddressVO nextRprsttvAddress = userService.getUserAddressWithRn(addressRn, userNo);
		
		log.info(prevRprsttvAddress);
		log.info(nextRprsttvAddress);
		Map<String, Integer> map1 = new HashMap<String, Integer>();
		map1.put("whereUserNo", userNo);
		map1.put("whereAddressNo", prevRprsttvAddress.getAddressNo());
		map1.put("modiAddressNo", 0);
		map1.put("modiAddressRprsttv", 0);
		
		userService.modiAddressNoAndRepresent(map1);
		
		Map<String, Integer> map2 = new HashMap<String, Integer>();
		map2.put("whereUserNo", userNo);
		map2.put("whereAddressNo", nextRprsttvAddress.getAddressNo());
		map2.put("modiAddressNo", prevRprsttvAddress.getAddressNo());
		map2.put("modiAddressRprsttv", 1);
		
		userService.modiAddressNoAndRepresent(map2);
		
		Map<String, Integer> map3 = new HashMap<String, Integer>();
		map3.put("whereUserNo", userNo);
		map3.put("whereAddressNo", 0);
		map3.put("modiAddressNo", nextRprsttvAddress.getAddressNo());
		map3.put("modiAddressRprsttv", 0);
		
		userService.modiAddressNoAndRepresent(map3);
		
		return "changed";
	}
	
	@GetMapping("/userAskCategoryBoardDetail/{askBoardNo}")
	public String userAskCategoryBoardDetail(@PathVariable int askBoardNo, Model model) {
		AskCategoryBoardVO askCategoryBoard = userService.getAskCategoryBoard(askBoardNo);
		
		model.addAttribute("askCategoryBoard", askCategoryBoard);
		
		return "/user/userAskCategoryBoardDetail";
	}


}
