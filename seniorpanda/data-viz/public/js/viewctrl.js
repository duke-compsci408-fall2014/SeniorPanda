var NONE = 0;
var BLOCKER_VIEW = 1;
var LOGIN_VIEW = 2;
var PATIENT_VIEW = 3;
var DOCTOR_VIEW = 4;
var BLOCKER_PATIENT_VIEW = 5;
var BLOCKER_DOCTOR_VIEW = 6;

// Convenience method for showing parts of the page
function showView(which) {
	$("#logoutButton").show();
	//$("#select").hide();
	//$("#legend").hide();
	$("#sel_leg_bar").hide();
	$("#patientView").hide();
	$("#doctorView").hide();
	$("#loginView").hide();
	$("#shadow").removeClass('blocker');
	$("#loadingImage").hide();
	switch (which) {
		case NONE:
			break;
		case BLOCKER_VIEW:
			$('#shadow').addClass('blocker');
			$("#loadingImage").show();
			break;
		case LOGIN_VIEW:
			$("#logoutButton").hide();
			$("#loginView").show();
			break;
		case BLOCKER_PATIENT_VIEW:
			$('#shadow').addClass('blocker');
			$("#loadingImage").show();
		case PATIENT_VIEW:
			//$("#select").show();
			//$("#legend").show();
			$("#sel_leg_bar").show();
			$("#patientView").show();
			break;
		case BLOCKER_DOCTOR_VIEW:
			$('#shadow').addClass('blocker');
			$("#loadingImage").show();
		case DOCTOR_VIEW:
			$("#doctorView").show();
			break;
		default:
			break;
	}
};