<!--
Author: W3layouts
Author URL: http://w3layouts.com
License: Creative Commons Attribution 3.0 Unported
License URL: http://creativecommons.org/licenses/by/3.0/
-->
<!DOCTYPE HTML>
<html>
<head>
<title>Synesica</title>
<meta name="viewport" content="width=device-width, initial-scale=1">
<meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
<meta name="keywords" content="Synesica, fragrance, perfume, parfum" />
<link href="css/bootstrap.css" rel='stylesheet' type='text/css' />
<!-- Custom Theme files -->
<link href="css/style.css" rel='stylesheet' type='text/css' />

    <!------IMPORTANT-->
    <link rel="stylesheet" href="css/selectize.bootstrap2.css">
	<script src="js/jquery-2.1.4.min.js"></script>
	<!--[if IE 8]><script src="js/es5.js"></script><![endif]-->
	<!--<script src="js/jquery.js"></script>-->
	<script src="js/jqueryui.js"></script>
	<script src="js/selectize.js"></script>
	<script src="js/index.js"></script>
	<script src="js/mainIndex.js"></script>
    <script type="text/javascript" src="js/bootstrap.js"></script>
    <!------//END IMPORTANT-->
</head>
<body>
<!-- banner -->
	<div class="banner">
		<div class="header">
			<div class="container">
				<div class="logo">
					<h1><a href="index.html">Synesica</a></h1>
				</div>
					<nav class="navbar navbar-default" role="navigation">
						<div class="navbar-header">
							<button type="button" class="navbar-toggle" data-toggle="collapse" data-target="#bs-example-navbar-collapse-1">
								<span class="sr-only">Toggle navigation</span>
								<span class="icon-bar"></span>
								<span class="icon-bar"></span>
								<span class="icon-bar"></span>
							</button>
						</div>
						<!--/.navbar-header-->
						<div class="collapse navbar-collapse" id="bs-example-navbar-collapse-1">
							<ul class="nav navbar-nav">
								<li class="active"><a href="index.html">Parfumuri preferate</a></li>
								<li><a href="rooms.html">Descopera parfumuri noi</a></li>
							</ul>
						</div>
						<!--/.navbar-collapse-->
					</nav>

					<div class="clearfix"> </div>


			</div>
		</div>
        <!------IMPORTANT-->
		<div class="banner-info">
			<div class="container" style="background-color:rgba(255, 255, 255, 0.75);">




							<div class="control-group col-md-10">
								<label for="input-tags" style="color:#553a99;">Alege parfumurile preferate:</label>
								<div class="btn-group" role="group" data-toggle="buttons">
									<button id="button-female" type="checkbox" class="btn btn-default" checked>pentru Ea</button>
									<button id="button-male" type="checkbox" class="btn btn-default">pentru El</button>
									<button id="button-unisex" type="checkbox" class="btn btn-default">pentru amândoi</button>
								</div>

								<input type="text" id="input-tags" class="input-tags demo-default" value="" placeholder="Alege ce parfumuri ți-au plăcut și îți recomandăm altele care ți se potrivesc ...">
							</div>






							<input id="recommendationButton" class="submit_button btn btn-default btn-lg" type="submit" value="Recomandă">


				</div>

		</div>
        <!------//END IMPORTANT-->
	</div>

	<!-- banner -->
	<!-- hod -->
	<div class="hod">
		<div class="container">
            <!------IMPORTANT-->
            <p>Pe baza preferințelor tale, îți recomandăm ...</p>
            <ul class="media-list" id="recommendationList">


            </ul>
            <!------//END IMPORTANT-->
			<div class="clearfix"></div>
		</div>
	</div>
	<!-- hod -->
	<!-- tels -->
	<!-- quick -->
<!-- footer -->
	<div class="footer">
		<div class="container">
			<div class="col-md-2 deco">
				<h4>Navigation</h4>
				<li><a href="index.html">Home</a></li>
				<li><a href="shortcodes.html">Short Codes </a></li>
				<li><a href="sigin.html">Sign in</a></li> 
				<li><a href="contact.html">Contact</a></li>
			</div>
			<div class="col-md-2 deco">
				<h4>Links</h4> 
				<li><a href="#">Qui Sommes-Nous ?</a></li>
				<li><a href="#">Mentions Légales </a></li>
				<li><a href="#">Conditions D'Utilisation </a></li>
			</div>
			<div class="col-md-2 deco">
				<h4>Social</h4>
				<div class="soci">
					<li><a href="#"><i class="f-1"> </i></a></li>
					<li><a href="#"><i class="t-1"> </i></a></li>
					<li><a href="#"><i class="g-1"> </i></a></li>
				</div>
			</div>
			<div class="col-md-3 cardss">
				<h4>Payment Sécure</h4>
				<li><i class="visa"></i></li>
				<li><i class="ma"></i></li>
				<li><i class="paypal"></i></li>
				<div class="clearfix"> </div>
			</div>
			<div class="col-md-3 pos">
			<h4>NewsLetter</h4>
					   <form method="post">
						 <input type="text" class="textbox" value="Email" onfocus="this.value = '';" onblur="if (this.value == '') {this.value = 'Email';}">
						 <div class="smt">
							<input type="submit" value="Subscribe">
						 </div>
					   </form>
			</div>
			<div class="clearfix"> </div>
		</div>
	</div>
	<div class="footer-bottom">
			<div class="container">
				<p>© 2015 Synesica. All Rights Reserved </p>
			</div>
		</div>
	<!-- footer -->
</body>
</html>