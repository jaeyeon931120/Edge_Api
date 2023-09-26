package egovframework.kevinlab.service.impl.api;

import java.io.Reader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import javax.annotation.Resource;

import org.apache.ibatis.io.Resources;
import org.springframework.stereotype.Service;

import egovframework.kevinlab.mapper.api.SystemLogMapper;
import egovframework.kevinlab.service.api.SystemLogService;

@Service("SystemLogService")
public class SystemLogServiceImpl implements SystemLogService {

	@Resource(name = "systemlogMapper")
	private SystemLogMapper systemlogMapper;

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
	
	/**
     * 시스템 로그 작성해서 시스템 로그 테이블에 insert
     *
     * @param 각 시스템별 로그값
     *
     * @return 없음
     */
	@Override
	public void Edge_Systemlog_Insert(Map<String, Object> param) throws Exception {
		HashMap<String, Object> parsingdata = new HashMap<String, Object>(); // sql에 필요한 조건을 넘겨주는 맵
		Map<String, Object> insertdata1 = new HashMap<String, Object>(); // sql에 필요한 조건을 넘겨주는 맵
		Map<String, Object> insertdata2 = new HashMap<String, Object>(); // sql에 필요한 조건을 넘겨주는 맵
		
		try {
			SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss"); // sql에 시간을 넘겨주기 위해서 포맷설정(연도,월,일) - valdate부분
			Calendar calF = Calendar.getInstance(Locale.KOREA); // 오늘 날짜
			String date = formatter.format(calF.getTime());
			
			parsingdata.putAll(param);
			parsingdata.put("val_date", date);
			
			List<String> keyList = new ArrayList<>(parsingdata.keySet());
			List<Object> valueList = new ArrayList<>(parsingdata.values());
			List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
			
			for(int i=0; i<keyList.size(); i++) {
				insertdata1 = new HashMap<String, Object>();
				
				insertdata1.put("name", keyList.get(i));
				insertdata1.put("val", valueList.get(i));
				
				result.add(insertdata1);
			}
			
			insertdata2.put("result", result);
			insertdata2.put("dbname", DBname()); // DB이름 저장

			this.systemlogMapper.systemLogInsert(insertdata2);
			
			
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
     * 시스템 로그 오류 작성해서 시스템 로그 테이블에 update
     *
     * @param 각 시스템별 오류로그값
     *
     * @return 없음
     */
	@Override
	public void Edge_Systemlog_Update(Map<String, Object> param) throws Exception {
		HashMap<String, Object> parsingdata = new HashMap<String, Object>(); // sql에 필요한 조건을 넘겨주는 맵
		
		try {
			parsingdata.putAll(param);

			this.systemlogMapper.systemLogUpdate(parsingdata);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
     * Api(프론트단)에서 원하는 조건에 맞춘 시스템 로그 데이터 반환
     *
     * @param Edge 프론트단에서 받아온 조건
     *
     * @return List<map>
     */
	@Override
	public List<Map<String, Object>> Edge_SystemLog_Data(HashMap<String, Object> param) throws Exception {

		HashMap<String, Object> parsingdata = new HashMap<String, Object>(); // 받아온 맵(param)을 훼손하지 않고 sql에 파싱하기위한 맵
		List<Map<String, Object>> data = new ArrayList<Map<String, Object>>(); // 현재 데이터
		Map<String, Object> e_data = new HashMap<String, Object>(); // 오류시 반환 데이터
		List<Map<String, Object>> returndata = new ArrayList<Map<String, Object>>(); // 반환할 데이터
		
		// param에서 받아온 값 및 sql에 넘길 조건들 선언 및 초기화
		String fromdate = null;
		String todate = null;
		String complexcode = "2002";
		String result = null;
		String type = null;

		if(param.get("fromdate") != null) { // param에 fromdate가 null이 아닐 경우
			fromdate = param.get("fromdate").toString(); // fromdate의 값을 저장
		} else { // param에 fromdate의 값이 null일 경우
			e_data.put("result", "nok"); // 에러 저장
			e_data.put("reason", "fromdate(시작날짜)가 없습니다. 로그 검색 시작날짜가 있어야 검색이 가능합니다."); // 에러의 이유를 저장 e_data에 저장
			returndata.add(e_data); // returndata(리스트 맵)에 e_data 저장
			return returndata; // returndata 반환
		}
		if(param.get("todate") != null) {
			todate = param.get("todate").toString();
		} else {
			e_data.put("result", "nok");
			e_data.put("reason", "todate(종료날짜)가 없습니다. 로그 검색 종료날짜가 있어야 검색이 가능합니다.");
			returndata.add(e_data);
			return returndata;
		}
		if(param.get("complexcode") != null) {
			complexcode = param.get("complexcode").toString();
		} else {
			e_data.put("result", "nok");
			e_data.put("reason", "complexcode가 없습니다. complexcode는 필수 키 입니다.");
			returndata.add(e_data);
			return returndata;
		}
		if(param.get("result") != null) {
			result = param.get("result").toString();
		}
		if(param.get("work_type") != null) {
			type = param.get("work_type").toString();
		}
		
		if(fromdate.length() >= 8) {
			fromdate = fromdate.substring(0,8);
		}
		if(todate.length() >= 8) {
			todate = todate.substring(0,8);
		}
		
		fromdate = fromdate + "000000";
		todate = todate + "235959";

		try {
			parsingdata.putAll(param);
			parsingdata.put("fromdate", fromdate);
			parsingdata.put("todate", todate);
			parsingdata.put("complexcode", complexcode);
			parsingdata.put("result", result);
			parsingdata.put("work_type", type);
			parsingdata.put("dbname", DBname()); // DB이름을 저장

			data = this.systemlogMapper.systemLogData(parsingdata);
			
			if (data != null && !data.isEmpty()) {
				for (int i = 0; i < data.size(); i++) {
					returndata.add(data.get(i));
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return returndata;
	}
}