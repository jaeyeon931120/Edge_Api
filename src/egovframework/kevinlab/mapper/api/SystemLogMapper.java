package egovframework.kevinlab.mapper.api;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Repository;

import egovframework.rte.psl.dataaccess.EgovAbstractMapper;

@Repository("systemlogMapper")
public class SystemLogMapper extends EgovAbstractMapper {
	
	// edge 시스템 로그 데이터 가져오기
	public List<Map<String, Object>> systemLogData(HashMap<String, Object> param) {		
		return selectList("systemlogMapper.systemLogData", param);
	}
	
	// edge 시스템 로그 생성
	public int systemLogInsert(Map<String, Object> param) {
		return insert("systemlogMapper.systemLogInsert", param);
	}
	
	// edge 시스템 로그 에러 업데이트
	public int systemLogUpdate(Map<String, Object> param) {
		return update("systemlogMapper.systemLogUpdate", param);
	}
}