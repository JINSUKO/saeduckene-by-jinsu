package kr.co.seaduckene.product.service;

import java.util.List;
import java.util.Map;

import kr.co.seaduckene.common.CategoryVO;
import kr.co.seaduckene.product.command.ProductBasketVO;
import kr.co.seaduckene.product.command.ProductImageVO;
import kr.co.seaduckene.product.command.ProductOrderVO;
import kr.co.seaduckene.product.command.ProductVO;
import kr.co.seaduckene.user.command.UserVO;

public interface IProductService {

	// 상품등록
	void insertProduct(Map<String, Object> map);

	// 상품상세
	ProductVO getContent(int num);

	// 장바구니 상품 불러오기
	List<ProductBasketVO> getBasketList(int userNo);

	// 썸네일 가져오기
	ProductImageVO getThumbnailImg(int productNo);

	// 재고수량 체크
	String checkStock(List<Integer> productNoList, UserVO user, int ea);

	// 장바구니 상품주문
	void order(List<Integer> orderProductNoList, ProductOrderVO order, String userEmail, UserVO user);
	
	// 상품상세 상품주문
	void order2(List<Integer> orderProductNoList, ProductOrderVO order, String userEmail, UserVO user, int ea);

	// 상품삭제
	void delProduct(int no);

	// 카테고리 리스트 불러오기
	List<CategoryVO> getCategory();
	
	// 대 카테고리 불러오기
	List<String> getMajor();

	// 소카테고리 불러오기
	List<String> getMinor(String major);

	// 카테고리넘버
	int getCNum(Map<String, Object> map);

	// 메인 상품 이미지 출력 세션 있음
	List<ProductImageVO> mainImage(int userNo);

	// 메인 상품 이미지 출력 세션 없음
	List<ProductImageVO> mainImageNo();

	// 이미지 정보
	List<ProductImageVO> getImg(int num);

	// 장바구니 등록
	void insertBasket(ProductBasketVO vo);

	// 장바구니 수량 변경
	public void cQuantity(Map<String, Object> map);

	// 장바구니 중복 체크
	public int basketChk(ProductBasketVO vo);

	// 장바구니 삭제
	public void delBasekt(int basketNo);

	// 주문정보 불러오기
	List<ProductOrderVO> getOrder(int userNo);

	// 카테고리 정보 불러오기
	CategoryVO getCt(int categoryNo);

	// 상품정보 업데이트
	void updateProduct(ProductVO vo);

	// 상품사진 수정 새로 등록
	void insertImg(ProductImageVO vo);

	// 기존사진 삭제
	void deleteImage(int num);

	// 환불/주문취소 신청
	void refund(Map<String, Object> map);
	
	// 상품 리스트 
	List<ProductVO> getProductList(int categoryNo);
	
	// productNo 가져오기
	int getProductNoWithInfo(ProductVO productVo);
	
}
