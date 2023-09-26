package egovframework.kevinlab.mapper.api;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Repository;

import egovframework.rte.psl.dataaccess.EgovAbstractMapper;

@Repository("energystatusMapper")
public class EnergyStatusMapper extends EgovAbstractMapper {
	
//	// lbems 에너지현황 센서정보 데이터 가져오기(현시점에서는 api로 정보를 받는거여서 센서번호는 하드코딩으로 대체한다.)
//	public Map<String, Object> energyStatusSensor(HashMap<String, Object> param) {
//		return selectOne("EnergyStatusMapper.energyStatusSensor", param);
//	}
	
	// Edge 에너지현황 시간 데이터 가져오기
	public List<Map<String, Object>> energystatusFloor(HashMap<String, Object> param) {
		return selectList("energystatusMapper.energystatusFloor", param);
	}
	
	// Edge 에너지현황 시간 데이터 가져오기
	public Map<String, Object> energystatusHourdata(HashMap<String, Object> param) {		
		return selectOne("energystatusMapper.energystatusHourdata", param);
	}
	
	// Edge 에너지현황 5분 데이터 가져오기
	public List<Map<String, Object>> energystatus5mindata(HashMap<String, Object> param) {		
		return selectList("energystatusMapper.energystatus5mindata", param);
	}
}