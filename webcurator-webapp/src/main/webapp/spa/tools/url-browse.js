var _browse_openwayback='/curator/tools/content';

function browseUrl(data, _browse_type){
	_browse_type=_browse_type.toUpperCase();
	var url;
	if (_browse_type==='LOCAL') {
		// if(data.contentType==='text/html') {		
		// 	url='/curator/tools/browse/'+harvestResultId+'/?url='+btoa(data.url);
		// }else{
		// 	url='/curator/tools/download/'+harvestResultId+'/?url='+btoa(data.url);
		// }
		url='/curator/tools/browse/'+harvestResultId+'/?url='+btoa(data.url);
	} else if(_browse_type==='LIVESITE'){
		url=data.url;
	}else{
		url=_browse_openwayback+'?'+data.url;
	}
	window.open(url);
}

function downloadUrl(data){
	_browse_type=_browse_type.toUpperCase();

	var url='/curator/tools/download/'+harvestResultId+'/?url='+btoa(data.url);

	downloadRemote(url, data.url);
}

function downloadRemote(url, name){
	fetch(url).then((res) => {
		if (res.ok) {
			return res.blob();
		}else if(res.status === 404){
			popupMessage('Resource Not Exist(404): ' + data.url);
		}else{
			popupMessage(res.status + ': ' + res.statusText);
		}
		return null;
	}).then((blob) => {
		console.log(blob);
		if(blob){
			saveAs(blob, name);
		}
	});
}


function popupMessage(msg){
	// setTimeout(function () {

	// }, 300);

	toastr.error(msg, 'Error', {
		showDuration: 30,
		hideDuration: 100,
		extendedTimeOut: 700,
		timeOut: 5000,
	});
}