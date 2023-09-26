package egovframework.kevinlab.service.api;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract interface SystemLogService {
	
	// Edge system log data
	public List<Map<String, Object>> Edge_SystemLog_Data(HashMap<String, Object> param) throws Exception;
	
	// Edge system log insert
	public void Edge_Systemlog_Insert(Map<String, Object> param) throws Exception;
	
	// Edge system log error update
	public void Edge_Systemlog_Update(Map<String, Object> param) throws Exception;
}
