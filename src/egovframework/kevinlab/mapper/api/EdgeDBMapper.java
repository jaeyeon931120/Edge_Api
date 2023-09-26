package egovframework.kevinlab.mapper.api;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Repository;

import egovframework.rte.psl.dataaccess.EgovAbstractMapper;

@Repository("edgedbMapper")
public class EdgeDBMapper extends EgovAbstractMapper {
	
	// EdgeDB
	public Map<String, Object> getComplexcode(HashMap<String, Object> param) {
		return selectOne("edgedbMapper.getComplexcode", param);
	}
	
	// lbems 해당 테이블 컬럼 네임 가져오기
	public List<Map<String, Object>> columnsName(HashMap<String, Object> param) {
		return selectList("edgedbMapper.columnsName", param);
	}
	
	// lbems 데이터 재전송을 위한 엣지DB에서 데이터 가져오기
	public List<Map<String, Object>> edgeSyncData(HashMap<String, Object> param) {		
		return selectList("edgedbMapper.edgeSyncData", param);
	}
	
	// lbemsDB에 로그인
	public Map<String, Object> lbemsLoginData(HashMap<String, Object> param) {		
		return selectOne("edgedbMapper.lbemsLoginData", param);
	}
	
	// EdgeDB에 로그인
	public Map<String, Object> edgeLoginData(HashMap<String, Object> param) {
		return selectOne("edgedbMapper.edgeLoginData", param);
	}
	
	// EdgeDB에 로그인 업데이트
	public int edgeLoginUpdate(HashMap<String, Object> param) {
		return update("edgedbMapper.edgeLoginUpdate", param);
	}
	
	// EdgeDB 테이블별 센서 가져오기
	public List<Map<String, Object>> edgeSensorName(HashMap<String, Object> param) {
		return selectList("edgedbMapper.edgeSensorName", param);
	}
	
	// EdgeDB 시간별 통계 가져오기
	public List<Map<String, Object>> edgeHourData(HashMap<String, Object> param) {
		return selectList("edgedbMapper.edgeHourData", param);
	}
	
	// EdgeDB 시간별 통계 가져오기
	public List<Map<String, Object>> edgeDayData(HashMap<String, Object> param) {
		return selectList("edgedbMapper.edgeDayData", param);
	}
	
	// EdgeDB 시간별 통계 insert
	public int hourDataInsert(Map<String, Object> param) {
		return insert("edgedbMapper.hourDataInsert", param);
	}
	
	// EdgeDB 시간별 통계 insert
	public int dayDataUpdate(Map<String, Object> param) {
		return update("edgedbMapper.dayDataUpdate", param);
	}
	
	// EdgeDB 삭제를 위한 전체 테이블이름
	public List<Map<String, Object>> edgeTableNames(HashMap<String, Object> param) {
		return selectList("edgedbMapper.edgeTableNames", param);
	}
	
	// EdgeDB 삭제
	public int edgeBackup(Map<String, Object> param) {
		return delete("edgedbMapper.edgeBackUp", param);
	}
	
	// EdgeDB 컬럼확인하여 각 테이블별 조건걸어서 검색
	public Map<String, Object> edgeCheckdata(Map<String, Object> param) {
		return selectOne("edgedbMapper.edgeCheckdata", param);
	}
	
	// EdgeDB 백업 기간설정 값 업데이트
	public int edgeSetDate(Map<String, Object> param) {
		return insert("edgedbMapper.edgeSetDate", param);
	}
	
	// EdgeDB 백업 기간설정된 값 가져오기
	public Map<String, Object> edgeGetDate(Map<String, Object> param) {
		return selectOne("edgedbMapper.edgeGetDate", param);
	}
	
	// EdgeDB 장비 동기화
	public int edgeEquipmentSync(Map<String, Object> param) {
		return insert("edgedbMapper.edgeEquipmentSync", param);
	}
	
	// EdgeDB 태양광 동기화
	public int edgeSolartSync(Map<String, Object> param) {
		return insert("edgedbMapper.edgeSolartSync", param);
	}
	
	// EdgeDB 각 지역코드별 층별 이름
	public List<Map<String, Object>> getFloorname(Map<String, Object> param) {
		return selectList("edgedbMapper.getFloorname", param);
	}
	
	// EdgeDB electric 전체전력 센서
	public List<Map<String, Object>> getAllSensorName(Map<String, Object> param) {
		return selectList("edgedbMapper.getAllSensorName", param);
	}
	
	// EdgeDB ntek meter테이블 데이터
	public Map<String, Object> ntekMeterSelect(Map<String, Object> param) {
		return selectOne("edgedbMapper.ntekMeterSelect", param);
	}
	
	// Edge 테이블별 센서
	public List<Map<String, Object>> electricSensorData(Map<String, Object> param) {
		return selectList("edgedbMapper.electricSensorData", param);
	}
	
	// Edge 신재생 에너지량
	public Map<String, Object> renewableSelect(Map<String, Object> param) {
		return selectOne("edgedbMapper.renewableSelect", param);
	}
	
	// Edge 각 에너지원별 meter테이블 데이터 insert
	public int edgeMetertInsert(Map<String, Object> param) {
		return insert("edgedbMapper.edgeMetertInsert", param);
	}
}