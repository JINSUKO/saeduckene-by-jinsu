<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>

<!DOCTYPE html>
<html>
<head>

<meta charset="utf-8">
<meta http-equiv="X-UA-Compatible" content="IE=edge">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>인덱스를 만들어 보자</title>

<link
	href="${pageContext.request.contextPath }/resources/css/bootstrap.css"
	rel="stylesheet">
<script
	src="https://ajax.googleapis.com/ajax/libs/jquery/1.11.2/jquery.min.js"></script>
<!--개인 디자인 추가-->
<link href="${pageContext.request.contextPath}/resources/css/style.css"
	rel="stylesheet">
<script
	src="${pageContext.request.contextPath}/resources/js/bootstrap.js"></script>

<meta name="viewport"
	content="width=device-width, initial-scale=1, minimum-scale=1, maximum-scale=1" />
<link rel="stylesheet"
	href="${pageContext.request.contextPath}/resources/css/swiper-bundle.min.css" />

<script src="${pageContext.request.contextPath}/resources/js/swiper.js"
	defer></script>

<script src="https://cdn.jsdelivr.net/npm/swiper/swiper-bundle.min.js"
	defer></script>

<link
	href="${pageContext.request.contextPath }/resources/css/bootstrap.min.css"
	rel="stylesheet">
<script
	src="${pageContext.request.contextPath}/resources/js/bootstrap.min.js"
	defer></script>

</head>
<body>
	<header class="header">
		<%-- <div class="container-fluid">
			<div class="row">
				<div class="container clearfix">
					<div class="navbar">

						<div
							class="title collapse navbar-collapse nav navbar-nav navbar-left"
							id="titleNavbar">
							<img class="col-lg-12" src="${pageContext.request.contextPath }/resources/img/saeduckname.png" alt="세상 모든 덕질 세덕이네">
						</div>

						<a class="logo" href="#"> <img
							src="${pageContext.request.contextPath }/resources/img/saeduck.png"
							alt="Brand">
						</a>

						<div class="navbar-header">
							<!--data-toggle 같은 것들은 내부적으로 지원하는 반응형 API기능이므로 지우면 안됩니다-->
							<!-- <button type="button" class="navbar-toggle"
                        data-toggle="collapse" data-target="#titleNavbar">
                        asdfa
                     </button> -->
							<!-- data-toggle 같은 것들은 내부적으로 지원하는 반응형 API기능이므로 지우면 안됩니다  -->

							<!-- <button type="button" class="navbar-toggle"
                                 data-toggle="collapse" data-target="#myNavbar">ds
                              </button> -->

							<div class="navbar-collapse menu" id="myNavbar">
								<ul class="nav navbar-right">
									<li class="dropdown"><a class="dropdown-toggle"
										data-toggle="dropdown" href="#">MENU</a>
										<ul class="dropdown-menu">
											<c:choose>
												<c:when test="${login == null}">
													<li><a href="${pageContext.request.contextPath }/">Join</a></li>
													<li><a href="#">Login</a></li>
												</c:when>
												<c:otherwise>
													<li><a href="#">MyPage</a></li>
													<li><a href="#">Cart</a></li>
												</c:otherwise>
											</c:choose>
										</ul></li>
								</ul>
							</div>
						</div>


					</div>
				</div>
			</div>
		</div> --%>
		<nav class="navbar navbar-default navbar-fixed-top" role="navigation">
			<div class="container-fluid">
				<div class="nav navbar-na">
					<div class="navbar-header text-center" id="container-fluid-div">
						<a class="navbar-brand img-fluid" href="#">세상 모든 덕직<br>세덕이네</a>

						<img id="duck-logo" class="rounded" src="${pageContext.request.contextPath }/resources/img/logo.png" alt="세상 모든 덕질 세덕이네">

						<button class="navbar-toggle collapsed" type="button"
							data-toggle="collapse" data-target=".js-navbar-scrollspy" id="toggle-btn">
							<span class="icon-bar"></span> 
							<span class="icon-bar"></span> 
							<span class="icon-bar"></span>
						</button>
					</div>
					<div class="collapse navbar-collapse js-navbar-scrollspy" id="nav-ui-div">
						
						<ul class="nav navbar-nav" id="navbar-nav-right">
							<li class=""><a href="#">로그인</a></li>
							<li class=""><a href="#">회원가입</a></li>
						</ul>
					</div>
					
				</div>
				
				
			</div>
		</nav>
	</header>