package egovframework.kevinlab.mapper.api;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Repository;

import egovframework.rte.psl.dataaccess.EgovAbstractMapper;

@Repository("sunlightMapper")
public class SunlightMapper extends EgovAbstractMapper {
	
	// Edge 태양광 발전정보 데이터 가져오기
	public Map<String, Object> sunlightData(HashMap<String, Object> param) {		
		return selectOne("sunlightMapper.sunlightData", param);
	}
	
	// Edge 실시간 태양광 발전현황 데이터 가져오기
	public Map<String, Object> sunlightStatus(HashMap<String, Object> param) {		
		return selectOne("sunlightMapper.sunlightStatus", param);
	}
	
	// Edge 시간별 태양광 데이터 그래프 가져오기
	public List<Map<String, Object>> sunlightGraph(HashMap<String, Object> param) {
		return selectList("sunlightMapper.sunlightGraph", param);
	}
	
	// Edge 에너지자립률을 구하기 위한 태양광 발전량 및 소비량
	public List<Map<String, Object>> independence_output(HashMap<String, Object> param) {
		return selectList("sunlightMapper.independence_output", param);
	}
	
	// Edge 에너지자립률을 구하기 위한 전력소비량
	public Map<String, Object> independence_consumption(HashMap<String, Object> param) {
		return selectOne("sunlightMapper.independence_consumption", param);
	}
	
	// Edge 테이블별 센서 가져오기
	public List<Map<String, Object>> edgeSensorName(HashMap<String, Object> param) {
		return selectList("sunlightMapper.edgeSensorName", param);
	}
}