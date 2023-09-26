package egovframework.kevinlab.service.impl.api;

import java.io.Reader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.annotation.Resource;

import org.apache.ibatis.io.Resources;
import org.springframework.stereotype.Service;

import egovframework.kevinlab.mapper.api.DashBoardMapper;
import egovframework.kevinlab.mapper.api.PredictionMapper;
import egovframework.kevinlab.service.api.PredictionService;

@Service("PredictionService")
public class PredictionServiceImpl implements PredictionService {

	@Resource(name = "predictionMapper")
	private PredictionMapper predictionMapper;

	@Resource(name = "dashboardMapper")
	private DashBoardMapper dashboardMapper;
	
	/**
	 * 접속하는 DB이름(스키마)를 추출
	 *
	 * @param void
	 *
	 * @return String
	 */
	public String DBname() throws Exception {
		String resource = "egovframework/properties/kevinlab.properties"; // properties설정파일위치
        Properties properties = new Properties(); // properties 읽어오기 위한 properties 객체 생성
        
        Reader reader = Resources.getResourceAsReader(resource); // properties 읽어오기
        properties.load(reader); // 읽은 값을 properties객체에 저장
        
        String dbnames[] = properties.getProperty("db.url").split("/"); // db url을 /로 구분하여 배열 생성
        
        String dbname = dbnames[dbnames.length-1]; // 배열의 마지막 값을 변수에 저장
        
        return dbname;
	}
		
	@Override
	public List<Map<String, Object>> predictionDataTop(HashMap<String, Object> param) throws Exception {
			
		List<Map<String, Object>> data = new ArrayList<Map<String, Object>>(); // 현재 데이터
		HashMap<String, Object> parsingdata = new HashMap<String, Object>(); // 받아온 맵(param)을 훼손하지 않고 sql에 파싱하기위한 맵

		SimpleDateFormat formatter1 = new SimpleDateFormat("yyyyMMdd"); // sql에 시간을 넘겨주기 위해서 포맷설정(연도,월,일) - 시작																			// fromdate부분
		SimpleDateFormat formatter2 = new SimpleDateFormat("yyyyMMddHHmmss"); // sql에 시간을 넘겨주기 위해서 포맷설정(연도,월,일,시간,분,초) -
																				// 끝 todate부분
		Calendar calF = Calendar.getInstance(); // 오늘 날짜
		Calendar calF2 = Calendar.getInstance(); // 전일 날짜
		calF2.add(Calendar.DATE, -1);
		String fromdate = formatter1.format(calF.getTime()); // 현재일자 00시부터 시작
		String todate = formatter2.format(calF.getTime());
		String prevfromdate = formatter1.format(calF2.getTime()); // 전일날짜 00시부터 시작
		String complexcode = null;;
		String floor = null;

		if(param.get("complexcode") != null) {
			complexcode = param.get("complexcode").toString();
			floor = complexcode + "_ALL";
			if(param.get("floor") != null) {
				floor = param.get("floor").toString();
				floor = complexcode + "_" + floor;
			}
		}

		try {
			parsingdata.putAll(param);
			parsingdata.put("fromdate", fromdate);
			parsingdata.put("todate", todate);
			parsingdata.put("prevfromdate", prevfromdate);
			parsingdata.put("complexcode", complexcode);
			parsingdata.put("floor", floor);

			data = this.predictionMapper.predictionTop(parsingdata);						
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		return data;
	}
	
	
	@Override
	public List<Map<String, Object>> predictionDataBottom(HashMap<String, Object> param) throws Exception {
		
		List<Map<String, Object>> data = new ArrayList<Map<String, Object>>(); // return 데이터 (전체데이터)
		HashMap<String, Object> parsingdata = new HashMap<String, Object>(); // 받아온 맵(param)을 훼손하지 않고 sql에 파싱하기위한 맵

		SimpleDateFormat formatter1 = new SimpleDateFormat("yyyyMMdd"); // sql에 시간을 넘겨주기 위해서 포맷설정(연도,월,일) - 시작 fromdate 부분																// fromdate부분
		SimpleDateFormat formatter2 = new SimpleDateFormat("yyyyMMddHHmmss"); // sql에 시간을 넘겨주기 위해서 포맷설정(연도,월,일,시간,분,초) - 끝 todate부분
		Calendar calF = Calendar.getInstance(); // 오늘 날짜
		Calendar calF2 = Calendar.getInstance(); // 전일 날짜
		calF2.add(Calendar.DATE, -1);
		String fromdate = formatter1.format(calF.getTime()); // 현재일자 00시부터 시작
		String todate = formatter2.format(calF.getTime());
		String prevfromdate = formatter1.format(calF2.getTime()); // 전일날짜 00시부터 시작
		String complexcode = null;;
		String floor = null;

		if(param.get("complexcode") != null) {
			complexcode = param.get("complexcode").toString();
			floor = complexcode + "_ALL";
			if(param.get("floor") != null) {
				floor = param.get("floor").toString();
				floor = complexcode + "_" + floor;
			}
		}

		try {
			parsingdata.putAll(param);
			parsingdata.put("fromdate", fromdate);
			parsingdata.put("todate", todate);
			parsingdata.put("prevfromdate", prevfromdate);
			parsingdata.put("complexcode", complexcode);
			parsingdata.put("floor", floor);

			data = this.predictionMapper.predictionBottom(parsingdata);									
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		return data;
	}
	
		 
}