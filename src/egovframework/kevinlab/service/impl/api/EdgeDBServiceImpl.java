package egovframework.kevinlab.service.impl.api;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Resource;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.ibatis.io.Resources;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import egovframework.kevinlab.mapper.api.EdgeDBMapper;
import egovframework.kevinlab.service.api.EdgeDBService;
import egovframework.kevinlab.service.api.SystemLogService;

@Service("EdgeDBService")
public class EdgeDBServiceImpl implements EdgeDBService {

	@Resource(name = "edgedbMapper")
	private EdgeDBMapper edgeDBMapper;
	
	@Resource(name = "SystemLogService")
	private SystemLogService systemlogService;
	
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
	 * EdgeDB와 lbemsDB의 싱크를 맞추기위한 데이터 전송
	 *
	 * @param 로그인 이후 access하기 위한 토큰 및 재전송이 필요한 날짜
	 *
	 * @return List<map>
	 */
	@Override
	public Map<String, Object> Edge_All_Data(HashMap<String, Object> param) throws Exception {

		HashMap<String, Object> parsingdata = new HashMap<String, Object>(); // sql에 필요한 조건을 넘겨주는 맵
		HashMap<String, Object> data = new HashMap<String, Object>(); // edgedb에서 데이터를 받아오기위해서 맵과 컬럼 이름 결합
		HashMap<String, Object> syncdata1 = new HashMap<String, Object>(); // edgedb서 데이터를 받아온 후 타입확인후 데이터 입력하기 위한 맵1
		HashMap<String, Object> syncdata2 = new HashMap<String, Object>(); // edgedb에서 데이터를 받아온 후 타입확인후 데이터 입력하기 위한 맵2
		Map<String, Object> logdata = new HashMap<String, Object>(); // edgedb에서 로그를 기록하기 위한 맵
		Map<String, Object> apiresult = new HashMap<String, Object>(); // lbemsdb api 데이터를 수신 후 결과확인을 위한 맵
		Map<String, Object> tokendata = new HashMap<String, Object>(); // lbemsdb api 데이터를 수신을 위한 로그인 결과확인(토큰값)을 위한 맵
		Map<String, Object> logindata = new HashMap<String, Object>(); // lbemsdb api 데이터를 수신 후 토큰을 분리하기 위한 맵
		Map<String, Object> returnresult = new HashMap<String, Object>(); // 반환할 데이터(프론트단으로)
		List<Map<String, Object>> columnsname = new ArrayList<Map<String, Object>>(); // 각 테이블별 컬럼네임 확인을 위한 맵
		List<Map<String, Object>> datalist = new ArrayList<Map<String, Object>>(); // edgedb에서 받아온 데이터를 저장하는 리스트맵
		List<Map<String, Object>> sensordata = new ArrayList<Map<String, Object>>(); // edgedb에서 받아온 데이터의 센서의 이름을 저장하는 리스트맵
		List<Map<String, Object>> returnlist = new ArrayList<Map<String, Object>>(); // edgedb에서 받아온 데이터를 센서이름과 데이터를 저장해서 lbemsdb에 거네는 리스트맵
		List<Map<String, Object>> returndata = new ArrayList<Map<String, Object>>(); // lbemsdb과의 통신후 나온 결과값을 저장하는 리스트맵
		ObjectMapper mapper = new ObjectMapper(); // 로그인 실패시의 결과값을 반환하기 위한 맵
		String login = null; // lbemsdb api에 로그인 유지를 위한 토큰
		String admin = null; // 로그에 저장하기위한 admin, 서버에서 요청시 null(즉, system), 사용자가 요청시 param에서 받아온값으로 저장
		String fromdate = null; // 시스템 재전송 하려는 시작날짜
		String todate = null; // 시스템 재전송 하려는 종료날짜
		String token = null; // 토큰값 저장
		String fromdatelog = null; // 데이터 재전송 시작시간 저장을 위한 변수
		String todatelog = null; // 데이터 재전송 종료시간 저장을 위한 변수
		String complexcode = "2002";
		
		SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHH"); // sql에 시간을 넘겨주기 위해서 포맷설정(연도,월,일) - 시작 fromdate부분
		SimpleDateFormat formatterday = new SimpleDateFormat("yyyyMMdd"); // sql에 시간을 넘겨주기 위해서 포맷설정(연도,월,일) - 시작 fromdate부분
		
		if (param != null) {
			if (param.get("fromdate") != null) {
				fromdate = param.get("fromdate").toString(); // 받아온 fromdate 저장
			} else {
				data.put("result", "nok");
				data.put("reason", "시작날짜가 없습니다. 시작날짜를 확인해주세요.");
				return data;
			}
			if (param.get("todate") != null) {
				todate = param.get("todate").toString(); // 받아온 todate 저장
			} else {
				data.put("result", "nok");
				data.put("reason", "종료날짜가 없습니다. 종료날짜를 확인해주세요.");
				return data;
			}
			if (param.get("complexcode") != null) {
				complexcode = param.get("complexcode").toString(); // 받아온 complecode 저장
			} else {
				data.put("result", "nok");
				data.put("reason", "complexcode가 없습니다. complexcode를 확인해주세요.");
				return data;
			}
			if(param.get("admin") != null) {
				admin = param.get("admin").toString(); // 받아온 admin값 저장
			}
			
			if(fromdate.length() >= 8) { // 받아온 fromdate의 값이 8이상일때
				fromdate = fromdate.substring(0,8); // fromdate의 값을 길이 8로 자른다.
				fromdate = fromdate + "00"; // fromdate에 00을 더한다.
			}
			if(todate.length() >= 8) { // 받아온 todate의 값이 8이상일때
				todate = todate.substring(0,8); // todate의 값을 길이 8로 자른다.
				if(fromdate.equals(todate + "00")) { // todate + "00"이 fromdate랑 같을때
					Calendar calT = Calendar.getInstance(Locale.KOREA); // 현재시간 설정
					Calendar calF = Calendar.getInstance(Locale.KOREA); // 현재시간 설정
					calF.setTime(formatterday.parse(fromdate));
					if(formatterday.format(calF.getTime()) != formatterday.format(calT.getTime())) {
						todate = todate + "23";
					} else {
						// todate랑 fromdate의 값이 같을때 todate을 현재시각으로 설정한 이유는 최대한 정확하게 데이터 재전송을 하기위함(시간을 러프하게 설정하면 오류발생)
						todate = formatter.format(calT.getTime()); // todate를 현재시각으로 설정
					}
				} else { // todate와 fromdate가같지 않을때
					SimpleDateFormat todayformat = new SimpleDateFormat("yyyyMMdd"); // 일자 비교를 위한 포맷
					Calendar calFT = Calendar.getInstance(Locale.KOREA); // 현재시간 설정
					calFT.setTime(todayformat.parse(fromdate)); // 받아온 fromdate(년,월,일)으로 calFT의 시간을 변경
					String daydate = todayformat.format(calFT.getTime());
					// 위에 todate랑 fromdate의 비교랑 같지 않은 이유는, fromdate가 오늘일 경우 todate의 값이 확실치 않을 경우에 대비해서 오늘날짜로 한번더 확인하기 위함
					if((daydate + "00").equals(fromdate)) {
						calFT = Calendar.getInstance(Locale.KOREA); // 오늘 시간으로 설정
						todate = formatter.format(calFT.getTime()); // 현재시간으로 todate 설정
					} else {
						todate = todate + "23"; // 아닐경우 끝시간인 23으로 저장
					}
				}
			}
		}

		try {
			// row테이블만 데이터 재전송으로 보내는거기 때문에 row테이블만 선택한다.(태백산은 전기로만 운용하기 때문에 테이블 명이
			// electric%로 진행된다.(태양광제외인지 확인해야 한다))
			// row테이블만 한다는건 김부장님께 여쭤봐서 확인받는 사항.
			// 검색조건에 필요한 각 row테이블의 센서테이블도 필요하므로 센서테이블 배열 정의
			String[] Sensorlist = { "bems_sensor_solar", "bems_sensor_electric", "bems_sensor_electric_elechot",
					"bems_sensor_electric_hotwater", "bems_sensor_electric_heating", "bems_sensor_electric_cold",
					"bems_sensor_electric_light", "bems_sensor_electric_vent", "bems_sensor_electric_elevator",
					"bems_sensor_electric_water", "bems_sensor_electric_equipment", "bems_sensor_electric_boiler"};
			// 각 row테이블의 이름으로 된 배열 정의
			String[] DBlist = { "bems_meter_solar", "bems_meter_electric", "bems_meter_electric_elechot",
					"bems_meter_electric_hotwater", "bems_meter_electric_heating", "bems_meter_electric_cold",
					"bems_meter_electric_light", "bems_meter_electric_vent", "bems_meter_electric_elevator",
					"bems_meter_electric_water", "bems_meter_electric_equipment", "bems_meter_electric_boiler"};

			// 각 테이블 컬럼에 맞게끔 설정 배열 정의.(나중에 api 통신때 다 string으로 변환하기때문에 CAST에 너무 신경쓸필요없다.)
			String[] column1 = { "IFNULL(C.sensor_sn, \"\") AS sensor_sn", "IFNULL(C.val_date, \"00000000000000\") AS val_date", "CAST(CAST(IFNULL(C.current_w, 0) AS UNSIGNED) AS CHAR) AS current_w",
					"CAST(CAST(IFNULL(C.total_wh, 0) AS UNSIGNED) AS CHAR) AS total_wh", "CAST(CAST(IFNULL(C.error_code, 0) AS UNSIGNED) AS CHAR) AS error_code" };
			String[] column2 = { "IFNULL(C.sensor_sn, \"\") AS sensor_sn", "IFNULL(C.val_date, \"00000000000000\") AS val_date", "CAST(CAST(IFNULL(C.current_w, 0) AS UNSIGNED) AS CHAR) AS current_w",
					"CAST(CAST(IFNULL(C.total_wh, 0) AS UNSIGNED) AS CHAR) AS total_wh", "CAST(CAST(IFNULL(C.pf, 0) AS UNSIGNED) AS CHAR) AS pf",
					"CAST(CAST(IFNULL(C.error_code, 0) AS UNSIGNED) AS CHAR) AS error_code" };
			String[] column3 = { "IFNULL(C.sensor_sn, \"\") AS sensor_sn", "IFNULL(C.val_date, \"00000000000000\") AS val_date", "CAST(CAST(IFNULL(C.current_w, 0) AS UNSIGNED) AS CHAR) AS current_w",
					"CAST(CAST(IFNULL(C.total_wh, 0) AS UNSIGNED) AS CHAR) AS total_wh", "CAST(CAST(IFNULL(C.error_code, 0) AS UNSIGNED) AS CHAR) AS error_code", "IFNULL(C.info, \"\") AS info" };
			
			SimpleDateFormat todayformat = new SimpleDateFormat("yyyyMMdd"); // fromdate나 todate가 없을경우 오늘 날짜로 포맷
			SimpleDateFormat formatterlog = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			
			Calendar calFlog = Calendar.getInstance(Locale.KOREA); // 오늘 날짜
			fromdatelog = formatterlog.format(calFlog.getTime()); // 데이터 재전송 시작시간

			tokendata = Lbems_Login(param); // 로그인 후 정보 받아오기(토큰)
			
			if(!tokendata.isEmpty()) {
				login = tokendata.get("token").toString();
			}

			if (login != null) {
				logindata = mapper.readValue(login, new TypeReference<Map<String, Object>>() {});
				if (logindata.get("token") == null) {
					return data;
				}
			} else {
				logindata = mapper.readValue(login, new TypeReference<Map<String, Object>>() {});
				String reason = logindata.get("reason").toString();
				
				if(admin != null) {
					logdata.put("complex_code_pk", complexcode);
					logdata.put("work_type", "시스템 재전송");
					logdata.put("admin", admin);
					logdata.put("result", "실패");
					logdata.put("reason", "로그인 실패 : " + reason);
					logdata.put("contents", fromdatelog + "~" + todatelog);
					this.systemlogService.Edge_Systemlog_Insert(logdata);
				} else {
					logdata.put("complex_code_pk", complexcode);
					logdata.put("work_type", "시스템 재전송");
					logdata.put("admin", "system");
					logdata.put("result", "실패");
					logdata.put("reason", "로그인 실패 : " + reason);
					logdata.put("contents", fromdatelog + "~" + todatelog);
					this.systemlogService.Edge_Systemlog_Insert(logdata);
				}
				
				data.put("result", "nok");
				data.put("reason", "통신장애가 발생했습니다. Kevinlab에 문의해주세요.");
				return data;
			}
			
			Calendar calF = Calendar.getInstance(Locale.KOREA); // 오늘 날짜
			Calendar calT = Calendar.getInstance(Locale.KOREA); // 오늘 날짜
			
			if(fromdate == null) {
				fromdate = todayformat.format(calF.getTime()) + "00";
			}
			
			if(todate == null) {
				todate = formatter.format(calT.getTime());
			}
			
			calF = Calendar.getInstance(Locale.KOREA); // 오늘 날짜
			calT = Calendar.getInstance(Locale.KOREA); // 오늘 날짜

			calF.setTime(formatter.parse(fromdate)); // 시간 대입
			calT.setTime(formatter.parse(todate)); // 시간 대입
			int count = 0;
			while (!calF.after(calT)) { // calF가 calT를 넘지 않을떄까지 반복
				count++; // 카운트 증가
				calF.add(Calendar.HOUR, 1); // 한시간 증가
			}
			calF.setTime(formatter.parse(fromdate)); // 시간 대입

			for (int i = 0; i < DBlist.length; i++) {
				parsingdata.put("sensor", Sensorlist[i]);
				parsingdata.put("complexcode", complexcode);
				parsingdata.put("dbname", DBname());
				sensordata = this.edgeDBMapper.edgeSensorName(parsingdata); // 각 센서테이블에서 엣지 태백산 지역코드인 2002에 해당하는 센서목록 출력
				
				if (!sensordata.isEmpty()) {
					parsingdata = new HashMap<String, Object>();
					
					parsingdata.put("table_name", DBlist[i]); // 위에서 선언한 row테이블 배열 parsingdata에 저장
					parsingdata.put("dbname", DBname());
					columnsname = this.edgeDBMapper.columnsName(parsingdata); // 각 row테이블의 컬럼네임 가져오기

					String[] namelist = new String[columnsname.size()]; // 받아온 컬럼네임으로 namelist size 결정
					List<String> columnlist = new ArrayList<String>(); // columnlist 선언 및 초기화
					data = new HashMap<String, Object>(); // data 초기화

					for (int j = 0; j < columnsname.size(); j++) {
						String name = columnsname.get(j).get("COLUMN_NAME").toString(); // 받아온 컬럼네임 name 변수에 저장
						namelist[j] = name; // 네임리스트배열에 이름 저장
					}

					if (Arrays.stream(namelist).anyMatch(s -> s.equals("pf"))) { // 네임리스트 배열에서 pf랑 같은 글자가 있을시
						columnlist = Stream.of(column2).collect(Collectors.toList()); // columnlist 배열을 List 전환(이유는
																						// mybatis를
																						// 이용하여 컬럼을 입력하기 위해서)
						data.put("dbname", DBname()); // DB리스트 배열에서 순서대로 DB입력
						data.put("table_name", DBlist[i]); // DB리스트 배열에서 순서대로 테이블입력
						data.put("column_name", columnlist); // 컬럼이름 배열에서 순서대로 컬럼이름 입력
						data.put("sensor_name", Sensorlist[i]); // 센서리스트 배열에서 순서대로 센서테이블입력
						datalist.add(data); // datalist에 data 저장
					} else if (Arrays.stream(namelist).anyMatch(s -> s.equals("info"))) { // 네임리스트 배열에서 info랑 같은 글자가 있을시
						columnlist = Stream.of(column3).collect(Collectors.toList()); // columnlist 배열을 List 전환(이유는 mybatis를 이용하여 컬럼을 입력하기 위해서)
						data.put("dbname", DBname()); // DB리스트 배열에서 순서대로 DB입력
						data.put("table_name", DBlist[i]); // DB리스트 배열에서 순서대로 테이블입력
						data.put("column_name", columnlist); // 컬럼이름 배열에서 순서대로 컬럼이름 입력
						data.put("sensor_name", Sensorlist[i]); // 센서리스트 배열에서 순서대로 센서테이블입력
						datalist.add(data); // datalist에 data 저장
					} else { // 네임리스트 배열이 기본 배열일 경우
						columnlist = Stream.of(column1).collect(Collectors.toList()); // columnlist 배열을 List 전환(이유는 mybatis를 이용하여 컬럼을 입력하기 위해서)
						data.put("dbname", DBname()); // DB리스트 배열에서 순서대로 DB입력
						data.put("table_name", DBlist[i]); // DB리스트 배열에서 순서대로 테이블입력
						data.put("column_name", columnlist); // 컬럼이름 배열에서 순서대로 컬럼이름 입력
						data.put("sensor_name", Sensorlist[i]); // 센서리스트 배열에서 순서대로 센서테이블입력
						datalist.add(data); // datalist에 data 저장
					}
				}
			}

			for (int i = 0; i < count; i++) {

				fromdate = formatter.format(calF.getTime()) + "0000"; // param에서 받아온 fromdate값 뒤에 시간 및 분 0000으로 저장
				todate = formatter.format(calF.getTime()) + "5959"; // param에서 받아온 fromdate값 뒤에 끝분,끝초를 더해서 저장
				calF.add(Calendar.HOUR, 1); // 한시간 증가

				for (int j = 0; j < datalist.size(); j++) { // 위에서 만든 데이터리스트 사이즈만큼 반복문 수행
					data = new HashMap<String, Object>(); // data 초기화
					syncdata1 = new HashMap<String, Object>(); // syncdata1 초기화
					syncdata2 = new HashMap<String, Object>(); // syncdata2 초기화
					// type에 테이블이름에서 반복되는 부분을 뺀 나머지 저장
					String type = datalist.get(j).get("table_name").toString().replace("bems_meter_", "");

					data.putAll(datalist.get(j)); // data에 datalist의 j번째 맵 저장
					data.putAll(param); // 받아온 param값 저장
					data.put("type", type); // 위에서 선언한 타입 저장
					data.put("fromdate", fromdate); // 위에서 받아온 fromdate 저장
					data.put("todate", todate); // 위에서 받아온 todate 저장

					returnlist = this.edgeDBMapper.edgeSyncData(data); // 엣지DB에서 위에서 설정한 조건의 값 returnlist에 저장
					
					// 밑에 주석처리한 이유는 데이터가 없는 경우 재전송을 할건지, 아니면 안할건지 판단 후 주석해제 및 삭제
//					if (!returnlist.isEmpty()) {
						// 위에서 받아온 데이터리스트를 한번더 맵에 저장하는 이유는 엣지 서버 외부 api규격서의 방식에 따라 파라미터를 넘기기 위해서
						syncdata1.put("data", returnlist); // syncdata1의 data값부분에 리스트 저장
						syncdata1.put("type", type); // syncdata1의 타입 저장
						syncdata2.put("sync_data", syncdata1); // syncdata2의 sync_data에 정렬한 syncdata1을 저장

						if (logindata.get("token") != null) { // 로그인 id가 null 아닐경우(나중에 이부분은 token으로 교체 필요!!!)
							token = logindata.get("token").toString(); // 토큰을 위에서 받아온 logindata의 id를 저장
						}

						apiresult = getSyncApi(syncdata2, token); // lbemsdb api에 보내기위한 getSyncApi클래스 데이터값 및 토큰을 보내고 결과값을 apiresult에 저장
						returndata.add(j, apiresult); // 반환할 데이터리스트 returndata리스트에 순서대로 저장
//					}
				}
			}
			
			Calendar calTlog = Calendar.getInstance(Locale.KOREA); // 오늘 날짜(데이터 재전송 종료시간)
			todatelog = formatterlog.format(calTlog.getTime()); // 데이터 재전송 종료시간 저장
			
			if (!returndata.isEmpty()) { // 데이터 재전송 결과가 있을 경우
				String resultok = null; // 데이터 재전송 결과를 저장하기 위한 변수
				
				for (int i = 0; i < returndata.size(); i++) {
					resultok = returndata.get(i).get("result").toString(); // 데이터 재전송값 저장
					
					if(resultok.equals("nok")) { // 결과값이 실패일때
						String reason = returndata.get(i).get("reason").toString(); // 실패 이유 저장
						if(admin != null) { // admin값이 있을때, 즉 사용자가 요청했을때
							/*시스템 로그 저장*/
							logdata.put("complex_code_pk", complexcode); // complexcode 저장
							logdata.put("work_type", "시스템 재전송"); // work_type 저장
							logdata.put("admin", admin); // admin 저장
							logdata.put("result", "실패"); // 결과값이 실패이므로 실패를 저장
							logdata.put("reason", reason); // 실패 이유 저장
							logdata.put("contents", fromdatelog + "~" + todatelog); // 상세내용에 시작시간~종료시간 저장
							this.systemlogService.Edge_Systemlog_Insert(logdata); // 시스템 로그 저장
						} else { // admin이 없을때, 즉 서버에서 요청했을떄
							/*시스템 로그 저장*/
							logdata.put("complex_code_pk", complexcode); // complexcode 저장
							logdata.put("work_type", "시스템 재전송"); // work_type 저장
							logdata.put("admin", "system"); // admin 시스템
							logdata.put("result", "실패"); // 결과값이 실패이므로 실패를 저장
							logdata.put("reason", reason); // 실패 이유 저장
							logdata.put("contents", fromdatelog + "~" + todatelog); // 상세내용에 시작시간~종료시간 저장
							this.systemlogService.Edge_Systemlog_Insert(logdata); // 시스템 로그 저장
						}
						
						return returndata.get(i); // 실패시 결과와 이유를 담아서 프론트단으로 반환
					}
				}
				
				if(logdata.isEmpty()) { // 위에서 실패했을때 logdata를 만드는데 logdata의 값이 없으면 성공
					
					if(admin != null) { // admin값이 있을때, 즉 사용자가 요청했을때
						/*시스템 로그 저장*/
						logdata.put("complex_code_pk", complexcode); // complexcode 저장
						logdata.put("work_type", "시스템 재전송"); // work_type 저장
						logdata.put("admin", admin); // 사용자 저장
						logdata.put("result", "정상"); // 실패데이터가 없으므로 정상
						logdata.put("contents", fromdatelog + "~" + todatelog); // 상세내용에 시작시간~종료시간 저장
						this.systemlogService.Edge_Systemlog_Insert(logdata); // 시스템 로그 저장
					} else { // admin이 없을때, 즉 서버에서 요청했을떄
						/*시스템 로그 저장*/
						logdata.put("complex_code_pk", complexcode); // complexcode 저장
						logdata.put("work_type", "시스템 재전송"); // work_type 저장
						logdata.put("admin", "system"); // 사용자가 없으므로, system 저장
						logdata.put("result", "정상"); // 실패데이터가 없으므로 정상
						logdata.put("contents", fromdatelog + "~" + todatelog); // 상세내용에 시작시간~종료시간 저장
						this.systemlogService.Edge_Systemlog_Insert(logdata); // 시스템 로그 저장
					}
					
					returnresult.put("result", "ok"); // 결과값 ok
					returnresult.put("reason", "시스템 재전송에 성공했습니다."); // 성공메세지 저장
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return returnresult;
	}

	/**
	 * lbemsDB api에 로그인을 하기 위한 클래스
	 *
	 * @param lbemsDB에 접속하기 위한 ID, PW
	 *
	 * @return map
	 */
	public Map<String, Object> Lbems_Login(HashMap<String, Object> param) throws Exception {
		Map<String, Object> data = new HashMap<String, Object>(); // lbemsdb에 로그인하기 위한 암호화된 id,pw 반환
		Map<String, Object> keydata = new HashMap<String, Object>(); // lbemsdb에 로그인하기 위한 암호화된 id,pw 반환
		Map<String, Object> returndata = new HashMap<String, Object>(); // lbemsdb에 로그인하기 위한 토큰을 담은 맵
		HashMap<String, Object> parsingdata = new HashMap<String, Object>(); // sql에 필요한 조건을 넘겨주는 맵

		parsingdata.putAll(param); // 받아온 id,pw 전체 parsingdata에 저장
		parsingdata.put("target", "EDGE"); // target은 엣지서버이므로 고정
		parsingdata.put("dbname", DBname());
		keydata = this.edgeDBMapper.lbemsLoginData(parsingdata); // 로그인을 해서 데이터(key, token) 가져오기

		data = encrypt(keydata); // id, pw 암호화

		returndata = getLogin(data); // getLogin 함수의 결과값

		return returndata;
	}

	/**
	 * EdgeSystem에 로그인을 하기 위한 클래스
	 *
	 * @param EdgeSystem에 접속하기 위한 ID, PW 확인
	 *
	 * @return map
	 */
	@Override
	public Map<String, Object> Edge_Login(HashMap<String, Object> param) throws Exception {
		Map<String, Object> keydata = new HashMap<String, Object>(); // lbemsdb에 로그인하기 위한 암호화된 id,pw 반환
		Map<String, Object> logdata = new HashMap<String, Object>(); // lbemsdb에 로그인하기 위한 암호화된 id,pw 반환
		Map<String, Object> returndata = new HashMap<String, Object>(); // lbemsdb에 로그인하기 위한 암호화된 id,pw 반환
		HashMap<String, Object> parsingdata = new HashMap<String, Object>(); // sql에 필요한 조건을 넘겨주는 맵
		
		String admin = null;
		String complexcode = "2002"; // 엣지서버 태백산지역코드
		
		if(param != null) { // param이 null이 아닌경우, id가 없으면 로그인이 불가능하므로
			if (param.get("id") == null) { // id가 null일 경우 반환
				returndata.put("result", "nok");
				returndata.put("reason", "id가 없습니다. id를 확인해주세요.");

				return returndata;
			} else {
				admin = param.get("id").toString();
			}
			if (param.get("password") == null) { // password가 null일 경우 반환
				returndata.put("result", "nok");
				returndata.put("reason", "password가 없습니다. password를 확인해주세요.");

				return returndata;
			}
			
			if (param.get("complexcode") != null) { // complexcode가 null이 아닐경우
				complexcode = param.get("complexcode").toString();
			}
		}
		
		try {
			
			parsingdata.putAll(param); // 받아온 id,pw 전체 parsingdata에 저장
			parsingdata.put("complexcode", complexcode);
			parsingdata.put("dbname", DBname());
			keydata = this.edgeDBMapper.edgeLoginData(parsingdata); // 엣지 로그인 처리
			
			if (keydata != null && !keydata.isEmpty()) {
				BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder(); // 단방향 암호화 비밀번호 확인을 위한 선언
				Boolean hashedPassword = passwordEncoder.matches(param.get("password").toString(), // 단방향 암호화 비밀번호 확인
						keydata.get("password").toString());
				
				if (!hashedPassword) { // 비밀번호가 다를 경우
					parsingdata = new HashMap<String, Object>(); // 맵 초기화
					int fail_cnt = 0;
					if(keydata.get("login_fail_cnt") != null) {
						fail_cnt = Integer.parseInt(keydata.get("login_fail_cnt").toString());
					} else {
						fail_cnt = 1;
					}
					
					parsingdata.put("id", admin);
					parsingdata.put("complexcode", complexcode);
					parsingdata.put("login_fail_cnt", fail_cnt);
					parsingdata.put("dbname", DBname());
					this.edgeDBMapper.edgeLoginUpdate(parsingdata);
					
					/*시스템 로그 저장*/
					logdata.put("complex_code_pk", complexcode); // complexcode 저장
					logdata.put("work_type", "로그인"); // worktype 저장
					logdata.put("admin", admin); // 로그인을 시도한 사용자 저장
					logdata.put("result", "실패"); // 비밀번호가 달랐으므로 실패 저장
					logdata.put("reason", "비밀번호가 일치하지 않습니다."); // 비밀번호가 달랐으므로 실패 저장
					logdata.put("contents", "로그인 실패"); // 상세내용 로그인 실패 저장
					this.systemlogService.Edge_Systemlog_Insert(logdata); // 시스템 로그 저장

					returndata.put("result", "nok"); // 로그인 실패 결과 프론트단으로 전송
					returndata.put("reason", "비밀번호가 일치하지 않습니다."); // 로그인 실패 결과 이유 프론트단으로 전송
				} else { // 비밀번호가 같을 경우(성공)
					parsingdata = new HashMap<String, Object>(); // 맵 초기화
					String date = null;
					String firstdate = null;
					String lastdate = null;
					
					
					if(keydata.get("first_login_date") != null) {
						firstdate = keydata.get("first_login_date").toString();
					}
					
					SimpleDateFormat formatter1 = new SimpleDateFormat("yyyy-MM-dd"); // sql에 시간을 넘겨주기 위해서 포맷설정(연도,월,일,시,분,초)
					SimpleDateFormat formatter2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); // sql에 시간을 넘겨주기 위해서 포맷설정(연도,월,일,시,분,초)
					Calendar calF = Calendar.getInstance(Locale.KOREA); // 오늘 날짜
					date = formatter1.format(calF.getTime()); // 뺸달로 fromdate의 값을 설정
					lastdate = formatter2.format(calF.getTime()); // 뺸달로 fromdate의 값을 설정
					
					parsingdata.put("id", admin);
					parsingdata.put("complexcode", complexcode);
					if(firstdate == null) {
						parsingdata.put("first_login_date", date);
					}
					parsingdata.put("last_login_date", lastdate);
					parsingdata.put("login_fail_cnt", 0);
					parsingdata.put("dbname", DBname());
					this.edgeDBMapper.edgeLoginUpdate(parsingdata);
					
					/*시스템 로그 저장*/
					logdata.put("complex_code_pk", complexcode); // complexcode 저장
					logdata.put("work_type", "로그인"); // worktype 저장
					logdata.put("admin", admin); // 로그인을 시도한 사용자 저장
					logdata.put("result", "정상"); // 비밀번호가 같았으므로 성공 저장
					logdata.put("contents", "로그인 성공"); // 상세내용 로그인 성공 저장
					this.systemlogService.Edge_Systemlog_Insert(logdata); // 시스템 로그 저장

					returndata.put("result", "ok"); // 로그인 성공 결과 프론트단으로 전송
					returndata.put("reason", "로그인에 성공했습니다."); // 로그인 실패 결과 이유 프론트단으로 전송
				}
				
			} else {
				/*시스템 로그 저장*/
				logdata.put("complex_code_pk", complexcode); // complexcode 저장
				logdata.put("work_type", "로그인"); // worktype 저장
				logdata.put("admin", admin); // 로그인을 시도한 사용자 저장
				logdata.put("result", "실패"); // 비밀번호가 달랐으므로 실패 저장
				logdata.put("reason", "해당하는 사용자가 없습니다."); // 비밀번호가 달랐으므로 실패 저장
				logdata.put("contents", "로그인 실패"); // 상세내용 로그인 실패 저장
				this.systemlogService.Edge_Systemlog_Insert(logdata); // 시스템 로그 저장

				returndata.put("result", "nok"); // 로그인 실패 결과 프론트단으로 전송
				returndata.put("reason", "해당하는 사용자가 없습니다. Kevinlab에 문의해주세요."); // 로그인 실패 결과 이유 프론트단으로 전송
			}

		} catch (Exception e) {
			e.printStackTrace();
			
			/* 로그인 함수 수행중 오류가 발생했을 때 시스템 로그를 저장하기 위한 data생성 후 update*/
			logdata.put("complex_code_pk", complexcode); // complexcode 저장
			logdata.put("work_type", "로그인"); // worktype 저장
			logdata.put("admin", admin); // 로그인 시도한 사용자 저장
			logdata.put("result", "실패"); // java오류가 나서 실패했기 때문에 결과값 실패 저장
			logdata.put("reason", e.getCause()); // 실패한 이유에 대한 java로그 저장
			logdata.put("contents", "로그인 실패"); // 상세내용 로그인 실패 저장
			this.systemlogService.Edge_Systemlog_Insert(logdata); // 시스템 로그 저장
		}
		return returndata;
	}
	
	/**
	 * EdgeSystem 백업기간을 설정하기 위한 클래스
	 *
	 * @param EdgeSystem에 백업기간 설정을 위한 정보
	 *
	 * @return map
	 */
	@Override
	public Map<String, Object> Edge_Set_BackUpDate(HashMap<String, Object> param) throws Exception {
		Map<String, Object> pkdata = new HashMap<String, Object>(); // edgedb에 중복시 pk값을 제외한 값을 업데이트 하기 위한 맵
		Map<String, Object> setdata = new HashMap<String, Object>(); // edgedb에 저장하기위해 백업기간과 정보를 정리하는 맵
		Map<String, Object> logdata = new HashMap<String, Object>(); // edgedb 시스템 로그에 저장하기 위한 로그 맵
		Map<String, Object> returndata = new HashMap<String, Object>(); // 백업기간 설정 결과를 담은 맵
		Map<String, Object> insertdata1 = new HashMap<String, Object>(); // lbemsdb에 로그인하기 위한 암호화된 id,pw 반환
		Map<String, Object> insertdata2 = new HashMap<String, Object>(); // lbemsdb에 로그인하기 위한 암호화된 id,pw 반환
		
		String complexcode = null;
		String setdate = null;
		String admin = null;

		if (param != null) { // param(즉, 프론트단에서 주는 정보)가 null이 아닐때
			if (param.get("setdate") != null) { // setdate(백업기간)이 null이 아닐때
				setdate = param.get("setdate").toString(); // 받아온 백업기간 저장
			} else {
				returndata.put("result", "nok");
				returndata.put("reason", "setdate(설정시간)가 없습니다. setdate(설정시간)값을 입력해주세요");
				return returndata;
			}

			if (param.get("complexcode") != null) { // complexcode가 null이 아닐때
				complexcode = param.get("complexcode").toString(); // 받아온 complexcode 저장
			} else {
				returndata.put("result", "nok");
				returndata.put("reason", "complexcode가 없습니다. complexcode를 입력해주세요");
				return returndata;
			}

			if (param.get("admin") != null) { // 사용자 로그인후 사용하므로, 사용자 ID를 param에 담겨있어야한다
				admin = param.get("admin").toString(); // 받아온 사용자이름 저장
			} else {
				returndata.put("result", "nok");
				returndata.put("reason", "admin가 없습니다. admin를 입력해주세요");
				return returndata;
			}
		}

		try {
			
			int minusdate = Integer.parseInt(setdate); // 받아온 백업기간 int형으로 변환
			
			setdata.put("complex_code_pk", complexcode); // complexcode값 저장
			setdata.put("set_backup_info", minusdate); // 백업기간 설정
			
			List<String> keyList = new ArrayList<>(setdata.keySet()); // 설정한 setdata의 키값으로 리스트 생성
			List<Object> valueList = new ArrayList<>(setdata.values()); // 설정한 setdata의 키값으로 리스트 생성
			List<Map<String, Object>> result1 = new ArrayList<Map<String, Object>>(); // pk값을 포함한 전체컬럼을 insert하는 리스트맵
			List<Map<String, Object>> result2 = new ArrayList<Map<String, Object>>(); // pk값을 제외한 컬럼을 update하기위한 리스트맵
			
			for(int i=0; i<keyList.size(); i++) { // 키값으로 만든 리스트 크기만큼 반복
				insertdata1 = new HashMap<String, Object>(); // 맵 초기화
				
				insertdata1.put("name", keyList.get(i)); // key리스트의 값을 name이라는 키값으로 저장
				insertdata1.put("val", valueList.get(i)); // value리스트의 값을 val이라는 키값으로 저장
				
				if(keyList.get(i).equals("set_backup_info")) { // set_backup_info컬럼을 제외한 나머지는 pk값이다
					pkdata.put("name", keyList.get(i)); // set_backup_info을 pkdata에 name key값으로 저장
					pkdata.put("val", valueList.get(i)); // set_backup_info의 값을 pkdata에 val key값으로 저장
					result2.add(pkdata);
				}
				
				result1.add(insertdata1);
			}
			
			insertdata2.put("result1", result1);
			insertdata2.put("result2", result1);
			insertdata2.put("dbname", DBname());

			if(this.edgeDBMapper.edgeSetDate(insertdata2) > 0) { // 백업기간 설정에 성공했을때
				/*시스템 로그 저장*/
				logdata.put("complex_code_pk", complexcode); // complexcode 저장
				logdata.put("work_type", "백업기간 설정"); // worktype저장
				logdata.put("admin", admin); // 사용자 저장
 				logdata.put("result", "정상"); // 결과값 저장(edgeSetDate(map)이 0보다 크면 정상)
				logdata.put("contents", setdate + "개월"); // 상세내용에 설정한 백업기간 기록
				this.systemlogService.Edge_Systemlog_Insert(logdata); // 시스템 로그 저장
				
				returndata.put("result", "ok"); // 결과가 성공했기 때문에 ok 저장
				returndata.put("reason", "백업기간 설정이 완료되었습니다."); // 백업기간 설정이 완료되었다고 성공메세지 멥에 저장
			} else {
				/*시스템 로그 저장*/
				logdata.put("complex_code_pk", complexcode); // complexcode 저장
				logdata.put("work_type", "백업기간 설정"); // worktype저장
				logdata.put("admin", admin); // 사용자 저장
				logdata.put("result", "실패"); // 결과값 저장(edgeSetDate(map)이 0보다 작으므로 실패)
				logdata.put("contents", setdate + "개월"); // 상세내용에 설정한 백업기간 기록
				this.systemlogService.Edge_Systemlog_Insert(logdata); // 시스템 로그 저장
				
				returndata.put("result", "nok"); // 결과가 실패했기 때문에 nok
				returndata.put("reason", "기간 설정중에 오류가 발생되었습니다. 다시 한번 시도해주세요."); // 백업기간 설정이 실패했다는 메세지 맵에 저장
			}
		} catch (Exception e) {
			e.printStackTrace();
			
			/* java클래스 수행중 오류가 발생했을 때 시스템 로그를 저장하기 위한 data생성 후 update*/
			logdata.put("complex_code_pk", complexcode); // complexcode 저장
			logdata.put("work_type", "백업기간 설정"); // worktype저장
			logdata.put("admin", admin); // 사용자 저장
			logdata.put("result", "실패"); // 결과값 저장(edgeSetDate(map)이 0보다 작으므로 실패)
			logdata.put("reason", e.getCause()); // 상세내용에 java가 실패한 이유 기록
			this.systemlogService.Edge_Systemlog_Update(logdata); // 시스템 로그 저장
		}
		return returndata;
	}

	/**
	 * EdgeSystem에 데이터 삭제를 위한 클래스
	 *
	 * @param void
	 *
	 * @return void
	 */
	@Override
	public void Edge_BackUp() throws Exception {
		Map<String, Object> data = new HashMap<String, Object>(); // 백업기간과 테이블이름을 저장한 맵
		Map<String, Object> date = new HashMap<String, Object>(); // 백업기간을 받기위한 맵
		Map<String, Object> logdata = new HashMap<String, Object>(); // 시스템 로그를 기록하기 위한 맵
		Map<String, Object> checkdata = new HashMap<String, Object>(); // val_date가 존재하는지 check하기 위한 맵
		Map<String, Object> check = new HashMap<String, Object>(); // 체크된 결과를 받기 위한 맵
		List<Map<String, Object>> result = new ArrayList<Map<String, Object>>(); // list형식으로 insert하기 위한 맵
		List<String> tablename = new ArrayList<String>(); // 테이블 이름 리스트
		HashMap<String, Object> parsingdata = new HashMap<String, Object>(); // sql에 필요한 조건을 넘겨주는 맵
		
		String fromdate = null;
		String setdate = null;
		String complexcode = "2002"; // 태백산 지역코드 2002 설정

		try {
	        String dbname = DBname();
			
			parsingdata.put("complexcode", complexcode); // complexcode를 맵에 저장
			parsingdata.put("dbname", DBname()); // DB이름을 저장
			
			date = this.edgeDBMapper.edgeGetDate(parsingdata); // edgeGetDate로 백업기간 불러오기
			
			if (date.get("setdate") != null) { // date의 값의 setdate(백업기간)이 null이 아닐때
				setdate = date.get("setdate").toString(); // setdate값 변수에 저장

				int minusdate = Integer.parseInt(setdate); // 백업기간을 int형으로 변환
				int resultok = 0; // flag를 위한 변수 선언

				SimpleDateFormat formatter1 = new SimpleDateFormat("yyyyMMddHHmmss"); // sql에 시간을 넘겨주기 위해서 포맷설정(연도,월,일,시,분,초)

				Calendar calF = Calendar.getInstance(Locale.KOREA); // 오늘 날짜

				calF.add(Calendar.MONTH, -minusdate); // 백업기간만큼 현재달에서 뺀다

				fromdate = formatter1.format(calF.getTime()); // 뺸달로 fromdate의 값을 설정

				result = this.edgeDBMapper.edgeTableNames(parsingdata); // edgeTableNames 가져오기

				tablename = result.stream().filter(t -> t.containsKey("Tables_in_" + dbname))
						.map(m -> m.get("Tables_in_" + dbname).toString()).collect(Collectors.toList()); // tablename(테이블이름으로 된 리스트) 생성

				if (tablename.size() > 0) { // 테이블리스트가 0이상일때
					for (int i = 0; i < tablename.size(); i++) { // 테이블리스트 크기만큼 반복
						data = new HashMap<String, Object>(); // 맵초기화
						String table = tablename.get(i); // 테이블리스트 순서대로 이름 출력후 저장

						/*각 테이블별 reg_date를 제외한 확인가능한 date로 val_date를 각 테이블에 맞게 변경*/
						if (tablename.get(i).equals("bems_admin_login_log")) {
							data.put("log_date", fromdate);
							data.put("tablename", table);
						} else if (tablename.get(i).equals("bems_alarm_log")) {
							data.put("alarm_off_time", fromdate);
							data.put("tablename", table);
						} else if (tablename.get(i).equals("bems_api_call_log")) {
							data.put("log_date", fromdate);
							data.put("tablename", table);
						} else if (tablename.get(i).equals("bems_autologin")) {
							data.put("reg_date", fromdate);
							data.put("tablename", table);
						} else {
							/*위에 해당하는 테이블을 제외한 나머지 테이블에 val_date가 있는 테이블에 한해서만 삭제를 하기위해 edgeCheckdata로 확인*/
							checkdata.put("columnname", "val_date");
							checkdata.put("tablename", table);
							checkdata.put("dbname", DBname()); // DB이름을 저장
							check = this.edgeDBMapper.edgeCheckdata(checkdata);
							int checkflag = Integer.parseInt(check.get("flag").toString());
							if (checkflag == 1) { // checkflag가 1일때는 val_date라는 컬럼이 존재
								data.put("val_date", fromdate);
								data.put("tablename", table);
							}
						}
						
						data.put("dbname", DBname()); // DB이름을 저장
						resultok = this.edgeDBMapper.edgeBackup(data); // 위에서 저장한 정보대로 edgeBackup 즉, 설정한 백업기간만큼 남기고 삭제
					}
				}

				if (resultok > 0) { // resultok 즉, 삭제가 정상적으로 이루어지면 resultok는 0보다 크다
					logdata.put("complex_code_pk", complexcode); // complexcode 저장
					logdata.put("work_type", "데이터 삭제"); // worktype 저장
					logdata.put("admin", "system"); // 자동 삭제이므로, admin은 무조건 system
					logdata.put("result", "정상"); // 0보다 큰 값이므로, 삭제는 정상적으로 이루어져서 정상
					logdata.put("contents", "정기삭제"); // 상세내용으로 정기삭제 저장
					this.systemlogService.Edge_Systemlog_Insert(logdata); // 시스템 로그 저장
				} else { // 삭제에 실패했을 경우
					logdata.put("complex_code_pk", complexcode); // complexcode 저장
					logdata.put("work_type", "데이터 삭제"); // worktype 저장
					logdata.put("admin", "system"); // 자동 삭제이므로, admin은 무조건 system 
					logdata.put("result", "실패"); // 0보다 큰 값이므로, 삭제는 정상적으로 이루어져서 정상
					logdata.put("contents", "정기삭제"); // 상세내용으로 정기삭제 저장
					this.systemlogService.Edge_Systemlog_Insert(logdata); // 시스템 로그 저장
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			
			/* java클래스 수행중 오류가 발생했을 때 시스템 로그를 저장하기 위한 data생성 후 update*/
			logdata.put("complex_code_pk", complexcode); // complexcode 저장
			logdata.put("work_type", "데이터 삭제"); // worktype 저장
			logdata.put("admin", "system"); // 자동 삭제이므로, admin은 무조건 system
			logdata.put("result", "실패"); // 0보다 큰 값이므로, 삭제는 정상적으로 이루어져서 정상
			logdata.put("reason", e.getCause()); // 이유로는 java가 실패한 이유를 저장
			this.systemlogService.Edge_Systemlog_Update(logdata); // 시스템 로그 업데이트
		}
	}

	/**
	 * lbems api에 접속
	 *
	 * @param 로그인시 발행되는 token 가져오기
	 *
	 * @return map
	 */
	public Map<String, Object> getLogin(Map<String, Object> param) {
		Map<String, Object> returndata = new HashMap<String, Object>(); // 반환하기 위한 맵
		Map<String, Object> headerdata = new HashMap<String, Object>(); // Api 통신 응답 헤더 맵
		String returnString = null;
		try {
//			String apiURL = "http://www.lbems.com/kbet/login.php"; // api url(실서버)
			String apiURL = "http://lbems.4st.co.kr/kbet/login.php"; // api url(개발 서버)
			URL url = new URL(apiURL); // 위에서 적은 api로 url 생성
			HttpURLConnection con = (HttpURLConnection) url.openConnection(); // urlconnection 즉, url 개통
			StringBuilder result = new StringBuilder(); // hearder의 내용을 읽기위한 스트링빌더 생성
			boolean first = true;
			/* api 통신을 위한 json으로 param값을 변경하기 위한 코드  */
			for (Map.Entry<String, Object> entry : param.entrySet()) {
				if (first)
					first = false;
				else
					result.append("&");
				result.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
				result.append("=");
				result.append(URLEncoder.encode((String) entry.getValue(), "UTF-8"));
			}
			String JsonValue = result.toString(); // json으로 변경한 param을 변수에 저장

			con.setRequestMethod("POST"); // 통신method 설정
			con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded"); // JSON DATA로 통신
			con.setRequestProperty("Accept-Charset", "UTF-8"); // UTF-8형식으로 통신
			con.setUseCaches(false); // 컨트롤 캐쉬 설정(캐시사용 X)
			con.setDoInput(true); // InputStream으로 서버로 부터 응답을 받겠다는 옵션.
			con.setDoOutput(true); // OutputStream으로 POST 데이터를 넘겨주겠다는 옵션.

			OutputStream os = con.getOutputStream(); // OutputStream 선언
			os.write(JsonValue.getBytes("UTF-8")); // JsonValue를 UTF-8형식으로 변환은 OutputStream에 쓰기
			os.flush(); // OutputStream 출력

			int responseCode = con.getResponseCode(); // 통신 코드 받아오기
			BufferedReader br;
			if (responseCode == 200) { // 정상 호출
				br = new BufferedReader(new InputStreamReader(con.getInputStream()));
			} else { // 에러 발생
				br = new BufferedReader(new InputStreamReader(con.getErrorStream()));
			}
			String inputLine;
			String res = new String();
			while ((inputLine = br.readLine()) != null) { // 에러 발생시 또는 결과값 출력
				res += inputLine;
			}

			// 응답 헤더의 정보를 모두 출력
			for (Map.Entry<String, List<String>> header : con.getHeaderFields().entrySet()) {
				for (String value : header.getValue()) {
					headerdata.put(header.getKey(), value); // 읽은 헤더의 내용을 맵에 저장
				}
			}

			if (headerdata.get("Authorization") != null) { // authorization에 토큰값이 저장되므로 없으면 오류 출력
				returnString = "{\"token\" : \"" + headerdata.get("Authorization").toString() + "\"}"; // 읽은 토큰값을 json형식으로 변경(map에 담기위해서)
				returndata.put("token", returnString);
			} else {
				returnString = res; // 토큰이 없으면 없는 이유에 대해 담아서 반환
				returndata.put("token", returnString);
			}
			br.close(); // bufferReader 종료
			con.disconnect(); // url connect 종료
		} catch (Exception e) {
			e.printStackTrace();
		}

		return returndata; // 결과값 반환
	}

	/**
	 * 데이터 재전송 버튼 클릭시 EdgeDB에서 데이터를 가져와 lbemsdb로 api 전송
	 *
	 * @param 로그인시 발행되는 token과 EdgeDB에서 가져온 데이터맵
	 *
	 * @return Map
	 */
	public Map<String, Object> getSyncApi(HashMap<String, Object> param, String token) {
		Map<String, Object> parsingdata = new HashMap<String, Object>(); // Api 통신 결과값 보내는 맵
		try {
//			String apiURL = "http://www.lbems.com/kbet/sync_data.php"; // api url(실서버)
			String apiURL = "http://lbems.4st.co.kr/kbet/sync_data.php"; // 개발서버
			URL url = new URL(apiURL); // 위에서 적은 api로 url 생성
			HttpURLConnection con = (HttpURLConnection) url.openConnection(); // urlconnection 즉, url 개통
			ObjectMapper objectMapper = new ObjectMapper(); // json형식으로 변경하여 저장하기위한 objectMapper
			String JsonValue = objectMapper.writeValueAsString(param); // String값으로 param(EdgeDB 데이터)값 변환
			
			con.setRequestMethod("POST"); // 통신method 설정
			con.setRequestProperty("Authorization", "Bearer " + token);
			con.setRequestProperty("Content-Type", "application/json"); // JSON DATA로 통신
			con.setRequestProperty("Accept-Charset", "UTF-8"); // UTF-8형식으로 통신
			con.setReadTimeout(100); // 읽기대기시간 100밀리세컨드
			con.setUseCaches(false); // 컨트롤 캐쉬 설정(캐시사용 X)
			con.setDoInput(true); // InputStream으로 서버로 부터 응답을 받겠다는 옵션.
			con.setDoOutput(true); // OutputStream으로 POST 데이터를 넘겨주겠다는 옵션.
			
			OutputStream os = con.getOutputStream(); // OutputStream 선언
			os.write(JsonValue.getBytes("UTF-8")); // JsonValue를 UTF-8형식으로 변환은 OutputStream에 쓰기
			os.flush(); // OutputStream 출력

			int responseCode = con.getResponseCode(); // 통신 코드 받아오기
			BufferedReader br;
			if (responseCode == 200) { // 정상 호출
				br = new BufferedReader(new InputStreamReader(con.getInputStream()));
			} else { // 에러 발생
				br = new BufferedReader(new InputStreamReader(con.getErrorStream()));
				parsingdata.put("error", responseCode); // 오류의 이유에 대해서 반환하기위해 맵에 저장
			}
			
			String inputLine;
			String res = new String();
			while ((inputLine = br.readLine()) != null) { // 에러 발생시 또는 결과값 출력
				res += inputLine;
			}
			
			parsingdata = objectMapper.readValue(res, new TypeReference<Map<String, Object>>() {}); // 받은 결과를 map형식으로 변환하여 저장

			br.close(); // bufferReader 종료
			con.disconnect(); // url connect 종료
		} catch (Exception e) {
			e.printStackTrace();
		}

		return parsingdata; // 결과값 반환
	}

	/**
	 * Edge와 lbems의 장비 동기화
	 *
	 * @param 로그인시 필요한 ID, PW
	 *
	 * @return Map
	 */
	@Override
	public Map<String, Object> equipmentSync(HashMap<String, Object> param) throws Exception {

		Map<String, Object> data = new HashMap<String, Object>(); // 프론트단에 장비동기화 결과를 반환하기 위한 map
		Map<String, Object> logdata = new HashMap<String, Object>(); // log에 기록하기 위한 map
		Map<String, Object> valdata = new HashMap<String, Object>(); // edgedb에 장비를 동기화하기 위한 map
		HashMap<String, Object> parsingdata = new HashMap<String, Object>(); // lbemsdb에 장비를 읽어오기 위한 조건을 저장하기 위한 map
		List<Map<String, Object>> columnsname = new ArrayList<Map<String, Object>>(); // 각 테이블별 컬럼이름을 읽어오기 위한 map
		List<Map<String, Object>> result1 = new ArrayList<Map<String, Object>>(); // list로 insert하기 위한 리스트맵
		List<Map<String, Object>> result2 = new ArrayList<Map<String, Object>>(); // list로 insert하기 위한 리스트맵
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;

		/* lbemsdb에 직접 연결하기 위한 연결에 필요한 정보 */
		Class.forName("com.mysql.jdbc.Driver");
		String url = "jdbc:mysql://db-acpe3.pub-cdb.ntruss.com:3306/lbems_db";
		String user = "lbemsuser";
		String passwd = "lbemsuser!";
		String complexcode = "2002"; // 태백산 위치코드인 2002
		String admin = null; // 사용자가 장비동기화 하는 여부를 파악하기 위한 변수
		
		if (param != null) { // param이 null인지 확인하기 위한 이유는 정기 장비동기화인지 사용자의 요청에 장비동기화인지 판단하기위함
			if (!param.isEmpty()) {
				if (param.get("admin") != null) {
					admin = param.get("admin").toString(); // admin(사용자) 저장
				}
			}
		}

		try {
			
			SimpleDateFormat formatterlog = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); // log에 기록하기 위한 장비동기화 시작시간
			
			String fromdatelog = null;
			String todatelog = null;
			
			Calendar calFlog = Calendar.getInstance(Locale.KOREA); // 오늘 날짜
			fromdatelog = formatterlog.format(calFlog.getTime()); // 데이터 재전송 시작시간

			/* 장비를 기록한 센서테이블목록 */
			String[] Sensorlist = { "bems_complex", "bems_home", "bems_sensor_solar", "bems_sensor_electric",
					"bems_sensor_electric_elechot", "bems_sensor_electric_hotwater", "bems_sensor_electric_heating",
					"bems_sensor_electric_cold", "bems_sensor_electric_light", "bems_sensor_electric_vent",
					"bems_sensor_electric_elevator", "bems_sensor_electric_water", "bems_sensor_electric_equipment",
					"bems_sensor_electric_boiler" };

			conn = DriverManager.getConnection(url, user, passwd); // DB연결

			for (int i = 0; i < Sensorlist.length; i++) {
				parsingdata.put("table_name", Sensorlist[i]); // 위에서 선언한 row테이블 배열 parsingdata에 저장
				parsingdata.put("dbname", DBname());
				columnsname = this.edgeDBMapper.columnsName(parsingdata); // 각 row테이블의 컬럼네임 가져오기
				
				/* 컬럼이름을 저장하는 리스트맵을 3개나 만든이유는 pk컬럼과 그냥 컬럼을 구분하는것과 sql을 통하지 않고 직접연결하는거기 떄문에 컬럼이름에 ``을 붙이기 위해서 총 3개를 사용 */
				List<String> namelist1 = new ArrayList<String>(); // 컬럼이름을 저장하기 위한 리스트맵1(컬럼이름 기본)
				List<String> namelist2 = new ArrayList<String>(); // 컬럼이름을 저장하기 위한 리스트맵2(컬럼이름에 ``추가)
				List<String> namelist3 = new ArrayList<String>(); // 컬럼이름을 저장하기 위한 리스트맵3(pk컬럼 제외)

				namelist1 = columnsname.stream().filter(t -> t.containsKey("COLUMN_NAME"))
						.map(m -> m.get("COLUMN_NAME").toString()).collect(Collectors.toList());
				for (int j = 0; j < namelist1.size(); j++) {
					namelist2.add("`" + namelist1.get(j) + "`");
				}
				for (int j = 0; j < columnsname.size(); j++) {
					String key = columnsname.get(j).get("COLUMN_KEY").toString();
					if (key != null && !key.equals("")) {
						namelist3.add(columnsname.get(j).get("COLUMN_NAME").toString());
					}
				}
				pstmt = conn.prepareStatement("select " + namelist2.toString().replace("[", "").replace("]", "").trim()
						+ " from " + Sensorlist[i] + " where complex_code_pk=\"" + complexcode + "\";");
				rs = pstmt.executeQuery();

				while (rs.next()) { // 위에서 검색한 센서갯수만큼 반복
					parsingdata = new HashMap<String, Object>();
					result1 = new ArrayList<Map<String, Object>>();
					result2 = new ArrayList<Map<String, Object>>();
					
					for (int j = 0; j < namelist2.size(); j++) { // 위에서 검색한 컬럼이름만큼 반복
						valdata = new HashMap<String, Object>(); // 맵 초기화

						valdata.put("name", namelist2.get(j)); // name이라는 키로 컬럼이름 저장
						valdata.put("val", rs.getString(namelist1.get(j))); // val이라는 키로 각 컬럼별 검색한값 저장

						/* pk컬럼을 구분해서 저장하기위한 코드이나 namelist1을 사용하는 이유는 namelist2가 ``를 추가해서 사용했기 때문에 */
						if (!namelist3.contains(namelist1.get(j)) || namelist1.get(j).equals("reg_date")) {
							valdata.put("name", namelist2.get(j));
							valdata.put("val", rs.getString(namelist1.get(j)));
							result2.add(valdata);
						}

						result1.add(valdata);
					}

					parsingdata.put("result1", result1);
					parsingdata.put("result2", result2);
					parsingdata.put("sensor", Sensorlist[i]);
					parsingdata.put("dbname", DBname());

					if (this.edgeDBMapper.edgeEquipmentSync(parsingdata) > 0) { // 가져온 센서를 edgeDB에 insert(0보다 크면 성공, 아니면 실패)
						data.put("result", "ok");
						data.put("reason", "완료했습니다.");
					} else {
						data.put("result", "nok");
						data.put("reason", "장비동기화 도중 오류가 발생했습니다.");
					}
				}
				
				Calendar calTlog = Calendar.getInstance(Locale.KOREA); // 오늘 날짜
				todatelog = formatterlog.format(calTlog.getTime()); // 데이터 재전송 시작시간
			}
			
			String resultok = null;
			
			if(!data.isEmpty()) { // java오류가 나지 않으면 위에서 입력한 data의 값이 나온다.
				resultok = data.get("result").toString(); // result값 저장
			}
			
			if(resultok.equals("nok")) { // 실패일 경우
				if(admin != null) { // 사용자가 요청했을 경우
					logdata.put("complex_code_pk", complexcode);
					logdata.put("work_type", "장비 동기화");
					logdata.put("admin", admin);
					logdata.put("result", "실패");
					logdata.put("contents", fromdatelog + "~" + todatelog); // 상세내용(시작시간 ~ 끝난시간)
					this.systemlogService.Edge_Systemlog_Insert(logdata);
				} else { // 시스템에 의한 정기 장비동기화일 경우
					logdata.put("complex_code_pk", complexcode);
					logdata.put("work_type", "장비 동기화");
					logdata.put("admin", "system");
					logdata.put("result", "실패");
					logdata.put("contents", fromdatelog + "~" + todatelog); // 상세내용(시작시간 ~ 끝난시간)
					this.systemlogService.Edge_Systemlog_Insert(logdata);
				}
			} else { // 성공일 경우
				if(admin != null) { // 사용자가 요청했을 경우
					logdata.put("complex_code_pk", complexcode);
					logdata.put("work_type", "장비 동기화");
					logdata.put("admin", admin);
					logdata.put("result", "정상");
					logdata.put("contents", fromdatelog + "~" + todatelog); // 상세내용(시작시간 ~ 끝난시간)
					this.systemlogService.Edge_Systemlog_Insert(logdata);
				} else { // 시스템에 의한 정기 장비동기화일 경우
					logdata.put("complex_code_pk", complexcode);
					logdata.put("work_type", "장비 동기화");
					logdata.put("admin", "system");
					logdata.put("result", "정상");
					logdata.put("contents", fromdatelog + "~" + todatelog); // 상세내용(시작시간 ~ 끝난시간)
					this.systemlogService.Edge_Systemlog_Insert(logdata);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			
			/* java클래스 수행중 오류가 발생했을 때 시스템 로그를 저장하기 위한 data생성 후 update*/
			if(admin != null) {
				logdata.put("complex_code_pk", complexcode);
				logdata.put("work_type", "장비 동기화");
				logdata.put("admin", admin);
				logdata.put("result", "실패");
				logdata.put("reason", e.getCause());
				this.systemlogService.Edge_Systemlog_Update(logdata);
			} else {
				logdata.put("complex_code_pk", complexcode);
				logdata.put("work_type", "장비 동기화");
				logdata.put("admin", "system");
				logdata.put("result", "실패");
				logdata.put("reason", e.getCause());
				this.systemlogService.Edge_Systemlog_Update(logdata);
			}
		} finally {
			rs.close();
			pstmt.close();
			conn.close(); // 데이터베이스 연결 종료
		}
		
		return data;
	}

	/**
	 * key값을 통한 ID,PW 암호화
	 *
	 * @param 로그인시 필요한 ID, PW
	 *
	 * @return Map
	 */
	public Map<String, Object> encrypt(Map<String, Object> param) throws Exception {

		Map<String, Object> data = new HashMap<String, Object>(); // lbemsdb에 로그인하기 위한 암호화된 id,pw 반환하기 위한 map

		String alg = "AES/CBC/PKCS5Padding"; // 엣지서버 외부 api 규격서에 따른 암호화 방식
		String id = null;
		String password = "edge2831@!";
		String key = null;
		String iv = null;

		// 받아온 id,pw,client_key,iv_key를 대입
		if (param.get("id") != null) {
			id = param.get("id").toString();
		}
		if (param.get("client_key") != null) {
			key = param.get("client_key").toString();
			key = toStringHex(key); // lbemsdb에 16진수로 키값이 저장되있므로 10진수로 변환
		}
		if (param.get("iv_key") != null) {
			iv = param.get("iv_key").toString();
			iv = toStringHex(iv); // lbemsdb에 16진수로 키값이 저장되있므로 10진수로 변환
		}

		Cipher cipher = Cipher.getInstance(alg); // 자바의 암호화, 복호화를 담당하는 클래스 Cipher를 불러서 암호화 실시
		SecretKeySpec keySpec = new SecretKeySpec(key.getBytes(), "AES"); // 주어진 바이트와 알고리즘(AES)를 활용하여 SecretKeySpec를 생성한다.
		IvParameterSpec ivParamSpec = new IvParameterSpec(iv.getBytes()); // 주어진 바이트로 ivParamSpec을 생성한다.
		cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivParamSpec); // init 즉, Cipher클래스를 keySpec, ivParamSpec를 활용하여 암호화모드로 초기화한다.

		byte[] encrypted1 = cipher.doFinal(id.getBytes("UTF-8")); // id의 UTF-8형식의 암호화
		byte[] encrypted2 = cipher.doFinal(password.getBytes("UTF-8")); // pw UTF-8형식 암호화
		id = Base64.getEncoder().encodeToString(encrypted1); // id가 암호화 되었기때문에 특수기호들을 그자체로 인식하기 위해서 Base64형식으로 인코딩
		password = Base64.getEncoder().encodeToString(encrypted2); // pw가 암호화 되었기때문에 특수기호들을 그자체로 인식하기 위해서 Base64형식으로 인코딩

		data.put("id", id); // 반환할 맵에 id 담기
		data.put("pw", password); // 반환할 맵에 pw 담기

		return data;
	}

	/**
	 * 키값으로 암호화 된 값 복호화를 위해 만들었으나, 사용처가 정해지지 않아서 주석처리
	 *
	 * @param 로그인시 필요한 ID, PW
	 *
	 * @return String
	 */
//	public String decrypt(Map<String, Object> param) throws Exception {
//
//		Map<String, Object> data = new HashMap<String, Object>(); // lbemsdb에 로그인하기 위한 암호화된 id,pw 반환
//
//		String alg = "AES/CBC/PKCS5Padding";
//		String password = null;
//		String key = null;
//		String iv = null;
//
//		if (param.get("password") != null) {
//			password = param.get("password").toString();
//		}
//		if (param.get("client_key") != null) {
//			key = param.get("client_key").toString().substring(0, 32);
//		}
//		if (param.get("iv_key") != null) {
//			iv = param.get("iv_key").toString().substring(0, 16);
//		}
//
//		Cipher cipher = Cipher.getInstance(alg);
//		SecretKeySpec keySpec = new SecretKeySpec(key.getBytes(), "AES");
//		IvParameterSpec ivParamSpec = new IvParameterSpec(iv.getBytes());
//		cipher.init(Cipher.DECRYPT_MODE, keySpec, ivParamSpec);
//
//		BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
//		BCryptPasswordDecoder passwordDecoder = new BCryptPasswordDecoder();
//
//		byte[] decodedBytes = Base64.getDecoder().decode(password);
//		byte[] decrypted = cipher.doFinal(decodedBytes);
//
//		return new String(decrypted, "UTF-8");
//	}

	/**
	 * client key, iv key 값이 16진수로 DB에 저장되있기에 암호화를 위해 10진수로 복호화
	 *
	 * @param 16진수로 DB에 저장되어있는 키값
	 *
	 * @return String
	 */
	public String toStringHex(String s) throws Exception {

		byte[] baKeyword = new byte[s.length() / 2]; // 16진수이므로 받아온 길이의 절반으로 자른다.

		for (int i = 0; i < baKeyword.length; i++) {
			try {
				// 16진수의 가장 기본적인 값을 통한 정수값 계산식을 통해 10진수로 변환
				baKeyword[i] = (byte) (0xff & Integer.parseInt(s.substring(i * 2, i * 2 + 2), 16));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		try {
			s = new String(baKeyword, "utf-8");// UTF-16le:Not 즉, utf-8형식으로 변환
		} catch (Exception e1) {
			e1.printStackTrace();
		}

		return s; // 10진수로 변환한 키값 반환
	}

	/**
	 * EdgeDB 일별 테이블 전체 사용량과 23시 사용량 업데이트
	 *
	 * @param void
	 *
	 * @return void
	 */
	@Override
	public void Edge_Day_Update() throws Exception {
		List<Map<String, Object>> data = new ArrayList<Map<String, Object>>();
		List<Map<String, Object>> sensorName = new ArrayList<Map<String, Object>>();
		Map<String, Object> hourdata = new HashMap<String, Object>();
		HashMap<String, Object> parsingdata = new HashMap<String, Object>(); // sql에 필요한 조건을 넘겨주는 맵
		String fromdate = null;
		String fivefromdate = null;
		String todate = null;
		String todaydate = null;
		String todaytodate = null;
		String complexcode = "2002"; // 태백산 지역코드인 2002

		try {

			// 각 sensor테이블의 이름으로 된 배열 정의
			String[] Sensorlist = { "bems_sensor_solar", "bems_sensor_electric", "bems_sensor_electric_elechot",
					"bems_sensor_electric_hotwater", "bems_sensor_electric_heating", "bems_sensor_electric_cold",
					"bems_sensor_electric_light", "bems_sensor_electric_vent", "bems_sensor_electric_elevator",
					"bems_sensor_electric_water", "bems_sensor_electric_equipment", "bems_sensor_electric_boiler" };
			// 각 row테이블의 이름으로 된 배열 정의
			String[] DBlist = { "bems_meter_solar", "bems_meter_electric", "bems_meter_electric_elechot",
					"bems_meter_electric_hotwater", "bems_meter_electric_heating", "bems_meter_electric_cold",
					"bems_meter_electric_light", "bems_meter_electric_vent", "bems_meter_electric_elevator",
					"bems_meter_electric_water", "bems_meter_electric_equipment", "bems_meter_electric_boiler" };
			// 각 에너지원별의 이름으로 된 배열 정의
			String[] namelist = { "solar", "electric", "electric_elechot", "electric_hotwater", "electric_heating",
					"electric_cold", "electric_light", "electric_vent", "electric_elevator", "electric_water",
					"electric_equipment", "electric_boiler" };

			SimpleDateFormat formatter1 = new SimpleDateFormat("yyyyMMdd"); // sql에 시간을 넘겨주기 위해서 포맷설정(연도,월,일)
			SimpleDateFormat formatter2 = new SimpleDateFormat("yyyyMMddHHmmss"); // sql에 시간을 넘겨주기 위해서 포맷설정(연도,월,일,시,분,초)

			Calendar calF = Calendar.getInstance(Locale.KOREA); // 오늘 날짜
			Calendar calT = Calendar.getInstance(Locale.KOREA); // 오늘 날짜
			
			calF.add(Calendar.DATE, -1); // 00시 30분에 돌기 때문에 하루를 뺀다.
			calT.add(Calendar.DATE, -1); // 00시 30분에 돌기 때문에 하루를 뺸다.
			
			calT.setTime(formatter2.parse(formatter1.format(calT.getTime()) + "235959")); // 시간 대입(어제 마지막 시간 대입)
			
			todaydate = formatter1.format(calT.getTime());

			todaytodate = formatter2.format(calT.getTime()); // 어제 23시59분59초
			
			calF.add(Calendar.DATE, -1); // 그제 23시00분부터 23시59분 값을 구해야하기 때문에 하루를 뺀다.
			calT.add(Calendar.DATE, -1); // 그제 23시00분부터 23시59분 값을 구해야하기 때문에 하루를 뺀다.
			
			todate = formatter2.format(calT.getTime()); // 그제 23시59분59초

			calF.setTime(formatter2.parse(formatter1.format(calF.getTime()) + "000000")); // 시간 대입(어제 시작 시간)

			fromdate = formatter2.format(calF.getTime()); // 그제 00시00분00초

			calF.setTime(formatter2.parse(formatter1.format(calF.getTime()) + "235500")); // 시간 대입(어제의 마지막 5분까지 포함해서 계산하기 떄문에 그에 맞는 시간대입)
			
			fivefromdate = formatter2.format(calF.getTime()); // 그제 23시55분00초

			for (int i = 0; i < Sensorlist.length; i++) {
				parsingdata = new HashMap<String, Object>(); // 맵 초기화
				data = new ArrayList<Map<String, Object>>(); // 맵 초기화
				List<String> sensorlist = new ArrayList<String>(); // 리스트 생성 및 초기화
				String daliytable = "bems_stat_daily_" + namelist[i]; // 일별 테이블이름 생성

				parsingdata.put("sensor", Sensorlist[i]); // 센서테이블 저장
				parsingdata.put("complexcode", complexcode); // 태백산은 2002
				parsingdata.put("dbname", DBname()); // DB이름 저장
				sensorName = this.edgeDBMapper.getAllSensorName(parsingdata); // 센서테이블에서 해당센서 가져오기

				if (!sensorName.isEmpty()) { // 가져온 센서의 갯수가 0이 아닐때

					for (int j = 0; j < sensorName.size(); j++) { // 센서갯수만큼 반복
						sensorlist.add(sensorName.get(j).get("sensor_sn").toString()); // 리스트에 센서이름 저장
					}
					for (int k = 0; k < sensorlist.size(); k++) { // 센서 갯수 만큼 반복
						hourdata = new HashMap<String, Object>(); // 맵 초기화
						parsingdata = new HashMap<String, Object>(); // 맵 초기화

						parsingdata.put("fromdate", fromdate);
						parsingdata.put("fivefromdate", fivefromdate);
						parsingdata.put("todate", todate);
						parsingdata.put("todaytodate", todaytodate);
						parsingdata.put("sensor", Sensorlist[i]);
						parsingdata.put("meter", DBlist[i]);
						parsingdata.put("sensor_sn", sensorlist.get(k));
						parsingdata.put("dbname", DBname()); // DB이름 저장

						data = this.edgeDBMapper.edgeDayData(parsingdata); // 그제 23시의 사용량과 어제 전체 사용량

						if (!data.isEmpty()) { // 가져온 사용량의 갯수가 0이 아니면
							for (int j = 0; j < data.size(); j++) { // 사용량의 갯수만큼 반복
								String sensor = data.get(j).get("sensor_sn").toString(); // 사용량의 센서
								if (sensorlist.get(k).equals(sensor)) { // 센서리스트의 센서랑 가져온 센서가 같으면
									hourdata.put("val", data.get(j).get("val").toString());
									hourdata.put("val_23", data.get(j).get("val_23").toString());
									hourdata.put("sensor_sn", data.get(j).get("sensor_sn").toString());
									hourdata.put("val_date", data.get(j).get("val_date").toString());
									hourdata.put("total_val", data.get(j).get("total_val").toString());
								}
							}
							if (!hourdata.isEmpty()) { // 위에서 만든 hourdata맵의 크기가 0이 아니면
								hourdata.put("table", daliytable); // 위에서 만든 일별 테이블 이름 저장
								hourdata.put("dbname", DBname()); // DB이름 저장
								this.edgeDBMapper.dayDataUpdate(hourdata); // 일별 테이블에 업데이트
							}
						} else { // 가져온 데이터의 크기가 0이면
							hourdata.put("table", daliytable);
							hourdata.put("sensor_sn", sensorlist.get(k));
							hourdata.put("val_date", todaydate);
							hourdata.put("val", 0);
							hourdata.put("val_23", 0);
							hourdata.put("total_val", 0);
							hourdata.put("dbname", DBname()); // DB이름 저장
							this.edgeDBMapper.dayDataUpdate(hourdata); // 일별 테이블에 업데이트
						}
					}
				}
			}
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
	}

	/**
	 * EdgeDB 각 일별 테이블에 시간별 사용량 추가(1시간 10분단위)
	 *
	 * @param void
	 *
	 * @return void
	 */
	@Override
	public void Edge_Hour_Insert() throws Exception {
		List<Map<String, Object>> data = new ArrayList<Map<String, Object>>();
		List<Map<String, Object>> sensorName = new ArrayList<Map<String, Object>>();
		Map<String, Object> insertdata1 = new HashMap<String, Object>(); // 데이터를 정리하기 위한 맵
		Map<String, Object> insertdata2 = new HashMap<String, Object>(); // pk를 포함한 전체 컬럼 맵
		Map<String, Object> insertdata3 = new HashMap<String, Object>(); // insert를 하기위한 맵
		Map<String, Object> pkdata = new HashMap<String, Object>(); // pk를 제외한 컬럼 맵
		HashMap<String, Object> parsingdata = new HashMap<String, Object>(); // sql에 필요한 조건을 넘겨주는 맵
		String fromdate = null;
		String todate = null;
		String hour = null;
		String complexcode = "2002"; // 태백산 지역코드 2002(엣지는 지금 태백산에만 설치되어있다.)
		int hourdate = 0;

		try {

			// 각 sensor테이블의 이름으로 된 배열 정의
			String[] Sensorlist = { "bems_sensor_solar", "bems_sensor_electric", "bems_sensor_electric_elechot",
					"bems_sensor_electric_hotwater", "bems_sensor_electric_heating", "bems_sensor_electric_cold",
					"bems_sensor_electric_light", "bems_sensor_electric_vent", "bems_sensor_electric_elevator",
					"bems_sensor_electric_water", "bems_sensor_electric_equipment", "bems_sensor_electric_boiler" };
			// 각 row테이블의 이름으로 된 배열 정의
			String[] DBlist = { "bems_meter_solar", "bems_meter_electric", "bems_meter_electric_elechot",
					"bems_meter_electric_hotwater", "bems_meter_electric_heating", "bems_meter_electric_cold",
					"bems_meter_electric_light", "bems_meter_electric_vent", "bems_meter_electric_elevator",
					"bems_meter_electric_water", "bems_meter_electric_equipment", "bems_meter_electric_boiler" };
			// 각 에너지원별의 이름으로 된 배열 정의
			String[] namelist = { "solar", "electric", "electric_elechot", "electric_hotwater", "electric_heating",
					"electric_cold", "electric_light", "electric_vent", "electric_elevator", "electric_water",
					"electric_equipment", "electric_boiler" };

			SimpleDateFormat formatter1 = new SimpleDateFormat("yyyyMMdd"); // sql에 시간을 넘겨주기 위해서 포맷설정(연도,월,일)
			SimpleDateFormat formatter2 = new SimpleDateFormat("yyyyMMddHH"); // sql에 시간을 넘겨주기 위해서 포맷설정(연도,월,일,시)
			SimpleDateFormat formatter3 = new SimpleDateFormat("yyyyMMddHHmmss"); // sql에 시간을 넘겨주기 위해서 포맷설정(연도,월,일,시,분,초)
			SimpleDateFormat formathour = new SimpleDateFormat("HH"); // 오류시 데이터입력 시간을 맞추기 위한 포맷설정(시)

			Calendar calF = Calendar.getInstance(Locale.KOREA); // 오늘 날짜
			Calendar calT = Calendar.getInstance(Locale.KOREA); // 오늘 날짜
			
			hourdate = Integer.parseInt(formathour.format(calT.getTime()));  // 현재 시

			calF.setTime(formatter3.parse(formatter1.format(calF.getTime()) + "000000")); // 오늘 날짜 00시00분00초
			calT.setTime(formatter3.parse(formatter2.format(calT.getTime()) + "5959")); // 현재 시각 59분 59초

			fromdate = formatter3.format(calF.getTime()); // 오늘 날짜 00시 00분 00초
			todate = formatter3.format(calT.getTime()); // 현재 시각 59분 59초
			
			for (int i = 0; i < Sensorlist.length; i++) { // 위에서 선언한 센서리스트 배열만큼 반복
				parsingdata = new HashMap<String, Object>(); // 맵 초기화
				data = new ArrayList<Map<String, Object>>(); // 맵 초기화
				List<String> sensorlist = new ArrayList<String>(); // 리스트 생성 및 초기화
				String daliytable = "bems_stat_daily_" + namelist[i]; // 일별 테이블이름 생성

				parsingdata.put("sensor", Sensorlist[i]); // 센서 테이블 이름 저장
				parsingdata.put("complexcode", complexcode); // 태백산은 2002
				parsingdata.put("dbname", DBname()); // DB이름 저장
				sensorName = this.edgeDBMapper.getAllSensorName(parsingdata); // 각 테이블별 센서 가져오기

				if (!sensorName.isEmpty()) {

					for (int j = 0; j < sensorName.size(); j++) { // 가져온 센서 갯수만큼 반복
						sensorlist.add(sensorName.get(j).get("sensor_sn").toString()); // 센서 이름 리스트에 저장
					}

					for (int k = 0; k < sensorlist.size(); k++) {
						insertdata1 = new HashMap<String, Object>(); // 맵 초기화
						insertdata2 = new HashMap<String, Object>(); // 맵 초기화
						insertdata3 = new HashMap<String, Object>(); // 맵 초기화
						parsingdata = new HashMap<String, Object>(); // 맵 초기화

						parsingdata.put("fromdate", fromdate); 
						parsingdata.put("todate", todate);
						parsingdata.put("sensor", Sensorlist[i]);
						parsingdata.put("meter", DBlist[i]);
						parsingdata.put("sensor_sn", sensorlist.get(k));
						parsingdata.put("dbname", DBname()); // DB이름 저장

						data = this.edgeDBMapper.edgeHourData(parsingdata); // 시간별 사용량 데이터 가져오기
						
						insertdata1.put("sensor_sn", sensorlist.get(k));
						insertdata1.put("val_date", formatter1.format(calF.getTime()));
						for (int p = 0; p < hourdate+1; p++) {
							insertdata1.put("val_" + Integer.toString(p), 0);
						}
						
						if (!data.isEmpty() && data != null) {
							for (int j = 0; j < data.size(); j++) {
								hour = data.get(j).get("hour").toString(); // 해당 데이터 시간 업데이트
								/*
								 * 시간을 int형으로 변환(변환하는 이유는 val_0으로 insert가 되야하는데, val_00으로 insert되는 현상을 해결하기 위해서)
								 */
								int hourtime = Integer.parseInt(hour);
								/* hourdata 즉, 시간별 데이터 생성 */
								insertdata1.put("val_" + hourtime, data.get(j).get("val").toString());
								insertdata1.put("val_date", data.get(j).get("day").toString());
								insertdata1.put("sensor_sn", data.get(j).get("sensor_sn").toString());
							}

							if (!insertdata1.isEmpty()) {
								/*
								 * 위에서 만든 맵을 key와 value를 각각 리스트로 만든다. 그리고 각각의 리스트를 맵에 저장한다.
								 */
								List<String> keyList = new ArrayList<>(insertdata1.keySet());
								List<Object> valueList = new ArrayList<>(insertdata1.values());
								List<Map<String, Object>> result1 = new ArrayList<Map<String, Object>>();
								List<Map<String, Object>> result2 = new ArrayList<Map<String, Object>>();

								for (int p = 0; p < keyList.size(); p++) {
									insertdata2 = new HashMap<String, Object>();
									pkdata = new HashMap<String, Object>();

									insertdata2.put("name", keyList.get(p));
									insertdata2.put("val", valueList.get(p));

									if (!(keyList.get(p).equals("sensor_sn") || keyList.get(p).equals("val_date")
											|| keyList.get(p).equals("reg_date"))) {
										pkdata.put("name", keyList.get(p));
										pkdata.put("val", valueList.get(p));
										result2.add(pkdata);
									}

									result1.add(insertdata2);
								}

								insertdata3.put("result1", result1);
								insertdata3.put("result2", result2);
								insertdata3.put("table", daliytable);
								insertdata3.put("dbname", DBname());

								this.edgeDBMapper.hourDataInsert(insertdata3); // 시간별 데이터 저장
							}
						} else { // data가 없을시, 0으로 insert
							if (!insertdata1.isEmpty()) {
								/*
								 * 위에서 만든 맵을 key와 value를 각각 리스트로 만든다. 그리고 각각의 리스트를 맵에 저장한다.
								 */
								List<String> keyList = new ArrayList<>(insertdata1.keySet());
								List<Object> valueList = new ArrayList<>(insertdata1.values());
								List<Map<String, Object>> result1 = new ArrayList<Map<String, Object>>();
								List<Map<String, Object>> result2 = new ArrayList<Map<String, Object>>();

								for (int p = 0; p < keyList.size(); p++) {
									insertdata2 = new HashMap<String, Object>();
									pkdata = new HashMap<String, Object>();

									insertdata2.put("name", keyList.get(p));
									insertdata2.put("val", valueList.get(p));

									if (!(keyList.get(p).equals("sensor_sn") || keyList.get(p).equals("val_date")
											|| keyList.get(p).equals("reg_date"))) {
										pkdata.put("name", keyList.get(p));
										pkdata.put("val", valueList.get(p));
										result2.add(pkdata);
									}

									result1.add(insertdata2);
								}

								insertdata3.put("result1", result1);
								insertdata3.put("result2", result2);
								insertdata3.put("table", daliytable);
								insertdata3.put("dbname", DBname());

								this.edgeDBMapper.hourDataInsert(insertdata3); // 시간별 데이터 저장
							}
						}
					}
				}
			}
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
	}
	
	/**
	 * EdgeDB meter(로우)테이블에 사용량 저장
	 *
	 * @param void
	 *
	 * @return void
	 */
	@Override
	public void Edge_meter_insert() throws Exception {
		Map<String, Object> insertdata1 = new HashMap<String, Object>(); // 데이터를 정리하기 위한 맵
		Map<String, Object> insertdata2 = new HashMap<String, Object>(); // pk를 포함한 전체 컬럼 맵
		Map<String, Object> insertdata3 = new HashMap<String, Object>(); // insert를 하기위한 맵
		Map<String, Object> pkdata = new HashMap<String, Object>(); // pk를 제외한 컬럼 맵
		Map<String, Object> c_data = new HashMap<String, Object>(); // 역률컬럼이 있는지 확인한 결과를 담은 맵
		Map<String, Object> data = new HashMap<String, Object>(); // 각 테이블별 센서를 저장한 리스트 맵
		Map<String, Object> ntekdata = new HashMap<String, Object>(); // 각 테이블별 센서를 저장한 리스트 맵
		List<Map<String, Object>> sensorlist = new ArrayList<Map<String, Object>>(); // 각 테이블별 센서를 저장한 리스트 맵
		HashMap<String, Object> parsingdata = new HashMap<String, Object>(); // sql에 필요한 조건을 넘겨주는 맵
		HashMap<String, Object> checkdata = new HashMap<String, Object>(); // 역률컬럼이 있는지 확인하기 위한 맵

		try {

			// 각 sensor테이블의 이름으로 된 배열 정의
			String[] Sensorlist = { "bems_sensor_electric", "bems_sensor_electric_hotwater",
					"bems_sensor_electric_heating", "bems_sensor_electric_cold", "bems_sensor_electric_light",
					"bems_sensor_electric_vent", "bems_sensor_electric_elevator", "bems_sensor_electric_water",
					"bems_sensor_electric_equipment", "bems_sensor_electric_boiler", "bems_sensor_gas",
					"bems_sensor_heating", "bems_sensor_hotwater", "bems_sensor_water" };
			// 각 row테이블의 이름으로 된 배열 정의
			String[] DBlist = { "bems_meter_electric", "bems_meter_electric_hotwater", "bems_meter_electric_heating",
					"bems_meter_electric_cold", "bems_meter_electric_light", "bems_meter_electric_vent",
					"bems_meter_electric_elevator", "bems_meter_electric_water", "bems_meter_electric_equipment",
					"bems_meter_electric_boiler", "bems_meter_gas", "bems_meter_heating", "bems_meter_hotwater",
					"bems_meter_water" };

			SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss"); // sql에 시간을 넘겨주기 위해서 포맷설정(연도,월,일,시,분,초)
			Calendar calF = Calendar.getInstance(Locale.KOREA); // 오늘 날짜
			String date = formatter.format(calF.getTime()); // 현재 시각(데이터가 0일경우 오류시간을 표시하기위해서)
			String todate = formatter.format(calF.getTime()); // 현재 시각
			String complexcode = "2002"; // 태백산 지역코드

			for (int i = 0; i < Sensorlist.length; i++) {
				c_data = new HashMap<String, Object>(); // 맵 초기화
				checkdata = new HashMap<String, Object>(); // 맵 초기화
				parsingdata = new HashMap<String, Object>(); // 맵 초기화
				sensorlist = new ArrayList<Map<String, Object>>(); // 맵 초기화

				int flag = 0; // 각 테이블별 역률컬럼(pf)가 있는지 확인하는 플래그

				parsingdata.put("complexcode", complexcode);
				parsingdata.put("sensor", Sensorlist[i]);
				parsingdata.put("dbname", DBname()); // DB이름 저장
				sensorlist = this.edgeDBMapper.edgeSensorName(parsingdata);
				
				if (!sensorlist.isEmpty()) {
					checkdata.put("tablename", DBlist[i]); // table이름(즉, insert하는 meter테이블)
					checkdata.put("columnname", "pf"); // 역률컬럼을 확인
					checkdata.put("dbname", DBname()); // DB이름 저장

					c_data = this.edgeDBMapper.edgeCheckdata(checkdata); // 역률컬럼이 있는지 확인 결과를 맵에 저장

					if (c_data != null) {
						flag = Integer.parseInt(c_data.get("flag").toString()); // 확인결과를 변수에 저장
					}

					for (int j = 0; j < sensorlist.size(); j++) {
						parsingdata = new HashMap<String, Object>(); // 맵 초기화
						data = new HashMap<String, Object>(); // 맵 초기화
						ntekdata = new HashMap<String, Object>(); // 맵 초기화

						parsingdata.put("sensor", Sensorlist[i]); // 센서테이블 저장
						parsingdata.put("complexcode", complexcode); // 태백산은 2002
						parsingdata.put("todate", todate);
						parsingdata.put("dbname", DBname()); // DB이름 저장
						parsingdata.put("sensor_sn", sensorlist.get(j).get("sensor_sn").toString());
						
						data = this.edgeDBMapper.ntekMeterSelect(parsingdata); // ntek에서 로우데이터를 가져온다.

						/*
						 * 5분안에 데이터가 있을경우에는 정상적인 통신이 된것이므로 오류코드는 0이나, 5분안에 데이터가 없을경우 오류가 발생한것이므로, 센서별 가장
						 * 최근값으로 가져온후 에러코드에 1를 추가한다.
						 */
						if (data != null && !data.isEmpty()) {
							SimpleDateFormat valdateformat = new SimpleDateFormat("yyyyMMddHHmmss"); // sql에 시간을 넘겨주기 위해서 포맷설정(연도,월,일,시,분,초)
							Calendar valC = Calendar.getInstance(Locale.KOREA); // 오늘 날짜
							String valdateS = data.get("val_date").toString();
							Date valdateD = valdateformat.parse(valdateS);
							Date today = valC.getTime();
							
							long differenceTime = today.getTime() - valdateD.getTime();
							long thirtytime = 1800000;
							
							/* 30분안에 데이터가 없을 경우에는 오류가 발생한거이므로 에러코드에 1(에러)을 삽입한다 */
							if(differenceTime >= thirtytime) {
								ntekdata.putAll(data);
								ntekdata.put("val_date", date);
								ntekdata.put("error_code", 1);
							} else {
								ntekdata.putAll(data);
								ntekdata.put("error_code", 0);
							}
						}

						/* 반복문마다 데이터가 겹치지 않기 위해서 맵 초기화를 한다. */
						insertdata1 = new HashMap<String, Object>(); // 맵 초기화
						insertdata2 = new HashMap<String, Object>(); // 맵 초기화
						insertdata3 = new HashMap<String, Object>(); // 맵 초기화

						/* ntek에서 가져온 데이터를 meter테이블에 맞게 insert 하기위해 가공 */
						String sensor = ntekdata.get("sensor_sn").toString();
						String val_date = ntekdata.get("val_date").toString();
						int current_w = Integer.parseInt(ntekdata.get("current_w").toString().replace(".", "").replace(",", ""));
						int total_wh = Integer.parseInt(ntekdata.get("total_wh").toString().replace(".", "").replace(",", ""));
						double pf = Double.parseDouble(ntekdata.get("pf").toString());
						String error_code = ntekdata.get("error_code").toString();

						/* 가져온 데이터를 meter테이블 컬럼을 키로 저장 */
						insertdata1.put("sensor_sn", sensor);
						insertdata1.put("current_w", current_w);
						insertdata1.put("total_wh", total_wh);
						insertdata1.put("val_date", val_date);
						insertdata1.put("error_code", error_code);
						if (flag > 0) {
							insertdata1.put("pf", pf);
						}

						/*
						 * 위에서 만든 맵을 key와 value를 각각 리스트로 만든다. 그리고 각각의 리스트를 맵에 저장한다.
						 */
						List<String> keyList = new ArrayList<>(insertdata1.keySet());
						List<Object> valueList = new ArrayList<>(insertdata1.values());
						List<Map<String, Object>> result1 = new ArrayList<Map<String, Object>>();
						List<Map<String, Object>> result2 = new ArrayList<Map<String, Object>>();

						for (int p = 0; p < keyList.size(); p++) {
							insertdata2 = new HashMap<String, Object>();
							pkdata = new HashMap<String, Object>();

							insertdata2.put("name", keyList.get(p));
							insertdata2.put("val", valueList.get(p));

							if (!(keyList.get(p).equals("sensor_sn") || keyList.get(p).equals("val_date")
									|| keyList.get(p).equals("reg_date"))) {
								pkdata.put("name", keyList.get(p));
								pkdata.put("val", valueList.get(p));
								result2.add(pkdata);
							}

							result1.add(insertdata2);
						}

						insertdata3.put("result1", result1);
						insertdata3.put("result2", result2);
						insertdata3.put("meter", DBlist[i]);
						insertdata3.put("dbname", DBname());

						this.edgeDBMapper.edgeMetertInsert(insertdata3);
					}
				}
			}
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
	}
	
	/**
	 * EdgeDB electric_meter(로우)테이블에 사용할 층별 및 전체 사용량을 계산해서 저장한다.
	 *
	 * @param void
	 *
	 * @return void
	 */
	@Override
	public void Edge_electric_insert() throws Exception {
		Map<String, Object> renewabledata = new HashMap<String, Object>(); // 신재생 에너지를 저장한 맵
		Map<String, Object> insertdata1 = new HashMap<String, Object>(); // 데이터를 정리하기 위한 맵
		Map<String, Object> insertdata2 = new HashMap<String, Object>(); // pk를 포함한 전체 컬럼 맵
		Map<String, Object> insertdata3 = new HashMap<String, Object>(); // insert를 하기위한 맵
		Map<String, Object> pkdata = new HashMap<String, Object>(); // pk를 제외한 컬럼 맵
		Map<String, Object> electricdata = new HashMap<String, Object>(); // 정리한 센서테이블에 해당하는 사용량을 담은 맵
		Map<String, Object> data = new HashMap<String, Object>(); // meter테이블에서 가져온 사용량
		Map<String, Object> ntekdata = new HashMap<String, Object>(); // meter테이블에서 가져온 사용량
		List<Map<String, Object>> floorname = new ArrayList<Map<String, Object>>(); // 총 몇층인지 저장한 맵
		List<Map<String, Object>> sensorname = new ArrayList<Map<String, Object>>(); // 추출한 센서목록을 저장하는 맵
		HashMap<String, Object> parsingdata = new HashMap<String, Object>(); // sql에 필요한 조건을 넘겨주는 맵

		try {
			
			// electric테이블만 사용한다
			String[] Sensorlist = {"bems_sensor_electric"};
			String[] DBlist = {"bems_meter_electric"};
			int renewable = 0; // 신재생 에너지 사용량
			int oneFdata_all = 0; // 1층 전체 사용량
			int oneFdata_all_e = 0; // 1층 전체 사용량 에러체크
			int twoFdata_all = 0; // 2층 전체 사용량
			int twoFdata_all_e = 0; // 2층 전체 사용량 에러체크
			int twoFdata = 0; // 2층 사용량
			int twoFdata_e = 0; // 2층 사용량 에러체크
			int threeFdata_all = 0; // 3층 전체 사용량
			int threeFdata_all_e = 0; // 3층 전체 사용량 에러체크
			int threeFdata = 0; // 3층 사용량
			int threeFdata_e = 0; // 3층 사용량 에러체크
			int total = 0; // 총 전체 사용량
			int error = 0; // 에러체크
			String r_sensor = "2002_1_3"; // 신재생에너지 센서(고정)
			String complexcode = "2002"; // 태백산 지역코드 2002
			List<String> floorlist = new ArrayList<String>();
			List<String> sensorlist = new ArrayList<String>();
			List<String> sensorall = new ArrayList<String>();
			
			SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss"); // sql에 시간을 넘겨주기 위해서 포맷설정(연도,월,일,시,분,초)
			Calendar calF = Calendar.getInstance(Locale.KOREA); // 오늘 날짜
			String date = formatter.format(calF.getTime()); // 오늘날짜를 formatter형식으로 전환해서 저장
			calF.add(Calendar.MINUTE, -2);
			String todate = formatter.format(calF.getTime()); // 오늘날짜를 formatter형식으로 전환해서 저장
			
			/* 신재생 에너지 사용량 */
			parsingdata.put("sensor_sn", r_sensor);
			parsingdata.put("dbname", DBname()); // DB이름 저장
			renewabledata = this.edgeDBMapper.renewableSelect(parsingdata);
			
			if(renewabledata != null && !renewabledata.isEmpty()) { // 신재생 에너지 사용량이 존재할시
				renewable = Integer.parseInt(renewabledata.get("total_wh").toString().replace(".", "").replace(",", "")); // 신재생 에너지를 변수에 저장
			}
			
			parsingdata = new HashMap<String, Object>(); // 맵 초기화
			
			parsingdata.put("complexcode", complexcode); // 태백산 지역코드 2002 저장
			parsingdata.put("dbname", DBname()); // DB이름 저장
			floorname = this.edgeDBMapper.getFloorname(parsingdata); // floorname 층 층수 가져오기
			
			if(floorname != null & !floorname.isEmpty()) {
				for (int j = 0; j < floorname.size(); j++) {
					floorlist.add(floorname.get(j).get("home_grp_pk").toString()); // 리스트에 층이름을 저장
				}
			}
			
			parsingdata = new HashMap<String, Object>(); // 맵 초기화
			
			/* 총 몇층이 있는지 확인 및 층별 전체전력 및 전체 전체전력 센서이름을 가져와서 배열 생성 */
			for (int i = 0; i < Sensorlist.length; i++) {
				parsingdata.put("sensor", Sensorlist[i]);
				parsingdata.put("complexcode", complexcode);
				parsingdata.put("dbname", DBname()); // DB이름 저장

				sensorname = this.edgeDBMapper.getAllSensorName(parsingdata);

				/* 미가공 센서와 가공센서 둘다 불러와서 분류하여 각각의 리스트에 저장한다. */
				if (sensorname != null & !sensorname.isEmpty()) {
					for (int j = 0; j < sensorname.size(); j++) {
						String home = sensorname.get(j).get("home_grp_pk").toString();
						if(home.equals("0M") || home.equals("ALL")) {
							sensorall.add(sensorname.get(j).get("sensor_sn").toString());
						} else {
							sensorlist.add(sensorname.get(j).get("sensor_sn").toString());
						}
					}
				}
			}
			
			for (int i = 0; i < Sensorlist.length; i++) {
				electricdata = new HashMap<String, Object>();
				
				for (int k = 0; k < floorlist.size(); k++) { // 층수만큼 반복
					for (int p = 0; p < sensorlist.size(); p++) {
						data = new HashMap<String, Object>();
						ntekdata = new HashMap<String, Object>();
						parsingdata = new HashMap<String, Object>();
						
						parsingdata.put("sensor", Sensorlist[i]);
						parsingdata.put("sensor_sn", sensorlist.get(p));
						parsingdata.put("complexcode", complexcode); // 태백산은 2002
						parsingdata.put("home_grp_pk", floorlist.get(k)); // home_grp_pk 조건 입력(이유: 계산식이 들어가서 만들어지는 meter가 있어서)
						parsingdata.put("todate", todate);
						parsingdata.put("dbname", DBname()); // DB이름 저장
						data = this.edgeDBMapper.ntekMeterSelect(parsingdata);

						if (data != null && !data.isEmpty()) {
							SimpleDateFormat valdateformat = new SimpleDateFormat("yyyyMMddHHmmss"); // sql에 시간을 넘겨주기 위해서 포맷설정(연도,월,일,시,분,초)
							Calendar valC = Calendar.getInstance(Locale.KOREA); // 오늘 날짜
							String valdateS = data.get("val_date").toString();
							Date valdateD = valdateformat.parse(valdateS);
							Date today = valC.getTime();
							
							long differenceTime = today.getTime() - valdateD.getTime();
							long thirtytime = 1800000;
							
							/* 30분안에 데이터가 없을 경우에는 오류가 발생한거이므로 에러코드에 1(에러)을 삽입한다 */
							if(differenceTime >= thirtytime) {
								ntekdata.putAll(data);
								ntekdata.put("error_code", 1);
							} else {
								ntekdata.putAll(data);
								ntekdata.put("error_code", 0);
							}
						}

						if (ntekdata != null && !ntekdata.isEmpty()) { // 가져온 데이터가 있을경우
							String sensor = ntekdata.get("sensor_sn").toString(); // 센서 저장
							int total_wh = Integer.parseInt(ntekdata.get("total_wh").toString().replace(".", "").replace(",", "")); // 전력 사용량 저장
							int error_code = Integer.parseInt(ntekdata.get("error_code").toString()); // 가져온 에러를 변수에 저장

							/* 각 계산식에 따른 층별 및 전체 전력사용량을 구하고 계산한다. */
							if (sensor.equals("2002_1_1")) {
								oneFdata_all += total_wh;
								oneFdata_all_e += error_code;
							}
							if (floorlist.get(k).equals("2F")) {
								twoFdata_all += total_wh;
								twoFdata_all_e += error_code;
								if (!sensor.equals("2002_4_1")) {
									twoFdata += total_wh;
									twoFdata_e += error_code;;
								}
							}
							if (floorlist.get(k).equals("3F")) {
								threeFdata_all += total_wh;
								threeFdata_all_e += error_code;
								if (sensor.equals("2002_6_1")) {
									threeFdata += total_wh;
									threeFdata_e += error_code;
								}
							}
						}
					}
				}
				
				/* 위에서 구한 전력사용량이 있는지 확인하기 위해서 맵에 저장한다. */
				electricdata.put("oneFdata_all", oneFdata_all);
				electricdata.put("oneFdata_all_e", oneFdata_all_e);
				electricdata.put("twoFdata_all", twoFdata_all);
				electricdata.put("twoFdata_all_e", twoFdata_all_e);
				electricdata.put("threeFdata_all", threeFdata_all);
				electricdata.put("threeFdata_all_e", threeFdata_all_e);
				electricdata.put("twoFdata", twoFdata);
				electricdata.put("twoFdata_e", twoFdata_e);
				electricdata.put("threeFdata", threeFdata);
				electricdata.put("threeFdata_e", threeFdata_e);
				
				for (int j = 0; j < sensorall.size(); j++) {
					if (!electricdata.isEmpty()) { // 위에서 구한 전력사용량이 존재할시
						insertdata1 = new HashMap<String, Object>(); // 맵 초기화
						insertdata2 = new HashMap<String, Object>(); // 맵 초기화
						insertdata3 = new HashMap<String, Object>(); // 맵 초기화
						total = 0; // 계산한 값을 저장하기 위한 변수 선언

						/* 각 층별 및 전체 사용량 센서별 계산한 값을 저장한다. */
						if (sensorall.get(j).equals("2002_1F")) {
							total = (oneFdata_all + renewable + 520000) - (twoFdata + threeFdata);
							error = oneFdata_all_e + twoFdata_e + threeFdata_e;
						} else if (sensorall.get(j).equals("2002_2F")) {
							total = twoFdata_all;
							error = twoFdata_all_e;
						} else if (sensorall.get(j).equals("2002_3F")) {
							total = threeFdata_all;
							error = threeFdata_all_e;
						} else if (sensorall.get(j).equals("2002_1_2_3_F")) {
							total = (oneFdata_all + renewable + 520000) - (twoFdata + threeFdata) + twoFdata_all
									+ threeFdata_all;
							error = oneFdata_all_e + twoFdata_e + threeFdata_e + twoFdata_all_e + threeFdata_all_e;
						} else if (sensorall.get(j).equals("2002_ALL")) {
							total = (oneFdata_all + renewable + 520000) - (twoFdata + threeFdata) + twoFdata_all
									+ threeFdata_all - (renewable + 520000);
							error = oneFdata_all_e + twoFdata_e + threeFdata_e + twoFdata_all_e + threeFdata_all_e;
						}

						insertdata1.put("sensor_sn", sensorall.get(j)); // 위에서 리스트에 저장한 가공센서를 불러와서 맵에 저장
						insertdata1.put("val_date", date); // 현재 시각 저장
						insertdata1.put("total_wh", total); // 위에서 계산한 전력사용량 저장
						if (error > 0) {
							insertdata1.put("error_code", 1);
						} else {
							insertdata1.put("error_code", 0);
						}

						/* 가공한 데이터를 기반으로 리스트를 만들어서 meter테이블에 층별 및 전체 전력사용을 적재한다. */
						List<String> keyList = new ArrayList<>(insertdata1.keySet());
						List<Object> valueList = new ArrayList<>(insertdata1.values());
						List<Map<String, Object>> result1 = new ArrayList<Map<String, Object>>();
						List<Map<String, Object>> result2 = new ArrayList<Map<String, Object>>();

						for (int p = 0; p < keyList.size(); p++) {
							insertdata2 = new HashMap<String, Object>();
							pkdata = new HashMap<String, Object>();

							insertdata2.put("name", keyList.get(p));
							insertdata2.put("val", valueList.get(p));

							if (!(keyList.get(p).equals("sensor_sn") || keyList.get(p).equals("val_date")
									|| keyList.get(p).equals("reg_date"))) {
								pkdata.put("name", keyList.get(p));
								pkdata.put("val", valueList.get(p));
								result2.add(pkdata);
							}

							result1.add(insertdata2);
						}

						insertdata3.put("result1", result1);
						insertdata3.put("result2", result2);
						insertdata3.put("meter", DBlist[i]);
						insertdata3.put("dbname", DBname()); // DB이름 저장

						this.edgeDBMapper.edgeMetertInsert(insertdata3);
						
					} else { // 아예 데이터가 존재하지 않을시
						insertdata1.put("sensor_sn", sensorall.get(j)); // 위에서 리스트에 저장한 가공센서를 불러와서 맵에 저장
						insertdata1.put("val_date", date); // 현재 시각 저장
						insertdata1.put("total_wh", 0); // 데이터가 0이므로 0 저장
						insertdata1.put("error_code", 1); // 오류가 발생한거이므로 오류코드를 1로 저장

						/* 가공한 데이터를 기반으로 리스트를 만들어서 meter테이블에 층별 및 전체 전력사용을 적재한다. */
						List<String> keyList = new ArrayList<>(insertdata1.keySet());
						List<Object> valueList = new ArrayList<>(insertdata1.values());
						List<Map<String, Object>> result1 = new ArrayList<Map<String, Object>>();
						List<Map<String, Object>> result2 = new ArrayList<Map<String, Object>>();

						for (int p = 0; p < keyList.size(); p++) {
							insertdata2 = new HashMap<String, Object>();
							pkdata = new HashMap<String, Object>();

							insertdata2.put("name", keyList.get(p));
							insertdata2.put("val", valueList.get(p));

							if (!(keyList.get(p).equals("sensor_sn") || keyList.get(p).equals("val_date")
									|| keyList.get(p).equals("reg_date"))) {
								pkdata.put("name", keyList.get(p));
								pkdata.put("val", valueList.get(p));
								result2.add(pkdata);
							}

							result1.add(insertdata2);
						}

						insertdata3.put("result1", result1);
						insertdata3.put("result2", result2);
						insertdata3.put("meter", DBlist[i]);
						insertdata3.put("dbname", DBname()); // DB이름 저장

						this.edgeDBMapper.edgeMetertInsert(insertdata3);
					}
				}
			}
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
	}
	
	/**
	 * EdgeDB electric_meter(로우)테이블에 사용할 전열 전력 사용량을 계산해서 저장한다.
	 *
	 * @param void
	 *
	 * @return void
	 */
	@Override
	public void Edge_elechot_insert() throws Exception {
		Map<String, Object> renewabledata = new HashMap<String, Object>(); // 신재생 에너지 사용량을 저장하는 맵
		Map<String, Object> electricdata = new HashMap<String, Object>(); // 각 테이블별 가져온 전력사용량을 저장하는 맵
		Map<String, Object> insertdata1 = new HashMap<String, Object>(); // 데이터를 정리하기 위한 맵
		Map<String, Object> insertdata2 = new HashMap<String, Object>(); // pk를 포함한 전체 컬럼 맵
		Map<String, Object> insertdata3 = new HashMap<String, Object>(); // insert를 하기위한 맵
		Map<String, Object> sensordata = new HashMap<String, Object>(); // 필요한 센서목록을 정의하기 위한 맵
		Map<String, Object> errordata = new HashMap<String, Object>(); // 가져온 센서들의 에러를 저장하기 위한 맵
		Map<String, Object> pkdata = new HashMap<String, Object>();
		Map<String, Object> data = new HashMap<String, Object>();
		Map<String, Object> ntekdata = new HashMap<String, Object>();
		List<Map<String, Object>> sensorlist = new ArrayList<Map<String, Object>>();
		List<Map<String, Object>> elechotlist = new ArrayList<Map<String, Object>>();
		HashMap<String, Object> parsingdata = new HashMap<String, Object>(); // sql에 필요한 조건을 넘겨주는 맵

		try {

			// 각 sensor테이블의 이름으로 된 배열 정의
			String[] Sensorlist = { "bems_sensor_electric", "bems_sensor_electric_hotwater", "bems_sensor_electric_heating",
					"bems_sensor_electric_cold", "bems_sensor_electric_light", "bems_sensor_electric_vent",
					"bems_sensor_electric_elevator", "bems_sensor_electric_water", "bems_sensor_electric_equipment",
					"bems_sensor_electric_boiler"};
			String r_sensor = "2002_1_3"; // 신재생 에너지 센서
			String complexcode = "2002"; // 태백산 지역코드 2002
			int renewable = 0;			  // 신재생 에너지 사용량
			
			/* 전열센서테이블에서 센서목록을 추출해서 리스트맵에 저장 */
			parsingdata.put("sensor", "bems_sensor_electric_elechot");
			parsingdata.put("complexcode", complexcode);
			parsingdata.put("dbname", DBname()); // DB이름 저장

			elechotlist = this.edgeDBMapper.edgeSensorName(parsingdata);
			
			/* 전열테이블 계산을 위한 위치별 센서를 map 저장한다. */
			sensordata.put("1층 전체 전력1", "2002_1_1");
			sensordata.put("2층 전체 전력1", "2002_3_1");
			sensordata.put("2층 전체 전력3", "2002_5_1");
			sensordata.put("1층 홀 냉난방1", "2002_1_2");
			sensordata.put("1층 홀 냉난방2", "2002_1_4");
			sensordata.put("1층 홀 냉난방3", "2002_1_11");
			sensordata.put("1층 홀 운송1", "2002_1_5");
			sensordata.put("1층 홀 전등1", "2002_1_6");
			sensordata.put("1층 홀 전등2", "2002_1_7");
			sensordata.put("1층 홀 전등3", "2002_1_9");
			sensordata.put("1층 홀 전등4", "2002_1_10");
			sensordata.put("1층 홀 환기1", "2002_1_8");
			sensordata.put("1층 홀 동력 시설1", "2002_2_1");
			sensordata.put("1층 물탱크 동력 시설1", "2002_2_1");
			sensordata.put("1층 물탱크 급수 시설1", "2002_2_2");
			sensordata.put("1층 물탱크 급탕 시설1", "2002_2_3");
			sensordata.put("1층 물탱크 급탕 시설2", "2002_2_4");
			sensordata.put("1층 물탱크 배수 시설1", "2002_2_5");
			sensordata.put("1층 물탱크 급탕 에너지1", "2002_2_6");
			sensordata.put("2층 홀 전등1", "2002_3_4");
			sensordata.put("2층 홀 급탕 에너지1", "2002_3_2");
			sensordata.put("2층 홀 환기1", "2002_3_3");
			sensordata.put("2층 홀 환기2", "2002_3_5");
			sensordata.put("2층 전체 전력2", "2002_4_1");
			sensordata.put("2층 판매장 전등1", "2002_4_4");
			sensordata.put("2층 판매장 급탕1", "2002_4_2");
			sensordata.put("2층 판매장 환기1", "2002_4_3");
			sensordata.put("2층 판매장 환기2", "2002_4_5");
			sensordata.put("2층 판매장 냉난방1", "2002_4_6");
			sensordata.put("2층 사무실 전등1", "2002_5_3");
			sensordata.put("2층 사무실 환기1", "2002_5_4");
			sensordata.put("3층 전체 전력1", "2002_6_1");
			sensordata.put("3층 홀 전등1", "2002_6_3");
			sensordata.put("3층 홀 환기1", "2002_6_2");
			sensordata.put("3층 홀 환기2", "2002_6_4");
			sensordata.put("3층 전체 전력2", "2002_7_1");
			sensordata.put("공관숙소1 전등1", "2002_7_5");
			sensordata.put("공관숙소1 급탕1", "2002_7_3");
			sensordata.put("공관숙소1 환기1", "2002_7_4");
			sensordata.put("공관숙소1 환기2", "2002_7_6");
			sensordata.put("공관숙소1 냉난방1", "2002_7_2");
			sensordata.put("3층 전체 전력3", "2002_8_1");
			sensordata.put("공관숙소2 전등1", "2002_8_4");
			sensordata.put("공관숙소2 급탕1", "2002_8_2");
			sensordata.put("공관숙소2 환기1", "2002_8_3");
			sensordata.put("공관숙소2 냉난방1", "2002_8_5");
			
			
					
			SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss"); // sql에 시간을 넘겨주기 위해서 포맷설정(연도,월,일,시,분,초) - valdate부분
			Calendar calF = Calendar.getInstance(Locale.KOREA); 				 // 오늘 날짜
			String date = formatter.format(calF.getTime());						 // 현재 시각
			calF.add(Calendar.MINUTE, -2);										 // 2분전 시각으로 구하는 이유는, 스케쥴러에서 2분뒤에 돌기때문에
			String todate = formatter.format(calF.getTime());					 // 2분전 시각
			calF.add(Calendar.MINUTE, -5);										 // 5분을 빼는 이유는, 스케쥴러의 도는 시각이 5분단위라서
			String fromdate = formatter.format(calF.getTime());					 // 7분전 시각
			
			parsingdata.put("sensor_sn", r_sensor);							// 신재생 에너지 센서 저장
			parsingdata.put("dbname", DBname()); 							// DB이름 저장
			renewabledata = this.edgeDBMapper.renewableSelect(parsingdata); // 신재생 에너지 사용량 추출
			
			/* 필요한 센서목록을 정리해서 저장한 map의 key,value를 각각 리스트로 만든다. */
			List<String> datakeyList = new ArrayList<>(sensordata.keySet());
			List<Object> datavalueList = new ArrayList<>(sensordata.values());
			
			if(renewabledata != null) { // 신재생 에너지 데이터가 있을경우
				renewable = Integer.parseInt(renewabledata.get("total_wh").toString().replace(".", "").replace(",", "")); // 신재생 에너지 데이터 추출 후 가공
			}

			for (int i = 0; i < Sensorlist.length; i++) { // 필요한 센서테이블의 갯수만큼 반복
				parsingdata = new HashMap<String, Object>(); // 맵 초기화
				
				parsingdata.put("sensor", Sensorlist[i]); 	 				// 센서테이블을 map 저장
				parsingdata.put("complexcode", complexcode); 				// 지역코드를 map 저장
				parsingdata.put("dbname", DBname()); 		 				// DB이름 저장
				sensorlist = this.edgeDBMapper.edgeSensorName(parsingdata); // 각 테이블마다 지역코드 2002에 해당하는 센서목록 추출
				
				if(sensorlist != null && !sensorlist.isEmpty()) { // 가져온 센서목록이 있을경우
					for (int k = 0; k < sensorlist.size(); k++) { // 가져온 센서목록의 갯수만큼 반복
						data = new HashMap<String, Object>();		 // 맵 초기화
						ntekdata = new HashMap<String, Object>();	 // 맵 초기화
						parsingdata = new HashMap<String, Object>(); // 맵 초기화

						parsingdata.put("sensor", Sensorlist[i]);									 // 센서테이블을 map 저장
						parsingdata.put("sensor_sn", sensorlist.get(k).get("sensor_sn").toString()); // 위에서 추출한 센서목록에서 센서 하나를 저장
						parsingdata.put("complexcode", complexcode); 								 // 태백산 지역코드인 2002
						parsingdata.put("fromdate", fromdate);										 // 7분전 시각
						parsingdata.put("todate", todate);											 // 2분전 시각
						parsingdata.put("dbname", DBname()); 										 // DB이름 저장
						data = this.edgeDBMapper.ntekMeterSelect(parsingdata);						 // map저장한 조건에 맞는 raw데이터(사용량) 추출

						/* 
						 * 가져온 데이터를 다른 map다시 담는 이유는, 데이터가 있을경우와 없을경우를 구분해서 데이터가 있을경우에는 그대로 다른 map에 담고, 데이터가
						 * 없을 경우에는, 7분이전 가장 최근값을 불러오고, 오류코드를 1로 수정해서 meter테이블에 적재하기 위해서다.
						 */
						if (data != null && !data.isEmpty()) {
							SimpleDateFormat valdateformat = new SimpleDateFormat("yyyyMMddHHmmss"); // sql에 시간을 넘겨주기 위해서 포맷설정(연도,월,일,시,분,초)
							Calendar valC = Calendar.getInstance(Locale.KOREA); // 오늘 날짜
							String valdateS = data.get("val_date").toString();
							Date valdateD = valdateformat.parse(valdateS);
							Date today = valC.getTime();
							
							long differenceTime = today.getTime() - valdateD.getTime();
							long thirtytime = 1800000;
							
							/* 30분안에 데이터가 없을 경우에는 오류가 발생한거이므로 에러코드에 1(에러)을 삽입한다 */
							if(differenceTime >= thirtytime) {
								ntekdata.putAll(data);
								ntekdata.put("error_code", 1);
							} else {
								ntekdata.putAll(data);
								ntekdata.put("error_code", 0);
							}
						}
						
						if (ntekdata != null && !ntekdata.isEmpty()) { // 가져온 데이터가 있을경우
							String sensor = ntekdata.get("sensor_sn").toString();													// 가져온 센서를 변수에 저장
							int total_wh = Integer.parseInt(ntekdata.get("total_wh").toString().replace(".", "").replace(",", "")); // 가져온 사용량을 가공하여 변수에 저장
							int error_code = Integer.parseInt(ntekdata.get("error_code").toString());								// 가져온 에러를 변수에 저장

							/* 위에서 정의한 필요한 센서목록과 일치하는 센서일 경우, 해당 센서를 센서목록 map에 담겨있는 위치를 key로, 사용량을 map에 저장 */
							if (datavalueList.contains(sensor)) { // 위에서 필요한 센서목록중에 있는 센서인경우
								for (int p = 0; p < datavalueList.size(); p++) { // 센서목록만큼 반복
									if (datavalueList.get(p).equals(sensor)) { // 센서목록 중에 데이터의 센서랑 일치하는 경우
										electricdata.put(datakeyList.get(p), total_wh); // 위치를 key로 사용량을 map저장
										errordata.put(datakeyList.get(p), error_code);	// 위치를 key로 에러를 map저장
									}
								}
							}
						}
					}
				}
			}

			/* 필요한 만든 map을 정리해서 key,value를 각각 리스트로 만든다. */
			List<String> eleckeyList = new ArrayList<>(electricdata.keySet());
			
			/* 각 위치별 데이터를 저장한 map이랑 필요한 센서목록을 비교, 체크해서 빈값이 있을시 오류 데이터 삽입*/
			for (int j = 0; j < datakeyList.size(); j++) {
				if(!eleckeyList.contains(datakeyList.get(j))) {
					electricdata.put(datakeyList.get(j), 0);
					errordata.put(datakeyList.get(j), 1);
				}
			}
			
			for (int j = 0; j < elechotlist.size(); j++) { // 전열센서목록만큼 반복
				String sensor = elechotlist.get(j).get("sensor_sn").toString(); // 전열센서
				/* 
				 * 사용량을 저장한 map의 크기가 0이 아니거나, 필요한 센서목록를 저장한 map의 크기랑 같지 않으면 오류로 처리한다(map의 크키가 0이면 데이터가 없다는 뜻이고, 
				 * 필요한 센서목록을 저장한 map과 크기가 가지 않다는 뜻은, 필요한 사용량데이터의 누락이 발생했다는 뜻이기 때문에 오류로 처리한다).
				 */
				if (!electricdata.isEmpty() && !errordata.isEmpty()) {
					
					insertdata1 = new HashMap<String, Object>(); // 맵 초기화
					insertdata2 = new HashMap<String, Object>(); // 맵 초기화
					insertdata3 = new HashMap<String, Object>(); // 맵 초기화
					int total = 0;								 // 계산한 사용량을 저장하기 위한 변수
					int error = 0;								 // 에러여부를 저장하기 위한 변수

					/* 전열테이블은 각 센서별(위치별)로 계산에 필요한 센서목록과 계산식이 정해져 있기 때문에, 각 센서에 맞게 계산식을 작성 */
					if (sensor.equals("2002_1_1")) {
						total = (Integer.parseInt(electricdata.get("1층 전체 전력1").toString()) + renewable)
								- (Integer.parseInt(electricdata.get("2층 전체 전력1").toString())
										+ Integer.parseInt(electricdata.get("2층 전체 전력3").toString())
										+ Integer.parseInt(electricdata.get("3층 전체 전력1").toString()))
								- (Integer.parseInt(electricdata.get("1층 홀 냉난방1").toString())
										+ Integer.parseInt(electricdata.get("1층 홀 냉난방2").toString())
										+ Integer.parseInt(electricdata.get("1층 홀 냉난방3").toString())
										+ Integer.parseInt(electricdata.get("1층 홀 운송1").toString())
										+ Integer.parseInt(electricdata.get("1층 홀 전등1").toString())
										+ Integer.parseInt(electricdata.get("1층 홀 전등2").toString())
										+ Integer.parseInt(electricdata.get("1층 홀 전등3").toString())
										+ Integer.parseInt(electricdata.get("1층 홀 전등4").toString())
										+ Integer.parseInt(electricdata.get("1층 홀 환기1").toString())
										+ Integer.parseInt(electricdata.get("1층 홀 동력 시설1").toString()))
								+ 520000;
						error = Integer.parseInt(errordata.get("1층 전체 전력1").toString()) + Integer.parseInt(errordata.get("2층 전체 전력1").toString())
								+ Integer.parseInt(errordata.get("2층 전체 전력3").toString()) + Integer.parseInt(errordata.get("3층 전체 전력1").toString())
								+ Integer.parseInt(errordata.get("1층 홀 냉난방1").toString()) + Integer.parseInt(errordata.get("1층 홀 냉난방2").toString())
								+ Integer.parseInt(errordata.get("1층 홀 냉난방3").toString()) + Integer.parseInt(errordata.get("1층 홀 운송1").toString())
								+ Integer.parseInt(errordata.get("1층 홀 전등1").toString()) + Integer.parseInt(errordata.get("1층 홀 전등2").toString())
								+ Integer.parseInt(errordata.get("1층 홀 전등3").toString()) + Integer.parseInt(errordata.get("1층 홀 전등4").toString())
								+ Integer.parseInt(errordata.get("1층 홀 환기1").toString()) + Integer.parseInt(errordata.get("1층 홀 동력 시설1").toString());
					} else if (sensor.equals("2002_1_2")) {
						total = Integer.parseInt(electricdata.get("1층 물탱크 동력 시설1").toString())
								- (Integer.parseInt(electricdata.get("1층 물탱크 급수 시설1").toString())
										+ Integer.parseInt(electricdata.get("1층 물탱크 급탕 시설1").toString())
										+ Integer.parseInt(electricdata.get("1층 물탱크 급탕 시설2").toString())
										+ Integer.parseInt(electricdata.get("1층 물탱크 배수 시설1").toString())
										+ Integer.parseInt(electricdata.get("1층 물탱크 급탕 에너지1").toString()));
						error = Integer.parseInt(errordata.get("1층 물탱크 동력 시설1").toString()) + Integer.parseInt(errordata.get("1층 물탱크 급수 시설1").toString())
								+ Integer.parseInt(errordata.get("1층 물탱크 급탕 시설1").toString()) + Integer.parseInt(errordata.get("1층 물탱크 급탕 시설2").toString())
								+ Integer.parseInt(errordata.get("1층 물탱크 배수 시설1").toString()) + Integer.parseInt(errordata.get("1층 물탱크 급탕 에너지1").toString());
					} else if (sensor.equals("2002_2_1")) {
						total = Integer.parseInt(electricdata.get("2층 전체 전력1").toString())
								- (Integer.parseInt(electricdata.get("2층 홀 전등1").toString())
										+ Integer.parseInt(electricdata.get("2층 홀 급탕 에너지1").toString())
										+ Integer.parseInt(electricdata.get("2층 홀 환기1").toString())
										+ Integer.parseInt(electricdata.get("2층 홀 환기2").toString()));
						error = Integer.parseInt(errordata.get("2층 전체 전력1").toString()) + Integer.parseInt(errordata.get("2층 홀 전등1").toString())
								+ Integer.parseInt(errordata.get("2층 홀 급탕 에너지1").toString()) + Integer.parseInt(errordata.get("2층 홀 환기1").toString())
								+ Integer.parseInt(errordata.get("2층 홀 환기2").toString());
					} else if (sensor.equals("2002_2_2")) {
						total = Integer.parseInt(electricdata.get("2층 전체 전력2").toString())
								- (Integer.parseInt(electricdata.get("2층 판매장 전등1").toString())
										+ Integer.parseInt(electricdata.get("2층 판매장 급탕1").toString())
										+ Integer.parseInt(electricdata.get("2층 판매장 환기1").toString())
										+ Integer.parseInt(electricdata.get("2층 판매장 환기2").toString())
										+ Integer.parseInt(electricdata.get("2층 판매장 냉난방1").toString()));
						error = Integer.parseInt(errordata.get("2층 전체 전력2").toString()) + Integer.parseInt(errordata.get("2층 판매장 전등1").toString())
								+ Integer.parseInt(errordata.get("2층 판매장 급탕1").toString()) + Integer.parseInt(errordata.get("2층 판매장 환기1").toString())
								+ Integer.parseInt(errordata.get("2층 판매장 환기2").toString()) + Integer.parseInt(errordata.get("2층 판매장 냉난방1").toString());
					} else if (sensor.equals("2002_2_3")) {
						total = Integer.parseInt(electricdata.get("2층 전체 전력3").toString())
								- (Integer.parseInt(electricdata.get("2층 사무실 전등1").toString())
										+ Integer.parseInt(electricdata.get("2층 사무실 환기1").toString()));
						error = Integer.parseInt(errordata.get("2층 전체 전력3").toString()) + Integer.parseInt(errordata.get("2층 사무실 전등1").toString())
								+ Integer.parseInt(errordata.get("2층 사무실 환기1").toString());
					} else if (sensor.equals("2002_3_1")) {
						total = Integer.parseInt(electricdata.get("3층 전체 전력1").toString())
								- (Integer.parseInt(electricdata.get("3층 홀 전등1").toString())
										+ Integer.parseInt(electricdata.get("3층 홀 환기1").toString())
										+ Integer.parseInt(electricdata.get("3층 홀 환기2").toString()));
						error = Integer.parseInt(errordata.get("3층 전체 전력1").toString()) + Integer.parseInt(errordata.get("3층 홀 전등1").toString())
								+ Integer.parseInt(errordata.get("3층 홀 환기1").toString()) + Integer.parseInt(errordata.get("3층 홀 환기2").toString());
					} else if (sensor.equals("2002_3_2")) {
						total = Integer.parseInt(electricdata.get("3층 전체 전력2").toString())
								- (Integer.parseInt(electricdata.get("공관숙소1 전등1").toString())
										+ Integer.parseInt(electricdata.get("공관숙소1 급탕1").toString())
										+ Integer.parseInt(electricdata.get("공관숙소1 환기1").toString())
										+ Integer.parseInt(electricdata.get("공관숙소1 환기2").toString())
										+ Integer.parseInt(electricdata.get("공관숙소1 냉난방1").toString()));
						error = Integer.parseInt(errordata.get("3층 전체 전력2").toString()) + Integer.parseInt(errordata.get("공관숙소1 전등1").toString())
								+ Integer.parseInt(errordata.get("공관숙소1 급탕1").toString()) + Integer.parseInt(errordata.get("공관숙소1 환기1").toString())
								+ Integer.parseInt(errordata.get("공관숙소1 환기2").toString()) + Integer.parseInt(errordata.get("공관숙소1 냉난방1").toString());
					} else if (sensor.equals("2002_3_3")) {
						total = Integer.parseInt(electricdata.get("3층 전체 전력3").toString())
								- (Integer.parseInt(electricdata.get("공관숙소2 전등1").toString())
										+ Integer.parseInt(electricdata.get("공관숙소2 급탕1").toString())
										+ Integer.parseInt(electricdata.get("공관숙소2 환기1").toString())
										+ + Integer.parseInt(electricdata.get("공관숙소2 냉난방1").toString()));
						error = Integer.parseInt(errordata.get("3층 전체 전력3").toString()) + Integer.parseInt(errordata.get("공관숙소2 전등1").toString())
								+ Integer.parseInt(errordata.get("공관숙소2 급탕1").toString()) + Integer.parseInt(errordata.get("공관숙소2 환기1").toString())
								+ Integer.parseInt(errordata.get("공관숙소2 냉난방1").toString());
					}

					insertdata1.put("sensor_sn", sensor);	// 전열센서 저장
					insertdata1.put("val_date", date);		// 현재 시각 저장
					insertdata1.put("total_wh", total);		// 위에서 계산한 사용량 저장
					if (error > 0) { // 계산식에 포함된 센서에 에러가 있을경우
						insertdata1.put("error_code", 1);	// 오류가 발생한 경우이므로 에러코드 1 저장
					} else {
						insertdata1.put("error_code", 0);	// 오류가 발생하지 않았으므로 에러코드 0 저장
					}

					/* DB에 저장하기위해 만든 map의 key와 value를 각각 리스트로 만들고, pk를 포함해서 컬럼, 값을 저장한 map, 제외한 map을 담을 리스트맵 2개를 선언한다. */
					List<String> keyList = new ArrayList<>(insertdata1.keySet());
					List<Object> valueList = new ArrayList<>(insertdata1.values());
					List<Map<String, Object>> result1 = new ArrayList<Map<String, Object>>();
					List<Map<String, Object>> result2 = new ArrayList<Map<String, Object>>();

					/* 각각 name,val이라는 키로 컬럼과 값을 각각의 map에 저장한다. */
					for (int p = 0; p < keyList.size(); p++) {
						insertdata2 = new HashMap<String, Object>();
						pkdata = new HashMap<String, Object>();

						insertdata2.put("name", keyList.get(p));
						insertdata2.put("val", valueList.get(p));

						if (!(keyList.get(p).equals("sensor_sn") || keyList.get(p).equals("val_date")
								|| keyList.get(p).equals("reg_date"))) {
							pkdata.put("name", keyList.get(p));
							pkdata.put("val", valueList.get(p));
							result2.add(pkdata);
						}

						result1.add(insertdata2);
					}

					insertdata3.put("result1", result1); 					 // pk를 포함한 컬럼,값을 저장한 리스트맵을 저장
					insertdata3.put("result2", result2); 					 // pk를 제외한 컬럼,값을 저장한 리스트맵을 저장
					insertdata3.put("meter", "bems_meter_electric_elechot"); // 데이터를 적재할 테이블의 이름
					insertdata3.put("dbname", DBname()); 					 // DB이름 저장

					this.edgeDBMapper.edgeMetertInsert(insertdata3);		 // DB 적제 시작
				} else { // 가져온 데이터가 없거나, 필요한 센서목록의 크기와 일치하지 않는 경우
					insertdata1 = new HashMap<String, Object>(); // 맵 초기화
					insertdata2 = new HashMap<String, Object>(); // 맵 초기화
					insertdata3 = new HashMap<String, Object>(); // 맵 초기화

					insertdata1.put("sensor_sn", sensor);	// 센서 저장
					insertdata1.put("val_date", date);		// 현재 시각 저장
					insertdata1.put("total_wh", 0);			// 사용량 0으로 변경
					insertdata1.put("error_code", 1); 		// 오류가 발생한것이므로 에러코드 1로 변경

					/* DB에 저장하기위해 만든 map의 key와 value를 각각 리스트로 만들고, pk를 포함해서 컬럼, 값을 저장한 map, 제외한 map을 담을 리스트맵 2개를 선언한다. */
					List<String> keyList = new ArrayList<>(insertdata1.keySet());
					List<Object> valueList = new ArrayList<>(insertdata1.values());
					List<Map<String, Object>> result1 = new ArrayList<Map<String, Object>>();
					List<Map<String, Object>> result2 = new ArrayList<Map<String, Object>>();

					/* 각각 name,val이라는 키로 컬럼과 값을 각각의 map에 저장한다. */
					for (int p = 0; p < keyList.size(); p++) {
						insertdata2 = new HashMap<String, Object>();
						pkdata = new HashMap<String, Object>();

						insertdata2.put("name", keyList.get(p));
						insertdata2.put("val", valueList.get(p));

						if (!(keyList.get(p).equals("sensor_sn") || keyList.get(p).equals("val_date")
								|| keyList.get(p).equals("reg_date"))) {
							pkdata.put("name", keyList.get(p));
							pkdata.put("val", valueList.get(p));
							result2.add(pkdata);
						}

						result1.add(insertdata2);
					}

					insertdata3.put("result1", result1);
					insertdata3.put("result2", result2);
					insertdata3.put("meter", "bems_meter_electric_elechot");
					insertdata3.put("dbname", DBname()); // DB이름 저장

					this.edgeDBMapper.edgeMetertInsert(insertdata3);
				}
			}
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
	}
}