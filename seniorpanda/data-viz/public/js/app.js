var emailObj; // Logged in user
var bambooID; // User whose data to view

if (!document.createEvent) {
	var loggedIn = new Event('loggedIn');
} else {
	var loggedIn = document.createEvent("Event");
	loggedIn.initEvent("loggedIn", true, false);
}

// Bind buttons
$(document).ready(function() {
	$("#backToListButton").click(backToList);
	$("#backToListButton").hide();
	$("#logoutButton").click(logout);
	$("#loginButton").click(login);
	$("#email").on("keypress", function(e) {if (e.keyCode == 13) login();});
	$("#password").on("keypress", function(e) {if (e.keyCode == 13) login();});
});

// Check session once API is ready to go
document.addEventListener("apiReady", function () {
	showView(BLOCKER_VIEW);
	checkSession();
}, false);

function checkSession() {
	window.df.apis.user.getSession({"body": {}}, function (response) {
        // Existing session found, assign session token to be used for the session duration
        window.authorizations.add("X-DreamFactory-Session-Token", new ApiKeyAuthorization("X-Dreamfactory-Session-Token", response.session_id, 'header'));
		emailObj = response.email;
		bambooID = emailObj;
		document.dispatchEvent(loggedIn);
    }, function (response) {
		// No valid session, show login view to try to log in
        showView(LOGIN_VIEW);
    });
};

function login() {
	var body = {
		email: document.getElementById("email").value,
		password: document.getElementById("password").value
	};
	window.df.apis.user.login({body: body}, function (response) {
		//assign session token to be used for the session duration
		//document.getElementById("login-status").innerHTML = "Logged In"
		window.authorizations.add("X-DreamFactory-Session-Token", new ApiKeyAuthorization("X-Dreamfactory-Session-Token", response.session_id, 'header'));
		emailObj = response.email;
		bambooID = emailObj;
		document.getElementById("loginStatus").innerHTML = "";
		document.dispatchEvent(loggedIn);
	}, function(response){
		document.getElementById("loginStatus").innerHTML = getErrorString(response);
	});
};

function logout() {
	window.df.apis.user.logout({}, function(response){}, function(response){});
	$("#backToListButton").hide();
	$("#username").empty();
	calendar = calendar.destroy();
	document.getElementById("email").value = "";
	document.getElementById("password").value = "",
	showView(LOGIN_VIEW);
};

function getData(inpt){
	var type = inpt;
	var emailDomain = emailObj.split("@")[1];
	$("#username").empty().append(emailObj);
	showView(BLOCKER_VIEW);
	$(document).ready(
		function() {
			window.df.apis.db.getRecordsByFilter({table_name: "user_access", filter: "logged_in_user=\"" + emailObj + "\"", include_count: true}, function (response) {
				//Do something with the data;
				checkIfDoctor(response);			
			}, function(response) {
				document.getElementById("get-results").innerHTML = getErrorString(response);
			});
		}
	);
	function checkIfDoctor(json) {
		if (json.meta.count != 0) {
			$("#doctorView").empty();
			showView(BLOCKER_DOCTOR_VIEW);
			$("#doctorView").append("<p><p><div><span class='bigwhite_nh' ><u>My Patients: (click to view)</u></span><p/></div>");
			var access = json.record[0]; // We should only ever have one, if any, access row per logged in user
			if (access.patients && access.patients != "") {
				var patients = access.patients.split(";");
				for (var i in patients) {
					$("#doctorView").append("</p><div><a class='bigwhite' href='javascript:getPatientData(\"" + patients[i] + "\"," + inpt + ");'>" + patients[i] + "</a></div>");
				}
			}
			showView(DOCTOR_VIEW);
			return null;
		} else {
			return getSymptomData(bambooID, inpt);
		}	
	};				
};

// Convenience method for showing back button if we came from doctor view with list of patients
function getPatientData(id,inpt) {
	$("#backToListButton").show();
	getSymptomData(id,inpt);
};

// Go back to list of patients
function backToList() {
	showView(DOCTOR_VIEW);
	$("#backToListButton").hide();
};
				
function getSymptomData(id,inpt) {		
	bambooID = id;
	showView(BLOCKER_PATIENT_VIEW);
	$("#username").empty().append("<b>Logged In As: </b>").append(emailObj).append("<span style='padding-left: 20px;'><b>Showing Project ID: </b></span>"+id);
	$(document).ready(
		function() {
			window.df.apis.db.getRecordsByFilter({table_name: "ms_latest_symptoms", filter: "uid=\"" + bambooID + "\"", order: "date_created DESC"}, function (response) {
				//Do something with the data;
				useSymptomData(response.record);
			}, function(response) {
				document.getElementById("get-results").innerHTML = getErrorString(response);
			});
		}
	);
	function useSymptomData(json) {
		var arr = {};
		for (var key in json) { 
			if (json[key].symptoms != "") {
				var arrOfSymp = json[key].symptoms.split(",",inpt);
				var selSymp = arrOfSymp[inpt-1].split(":");
				var selSympLvl = parseInt(selSymp[1]);
				var createTime = json[key].time;
				arr[Math.round(createTime/(1000))] = selSympLvl;
			}
		}
		console.log(arr);
		calendar.update(arr);
		colorButton(inpt);
		showView(PATIENT_VIEW);
	}
}
		
function getStressFactorData(id,inpt) {
	bambooID = id;
	showView(BLOCKER_PATIENT_VIEW);
	$("#username").empty().append("<b>Logged In As: </b>").append(emailObj).append("<span style='padding-left: 20px;'><b>Showing Project ID: </b></span>"+id);
	$(document).ready(
		function() {
			window.df.apis.db.getRecordsByFilter({table_name: "ms_latest_stress_factors", filter: "uid=\"" + bambooID + "\"", order: "date_created DESC"}, function (response) {
				//Do something with the data;
				useStressFactorData(response.record);
			}, function(response) {
				document.getElementById("get-results").innerHTML = window.app.getErrorString(response);
			});
		}
	);
	function useStressFactorData(json) {
		var arr = {};
		for (var key in json) { 
			if (json[key].environment != "") {
				var arrOfStress = json[key].environment.split(",",inpt);
				var selStress = arrOfStress[inpt-5].split(":");
				var selStressLvl = parseInt(selStress[1]);
				var createTime = json[key].time;
				arr[Math.round(createTime/(1000))] = selStressLvl;
			}
		}
		console.log(arr);
		calendar.update(arr);
		colorButton(inpt);
		showView(PATIENT_VIEW);
	}
}

function colorButton(which) {
	document.getElementById("pain").style.backgroundColor = 'buttonface';
	document.getElementById("mobility").style.backgroundColor = 'buttonface';
	document.getElementById("fatigue").style.backgroundColor = 'buttonface';
	document.getElementById("brainfog").style.backgroundColor = 'buttonface';
	document.getElementById("family").style.backgroundColor = 'buttonface';
	document.getElementById("work").style.backgroundColor = 'buttonface';
	document.getElementById("sleep").style.backgroundColor = 'buttonface';
	document.getElementById("heat").style.backgroundColor = 'buttonface';
	switch (which) {
		case 1:
			document.getElementById("pain").style.backgroundColor = 'yellow';
			break;
		case 2:
			document.getElementById("mobility").style.backgroundColor = 'yellow';
			break;
		case 3:
			document.getElementById("fatigue").style.backgroundColor = 'yellow';
			break;
		case 4:
			document.getElementById("brainfog").style.backgroundColor = 'yellow';
			break;
		case 5:
			document.getElementById("family").style.backgroundColor = 'yellow';
			break;
		case 6:
			document.getElementById("work").style.backgroundColor = 'yellow';
			break;
		case 7:
			document.getElementById("sleep").style.backgroundColor = 'yellow';
			break;
		case 8:
			document.getElementById("heat").style.backgroundColor = 'yellow';
			break;
	}
};

function getErrorString(response){
    var msg = "An error occurred, but the server provided no additional information.";
    if (response.content && response.content.data && response.content.data.error) {
        msg = response.content.data.error[0].message;
    }
    msg = msg.replace(/&quot;/g, '"').replace(/&gt;/g, '>').replace(/&lt;/g, '<').replace(/&amp;/g, '&').replace(/&apos;/g, '\'');
    return msg;
};