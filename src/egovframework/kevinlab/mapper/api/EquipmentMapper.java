package egovframework.kevinlab.mapper.api;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Repository;

import egovframework.rte.psl.dataaccess.EgovAbstractMapper;

@Repository("equipmentMapper")
public class EquipmentMapper extends EgovAbstractMapper {
	
//	// lbems 설비현황 센서정보 데이터 가져오기(현시점에서는 api로 정보를 받는거여서 센서번호는 하드코딩으로 대체한다.)
//	public Map<String, Object> energyStatusSensor(HashMap<String, Object> param) {
//		return selectOne("EnergyStatusMapper.energyStatusSensor", param);
//	}
	
	// Edge 설비현황 데이터 가져오기
	public Map<String, Object> equipmentData(HashMap<String, Object> param) {		
		return selectOne("equipmentMapper.equipmentData", param);
	}
	
	// Edge 설비현황 시간별 그래프 데이터 가져오기
	public List<Map<String, Object>> equipmentGraph(HashMap<String, Object> param) {		
		return selectList("equipmentMapper.equipmentGraph", param);
	}
}