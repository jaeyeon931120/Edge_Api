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

import egovframework.kevinlab.mapper.api.DashBoardMapper;
import egovframework.kevinlab.mapper.api.EdgeDBMapper;
import egovframework.kevinlab.service.api.DashBoardService;

@Service("DashBoardService")
public class DashBoardServiceImpl implements DashBoardService {

	@Resource(name = "dashboardMapper")
	private DashBoardMapper dashboardMapper;
	
	@Resource(name = "edgedbMapper")
	private EdgeDBMapper edgeDBMapper;
	
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
     * Edge 대시보드의 전체 데이터
     *
     * @param 프론트단에서 넘겨준 조건
     *
     * @return List<Map<String, Object>>
     */
	@Override
	public List<Map<String, Object>> dashboard_data(HashMap<String, Object> param) throws Exception {

		Map<String, Object> sunlightdata = new HashMap<String, Object>(); // 현재 데이터 (태양광)
		List<Map<String, Object>> realtimepowerdata = new ArrayList<Map<String, Object>>(); // 현재 데이터(실시간 전력사용량)
		Map<String, Object> floordata = new HashMap<String, Object>(); // 현재 데이터(층별)
		Map<String, Object> applicationdata = new HashMap<String, Object>(); // 현재 데이터(용도별)
		List<Map<String, Object>> returndata = new ArrayList<Map<String, Object>>(); // 전체 데이터

		try {

			sunlightdata = dash_sunlight(param);
			realtimepowerdata = dash_realtime_power(param);
			floordata = dash_floor(param);
			applicationdata = dash_application(param);

			returndata.add(sunlightdata);
			returndata.addAll(realtimepowerdata);
			returndata.add(floordata);
			returndata.add(applicationdata);

		} catch (Exception e) {
			e.printStackTrace();
		}
		return returndata;
	}
	
	/**
     * Edge 대시보드의 태양광 데이터
     *
     * @param 프론트단에서 넘겨준 조건
     *
     * @return Map<String, Object>
     */
	@Override
	public Map<String, Object> dash_sunlight(HashMap<String, Object> param) throws Exception {

		Map<String, Object> data = new HashMap<String, Object>(); // 현재 데이터 (태양광)
		HashMap<String, Object> parsingdata = new HashMap<String, Object>(); // 받아온 맵(param)을 훼손하지 않고 sql에 파싱하기위한 맵

		SimpleDateFormat formatter1 = new SimpleDateFormat("yyyyMMdd"); // sql에 시간을 넘겨주기 위해서 포맷설정(연도,월,일)
		SimpleDateFormat formatter2 = new SimpleDateFormat("yyyyMMddHHmmss"); // sql에 시간을 넘겨주기 위해서 포맷설정(연도,월,일,시간,분,초)
		Calendar calF = Calendar.getInstance(Locale.KOREA); // 오늘 날짜
		Calendar calF2 = Calendar.getInstance(Locale.KOREA); // 전일 날짜
		calF2.add(Calendar.DATE, -1); // 하루 감소
		String fromdate = formatter1.format(calF.getTime()) + "000000"; // 시작시간이기 때문에 000000(시,분,초) 추가
		String todate = formatter2.format(calF.getTime()); // 현재시각을 마지막 시간으로 설정
		String prevfromdate = formatter1.format(calF2.getTime()) + "000000"; // 시작시간이기 때문에 000000(시,분,초) 추가(전일이기 때문에 하루감소)
		String prevtodate = formatter2.format(calF2.getTime()); // 현재시각을 마지막 시간으로 설정(전일이기 때문에 하루감소)
		String complexcode = ""; // 받아온 complexcode를 저장하기 위한 변수

		if(param.get("complexcode") != null) {
			complexcode = param.get("complexcode").toString(); // 받아온 complexcode값을 저장
		} else {
			data.put("result", "nok");
			data.put("reason", "complexcode가 없습니다. complexcode는 필수 키 입니다.");
			return data;
		}

		try {
			// 프론트에서 받아온 값과 시간을 맵에 저장
			parsingdata.putAll(param);
			parsingdata.put("fromdate", fromdate);
			parsingdata.put("todate", todate);
			parsingdata.put("prevfromdate", prevfromdate);
			parsingdata.put("prevtodate", prevtodate);
			parsingdata.put("complexcode", complexcode);
			parsingdata.put("dbname", DBname()); // DB이름을 저장

			data = this.dashboardMapper.dashSunlightData(parsingdata); // 저장한 조건에 맞는 태양광 데이터 조회
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		return data;
	}
	
	/**
     * Edge 대시보드의 실시간 전력사용량
     *
     * @param 프론트단에서 넘겨준 조건
     *
     * @return List<Map<String, Object>>
     */
	@Override
	public List<Map<String, Object>> dash_realtime_power(HashMap<String, Object> param) throws Exception {

		List<Map<String, Object>> data = new ArrayList<Map<String, Object>>(); // 현재 데이터
		Map<String, Object> e_data = new HashMap<String, Object>(); // 오류시 반환 데이터
		List<Map<String, Object>> returndata = new ArrayList<Map<String, Object>>(); // 반환 데이터
		HashMap<String, Object> parsingdata = new HashMap<String, Object>(); // 받아온 맵(param)을 훼손하지 않고 sql에 파싱하기위한 맵

		SimpleDateFormat formatter1 = new SimpleDateFormat("yyyyMMdd"); // sql에 시간을 넘겨주기 위해서 포맷설정(연도,월,일)
		SimpleDateFormat formatter2 = new SimpleDateFormat("yyyyMMddHHmmss"); // sql에 시간을 넘겨주기 위해서 포맷설정(연도,월,일,시간,분,초)
		Calendar calF = Calendar.getInstance(Locale.KOREA); // 오늘 날짜
		Calendar calF2 = Calendar.getInstance(Locale.KOREA); // 전일 날짜
		calF2.add(Calendar.DATE, -1);
		String fromdate = formatter1.format(calF.getTime()) + "000000"; // 현재일자 00시부터 시작
		String todate = formatter2.format(calF.getTime()) + "235959"; // 현재일자 끝시간
		String prevfromdate = formatter1.format(calF2.getTime()); // 전일날짜 00시부터 시작
		String complexcode = null;
		String floor = null;

		if(param.get("complexcode") != null) {
			complexcode = param.get("complexcode").toString(); // complexcode의 값 저장
			floor = complexcode + "_ALL"; // 전체 값(이유: 층을 선택하지 않았을 경우)
			if(param.get("floor") != null) { // floor 값이 null이 아닐경우(전체값이 아닐경우)
				floor = param.get("floor").toString(); // floor의 저장
				floor = complexcode + "_" + floor; // floor의 값을 lbems센서 규격에 맞게 생성
			}
		} else {
			e_data.put("result", "nok");
			e_data.put("reason", "complexcode가 없습니다. complexcode는 필수 키 입니다.");
			returndata.add(e_data);
			return returndata;
		}

		try {
			// 프론트단에서 받아온 param값, 시간, complexcode, floor저장
			parsingdata.putAll(param);
			parsingdata.put("fromdate", fromdate);
			parsingdata.put("todate", todate);
			parsingdata.put("prevfromdate", prevfromdate);
			parsingdata.put("complexcode", complexcode);
			parsingdata.put("floor", floor);
			parsingdata.put("dbname", DBname()); // DB이름을 저장

			data = this.dashboardMapper.dashRealTimePower(parsingdata); // edge의 실시간 전력사용량 조회
			for (int i = 0; i < data.size(); i++) { // data크기만큼 반복
				data.get(i).put("TITLE", "전일"); // sql 값에 따라 첫번쨰는 전일값, 두번쨰는 금일값
				if (i % 2 == 0) {
					data.get(i).put("TITLE", "금일");
				}
				
				returndata.add(data.get(i)); // 반환할 리스트맵에 저장
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return returndata;
	}
	
	/**
     * Edge 대시보드의 층별 데이터
     *
     * @param 프론트단에서 넘겨준 조건
     *
     * @return Map<String, Object>
     */
	@Override
	public Map<String, Object> dash_floor(HashMap<String, Object> param) throws Exception {

		List<Map<String, Object>> data = new ArrayList<Map<String, Object>>(); // 현재 데이터
		List<Map<String, Object>> sensordata = new ArrayList<Map<String, Object>>(); // 현재 데이터
		Map<String, Object> returndata = new HashMap<String, Object>(); // 가져온 데이터를 기반으로 층별 총사용량 입력후 response
		HashMap<String, Object> parsingdata = new HashMap<String, Object>(); // 받아온 맵(param)을 훼손하지 않고 sql에 파싱하기위한 맵
		List<String> sensorlist = new ArrayList<String>(); // 센서를 저장할 리스트

		SimpleDateFormat formatter1 = new SimpleDateFormat("yyyyMMdd"); // sql에 시간을 넘겨주기 위해서 포맷설정(연도,월,일)
		SimpleDateFormat formatter2 = new SimpleDateFormat("yyyyMMddHHmmss"); // sql에 시간을 넘겨주기 위해서 포맷설정(연도,월,일,시간,분,초)
		Calendar calF = Calendar.getInstance(Locale.KOREA); // 오늘 날짜
		Calendar calF2 = Calendar.getInstance(Locale.KOREA); // 전일 날짜
		calF2.add(Calendar.DATE, -1);
		String fromdate = formatter1.format(calF.getTime()) + "000000"; // 현재일자 00시부터 시작
		String todate = formatter2.format(calF.getTime()) + "235959"; // 현재일자 끝시간
		String prevfromdate = formatter1.format(calF2.getTime()); // 전일날짜 00시부터 시작
		String complexcode = null;

		if(param.get("complexcode") != null) {
			complexcode = param.get("complexcode").toString(); // 받아온 complexcode값 저장
		} else {
			returndata.put("result", "nok");
			returndata.put("reason", "complexcode가 없습니다. complexcode는 필수 키 입니다.");
			return returndata;
		}

		try {
			String key_pk = "ELECTRIC_" + complexcode + "_";
			parsingdata.put("complexcode", complexcode);
			parsingdata.put("key_pk", key_pk);
			parsingdata.put("sensor", "bems_sensor_electric");
			parsingdata.put("dbname", DBname()); // DB이름을 저장
			
			sensordata = this.edgeDBMapper.getAllSensorName(parsingdata);
			
			if (sensordata != null && !sensordata.isEmpty()) {
				for (int i = 0; i < sensordata.size(); i++) {
					String sensor = sensordata.get(i).get("sensor_sn").toString();
					sensorlist.add(sensor);
				}
			}
			
			parsingdata = new HashMap<String, Object>(); // 맵 초기화
			
			// 프론트단에서 받아온 param값 저장 및 각 시간값 parsingdata에 저장
			parsingdata.putAll(param);
			parsingdata.put("fromdate", fromdate);
			parsingdata.put("todate", todate);
			parsingdata.put("prevfromdate", prevfromdate);
			parsingdata.put("complexcode", complexcode);
			parsingdata.put("sensor", sensorlist);
			parsingdata.put("dbname", DBname()); // DB이름을 저장

			data = this.dashboardMapper.dashFloor(parsingdata); // 받아온 조건에 맞는 각 층별 전력량 검색

			if (data != null) { // data값이 null이 아닐경우
				for (int i = 0; i < data.size(); i++) { // data 크기만큼 반복
					String homekeypk = data.get(i).get("HOME_KEY_PK").toString(); // 각 층별 전체전력을 구분하기 위한 층별 이름 구분을 위한 구분자
					homekeypk = homekeypk.replace(key_pk, "");
					if(!homekeypk.equals("ALL")) {
						returndata.put(homekeypk, data.get(i).get("VAL"));
					}
				}
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		return returndata;
	}
	
	/**
     * Edge 대시보드의 용도별 사용량 데이터
     *
     * @param 프론트단에서 넘겨준 조건
     *
     * @return Map<String, Object>
     */
	@Override
	public Map<String, Object> dash_application(HashMap<String, Object> param) throws Exception {

		Map<String, Object> data = new HashMap<String, Object>(); // 현재 데이터
		Map<String, Object> returndata = new HashMap<String, Object>(); // 반환 데이터
		List<Map<String, Object>> sensordata = new ArrayList<Map<String, Object>>(); // 센서 이름
		HashMap<String, Object> parsingdata = new HashMap<String, Object>(); // 받아온 맵(param)을 훼손하지 않고 sql에 파싱하기위한 맵

		SimpleDateFormat formatter1 = new SimpleDateFormat("yyyyMMdd"); // sql에 시간을 넘겨주기 위해서 포맷설정(연도,월,일)
		Calendar calF = Calendar.getInstance(Locale.KOREA); // 오늘 날짜
		String today = formatter1.format(calF.getTime());
		String todayfromdate = formatter1.format(calF.getTime()) + "%"; // 오늘날짜로 저장(+"%"를 하는 이유는 like로 검색하기 때문에)
		calF.add(Calendar.DATE, -1); // 하루전
		String fromdate = formatter1.format(calF.getTime()) + "%"; // 전일날짜 00시부터 시작
		String complexcode = "";
		double alldata = 0; // 전체 사용량
		double heatcold = 0; // 냉방 사용량

		if(param.get("complexcode") != null) {
			complexcode = param.get("complexcode").toString(); // complexcode값 저장
		} else {
			data.put("result", "nok");
			data.put("reason", "complexcode가 없습니다. complexcode는 필수 키 입니다.");
			return data;
		}

		try {
			
			// 각 row테이블의 센서테이블 배열 정의
			String[] Sensorlist = { "bems_sensor_electric_elechot", "bems_sensor_electric_hotwater", "bems_sensor_electric_heating",
					"bems_sensor_electric_cold", "bems_sensor_electric_light", "bems_sensor_electric_vent", "bems_sensor_electric_elevator",
					"bems_sensor_electric_water"};
			// 각 row테이블의 이름으로 된 배열 정의
			String[] DBlist = {"bems_meter_electric_elechot", "bems_meter_electric_hotwater", "bems_meter_electric_heating",
					"bems_meter_electric_cold", "bems_meter_electric_light", "bems_meter_electric_vent",
					"bems_meter_electric_elevator", "bems_meter_electric_water"};
			String[] namelist1 = {"전열", "급탕", "난방", "냉방", "전등", "환기", "승강", "동력(펌프)"};
			String[] namelist2 = {"전열", "급탕", "냉방", "전등", "환기", "승강", "동력(펌프)"};
			
			for (int i = 0; i < Sensorlist.length; i++) {
				List<String> sensorlist = new ArrayList<String>(); // 리스트 변수 선언 및 초기화
				parsingdata = new HashMap<String, Object>(); // 맵 초기화
				sensordata = new ArrayList<Map<String, Object>>(); // 맵 초기화
				
				parsingdata.put("sensor", Sensorlist[i]); // 센서 테이블 입력
				parsingdata.put("complexcode", complexcode); // complexcode 값 저장
				parsingdata.put("dbname", DBname()); // DB이름을 저장
				sensordata = this.dashboardMapper.edgeSensorName(parsingdata); // 각 테이블에 해당되는 센서 검색
				
				if (!sensordata.isEmpty()) { // 센서데이터가 존재할 경우
					for (int j = 0; j < sensordata.size(); j++) { // 센서데이터 사이즈만큼 반복
						String sensor = sensordata.get(j).get("sensor_sn").toString(); // 가져온 센서를 변수에 저장
						sensorlist.add(sensor); // 리스트에 센서 저장
					}
				}
				
				parsingdata = new HashMap<String, Object>(); // 맵 초기화
				data = new HashMap<String, Object>(); // 맵 초기화
				
				parsingdata.putAll(param); // 프론트에서 받아온 조건 parsingdata에 저장
				parsingdata.put("sensor", Sensorlist[i]); // 센서테이블 저장
				parsingdata.put("meter", DBlist[i]); // 미터 테이블 저장
				parsingdata.put("fromdate", fromdate); // 어제 시작시간 저장
				parsingdata.put("todayfromdate", todayfromdate); // 현재시간 저장
				parsingdata.put("complexcode", complexcode); // 지역코드 저장
				parsingdata.put("sensorlist", sensorlist); // 각 미터테이블별 센서리스트 저장
				parsingdata.put("dbname", DBname()); // DB이름을 저장
				
				if (!sensorlist.isEmpty()) { // 센서리스트가 0이 아닌경우
					data = this.dashboardMapper.dashApplication(parsingdata); // 용도별 사용량 검색
				}
				
				if(data != null && !data.isEmpty()) { // data의 값이 null이 아니거나 존재 할 경우
					if (DBlist[i].equals("bems_meter_electric_heating") || DBlist[i].equals("bems_meter_electric_cold")) { // 냉난방테이블일 경우
						heatcold += Double.parseDouble(data.get("VAL").toString()); // 냉방값만 따로 계산하는 이유는 프론트단에서 냉난방으로 출력되기 때문
					} else {
						returndata.put("VAL_DATE", data.get("VAL_DATE")); // 받아온 val_date 입력
						returndata.put(namelist1[i], data.get("VAL")); // 받아온 값을 namelist[i]를 키값으로 저장
					}
					alldata += Double.parseDouble(data.get("VAL").toString()); // 전체사용량을 뽑기 위해 더한다
				} else { // data의 값이 null일 경우
					if (DBlist[i].equals("bems_meter_electric_heating") || DBlist[i].equals("bems_meter_electric_cold")) { // 냉난방테이블일 경우
						heatcold += 0; // 데이터의 값이 null이므로 0으로 저장
					} else {
						returndata.put(namelist1[i], 0); // 각 namelist[i]를 키값으로 0값 저장
						returndata.put("VAL_DATE", today); // val_data를 오늘로 저장
					}
				}
			}
			
			returndata.put("VAL_DATE", today); // val_date를 오늘로 저장
			returndata.put("냉방", heatcold); // 냉방값 저장
			
			if (alldata > 0) { // 위에서 더한 alldata의 값이 0이상일 경우
				for (int i = 0; i < namelist2.length; i++) { // namelist2 크기만큼 반복
					double val = Double.parseDouble(returndata.get(namelist2[i]).toString()); // namelist[i]를 키로 하는 값을 val 저장
					double percentage = Math.round((val/alldata) * 10000) / 100.0; // 백분율을 계산해서 저장(소수점 둘째짜리에서 반올림)
					percentage = Math.round(percentage * 10) / 10.0; // 퍼센트지를 소수점 첫째자리에서 반올림
					returndata.put(namelist2[i]+"백분율", Math.round(percentage)); // namlist2[i] + 백분율을 키값으로 퍼센트지를 정수로 반올림해서 저장
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return returndata;
	}
}