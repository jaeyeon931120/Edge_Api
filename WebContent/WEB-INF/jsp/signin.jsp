<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="javax.servlet.jsp.PageContext" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">

    <title>INSPINIA | Login MAIN</title>
	<!-- CSS -->
	<jsp:include page="/common/inc/cmmn/inc_css.jsp" />
	<script src="http://code.jquery.com/jquery-1.11.1.min.js"></script>
	<script src="http://code.jquery.com/jquery-migrate-1.2.1.min.js"></script>
</head>
<body class="gray-bg">

    <div class="loginColumns animated fadeInDown">
        <div class="row">

            <div class="col-md-6">
                <h2 class="font-bold">이주희 바보  Welcome to IN+</h2>

                <p>
                    Perfectly designed and precisely prepared admin theme with over 50 pages with extra new web app views.
                </p>

                <p>
                    Lorem Ipsum is simply dummy text of the printing and typesetting industry. Lorem Ipsum has been the industry's standard dummy text ever since the 1500s.
                </p>

                <p>
                    When an unknown printer took a galley of type and scrambled it to make a type specimen book.
                </p>

                <p>
                    <small>It has survived not only five centuries, but also the leap into electronic typesetting, remaining essentially unchanged.</small>
                </p>

            </div>
            <div class="col-md-6">
                <div class="ibox-content">                    
                </div>
            </div>
        </div>
        <hr/>
        <!-- 등록 Modal s -->
            <div class="modal fade" id="cretModal" role="dialog">
              <div class="modal-dialog modal-lg">
                <!-- Modal content-->
                <div class="modal-content">
                  <div class="modal-header">
                    <button type="button" class="close" data-dismiss="modal">&times;</button>
                    <h4 class="modal-title">계정생성</h4>
                  </div>
                  <div class="modal-body">
                    <form>
                        <div class="form-group">
                          <label for="showEasing">신규사용자 계정 생성은 관리자에게 문의해주시기바랍니다.(000-000-0000)</label>
                        </div>
                    </form>

                  </div>
                  <div class="modal-footer">
                    <button type="button" class="btn btn-default" data-dismiss="modal">Close</button>
                  </div>
                </div>

              </div>
            </div>
        <!-- 등록 Modal e -->
        <div class="row">
            <div class="col-md-6">
                Copyright Kevin LAB
            </div>
            <div class="col-md-6 text-right">
               <small>Since 2021</small>
            </div>
        </div>
    </div>
    <!-- JS -->
    <jsp:include page="/common/inc/cmmn/inc_js.jsp" />
	<script>
    $(document).ready(function() {
        $( '#create' ).click(function( event ){
             $('#cretModal').modal();
        });

    });
	function get_msg(message) {
	     var move = '90px';
	     jQuery('#message').text(message);
	     jQuery('#message').animate({
	          left : '+=' + move
	     }, 'slow', function() {
	          jQuery('#message').delay(500).animate({ left : '-=' + move }, 'slow');
	     });
	}

	<c:if test="${error == 'true'}">
	jQuery(function() {
	     get_msg("로그인 실패하였습니다.");
	});

	</c:if>

	function signin() {
	     $.ajax({
	          url : '${pageContext.request.contextPath}/signinProcess',
	          data: $('form input').serialize(),
	          type: 'POST',
	          dataType : 'json',
	          beforeSend: function(xhr) {
	               xhr.setRequestHeader("Accept", "application/json");
	          }
	     }).done(function(body) {

	          var message = body.response.message;
	          var error = body.response.error;
	          if (error) get_msg(message);
	          if (error == false) {
	               var url = '${referer}';
	               if (url == '') url = '<c:url value="${pageContext.request.contextPath}/m/m01" />';
	               location.href = url;
	          }

	     });

	}
	</script>
</body>
</html>