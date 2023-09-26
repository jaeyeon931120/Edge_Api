package egovframework.kevinlab.service.api;


import java.util.HashMap;
import java.util.List;
import java.util.Map;


public abstract interface DashBoardService {
	
	//Edge 대시보드 전체 데이터 response
	public List<Map<String, Object>> dashboard_data(HashMap<String, Object> param) throws Exception;
	
	//Edge 대시보드 태양광 발전정보 데이터 response
	public Map<String, Object> dash_sunlight(HashMap<String, Object> param) throws Exception;
	
	//Edge 대시보드 실시간 전력사용량 데이터 response
	public List<Map<String, Object>> dash_realtime_power(HashMap<String, Object> param) throws Exception;
	
	//Edge 대시보드 층별 에너지소비정보 데이터 response
	public Map<String, Object> dash_floor(HashMap<String, Object> param) throws Exception;
	
	//Edge 대시보드 용도별 에너지소비정보 데이터 response
	public Map<String, Object> dash_application(HashMap<String, Object> param) throws Exception;
}
