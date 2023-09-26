package egovframework.kevinlab.mapper.api;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Repository;

import egovframework.rte.psl.dataaccess.EgovAbstractMapper;

@Repository("predictionMapper")
public class PredictionMapper extends EgovAbstractMapper {

	public List<Map<String, Object>> predictionTop(HashMap<String, Object> parsingdata) {		
		return selectList("predictionMapper.predictionTop", parsingdata);
	}
		
	public List<Map<String, Object>> predictionBottom(HashMap<String, Object> param) {		
		return selectList("predictionMapper.predictionBottom", param);
	}
}