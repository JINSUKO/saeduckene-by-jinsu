package kr.co.seaduckene.board;


import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.WebUtils;

import com.google.gson.JsonObject;

import kr.co.seaduckene.board.command.BoardListVO;
import kr.co.seaduckene.board.command.BoardVO;
import kr.co.seaduckene.board.service.IBoardService;
import kr.co.seaduckene.common.NoticeVO;
import kr.co.seaduckene.user.command.UserVO;
import kr.co.seaduckene.user.service.IUserService;
import kr.co.seaduckene.util.PageVO;
import kr.co.seaduckene.util.SummernoteCopy;
import lombok.extern.log4j.Log4j;

@Log4j
@Controller
@RequestMapping("/board")
public class boardListController {
	
	@Autowired
	private IBoardService boardService;
	
	@Autowired
	private IUserService userService;
	
	// summernote??? ????????? ?????? ???????????? ????????? ?????? ?????????
	@Autowired
	private SummernoteCopy summernoteCopy;

	//????????? ???????????? ??????
	@GetMapping("/boardList/{categoryNo}")
	public String boardList(Model model, @PathVariable int categoryNo) {

		// a?????? ?????? ? ???????????? ?????? ????????? ????????? GET
		System.out.println("????????? ???????????? ??????!");
		model.addAttribute("categoryNo", categoryNo);
		model.addAttribute("productList", boardService.proList(categoryNo));
		model.addAttribute("category",boardService.getCategory(categoryNo));
		model.addAttribute("total", boardService.getTotal(categoryNo));
		
		return "board/boardList";
	}
	
	//?????????
	@GetMapping("/boardLists")
	@ResponseBody
	public List<BoardListVO> boardList(PageVO paging, int categoryNo) {
		
		paging.setCpp(9);
		
		return boardService.list(paging,categoryNo);
	}
	
	//????????? ???????????? ?????? ??????
	@GetMapping("/boardWrite/{categoryNo}")
	public String boardWrite(@PathVariable int categoryNo, Model model, HttpSession session) {
		System.out.println("/board/boardWrite: GET");
		model.addAttribute("categoryNo", categoryNo);
		model.addAttribute("category",boardService.getCategory(categoryNo));
		
		int userNo = ((UserVO) session.getAttribute("login")).getUserNo();
		UserVO userVo = userService.getUserVoWithNo(userNo);
		model.addAttribute("nickName", userVo.getUserNickname());
		
		return "board/boardWrite";
	}
	// TODO Auto-generated catch block
	//???????????? DB ?????? ??????
	@PostMapping("/boardWrite")
	public String boardWrite(BoardVO boardVo, @RequestParam(value="filename", required=false) List<String> summerfileNames,
			MultipartFile thumbnail) {
		log.info("??? ?????? ????????? ?????????!");
		log.info("vo: " + boardVo);
		
		boardService.write(boardVo);
		int boardNo = boardService.boardNoSearch(boardVo.getBoardUserNo());
		boardVo.setBoardContent(boardVo.getBoardContent().replaceAll("-_-_-", Integer.toString(boardNo)));
		boardVo.setBoardNo(boardNo);
		
		if (summerfileNames != null) {
			log.info("summerfileNames: " + summerfileNames);
			
			List<String> summerfileBnNames = new ArrayList<String>();
			
			for (String summerfileName : summerfileNames) {
				String summerfileBnName = summerfileName.replaceAll("-_-_-", Integer.toString(boardNo));
				summerfileBnNames.add(summerfileBnName);
				
				boardService.boardImageAdd(boardNo, summerfileBnName);
			}
			
			try {
				summerfileUpload(boardVo, summerfileNames, summerfileBnNames);
			} catch (Exception e) {
				e.printStackTrace();
			}
			
		}
		
		if (thumbnail.getSize() != 0) {
			String uuid = UUID.randomUUID().toString().replaceAll("-", "");
			String fileRealName = thumbnail.getOriginalFilename();
			String fileExtension = fileRealName.substring(fileRealName.lastIndexOf("."), fileRealName.length());
			String boardThumbnailPath = "c:/imgduck/board/";
			
			boardVo.setBoardThumbnailPath(boardThumbnailPath);
			boardVo.setBoardThumbnailFileName(uuid + fileExtension);
			boardVo.setBoardThumbnailFileRealName(fileRealName);
			
			// ????????? ????????? ????????? ????????? ??? ?????????.
			/*File folder = new File(boardThumbnailPath);
			if(!folder.exists()) {
				folder.mkdirs();
			}*/
			File saveFile = new File(boardThumbnailPath + uuid + fileExtension);
			try {
				thumbnail.transferTo(saveFile);
			} catch (IllegalStateException | IOException e) {
				e.printStackTrace();
			}
		}
		
		boardService.update(boardVo);
		return "redirect:/board/boardList/" + boardVo.getBoardCategoryNo();
	}
	
	private void summerfileUpload(BoardVO boardVo, List<String> summerfileNames, List<String> summerfileBnNames) throws Exception {
		String boardContent;
		boardContent = boardVo.getBoardContent();
		String imgEditedContent = boardContent.replaceAll("summernoteImage", "getImg");
		boardVo.setBoardContent(imgEditedContent);
		
		// temp ???????????? board????????? ????????? ???????????? ?????? temp??? ????????? ???????????????.
		summernoteCopy.summerCopy(summerfileNames, summerfileBnNames, boardVo.getBoardNo());
		

	}
	
	//???????????? ?????????
	@GetMapping("/boardDetail/{boardNo}")
	public String boardDetail(@PathVariable int boardNo, PageVO pageVo, Model model, 
							HttpSession session, HttpServletRequest request, HttpServletResponse response) {
		System.out.println("boardNo" + boardNo);
		
		int userNo;
		if(session.getAttribute("login") != null) {
			UserVO userVo = (UserVO) session.getAttribute("login");
			userNo = userVo.getUserNo();
			userVo = userService.getUserVoWithNo(userNo);
			model.addAttribute("loginUser", userVo);
		} else {
			userNo = 0;
		}
		System.out.println("?????? userNo: " + userNo);
		
		Cookie countView = WebUtils.getCookie(request, "countView" + userNo);
		if(countView==null) {
			System.out.println("countView ??????");
			Cookie newCookie = new Cookie("countView"+userNo, "bNo"+boardNo);
			newCookie.setMaxAge(60*60*2); // ?????????
			response.addCookie(newCookie);
			boardService.addViewCount(boardNo);
		} else {
			System.out.println("countView ?????????");
			
			String value = countView.getValue();
			String[] bNoArray = value.split("bNo");
			List<String> bNoList = new ArrayList<String>(Arrays.asList(bNoArray)); 
			System.out.println("????????? ????????? ????????? ?????????:" + bNoList);
			System.out.println("????????? value:" + value);
			if(!bNoList.contains(Integer.toString(boardNo))) {
				Cookie newCookie = new Cookie("countView"+userNo, value + "bNo"+boardNo);
				newCookie.setMaxAge(60*60*2); // ?????????
				response.addCookie(newCookie);
				boardService.addViewCount(boardNo);
			}
		}

		/*
		 * model.addAttribute("list", service.content(boardNo));
		 */
	
		BoardVO boardVo = boardService.getBoardDetailVo(boardNo);
		int categoryNo = boardVo.getBoardCategoryNo();

		model.addAttribute("category",boardService.getCategory(categoryNo));
		model.addAttribute("board", boardVo);
		
		UserVO boardUserVo = userService.getUserVoWithNo(boardVo.getBoardUserNo());
		model.addAttribute("nickName", boardUserVo.getUserNickname());
		
		return "board/boardDetail";
	}
	
	//?????? ???????????? ??????
	@PostMapping("/boardModify")
	public void modify(@ModelAttribute("board") BoardVO boardVo, Model model, HttpSession session) {
		log.info(boardVo);
		int categoryNo = boardVo.getBoardCategoryNo();
		model.addAttribute("category", boardService.getCategory(categoryNo));
		
		int userNo = ((UserVO) session.getAttribute("login")).getUserNo();
		UserVO userVo = userService.getUserVoWithNo(userNo);
		
		model.addAttribute("nickName", userVo.getUserNickname());
	}
	
	/** 
	 * // TODO Auto-generated catch block
	 * */
	//??? ?????? ??????
	@PostMapping("/boardUpdate")
	public String boardUpdate(BoardVO updatedBoardVo, @RequestParam(value="filename", required=false) List<String> summerfileNames, MultipartFile thumbnail) {
		log.info(updatedBoardVo);
		log.info("??? ?????? ????????? ?????????!");

		int boardNo = updatedBoardVo.getBoardNo();
		updatedBoardVo.setBoardContent(updatedBoardVo.getBoardContent().replaceAll("-_-_-", Integer.toString(boardNo)));
		
		BoardVO previousBoardVo = boardService.getBoardDetailVo(boardNo);
		
		updatedBoardVo.setBoardThumbnailPath(previousBoardVo.getBoardThumbnailPath());
		updatedBoardVo.setBoardThumbnailFolder(previousBoardVo.getBoardThumbnailFolder());
		updatedBoardVo.setBoardThumbnailFileName(previousBoardVo.getBoardThumbnailFileName());
		updatedBoardVo.setBoardThumbnailFileRealName(previousBoardVo.getBoardThumbnailFileRealName());
		
		if (summerfileNames != null) {
			log.info("summerfileNames: " + summerfileNames);
			
			List<String> summerfileBnNames = new ArrayList<String>();
			
			for (String summerfileName : summerfileNames) {
				String summerfileBnName = summerfileName.replaceAll("-_-_-", Integer.toString(boardNo));
				summerfileBnNames.add(summerfileBnName);
				
				boardService.boardImageAdd(boardNo, summerfileBnName);
			}
			
			try {
				summerfileUpload(updatedBoardVo, summerfileNames, summerfileBnNames);
			} catch (Exception e) {
				e.printStackTrace();
			}
			
		}
		
		if (thumbnail.getSize() != 0) {
			String uuid = UUID.randomUUID().toString().replaceAll("-", "");
			String fileRealName = thumbnail.getOriginalFilename();
			String fileExtension = fileRealName.substring(fileRealName.lastIndexOf("."), fileRealName.length());
			String boardThumbnailPath = "c:/imgduck/board/";
			
			updatedBoardVo.setBoardThumbnailPath(boardThumbnailPath);
			updatedBoardVo.setBoardThumbnailFileName(uuid + fileExtension);
			updatedBoardVo.setBoardThumbnailFileRealName(fileRealName);
			
			// ????????? ????????? ????????? ????????? ??? ?????????.
			/*File folder = new File(boardThumbnailPath);
			if(!folder.exists()) {
				folder.mkdirs();
			}*/
			File saveFile = new File(boardThumbnailPath + uuid + fileExtension);
			try {
				thumbnail.transferTo(saveFile);
			} catch (IllegalStateException | IOException e) {
				e.printStackTrace();
			}
			
			// ?????? ????????? ?????? ????????? ??????
		    File previousThumbnailFile = new File(previousBoardVo.getBoardThumbnailPath() +
//				previousBoardVo.getBoardThumbnailFolder() + "/" +
			    previousBoardVo.getBoardThumbnailFileName());
		
		    previousThumbnailFile.delete();
			  
			 
		}
		
		boardService.update(updatedBoardVo);
		return "redirect:/board/boardDetail/" + boardNo;
	}
	
	//??? ?????? ??????
	@PostMapping("/boardDelete")
	public String boardDelete(int boardNo, int boardCategoryNo) {
		boardService.delete(boardNo);
		
		return "redirect:/board/boardList/" + boardCategoryNo;
	}
	
	// ???????????? ?????????
	@GetMapping("/noticeList")
	@ResponseBody
	public List<NoticeVO> noticeList() {
		System.out.println("noticeList :" + boardService.noticeList() );
		return boardService.noticeList();
	}
	
	// ????????????????????? ??????
	@GetMapping("/notice")
	public void notice(Model model) {
		model.addAttribute("total", boardService.getNoticeTotal());
	}
	
	// ???????????? ?????????
	@GetMapping("/noticeLists")
	@ResponseBody
	public List<NoticeVO> noticeLists(PageVO paging) {
		
		paging.setCpp(10);
		System.out.println("noticeLists :" + boardService.noticeLists(paging) );
		return boardService.noticeLists(paging);
	}
	
	@PostMapping(value="/uploadSummernoteImageFile", produces = "application/json")
	@ResponseBody
	public String uploadSummernoteImageFile(@RequestParam("file") MultipartFile multipartFile,
										String categoryNo) {
		log.info("uploadSummernoteImageFile POST : " + multipartFile);
		log.info("categoryNo : " + categoryNo);
		
		JsonObject jsonObject = new JsonObject();
		
		String fileRoot = "c:/imgduck/temp/";	//????????? ?????? ?????? ??????
		String originalFileName = multipartFile.getOriginalFilename();	//???????????? ?????????
		String extension = originalFileName.substring(originalFileName.lastIndexOf("."));	//?????? ????????? 
				
		categoryNo = categoryNo.length() == 1 ? "0" + categoryNo : categoryNo;
		
		String uuid = UUID.randomUUID().toString().replaceAll("-", "");
		String savedFileName = uuid + "(BN-_-_-CN" + categoryNo + ")" + extension;	//????????? ?????????
		
		File targetFile = new File(fileRoot + savedFileName);	
		
		try { 
			InputStream fileStream = multipartFile.getInputStream();
			FileUtils.copyInputStreamToFile(fileStream, targetFile);	//?????? ??????
			jsonObject.addProperty("url", "/board/summernoteImage/"+savedFileName);
			jsonObject.addProperty("responseCode", "success");
				
		} catch (IOException e) {
			FileUtils.deleteQuietly(targetFile);	//????????? ?????? ??????
			jsonObject.addProperty("responseCode", "error");
			e.printStackTrace();
		}
		
		String str = jsonObject.toString();
		System.out.println("????????? json ?????????: " + str);
		
		return str;
	}
	
	
	@GetMapping("/summernoteImage/{savedFileName}")
	@ResponseBody
	public ResponseEntity<byte[]> getImg(@PathVariable String savedFileName, HttpServletRequest request, HttpServletResponse response) {
		String reqUri = request.getRequestURI();
		System.out.println("?????? URI: " + reqUri);
		System.out.println("???????????? ????????? ?????? ??????!");
		System.out.println("param: " + savedFileName);
		String fileRoot = "c:/imgduck/temp/";
		String filePath = fileRoot + savedFileName;
		System.out.println("????????? ?????? ??????: " + filePath);
		File file = new File(filePath);
		
		ResponseEntity<byte[]> result = null;
		HttpHeaders headers = new HttpHeaders();
		
		try {
			headers.add("Content-Type", Files.probeContentType(file.toPath()));
			result = new ResponseEntity<byte[]>(FileCopyUtils.copyToByteArray(file), headers, HttpStatus.OK);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return result;
		
	}

	@GetMapping("/getImg/{savedFileName}")
	public ResponseEntity<byte[]> getImgCopy(@PathVariable String savedFileName, HttpServletResponse response){
	  
	  String fileRoot = "c:/imgduck/board/";
	  String filePath = fileRoot + savedFileName; 
	  File file = new File(filePath);
		
		ResponseEntity<byte[]> result = null;
		HttpHeaders headers = new HttpHeaders();
		
		try {
			headers.add("Content-Type", Files.probeContentType(file.toPath()));
			result = new ResponseEntity<byte[]>(FileCopyUtils.copyToByteArray(file), headers, HttpStatus.OK);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return result;
	}
	
	//???????????? ?????? ??????
	@PostMapping("/tempDelete")
	@ResponseBody
	public void tempDelete(@RequestBody List<String> list) {
		System.out.println("?????? ?????? ?????? ??????!");
		System.out.println("deleteFiles: " + list);
		
		for(String fileName : list) {
			String tempRoot = "c:/imgduck/temp/";
			File file = new File(tempRoot + fileName);
			if(file.exists()) {
				System.out.println("?????? ?????? ?????? ??????!");
				file.delete();
			}
			
		}	
	}
	@GetMapping("boardMyList")
	@ResponseBody
	public List<BoardVO> myList(PageVO paging, HttpSession session) {
		log.info("GET boardMyList ??????");
		int userNo = ((UserVO)session.getAttribute("login")).getUserNo();
		Map<String, Object> data = new HashMap<String, Object>();
		paging.setCpp(20);
		data.put("paging", paging);
		data.put("userNo", userNo);
		List<BoardVO> list= boardService.getMyList(data);
		
		System.out.println("????????? ?????????:"+list);
		
		return list;
	}

}