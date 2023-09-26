package egovframework.kevinlab.service.api;


import java.util.HashMap;
import java.util.List;
import java.util.Map;


public abstract interface PredictionService {
	
	//사용량 예측량 상단
	public List<Map<String, Object>> predictionDataTop(HashMap<String, Object> param) throws Exception;
	//사용량 예측 하단
	public List<Map<String, Object>> predictionDataBottom(HashMap<String, Object> param) throws Exception;
	}
