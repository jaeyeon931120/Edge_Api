package egovframework.kevinlab.service.impl.api;

import java.io.Reader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
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

import egovframework.kevinlab.mapper.api.SunlightMapper;
import egovframework.kevinlab.service.api.SunlightService;

@Service("SunlightService")
public class SunlightServiceImpl implements SunlightService {

	@Resource(name = "sunlightMapper")
	private SunlightMapper sunlightMapper;

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
	 * 태양광 페이지의 전체 데이터 반환
	 *
	 * @param map
	 *
	 * @return List<map>
	 */
	@Override
	public List<Map<String, Object>> sunlightAllData(HashMap<String, Object> param) throws Exception {

		Map<String, Object> sunlightdata = new HashMap<String, Object>(); // 현재 데이터 (태양광)
		Map<String, Object> statusdata = new HashMap<String, Object>(); // 현재 데이터(발전효율 및 시간)
		Map<String, Object> independencedata = new HashMap<String, Object>(); // 현재 데이터(에너지자립률)
		List<Map<String, Object>> graphdata = new ArrayList<Map<String, Object>>(); // 현재 데이터(발전량, 소비량)
		List<Map<String, Object>> returndata = new ArrayList<Map<String, Object>>(); // 전체 데이터

		try {

			sunlightdata = sunlightData(param);
			statusdata = sunlightStatus(param);
			independencedata = independence(param);
			graphdata = sunlightGraph(param);

			returndata.add(sunlightdata);
			returndata.add(statusdata);
			returndata.add(independencedata);
			returndata.addAll(graphdata);

		} catch (Exception e) {
			e.printStackTrace();
		}
		return returndata;
	}

	/**
	 * 태양광 페이지의 금일, 전일, 전일누적 발전량을 추출 후 반환하는 시스템
	 *
	 * @param hashmap
	 *
	 * @return map
	 */
	@Override
	public Map<String, Object> sunlightData(HashMap<String, Object> param) throws Exception {

		Map<String, Object> data = new HashMap<String, Object>(); // 현재 데이터 (태양광)
		HashMap<String, Object> parsingdata = new HashMap<String, Object>(); // 받아온 맵(param)을 훼손하지 않고 sql에 파싱하기위한 맵

		SimpleDateFormat formatter1 = new SimpleDateFormat("yyyyMMdd"); // sql에 시간을 넘겨주기 위해서 포맷설정(연도,월,일) - 시작 fromdate부분
		SimpleDateFormat formatter2 = new SimpleDateFormat("yyyyMMddHHmmss"); // sql에 시간을 넘겨주기 위해서 포맷설정(연도,월,일,시간,분,초) - 끝 todate부분
		Calendar calF = Calendar.getInstance(Locale.KOREA); // 오늘 날짜
		Calendar calF2 = Calendar.getInstance(Locale.KOREA); // 전일 날짜
		calF2.add(Calendar.DATE, -1); // 하루 감소
		String fromdate = formatter1.format(calF.getTime()) + "000000";
		String todate = formatter2.format(calF.getTime());
		String prevfromdate = formatter1.format(calF2.getTime()) + "000000";
		String sprevfromdate = formatter1.format(calF2.getTime()) + "%";
		String prevtodate = formatter2.format(calF2.getTime());
		String complexcode = "";

		if(param.get("complexcode") != null) {
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
			parsingdata.put("prevfromdate", prevfromdate);
			parsingdata.put("sprevfromdate", sprevfromdate);
			parsingdata.put("prevtodate", prevtodate);
			parsingdata.put("complexcode", complexcode);
			parsingdata.put("dbname", DBname()); // DB이름을 저장

			data = this.sunlightMapper.sunlightData(parsingdata);

		} catch (Exception e) {
			e.printStackTrace();
		}
		return data;
	}

	/**
	 * 태양광 모니터링에 필요한 발전시간, 발전효율 데이터를 추출 후 반환
	 *
	 * @param hashmap
	 *
	 * @return map
	 */
	@Override
	public Map<String, Object> sunlightStatus(HashMap<String, Object> param) throws Exception {

		Map<String, Object> data = new HashMap<String, Object>(); // 현재 데이터 (발전효율 및 시간)
		HashMap<String, Object> parsingdata = new HashMap<String, Object>(); // 받아온 맵(param)을 훼손하지 않고 sql에 파싱하기위한 맵

		SimpleDateFormat formatter1 = new SimpleDateFormat("yyyyMMdd"); // sql에 시간을 넘겨주기 위해서 포맷설정(연도,월,일) - 시작 fromdate부분
		SimpleDateFormat formatter2 = new SimpleDateFormat("yyyyMMddHHmmss"); // sql에 시간을 넘겨주기 위해서 포맷설정(연도,월,일,시간,분,초) - 끝 todate부분
		Calendar calF = Calendar.getInstance(Locale.KOREA); // 오늘 날짜
		String fromdate = formatter1.format(calF.getTime()) + "000000";
		String todate = formatter2.format(calF.getTime());
		String complexcode = "";

		if(param.get("complexcode") != null) {
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

			data = this.sunlightMapper.sunlightStatus(parsingdata);

		} catch (Exception e) {
			e.printStackTrace();
		}
		return data;
	}

	/**
	 * 태양광 모니터링의 에너지 자립률에 필요한 데이터를 추출 후 계산하여 반환
	 *
	 * @param hashmap
	 *
	 * @return map
	 */
	@Override
	public Map<String, Object> independence(HashMap<String, Object> param) throws Exception {

		Map<String, Object> data = new HashMap<String, Object>(); // 현재 데이터 (에너지자립률)
		Map<String, Object> returndata = new HashMap<String, Object>(); // 현재 데이터 (에너지자립률)
		List<Map<String, Object>> sensordata = new ArrayList<Map<String, Object>>(); // 현재 센서 데이터
		List<Map<String, Object>> solardata = new ArrayList<Map<String, Object>>(); // 현재 센서 데이터
		HashMap<String, Object> parsingdata = new HashMap<String, Object>(); // 받아온 맵(param)을 훼손하지 않고 sql에 파싱하기위한 맵

		SimpleDateFormat formatter1 = new SimpleDateFormat("yyyyMMdd"); // sql에 시간을 넘겨주기 위해서 포맷설정(연도,월,일) - 시작 fromdate부분
		SimpleDateFormat formatter2 = new SimpleDateFormat("yyyyMMddHHmmss"); // sql에 시간을 넘겨주기 위해서 포맷설정(연도,월,일,시간,분,초) - 끝 todate부분
		Calendar calF = Calendar.getInstance(Locale.KOREA); // 오늘 날짜
		String todayfromdate = formatter1.format(calF.getTime()) + "000000"; // 오늘날짜 00시부터 시작
		String todaytodate = formatter2.format(calF.getTime()); // 오늘날짜 현재시간
		String date = formatter1.format(calF.getTime()); // 오늘 일자
		calF.add(Calendar.DATE, -1); // 하루 전 전력량을 구하기 위해 -1일
		String fromdate = formatter1.format(calF.getTime()) + "000000"; // 전일날짜 00시부터 시작
		String todate = formatter1.format(calF.getTime()) + "235959"; // 전일날짜 끝시간
		String complexcode = "";

		if(param.get("complexcode") != null) {
			complexcode = param.get("complexcode").toString();
		} else {
			data.put("result", "nok");
			data.put("reason", "complexcode가 없습니다. complexcode는 필수 키 입니다.");
			return data;
		}

		try {
			String Sensorlist[] = {"bems_sensor_solar", "bems_sensor_electric_hotwater", "bems_sensor_electric_heating", 
									"bems_sensor_electric_cold", "bems_sensor_electric_light", "bems_sensor_electric_vent"}; // 쿼리 속도 개선을 위해 센서까지 조건으로 건다.
			String Meterlist[] = {"bems_meter_solar", "bems_meter_electric_hotwater", "bems_meter_electric_heating", 
									"bems_meter_electric_cold", "bems_meter_electric_light", "bems_meter_electric_vent"}; // 쿼리 속도 개선을 위해 센서까지 조건으로 건다.
			double solarval = 0;
			double solarCval = 0;
			double totalval = 0;
			String grade = "";
			
			
			for(int i=0; i<Sensorlist.length; i++) {
				data = new HashMap<String, Object>();
				solardata = new ArrayList<Map<String, Object>>();
				sensordata = new ArrayList<Map<String, Object>>();
				parsingdata = new HashMap<String, Object>();
				List<String> sensorlist = new ArrayList<String>();

				parsingdata.put("sensor", Sensorlist[i]);
				parsingdata.put("complexcode", complexcode);
				parsingdata.put("dbname", DBname()); // DB이름을 저장

				sensordata = this.sunlightMapper.edgeSensorName(parsingdata);

				if (!sensordata.isEmpty()) {
					for (int j = 0; j < sensordata.size(); j++) {
						String sensor = sensordata.get(j).get("sensor_sn").toString();
						sensorlist.add(sensor);
					}
				}

				parsingdata = new HashMap<String, Object>();

				parsingdata.putAll(param);
				parsingdata.put("sensor", Sensorlist[i]);
				parsingdata.put("meter", Meterlist[i]);
				parsingdata.put("sensorlist", sensorlist);
				parsingdata.put("fromdate", fromdate);
				parsingdata.put("todate", todate);
				parsingdata.put("todayfromdate", todayfromdate);
				parsingdata.put("todaytodate", todaytodate);
				parsingdata.put("complexcode", complexcode);
				parsingdata.put("dbname", DBname()); // DB이름을 저장
				
				if (!sensorlist.isEmpty()) {
					if (Sensorlist[i].equals("bems_sensor_solar")) {
						solardata = this.sunlightMapper.independence_output(parsingdata);
					} else {
						data = this.sunlightMapper.independence_consumption(parsingdata);
					}
				}
				
				if(solardata != null && !solardata.isEmpty()) {
					for(int j=0; j<solardata.size(); j++) {
						String inout = solardata.get(j).get("inout").toString();
						
						if(inout.equals("I")) {
							solarval = Double.parseDouble(String.format("%.2f", Double.parseDouble(solardata.get(j).get("val").toString().replace(".", "").replace(",", "")) / 1000));
						} else {
							solarCval = Double.parseDouble(String.format("%.2f", Double.parseDouble(solardata.get(j).get("val").toString().replace(".", "").replace(",", "")) / 1000));
						}
					}
				}

				if (data != null && !data.isEmpty()) {
					totalval += Double.parseDouble(String.format("%.2f", Double.parseDouble(data.get("val").toString()) / 1000));
				}
			}
			
			int energy = (int)Math.round((solarval * 2.75) / (totalval * 2.75) * 100.0);
			
			switch (energy / 20) {
				case 4:{
					grade = "2등급";
				} break;
				
				case 3:{
					grade = "3등급";
				} break;
				
				case 2:{
					grade = "4등급";
				} break;
				
				case 1:{
					grade = "5등급";
				} break;
				
				case 0:{
					grade = "0등급";
				} break;
				
				default:{
					grade = "1등급";
				} break;
			}
			
			returndata.put("VAL_DATE", date);
			returndata.put("ENERGYINDEPENDENCE", energy);
			returndata.put("SOLARVAL", String.format("%.1f", solarval));
			returndata.put("TOTALELEC", String.format("%.1f", totalval));
			returndata.put("GRADE", grade);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		return returndata;
	}

	/**
	 * 태양광 모니터링의 태양광 시간별 비교 그래프 데이터 추출 후 반환
	 *
	 * @param hashmap
	 *
	 * @return List<map>
	 */
	@Override
	public List<Map<String, Object>> sunlightGraph(HashMap<String, Object> param) throws Exception {

		List<Map<String, Object>> data = new ArrayList<Map<String, Object>>(); // 현재 데이터
		Map<String, Object> e_data = new HashMap<String, Object>(); // 오류시 반환 데이터
		List<Map<String, Object>> returndata = new ArrayList<Map<String, Object>>(); // 반환 데이터
		HashMap<String, Object> parsingdata = new HashMap<String, Object>(); // 받아온 맵(param)을 훼손하지 않고 sql에 파싱하기위한 맵

		SimpleDateFormat formatter1 = new SimpleDateFormat("yyyyMMdd"); // sql에 시간을 넘겨주기 위해서 포맷설정(연도,월,일) - 시작
																		// fromdate부분
		Calendar calF = Calendar.getInstance(Locale.KOREA); // 오늘 날짜
		String fromdate = formatter1.format(calF.getTime()) + "%"; // 현재일자 00시부터 시작
		String complexcode = "";

		if(param.get("complexcode") != null) {
			complexcode = param.get("complexcode").toString();
		} else {
			e_data.put("error", "complexcode가 없습니다. complexcode는 필수 키 입니다.");
			e_data.put("result", "-9");
			returndata.add(e_data);
			return returndata;
		}

		try {
			parsingdata.putAll(param);
			parsingdata.put("fromdate", fromdate);
			parsingdata.put("complexcode", complexcode);
			parsingdata.put("dbname", DBname()); // DB이름을 저장

			data = this.sunlightMapper.sunlightGraph(parsingdata);
			
			for (int i = 0; i < data.size(); i++) {
				returndata.add(data.get(i));
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return returndata;
	}
}