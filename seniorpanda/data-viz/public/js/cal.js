var calendar;
showView(BLOCKER_VIEW);
document.addEventListener("loggedIn", function makeCal(){
	calendar = new CalHeatMap();
	if(screen.width <= 500) {
		calendar.init({
			data:getData(1),
			//dataType: "json",
			itemSelector: "#calView",
			nextSelector: "#domainDynamicDimension-next",
			previousSelector: "#domainDynamicDimension-previous",
			start: new Date(new Date().getFullYear(), (new Date().getMonth() + 1)-3),
			minDate: new Date(new Date().getFullYear(), (new Date().getMonth() + 1)-6),
			maxDate: new Date(),
			range :3,
			id : "graph_c",
			domain: "month",
			subDomain: "x_day",
			subDomainTextFormat: "%a,%d",
			domainGutter: 10,
			highlight: ["now"],
			considerMissingDataAsZero : false,
			subDomainTitleFormat: {
				empty : "No data on {date}",
				filled : "Severity: {count} {connector} {date}"
			},
			onClick: function(date, nb) {
				$('#chngVal').show();
				$('#changeVal').val(nb);
				$("#onClick-placeholder").html("You just clicked <br/>on <b>" +
					date + "</b> <br/>with value <b>" +
					(nb === null ? "NULL" : nb) + "</b> "
				);
			},
			//subDomainDateFormat: function(date) {
			//	return moment(date).format("LL"); // Use the moment library to format the Date
			//},
			itemName: ["level", "level"],
			scale: [0,1,2,3,4,5],
			cellSize:48,
			cellPadding: 2,
			label: {
				position: "top"
			},
			legendCellSize: 50,
			displayLegend: false,
			legend: [0,1,2,3,4,5] 	// Custom threshold for the scale
		});
	} else {
		calendar.init({
			data:getData(1),
			//dataType: "json",
			itemSelector: "#calView",
			nextSelector: "#domainDynamicDimension-next",
			previousSelector: "#domainDynamicDimension-previous",
			start: new Date(new Date().getFullYear(), (new Date().getMonth() + 1)-3),
			minDate: new Date(new Date().getFullYear(), (new Date().getMonth() + 1)-6),
			maxDate: new Date(),
			range :3,
			id : "graph_c",
			domain: "month",
			subDomain: "x_day",
			subDomainTextFormat: "%a,%d",
			domainGutter: 20,
			highlight: ["now"],
			considerMissingDataAsZero : false,
			subDomainTitleFormat: {
				empty : "No data on {date}",
				filled : "Severity: {count} {connector} {date}"
			},
			onClick: function(date, nb) {
				$('#chngVal').show();
				$('#changeVal').val(nb);
				$("#onClick-placeholder").html("You just clicked <br/>on <b>" +
					date + "</b> <br/>with value <b>" +
					(nb === null ? "NULL" : nb) + "</b> "
			);
			},
			//subDomainDateFormat: function(date) {
			//	return moment(date).format("LL"); // Use the moment library to format the Date
			//},
			itemName: ["level", "level"],
			scale: [0,1,2,3,4,5],
			cellSize:48,
			cellPadding: 2,
			label: {
				position: "top"
			} ,
			legendCellSize: 50,
			displayLegend: false,
			legend: [0,1,2,3,4,5] 	// Custom threshold for the scale
		});
	}
	
	$('#cancelchngval').click(function() {
		$('#changeVal').val();
		$('#chngVal').hide();					
	});
}, false);