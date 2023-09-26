package egovframework.kevinlab.util;

import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFDataFormat;
import org.apache.poi.hssf.usermodel.HSSFFont;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.util.HSSFColor;


/**
 * Excel Upload Util
 * @author 권관호
 * @since  2017.08.29
 * @version 1.0
 * @see
 *
 *
 *  수정일     			수정자          	 수정내용
 *  -----------    	--------    ---------------------------
 *  2017.08.29  	권관호         	 최초 생성
 *
 */
/**
 * @author eco
 *
 */
public class ExcelUtil {

	private static String workType;
	/**
	* <pre>
	* 1. MethodName : excelDownload
	* 2. ClassName  : ExcelUtil.java
	* 3. Comment    : 데이터베이스에서 조회한 정보를 엑셀로 다운로드 한다.
	* 4. dataType 	: String, Number, Boolean, Date, Dollar
	* </pre>
	*
	* @param response
	* @param rowTitle
	* @param cellSize
	* @param dataList
	* @param dataName
	* @param dataType
	* @return void
	*/
	public static void excelDownload (HttpServletResponse response, String[] rowTitle, String[] cellSize, List<Map<String, Object>> dataList, String[] dataName, String[] dataType, String fileName) {

		OutputStream os = null;

		try {

			// Excel Write
			HSSFWorkbook workbook = new HSSFWorkbook();

			// Sheet 생성
			HSSFSheet sheet = workbook.createSheet("Sheet1"); // 시트 생성

			// Cell Size 설정
			for ( int i = 0 ; i < cellSize.length ; i ++ ) {
				sheet.setColumnWidth(i, (Integer.parseInt(cellSize[i]) * 100));
			}


			/* ########################### TITLE 설정 및 ROW 생성 START ########################### */

			// Font 설정
			HSSFFont titleFont = workbook.createFont(); // 폰트 객체 생성
			titleFont.setFontName(HSSFFont.FONT_ARIAL); // 글씨체 설정 (ARIAL)
			titleFont.setFontHeightInPoints((short) 12); //글씨 크기 설정
			titleFont.setBoldweight((short)HSSFFont.BOLDWEIGHT_BOLD); // 글씨 굵기 설정 ( bold )
			titleFont.setColor(HSSFColor.WHITE.index); // 타이틀 글자 색상 설정

			// 제목의 스타일 지정
			HSSFCellStyle titleStyle = workbook.createCellStyle(); // 스타일 객체 생성
			titleStyle.setFillForegroundColor(HSSFColor.DARK_TEAL.index); // 셀에 색상 적용
			titleStyle.setFillPattern(HSSFCellStyle.SOLID_FOREGROUND); // Background Color 없음
			titleStyle.setAlignment(HSSFCellStyle.ALIGN_CENTER); // 가운데 정렬
			titleStyle.setBottomBorderColor(HSSFColor.BLACK.index); // 셀 선 색 설정
			titleStyle.setBorderBottom(HSSFCellStyle.BORDER_THIN); // 셀 하단 선 긋기
			titleStyle.setBorderTop(HSSFCellStyle.BORDER_THIN); // 셀 상단 선 긋기
			titleStyle.setBorderLeft(HSSFCellStyle.BORDER_THIN); // 셀 왼쪽 선 긋기
			titleStyle.setBorderRight(HSSFCellStyle.BORDER_THIN); // 셀 오른쪽 선 긋기
			titleStyle.setFont(titleFont); // 폰트 적용

			// Title Row 생성
			HSSFRow titleRow = sheet.createRow((short)0);
			for( int i = 0 ; i < rowTitle.length ; i ++ ) {
				/* cell에 Title 맵핑 */
				HSSFCell cell = titleRow.createCell(i);
				cell.setCellStyle(titleStyle); // 셀 스타일 정의
				cell.setCellType(HSSFCell.CELL_TYPE_STRING); // 셀 타입 정의
				cell.setCellValue(rowTitle[i]); // 셀 내용 삽입
			}

			/* ########################### TITLE 설정 및 ROW 생성 END ########################### */


			/* ########################### CONTENT 설정 및 ROW 생성 START ########################### */

			// Font 설정
			HSSFFont contentFont = workbook.createFont(); // 폰트 객체 생성
			contentFont.setFontName(HSSFFont.FONT_ARIAL); // 글씨체 설정 (ARIAL)
			contentFont.setFontHeightInPoints((short) 10); //글씨 크기 설정
			contentFont.setBoldweight((short)HSSFFont.BOLDWEIGHT_NORMAL); // 글씨 굵기 설정 ( normal )


			// 날짜 포맷 스타일 지정
			HSSFCellStyle dateStyle = workbook.createCellStyle(); // 셀 스타일 객체 생성
			dateStyle.setAlignment(HSSFCellStyle.ALIGN_CENTER); // 가운데 정렬
			dateStyle.setFont(contentFont); // 폰트 적용
			dateStyle.setDataFormat(HSSFDataFormat.getBuiltinFormat("m/d/yy h:mm")); // 날짜포맷으로 변경
			dateStyle.setBottomBorderColor(HSSFColor.BLACK.index); // 셀 선 색 설정
			dateStyle.setBorderBottom(HSSFCellStyle.BORDER_THIN); // 셀 하단 선 긋기
			dateStyle.setBorderTop(HSSFCellStyle.BORDER_THIN); // 셀 상단 선 긋기
			dateStyle.setBorderLeft(HSSFCellStyle.BORDER_THIN); // 셀 왼쪽 선 긋기
			dateStyle.setBorderRight(HSSFCellStyle.BORDER_THIN); // 셀 오른쪽 선 긋기

			// dollar 포맷 스타일 지정 ex) 20,000
			HSSFCellStyle dollarStyle = workbook.createCellStyle(); // 셀 스타일 객체 생성
			dollarStyle.setAlignment(HSSFCellStyle.ALIGN_CENTER); // 가운데 정렬
			dollarStyle.setFont(contentFont); // 폰트 적용
			dollarStyle.setDataFormat(HSSFDataFormat.getBuiltinFormat("#,##0")); // dollar 포맷으로 변경
			dollarStyle.setBottomBorderColor(HSSFColor.BLACK.index); // 셀 선 색 설정
			dollarStyle.setBorderBottom(HSSFCellStyle.BORDER_THIN); // 셀 하단 선 긋기
			dollarStyle.setBorderTop(HSSFCellStyle.BORDER_THIN); // 셀 상단 선 긋기
			dollarStyle.setBorderLeft(HSSFCellStyle.BORDER_THIN); // 셀 왼쪽 선 긋기
			dollarStyle.setBorderRight(HSSFCellStyle.BORDER_THIN); // 셀 오른쪽 선 긋기

			// 기본스타일 지정
			HSSFCellStyle contentStyle = workbook.createCellStyle(); // 셀 스타일 객체 생성
			contentStyle.setAlignment(HSSFCellStyle.ALIGN_CENTER); // 가운데 정렬
			contentStyle.setFont(contentFont); // 폰트 적용
			contentStyle.setBottomBorderColor(HSSFColor.BLACK.index); // 셀 선 색 설정
			contentStyle.setBorderBottom(HSSFCellStyle.BORDER_THIN); // 셀 하단 선 긋기
			contentStyle.setBorderTop(HSSFCellStyle.BORDER_THIN); // 셀 상단 선 긋기
			contentStyle.setBorderLeft(HSSFCellStyle.BORDER_THIN); // 셀 왼쪽 선 긋기
			contentStyle.setBorderRight(HSSFCellStyle.BORDER_THIN); // 셀 오른쪽 선 긋기

			// Cell에 내용 맵핑
			for( int i = 0 ; i < dataList.size() ; i ++ ) {

				Map<String, Object> data = dataList.get(i);

				// Content Row 생성
				HSSFRow contentRow = sheet.createRow(i+1);

				for( int j = 0 ; j < dataName.length ; j ++ ){

					/* cell에 Content 맵핑 */
					HSSFCell cell = contentRow.createCell(j);

					// data Type에 맞추어 형변환을 하고, Cell type을 정의한다.
					if ("String".equals(CmmnUtil.getString(dataType[j], ""))) { // 문자형

						cell.setCellStyle(contentStyle); // 셀 스타일 정의
						cell.setCellType(HSSFCell.CELL_TYPE_STRING); // 셀 타입 정의
						cell.setCellValue(CmmnUtil.getString(data.get(dataName[j]), "")); // 셀 내용 삽입

					} else if ("Number".equals(CmmnUtil.getString(dataType[j], ""))) { // 숫자형

						cell.setCellStyle(contentStyle); // 셀 스타일 정의
						cell.setCellType(HSSFCell.CELL_TYPE_NUMERIC); // 셀 타입 정의
						cell.setCellValue(Integer.parseInt(CmmnUtil.getString(data.get(dataName[j]), "0"))); //셀 내용 삽입

					} else if ("Boolean".equals(CmmnUtil.getString(dataType[j], ""))) { // boolean 형

						cell.setCellStyle(contentStyle); // 셀 스타일 정의
						cell.setCellType(HSSFCell.CELL_TYPE_BOOLEAN); // 셀 타입 정의
						cell.setCellValue("true".equals(CmmnUtil.getString(data.get(dataName[j]), "false"))); // 셀 내용 삽입

					} else if ("Date".equals(CmmnUtil.getString(dataType[j], ""))) { // 날짜형

						SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss", Locale.KOREA); // 날짜 형식 지정

						Date date = sdf.parse(CmmnUtil.getString(data.get(dataName[j]), "20140127000000")); // 날짜 객체 생성
						cell.setCellStyle(dateStyle); // 셀 스타일 정의
						cell.setCellValue(date); // 셀 내용 삽입

					} else if ("Dollar".equals(CmmnUtil.getString(dataType[j], ""))){ // 화폐형

						cell.setCellStyle(dollarStyle); // 셀 스타일 정의
						cell.setCellType(HSSFCell.CELL_TYPE_NUMERIC); // 셀 타입 정의
						cell.setCellValue(Long.parseLong(CmmnUtil.getString(data.get(dataName[j]), "0"))); // 셀 내용 삽입

					} else { // 문자형으로 정의

						cell.setCellStyle(contentStyle); // 셀 스타일 정의
						cell.setCellType(HSSFCell.CELL_TYPE_STRING); // 셀 타입 정의
						cell.setCellValue(CmmnUtil.getString(data.get(dataName[j]), "")); // 셀 내용 삽입
					}
				}
			}

			// ContetnType, Header 정보 설정
			System.out.println("fileName:::::::::::::::::"+fileName);
	        response.setHeader("Content-type", "application/vnd.ms-excel; charset=EUC-KR");
	        response.setHeader("Content-Disposition", "attachment;filename=" + new String((fileName).getBytes("KSC5601"),"8859_1")+".xls");
	        response.setHeader("Content-Description", "JSP Generated Data");
	        response.setContentType("application/vnd.ms-excel;charset=EUC-KR");
	        response.setCharacterEncoding("EUC-KR");


	        os = response.getOutputStream();
	        System.err.println(response.getCharacterEncoding());

	        // Excel File 다운로드
			workbook.write(os);


		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if(os != null){
				try{
					os.close();
				} catch (IOException ex){
					//ignore
				}
			}
		}
	}

//
//
//	/**
//	 * 엑셀 업로드 함수
//	 * @param b03vo
//	 * @return
//	 * @throws Exception
//	 */
//	public static List<B03VO> excelReader(B03VO b03vo) throws Exception{
//
//		XSSFWorkbook myWorkBook = null;
//		FileInputStream fis = null;
//		File myFile = null;
//		List<B03VO> excelList = new ArrayList<B03VO>();
//
///*		int fileCnt = 0;
//		boolean cntType = false;
//*/
//		try{
//			System.out.println(b03vo.getFilePath()+ "\\" +b03vo.getFileName());
//			myFile = new File(b03vo.getFilePath()+ "\\" +b03vo.getFileName());
//		    fis = new FileInputStream(myFile);
//
//		    myWorkBook = new XSSFWorkbook (fis);
//		    String sheetDate = b03vo.getSearchStartDate().substring(8);
//		    String inputDate = b03vo.getSearchStartDate();
//
//		    XSSFSheet mySheet = myWorkBook.getSheet(sheetDate);
//		    Iterator<Row> rowIterator = mySheet.iterator();
//
//
//		    /*if("생산".equals( b03vo.getFileName().substring(2, 4) ) ) {
//		    	while (rowIterator.hasNext()) {
//		    		if(cntType) {
//						break;
//					}
//			        Row row = rowIterator.next();
//			        Iterator<Cell> cellIterator = row.cellIterator();
//			        if(row.getRowNum() > 5) {
//				        while (cellIterator.hasNext()) {
//				        	Cell cell = cellIterator.next();
//				        	if(cell.getColumnIndex() == 1) {
//				        		//System.out.println("################ 전체 getNumericCellValue : "+cell.getNumericCellValue());
//				        		if( Cell.CELL_TYPE_NUMERIC == 0 ) {
//				        			fileCnt++;
//				        			System.out.println("################ 생산 fileCnt : "+fileCnt);
//				        		}else{
//				        			cntType = false;
//				        			break;
//				        		}
//				        	}
//				        }
//			        }
//			    }
//		    }else if("중자".equals( b03vo.getFileName().substring(2, 4) ) ) {
//		    	while (rowIterator.hasNext()) {
//					if(cntType) {
//						break;
//					}
//			        Row row = rowIterator.next();
//			        Iterator<Cell> cellIterator = row.cellIterator();
//			        if(row.getRowNum() > 3) {
//				        while (cellIterator.hasNext()) {
//				        	Cell cell = cellIterator.next();
//				        	if(cell.getColumnIndex() == 1) {
//				        		//System.out.println("################ 중자 getNumericCellValue : "+cell.getNumericCellValue());
//				        		if( Cell.CELL_TYPE_NUMERIC == 0 ) {
//				        			fileCnt++;
//				        		}else{
//				        			cntType = true;
//				        			break;
//				        		}
//				        	}
//				        }
//			        }
//			    }
//		    }*/
//
//		    if("생산".equals( b03vo.getFileName().substring(2, 4) ) ) {
//		    	while (rowIterator.hasNext()) {
//			        Row row = rowIterator.next();
//
//			        if(row.getRowNum() > 5 && row.getRowNum() < 32) {
//			        Iterator<Cell> cellIterator = row.cellIterator();
//			        B03VO b03Vo = new B03VO();
//			        while (cellIterator.hasNext()) {
//
//			            Cell cell = cellIterator.next();
//
//			            	switch ( cell.getColumnIndex() ) {
//				            case 3:
//				            	if( Cell.CELL_TYPE_STRING == 1 ) {
//				            		System.out.print(cell.getStringCellValue() + "\t");
//				            		b03Vo.setsPrdtNm(cell.getStringCellValue());
//				            	}
//				                break;
//				            case 6:
//				            	if( Cell.CELL_TYPE_NUMERIC == 0 ) {
//				            		System.out.print(cell.getNumericCellValue() + "\t");
//				            		b03Vo.setnMold( cell.getNumericCellValue() );
//				            	}
//				                break;
//				            case 7:
//				            	if( Cell.CELL_TYPE_NUMERIC == 0 ) {
//				            		System.out.print(cell.getNumericCellValue() + "\t");
//				            		b03Vo.setnEa(cell.getNumericCellValue());
//				            	}
//				                break;
//				            case 8:
//				            	if( Cell.CELL_TYPE_NUMERIC == 0 ) {
//				            		System.out.print(cell.getNumericCellValue() + "\t");
//				            		b03Vo.setnFaultyEa(cell.getNumericCellValue());
//				            	}
//				                break;
//				            case 9:
//				            	if( Cell.CELL_TYPE_NUMERIC == 0 ) {
//				            		System.out.print(cell.getNumericCellValue() + "\t");
//				            		b03Vo.setnFaultyKg(cell.getNumericCellValue());
//				            	}
//				                break;
//				            case 10:
//				            	if( Cell.CELL_TYPE_NUMERIC == 0 ) {
//				            		System.out.print(cell.getNumericCellValue() + "\t");
//				            		b03Vo.setnGoodEa(cell.getNumericCellValue());
//				            	}
//				                break;
//				            case 11:
//				            	if( Cell.CELL_TYPE_NUMERIC == 0 ) {
//				            		System.out.print(cell.getNumericCellValue() + "\t");
//				            		b03Vo.setnFaultyRate(cell.getNumericCellValue());
//				            	}
//				                break;
//				            case 13:
//				            	if( Cell.CELL_TYPE_NUMERIC == 0 ) {
//				            		System.out.print(cell.getNumericCellValue() + "\t");
//				            		b03Vo.setnPlanKg(cell.getNumericCellValue());
//				            	}
//				                break;
//				            case 14:
//				            	if( Cell.CELL_TYPE_NUMERIC == 0 ) {
//				            		System.out.print(cell.getNumericCellValue() + "\t");
//				            		b03Vo.setnKg(cell.getNumericCellValue());
//				            	}
//				                break;
//				            case 15:
//				            	if( Cell.CELL_TYPE_NUMERIC == 0 ) {
//				            		System.out.print(cell.getNumericCellValue() + "\t");
//				            		b03Vo.setnGoodKg(cell.getNumericCellValue());
//				            	}
//				                break;
//				            default :
//
//				            }
//			            	b03Vo.setcDate(inputDate);
//			            	b03Vo.setsWorkTime("10");
//			            	b03Vo.setsType("전체");
//			            }
//			        excelList.add(b03Vo);
//			        }
//			    }
//		    }else if("중자".equals(b03vo.getFileName().substring(2, 4) )){
//
//		    	while (rowIterator.hasNext()) {
//			        Row row = rowIterator.next();
//
//			        if( (row.getRowNum() > 3 && row.getRowNum() < 20) || (row.getRowNum() > 22 && row.getRowNum() < 39)) {
//				        Iterator<Cell> cellIterator = row.cellIterator();
//				        B03VO b03Vo = new B03VO();
//				        if(row.getRowNum() > 3 && row.getRowNum() < 20) {
//				        	workType = "10";
//				        }else {
//				        	workType = "11";
//				        }
//
//				        while (cellIterator.hasNext()) {
//
//			            Cell cell = cellIterator.next();
//
//			            	switch ( cell.getColumnIndex() ) {
//				            case 2:
//				            	if( Cell.CELL_TYPE_STRING == 1 ) {
//				            		System.out.print(cell.getStringCellValue() + "\t");
//				            		b03Vo.setsPrdtNm(cell.getStringCellValue());
//				            	}
//				                break;
//				            case 4:
//				            	if( Cell.CELL_TYPE_NUMERIC == 0 ) {
//				            		System.out.print(cell.getNumericCellValue() + "\t");
//				            		b03Vo.setnMold( cell.getNumericCellValue() );
//				            	}
//				                break;
//				            case 5:
//				            	if( Cell.CELL_TYPE_NUMERIC == 0 ) {
//				            		System.out.print(cell.getNumericCellValue() + "\t");
//				            		b03Vo.setnEa(cell.getNumericCellValue());
//				            	}
//				                break;
//				            case 6:
//				            	if( Cell.CELL_TYPE_NUMERIC == 0 ) {
//				            		System.out.print(cell.getNumericCellValue() + "\t");
//				            		b03Vo.setnFaultyEa(cell.getNumericCellValue());
//				            	}
//				                break;
//				            case 7:
//				            	if( Cell.CELL_TYPE_NUMERIC == 0 ) {
//				            		System.out.print(cell.getNumericCellValue() + "\t");
//				            		b03Vo.setnGoodEa(cell.getNumericCellValue());
//				            	}
//				                break;
//				            case 8:
//				            	if( Cell.CELL_TYPE_NUMERIC == 0 ) {
//				            		System.out.print(cell.getNumericCellValue() + "\t");
//
//				            		b03Vo.setnKg(cell.getNumericCellValue());
//				            	}
//				                break;
//				            case 9:
//				            	if( Cell.CELL_TYPE_NUMERIC == 0 ) {
//				            		System.out.print(cell.getNumericCellValue() + "\t");
//				            		b03Vo.setnFaultyKg(cell.getNumericCellValue());
//				            	}
//				                break;
//				            case 10:
//				            	if( Cell.CELL_TYPE_NUMERIC == 0 ) {
//				            		System.out.print(cell.getNumericCellValue() + "\t");
//				            		b03Vo.setnGoodKg(cell.getNumericCellValue());
//				            	}
//				                break;
//				            case 11:
//				            	if( Cell.CELL_TYPE_NUMERIC == 0 ) {
//				            		System.out.print(cell.getNumericCellValue() + "\t");
//				            		b03Vo.setnFaultyRate(cell.getNumericCellValue());
//				            	}
//				                break;
//				            default :
//
//				            }
//			            	b03Vo.setcDate(inputDate);
//			            	b03Vo.setsWorkTime(workType);
//			            	b03Vo.setsType("중자");
//			            }
//				        excelList.add(b03Vo);
//			        }
//			    }
//		    }
//
//		}catch(Exception ex){
//			System.out.println("############ Exception : "+ex.toString());
//		}finally{
//			myWorkBook.close();
//			fis.close();
//			myFile.delete();
//		}
//		return excelList;
//	}

}