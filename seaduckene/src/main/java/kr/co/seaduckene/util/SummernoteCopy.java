package kr.co.seaduckene.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

@Service
public class SummernoteCopy {

	public Map<String, Object> summerCopy(List<String> summerfileNames, int boardNo) throws Exception {

		Map<String, Object> result = new HashMap<String, Object>();

		String boardNoStr = Integer.toString(boardNo);
		
		if(summerfileNames != null ) {
			for (int i = 0; i < summerfileNames.size(); i++) {
				String oriFilePath = "c:/imgduck/temp/" + summerfileNames.get(i);

				// 복사될 파일경로
				String copyFilePath = ("c:/imgduck/board/" + summerfileNames.get(i)).replace("_", boardNoStr);

				try {
					// 파일 이름은 안 건드리고 내부 byte[]만 복사한다
					FileInputStream fis = new FileInputStream(oriFilePath); // 읽을파일
					FileOutputStream fos = new FileOutputStream(copyFilePath); // 복사할파일
					int data = 0;
					while ((data = fis.read()) != -1) {
						fos.write(data);
					}
					fis.close();
					fos.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
				
				File delFile = new File(oriFilePath);
				
				if(delFile.exists()) {
					delFile.delete();
				}
				
			}

		}
		result.put("SUCCESS", true);
		return result;
	}
}
