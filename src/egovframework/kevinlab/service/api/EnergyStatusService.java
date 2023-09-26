package egovframework.kevinlab.service.api;


import java.util.HashMap;
import java.util.List;
import java.util.Map;


public abstract interface EnergyStatusService {
	
	// Edge 에너지현황 페이지 전체 데이터 response
	public List<Map<String, Object>> energyStatus_data(HashMap<String, Object> param) throws Exception;
	
	// Edge 시간별 에너지사용 현황 response
	public String[] energy_floor_info(HashMap<String, Object> param) throws Exception;

	// Edge 시간별 에너지사용 현황 response
	public Map<String, Object> energy_hour_data(HashMap<String, Object> param) throws Exception;
	
	// Edge 5분별 에너지사용 현황 response
	public List<Map<String, Object>> energy_5min_data(HashMap<String, Object> param) throws Exception;
}
