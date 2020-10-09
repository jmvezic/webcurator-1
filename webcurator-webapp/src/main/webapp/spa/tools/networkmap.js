class NetworkMap{
	constructor(){
		this.graph=new NetworkMapGraph('network-map-canvas');
		this.gridStatusCode=new NetworkMapGrid('#networkmap-side-table-group-by-status-code', 'statusCode');
		this.gridContentType=new NetworkMapGrid('#networkmap-side-table-group-by-content-type', 'contentType');
		this.data={};
		this.timerProgress;
	}


	init(jobId, harvestResultNumber){
		this.jobId=jobId;
		this.harvestResultNumber=harvestResultNumber;
		var reqUrl="/networkmap/get/common?job=" + jobId + "&harvestResultNumber=" + harvestResultNumber + "&key=keyGroupByDomain";
		var that=this;
    	fetchHttp(reqUrl, null, function(response){
    		console.log(response.rspCode + ': ' + response.rspMsg);
    		if (response.rspCode === 0 && response.payload !== null) {
    			var data=JSON.parse(response.payload);
    			that.data=data;
    			that.formatData(data);
    			that.initDraw(data);
    			return true;
    		}else if(response.rspCode === -1){
    			if (confirm("Index file is missing. Would you reindex the harvest result?")) {
    				that.reindex();
    			}
    		}
    	});
	}

	reindex(){
		var reqUrl="/visualization/index/initial?job=" + this.jobId + "&harvestResultNumber=" + this.harvestResultNumber;
		var that=this;
		fetchHttp(reqUrl, null, function(response){
			console.log(response.rspCode + ': ' + response.rspMsg);

			if (response.rspCode!==0) {
				return;
			}

			//show progress bar
			$('#popup-window-progress').show();

			//refresh progress
			that.timerProgress=setInterval(function(){
				that.refreshprogress();
			}, 3000);
		});
	}

	refreshprogress(){
		var reqUrl="/curator/visualization/progress?job=" + this.jobId + "&harvestResultNumber=" + this.harvestResultNumber;
		var that=this;
		fetch(reqUrl, { 
	    method: 'GET',
	    redirect: 'follow',
	    headers: {'Content-Type': 'application/json'},
	  }).then((response) => {
	  	console.log(response);
	    return response.json();
	  }).then((response) => {
    	console.log(response.rspCode + ': ' + response.rspMsg);

		var progressPercentage=0;
		if(response.rspCode===0){
			var responseProgressBar=JSON.parse(response.payload);
			progressPercentage=responseProgressBar.progressPercentage;

			$('#progressIndexerValue').html(progressPercentage);
			$('#progressIndexer').val(progressPercentage);
			$('#progressIndexer').attr('data-label', progressPercentage + '% Complete');
		}
		
		if((response.rspCode===0 && progressPercentage >= 100) || response.rspCode!=0){
			clearInterval(that.timerProgress);
			$('#popup-window-progress').hide();
			that.init(that.jobId, that.harvestResultNumber);
		}
	  });
	}

	initDraw(node){
		this.graph.draw(node.children);
        this.gridStatusCode.draw(node);
        this.gridContentType.draw(node);
	}

	formatData(node){
		if(!node){
			return;
		}
		this.data[node.id]=node;

		var children=node.children;
		for (var i = 0; i<children.length; i++) {
			this.formatData(children[i]);
		}
	}

	reset(){
		this.switchNode(0);
	}

	switchNode(nodeId){
		var node=this.data[nodeId];
		this._switchNode(node);
	}

	_switchNode(node){
		this.gridStatusCode.draw(node);
        this.gridContentType.draw(node);

		var title='Root';
		if(node.title){
			title=node.title;
		}

		if(title.length > 60){
			title=title.substr(0, 60) + '...';
		}

		$('#networkmap-side-title').text(title);
	}

	contextMenuCallback(key, condition, source){
		var keyItems=key.split('-');
		var action=keyItems[0], scope=keyItems[1];
		if(scope==='selected'){
			condition=source.getSelectedNodes();
		}

		gPopupModifyHarvest.checkUrls(condition, action);
	}

	static contextMenuItemsGrid={
        "prune-current": {"name": "Prune Current", icon: "far fa-trash-alt"},
		"prune-selected": {"name": "Prune Selected", icon: "fas fa-trash-alt"},
    	"sep1": "---------",
    	"inspect-current": {"name": "Inspect Current", icon: "far fa-eye"},
		"inspect-selected": {"name": "Inspect Selected", icon: "fas fa-eye"}
    };

    static contextMenuItemsGraph={
        "prune-current": {"name": "Prune", icon: "far fa-trash-alt"},
    	"sep1": "---------",
    	"inspect-current": {"name": "Inspect", icon: "far fa-eye"},
    };
}


