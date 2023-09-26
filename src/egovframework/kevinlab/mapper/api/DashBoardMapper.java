package egovframework.kevinlab.mapper.api;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Repository;

import egovframework.rte.psl.dataaccess.EgovAbstractMapper;

@Repository("dashboardMapper")
public class DashBoardMapper extends EgovAbstractMapper {
	
//	// lbems 대시보드 센서정보 데이터 가져오기(현시점에서는 api로 정보를 받는거여서 센서번호는 하드코딩으로 대체한다.)
//	public Map<String, Object> dashSensor(HashMap<String, Object> param) {
//		return selectOne("dashboardMapper.dashSensor", param);
//	}
	
	// Edge 대시보드 태양광 발전정보 데이터 가져오기
	public Map<String, Object> dashSunlightData(HashMap<String, Object> param) {		
		return selectOne("dashboardMapper.dashSunlight", param);
	}
	
	// Edge 대시보드 실시간 전력사용량 데이터 가져오기
	public List<Map<String, Object>> dashRealTimePower(HashMap<String, Object> param) {		
		return selectList("dashboardMapper.dashRealTimePower", param);
	}
	
	// Edge 대시보드 실시간 전력사용량 데이터 가져오기
	public List<Map<String, Object>> dashFloor(HashMap<String, Object> param) {
		return selectList("dashboardMapper.dashFloor", param);
	}

	// Edge 대시보드 실시간 용도별 에너지소비정보 데이터 가져오기
	public Map<String, Object> dashApplication(HashMap<String, Object> param) {
		return selectOne("dashboardMapper.dashApplication", param);
	}
	
	// EdgeDB 테이블별 센서 가져오기
	public List<Map<String, Object>> edgeSensorName(HashMap<String, Object> param) {
		return selectList("dashboardMapper.edgeSensorName", param);
	}
}