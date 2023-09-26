<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="utf-8"%>
<!DOCTYPE html>
<html>

<head>

    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">

    <title>INSPINIA | 500 Error</title>

	<!-- CSS -->
	<jsp:include page="/common/inc/cmmn/inc_css.jsp" />

</head>

<body class="gray-bg">


    <div class="middle-box text-center animated fadeInDown">
        <h1>500</h1>
        <h3 class="font-bold">Internal Server Error</h3>

        <div class="error-desc">
            The server encountered something unexpected that didn't allow it to complete the request. We apologize.<br/>
            You can go back to main page: <br/><a href="/" class="btn btn-primary m-t">Dashboard</a>
        </div>
    </div>


	<!-- JS -->
	<jsp:include page="/common/inc/cmmn/inc_js.jsp" />

</body>
</html>