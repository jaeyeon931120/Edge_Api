package egovframework.kevinlab.service.impl.api;

import java.io.Reader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import org.apache.ibatis.io.Resources;
import org.springframework.stereotype.Service;

import egovframework.kevinlab.mapper.api.EquipmentMapper;
import egovframework.kevinlab.service.api.EquipmentService;

@Service("EquipmentService")
public class EquipmentServiceImpl implements EquipmentService {

	@Resource(name = "equipmentMapper")
	private EquipmentMapper equipmentMapper;

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

	// api에 응답하기위한 전체데이터
	@Override
	public List<Map<String, Object>> equipment_All_Data(HashMap<String, Object> param) throws Exception {

		Map<String, Object> hourdata = new HashMap<String, Object>(); // 현재 데이터 (시간데이터)
		List<Map<String, Object>> returndata = new ArrayList<Map<String, Object>>(); // return 데이터 (전체데이터)
		List<Map<String, Object>> fivemindata = new ArrayList<Map<String, Object>>(); // 현재 데이터(5분데이터)

		try {

			hourdata = equipmentData(param);
			fivemindata = equipmentGraph(param);

			returndata.add(hourdata);
			returndata.addAll(fivemindata);

		} catch (Exception e) {
			e.printStackTrace();
		}
		return returndata;
	}

	@Override
	public Map<String, Object> equipmentData(HashMap<String, Object> param) throws Exception {

		HashMap<String, Object> parsingdata = new HashMap<String, Object>(); // 받아온 맵(param)을 훼손하지 않고 sql에 파싱하기위한 맵
		Map<String, Object> data = new HashMap<String, Object>(); // 현재 데이터 (태양광)

		SimpleDateFormat formatter1 = new SimpleDateFormat("yyyyMMdd"); // sql에 시간을 넘겨주기 위해서 포맷설정(연도,월,일) - 시작
																		// fromdate부분
		Calendar calF = Calendar.getInstance(Locale.KOREA); // 오늘 날짜
		String fromdate = formatter1.format(calF.getTime()) + "000000";
		String todate = formatter1.format(calF.getTime()) + "235959";
		String complexcode = "";

		if (param.get("complexcode") != null) {
			complexcode = param.get("complexcode").toString();
		} else {
			data.put("error", "complexcode가 없습니다. complexcode는 필수 키 입니다.");
			data.put("result", "-9");
			return data;
		}

		try {
			parsingdata.putAll(param);
			parsingdata.put("fromdate", fromdate);
			parsingdata.put("todate", todate);
			parsingdata.put("complexcode", complexcode);
			parsingdata.put("dbname", DBname()); // DB이름을 저장

			data = this.equipmentMapper.equipmentData(parsingdata);

		} catch (Exception e) {
			e.printStackTrace();
		}
		return data;
	}

	@Override
	public List<Map<String, Object>> equipmentGraph(HashMap<String, Object> param) throws Exception {

		HashMap<String, Object> parsingdata = new HashMap<String, Object>(); // 받아온 맵(param)을 훼손하지 않고 sql에 파싱하기위한 맵
		List<Map<String, Object>> data = new ArrayList<Map<String, Object>>(); // 현재 데이터
		Map<String, Object> e_data = new HashMap<String, Object>(); // 오류시 반환 데이터
		List<Map<String, Object>> returndata = new ArrayList<Map<String, Object>>(); // 반환할 데이터

		SimpleDateFormat formatter1 = new SimpleDateFormat("yyyyMMdd"); // sql에 시간을 넘겨주기 위해서 포맷설정(연도,월,일,시간,분,초) -

		String fromdate = "";
		String complexcode = "";

		if (param.get("complexcode") != null) {
			complexcode = param.get("complexcode").toString();
		} else {
			e_data.put("error", "complexcode가 없습니다. complexcode는 필수 키 입니다.");
			e_data.put("result", "-9");
			returndata.add(e_data);
			return returndata;
		}

		Calendar calF = Calendar.getInstance(Locale.KOREA); // 받아온 시간에서 분초 변경하고 5분을 빼기위해서 필요해서 선언
		Date fdate = calF.getTime(); // Date로 형변환하기 위한 준비
		fromdate = formatter1.format(fdate);

		try {
			parsingdata.putAll(param);
			parsingdata.put("fromdate", fromdate);
			parsingdata.put("complexcode", complexcode);
			parsingdata.put("dbname", DBname()); // DB이름을 저장

			data = this.equipmentMapper.equipmentGraph(parsingdata);
			
			for (int i = 0; i < data.size(); i++) {
				returndata.add(data.get(i));
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return returndata;
	}
}