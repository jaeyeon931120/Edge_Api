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

import egovframework.kevinlab.mapper.api.EnergyStatusMapper;
import egovframework.kevinlab.service.api.EnergyStatusService;

@Service("EnergyStatusService")
public class EnergyStatusServiceImpl implements EnergyStatusService {

	@Resource(name = "energystatusMapper")
	private EnergyStatusMapper energystatusMapper;

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
	 * 에너지현황 전체 데이터 추출 후 에너지현황 페이지로 반환
	 *
	 * @param HashMap
	 *
	 * @return List<Map>
	 */
	@Override
	public List<Map<String, Object>> energyStatus_data(HashMap<String, Object> param) throws Exception {

		Map<String, Object> hourdata = new HashMap<String, Object>(); // 현재 데이터 (시간데이터)
		List<Map<String, Object>> returndata = new ArrayList<Map<String, Object>>(); // return 데이터 (전체데이터)
		List<Map<String, Object>> fivemindata = new ArrayList<Map<String, Object>>(); // 현재 데이터(5분데이터)

		try {

			hourdata = energy_hour_data(param);
			fivemindata = energy_5min_data(param);

			returndata.add(hourdata);
			returndata.addAll(fivemindata);

		} catch (Exception e) {
			e.printStackTrace();
		}
		return returndata;
	}
	
	/**
	 * 에너지현황 지역코드별 층수 반환
	 *
	 * @param HashMap
	 *
	 * @return String[]
	 */
	@Override
	public String[] energy_floor_info(HashMap<String, Object> param) throws Exception {

		HashMap<String, Object> parsingdata = new HashMap<String, Object>(); // 받아온 맵(param)을 훼손하지 않고 sql에 파싱하기위한 맵
		
		List<Map<String, Object>> data = new ArrayList<Map<String, Object>>(); // 반환 데이터(층별정보)
		Map<String, Object> e_data = new HashMap<String, Object>(); // 오류시 반환 데이터
		
		String complexcode;
		String floor;
		String result[] = null;
		
		if(param.get("complexcode") != null) {
			complexcode = param.get("complexcode").toString();
		} else {
			e_data.put("error", "complexcode가 없습니다. complexcode는 필수 키 입니다.");
			e_data.put("result", "-9");
			data.add(e_data);
			result = new String[1];
			result[0] = data.toString();
			return result;
		}

		try {
			parsingdata.putAll(param);
			parsingdata.put("complexcode", complexcode);
			parsingdata.put("dbname", DBname()); // DB이름을 저장

			data = this.energystatusMapper.energystatusFloor(parsingdata);
			
			result = new String[data.size()];
			
			for (int i = 0; i < data.size(); i++) {
				floor = data.get(i).get("HOME_GRP_PK").toString();
				result[i] = floor;
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	/**
	 * 에너지현황 시간단위 데이터를 추출 후 반환
	 *
	 * @param HashMap
	 *
	 * @return Map
	 */
	@Override
	public Map<String, Object> energy_hour_data(HashMap<String, Object> param) throws Exception {

		HashMap<String, Object> parsingdata = new HashMap<String, Object>(); // 받아온 맵(param)을 훼손하지 않고 sql에 파싱하기위한 맵
		Map<String, Object> data = new HashMap<String, Object>(); // 현재 데이터 (태양광)

		SimpleDateFormat formatter1 = new SimpleDateFormat("yyyyMMdd"); // sql에 시간을 넘겨주기 위해서 포맷설정(연도,월,일) - 시작 fromdate부분
		Calendar calF = Calendar.getInstance(Locale.KOREA); // 오늘 날짜
		String fromdate = formatter1.format(calF.getTime());
		String complexcode = null;
		String floor = null;

		if(param.get("complexcode") != null) {
			complexcode = param.get("complexcode").toString();
			floor = complexcode + "_ALL";
			if(param.get("floor") != null) {
				floor = param.get("floor").toString();
				floor = complexcode + "_" + floor;
			}
		} else {
			data.put("error", "complexcode가 없습니다. complexcode는 필수 키 입니다.");
			data.put("result", "-9");
			return data;
		}

		try {
			parsingdata.putAll(param);
			parsingdata.put("fromdate", fromdate);
			parsingdata.put("complexcode", complexcode);
			parsingdata.put("floor", floor);
			parsingdata.put("dbname", DBname()); // DB이름을 저장

			data = this.energystatusMapper.energystatusHourdata(parsingdata);

		} catch (Exception e) {
			e.printStackTrace();
		}
		return data;
	}

	/**
	 * 에너지현황 선택한 시간의 5분단위 데이터를 추출 후 반환
	 *
	 * @param HashMap
	 *
	 * @return List<Map>
	 */
	@Override
	public List<Map<String, Object>> energy_5min_data(HashMap<String, Object> param) throws Exception {

		HashMap<String, Object> parsingdata = new HashMap<String, Object>(); // 받아온 맵(param)을 훼손하지 않고 sql에 파싱하기위한 맵
		List<Map<String, Object>> data = new ArrayList<Map<String, Object>>(); // 현재 데이터
		Map<String, Object> e_data = new HashMap<String, Object>(); // 오류시 반환 데이터

		SimpleDateFormat formatter1 = new SimpleDateFormat("yyyyMMddHHmm"); // sql에 시간을 넘겨주기 위해서 포맷설정(연도,월,일,시간,분,초) -
		SimpleDateFormat formatter2 = new SimpleDateFormat("yyyyMMddHH"); // 분초 변경을 위한 포맷설정(연도,월,일,시간) - 시작 fromdate부분
		SimpleDateFormat formatter3 = new SimpleDateFormat("yyyyMMddHHmmss"); // 처음 시간을 설정을 위한 포맷

		String fromdate = "";
		String fiveminfromdate = "";
		String todate = "";
		String complexcode = "";
		String floor = null;

		if(param.get("fromdate") != null) {
			fromdate = param.get("fromdate").toString();
		} else {
			e_data.put("error", "fromdate(시작시간)가 없습니다. 5분 검색시 시작시간이 있어야 검색이 가능합니다.");
			e_data.put("result", "-9");
			data.add(e_data);
			return data;
		}
		if(param.get("complexcode") != null) {
			complexcode = param.get("complexcode").toString();
			floor = complexcode + "_ALL";
			if(param.get("floor") != null) {
				floor = param.get("floor").toString();
				floor = complexcode + "_" + floor;
			}
		} else {
			e_data.put("error", "complexcode가 없습니다. complexcode는 필수 키 입니다.");
			e_data.put("result", "-9");
			data.add(e_data);
			return data;
		}

		Calendar calF = Calendar.getInstance(Locale.KOREA); // 받아온 시간에서 분초 변경하고 5분을 빼기위해서 필요해서 선언
		Date fdate = calF.getTime(); // Date로 형변환하기 위한 준비
		if (fromdate.length() >= 14) {
			fdate = formatter3.parse(fromdate); // Date로 형변환
		} else if (fromdate.length() >= 10) {
			fdate = formatter2.parse(fromdate); // Date로 형변환
		} else {
			fdate = formatter2.parse(fromdate.substring(0, 10));
		}
		fromdate = formatter2.format(fdate) + "0000";
		calF.setTime(fdate); // 받아온 시간으로 설정
		calF.add(Calendar.HOUR, 1); // 1시간 더하기
		todate = formatter2.format(calF.getTime()) + "0000";
		calF.setTime(fdate); // 받아온 시간으로 설정
		calF.add(Calendar.MINUTE, -5); // 5분빼기
		fiveminfromdate = formatter1.format(calF.getTime()) + "00"; // param에 담기위해 스트링으로 포맷변환

		try {
			parsingdata.putAll(param);
			parsingdata.put("fromdate", fromdate);
			parsingdata.put("todate", todate);
			parsingdata.put("fiveminfromdate", fiveminfromdate);
			parsingdata.put("complexcode", complexcode);
			parsingdata.put("floor", floor);
			parsingdata.put("dbname", DBname()); // DB이름을 저장

			data = this.energystatusMapper.energystatus5mindata(parsingdata);

		} catch (Exception e) {
			e.printStackTrace();
		}
		return data;
	}
}