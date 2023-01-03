package kr.co.seaduckene.product.service;

import java.util.List;
import java.util.Map;

import kr.co.seaduckene.common.CategoryVO;
import kr.co.seaduckene.product.command.ProductImageVO;
import kr.co.seaduckene.product.command.ProductOrderVO;
import kr.co.seaduckene.product.command.ProductVO;
import kr.co.seaduckene.user.command.UserVO;

public interface IProductService {
	
	// 상품등록
	void insertProduct(Map<String, Object> map);
	// 상품상세
	ProductVO getContent(int num);
	
	// 상품리스트
	
	// 장바구니 목록
	
	// 상품주문
	void order(List<Integer> orderProductNoList ,ProductOrderVO order, String userEmail, UserVO user);
	
	//카테고리 리스트 불러오기
	List<CategoryVO> getCategory();
	
	//소카테고리 불러오기
	List<String> getMinor(String major);
	
	//카테고리넘버
	int getCNum(Map<String, Object> map);
	
	//이미지 삽입
	void insertImg(ProductImageVO vo);

}
