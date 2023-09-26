package egovframework.kevinlab.service.api;


import java.util.HashMap;
import java.util.List;
import java.util.Map;


public abstract interface SunlightService {
	
	//Edge 태양광 전체 데이터 response
	public List<Map<String, Object>> sunlightAllData(HashMap<String, Object> param) throws Exception;
	
	//Edge 태양광 발전정보 데이터 response
	public Map<String, Object> sunlightData(HashMap<String, Object> param) throws Exception;
	
	//Edge 태양광 실시간 발전효율 및 시간 데이터 response
	public Map<String, Object> sunlightStatus(HashMap<String, Object> param) throws Exception;
	
	//Edge 태양광 에너지 자립률 데이터 response
	public Map<String, Object> independence(HashMap<String, Object> param) throws Exception;
	
	//Edge 태양광 발전량 시간별 그래프 데이터 response
	public List<Map<String, Object>> sunlightGraph(HashMap<String, Object> param) throws Exception;
}
