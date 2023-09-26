package egovframework.kevinlab.service.api;


import java.util.HashMap;
import java.util.List;
import java.util.Map;


public abstract interface AlarmService {
	
	// Edge 알람 페이지 전체 데이터 response
	public List<Map<String, Object>> alarm_All_Data(HashMap<String, Object> param) throws Exception;

	// Edge 알람 갯수 response
	public Map<String, Object> alarmCounter(HashMap<String, Object> param) throws Exception;
	
	// Edge 알람 데이터 response
	public List<Map<String, Object>> alarmData(HashMap<String, Object> param) throws Exception;
	
	// Edge 알람 로그 insert
	public void Edge_alarm_insert() throws Exception;
	
	// Edge 알람 확인시 카운트 없앰
	public Map<String, Object> alarmConfirm(HashMap<String, Object> param) throws Exception;
}
