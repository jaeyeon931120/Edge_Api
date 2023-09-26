package egovframework.kevinlab.mapper.api;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Repository;

import egovframework.rte.psl.dataaccess.EgovAbstractMapper;

@Repository("alarmMapper")
public class AlarmMapper extends EgovAbstractMapper {
	
	// edge 알람 갯수 가져오기
	public Map<String, Object> alarmCounter(HashMap<String, Object> param) {		
		return selectOne("alarmMapper.alarmCounter", param);
	}
	
	// edge 알람 데이터 가져오기
	public List<Map<String, Object>> alarmData(HashMap<String, Object> param) {		
		return selectList("alarmMapper.alarmData", param);
	}
	
	// edge 센서별 알람 데이터 가져오기
	public Map<String, Object> getSensorInformation(HashMap<String, Object> param) {
		return selectOne("alarmMapper.getSensorInformation", param);
	}
	
	// edge 알람 로그 생성
	public int alarmLogInsert(Map<String, Object> param) {
		return insert("alarmMapper.alarmLogInsert", param);
	}
	
	// edge 알람 로그 확인
	public List<Map<String, Object>> getAlarmOnOff(Map<String, Object> param) {
		return selectList("alarmMapper.getAlarmOnOff", param);
	}
	
	// EdgeDB 컬럼확인하여 각 테이블별 조건걸어서 검색
	public Map<String, Object> edgeCheckdata(Map<String, Object> param) {
		return selectOne("alarmMapper.edgeCheckdata", param);
	}
	
	// edge 알람 해결 시간 업데이트
	public int updateAlarm(Map<String, Object> param) {
		return update("alarmMapper.updateAlarm", param);
	}
	
	// edge 알람 해결 시간 업데이트
	public int updateConfirm(Map<String, Object> param) {
		return update("alarmMapper.updateConfirm", param);
	}
}