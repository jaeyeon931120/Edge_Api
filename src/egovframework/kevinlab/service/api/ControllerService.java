package egovframework.kevinlab.service.api;


import java.util.HashMap;
import java.util.List;
import java.util.Map;


public abstract interface ControllerService {
	
	// Edge 제어
	public Map<String, Object> controller(HashMap<String, Object> param) throws Exception;
}
