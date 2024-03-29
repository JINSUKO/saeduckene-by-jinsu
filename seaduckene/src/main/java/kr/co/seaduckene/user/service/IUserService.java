package kr.co.seaduckene.user.service;

import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import kr.co.seaduckene.common.AddressVO;
import kr.co.seaduckene.common.CategoryVO;
import kr.co.seaduckene.favorite.FavoriteVO;
import kr.co.seaduckene.product.command.ProductBasketVO;
import kr.co.seaduckene.user.command.Categories;
import kr.co.seaduckene.user.command.UserVO;
import kr.co.seaduckene.util.AskCategoryBoardVO;

public interface IUserService {

	// 유저 생성
	void registUser(UserVO userVO);
	
	// 카카오 계정 정보 저장
	void registKKLAcc(UserVO userVO);
	
	// 로그아웃 후 다시 카카오 계정 정보 업데이트
	void updateKKLAccToken(UserVO userVO);
	
	// 네이버 계정 정보 저장
	void registNVLAcc(UserVO userVO);
	
	// 로그아웃 후 다시 네이버 계정 정보 업데이트
	void updateNVLAccToken(UserVO userVO);
	
	// 유저 정보 가져오기
	UserVO getUserVo(UserVO userVO);
	
	// 이미 생성한 카카오계정인지 확인
	boolean checkKKL(String KKLId);
	
	// 이미 생성한 네이버계정인지 확인
	boolean checkNL(String NVLId);
	
	// 카카오 계정으로 유저 정보 가져오기
	UserVO getUserVoWithKKLId(String KKLId);
	
	// 네이버 계정으로 유저 정보 가져오기
	UserVO getUserVoWithNVLId(String NVLId);
	
	// userNo로 유저 정보 가져오기 
	UserVO getUserVoWithNo(int userNo);
	
	// 유저 아이디 중복 확인
	int checkId(String userId);
	
	// 유저 닉네임 중복 확인
	int checkNickname(String userNickname);
	
	// 카테고리 정보 가져오기
	List<Categories> getCategories();
	
	// 유저 favorite table 연동
	void addUserFavorites(String[] categoryMajorTitles, String[] categoryMinorTitles, int userNo);
	
	// 유저에서 주소 테이블에 입력
	String registAddr(AddressVO addressVO);
	
	// 유저에서 주소 테이블의 내용 수정
	void updateAddr(AddressVO addressVO);
	
	//장바구니데이터 불러오기
	List<ProductBasketVO> getBasket(int num);
	
	// 유저의 카테고리 정보를 불러오기
	List<CategoryVO> getUserCategories(int userNo);
	
	// 유저의 주소 정보를 불러오기
	List<AddressVO> getUserAddr(int userNo);
	
	// 아이디 찾기
	List<String> findAccount(String userName, String userEmail);
	
	// 임시 비밀번호로 정보수정
	void updatePw(String userId, String tmpPw);
	
	// 자동 로그인 설정
	void setAutoLogin(UserVO userVo);
	
	// 쿠키의 sessionId로 session userVO 얻기
	UserVO getUserBySessionId(String sessionId);
	
	// 자동 로그인 해제
	void undoAutoLogin(int userNo);
	
	// 현재 입력한 비밀번호 검증 
	int checkCurrPw(Map<String, String> pwkMap);
	
	// 입력한 번호로 비밀번호 변경
	void changePw(Map<String, String> pwkMap);
	
	// 유저 정보 변경 
	void updateUserInfo(UserVO userVo);
	
	// 유저의 favorite 가져오기
	List<FavoriteVO> getUserFavorites(int userNo);
	
	// 유저의 favorite 삭제
	void deleteUserFavorites(Map<String, Object> deletedCount);
	
	// 유저의 favorite 변경
	void updateUserFavorites(String[] newMajorArray, String[] newMinorArray, int userNo);
	
	// 유저의 추가 주소 저장
	void addNewAddress(String[] newAddressBasicArray, String[] newAddressDetailArray, String[] newAddressZipNumArray, int userNo);
	
	// 유저의 address 삭제
	void deleteUserAddress(Map<String, Object> deletedCount);
	
	// 유저의 address 변경
	void updateUserAddress(String[] modiAddressBasicArray, String[] modiAddressDetailArray, String[] modiAddressZipNumArray, int userNo);
	
	// 유저의 address count 조회
	int getCountUserAddress(int userNo);
	
	// rn 순서, userNo로 addressVO 얻기
	AddressVO getUserAddressWithRn(int addressIndex, int userNo);
	
	// addressNo, addressRepresentative 변경
	void modiAddressNoAndRepresent(Map<String, Integer> map);
	
	// 유저 삭제 시 유저 정보 삭제
	void deleteUserAllInfo(int userNo, HttpServletRequest request, HttpServletResponse response);
	
	// 비밀번호 찾기 유저정보 확인
	int checkUser(String userId, String userEmail);
	
	// 카테고리별 게시판 해당 유저의 문의글 가져오기
	List<AskCategoryBoardVO> getUserAskCategoryBoardList(int userNo);
	
	// 카테고리별 게시판 문의글 상세보기 가져오기
	AskCategoryBoardVO getAskCategoryBoard(int askBoardNo);
}
