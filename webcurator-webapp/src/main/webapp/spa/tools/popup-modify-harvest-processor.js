class ModifyHarvestProcessor{
	constructor(jobId, harvestResultId, harvestResultNumber){
		this.jobId=jobId;
		this.harvestResultId=harvestResultId;
		this.harvestResultNumber=harvestResultNumber;
		this.currentEdittingNode=null;
		this.chunk_size = 1*1024*1024; // 1Mbyte Chunk
		this.offset = 0;
	}

	uploadFile(cmd, file, callback){
		// $('#popup-window-loading').show();
		this.offset = 0;
		this.recurseUploadFile(cmd, file, callback);
	}

	recurseUploadFile(cmd, file, callback){
		if (this.offset >= file.size) {
			return;
		}

		var blob=file.slice(this.offset, this.offset + this.chunk_size);
		this.offset+=blob.size;

		var that=this;
		var reader = new FileReader();
		reader.addEventListener("loadend", function () {
			var req={
				content: reader.result,
				metadata: {
					name: file.name,
					length: file.size,
					contentType: file.type,
					lastModified: file.lastModified
				},
				start: (that.offset <= that.chunk_size)
			};

			var url="/modification/upload-file-stream?job=" + that.jobId + "&harvestResultNumber=" + that.harvestResultNumber;
			fetchHttp(url, req, function(response){
				if (that.offset>=file.size) {
					callback(cmd, response);
				}else{
					that.recurseUploadFile(cmd, file, callback);
					return true;
				}
			}, true);
		});

		reader.readAsDataURL(blob);
		// reader.readAsArrayBuffer(file);
	}

	singleImportCallback(resp, node){
		//Check result
		if(resp && resp.respCode!=1){
			alert(resp.respMsg);
			return;
		}

		node.uploadedFlag=1;
		
		$('#tab-btn-import').trigger('click');
		if(this.tobeReplaceNode){
			gPopupModifyHarvest.gridImport.gridOptions.api.updateRowData({remove: [this.tobeReplaceNode]});
		}
		gPopupModifyHarvest.gridImport.insert([node]);
		$('#popup-window-single-import').hide();

		if(node.pruneFlag){
			gPopupModifyHarvest.pruneHarvestByUrls([node]);
		}
	}

	singleImport(){
		var singleImportMode=$('#single-import-mode').html().toUpperCase();
		if (singleImportMode==='NEW') {
			this.singleImportNew();
		}else{
			this.singleImportEdit();
		}
	}

	singleImportNew(){
		var that=this;
		var node={
			url: $("#specifyTargetUrlInput").val(),
		};

		// Check if targetURL exist in "to be imported" table
		this.tobeReplaceNode=null;
		gPopupModifyHarvest.gridImport.gridOptions.api.forEachNode(function(row, index){
			if(node.url===row.data.url){
				that.tobeReplaceNode=row.data;
			}
		});
		if (this.tobeReplaceNode) {
			var decision=confirm("The targetUrl has been exist in the ToBeImported table. \n Would you replace it?");
			if(!decision){
				return;
			}
		}
		node.pruneFlag=$("#checkbox-prune-of-single-import").is(":checked");
		node.option=$("#popup-window-single-import input[name='customRadio']:checked").attr("flag");
		if(node.option==='File'){
			if(!node.url.toLowerCase().startsWith("http://")){
				alert("You must specify a valid target URL. Starts with: http://");
				return;
			}
			var file=$('#sourceFile')[0].files[0];
			if(!file){
				alert("You must specify a source file name to import.");
				return;
			}
			node.name=file.name;
			node.length=file.size;
			node.contentType=file.type;

			var modifiedMode=$("#radio-group-modified-date input[name='r1']:checked").attr("flag");
			node.modifiedMode=modifiedMode;
			if (modifiedMode.toUpperCase() === 'TBC') {
				node.lastModified=0;
			}else if (modifiedMode.toUpperCase() === 'FILE') {
				node.lastModified=file.lastModified;
			}else if (modifiedMode.toUpperCase() === 'CUSTOM') {
				var customModifiedDate=moment($("#datetime-local-customizard").val()).valueOf();
				node.lastModified=customModifiedDate;
			}else{
				alert('Invalid modified mode: ' + modifiedMode);
				return;
			}

			var that=this;
			this.uploadFile(node, file, function(cmd, resp){
				that.singleImportCallback(resp, cmd);
			});
		}else{
			if(!node.url.toLowerCase().startsWith("http://") &&
				!node.url.toLowerCase().startsWith("https://")){
				alert("You must specify a valid target URL.");
				return;
			}

			node.name='--';
			node.modifiedMode='TBC';
			node.lastModified=0;

			this.singleImportCallback(null, node);
		}
	}

	singleImportEdit(){
		//Only supporting lastModifiedDate
		var modifiedMode=$("#radio-group-modified-date input[type='radio']:checked").attr("flag");
		this.currentEdittingNode.modifiedMode=modifiedMode;
		if (modifiedMode.toUpperCase() === 'TBC') {
			this.currentEdittingNode.lastModified=0;
		}else if (modifiedMode.toUpperCase() === 'FILE') {
			this.currentEdittingNode.lastModified=file.lastModified;
		}else if (modifiedMode.toUpperCase() === 'CUSTOM') {
			var customModifiedDate=moment($("#datetime-local-customizard").val()).valueOf();
			this.currentEdittingNode.lastModified=customModifiedDate;
		}else{
			alert('Invalid modified mode: ' + modifiedMode);
			return;
		}

		$('#popup-window-single-import').hide();

		gPopupModifyHarvest.gridImport.gridOptions.api.redrawRows(true);
	}
	

	bulkUploadFiles(){
		var dataset=gPopupModifyHarvest.gridImportPrepare.getAllNodes();
		var bulkFileNameMap={};
		for(var j=0; j<dataset.length; j++){
			var node=dataset[j];
			if(node.option.toLowerCase()!=='file' || node.respCode===1){
				continue;
			}
			var ary=bulkFileNameMap[node.name];
			if(!ary){
				ary=[node];
				bulkFileNameMap[node.name]=ary;
			}else{
				ary.push(node);
			}
		}

		var that=this;
		var files=$('#bulkImportContentFile')[0].files;
		for(var i=0; i<files.length; i++){
			var file=files[i];
			var node={name: file.name};

			var ary=bulkFileNameMap[node.name];
			if(!ary){
				console.log("Selected file not match any item to be bulk imported. Selected file name: " + cmd.srcName);
				continue;
			}

			for(var j=0; j<ary.length; j++){
				ary[j].length=file.size;
				ary[j].contentType=file.type;
				ary[j].lastModified=file.lastModified;
				ary[j].uploadedFlag=0;
			}

			this.uploadFile(node, file, function(cmd, response){
				var ary=bulkFileNameMap[cmd.name];
				if(!ary){
					console.log("System error. Selected file name: " + cmd.name);
					return;
				}

				for(var j=0; j<ary.length; j++){
					ary[j].respCode=response.respCode;
					ary[j].respMsg=response.respMsg;
					ary[j].uploadedFlag=1;
				}
				gPopupModifyHarvest.gridImportPrepare.gridOptions.api.redrawRows(true);

				delete bulkFileNameMap[cmd.name];
				var unUploadedNumber=0;
				for(var key in bulkFileNameMap){
					unUploadedNumber++;
				}
				if(unUploadedNumber>0){
					var html=$('#tip-bulk-import-prepare-invalid').html();
					$('#tip-bulk-import-prepare').html(html);
				}else{
					$('#tip-bulk-import-prepare').html('All rows are valid.');
				}
			});
		}

		gPopupModifyHarvest.gridImportPrepare.gridOptions.api.redrawRows(true);
		$('#bulkImportContentFile').val(null);
	}

	_bulkValidateImportMetaData(dataset){
		var map={};

		for(var i=0;i<dataset.length;i++){
			var node=dataset[i];

			var target=node.target;
			if(map[target]){
				alert("Duplicated target URL at line: " + (i+1) + " and " + map[target]);
				return;
			}else{
				map[target]=i;
			}

			var option=node.option;
			var modifiedMode=node.modifiedMode;
			var lastModifiedDate=node.lastModifiedDate;

			if(option==="FILE"){
				if(!target.toLowerCase().startsWith("http://")){
					alert("You must specify a valid target URL at line:" + (i+1) + ". URL starts with: http://");
					return;
				}

				if(modifiedMode==='TBC' || modifiedMode==='FILE'){
					node.lastModified=0;
				}else if (modifiedMode==='CUSTOM') {
					var dt=moment(lastModifiedDate);
					if(!dt){
						alert("Invalid modification datetime at line: " + (i+1));
						return;
					}

					node.lastModified=dt.valueOf();
				}else{
					alert("Invalid modification mode or datetime at line: " + (i+1));
					return;
				}
			}else if(option==='PRUNE' || option==='RECRAWL'){
				node.modifiedMode='TBC';
				node.lastModifiedDate=0;
				if(!target.toLowerCase().startsWith("http://") &&
					!target.toLowerCase().startsWith("https://")){
					alert("You must specify a valid target URL at line:" + (i+1));
					return;
				}
			}else{
				delete dataset[i];
				console.log('Skip invalid line: ' + (i+1));
			}
		}

		var gridImportNodes=gPopupModifyHarvest.gridToBeModified.getAllNodes();
		for(var i=0; i<gridImportNodes.length; i++){
			var key=gridImportNodes[i].url;
			if(map[key]>=0){
				alert("Duplicated target URL at line: " + (map[key]+1));
				return;
			}
		}

		return map;
	}

	bulkOpenMetadataFile(){
        setTimeout(function(){
    		$('#bulkImportMetadataFile').trigger('click');
		}, 200);
    }

	bulkUploadMetadataFile(file){
		var that=this;
		var reader = new FileReader();
		reader.addEventListener("loadend", function () {
			var url="/bulk-import/parse?targetInstanceOid=" + that.jobId + "&harvestNumber=" + that.harvestResultNumber;
			var req={
				content: reader.result,
				metadata: {}
			};
			fetchHttp(url, req, function(rsp){
				$('#popup-window-bulk-import .overlay').hide();
				if (!rsp || rsp.rspCode !== 0) {
					console.log('Invalid response from WCT server');
					return;
				}

				var dataset=JSON.parse(rsp.payload);
				
				var newBulkTargetUrlMap=that._bulkValidateImportMetaData(dataset);
				if (!newBulkTargetUrlMap) {
					return;
				}

				gPopupModifyHarvest.gridImportPrepare.setRowData(dataset);
			});
		});

		$('#popup-window-bulk-import .overlay').show();
		$('#popup-window-bulk-import').show();

		reader.readAsDataURL(file);
	}

	bulkAddData2ToBeImportedGrid(){
		var that=this;
		var dataset=gPopupModifyHarvest.gridImportPrepare.getAllNodes();
		gPopupModifyHarvest.insertImportData(dataset);
		$('#popup-window-bulk-import').hide();
	}

	checkFilesExistAtServerSide(dataset, callback){	
		var url = "/modification/check-files?job=" + this.jobId + "&harvestResultNumber=" + this.harvestResultNumber;
		fetchHttp(url, dataset, function(response){
			if(response.respCode!==1){
				var html=$('#tip-bulk-import-prepare-invalid').html();
				$('#tip-bulk-import-prepare').html(html);
			}else{
				$('#tip-bulk-import-prepare').html('All rows are valid.');
			}

			callback(response);
		});
	}

	nextBulkImportTab(step){
      step=(step+1) % 2;
      $('.tab-bulk-import').hide();
      $('#tab-bulk-import-'+step).show();
      $('#btn-bulk-import-submit').attr('step', step);
      if(step===0){
        $('#bulkImportMetadataFile').val(null);
        $('#label-bulk-import-metadata-file').html('Choose file');
        $('#bulkImportContentFile').val(null);
        $('#btn-bulk-import-submit').html('Next');        
      }else{
        $('#btn-bulk-import-submit').html('Re-crawl');
        $('#btn-bulk-import-submit').attr('status', 'recrawl');
      }
    }


    bulkPrune(){
		var file=$('#bulkPruneMetadataFile')[0].files[0];
		if(!file){
			alert("You must specify a metadata file name to prune.");
			return;
		}
		var ignoreInvalidURLsFlag=$("#checkbox-ignore-invalid-prune-urls").is(":checked");
		var ignoreDuplicatedURLsFlag=$("#checkbox-ignore-duplicated-prune-urls").is(":checked");

		var that=this;
		var reader = new FileReader();
		reader.addEventListener("loadend", function () {
			var searchCondition={
	          "domainNames": [],
	          "contentTypes": [],
	          "statusCodes": [],
	          "urlNames": []
	        }

	        var map={};
			var gridImportNodes=gPopupModifyHarvest.gridImport.getAllNodes();
			for(var i=0; i<gridImportNodes.length; i++){
				var key=gridImportNodes[i].url;
				map[key]=2;
			}

			var text=reader.result;
			var lines=text.split('\n');
			for(var i=0;i<lines.length;i++){
				var url=lines[i].trim();

				console.log(url);

				if(!url.toLowerCase().startsWith("http://") &&
					!url.toLowerCase().startsWith("https://")){
					if(!ignoreInvalidURLsFlag){
						alert("You must specify a valid URL at line:" + (i+1));
						return;
					}
				}

				if(map[url]===2 || map[url]===1){
					if(!ignoreDuplicatedURLsFlag){
						alert('Duplicated URL: ' + url);
						return;
					}
				}else{
					map[url]=1;
					searchCondition.urlNames.push(url);
				}
			}

			$('#popup-window-bulk-prune').hide();

			gPopupModifyHarvest.checkUrls(searchCondition, 'prune');

		});

		// reader.readAsDataURL(file);
		reader.readAsText(file);

	}
}