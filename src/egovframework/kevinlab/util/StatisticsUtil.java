package egovframework.kevinlab.util;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.map.LinkedMap;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.math3.distribution.TDistribution;
import org.apache.commons.math3.exception.NotStrictlyPositiveException;
import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;
import org.apache.commons.math3.util.FastMath;
//import org.apache.log4j.Logger;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;

public class StatisticsUtil {
	
	/** Log */
//	private Logger log = Logger.getLogger(StatisticsUtil.class);
	private List<Double> data;
	private Map<String,List<Double>> stsData;
	public DecimalFormat df = new DecimalFormat("#0.0000000000");
	DecimalFormat df_out = new DecimalFormat("#0.0000000000");
	
	//과거데이터를 이용한 열(col)별 추정식 가져오기
//	public Map<String,String> getColsEquation(Map<String,List<String>> colMapData){
//		
//		StatisticsUtil sts = new StatisticsUtil();
//		
//		Map<String,String> ret = new HashMap<String,String>();
//		
//			Set<String> set = colMapData.keySet();
//			for(String key : set){
//				try{
//					List<String> list = colMapData.get(key);			
//					
//					Map<String,List<Double>> stsData = new LinkedMap();
//					List<Double> YList = new ArrayList<Double>();
//					List<Double> X1List = new ArrayList<Double>();
//					double i = 1;
//					for(String v:list){
//						YList.add(Double.parseDouble(v));
//						X1List.add(i++);
//					}
//					stsData.put("Y", YList);
//					stsData.put("X1", X1List);
//					sts.setSTSDataSource(stsData);			
//					//통계 결과값 가져오기
//					Map<String,String> rm = sts.getResultMap();			
//					ret.put(key, rm == null?"":rm.get("equation"));
//					ret.put(key+"_"+"size", rm == null?"0":String.valueOf(list.size()));
//				} catch(Exception e){
//					continue;
//				}
//			}				
//		return ret;
//	}
	
//	public String getResultMap(List list,String key) throws Exception{
//		StatisticsUtil sts = new StatisticsUtil();		
//		//통계 데이타 설정
//		sts.setSTSDataSource(parseGridData(list));
//		return getResultMap(sts,key);
//	}
//	
//	public String getResultMap(StatisticsUtil sts,String key) throws Exception{		
//		Map<String,String> rm = null;
//		try{
//			rm = sts.getResultMap();
//		}catch(Exception e){}		
//		return rm == null?"":rm.get(key);//"equation"		
//	}
	
	
	
	public Map<String,List<Double>> parseGridData(String gridDataStr) throws Exception{
		
		List gridData = JSONUtil.parse(gridDataStr);
		
		return parseGridData(gridData);
	}
	
	public Map<String,List<Double>> parseGridData(List<Map<String,Object>> gridData) throws JsonParseException, JsonMappingException, IOException{
		
		Map<String,List<Double>> stsData = new LinkedMap();
		for(Map<String,Object> m : gridData){
			Set<String> set = m.keySet();
			int i=0;
			for(String key : set){
				String key_ = "";
				if(0 == i) key_ = "Y";
				else key_ = "X"+i;
				i++;
				List<Double> list = new ArrayList<Double>();
				if(stsData.containsKey(key_)){
					list = stsData.get(key_);
				} else {
					stsData.put(key_, list);
				}
				list.add(Double.parseDouble(m.get(key).toString()));
			}
		}
		return stsData;
	}
	
	public Double[][] toDouble(List list) {
		return (Double[][]) list.toArray(new Double[list.size()][]);
	}
	
	public double[][] toPrimitive(List list){
		return toPrimitive(toDouble(list));
	};
	
	public double[][] toPrimitive(Double[][] d){
		double[][] ret = new double[d.length][];
        for (int i = 0; i < d.length; i++) {
        	ret[i] = ArrayUtils.toPrimitive(d[i]);
        }
		return ret;
	};
	
	//아파치 math에서 사용할 다중회구분석 데이타 만들기
	public double[][] toMultipleLinearRegressionData(int size,List<List<Double>> list){
		double[][] ret = new double[size][];		
		for(int i=0;i<size;i++){
			double[] d = new double[list.size()];
			ret[i] = d;
			int j = 0;
			for(List<Double> node: list){
				d[j++] = node.get(i).doubleValue();
			}
		}
		return ret;
	}
	
	
	
	//p-value 구하기
	public String getPValue(int nObs,double r){
		double r2 = r*r;
		TDistribution tDistribution = new TDistribution(nObs - 2);
		double t = Math.abs(r * FastMath.sqrt((nObs-2)/(1-r2)));		
		return df.format(2 * tDistribution.cumulativeProbability(-t));
	}
	
	public boolean isNumeric(String str)  
	{  
	  try  
	  { 		
	    double d = Double.parseDouble(str);
	  }  
	  catch(NumberFormatException nfe)  
	  {
	    return false;  
	  }  
	  return true;  
	}
	
	
	public void setDataSource(List data){
		this.data = data;
	}
	
	//다중회귀분석 P-value list구하기
	public Map<String,Double> getPValueMap() throws NotStrictlyPositiveException{
		double[][] data = getPearsonsCorrelationData();
		PearsonsCorrelation pc = new PearsonsCorrelation(data);		
		Set<String> set = stsData.keySet();
		Map<String,Double> m = new HashMap<String,Double>();		
		int i=1;
		for(String key : set){
			if(key.equals("Y")) continue;
			double v = pc.getCorrelationPValues().getEntry(0,i++);
			//if(Double.isNaN(v)) v = 0.0;			
			m.put(key,new Double(v));
		}
		return m;
	}
	
	//절편,기울기,x1,x2...
//	public Map<String,String> getResultMap(C0103VO vo) throws MathIllegalArgumentException {
//		
//		double[] Y = getRegressionY();		
//		//log.debug(">>>>>>>>>>>>>>>>>>>>>>>>>>");		
//		//log.debug(Arrays.toString(Y));
//		double[][] X = getRegressionX();
//		//log.debug(">>>>>>>>>>>>>>>>>>>>>>>>>>");
//		//log.debug(Arrays.deepToString(X));
//		OLSMultipleLinearRegression regression = new OLSMultipleLinearRegression();
//		regression.newSampleData(Y, X);
//		double[] beta = regression.estimateRegressionParameters();//절편
//		
//		double rSquared = regression.calculateRSquared();//regression.calculateAdjustedRSquared();//
//		Map<String,String> ret = new LinkedMap();
//		
//		String intercept = df_out.format(beta[0]);
//		
//		StringBuilder sb = new StringBuilder();
//		sb.append(intercept);
//		
//		ret.put("R2", df_out.format(rSquared));
//		ret.put("intercept", intercept);
//		Set<String> set = stsData.keySet();
//		List<String> effIdList = vo.getEffIdList();
//		
//		int i=1;
//		for(String key : set){
//			if(key.equals("Y")) continue;
//			String slope = df_out.format(beta[i]);
//			sb.append(" + ").append("(").append(slope).append(" * ").append(vo.getEffAlpabetMap().get(effIdList.get(i-1))).append(")");
//			ret.put(key,slope);
//			i++;
//		}
//		ret.put("equation", sb.toString());
//		return ret;
//	}
	
	//X축 데이타 만들기
	private double[][] getRegressionX(){
		if(stsData == null) throw new RuntimeException("data not exist");
		List<List<Double>> list = new ArrayList<List<Double>>();		
		Set<String> set = stsData.keySet();
		
		for(String key : set){
			if(key.equals("Y")) continue;
			List<Double> DList = this.getSTSDataSource(key);
			list.add(DList);
		}
		int size = list.get(0).size();		
		return toMultipleLinearRegressionData(size,list);	
	}
	
	//Y축 데이타 만들기
	private double[] getRegressionY(){
		List<Double> YList = this.getSTSDataSource("Y");
		Double[] YD = YList.toArray(new Double[YList.size()]);
		return ArrayUtils.toPrimitive(YD);
	}
	//PearsonsCorrelation 관련 데이타 만들기
	private double[][] getPearsonsCorrelationData(){
		if(stsData == null) throw new RuntimeException("data not exist");
		List<List<Double>> list = new ArrayList<List<Double>>();		
		Set<String> set = stsData.keySet();
		for(String key : set){
			List<Double> DList = this.getSTSDataSource(key);
			list.add(DList);
		}
		int size = list.get(0).size();
		return toMultipleLinearRegressionData(size,list);	
	}
	
	/**
	 * 중앙값
	 * @param a
	 * @return
	 */
	public double getMedian (){
		if(data == null) throw new RuntimeException("data not exist");
		int size = this.getSize();
        int middle = size/2;
 
        if (size % 2 == 1) {
            return data.get(middle);
        } else {
           return (data.get(middle-1) + data.get(middle)) / 2.0;
        }
    }
	
	
	/**
	 * 표준 오차
	 * @param option
	 * @return
	 */
	public double getSTDErr(double reliability) {	
		if(data == null) throw new RuntimeException("data not exist");		
	    return reliability * (this.getSTDEV(1)/Math.sqrt(this.getSize()));
	    //return 1.96*Math.sqrt( ((0.5)*(1-0.5) / (2000)) );
	}
	
	/**
	 * 표준 편차
	 * @param option
	 * @return
	 */
	public double getSTDEV(int option) {
		if(data == null) throw new RuntimeException("data not exist");
	    return Math.sqrt(this.getVar(option));
	}
	
	/**
	 * 분산
	 * @param data
	 * @return
	 */
	public double getVar(int option) {
		if(data == null) throw new RuntimeException("data not exist");
		int size = this.getSize();
		if (size < 1) return Double.NaN;
	    double sum = 0.0;
	    double meanValue = this.getAvg();
	    for(Double d : data){
	    	sum += Math.pow((d.doubleValue() - meanValue), 2);
		}
	    return (sum / (size - option));
	}
	
	/**
	 * 평균
	 * @return
	 */
	public double getAvg(){
		if(data == null) throw new RuntimeException("data not exist");
		double ret = 0;		
		int size = data.size();		
		ret = getSum()/size;		
		return ret;
	}
	
	//합계
	public double getSum(){
		if(data == null) throw new RuntimeException("data not exist");
		double sum = 0;		
		for(Double d : data){
			sum += d.doubleValue();
		}			
		return sum;
	}

	/**
	 * 데이타사이즈
	 * @return
	 */
	public int getSize() {
		if(data == null) throw new RuntimeException("data not exist");
		return data.size();
	}


	public void setSTSDataSource(Map<String,List<Double>> stsData) {

		this.stsData = stsData;
	}


	public List<Double> getSTSDataSource(String key) {		
		return this.stsData.get(key);
	}
}
