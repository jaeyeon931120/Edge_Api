package egovframework.kevinlab.controller.api;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.view.json.MappingJacksonJsonView;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import egovframework.kevinlab.cmmn.CorsFilter;
import egovframework.kevinlab.service.api.AlarmService;
import egovframework.kevinlab.service.api.ControllerService;
import egovframework.kevinlab.service.api.DashBoardService;
import egovframework.kevinlab.service.api.EdgeDBService;
import egovframework.kevinlab.service.api.EnergyStatusService;
import egovframework.kevinlab.service.api.EquipmentService;
import egovframework.kevinlab.service.api.PredictionService;
import egovframework.kevinlab.service.api.SunlightService;
import egovframework.kevinlab.service.api.SystemLogService;

@Controller
@RequestMapping("/")
public class ApiController {
	
	@Resource
	MappingJacksonJsonView jsonView;
	
	@Resource
	CorsFilter corsFilter;

	@Resource(name = "DashBoardService")
	protected DashBoardService dashBoardService;
	
	@Resource(name = "EnergyStatusService")
	protected EnergyStatusService energyStatusService;
	
	@Resource(name = "SunlightService")
	protected SunlightService sunlightService;
	
	@Resource(name = "EquipmentService")
	protected EquipmentService equipmentService;
	
	@Resource(name = "AlarmService")
	protected AlarmService alarmService;
	
	@Resource(name = "EdgeDBService")
	protected EdgeDBService edgeDBService;
	
	@Resource(name = "ControllerService")
	protected ControllerService controllerService;
	
	@Resource(name = "PredictionService")
	protected PredictionService predictionService;
	
	@Resource(name = "SystemLogService")
	protected SystemLogService systemlogService;
	
	Logger log = LogManager.getLogger(ApiController.class);
	
	@RequestMapping(value ="/{parentPath}/{childPath}", method = RequestMethod.POST)
    public ResponseEntity<?> getUser(@PathVariable("parentPath") String parentPath, @PathVariable("childPath") String childPath
    		, HttpServletRequest request, HttpServletResponse res) throws Exception {
		
		HashMap<String,Object> data = new HashMap<String,Object>(); // return 용
		
		log.info("parentPath : " +parentPath +" childPath: "+childPath);
		log.info("---------- Log 테스트 ---------");
						
		StringBuilder stringBuilder = new StringBuilder();
		BufferedReader bufferedReader = null;
	 
        try {
            res.setHeader("Access-Control-Allow-Origin", "http://localhost:3030");
            res.setHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS, DELETE");
            res.setHeader("Access-Control-Max-Age", "3600");
            res.setHeader("Access-Control-Allow-Headers", "Content-Type, Access-Control-Allow-Headers, Authorization, X-Requested-With");
        	
            InputStream inputStream = request.getInputStream();
            if (inputStream != null) {

                bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

                char[] charBuffer = new char[128];
                int bytesRead = -1;
                while ((bytesRead = bufferedReader.read(charBuffer)) > 0) {
                    stringBuilder.append(charBuffer, 0, bytesRead);
                }
            }
        } catch (IOException ex) {
            throw ex;
        } finally {
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (IOException ex) {
                    throw ex;
                }
            }
        }
        
        HashMap<String,Object> param = new HashMap<String,Object>();
        ObjectMapper mapper = new ObjectMapper();
    	
        try {
        	param = mapper.readValue(stringBuilder.toString(), new TypeReference<HashMap<String,Object>>(){});
    		 
    		String[] stringData; // // return용 String 배열에 데이터를 파싱하기 위한 맵
    		HashMap<String,Object> data1 = new HashMap<String,Object>(); // return용 Map에 데이터를 파싱하기 위한 맵
    		HashMap<String,Object> data2 = new HashMap<String,Object>(); // return용 Map에 데이터를 파싱하기 위한 맵
    		HashMap<String,Object> data3 = new HashMap<String,Object>(); // return용 Map에 데이터를 파싱하기 위한 맵
    		List<Map<String, Object>> dataList = new ArrayList<Map<String, Object>>(); // return용 Map에 데이터를 파싱하기 위한 맵리스트
    		    		    		    		
			if (parentPath.equals("EdgeData")) {
				if (childPath.equals("dashboard_left_data")) { // Edge 대시보드 왼쪽 데이터(태양광과 층별 데이터는 층에 따라 변화하는 데이터가 아니다.)
					data1 = (HashMap<String, Object>) this.dashBoardService.dash_sunlight(param);
					data.put("DASHBOARD_SUNLIGHT", data1);
				} else if (childPath.equals("dashboard_right_data")) { // Edge 대시보드 오른쪽 데이터(실시간 에너자 사용량 및 용도별 사용 현황은 층에 따라 변화하는 데이터다.)
					dataList = this.dashBoardService.dash_realtime_power(param);
					data2 = (HashMap<String, Object>) this.dashBoardService.dash_floor(param);
					data3 = (HashMap<String, Object>) this.dashBoardService.dash_application(param);
					data.put("DASHBOARD_FLOOR", data2);
					data.put("DASHBOARD_REALTIMEPOWER", dataList);
					data.put("DASHBOARD_APPLICATION", data3);
				} else if (childPath.equals("energystatus_floor_info")) { // Edge 에너지현황 위치(층별이름) 데이터
					stringData = this.energyStatusService.energy_floor_info(param);
					data.put("ENERGYSTATUS_FLOORINFO", stringData);
				} else if (childPath.equals("energystatus_hour_data")) { // Edge 에너지현황 시간별 그래프 데이터
					data1 = (HashMap<String, Object>) this.energyStatusService.energy_hour_data(param);
					data.put("ENERGYSTATUS_HOURDATA", data1);
				} else if (childPath.equals("energystatus_5min_data")) { // Edge 에너지현황 5분 데이터
					dataList = this.energyStatusService.energy_5min_data(param);
					data.put("ENERGYSTATUS_5MINDATA", dataList);
				} else if (childPath.equals("sunlight_data")) { // Edge 태양광 데이터
					data1 = (HashMap<String, Object>) this.sunlightService.sunlightData(param);
					data2 = (HashMap<String, Object>) this.sunlightService.sunlightStatus(param);
					data3 = (HashMap<String, Object>) this.sunlightService.independence(param);
					dataList = this.sunlightService.sunlightGraph(param);
					data.put("SUNLIGHT_DATA", data1);
					data.put("SUNLIGHT_STATUS", data2);
					data.put("SUNLIGHT_INDEPENDENCE", data3);
					data.put("SUNLIGHT_GRAPH", dataList);
				} else if (childPath.equals("equipment_data")) { // Edge 설비현황 데이터
					data1 = (HashMap<String, Object>) this.equipmentService.equipmentData(param);
					dataList = this.equipmentService.equipmentGraph(param);
					data.put("EQUIPMENT_DATA", data1);
					data.put("EQUIPMENT_GRAPH", dataList);
				} else if (childPath.equals("alarm_data")) { // Edge 알람 데이터
					dataList = this.alarmService.alarmData(param);
					data.put("ALARM_DATA", dataList);
				} else if (childPath.equals("sync_data")) { // Edge 데이터 재전송
					data1 = (HashMap<String, Object>)this.edgeDBService.Edge_All_Data(param);
					data.put("result", data1);
				} else if (childPath.equals("login")) { // Edge 로그인
					data1 = (HashMap<String, Object>) this.edgeDBService.Edge_Login(param);
					data.put("result", data1);
				} else if (childPath.equals("test_data")) { // Edge 테스트 데이터(여러가지 용도의 테스트를 위한 url)
					data1 = (HashMap<String, Object>)this.edgeDBService.Edge_Set_BackUpDate(param);
					data.put("result", data1);
				} else if (childPath.equals("controller")) { // Edge 제어
					data1 = (HashMap<String, Object>)this.controllerService.controller(param);
					data.put("result", data1);
				} else if (childPath.equals("equipment_sync")) { // 장비 동기화
					data1 = (HashMap<String, Object>)this.edgeDBService.equipmentSync(param);
					data.put("result", data1);
				} else if (childPath.equals("set_backup_date")) { // 백업 시간 설정
					data1 = (HashMap<String, Object>)this.edgeDBService.Edge_Set_BackUpDate(param);
					data.put("result", data1);
				} else if (childPath.equals("alarm_count")) { // 알람 카운트
					data1 = (HashMap<String, Object>) this.alarmService.alarmCounter(param);
					data.put("ALARM_COUNTER", data1);
				} else if (childPath.equals("alarm_confirm")) { // 알람 카운트 초기화
					data1 = (HashMap<String, Object>) this.alarmService.alarmConfirm(param);
					data.put("result", data1);
				} else if (childPath.equals("systemlog_data")) { // 시스템 로그 데이터
					dataList = this.systemlogService.Edge_SystemLog_Data(param);
					data.put("SYSTEMLOG_DATA", dataList);
				} else if (childPath.equals("prediction_top")) { // 예상 사용량					
					dataList = this.predictionService.predictionDataTop(param);					
					data.put("PREDICTION_TOP", dataList);					
				} else if (childPath.equals("prediction_bottom")) { // 예상 사용량
					dataList = this.predictionService.predictionDataBottom(param);					
					data.put("PREDICTION_BOTTOM", dataList);					
				} else {
					data.put("result", "api 서버 이상 url 호출 오류");
				}
	        		        	
	        }else {	        	
	        	data.put("result", "api 서버 이상 url 호출 오류");
	        }

        } catch (Exception e) {
        	data.put("result", "json 데이터 value값 오류");
        	e.printStackTrace();
		}
		return new ResponseEntity<Object>(data, HttpStatus.OK);
    }
}