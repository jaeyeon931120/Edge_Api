<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ page import="javax.servlet.jsp.PageContext"%>

<style>
.bg-light {background: #fff !important}
.fixedHeader {position: sticky;top: 0}
.footer-input {marin: 0 auto;padding: 15px;background-color: #000;position: sticky;bottom: 0}
/* ajax 로딩 */
.mw {	position: fixed;position: absolute;	top: 0;	left: 0;	width: 100%;height: 100%;z-index: 9000;}
.mw .bg {position: absolute;top: 0;left: 0;	width: 100%;height: 100%;background: #000;opacity: 0.6}
.jk_loading_display {display: none;}
.wrap-loading div {position: fixed;top: 50%;left: 50%;margin-left: -60px;margin-top: -20px}
#timeSelect {color: #000;width: 100%;border: 1px solid #484f5627a5d4;	border-radius: 5px;padding: 8px 10px;text-align: center;	font-size: 14px;}

.dash05_search ul li:first-child {padding: 0 20px !important;}
.release .th {	background: #f5f5f5;font-weight: bold;text-align: right}

.ui-corner-all{background: #fff!important}
.ui-state-highlight, .ui-widget-content .ui-state-highlight, .ui-widget-header .ui-state-highlight {
border: 1px solid #fd7e14!important; background: #fd7e14!important;color: #363636}
</style>