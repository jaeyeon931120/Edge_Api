package egovframework.kevinlab.service.api;


import java.util.HashMap;
import java.util.List;
import java.util.Map;


public abstract interface EquipmentService {
	
	// Edge 설비현황 페이지 전체 데이터 response
	public List<Map<String, Object>> equipment_All_Data(HashMap<String, Object> param) throws Exception;

	// Edge 설비현황 데이터 response
	public Map<String, Object> equipmentData(HashMap<String, Object> param) throws Exception;
	
	// Edge 설비현황 그래프 데이터 response
	public List<Map<String, Object>> equipmentGraph(HashMap<String, Object> param) throws Exception;
}
