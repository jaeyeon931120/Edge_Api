package egovframework.kevinlab.service.impl.api;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import org.apache.ibatis.io.Resources;
import org.springframework.stereotype.Service;

import egovframework.kevinlab.service.api.ControllerService;

@Service("ControllerService")
public class ControllerServiceImpl implements ControllerService {

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
     * EdgeDB의 태백산 에어컨시스템 제어
     *
     * @param 컨트롤 시스템 사용에 필요한 fc, 에어컨id, complexcode, opeartion, cmd 값
     *
     * @return map
     */
	public Map<String, Object> controller(HashMap<String, Object> param) throws Exception {

		HashMap<String, Object> parsingdata = new HashMap<String, Object>(); // sql에 필요한 조건을 넘겨주는 맵
		Map<String, Object> c_result = new HashMap<String, Object>(); // lbemsdb api 데이터를 수신 후 결과확인을 위한 맵
		List<Map<String, Object>> columnsname = new ArrayList<Map<String, Object>>(); // 반환할 데이터

		try {
			String fc = null;
			String id = null;
			String complex_code = null;
			String operation = null;
			String cmd = null;
			String[] ids = new String[] {};

			if (param.get("fc") != null) {
				fc = param.get("fc").toString(); // 받아온 fc 저장
			} else {
				c_result.put("result", "nok");
				c_result.put("reason", "FunctionCode의 값이 없습니다. FunctionCode의 값을 확인해 주시기 바랍니다.");
				return c_result;
			}
			if (param.get("id") != null) {
				id = param.get("id").toString(); // 받아온 id 저장
				if(id.indexOf(",") > -1) {
					ids = id.replace("[", "").replace("]", "").split(",");
				}
			} else {
				c_result.put("result", "nok");
				c_result.put("reason", "에어컨 ID의 값이 없습니다. 에어컨 ID의 값을 확인해 주시기 바랍니다.");
				return c_result;
			}
			if(param.get("complexcode") != null) {
				complex_code = param.get("complexcode").toString(); // 받아온 complexcode 저장
			} else {
				c_result.put("result", "nok");
				c_result.put("reason", "complexcode의 값이 없습니다. complexcode의 값을 확인해 주시기 바랍니다.");
				return c_result;
			}
			if(fc.equals("fc5") || fc.equals("fc6")) {
				if (param.get("operation") != null) {
					operation = param.get("operation").toString(); // 받아온 operation값 저장
				} else {
					c_result.put("result", "nok");
					c_result.put("reason", "operation의 값이 없습니다. operation의 값을 확인해 주시기 바랍니다.");
					return c_result;
				}
				if (param.get("cmd") != null) {
					cmd = param.get("cmd").toString(); // 받아온 cmd값 저장
				} else {
					c_result.put("result", "nok");
					c_result.put("reason", "cmd의 값이 없습니다. cmd의 값을 확인해 주시기 바랍니다.");
					return c_result;
				}
			}

			if(ids.length > 0) {
				for(int i=0; i<ids.length; i++) { // ids 즉, id값이 여러개가 올 경우
					parsingdata.putAll(param); // 받아온 값 전체 parsingdata에 저장
					parsingdata.put("id", id); // id 값 저장
					c_result.putAll(getController(parsingdata)); // 에어컨 제어
				}
			} else {
				parsingdata.putAll(param); // 받아온 값 전체 parsingdata에 저장
				c_result.putAll(getController(parsingdata)); // 에어컨 제어
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return c_result;
	}

	 /**
     * 받아온 정보(fc값, id, complexcode 등)을 정리해서 url로 변환하여 제어url에 접속
     *
     * @param 제어에 필요한 값(fc값, id, complexcode 등)
     *
     * @return Map
     */
	public Map<String, Object> getController(HashMap<String, Object> param) {
		Map<String, Object> parsingdata = new HashMap<String, Object>(); // 제어 통신 결과값 보내는 맵
		try {
			String c_param = ""; // url를 정리하기 위한 c_param 변수 선언
			String fc = param.get("fc").toString(); // 받아온 fc값 저장
			InetAddress ipAddress = InetAddress.getLocalHost(); // 로컬호스트 ip받아오기
			if(fc.equals("fc5") || fc.equals("fc6")) { // fc의 값이 fc5, fc6일 경우 (구분하는 이유는, fc5,fc6은 에어컨 제어 나머지는 에어컨 상태값이기 때문)
				c_param = param.get("fc").toString() + "?id=" + param.get("id").toString() + "&complex_code=" + param.get("complexcode").toString()
				+ "&operation=" + param.get("operation").toString() + "&cmd=" + param.get("cmd").toString();  // 제어를 위한 operation, cmd값 추가
			} else {
				// 상태를 확인하기 위한 fc값과 id값 complexcode값 확인
				c_param = param.get("fc").toString() + "?id=" + param.get("id").toString() + "&complex_code=" + param.get("complexcode").toString();
			}

			String apiURL = "http://" + ipAddress.getHostAddress() + ":5001/lg/" + c_param; // 앞에 제어통신을 위한 ip와 url을 위에서 정리한 값 추가
			URL url = new URL(apiURL); // 위에서 적은 api로 url 생성
			HttpURLConnection con = (HttpURLConnection) url.openConnection(); // urlconnection 즉, url 개통

			con.setRequestMethod("GET"); // 통신method 설정
			con.setRequestProperty("Content-Type", "application/json"); // JSON DATA로 통신
			con.setRequestProperty("Accept-Charset", "UTF-8"); // UTF-8형식으로 통신
			con.setUseCaches(false); // 컨트롤 캐쉬 설정(캐시사용 X)
			con.setDoInput(true); // InputStream으로 서버로 부터 응답을 받겠다는 옵션.

			int responseCode = con.getResponseCode(); // 통신 코드 받아오기
			BufferedReader br;
			if (responseCode == 200) { // 정상 호출
				br = new BufferedReader(new InputStreamReader(con.getInputStream()));
				parsingdata.put("result", "ok"); // 결과를 반환할 맵에 담기
			} else { // 에러 발생
				br = new BufferedReader(new InputStreamReader(con.getErrorStream()));
				parsingdata.put("result", "nok"); // 결과를 반환할 맵에 담기
			}
			String inputLine;
			String res = new String();
			while ((inputLine = br.readLine()) != null) { // 에러 발생시 또는 결과값 출력
				res += inputLine; // 발생한 결과를 res에 저장
			}

			String[] result = new String[] {};
			result = res.replace("[", "").replace("]", "").split(","); // []부분을 제거하는 이유는, 제어에서 오는 결과값이 []를 포함해서 오기때문에
			List<String> list = Arrays.stream(result).collect(Collectors.toList());

			if(parsingdata.get("result").toString().equals("ok")) { // 에러 발생시 이유 출력
				for(int i=0; i<list.size(); i++) {
					if(list.get(i) != null || !list.get(i).equals("")) {
						if(fc.equals("fc1")) {
							parsingdata.put("status" + Integer.toString(i), list.get(i).trim()); // 이유를 반환할 맵에 담기
						} else if(fc.equals("fc3")) {
							parsingdata.put("status" + Integer.toString(i), list.get(i).trim()); // 이유를 반환할 맵에 담기
						} else {
							parsingdata.put("status", res); // 이유를 반환할 맵에 담기
						}
					}
				}
			} else { // 에러가 없을시 엣지 서버 api 외부 규격서에 따른 -로 통일
				parsingdata.put("reason", "통신제어중에 오류가 발생했습니다. 다시 시도해주세요."); // 이유를 반환할 맵에 담기
			}
			br.close(); // bufferReader 종료
			con.disconnect(); // url connect 종료
		} catch (Exception e) {
			e.printStackTrace();
		}

		return parsingdata; // 결과값 반환
	}
}