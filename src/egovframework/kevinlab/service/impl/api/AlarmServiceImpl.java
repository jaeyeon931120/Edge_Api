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

import egovframework.kevinlab.mapper.api.AlarmMapper;
import egovframework.kevinlab.mapper.api.EdgeDBMapper;
import egovframework.kevinlab.service.api.AlarmService;

@Service("AlarmService")
public class AlarmServiceImpl implements AlarmService {

	@Resource(name = "alarmMapper")
	private AlarmMapper alarmMapper;
	
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
     * Api(프론트단)에 응답하기위한 알람 전체데이터(사용 X)
     *
     * @param EdgeDB에서 받아온 데이터 일체
     *
     * @return List<map>
     */
	@Override
	public List<Map<String, Object>> alarm_All_Data(HashMap<String, Object> param) throws Exception {

		Map<String, Object> counterdata = new HashMap<String, Object>(); // 현재 데이터 (발생한 알람 갯수)
		List<Map<String, Object>> alarmdata = new ArrayList<Map<String, Object>>(); // 현재 데이터(알람 데이터)
		List<Map<String, Object>> returndata = new ArrayList<Map<String, Object>>(); // return 데이터 (전체 데이터)

		try {

			counterdata = alarmCounter(param); // 알람 카운터 갯수를 받는 alarmCounter클래스의 값을 counterdata에 저장
			alarmdata = alarmData(param); // 알람 리스트를 받아오는 alarmData클래스의 값을 alarmdata에 저장

			returndata.add(counterdata); // returndata에 저장
			returndata.addAll(alarmdata); // returndata에 저장

		} catch (Exception e) {
			e.printStackTrace();
		}
		return returndata;
	}

	/**
     * Api(프론트단)에 알람 갯수 반환
     *
     * @param Edge Server에서 받아온 조건
     *
     * @return map
     */
	@Override
	public Map<String, Object> alarmCounter(HashMap<String, Object> param) throws Exception {

		HashMap<String, Object> parsingdata = new HashMap<String, Object>(); // 받아온 맵(param)을 훼손하지 않고 sql에 파싱하기위한 맵
		Map<String, Object> data = new HashMap<String, Object>(); // 현재 데이터 (태양광)

		String complexcode = ""; // complexcode 선언 및 초기화

		if(param.get("complexcode") != null) { // param의 저장된 complexcode의 값이 null이 아닐경우
			complexcode = param.get("complexcode").toString(); // complexcode에 param에서 받아온 code 값을 저장
		} else { // param의 complexcode가 없을경우
			data.put("result", "nok"); // 에러 출력
			data.put("reason", "complexcode가 없습니다. complexcode는 필수 키 입니다."); // 에러 출력(이유는 그대로) 
			return data; // 입력한 에러의 이유를 반환
		}

		try {
			parsingdata.putAll(param); // parsingdata에 param값 일체를 저장
			parsingdata.put("complexcode", complexcode); // complexcode의 값을 parsingdata에 저장
			parsingdata.put("dbname", DBname()); // DB이름을 저장

			data = this.alarmMapper.alarmCounter(parsingdata); // 알람갯수를 받아와서 data에 저장

		} catch (Exception e) {
			e.printStackTrace();
		}
		return data; // 알람갯수 반환
	}
	
	/**
     * Api(프론트단)에서 원하는 조건에 맞춘 알람 데이터 반환
     *
     * @param Edge Server에서 받아온 조건
     *
     * @return map
     */
	@Override
	public List<Map<String, Object>> alarmData(HashMap<String, Object> param) throws Exception {

		HashMap<String, Object> parsingdata = new HashMap<String, Object>(); // 받아온 맵(param)을 훼손하지 않고 sql에 파싱하기위한 맵
		List<Map<String, Object>> data = new ArrayList<Map<String, Object>>(); // 현재 데이터
		Map<String, Object> e_data = new HashMap<String, Object>(); // 오류시 반환 데이터
		List<Map<String, Object>> returndata = new ArrayList<Map<String, Object>>(); // 반환할 데이터
		
		// param에서 받아온 값 및 sql에 넘길 조건들 선언 및 초기화
		String fromdate = null;
		String todate = null;
		String complexcode = null;
		String name = null;
		String floor = null;
		String type = null;
		String on_off = null;
		String confirm_yn = null;

		if(param.get("fromdate") != null) { // param에 fromdate가 null이 아닐 경우
			fromdate = param.get("fromdate").toString(); // fromdate의 값을 저장
		}
		
		if(param.get("todate") != null) { // param에 todate가 null이 아닐 경우
			todate = param.get("todate").toString(); // todate의 값을 저장
		}
		
		if(param.get("complexcode") != null) { // param에 complexcode가 null이 아닐 경우
			complexcode = param.get("complexcode").toString(); // complexcode의 값을 저장
		} else { // param에 complexcode의 값이 null일 경우
			e_data.put("result", "nok"); // 에러 저장
			e_data.put("reason", "complexcode가 없습니다. complexcode는 필수 키 입니다."); // 에러의 이유를 저장 e_data에 저장
			returndata.add(e_data); // returndata(리스트 맵)에 e_data 저장
			return returndata; // returndata 반환
		}
		if(param.get("floor") != null) { // param에 floor가 null이 아닐 경우
			floor = param.get("floor").toString(); // floor의 값을 저장
		}
		if(param.get("sensor_type") != null) { // param에 sensor_type이 null이 아닐 경우
			type = param.get("sensor_type").toString(); // sensor_type의 값을 저장
		}
		if(param.get("alarm_on_off") != null) { // param에 alarm_on_off가 null이 아닐 경우
			on_off = param.get("alarm_on_off").toString(); // alarm_on_off의 값을 저장
		}
		if(param.get("confirm_yn") != null) {
			confirm_yn = param.get("confirm_yn").toString();
		}
		
		if(complexcode.equals("2002")) { // complexcode가 2002일 경우
			name = "태백산국립공원"; // 지역이름은 태백산국립공원이다
		}
		
		if (fromdate != null && todate != null) {
			if (fromdate.length() >= 8) { // fromdate의 길이가 8이상일 경우
				fromdate = fromdate.substring(0, 8); // fromdate의 길이 8정도만큼 자른다.(이유 : 일자로 끊어서 계산하기위해)
			}
			if (todate.length() >= 8) { // todate의 길이가 8이상일 경우
				todate = todate.substring(0, 8); // todate의 길이 8정도만큼 자른다.(이유 : 일자로 끊어서 계산하기위해)
			}
		}

		try {
			// 위에서 받아온 시간 및 조건을 parsingdata에 넣는다.
			parsingdata.putAll(param);
			parsingdata.put("fromdate", fromdate);
			parsingdata.put("todate", todate);
			parsingdata.put("complexcode", complexcode);
			parsingdata.put("name", name);
			parsingdata.put("floor", floor);
			parsingdata.put("sensor_type", type);
			parsingdata.put("alarm_on_off", on_off);
			parsingdata.put("confirm_yn", confirm_yn);
			parsingdata.put("dbname", DBname()); // DB이름을 저장
			
			// alarmData로 알람로그를 검색한다.
			data = this.alarmMapper.alarmData(parsingdata);
			
			// 검색한 로그를 반환할 리스트에 넣는다.
			for (int i = 0; i < data.size(); i++) {
				returndata.add(data.get(i));
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return returndata;
	}
	
	/**
     * Api(프론트단)에서 종 버튼 클릭시 알람 카운트 갯수 초기화
     *
     * @param 종 버튼을 누른 사이트 complexcode
     *
     * @return map
     */
	@Override
	public Map<String, Object> alarmConfirm(HashMap<String, Object> param) throws Exception {

		HashMap<String, Object> parsingdata = new HashMap<String, Object>(); // 받아온 맵(param)을 훼손하지 않고 sql에 파싱하기위한 맵
		Map<String, Object> e_data = new HashMap<String, Object>(); // 오류시 반환 데이터
		
		// param에서 받아온 값 및 sql에 넘길 조건들 선언 및 초기화
		String complexcode = null;
		
		if(param.get("complexcode") != null) { // complexcode가 null이 아닐 경우
			complexcode = param.get("complexcode").toString(); // complexcode의 값을 저장
		} else { // complexcode가 null일 경우
			e_data.put("result", "nok"); // 에러 저장
			e_data.put("reason", "complexcode가 없습니다. complexcode는 필수 키 입니다."); // 에러의 이유를 저장
			return e_data; // 에러의 이유 반환
		}

		try {
			int result = 0; // flag로 사용하기 위한 result 변수 생성
			
			parsingdata.putAll(param);
			parsingdata.put("complexcode", complexcode);
			parsingdata.put("dbname", DBname()); // DB이름을 저장
			
			result = this.alarmMapper.updateConfirm(parsingdata); // 알람 카운트 초기화
			
			if(result >= 1) { // flag가 1 이상일 경우(성공)
				e_data.put("result", "ok");
				e_data.put("reason", "카운트 초기화에 성공했습니다.");
			} else { // flag가 1보다 작을 경우(실패)
				e_data.put("result", "nok");
				e_data.put("reason", "카운트 초기화에 실패했습니다.");		
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return e_data;
	}
	
	/**
     * Edge meter테이블 데이터 insert 중 오류 발생시 알람 데이터 생성
     *
     * @param void
     *
     * @return void
     */
	@Override
	public void Edge_alarm_insert() throws Exception {
		Map<String, Object> pkdata = new HashMap<String, Object>();
		Map<String, Object> c_data = new HashMap<String, Object>();
		Map<String, Object> alarmdata = new HashMap<String, Object>();
		Map<String, Object> errordata = new HashMap<String, Object>();
		Map<String, Object> insertdata1 = new HashMap<String, Object>();
		Map<String, Object> insertdata2 = new HashMap<String, Object>();
		List<Map<String, Object>> data = new ArrayList<Map<String, Object>>();
		List<Map<String, Object>> errorList = new ArrayList<Map<String, Object>>();
		List<Map<String, Object>> sensorList = new ArrayList<Map<String, Object>>();
		HashMap<String, Object> parsingdata = new HashMap<String, Object>(); // sql에 필요한 조건을 넘겨주는 맵
		HashMap<String, Object> checkdata = new HashMap<String, Object>(); // sql에 필요한 조건을 넘겨주는 맵
		
		try {
			
			// 각 row테이블의 센서이름으로 된 배열 정의
			String[] Sensorlist = { "bems_sensor_electric", "bems_sensor_electric_hotwater", "bems_sensor_electric_elechot",
					"bems_sensor_electric_heating", "bems_sensor_electric_cold", "bems_sensor_electric_light",
					"bems_sensor_electric_vent", "bems_sensor_electric_elevator", "bems_sensor_electric_water",
					"bems_sensor_electric_equipment", "bems_sensor_electric_boiler", "bems_sensor_gas",
					"bems_sensor_heating", "bems_sensor_hotwater", "bems_sensor_water" };
			// 각 row테이블의 이름으로 된 배열 정의
			String[] DBlist = { "bems_meter_electric", "bems_meter_electric_hotwater", "bems_meter_electric_elechot", 
					"bems_meter_electric_heating", "bems_meter_electric_cold", "bems_meter_electric_light",
					"bems_meter_electric_vent", "bems_meter_electric_elevator", "bems_meter_electric_water",
					"bems_meter_electric_equipment", "bems_meter_electric_boiler", "bems_meter_gas", 
					"bems_meter_heating", "bems_meter_hotwater", "bems_meter_water" };
			
			SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss"); // sql에 시간을 넘겨주기 위해서 포맷설정(연도,월,일) - valdate부분
			Calendar calF = Calendar.getInstance(Locale.KOREA); // 오늘 날짜
			String date = formatter.format(calF.getTime()); // 오늘 일자 저장
			String todate = formatter.format(calF.getTime()); // 현재 시간으로 저장
			int flag = 0; // flag 변수 선언(이유: 각 테이블에 컬럼확인)
			String complexcode = "2002"; // complexcode 선언 및 초기화
			
			for (int i = 0; i < Sensorlist.length; i++) { // 위에서 선언한 배열의 길이만큼 반복
				checkdata = new HashMap<String, Object>();  // 맵 초기화
				errordata = new HashMap<String, Object>();  // 맵 초기화
				parsingdata = new HashMap<String, Object>(); // 맵 초기화
				
				checkdata.put("tablename", DBlist[i]); // tablename 저장(각 meter테이블)
				checkdata.put("columnname", "total_wh"); // columnname total_wh 저장
				
				c_data = this.alarmMapper.edgeCheckdata(checkdata); // 각 테이블에 columnname total_wh가 있는지 확인

				if (c_data != null) { // checkdata가 null일 아닐경우
					flag = Integer.parseInt(c_data.get("flag").toString()); // flag 값 저장
				}
				
				parsingdata.put("complexcode", complexcode);
				parsingdata.put("sensor", Sensorlist[i]);
				parsingdata.put("dbname", DBname()); // DB이름 저장
				sensorList = this.edgeDBMapper.edgeSensorName(parsingdata);
				
				for(int j=0; j < sensorList.size(); j++) {
					errordata = new HashMap<String, Object>();  // 맵 초기화
					parsingdata = new HashMap<String, Object>(); // 맵 초기화
					
					if(flag >= 1) { // flag가 1이상인 경우
						//total_wh를 포함한 현재시간 및 데이터값 저장
						parsingdata.put("sensor", Sensorlist[i]);
						parsingdata.put("meter", DBlist[i]);
						parsingdata.put("todate", todate);
						parsingdata.put("sensor_sn", sensorList.get(j).get("sensor_sn").toString());
						parsingdata.put("total_wh", "total_wh");
						parsingdata.put("dbname", DBname()); // DB이름을 저장
					} else { // flag가 1미만인 경우
						//total_wh가 없는 경우이므로 val를 포함한 현재시간 및 데이터값 저장
						parsingdata.put("sensor", Sensorlist[i]);
						parsingdata.put("meter", DBlist[i]);
						parsingdata.put("todate", todate);
						parsingdata.put("sensor_sn", sensorList.get(j).get("sensor_sn").toString());
						parsingdata.put("val", "val");
						parsingdata.put("dbname", DBname()); // DB이름을 저장
					}
					
					// 알람 테이블 조회
					errordata = this.alarmMapper.getSensorInformation(parsingdata); // 최신데이터 장애 조회(각 테이블별 장애 조회)
					errorList.add(errordata);
				}

				if (!errorList.isEmpty()) { // errorList가 비지않았을 경우
					for (int j = 0; j < errorList.size(); j++) { // errorList사이즈만큼 반복
						String errorcode = null; // 에러코드 변수 선언
						String sensor = errorList.get(j).get("sensor_sn").toString(); // 조회한 센서를 저장
						String sensortype = Sensorlist[i].replace("bems_sensor_", ""); // 조회한 센서타입을 저장
						complexcode = errorList.get(j).get("complex_code_pk").toString(); // 조회한 complexcode값을 저장

						if (errorList.get(j).get("error_code") != null) { // errorList의 에러코드가 null이 아닐경우
							errorcode = errorList.get(j).get("error_code").toString(); // 에러코드를 errorList의 에러코드를 저장
						}

						if (errorcode.equals("1")) { // 에러코드가 1일 경우(에러발생)
							alarmdata.putAll(errorList.get(j)); // 에러리스트(조회한 에러데이터)의 값을 저장
							alarmdata.put("alarm_on_off", "on"); // alarm_on_off의 값을 on으로 저장
							alarmdata.put("alarm_msg", "TIME OUT"); // alarm_msg는 TIME OUT으로 통일해서 저장
							alarmdata.put("alarm_on_time", date); // alarm 발생시간은 현재시간으로 저장
							alarmdata.put("confirm_yn", "n"); // 프론트단에서 확인여부(n으로 저장)
							alarmdata.put("sensor_type", sensortype); // 센서타입 저장
							alarmdata.remove("error_code"); // 에러리스트에서 받아온 에러코드 제거(이유: 에러코드 컬럼이 없어서)
							alarmdata.remove("total_wh"); // total_wh제거(이유:total_wh컬럼이 없어서)

							List<String> keyList = new ArrayList<>(alarmdata.keySet()); // 알람데이터의 key값으로 리스트생성
							List<Object> valueList = new ArrayList<>(alarmdata.values()); // 알람데이터의 value값으로 리스트생성
							List<Map<String, Object>> result1 = new ArrayList<Map<String, Object>>(); // sql에 조건을 리스트로 넘기기위한 리스트맵
							List<Map<String, Object>> result2 = new ArrayList<Map<String, Object>>(); // sql에 조건을 리스트로 넘기기위한 리스트맵

							for (int p = 0; p < keyList.size(); p++) { // 키리스트의 사이즈만큼 반복
								insertdata1 = new HashMap<String, Object>(); // 맵 초기화
								pkdata = new HashMap<String, Object>(); // 맵 초기화

								insertdata1.put("name", keyList.get(p)); // key리스트를 name이라는 키로 저장
								insertdata1.put("val", valueList.get(p)); // value리스트를 val이라는 키로 저장

								if (!(keyList.get(p).equals("complex_code_pk"))) { // key리스트의 p번째 값이 complexcodepk가 아닐때
									pkdata.put("name", keyList.get(p)); // pk키를 저장하는 pk맵
									pkdata.put("val", valueList.get(p)); // pk값을 저장하는 pk맵
									result2.add(pkdata); // 리스트2에 pk 키와 값 저장
								}

								result1.add(insertdata1); // 리스트1에 pk키와 값 저장
							}

							insertdata2.put("result1", result1); // result1라는 키로 result1값 저장
							insertdata2.put("result2", result2); // result2라는 키로 result2값 저장
							insertdata2.put("dbname", DBname()); // DB이름을 저장

							this.alarmMapper.alarmLogInsert(insertdata2); // 알람로그 저장
						} else { // error_code가 0일 경우
							parsingdata = new HashMap<String, Object>(); // 맵 초기화

							parsingdata.put("complexcode", complexcode); // complexcode 저장
							parsingdata.put("sensor_type", sensortype); // 센서타입 저장
							parsingdata.put("sensor_sn", sensor); // 센서 저장
							parsingdata.put("alarm_on_off", "on"); // 알람 on 저장
							parsingdata.put("dbname", DBname()); // DB이름을 저장

							data = new ArrayList<Map<String, Object>>(); // data 리스트맵 초기화

							data = this.alarmMapper.getAlarmOnOff(parsingdata); // 해당 센서타입의 센서가 알람 on이 있는지 확인
							
							if(!data.isEmpty()) { // data의 값이 비지 않았을 경우
								parsingdata = new HashMap<String, Object>(); // 맵 초기화
								
								parsingdata.put("complexcode", complexcode); // complexcode값 저장
								parsingdata.put("sensor_type", sensortype); // 센서타입 저장
								parsingdata.put("sensor_sn", sensor); // 센서 저장
								parsingdata.put("alarm_on_off", "off"); // 알람 off로 전환 (이유: 에러코드가 0으로 들어왔기 때문에 데이터가 안들어오는 문제가 해결)
								parsingdata.put("alarm_off_time", date); // 알람 offtime을 현재시간으로 설정
								parsingdata.put("dbname", DBname()); // DB이름을 저장
								
								this.alarmMapper.updateAlarm(parsingdata); // 기존에 있던 알람로그에 업데이트
							}
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}